# ADR-003: Rule-based Scoring before ML

- **Status:** Accepted for MVP
- **Context:** no validated labeled dataset; MVP needs explainable, repeatable behavior and robust workflow.
- **Decision:** deterministic, versioned rules in risk module; persist score/reasons/ruleset snapshot; default threshold 70 configurable.
- **Alternatives:** ML first (needs labels/evaluation/drift operations); FastAPI remote scorer immediately (adds network failure modes prematurely).
- **Consequences/trade-offs:** transparent and testable, but detection quality limited and weights are demo assumptions. Later scoring interface/adapter can call FastAPI while preserving orchestration and snapshot contract.
