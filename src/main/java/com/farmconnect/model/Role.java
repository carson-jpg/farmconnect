package com.farmconnect.model;

/**
 * ADMIN is the super administrator (whole platform). COUNTY_ADMIN manages one county, assigned by the super admin.
 * OFFICER is a county agricultural officer. FARMER and BUYER are the marketplace users.
 */
public enum Role { FARMER, BUYER, ADMIN, OFFICER, COUNTY_ADMIN }
