package com.farmconnect.config;

import com.farmconnect.payment.model.AccountStatus;
import com.farmconnect.payment.model.AccountType;
import com.farmconnect.payment.model.BeneficiaryType;
import com.farmconnect.payment.service.BeneficiaryService;
import com.farmconnect.payment.service.PaymentPolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the FarmConnect and County payout accounts from application.properties at startup. Operator-configured,
 * so they are trusted (VERIFIED). Farmer accounts are created automatically when their first payout is due.
 *
 *   payments.platform.account-type=RETAINED          (default: your 5% simply stays in the till)
 *   payments.county.account-type=PAYBILL             (PAYBILL | TILL | MPESA_PHONE | BANK)
 *   payments.county.account-reference=123456         (paybill/till number, phone, or bank reference)
 *   payments.county.account-number=TRANSNZOIA        (PAYBILL only: the account number to credit)
 * Until the county account is set, the county's 5% is recorded and held, never lost.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class PaymentSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PaymentSeeder.class);
    private final BeneficiaryService accounts;
    private final PaymentPolicy policy;

    @Value("${payments.platform.account-type:RETAINED}") private String platformType;
    @Value("${payments.platform.account-reference:FARMCONNECT_TILL}") private String platformRef;
    @Value("${payments.platform.account-number:}") private String platformNumber;
    @Value("${payments.county.account-type:}") private String countyType;
    @Value("${payments.county.account-reference:}") private String countyRef;
    @Value("${payments.county.account-number:}") private String countyNumber;

    @Override public void run(String... args) {
        seed(BeneficiaryType.PLATFORM, "FARMCONNECT", platformType, platformRef, platformNumber);
        if (countyType.isBlank()) log.warn("payments.county.account-type is not set: the county's share will be recorded but not paid out yet");
        else seed(BeneficiaryType.COUNTY, policy.getCountyName(), countyType, countyRef, countyNumber);
    }

    private void seed(BeneficiaryType who, String id, String type, String ref, String number) {
        try {
            AccountType t = AccountType.valueOf(type.trim().toUpperCase());
            if (t != AccountType.RETAINED && ref.isBlank()) { log.warn("{} payout account has no reference; skipped", who); return; }
            accounts.save(who, id, t, ref.isBlank() ? null : ref.trim(), number.isBlank() ? null : number.trim(), AccountStatus.VERIFIED);
        } catch (IllegalArgumentException e) {
            log.error("Unknown account type '{}' for {}. Use PAYBILL, TILL, MPESA_PHONE, BANK or RETAINED", type, who);
        }
    }
}
