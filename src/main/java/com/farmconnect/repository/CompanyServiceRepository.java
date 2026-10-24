package com.farmconnect.repository;

import com.farmconnect.model.CompanyServiceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CompanyServiceRepository extends JpaRepository<CompanyServiceListing, Long> {
    List<CompanyServiceListing> findByCompanyIdOrderByNameAsc(Long companyId);

    @Query("select s from CompanyServiceListing s where s.active = true "
            + "and s.company.status = com.farmconnect.model.CompanyStatus.VERIFIED and lower(s.name) like lower(:like) order by s.name")
    List<CompanyServiceListing> browse(@Param("like") String like);
}
