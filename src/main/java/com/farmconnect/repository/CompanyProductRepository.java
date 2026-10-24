package com.farmconnect.repository;

import com.farmconnect.model.CompanyProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CompanyProductRepository extends JpaRepository<CompanyProduct, Long> {
    List<CompanyProduct> findByCompanyIdOrderByNameAsc(Long companyId);

    /** What farmers can see: active listings of VERIFIED companies. like = "%text%" (use "%" for everything). */
    @Query("select p from CompanyProduct p where p.active = true "
            + "and p.company.status = com.farmconnect.model.CompanyStatus.VERIFIED "
            + "and lower(p.name) like lower(:like) order by p.name")
    List<CompanyProduct> browse(@Param("like") String like);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CompanyProduct p where p.id = :id")
    Optional<CompanyProduct> lockById(@Param("id") Long id);
}
