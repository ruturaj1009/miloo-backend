package com.miloo.modules.chat.controller;

import com.miloo.modules.chat.dto.SendMessageRequestDto;
import com.miloo.modules.chat.dto.SignalingPayloadDto;
import com.miloo.modules.chat.dto.TypingStatusDto;
import com.miloo.modules.chat.service.ChatService;
import com.miloo.modules.chat.service.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignalingSocketController {

    private final ChatService chatService;
    private final SignalingService signalingService;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload SendMessageRequestDto payload, Principal principal) {
        if (principal == null) {
            log.warn("[STOMP] Unauthenticated attempt to send chat message");
            return;
        }
        UUID senderId = UUID.fromString(principal.getName());
        chatService.sendMessage(senderId, payload);
    }

    @MessageMapping("/chat.typing")
    public void handleTypingStatus(@Payload TypingStatusDto payload, Principal principal) {
        if (principal == null) return;
        payload.setUserId(principal.getName());
        signalingService.routeTypingIndicator(payload, payload.getMatchId());
    }

    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload SignalingPayloadDto payload, Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        signalingService.routeSignalingPayload(payload);
    }

    @MessageMapping("/call.action")
    public void handleCallAction(@Payload SignalingPayloadDto payload, Principal principal) {
        if (principal != null) {
            payload.setSenderId(principal.getName());
        }
        signalingService.handleCallAction(payload);
    }
}
