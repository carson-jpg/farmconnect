package com.farmconnect.repository;

import com.farmconnect.model.CompanyDocType;
import com.farmconnect.model.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {
    List<CompanyDocument> findByCompanyIdOrderByUploadedAtAsc(Long companyId);
    long countByCompanyId(Long companyId);
    boolean existsByCompanyIdAndType(Long companyId, CompanyDocType type);
}
