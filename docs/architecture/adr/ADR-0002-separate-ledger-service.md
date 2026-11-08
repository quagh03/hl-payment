# ADR-0002: Separate `ledger-service` from `wallet-service`

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-08 |
| Deciders | huylq |

## Context

The original proposal placed wallet, balance, ledger, deposit, debit, and credit in a single
`wallet-service`. Double-entry bookkeeping and distributed transaction management are explicit learning
goals.

## Decision

Introduce a dedicated **`ledger-service`** as the system of record for money. `wallet-service` owns the
product-facing wallet aggregate (owner, status, limits, currency, mapping to a ledger account) and keeps a
balance **read model** projected from ledger events.

The ledger exposes two-phase transfers (`reserve → post | void | expire`) so sagas can hold funds while
deciding, and `postImmediately` for single-step movements such as confirmed top-ups.

## Consequences

- Positive: clean ubiquitous language per context (product vs accounting); PSP receivables, merchant balances, fees, and suspense accounts have a natural home.
- Positive: exercises the real problem of a projected balance that must reconcile against the ledger.
- Negative: more moving parts in Phase 1; P2P transfer becomes a cross-service flow instead of a local transaction.
- Mitigation: balance reads for authorization decisions (`available ≥ amount`) are always performed by the ledger during `reserve`, never from the wallet projection.

## Alternatives considered

- **Single `wallet-service` with Spring Modulith modules `wallet` and `ledger`**: viable fallback that keeps money movement in one local transaction; can be split later. Rejected for now because it defers the distributed consistency problems the project intends to study.
