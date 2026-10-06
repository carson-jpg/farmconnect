package com.farmconnect.repository;

import com.farmconnect.model.FarmerVerification;
import com.farmconnect.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FarmerVerificationRepository extends JpaRepository<FarmerVerification, Long> {
    Optional<FarmerVerification> findByFarmerId(Long farmerId);
    boolean existsByNationalIdHashAndFarmerIdNot(String hash, Long farmerId);
    boolean existsByPhoneAndPhoneVerifiedTrueAndFarmerIdNot(String phone, Long farmerId);
    List<FarmerVerification> findByStatusOrderBySubmittedAtAsc(VerificationStatus status);
}