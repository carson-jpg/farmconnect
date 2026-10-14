package com.farmconnect.payment.model;

/**
 * NEEDS_REVIEW = money may have arrived but we cannot safely allocate it (paid less than the order total, no receipt
 * number, a receipt we already saw, or STK-query says "paid" but the callback never came). An admin resolves it.
 */
public enum PaymentStatus { PENDING, SUCCESS, FAILED, REVERSED, NEEDS_REVIEW }
