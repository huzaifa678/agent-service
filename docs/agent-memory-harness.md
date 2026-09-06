# The agent memory harness

The harness is the envelope around the agent's memory. It does two jobs: it *manages* memory
as two tiers, and it *governs* what crosses the memory boundary. Both live in
`agent-application`, under `execution.service.harness`.

## Two tiers of memory

Agents need memory at two timescales, and they're different mechanisms:

- **Short-term (working) memory** — `ShortTermMemory`. The most recent turns of the live
  conversation, verbatim, bounded to a window (`agent.memory.short-term.max-messages`). This
  is recency-based and exact: no embedding, no search, just the tail of the transcript. The
  window keeps a long conversation from growing the prompt without limit and eventually blowing
  the context window.
- **Long-term memory** — the pgvector store, behind `RagService`. Everything the agent has
  ever said in a conversation, recalled by *semantic relevance* rather than recency,
  score-gated, and fused across dense + sparse retrieval (see
  [rag-hybrid-search.md](rag-hybrid-search.md)). When a turn falls out of the short-term
  window it isn't lost — it's still in long-term memory, retrievable when it's relevant again.

`AgentMemory` is the facade that ties them together. On each turn it assembles the prompt tier
by tier — system prompt, then the short-term window, then the long-term passages that survived
— and after the answer it commits the reply back to long-term memory. The workflow talks only
to `AgentMemory`; it no longer wires the prompt and RAG services together itself.

```
AgentMemory.recall(conversation, query):
    system prompt                     ← PromptAssemblyService.systemMessage()
    + short-term window (last N turns, ← ShortTermMemory  (+ SecretRedactor)
        secrets masked)
    + long-term passages (relevant,    ← RagService  ─┐ rate limit (per conversation)
        allowed)                                       ├ score gating
                                                       └ memory policy (below)

AgentMemory.remember(conversationId, answer):
    → long-term write                 ← RagService  (rate-limited, policy-gated)
```

## Governing the boundary

Managing memory isn't enough — you also have to decide what's *allowed* into it. Whatever
lands in long-term memory gets replayed to the model on future turns, so a leaked credential
or one tenant's data captured in another's conversation doesn't leak once, it's remembered and
re-served.

Score gating already decides whether a passage is *relevant*; that's a scoring problem and
lives in code. "Allowed" is a governance problem — the kind of rule that changes for
compliance reasons, per tenant, without a code review — so it belongs in policy.

The two tiers are governed differently, because their constraints differ:

- **Long-term** memory *drops* disallowed content — a passage the policy denies simply doesn't
  enter the prompt, and a message it denies isn't persisted. This is where the OPA policy runs.
- **Short-term** memory can't drop — it's the live transcript and the model needs the turn in
  front of it — so it *redacts* instead. `SecretRedactor` masks credential-shaped substrings
  (the same shapes the Rego policy screens: provider tokens, private-key blocks, inline
  `key=value` secrets) so a key a user just pasted is answered around but never echoed back
  through the prompt, and so never captured into long-term memory on the following turn.

## Where the policy sits

`AgentMemoryHarness` (`agent-application`, `execution.service.harness`) wraps the two memory
boundaries. `RagService` calls it at each:

| Boundary | When | What the harness does |
| --- | --- | --- |
| **recall** | after score gating, before passages enter the prompt | checks each passage; drops the ones policy denies |
| **persist** | before an assistant message is embedded and stored | allows or vetoes the write |

The verdict comes from a `MemoryPolicyPort`. In production that port is
`OpaMemoryPolicyAdapter`, which POSTs the access to an **Open Policy Agent** sidecar and lets
the `agent/memory` Rego bundle decide. OPA is the same policy-as-code engine the repo already
runs in CI to lint Dockerfiles (`policy/docker`); this just puts it on the runtime path.

```
        recall path                              persist path
  ┌────────────────────┐                   ┌────────────────────┐
  │ vector search      │                   │ assistant message  │
  │   → score gating   │                   └─────────┬──────────┘
  └─────────┬──────────┘                             │
            │ gated passages                         │ content
            ▼                                         ▼
     AgentMemoryHarness.guardRecall          AgentMemoryHarness.guardPersist
            │  per passage                            │
            ▼                                         ▼
        MemoryPolicyPort ────────► OPA sidecar ◄──────┘
                                (policy/agent/memory.rego)
            │ allowed subset                          │ allow / deny
            ▼                                         ▼
     into the prompt                          vectorStore.index(...) or skip
```

## The policy

`policy/agent/memory.rego` (package `agent.memory`) is evaluated per access. The input is a
plain document:

```json
{
  "operation": "RECALL",
  "conversation_id": "…",
  "content": "…",
  "attributes": { "score": 0.91 }
}
```

and the bundle returns `{"allow": <bool>, "reason": "<why>"}`. The reason is carried into logs
and metrics so a denial can be traced back to the rule that produced it. The rules shipped
today:

| Operation | Rule | Reasoning |
| --- | --- | --- |
| persist | content matching a credential pattern (OpenAI/Slack tokens, private-key headers, inline `password=…`) is refused | a secret embedded into memory is a secret replayed on every future turn |
| persist | `attributes.retention == "none"` is refused | lets a tenant opt out of retention (erasure / no-retention contracts) |
| recall | `attributes.tenant_mismatch == true` is dropped | a passage tagged for another tenant must never re-enter this prompt |

Anything no rule matches is allowed — the policy is a deny-list, so a fresh checkout with an
empty ruleset is a no-op. Rules are unit-tested in `policy/agent/memory_test.rego`
(`conftest verify -p policy/agent`), which runs in the `security` CI workflow next to the
Dockerfile checks.

