# Roadmap

| | |
|---|---|
| Status | Proposed |
| Last updated | 2026-11-09 |
| HLD | [high-level-design.md](../architecture/high-level-design.md) |

Each phase ends with a runnable system and at least one end-to-end scenario test. Milestones and epics
are [delivery-plan.md](./delivery-plan.md). Write the component design, then split the epic into
`epics/`, `features/`, and `user-stories/` when it moves to Ready. M0 is the exception: its features
are already listed in the delivery plan.

## Phase 0 — Foundation

Deliverables:
- Gradle monorepo: `settings.gradle.kts`, version catalog, convention plugins (`build-logic/`). After the probe service exists, a `build-logic` task scaffolds the next service onto those plugins without copying versions out of the catalog.
- `platform-*` libraries: core types, `ProblemDetail` handling, required headers, correlation id, idempotency filter, outbox/inbox, event envelope, resource-server security, observability defaults, Testcontainers fixtures, ArchUnit rules.
- `deploy/local/docker-compose.yml`: Postgres, Kafka + UI, Redis, Kong (DB-less), MailHog, OTel Collector, Prometheus, Grafana, Tempo, Loki.
- `identity-service`: Spring Authorization Server, users/clients/roles, JWKS; Kong `jwt` plugin wired to its public key.

Exit criteria: a token from `identity-service` passes through Kong to a protected `/v1/ping` endpoint in a sample service, with trace visible in Grafana Tempo.

Concepts: Gradle multi-project, Spring Boot 4 baseline, OAuth2/OIDC, edge routing, observability pipeline.

## Phase 1 — Internal money

Deliverables:
- `ledger-service`: chart of accounts, journal/postings, two-phase transfers, balances, outbox.
- `wallet-service`: wallet aggregate, limits, balance projection from `ledger.events.v1` via inbox.
- `user-service`: profile, KYC tiers, status, limits API.
- P2P transfer end-to-end (reserve → limits/KYC check → post/void) orchestrated by a first slice of `payment-service`.

Exit criteria: property tests prove ledger invariants; a transfer between two wallets updates both projections; killing `wallet-service` mid-flow and restarting results in a consistent projection.

Concepts: DDD aggregates, double-entry bookkeeping, optimistic locking, outbox/inbox, projections.

## Phase 2 — External money in (Mock PSP)

Deliverables:
- `gateway-adapter-service` with Mock PSP provider, webhook endpoint, signature verification, webhook inbox, status query.
- `tools/mock-psp` simulator: success, failure, duplicate webhook, late webhook, no webhook.
- `payment-service` top-up saga, saga persistence, polling fallback, `REQUIRES_REVIEW` with `SUSPENSE` posting.
- Kong `/webhooks/*` routes.

Exit criteria: all Mock PSP scenarios end in a consistent payment and ledger state; duplicate webhooks produce exactly one posting.

Concepts: orchestration saga, anti-corruption layer, idempotency, webhook handling, process timeouts.

## Phase 3 — Real PSPs, merchants, notifications

Deliverables:
- MoMo and VNPay sandbox providers in `gateway-adapter-service`.
- `merchant-service`: onboarding, API keys + HMAC, merchant orders, merchant payment (hold/capture/void), refunds.
- `notification-service`: email, in-app, outbound merchant webhooks with retry/backoff and delivery log.

Exit criteria: a user tops up via VNPay sandbox, pays a merchant order, receives a partial refund; merchant receives signed webhooks; all reflected in the ledger.

Concepts: real integrations, hold/capture, choreography consumers, outbound webhook reliability.

## Phase 4 — Money out, reconciliation, resilience engineering

Deliverables:
- Withdrawal saga with compensation (pending debit → disbursement via Mock bank → post or void).
- `reconciliation-service`: ingest PSP settlement files, match against ledger/gateway, raise breaks.
- Debezium CDC outbox relay replacing the polling relay.
- Chaos scenarios, DLT replay runbook, Grafana dashboards, SLO definitions.
- Optional: Avro + Schema Registry for one topic (compare with JSON).

Exit criteria: chaos scenarios pass; reconciliation detects an injected break; dashboards show SLOs.

Concepts: compensation, reconciliation, CDC, fault injection, SLOs.
