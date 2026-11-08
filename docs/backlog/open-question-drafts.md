# Open question drafts

| | |
|---|---|
| Status | Draft |
| Owner | huylq |
| Last updated | 2026-11-08 |
| Source | [delivery-plan.md](./delivery-plan.md) Open questions |

Proposed answers only. Not decisions until copied into the named service design and that design is Accepted.
When the blocking feature moves to Ready, promote the section below into the file in the "Write the answer" column, then mark the question Resolved here.

---

## Q1 — Transfer posting shape

| | |
|---|---|
| Blocks | FT-020, FT-030, FT-058 (answer before coding: FT-018) |
| Write the answer | `architecture/services/ledger-service.md` |
| Promote when | EP-005 / FT-018 |

### Proposed answer

A `Transfer` is one journal entry with **N ≥ 2 balanced legs**, single currency. Two-legged transfers are the N = 2 case. There is no separate single-pair transfer API and no "batch of paired transfers".

### Why

- HLD §6.2 already requires every journal entry to have ≥ 2 postings and Σ debits = Σ credits. The domain model is multi-leg; only the sketched `transfer` table and API were pair-shaped.
- Fee and principal must post or void together. Pairing transfers still needs a batch id for atomicity and invents a half-posted state. That batch is a multi-leg transfer under another name.
- Reserve for P2P fixture: one hold of 51,000 on the sender, not two holds of 50,000 and 1,000 fighting for the same `available`.
- Settlement FT-058 needs `Dr BANK_SETTLEMENT 99,000 / Dr PSP_FEE_EXPENSE 1,000 / Cr PSP_RECEIVABLE 100,000`. Pairing would fake-split the credit.
- Wallet projection consumes one `TransferPosted` per payment and filters legs for its accounts.

### Design notes to land in ledger-service.md

- Schema: drop `debit_account_id, credit_account_id, amount_minor` from `transfer`; add `transfer_leg(transfer_id, seq, account_id, direction, amount_minor)`. `posting` stays append-only and is created from legs on post.
- Reserve invariant: ≥ 2 legs, balanced, one currency, amounts > 0, hard cap on leg count (e.g. 16) to bound lock cost.
- Reserve: for each debit leg on a liability account, check `available ≥ amount` and increase `pending_debit`. Optimistic-lock every touched account in ascending id order.
- API: `POST /v1/transfers { idempotencyKey, currency, legs:[{accountCode, direction, amountMinor}], mode: RESERVE | POST_IMMEDIATELY, expiresInSeconds, reference }`.
- Events `TransferPosted` / `TransferVoided` carry a `legs` array (additive within the version).
- Rejected alternative: paired transfers + batch id.

---

## Q2 — MoMo and VNPay sandbox signature facts

| | |
|---|---|
| Blocks | FT-042, FT-043 |
| Write the answer | `architecture/services/gateway-adapter-service.md` |
| Promote when | EP-011 |

### Proposed answer

Record provider facts under a **Provider facts** section, dated and linked to the sandbox docs used that day. Algorithms live in adapter code; IP allowlists and credentials live in env-specific config. Re-verify against live sandbox docs at the start of FT-042 / FT-043 before coding.

### MoMo (AIO v2 — `developers.momo.vn/v2`)

- HMAC-SHA256, hex, key = `secretKey`. Raw string is a fixed per-endpoint `key=value&…` template, not a generic sort. `accessKey` is signed from config, not from the body.
- IPN: POST JSON to `ipnUrl`. Signed field order: `accessKey, amount, extraData, message, orderId, orderInfo, orderType, partnerCode, payType, requestId, responseTime, resultCode, transId`. Empty optionals still emit `&extraData=`.
- `resultCode = 0` success; `9000` authorization only (`autoCapture=false`, not used for top-up); anything else failure.
- Status query: `/v2/gateway/api/query`, sign `accessKey, orderId, partnerCode, requestId`.
- MoMo ignores IPN response body; return 204.
- Caveat: MoMo sample repos disagree on IPN field order. Pin the v2 list above, allow config override, confirm with one real sandbox IPN before trusting.
- IP ranges: not published as a stable allowlist in MoMo docs. Do **not** put Kong `ip-restriction` on `/webhooks/momo`; signature is the gate.

