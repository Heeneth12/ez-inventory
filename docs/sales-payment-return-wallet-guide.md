# Sales Module Guide: Payments, Sales Return, Wallet, and MB Methods

This guide explains how **Sales Return**, **Payment**, and **Wallet** currently work in this project.

---

## 1) What happens when you create a Sales Return?

When you create Sales Return (`POST /v1/sales/return`):

1. System validates invoice and returned items.
2. System checks returned quantity is not more than remaining quantity.
3. System adds returned stock back to inventory (`MovementType.IN`, `ReferenceType.SALES_RETURN`).
4. System calculates refund amount using invoice item unit price.
5. System auto-creates a **Credit Note payment record** for customer wallet.

Important: It does **not** directly send cash/bank refund to customer. It creates wallet credit via Credit Note.

---

## 2) How is refund money stored after Sales Return?

After Sales Return, this API flow is called internally:

- `paymentService.createCreditNote(customerId, amount, returnRefNumber)`

That creates a new `Payment` row with:

- `paymentMethod = CREDIT_NOTE`
- `status = RECEIVED`
- `amount = return amount`
- `allocatedAmount = 0`
- `unallocatedAmount = full return amount`

So, return money is added as **customer wallet balance** (unallocated credit).

---

## 3) How to use returned money (wallet credit)?

You have 2 choices:

### A) Use it against another invoice (recommended)

Use:

- `POST /v1/payment/wallet/apply`

Payload example:

```json
{
  "customerId": 101,
  "invoiceId": 5001,
  "amount": 1500
}
```

Behavior:

- System collects all unallocated credits of that customer.
- Applies by FIFO (oldest credit first).
- Creates `PaymentAllocation` to invoice.
- Reduces wallet `unallocatedAmount`.

### B) Give money back to customer (cash/bank outside system)

Use:

- `POST /v1/payment/wallet/refund/{paymentId}?amount=...`

Behavior currently:

- Only reduces `unallocatedAmount` of that payment record.
- Adds text in remarks (`| Refunded: X`).
- Does not create separate payout voucher entity.

Operational note: If you physically return cash/MB transfer, record reference details in remarks/reference fields for audit.

---

## 4) How to add advance money to wallet?

Use:

- `POST /v1/payment/wallet/add`

Payload example:

```json
{
  "customerId": 101,
  "amount": 2000,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN-7788",
  "remarks": "Advance from customer"
}
```

This creates an ADV payment entry with full `unallocatedAmount`, so it can be used later for invoices.

---

## 5) MB (Mobile Banking) usage for Payment and Sales Return

There is no exact enum value named `MB`.
Use one of these methods based on your meaning:

- `MOBILE_WALLET` → if payment came from wallet apps.
- `NET_BANKING` or `BANK_TRANSFER` → if payment came from online banking transfer.
- `CREDIT_NOTE` → only for auto credit from Sales Return.

### For normal payment via MB

Use `POST /v1/payment` with:

- `paymentMethod` = `MOBILE_WALLET` / `NET_BANKING` / `BANK_TRANSFER`
- provide transaction ID in `referenceNumber`

### For sales return + MB refund

Current best-practice in this code:

1. Create sales return (system creates CREDIT_NOTE wallet amount).
2. If you want to settle by MB transfer to customer, call wallet refund endpoint.
3. Store MB transaction ID in remarks/reference (manual audit trail).

---

## 6) Quick end-to-end scenarios

### Scenario 1: Return and keep credit for next purchase

1. Invoice paid: 5000
2. Customer returns goods worth: 1200
3. Sales return created → credit note wallet +1200
4. Next invoice 3000 → apply wallet 1200
5. Customer pays only 1800

### Scenario 2: Return and immediate payout to customer

1. Customer returns goods worth: 700
2. Sales return created → wallet +700
3. Finance sends 700 by cash/MB to customer
4. Call wallet refund endpoint for same amount
5. Wallet balance becomes 0 for that credit

---

## 7) APIs involved (reference)

- `POST /v1/sales/return` → Create sales return
- `POST /v1/payment` → Record direct payment (can include allocations)
- `POST /v1/payment/wallet/add` → Add advance money to wallet
- `POST /v1/payment/wallet/apply` → Apply wallet to invoice
- `POST /v1/payment/wallet/refund/{paymentId}?amount=` → Mark wallet amount refunded
- `GET /v1/payment/summary/customer/{customerId}` → Get outstanding + wallet balance

---

## 8) Important current limitation

The wallet refund endpoint currently updates only the existing payment record (`unallocatedAmount` + remarks). It does not create a dedicated outbound refund transaction table/document.

If you need strict accounting/audit, consider adding a separate Refund Voucher entity in future.

---

## 9) End-to-end sanity check checklist (Sales + Payment + Return)

Use this once in QA/UAT to verify complete workflow:

1. Create invoice with 2 line items and complete payment via `POST /v1/payment`.
2. Verify invoice payment summary shows paid and balance is correct.
3. Create partial sales return via `POST /v1/sales/return`.
4. Verify stock increased for returned batch and ledger reference is Sales Return ID.
5. Verify auto credit note is created and appears as customer wallet balance.
6. Apply wallet credit to another unpaid invoice via `POST /v1/payment/wallet/apply`.
7. Verify invoice balance reduced and payment allocation entry created.
8. Add wallet advance via `POST /v1/payment/wallet/add` and verify unallocated amount increases.
9. Refund part of wallet via `POST /v1/payment/wallet/refund/{paymentId}` and verify unallocated amount decreases.
10. Re-check customer summary (`GET /v1/payment/summary/customer/{customerId}`) and confirm totals match expected.

If all 10 steps pass with expected balances/stock/allocations, sales payment-return workflow is healthy.
