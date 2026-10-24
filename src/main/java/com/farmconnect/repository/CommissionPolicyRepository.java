package com.farmconnect.repository;

import com.farmconnect.model.CommissionPolicy;
import com.farmconnect.model.CommissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {
    List<CommissionPolicy> findByTypeOrderByEffectiveFromDesc(CommissionType type);

    @Query("select p from CommissionPolicy p where p.type = :type and p.effectiveFrom <= :now "
            + "and (p.effectiveTo is null or p.effectiveTo > :now) order by p.effectiveFrom desc")
    List<CommissionPolicy> inForce(@Param("type") CommissionType type, @Param("now") Instant now);

    default Optional<CommissionPolicy> current(CommissionType type, Instant now) {
        return inForce(type, now).stream().findFirst();
    }
}
