# ADR-0005: JSON events with JSON Schema before Avro and Schema Registry

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-08 |
| Deciders | huylq |

## Context

Integration events need a serialization format and a compatibility strategy. Options: JSON with JSON
Schema in the repo, Avro/Protobuf with a Schema Registry.

## Decision

Start with **JSON payloads in a CloudEvents-shaped envelope**, schemas stored under `contracts/events/`
and validated in consumer tests. Compatibility rule: additive-only changes within a version; breaking
changes create `v<n+1>` of the event type and a new topic suffix.

## Consequences

- Positive: human-readable messages in Kafka UI and logs; zero extra infrastructure; schemas reviewed in pull requests.
- Negative: larger payloads and no registry-enforced compatibility; acceptable for an educational platform.
- Future: Phase 4 may introduce Avro + Schema Registry for `ledger.events.v1` to compare approaches; this ADR will be superseded if that becomes the default.
