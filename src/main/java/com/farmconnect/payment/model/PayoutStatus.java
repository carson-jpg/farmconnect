package com.farmconnect.payment.model;

/** UNKNOWN = we asked Safaricom but never learned the outcome. Never retried automatically (could double-pay). */
public enum PayoutStatus { QUEUED, PROCESSING, SUCCESS, FAILED, UNKNOWN, CANCELLED }
