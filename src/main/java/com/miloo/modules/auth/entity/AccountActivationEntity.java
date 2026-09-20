package com.miloo.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_activations", schema = "auth_ctx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "activation_id", updatable = false, nullable = false)
    private UUID activationId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
