# Glossary — Ubiquitous Language

| | |
|---|---|
| Status | Draft |
| Last updated | 2026-11-08 |

Terms are grouped by bounded context. Use them exactly as written in code, APIs, events, and docs.

## Shared

| Term | Definition |
|---|---|
| Money | A value object `amount_minor` (long) + `currency` (ISO 4217). VND has minor unit 1. |
| Correlation Id | `X-Correlation-Id`; identifies one business request across all services and events. |
| Idempotency Key | Client-supplied `Idempotency-Key` header making a mutating request safely retryable. |
| Outbox | Table written in the same transaction as an aggregate change; relayed to Kafka. |
| Inbox | Table recording processed event ids per consumer to make handling idempotent. |
| Integration Event | A fact published to Kafka for other bounded contexts (`payment.completed`). |

## Identity

| Term | Definition |
|---|---|
| Credential | Username/password or client secret used to authenticate a principal. |
| Scope | OAuth2 permission string (`payment:write`). |
| Role | Named set of scopes assigned to a user (`CUSTOMER`, `OPS`, `ADMIN`). |

## User

| Term | Definition |
|---|---|
| User | A customer of the platform (profile, status). |
| KYC Tier | `UNVERIFIED`, `BASIC`, `FULL`; determines transaction limits. |
| Account Status | `ACTIVE`, `FROZEN`, `CLOSED`. |

## Wallet

| Term | Definition |
|---|---|
| Wallet | A customer-facing container of funds in one currency, mapped to exactly one ledger account. |
| Available Balance | Posted balance minus pending debits, as computed by the ledger. The wallet stores a projection. |
| Limit Usage | Running totals of daily/monthly amounts per wallet used for limit checks. |

## Ledger

| Term | Definition |
|---|---|
| Ledger Account | An account in the chart of accounts (asset, liability, revenue, expense). |
| Journal Entry | An immutable, balanced set of postings describing one money movement. |
| Posting | One debit or credit line of a journal entry. |
| Transfer | A two-phase money movement: `PENDING → POSTED | VOIDED | EXPIRED`. |
| Reserve | Create a `PENDING` transfer that holds funds. |
| Post | Finalize a transfer by writing its journal entry. |
| Void | Cancel a pending transfer and release held funds. |
| Suspense | Liability account for money received but not yet attributable. |
| PSP Receivable | Asset account for amounts a PSP owes us before settlement. |

## Payment

| Term | Definition |
|---|---|
| Payment | The customer-visible intent to move money (top-up, transfer, merchant payment, refund, withdrawal). |
| Top-up | External money in: PSP → user wallet. |
| Saga | A multi-step process managed by `payment-service` with persisted state and compensations. |
| Saga Instance | Persisted record of a saga's current step and state. |
| Requires Review | Payment state where money arrived but could not be credited; parked in Suspense. |

## Gateway

| Term | Definition |
|---|---|
| PSP | Payment Service Provider (MoMo, VNPay, Mock). |
| Payment Intent | Request to a PSP to collect a payment; returns a pay URL and PSP transaction id. |
| IPN / Webhook | PSP callback reporting a transaction result. |
| Webhook Receipt | Stored raw webhook (body, headers, signature result) for audit and replay. |

## Merchant

| Term | Definition |
|---|---|
| Merchant | A business accepting wallet payments through the Merchant API. |
| API Credential | Merchant API key + secret used for HMAC request signing. |
| Merchant Order | A merchant's request for payment from a user. |
| Capture / Void | Finalize or cancel a held merchant payment. |

## Notification

| Term | Definition |
|---|---|
| Notification | Message to a user via email or in-app channel. |
| Webhook Delivery | An attempt to deliver an event to a merchant's webhook URL, with retry history. |

## Reconciliation

| Term | Definition |
|---|---|
| Settlement Report | PSP file listing settled transactions and fees for a period. |
| Break | A mismatch between the settlement report and internal records. |
