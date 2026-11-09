# The agent guardrails

The memory harness ([agent-memory-harness.md](agent-memory-harness.md)) governs one boundary —
what the agent *remembers*. These three additions extend the same harness-plus-policy pattern to
the agent's other risk boundaries: what it may **do** (tools), what crosses the **model**
(input/output), and what it may **spend** (budget). Each is an outbound port with an OPA/Rego
adapter in production and an allow-all adapter by default, so a fresh checkout and every local/dev
run behave exactly as before until the governance flags are turned on.

```
                        the agent's risk boundaries
  ┌───────────────┬───────────────┬───────────────┬───────────────┐
  │   memory      │    tools      │   model I/O   │    spend      │
  │ AgentMemory-  │ AgentTool-    │ AgentGuard-   │ AgentBudget-  │
  │   Harness     │   Harness     │  railHarness  │    Guard      │
  ├───────────────┼───────────────┼───────────────┼───────────────┤
  │ recall /      │ enable /      │ input /       │ per-subject   │
  │ persist       │ invoke        │ output        │ token + cost  │
  ├───────────────┼───────────────┼───────────────┼───────────────┤
  │ agent/memory  │ agent/tools   │ agent/io      │ (in code —    │
  │  .rego (OPA)  │  .rego (OPA)  │  .rego (OPA)  │  like the     │
  │               │               │               │ rate limiter) │
  └───────────────┴───────────────┴───────────────┴───────────────┘
```

Why policy vs. code is split the same way as the memory harness: a *decision that changes per
tenant or for compliance reasons without a code review* belongs in OPA (tool allow-lists,
injection patterns, grounding floors); *arithmetic* (is this conversation over its token
ceiling?) belongs in code, exactly as score gating and rate limiting already do.

## 1. Tool authorization gate

`AgentToolHarness` (`execution.service.harness`) sits in front of
`ToolExecutionService`. `ToolExecutionService` already refuses tools the build does not
*support*; the harness decides whether a supported tool is *authorized* in this context. It
guards two points:

| Boundary | When | On deny |
| --- | --- | --- |
| **enable** | pre-flight, as the client's `enabledTools` are validated | `ToolNotAuthorizedException` (HTTP 422) — the tool is never offered to the model |
| **invoke** | as the model's actual tool calls are processed | the call is recorded as a failed (blocked) `ToolExecution` and skipped; the turn still answers |

The verdict comes from `ToolPolicyPort` → `OpaToolPolicyAdapter` → `agent/tools.rego`. Starter
rules: an optional per-tenant allow-list / deny-list, a `code_interpreter` entitlement gate, and
an SSRF guard that refuses a `web_search` aimed at a loopback, private, or cloud-metadata host.
When OPA is unreachable the harness applies `agent.tools.policy.fail-open` — **fail-closed**
(default) denies the tool, since an ungoverned side-effecting tool is worse than a thinner answer.

## 2. Input / output guardrails

`AgentGuardrailHarness` screens what crosses the model boundary, wired into
`AgentExecutionWorkflow`:

- **`guardInput`** (in `prepare`, before the user message is added to the conversation) — blocks
  detected prompt-injection / jailbreak attempts, so a crafted message never steers the model or
  lands in memory. On block: `InputGuardrailViolationException` (HTTP 400).
- **`guardOutput`** (in `finish`, before tools run or the answer is persisted) — blocks leakage /
  toxicity and enforces a **minimum-grounding floor** using the turn's RAG retrieval confidence
  (`AgentExecution.getRetrievalConfidence()`), handed to the policy as `grounding_confidence`. On
  block: `OutputGuardrailViolationException` (HTTP 422) — the answer is withheld and nothing is
  persisted.

Unlike the memory harness, which *drops* disallowed content and degrades the prompt, a guardrail
block is a **hard stop on the turn**. The verdict comes from `GuardrailPolicyPort` →
`OpaGuardrailPolicyAdapter` → `agent/io.rego`. The grounding floor is opt-in (`min_grounding`
defaults to 0), so a non-RAG question with zero grounding is never blocked by default. Posture on
OPA outage: `agent.guardrail.fail-open` (default fail-closed = block).

## 3. Budget (cost governance)

`AgentBudgetGuard` accumulates each completed turn's token usage and cost against a subject and
refuses a turn once a ceiling is reached (`BudgetExceededException`, HTTP 429), before any LLM
spend. Where `MemoryRateLimiter` caps the *frequency* of memory ops, this caps cumulative
*spend*. It is wired into `AgentExecutionWorkflow`: `check` in `prepare`, `record` in `finish`.

The subject is the **conversation** today. `RunAgentCommand` does not yet carry a `TenantId`;
once it does, keying this guard (and the policy `attributes` of the gates above) by tenant turns
it into the per-tenant monthly budget it is really meant to be — the enforcement does not change,
only the key. Like the rate limiter, counters are in-process: not shared across replicas and
reset on restart (a Redis-backed counter is the natural next step).

## Configuration

All three default **off**, so wiring them in changed no existing behaviour.

| Property | Default | Meaning |
| --- | --- | --- |
| `agent.tools.policy.enabled` | `false` | activate the OPA tool gate (else allow-all) |
| `agent.tools.policy.fail-open` | `false` | posture when OPA is unreachable |
| `agent.guardrail.enabled` | `false` | activate OPA input/output screening (else pass-all) |
| `agent.guardrail.fail-open` | `false` | posture when OPA is unreachable |
| `agent.budget.enabled` | `false` | activate per-conversation budget enforcement |
| `agent.budget.max-tokens` | `0` | per-conversation cumulative token ceiling; `0` = unlimited |
| `agent.budget.max-cost-usd` | `0` | per-conversation cumulative cost ceiling (USD); `0` = unlimited |

OPA base-url / decision-path / timeout mirror the memory harness keys under
`agent.tools.policy.opa.*` and `agent.guardrail.opa.*`. Each OPA call is wrapped in a
Resilience4j circuit breaker (`toolPolicy`, `guardrailPolicy`) alongside the existing
`memoryPolicy` breaker, visible on `/actuator/health`.

## Metrics

Emitted through the same OTLP pipeline as the memory and RAG metrics:

- `agent.tools.policy.decisions` — tagged `tool`, `outcome` (allow/deny).
- `agent.tools.policy.unavailable` — tagged `tool`.
- `agent.guardrail.decisions` — tagged `direction` (input/output), `outcome`.
- `agent.guardrail.unavailable` — tagged `direction`.

A rising deny rate on `guardrail.decisions{direction=input}` means injection attempts are being
blocked; on `tools.policy.decisions` that tools are being refused. Alert on any sustained
`*.unavailable` rate — it means a gate is running on its fallback posture.
