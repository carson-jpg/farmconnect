# FarmConnect payment system

Every successful produce sale is split **5% FarmConnect / 5% County Government of Trans Nzoia / 90% Farmer** (rates in
`application.properties`, farmer = remainder). Collection, allocation, ledger and payout are separate steps.

## Flow
1. Buyer starts M-Pesa payment (`POST /api/payments/mpesa/{orderId}/stk`). Amount comes from the order on the server. A `payments` row is saved as PENDING.
2. Safaricom calls back (STK `/api/payments/callback/{secret}`, or C2B `/api/payments/c2b/confirmation/{secret}` for direct till payments with account ref `FC<orderId>`).
3. `PaymentService` verifies receipt, amount and duplicates, then **in one database transaction** marks the payment SUCCESS, writes the ledger and creates the three allocations. Repeat callbacks change nothing (row lock + unique receipt).
4. When the order is DELIVERED and `payments.hold-hours` has passed, allocations become ELIGIBLE and a payout is queued, provided a **verified** payout account exists.
5. `PayoutWorker` (every minute) sends B2C (farmer phone) or B2B (county paybill/till). The allocation becomes SETTLED only when Safaricom's result says success.
6. Nightly (and on demand) reconciliation checks the books; admins fix what it flags.

## Money rules worth knowing
* Shares are worked out in **whole shillings** (M-Pesa only moves whole shillings); the farmer gets the remainder, so the three always equal the sale exactly. Orders with several farmers split the farmer pool by each farmer's produce subtotal.
* Commission base = produce price of the order (no delivery fee exists yet). M-Pesa fees are carried by FarmConnect, not deducted from farmer or county.
* FarmConnect's 5% is already in your till, so it is "paid" by a ledger entry (`RETAINED`), not a transfer.
* Underpayment, overpayment, duplicate payment: excess is recorded as owed to the buyer and refunded by an admin. Underpaid orders are not allocated.
* Cash sales do not go through this collection flow. They have their own approval and commission-bill flow (see "Cash payments" at the end).

## Safety against double payment
Payout is claimed with one atomic UPDATE and committed **before** anything is sent; each attempt has its own `OriginatorConversationID`.
If the outcome is unclear (timeout / 5xx / no result in 30 min) the payout becomes **UNKNOWN** and is never retried automatically. An admin checks the M-Pesa statement, then either `mark-paid` or `retry` with `confirmNotSent=true`.

## Statuses
Payment: PENDING, SUCCESS, FAILED, REVERSED, NEEDS_REVIEW | Allocation: RECORDED, ELIGIBLE, SETTLING, SETTLED, ON_HOLD, REVERSED | Payout: QUEUED, PROCESSING, SUCCESS, FAILED, UNKNOWN, CANCELLED.
Your existing order statuses are unchanged (PENDING..DELIVERED); `orders.paymentStatus` still says PAID/FAILED/REFUNDED.

## Admin API (ADMIN role) `/api/admin/payments`
`GET /` · `GET /{id}` (payment+allocations+payouts+ledger) · `POST /{id}/confirm` · `POST /{id}/refund` · `POST /{id}/refund-excess` ·
`GET /payouts` · `POST /payouts/{id}/retry|mark-paid` · `POST /allocations/{id}/hold|release` ·
`GET /accounts` + `POST /accounts/{id}/approve|reject` · `GET /summary` · `GET /reconciliation` · `POST /reconciliation/statement`.
Farmer API: `GET /api/payments/farmer/earnings`, `GET|PUT /api/payments/farmer/account`.

## Screens
* **Buyer (Android):** checkout now has **"Pay now with M-Pesa (in the app)"**. After the order is placed, `PayNowActivity` sends the M-Pesa prompt, shows "Check your phone", checks the server every 4 s for up to 2 min, then shows the receipt (or lets the buyer retry with another number). Unpaid in-app orders show a **Pay now** button on the order page. The only other option is **Cash (recorded in the app)**, described under "Cash payments". The old Cash on delivery and M-Pesa on delivery options were removed.
* **Farmer (Android):** "My earnings and payouts" link on the farmer dashboard opens `EarningsActivity`: paid vs on-the-way totals, each sale's state, and the payout phone number (a new number needs admin approval).
* **Admin (web, `/officer/#payments`, ADMIN role only):** totals per beneficiary, a "Needs attention" list (payments to review, refunds owed, unknown/failed payouts, payout numbers to approve), all payments and payouts with filters, a payment detail view (split, transfers, full ledger) with hold / release / refund, payout-account approval, and reconciliation including a paste-in M-Pesa statement comparison.
* Order responses now include `paymentStatus` and `mpesaReceipt`.

