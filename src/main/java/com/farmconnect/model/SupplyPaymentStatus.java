package com.farmconnect.model;

/** NEEDS_REVIEW: money may have arrived but cannot be safely allocated (underpaid, no receipt, duplicate receipt, lost callback). */
public enum SupplyPaymentStatus { PENDING, SUCCESS, FAILED, NEEDS_REVIEW }
