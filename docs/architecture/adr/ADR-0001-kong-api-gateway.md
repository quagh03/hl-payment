# ADR-0001: Kong Gateway as the edge API gateway

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-08 |
| Deciders | huylq |

## Context

The platform needs an edge layer for routing, JWT validation, rate limiting, correlation-id injection,
request size limits, and PSP webhook ingress. Options: Spring Cloud Gateway (a Java service we would own),
Kong Gateway OSS, or no gateway (clients call services directly).

## Decision

Use **Kong Gateway 3.x OSS in DB-less mode** with a declarative `kong.yml` checked into `deploy/local/kong/`.

- Kong handles coarse concerns: routing, `jwt` validation (RS256 public key from `identity-service`), `rate-limiting`, `correlation-id`, `request-size-limiting`, `cors`, `prometheus`, `opentelemetry`.
- Services remain OAuth2 resource servers and re-validate tokens and scopes (zero trust).
- Webhook routes (`/webhooks/*`) bypass JWT; signature verification is the adapter's responsibility.
- Merchant HMAC verification stays in `merchant-service` rather than Kong `hmac-auth`, so merchant credential lifecycle (rotation, revocation, audit) is owned by the merchant bounded context and not duplicated into Kong consumers.
- Kong Admin API is bound to localhost; configuration changes go through the declarative file only.

## Consequences

- Positive: no Java gateway to build and maintain; industry-standard edge; plugins cover rate limiting and observability out of the box; Kong traces join the OTel pipeline.
- Positive: forces explicit, reviewable routing config in version control.
- Negative: one more infrastructure component locally; Kong OSS `jwt` plugin needs the public key configured as a consumer credential (no JWKS auto-discovery in OSS), so key rotation requires a config update.
- Negative: less opportunity to learn reactive gateway programming; accepted because edge logic is not a learning goal here.

## Alternatives considered

- **Spring Cloud Gateway**: full control in Java, but adds a service to build, scale, and observe; edge logic is not a learning objective.
- **No gateway**: simplest, but spreads rate limiting and CORS across every service and exposes internal topology to clients.
