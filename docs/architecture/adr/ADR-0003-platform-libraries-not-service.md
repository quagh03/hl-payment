# ADR-0003: Cross-cutting concerns as `platform-*` libraries, not a `common-service`

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-08 |
| Deciders | huylq |

## Context

The original proposal listed a `common-service` to standardize API responses, exceptions, required headers,
and correlation IDs across microservices.

## Decision

Ship cross-cutting code as versioned Gradle libraries under `platform/`:
`platform-bom`, `platform-core`, `platform-web`, `platform-messaging`, `platform-security`,
`platform-observability`, `platform-test`. No runtime `common-service`.

Rules for the shared kernel:

- Infrastructure and primitive types only (`Money`, typed ids, `ErrorCode`, `EventEnvelope`, filters, outbox/inbox). **No domain aggregates or business rules.**
- Each service pins its own `platform-*` version via `platform-bom`; upgrades are per service.
- Additive, backward-compatible changes only within a major version.

## Consequences

- Positive: no network hop or single point of failure for in-process concerns; no distributed monolith.
- Positive: consistent `ProblemDetail` errors, headers, idempotency, and messaging semantics everywhere.
- Negative: library upgrades require redeploying services; mitigated by small, focused modules and semantic versioning.
