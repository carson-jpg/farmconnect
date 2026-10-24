package com.farmconnect.model;

/** REQUESTED -> ACCEPTED -> COMPLETED. The farmer can cancel while REQUESTED or ACCEPTED. */
public enum ServiceRequestStatus { REQUESTED, ACCEPTED, DECLINED, COMPLETED, CANCELLED }
