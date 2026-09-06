# RAG reliability: gating, confidence & hybrid search

The classic way a grounded agent fails in production isn't a crash — it's answering
confidently from context that's irrelevant or simply missing. Two layers guard against that.

## 1. Score gating + retrieval confidence (application layer)

This lives in `RagRetrievalPolicy` and `RagService` (`agent-application`,
`execution.service.common`).

Retrieval over-fetches `agent.rag.candidate-pool-size` candidates and then throws most of them
away: a candidate survives only if its similarity score (`(1 + cosine) / 2`, on a 0–1 scale)
clears `agent.rag.min-score`, and at most `agent.rag.max-passages` are kept. Each survivor is
annotated with its score before it goes into the prompt, so the model can weigh a 0.78 passage
differently from a 0.96 one.

The strongest surviving score becomes the **retrieval confidence** — logged per execution and
written into a grounding header the model reads.

When nothing clears the bar — a first-turn conversation, or a retrieval that failed outright —
the **grounding guard** kicks in: instead of a silent empty context, the agent is told plainly
that no reliable context was found and instructed to set `data_sufficient=false` and
`confidence=low` rather than make something up. That lines up with the structured-output schema
fields (`confidence`, `data_sufficient`) the model already has to fill in.

| Property | Default | Meaning |
| --- | --- | --- |
| `agent.rag.candidate-pool-size` | 20 | candidates fetched before gating |
| `agent.rag.min-score` | 0.75 | keep threshold (≈ cosine 0.5) |
| `agent.rag.max-passages` | 5 | max kept passages injected |

## 2. Hybrid dense + sparse retrieval (adapter layer)

`HybridVectorStoreAdapter` (`agent-adapter`, `out.vector`, marked `@Primary`) runs two
searches and merges them.

The dense side is the ordinary langchain4j vector search over pgvector. The sparse side is a
Postgres `websearch_to_tsquery` full-text match on the passage text, scoped to the
conversation. The trick that makes them combinable: the sparse query computes cosine
similarity in the *same* SQL using langchain4j's own formula, `(2 - (embedding <=> q)) / 2`, so
a sparse hit and a dense hit carry directly comparable scores and both feed the gating layer
without special-casing.

The two result sets are merged with Reciprocal Rank Fusion (`score = Σ 1 / (rrf-k + rank)`);
each passage keeps its strongest cosine score. If the sparse side fails or is switched off, the
adapter quietly falls back to dense-only, and indexing is left to `PgVectorStoreAdapter`
untouched.

Why bother with sparse at all? Dense embeddings are great at meaning and routinely fumble exact
tokens — invoice IDs, error codes, plan names. Ask for `INV-4821` and full-text search finds it
where the vector search wanders off to "billing" in general.

| Property | Default | Meaning |
| --- | --- | --- |
| `agent.rag.hybrid.enabled` | true | enable the sparse side + fusion |
| `agent.rag.hybrid.dense-limit` | 20 | dense candidates before fusion |
| `agent.rag.hybrid.sparse-limit` | 20 | sparse candidates before fusion |
| `agent.rag.hybrid.rrf-k` | 60 | RRF constant (larger = flatter weighting) |

### Schema & the full-text index

Schema changes — the `retrieval_confidence` column and the full-text GIN index that backs
sparse retrieval — are managed by **Liquibase** in the CD repo
(`charts/agent-service/migrations/changelog.sql`), run as an Argo CD PreSync hook before
each rollout. The GIN index affects latency only; the sparse query is correct without it.

## See also

Relevance is only half the story. Whether a passage is *allowed* into the prompt — or allowed
into memory in the first place — is governed separately by the
[agent memory harness](agent-memory-harness.md).

## Not (yet) done

- Retrieval confidence is logged and threaded into the prompt, but not persisted on
  `AgentExecution`. Persisting it (and a combined answer-reliability score) would let ops
  alert on low-confidence answers — a natural follow-up.
- The text-search configuration is fixed to `'english'`.
