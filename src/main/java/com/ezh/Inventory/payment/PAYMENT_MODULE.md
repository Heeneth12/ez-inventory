# Payment Module — EZ Inventory

> **Audience:** LLMs, backend developers, product reviewers.
> **Last updated:** 2026-04-12

---

## Core Design Principle

Three separate concepts, three separate tables. They never mix.

| Concept | Table | Document | What it is |
|---|---|---|---|
| **Payment** | `payment` | `PAY-` | Revenue — cash received for specific invoices |
| **Advance** | `customer_advance` | `ADV-` | Liability — customer deposits money before invoice |
| **Credit Note** | `credit_note` | `CN-` | Liability — value owed after a sales return |

**Refunds** always have their own records too:

| Concept | Table | Document |
|---|---|---|
| Advance Refund | `advance_refund` | `RFD-` |
| Credit Note Refund | `credit_note_refund` | `RFD-` |

---

## 1. Payment (Revenue)

**Rule:** `payment.amount == sum(allocations.allocatedAmount)` — always exactly equal.
A payment is always fully allocated at creation. No excess, no wallet concept here.

```
payment
├── paymentNumber     PAY-2025-00001
├── customerId
├── paymentDate
├── amount            ← immutable after CONFIRMED. Must equal sum of allocations.
├── paymentMethod     CASH | UPI | CHEQUE | BANK_TRANSFER | RAZOR_PAY
├── status            DRAFT → CONFIRMED → CANCELLED
├── referenceNumber   Cheque no / UTR / Razorpay ID
└── allocations[]
      └── payment_allocation
            ├── payment  → Payment
            ├── invoiceId (Long FK — clean, no lazy load overhead)
            ├── allocatedAmount
            └── allocationDate
```

**Lifecycle:** DRAFT → CONFIRMED → CANCELLED

---

## 2. CustomerAdvance (Liability)

Customer deposits money before any invoice is raised. Business owes goods or a refund.

```
customer_advance
├── advanceNumber     ADV-2025-00001
├── customerId
├── receivedDate
├── amount            ← immutable after CONFIRMED (what was received)
├── availableBalance  ← decreases on utilization or refund
├── paymentMethod
├── status            DRAFT → CONFIRMED → PARTIALLY_UTILIZED → FULLY_UTILIZED | REFUNDED | CANCELLED
└── utilizations[]    → AdvanceUtilization (one per invoice application)
    refunds[]         → AdvanceRefund (one per refund event)
```

**Invariant:** `amount = sum(CONFIRMED utilizations) + sum(CLEARED refunds) + availableBalance`

### AdvanceUtilization

Created when advance balance is applied to an invoice.

```
advance_utilization
├── advance  → CustomerAdvance
├── invoiceId
├── utilizedAmount
├── utilizationDate
└── status            CONFIRMED | REVERSED
```

REVERSED when invoice is cancelled — advance balance is restored.

### AdvanceRefund

Created when unused advance is returned as cash to the customer.

```
advance_refund
├── refundNumber      RFD-2025-00001
├── advance  → CustomerAdvance
├── refundAmount
├── refundDate
├── refundMethod      CASH | CHEQUE | BANK_TRANSFER | UPI
├── refundReferenceNumber
└── status            PENDING → CLEARED | CANCELLED
```

PENDING = initiated. CLEARED = cash confirmed received by customer.

---

## 3. CreditNote (Liability)

Auto-created when a SalesReturn is confirmed. No cash comes in — business owes the value.

```
credit_note
├── creditNoteNumber  CN-2025-00001
├── customerId
├── sourceReturnId    ← always traceable to the SalesReturn that created it
├── issueDate
├── amount            ← immutable after ISSUED
├── availableBalance  ← decreases on utilization or refund
├── status            ISSUED → PARTIALLY_UTILIZED → FULLY_UTILIZED | REFUNDED | CANCELLED
└── utilizations[]    → CreditNoteUtilization
    refunds[]         → CreditNoteRefund
```

