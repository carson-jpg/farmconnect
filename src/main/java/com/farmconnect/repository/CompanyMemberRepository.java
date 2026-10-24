package com.farmconnect.repository;

import com.farmconnect.model.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {
    List<CompanyMember> findByCompanyIdOrderByNameAsc(Long companyId);
    long countByCompanyId(Long companyId);
}