## Rate limiting

Long-term recall and persist are the expensive memory operations — each embeds text and hits
pgvector (and, with governance on, OPA). A runaway agent loop or an abusive client can drive a
lot of them. `MemoryRateLimiter` puts a per-conversation ceiling on the long-term tier using a
Bucket4j token bucket held in process: each conversation gets `capacity` tokens, recall and
persist spend one each, and the bucket refills at `refill-tokens` every `refill-period-seconds`.

It's a protective ceiling, not an authorization check — so when a conversation is out of tokens
the harness *skips that turn's long-term work* (recall falls back to the short-term window; a
persist is dropped) and logs it, rather than failing the turn or returning a 429. The circuit
breakers below handle dependency *outages*; the rate limiter handles *volume*.

Buckets are keyed by conversation and never evicted, which is fine for the request volumes here;
a Caffeine-backed map with idle eviction (or a distributed bucket over Redis, for multi-replica
fairness) is the natural next step if that changes.

## Failure behaviour

Memory sits on the hot path of every turn but nothing in it should be able to take a turn
down. Three things can go wrong:

- **Policy denies.** Expected and cheap. A denied recall passage is dropped (the prompt is
  slightly thinner); a denied persist is skipped (memory indexing is already best-effort).
- **Rate limit exhausted.** The turn's long-term work is skipped (see above); short-term memory
  still answers the turn.
- **A dependency is down or slow.** The two external calls memory makes — OPA, and the pgvector
  store — are each wrapped in a Resilience4j circuit breaker (`memoryPolicy` and `vectorStore`,
  configured alongside the existing `llmProvider` breaker in `application.properties`). A
  sustained failure trips the breaker so subsequent calls fail fast instead of each paying the
  timeout, and each breaker's fallback maps the failure onto the degradation the caller already
  expects:
  - **OPA** → `MemoryPolicyUnavailableException`. The *harness* owns the response, governed by
    `agent.memory.policy.fail-open`: **fail-closed** (default) drops the recall and skips the
    persist so an ungoverned access never slips through; **fail-open** treats the outage as an
    allow, for deployments where availability beats strict governance.
  - **pgvector** → the best-effort `VectorSearchException` / `EmbeddingGenerationException` that
    `RagService` already handles, so recall degrades to the short-term window plus a grounding
    guard and a write becomes a no-op. A genuine config error
    (`UnsupportedEmbeddingModelException`) is excluded from the breaker and still surfaces.

Breaker state is exposed on `/actuator/health` via the Resilience4j health indicator, so an
open memory-policy or vector-store circuit is visible to the orchestrator and to ops.

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `agent.memory.short-term.max-messages` | `20` | short-term window size; `0` or less = unbounded (whole transcript) |
| `agent.memory.short-term.redact.enabled` | `true` | mask credential-shaped substrings in the short-term window |
| `agent.memory.rate-limit.enabled` | `true` | per-conversation rate limit on long-term operations |
| `agent.memory.rate-limit.capacity` | `30` | bucket size (tokens) per conversation |
| `agent.memory.rate-limit.refill-tokens` | `30` | tokens added each refill period |
| `agent.memory.rate-limit.refill-period-seconds` | `60` | refill period |
| `agent.memory.policy.enabled` | `false` | when off, `AllowAllMemoryPolicyAdapter` permits everything — no OPA needed for local/dev |
| `agent.memory.policy.fail-open` | `false` | posture when OPA is unreachable (see above) |
| `agent.memory.policy.opa.base-url` | `http://localhost:8181` | OPA sidecar address |
| `agent.memory.policy.opa.decision-path` | `/v1/data/agent/memory/decision` | Data API path for the decision |
| `agent.memory.policy.opa.timeout-ms` | `250` | connect + read timeout for the localhost round-trip |

Because it defaults off, wiring the harness in changed no existing behaviour: with the
allow-all adapter active, `guardRecall` returns its input untouched and `guardPersist` always
returns true.

## Metrics

`MicrometerMemoryPolicyMetricsAdapter` emits through the same OTLP pipeline as the RAG metrics:

- `agent.memory.policy.decisions` — tagged `operation` (recall/persist) and `outcome`
  (allow/deny). A rising deny rate tells you the policy is actively redacting.
- `agent.memory.policy.unavailable` — tagged `operation`; increments whenever the harness had
  to fall back to its posture. Alert on any sustained rate — it means governance is running
  blind.

## Not (yet) done

- Recall evaluates one OPA call per passage. With `max-passages` ≤ 5 that's fine, but a batch
  decision endpoint would cut it to one round-trip.
- `RunAgentCommand` carries no tenant/user, so the tenant-oriented rules (`retention`,
  `tenant_mismatch`) are wired through `attributes` but not yet populated end-to-end —
  threading `TenantId` down from the API is the natural follow-up.
- Long-term denials are all-or-nothing per passage; the policy can't yet return a *redacted*
  rewrite of a passage the way short-term memory does (mask a token, keep the rest).
- The secret patterns (shared in spirit by `SecretRedactor` and the Rego policy) are a
  pragmatic starter set, not a full DLP classifier — and they're maintained in two places, which
  a shared source would fix.
- Rate-limit buckets are per-conversation and in-process: never evicted, and not shared across
  replicas (see the note in "Rate limiting").
- Short-term memory is a plain fixed-size window. Turns that fall out survive in long-term
  memory but aren't summarised into a rolling digest — a recency+summary hybrid would carry
  more of the early conversation forward cheaply.
