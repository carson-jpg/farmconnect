package com.farmconnect.model;

/** ISSUED (company accepted a quotation) -> CONFIRMED (farmer agrees) -> DELIVERED (company received the produce). */
public enum PurchaseOrderStatus { ISSUED, CONFIRMED, DECLINED, DELIVERED, CANCELLED }
