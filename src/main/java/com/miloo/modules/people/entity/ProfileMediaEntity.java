package com.miloo.modules.people.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_media", schema = "people_ctx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_id", updatable = false, nullable = false)
    private UUID mediaId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "r2_object_key", nullable = false, length = 512)
    private String r2ObjectKey;

    @Column(name = "media_url", nullable = false, length = 1024)
    private String mediaUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