## Before going live (things only you can do)
1. **Rotate the secrets committed in `application.properties`** (JWT, encryption key, admin password, M-Pesa key/secret/passkey) and move them to environment variables.
2. B2C needs a Safaricom **paybill with B2C enabled** plus an initiator and security credential. A Buy-Goods till cannot send B2C. Set `mpesa.initiator.*`.
3. Set `payments.county.account-type/reference` once the County gives its destination. Until then the county share is recorded and held.
4. Register the C2B URLs with Safaricom if you want direct till payments; get the contract/legal approval for splitting funds on behalf of the County.
5. Test in the Daraja sandbox end to end (STK, B2C result, B2C timeout, duplicate callback) before production. This code has not been run against Safaricom.

## Farm-input (supply) payments

Companies sell farm inputs to farmers (Phase 2). Money for those orders follows its own, separate path so the produce
payments above are never touched. It is OFF until you set `supply.payments.enabled=true`.

- **Collect after delivery.** A farmer can pay by M-Pesa only once the order is DELIVERED, so no money is held for goods
  that were not handed over. The amount always comes from the server (order total rounded up to whole shillings).
- **Same safety rules as produce:** payment row locked, receipt must be new (checked against produce receipts too),
  underpaid / duplicate / no-receipt payments go to NEEDS_REVIEW, and the collection, the split and the ledger lines are
  written in one database transaction.
- **Split:** the rate in force when the order was placed (Commission tab). Platform and county shares are worked out in
  whole shillings; the company gets the remainder. The county share is only recorded (`PAYABLE:COUNTY:SUPPLY`); settle it by hand.
- **Company payout:** the company's share is sent by B2C (phone) or B2B (paybill/till) to the payout account the
  super admin approved. Same rules as produce payouts: atomic claim, origin id saved before sending, unclear outcomes become
  UNKNOWN and are never retried automatically.
- **Overpayment, duplicates and refunds are recorded, not sent.** Excess is booked as `PAYABLE:BUYER:{id}`; refund it by hand
  from the admin screen's list of payments needing attention.
- **Admin (super admin only):** `GET /api/admin/supply-payments/payments`, `.../payouts`, `POST .../payments/{id}/confirm`,
  `POST .../payouts/{id}/requeue`, `POST .../payouts/{id}/mark-paid`, `GET .../ledger-check` (must say `balanced: true`).
- **Callbacks:** farm-input payments reuse your existing STK and B2C result/timeout URLs. Supply payouts use an
  originator id starting `SUP-`, so results are routed to the right place automatically.


## Cash payments (recorded in the app, approved by the super admin)

Every sale must be in the app. When the buyer pays the farmer in cash:

1. **Farmer records it** (order page -> Cash payment). The amount is the farmer's produce on that order, fixed by the server.
2. **Buyer confirms or disputes it** in the app.
3. **Super admin approves or rejects it** (web console -> Cash approvals). The admin can approve even if the buyer has not answered, and sees any dispute.
4. **On approval** the order becomes PAID (once every farmer on the order is approved) and a **commission bill** is created:
   platform share + county share (5% + 5% by default, the `payments.platform-bps` / `payments.county-bps` values, worked out in whole shillings).
   The farmer keeps the rest, which was already received in cash.
5. **The farmer pays the bill by M-Pesa in the app** (STK push; the shared callback URL routes it to the bill). The admin can also mark a bill paid by hand (for example if the money came straight to the till).
6. A farmer whose bill is unpaid for more than `app.cash.max-overdue-days` (7) cannot record new cash sales.

An order cannot be marked DELIVERED until it is PAID (M-Pesa received, or every cash sale approved).

Commission bills are tracked in `commission_bills` and shown in the Cash approvals page (owed / received). They are not part of the M-Pesa ledger tables
or the reconciliation report; the platform share reaches your M-Pesa account when the farmer pays the bill, and the county share is settled by you from that total, as for the other county shares.
