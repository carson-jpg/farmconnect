package com.farmconnect.model;

/** PLACED -> CONFIRMED -> DISPATCHED -> DELIVERED. The farmer can cancel only while PLACED; the company can reject while PLACED or CONFIRMED. */
public enum SupplyOrderStatus { PLACED, CONFIRMED, DISPATCHED, DELIVERED, CANCELLED, REJECTED }
