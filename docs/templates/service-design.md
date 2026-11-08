# <service-name> — Service Design

| | |
|---|---|
| Status | Draft / Proposed / Accepted |
| Owner | |
| Last updated | YYYY-MM-DD |
| Database | `<service>_db` |
| HLD | [high-level-design.md](../high-level-design.md) |

## Responsibility

One paragraph: the bounded context this service owns and what it explicitly does not own.

## Ubiquitous language

| Term | Meaning |
|---|---|

## Domain model

- Aggregates, entities, value objects
- Invariants
- State machines (Mermaid `stateDiagram-v2`)

## API

| Method | Path | Purpose | Auth |
|---|---|---|---|

Link to `contracts/openapi/<service>.yaml`.

## Events

| Direction | Event | Topic | Schema |
|---|---|---|---|
| Produces | | | |
| Consumes | | | |

## Persistence

- Tables (key columns, indexes, uniqueness constraints)
- Outbox / inbox usage

## Dependencies

| Depends on | Type | Failure behavior |
|---|---|---|
| | sync REST / event | |

## Non-functional

- Idempotency, concurrency control, limits, timeouts, circuit breakers
- Metrics, logs, traces specific to this service

## Open questions

- 
