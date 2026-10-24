package com.farmconnect.model;

public enum CompanyDocType {
    /** Certificate of incorporation / business registration. Required before verification. */
    REGISTRATION_CERT,
    /** KRA PIN certificate. Required before verification. */
    KRA_PIN_CERT,
    TRADE_LICENCE,
    /** Sector licence, e.g. KEPHIS for seed, PCPB for pesticides, NTSA for transport. */
    SECTOR_LICENCE,
    REP_ID,
    OTHER
}