### VNPay (v2.1.0 — `sandbox.vnpayment.vn`)

- HMAC-SHA512, hex, key = `vnp_HashSecret`. Sign all `vnp_*` params sorted by key, URL-encode values (space as `+`), join `key=value&`, exclude `vnp_SecureHash` and `vnp_SecureHashType`.
- IPN: GET with query string to the IPN URL configured in the sandbox portal. Main fields: `vnp_TmnCode, vnp_Amount, vnp_BankCode, vnp_BankTranNo, vnp_CardType, vnp_PayDate, vnp_OrderInfo, vnp_TransactionNo, vnp_ResponseCode, vnp_TransactionStatus, vnp_TxnRef, vnp_SecureHash`.
- Success when `vnp_ResponseCode = 00` **and** `vnp_TransactionStatus = 00`.
- Must respond JSON `{RspCode, Message}`: `00` recorded, `02` already recorded, `01` order not found, `04` amount mismatch, `97` bad signature, `99` other. `00`/`02` stop retries; other codes or timeout → up to 10 retries every 5 minutes. Inbox dedupe must answer `02` for duplicates.
- `vnp_Amount` is ×100. `vnp_TxnRef` unique per day → use `paymentId`. `vnp_CreateDate` / `vnp_ExpireDate` in GMT+7.
- IP: sandbox `113.160.92.202`. Production list comes from VNPay during SIT. Store in `kong.yml` env vars; disabled locally.

### Design notes to land in gateway-adapter-service.md

- `WebhookReceipt` stores original bytes first (JSON body for MoMo, raw query string for VNPay) before parse. Sign over those bytes; never re-serialize.
- Constant-time signature compare. Failures increment `webhook_signature_failures_total{provider}`.
- Provider result codes map to neutral `SUCCEEDED | FAILED | PENDING` inside the adapter.

### Sources checked (2026-11-08)

- MoMo: https://developers.momo.vn/v2/
- VNPay: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- Re-check both before coding FT-042 / FT-043.

---

## Q3 — Who exposes SUSPENSE → wallet ops action

| | |
|---|---|
| Blocks | FT-059 |
| Write the answer | `architecture/services/payment-service.md` |
| Promote when | EP-015 / FT-059 |

### Proposed answer

**Keep the planning assumption.** `payment-service` exposes the ops action under scope `ops:review`. `ledger-service` only posts; it has no ops endpoint for this flow.

### Why

- Ops is resolving a `Payment` in `REQUIRES_REVIEW`. Completing that payment is `payment-service`'s state machine; the ledger does not know why money sits in `SUSPENSE`.
- Audit needs actor, payment id, and reason. `payment-service` already owns `SagaInstance`.
- Preserves the HLD Customer–Supplier boundary: ledger exposes a general transfer API only.

### Design notes to land in payment-service.md

- API: `POST /v1/payments/{paymentId}/review` body `{ action: RELEASE_TO_WALLET, reason }`, scope `ops:review`, required `Idempotency-Key`. Enum may reserve `REFUND_TO_PSP` but that is out of FT-059 scope.
- Posting: `Dr SUSPENSE / Cr USER_WALLET:<walletId>` via `POST_IMMEDIATELY`, `idempotencyKey = paymentId:suspense-release`.
- If the wallet is still frozen, ledger rejects again and Payment stays `REQUIRES_REVIEW`. `payment-service` does not unfreeze; ops unfreezes via `wallet-service` first.
- Persist `payment_review_action(payment_id, action, reason, actor_sub, created_at)` and emit `PaymentReviewResolved` via outbox.
- Boundary (do not reverse casually): payment-linked `SUSPENSE` goes through `payment-service`. Unmatched settlement `SUSPENSE` (EP-015 break with no Payment) is a manual ledger posting called by `reconciliation-service`, also under `ops:review`. The two paths do not share an endpoint.

---

## Checklist when promoting

1. Copy the proposed answer into the service design named above.
2. Update HLD snippets that still show pair-shaped `transfer` columns if Q1 is Accepted (schema / API sections).
3. Mark the question Resolved in [delivery-plan.md](./delivery-plan.md) Open questions (or strike the row and link the service design).
4. Mark the matching section here `Promoted` with the date and service-design link.
