package com.miloo.modules.chat.service;

import com.miloo.modules.chat.dto.SignalingPayloadDto;
import com.miloo.modules.chat.dto.TypingStatusDto;
import com.miloo.modules.chat.entity.CallSessionEntity;
import com.miloo.modules.chat.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CallSessionRepository callSessionRepository;

    public void routeSignalingPayload(SignalingPayloadDto payload) {
        log.info("[WebRTC Signaling] Routing type '{}' from {} to {}",
                payload.getType(), payload.getSenderId(), payload.getTargetId());

        // Forward to target user's STOMP queue
        messagingTemplate.convertAndSendToUser(
                payload.getTargetId(),
                "/queue/signaling",
                payload
        );
    }

    public void routeTypingIndicator(TypingStatusDto typingStatus, String targetUserId) {
        messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/typing",
                typingStatus
        );
    }

    @Transactional
    public void handleCallAction(SignalingPayloadDto payload) {
        log.info("[Call Action] Action '{}' for session '{}'", payload.getType(), payload.getSessionId());

        try {
            if ("CALL_INVITE".equalsIgnoreCase(payload.getType())) {
                CallSessionEntity session = CallSessionEntity.builder()
                        .matchId(UUID.fromString(payload.getSessionId())) // or derived matchId
                        .callerId(UUID.fromString(payload.getSenderId()))
                        .receiverId(UUID.fromString(payload.getTargetId()))
                        .callType(payload.getCallType() != null ? payload.getCallType() : "VIDEO")
                        .status("CALLING")
                        .startedAt(Instant.now())
                        .build();
                callSessionRepository.save(session);
            } else if ("CALL_ACCEPT".equalsIgnoreCase(payload.getType())) {
                try {
                    UUID sId = UUID.fromString(payload.getSessionId());
                    callSessionRepository.findById(sId).ifPresent(s -> {
                        s.setStatus("CONNECTED");
                        s.setStartedAt(Instant.now());
                        callSessionRepository.save(s);
                    });
                } catch (Exception ignored) {}
            } else if ("CALL_END".equalsIgnoreCase(payload.getType()) || "CALL_REJECT".equalsIgnoreCase(payload.getType())) {
                try {
                    UUID sId = UUID.fromString(payload.getSessionId());
                    callSessionRepository.findById(sId).ifPresent(s -> {
                        s.setStatus("ENDED");
                        s.setEndedAt(Instant.now());
                        callSessionRepository.save(s);
                    });
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Could not persist call session state: {}", e.getMessage());
        }

        // Forward action to target peer
        messagingTemplate.convertAndSendToUser(
                payload.getTargetId(),
                "/queue/signaling",
                payload
        );
    }
}
