package com.farmconnect.repository;

import com.farmconnect.model.Company;
import com.farmconnect.model.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwnerId(Long ownerId);
    Optional<Company> findByKraPinHash(String hash);
    Optional<Company> findByBusinessRegNumberIgnoreCase(String number);
    List<Company> findByStatusOrderBySubmittedAtAsc(CompanyStatus status);
    long countByStatus(CompanyStatus status);
}
