package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.BeneficiaryAccount;
import com.farmconnect.payment.model.BeneficiaryType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BeneficiaryAccountRepository extends JpaRepository<BeneficiaryAccount, Long> {
    Optional<BeneficiaryAccount> findByBeneficiaryTypeAndBeneficiaryId(BeneficiaryType t, String id);
    List<BeneficiaryAccount> findByVerificationStatus(com.farmconnect.payment.model.AccountStatus s);
}
