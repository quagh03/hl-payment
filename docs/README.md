# HL Payment Platform — Documentation

Single source of truth for product, architecture, domain, and backlog documentation.
Code lives in `platform/` and `services/`; everything about *why* and *what* lives here.

## Folder structure

```
docs/
├── README.md                         # this file: structure, conventions, naming
├── product/                          # business intent
│   ├── vision.md                     # problem, goals, non-goals, success criteria
│   ├── glossary.md                   # ubiquitous language (DDD)
│   └── personas.md                   # user, merchant, ops, PSP
├── architecture/                     # how the system is built
│   ├── high-level-design.md          # HLD: services, context map, money model, sagas, infra
│   ├── context-map.md                # bounded contexts and relationships (detail)
│   ├── cross-cutting/                # one doc per platform concern
│   │   ├── messaging.md              # outbox, inbox, envelope, topics
│   │   ├── idempotency.md
│   │   ├── security.md
│   │   ├── observability.md
│   │   └── resilience.md
│   ├── services/                     # one low-level design per service
│   │   ├── ledger-service.md
│   │   ├── payment-service.md
│   │   └── ...
│   └── adr/                          # Architecture Decision Records (immutable once accepted)
│       ├── ADR-0001-kong-api-gateway.md
│       └── ...
├── domain/                           # DDD working artifacts
│   ├── event-storming/               # big-picture + process-level boards (images/markdown)
│   ├── aggregates/                   # aggregate invariants, state machines
│   └── events/                       # domain/integration event catalog
├── api/                              # contracts (generated or hand-written)
│   ├── openapi/                      # <service>.yaml
│   └── events/                       # JSON Schema per event type + version
├── backlog/                          # delivery planning
│   ├── roadmap.md                    # phases and milestones
│   ├── delivery-plan.md              # milestone → epic → feature → story breakdown
│   ├── epics/                        # EP-xxx-<slug>.md
│   ├── features/                     # FT-xxx-<slug>.md
│   └── user-stories/                 # US-xxx-<slug>.md
├── runbooks/                         # operational procedures (local env, recon breaks, DLT replay)
└── templates/                        # copy these to create new docs
    ├── epic.md
    ├── feature.md
    ├── user-story.md
    ├── adr.md
    └── service-design.md
```

## Naming and identifiers

| Artifact | ID format | File name | Example |
|---|---|---|---|
| Epic | `EP-<3 digits>` | `backlog/epics/EP-001-<slug>.md` | `EP-001-external-top-up.md` |
| Feature | `FT-<3 digits>` | `backlog/features/FT-001-<slug>.md` | `FT-003-momo-webhook-ingestion.md` |
| User story | `US-<3 digits>` | `backlog/user-stories/US-001-<slug>.md` | `US-012-top-up-via-momo.md` |
| ADR | `ADR-<4 digits>` | `architecture/adr/ADR-0001-<slug>.md` | `ADR-0002-separate-ledger-service.md` |
| Service design | service name | `architecture/services/<service>.md` | `ledger-service.md` |
| Event schema | `<context>.<event>.v<n>` | `api/events/<context>/<event>.v<n>.json` | `api/events/payment/payment-completed.v1.json` |

Hierarchy: **Epic → Feature → User story**. Every story links up to its feature; every feature links up to its epic and to the services it touches.

## Conventions

- Markdown only, Mermaid for diagrams (renders on GitHub/Cursor). Images go next to the doc in an `assets/` subfolder.
- Each doc starts with a metadata block: `Status`, `Owner`, `Last updated`, and (for backlog) `Phase`.
- Statuses: `Draft → Proposed → Accepted → Deprecated/Superseded` for design and ADRs; `Backlog → Ready → In progress → Done` for backlog items.
- ADRs are never edited after `Accepted`; write a new ADR that supersedes the old one.
- Use terms from `product/glossary.md` exactly. If a term is missing, add it before using it in code.
- Amounts in docs are written as `100,000 VND`; in code and schemas they are `amount_minor` + `currency`.

## Start here

1. `product/vision.md` — what we are building and why.
2. `architecture/high-level-design.md` — the system in one read.
3. `backlog/roadmap.md` — what gets built in which phase.
