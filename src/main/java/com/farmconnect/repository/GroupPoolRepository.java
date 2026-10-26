package com.farmconnect.repository;

import com.farmconnect.model.GroupPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GroupPoolRepository extends JpaRepository<GroupPool, Long> {
    List<GroupPool> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    List<GroupPool> findByGroupIdInOrderByCreatedAtDesc(Collection<Long> groupIds);
    List<GroupPool> findTop200ByKindAndStatusOrderByCreatedAtDesc(String kind, String status);
    void deleteByGroupId(Long groupId);
}