Structure of CreditNoteUtilization and CreditNoteRefund mirrors Advance exactly.

---

## 4. API Endpoints

### Payment — `POST/GET /v1/payment`

| Method | Path | Purpose |
|---|---|---|
| POST | `/v1/payment` | Record payment — allocations sum must equal total |
| POST | `/v1/payment/all` | List payments (paginated + filtered) |
| GET | `/v1/payment?paymentId=` | Single payment detail |
| GET | `/v1/payment/{invoiceId}` | Payment history for invoice |
| GET | `/v1/payment/invoice/{invoiceId}/summary` | Full invoice payment summary |
| GET | `/v1/payment/summary/customer/{customerId}` | Customer financial summary |
| POST | `/v1/payment/stats` | Aggregate stats |
| GET | `/v1/payment/{paymentId}/pdf` | Receipt PDF |

### Advance — `/v1/advance`

| Method | Path | Purpose |
|---|---|---|
| POST | `/v1/advance` | Record new advance |
| POST | `/v1/advance/utilize` | Apply advance to invoice |
| POST | `/v1/advance/refund` | Initiate cash refund (PENDING) |
| PATCH | `/v1/advance/refund/{id}/confirm` | Mark refund CLEARED |
| GET | `/v1/advance/{advanceId}` | Advance detail with full history |
| GET | `/v1/advance/customer/{customerId}` | All advances for customer |

### Credit Note — `/v1/credit-note`

| Method | Path | Purpose |
|---|---|---|
| POST | `/v1/credit-note/utilize` | Apply CN to invoice |
| POST | `/v1/credit-note/refund` | Initiate CN refund as cash (PENDING) |
| PATCH | `/v1/credit-note/refund/{id}/confirm` | Mark CN refund CLEARED |
| GET | `/v1/credit-note/{creditNoteId}` | CN detail with full history |
| GET | `/v1/credit-note/customer/{customerId}` | All CNs for customer |

---

## 5. Customer Financial Summary

`GET /v1/payment/summary/customer/{customerId}` returns:

```json
{
  "customerId": 42,
  "totalOutstandingAmount": 15000,
  "advanceBalance": 5000,
  "creditNoteBalance": 2000,
  "totalCreditAvailable": 7000
}
```

`totalCreditAvailable` = `advanceBalance + creditNoteBalance` — computed field, not stored.

---

## 6. Enums

| Enum | Values |
|---|---|
| `PaymentStatus` | DRAFT, CONFIRMED, CANCELLED |
| `AdvanceStatus` | DRAFT, CONFIRMED, PARTIALLY_UTILIZED, FULLY_UTILIZED, REFUNDED, CANCELLED |
| `CreditNoteStatus` | ISSUED, PARTIALLY_UTILIZED, FULLY_UTILIZED, REFUNDED, CANCELLED |
| `UtilizationStatus` | CONFIRMED, REVERSED |
| `RefundStatus` | PENDING, CLEARED, CANCELLED |
| `PaymentMethod` | CASH, UPI, CHEQUE, BANK_TRANSFER, CREDIT_CARD, NET_BANKING, RAZOR_PAY, ... |

---

## 7. Key Questions — Answered

| Question | Where to look |
|---|---|
| "Did customer pay invoice INV-001?" | `payment_allocation` where `invoice_id = INV-001` |
| "How much advance does customer X have?" | `CustomerAdvance.availableBalance` sum via `GET /v1/advance/customer/{id}` |
| "Which invoice consumed advance ADV-001?" | `AdvanceUtilization` where `advance_id = ADV-001` |
| "Was advance ADV-001 refunded? When?" | `AdvanceRefund` where `advance_id = ADV-001` |
| "Which sales return created CN-001?" | `CreditNote.sourceReturnId` |
| "What's the customer's total credit?" | `GET /v1/payment/summary/customer/{id}` → `totalCreditAvailable` |
