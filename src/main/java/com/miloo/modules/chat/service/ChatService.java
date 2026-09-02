package com.miloo.modules.chat.service;

import com.miloo.common.exception.ApiException;
import com.miloo.modules.chat.dto.ChatMessageDto;
import com.miloo.modules.chat.dto.ConversationDto;
import com.miloo.modules.chat.dto.SendMessageRequestDto;
import com.miloo.modules.chat.entity.MessageEntity;
import com.miloo.modules.chat.repository.MessageRepository;
import com.miloo.modules.interaction.entity.MatchEntity;
import com.miloo.modules.interaction.repository.MatchRepository;
import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final ProfileService profileService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(ZoneId.systemDefault());

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(UUID currentUserId) {
        List<MatchEntity> matches = matchRepository.findActiveMatchesByUserId(currentUserId);

        return matches.stream().map(m -> {
            UUID partnerId = m.getUserOneId().equals(currentUserId) ? m.getUserTwoId() : m.getUserOneId();
            UserProfileDto partner = profileService.getProfileById(partnerId, currentUserId);

            Optional<MessageEntity> latestMsg = messageRepository.findTopByMatchIdOrderByCreatedAtDesc(m.getMatchId());
            long unread = messageRepository.countByMatchIdAndRecipientIdAndIsReadFalse(m.getMatchId(), currentUserId);

            String partnerAvatar = (partner.getPhotos() != null && !partner.getPhotos().isEmpty())
                    ? partner.getPhotos().get(0).getMediaUrl()
                    : "";

            return ConversationDto.builder()
                    .matchId(m.getMatchId())
                    .partnerId(partnerId)
                    .partnerName(partner.getFirstName())
                    .partnerAvatar(partnerAvatar)
                    .isOnline(true)
                    .isVerified(partner.getIsVerified())
                    .lastMessage(latestMsg.map(MessageEntity::getContent).orElse("New match! Say hello."))
                    .lastMessageTime(latestMsg.map(msg -> TIME_FORMAT.format(msg.getCreatedAt())).orElse(null))
                    .unreadCount((int) unread)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(UUID currentUserId, UUID matchId, int page, int size) {
        verifyMatchParticipation(matchId, currentUserId);

        int querySize = (size > 0) ? size : 50;
        int queryPage = (page >= 0) ? page : 0;

        List<MessageEntity> entities = messageRepository.findByMatchIdOrderByCreatedAtDesc(
                matchId, PageRequest.of(queryPage, querySize)
        );

        // Reverse to chronological order (earliest to latest) for chat UI thread
        return entities.stream()
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendMessage(UUID currentUserId, SendMessageRequestDto req) {
        verifyMatchParticipation(req.getMatchId(), currentUserId);

        MessageEntity message = MessageEntity.builder()
                .matchId(req.getMatchId())
                .senderId(currentUserId)
                .recipientId(req.getRecipientId())
                .content(req.getContent())
                .mediaUrl(req.getMediaUrl())
                .isRead(false)
                .build();

        MessageEntity saved = messageRepository.save(message);
        ChatMessageDto dto = toDto(saved);

        // Push real-time event to recipient and sender via STOMP /user/queue/messages
        try {
            messagingTemplate.convertAndSendToUser(req.getRecipientId().toString(), "/queue/messages", dto);
            messagingTemplate.convertAndSendToUser(currentUserId.toString(), "/queue/messages", dto);
        } catch (Exception ex) {
            log.warn("Could not dispatch STOMP chat message: {}", ex.getMessage());
        }

        return dto;
    }

    @Transactional
    public int markAsRead(UUID currentUserId, UUID matchId) {
        verifyMatchParticipation(matchId, currentUserId);
        return messageRepository.markMessagesAsRead(matchId, currentUserId);
    }

    private void verifyMatchParticipation(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Match not found"));
        if (!match.getUserOneId().equals(userId) && !match.getUserTwoId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not a participant in this conversation");
        }
    }

    public ChatMessageDto toDto(MessageEntity entity) {
        return ChatMessageDto.builder()
                .messageId(entity.getMessageId())
                .matchId(entity.getMatchId())
                .senderId(entity.getSenderId())
                .recipientId(entity.getRecipientId())
                .content(entity.getContent())
                .mediaUrl(entity.getMediaUrl())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt() != null ? TIME_FORMAT.format(entity.getCreatedAt()) : null)
                .build();
    }
}
