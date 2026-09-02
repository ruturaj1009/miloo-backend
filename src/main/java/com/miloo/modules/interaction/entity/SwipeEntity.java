package com.miloo.modules.interaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "swipes", schema = "interaction_ctx",
        uniqueConstraints = @UniqueConstraint(name = "uq_swiper_target", columnNames = {"swiper_id", "target_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "swipe_id", updatable = false, nullable = false)
    private UUID swipeId;

    @Column(name = "swiper_id", nullable = false)
    private UUID swiperId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // LIKE, PASS, SUPERLIKE

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
