package com.miloo.modules.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions", schema = "chat_ctx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "caller_id", nullable = false)
    private UUID callerId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Builder.Default
    @Column(name = "call_type", nullable = false, length = 20)
    private String callType = "VIDEO"; // AUDIO, VIDEO

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "INITIATED";

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
