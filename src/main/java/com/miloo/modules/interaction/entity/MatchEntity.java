package com.miloo.modules.interaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches", schema = "interaction_ctx",
        uniqueConstraints = @UniqueConstraint(name = "uq_match_pair", columnNames = {"user_one_id", "user_two_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "match_id", updatable = false, nullable = false)
    private UUID matchId;

    @Column(name = "user_one_id", nullable = false)
    private UUID userOneId;

    @Column(name = "user_two_id", nullable = false)
    private UUID userTwoId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
