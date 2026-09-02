package com.miloo.modules.chat.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.chat.dto.ChatMessageDto;
import com.miloo.modules.chat.dto.ConversationDto;
import com.miloo.modules.chat.dto.SendMessageRequestDto;
import com.miloo.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<ConversationDto> conversations = chatService.getConversations(currentUserId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{matchId}/history")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @PathVariable("matchId") UUID matchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<ChatMessageDto> history = chatService.getChatHistory(currentUserId, matchId, page, size);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{matchId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable("matchId") UUID matchId,
            @Valid @RequestBody SendMessageRequestDto request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        request.setMatchId(matchId);
        ChatMessageDto message = chatService.sendMessage(currentUserId, request);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{matchId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("matchId") UUID matchId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        int marked = chatService.markAsRead(currentUserId, matchId);
        return ResponseEntity.ok(Map.of("marked_count", marked));
    }
}
