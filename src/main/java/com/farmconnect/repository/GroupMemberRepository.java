package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupIdOrderByLeaderDescJoinedAtAsc(Long groupId);
    List<GroupMember> findByUserId(Long userId);
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}
