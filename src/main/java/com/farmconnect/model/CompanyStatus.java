package com.farmconnect.model;

/** Only VERIFIED companies get the privileges that need verification. Only the super admin changes this. */
public enum CompanyStatus { PENDING_VERIFICATION, UNDER_REVIEW, VERIFIED, REJECTED, SUSPENDED }
