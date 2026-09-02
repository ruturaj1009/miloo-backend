package com.miloo.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_embeddings", schema = "ai_ctx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmbeddingEntity {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "interest_tags", columnDefinition = "TEXT[]")
    private String[] interestTags;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private Instant lastUpdated;
}
