package com.farmconnect.payment.service;

import com.farmconnect.model.User;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.BeneficiaryAccountRepository;
import com.farmconnect.repository.FarmerVerificationRepository;
import com.farmconnect.service.MpesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Who may be paid, and where. Money is only ever sent to a VERIFIED + active account. */
@Service
@RequiredArgsConstructor
public class BeneficiaryService {
    private final BeneficiaryAccountRepository accounts;
    private final FarmerVerificationRepository verifications;

    /** The account to pay, or empty if there is none that is verified and active. */
    @Transactional
    public Optional<BeneficiaryAccount> payable(BeneficiaryType type, String id) {
        return accounts.findByBeneficiaryTypeAndBeneficiaryId(type, id)
                .filter(a -> a.isActive() && a.getVerificationStatus() == AccountStatus.VERIFIED);
    }

    /**
     * A farmer's payout account. Created the first time it is needed from the phone the farmer proved with an SMS code
     * (-> VERIFIED automatically). Without such a phone it falls back to the profile phone as PENDING for an admin to approve.
     */
    @Transactional
    public Optional<BeneficiaryAccount> farmerAccount(User farmer) {
        String id = String.valueOf(farmer.getId());
        Optional<BeneficiaryAccount> existing = accounts.findByBeneficiaryTypeAndBeneficiaryId(BeneficiaryType.FARMER, id);
        if (existing.isPresent()) return existing;
        var v = verifications.findByFarmerId(farmer.getId());
        String phone = null;
        boolean proven = false;
        if (v.isPresent() && v.get().isPhoneVerified() && v.get().getPhone() != null) { phone = v.get().getPhone(); proven = true; }
        else if (farmer.getPhone() != null && !farmer.getPhone().isBlank()) phone = farmer.getPhone();
        if (phone == null) return Optional.empty();
        try { phone = MpesaService.normalizePhone(phone); } catch (Exception e) { return Optional.empty(); }
        return Optional.of(save(BeneficiaryType.FARMER, id, AccountType.MPESA_PHONE, phone, null,
                proven ? AccountStatus.VERIFIED : AccountStatus.PENDING));
    }

    /** Farmer asks to be paid to another number. It must be approved by an admin before it is used. */
    @Transactional
    public BeneficiaryAccount changeFarmerPhone(User farmer, String rawPhone) {
        String phone = MpesaService.normalizePhone(rawPhone);
        String id = String.valueOf(farmer.getId());
        var v = verifications.findByFarmerId(farmer.getId());
        boolean sameAsProven = v.isPresent() && v.get().isPhoneVerified() && phone.equals(v.get().getPhone());
        return save(BeneficiaryType.FARMER, id, AccountType.MPESA_PHONE, phone, null,
                sameAsProven ? AccountStatus.VERIFIED : AccountStatus.PENDING);
    }

    @Transactional
    public BeneficiaryAccount save(BeneficiaryType type, String id, AccountType accountType, String reference, String number, AccountStatus status) {
        BeneficiaryAccount a = accounts.findByBeneficiaryTypeAndBeneficiaryId(type, id).orElseGet(BeneficiaryAccount::new);
        a.setBeneficiaryType(type);
        a.setBeneficiaryId(id);
        a.setAccountType(accountType);
        a.setAccountReference(reference);
        a.setAccountNumber(number);
        a.setVerificationStatus(status);
        a.setVerifiedAt(status == AccountStatus.VERIFIED ? Instant.now() : null);
        a.setActive(true);
        a.setUpdatedAt(Instant.now());
        return accounts.save(a);
    }

    @Transactional
    public BeneficiaryAccount review(Long accountId, boolean approve) {
        BeneficiaryAccount a = accounts.findById(accountId).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));
        a.setVerificationStatus(approve ? AccountStatus.VERIFIED : AccountStatus.REJECTED);
        a.setVerifiedAt(approve ? Instant.now() : null);
        a.setUpdatedAt(Instant.now());
        return accounts.save(a);
    }
}
