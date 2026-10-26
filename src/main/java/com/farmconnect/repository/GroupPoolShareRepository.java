package com.farmconnect.repository;

import com.farmconnect.model.GroupPoolShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupPoolShareRepository extends JpaRepository<GroupPoolShare, Long> {
    List<GroupPoolShare> findByPoolIdOrderByUpdatedAtAsc(Long poolId);
    List<GroupPoolShare> findByPoolIdIn(Collection<Long> poolIds);
    Optional<GroupPoolShare> findByPoolIdAndUserId(Long poolId, Long userId);
    void deleteByPoolGroupId(Long groupId);
}
