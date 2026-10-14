package com.farmconnect.payment.model;

/**
 * Where a beneficiary's money goes.
 * MPESA_PHONE -> B2C, PAYBILL / TILL -> B2B, BANK -> paid by finance outside the app and confirmed by an admin,
 * RETAINED -> already sits in FarmConnect's own till (the platform share): settled by a ledger entry, no transfer.
 */
public enum AccountType { MPESA_PHONE, PAYBILL, TILL, BANK, RETAINED }
