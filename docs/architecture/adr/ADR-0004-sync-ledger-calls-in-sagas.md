# ADR-0004: Synchronous REST for `payment-service` → `ledger-service` inside sagas

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-08 |
| Deciders | huylq |

## Context

Saga steps that reserve, post, or void funds need a definitive answer before the orchestrator decides the
next step. Options: synchronous REST calls, or asynchronous command events with reply events.

## Decision

Use **synchronous REST** (`RestClient` + `@HttpExchange`) for ledger commands within sagas, with:

- an `idempotency_key` on every command (replays return the original outcome),
- strict timeouts and Resilience4j circuit breaker,
- persisted saga state before and after each step so a crash resumes safely.

Ledger **facts** (`TransferPosted`, `TransferVoided`) are still published as events for projections and
other consumers.

## Consequences

- Positive: simpler orchestrator; immediate error semantics (`INSUFFICIENT_FUNDS`, `ACCOUNT_FROZEN`); easier to debug.
- Negative: temporal coupling to ledger availability; mitigated by idempotent retries and the circuit breaker, and by the persisted saga that resumes when the ledger returns.
- Future: an async command/reply variant can be implemented for P2P transfer as a comparison exercise without changing the ledger API.
