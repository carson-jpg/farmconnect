package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class GroupMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FarmerGroup group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    private boolean leader;
    private Instant joinedAt = Instant.now();
}
