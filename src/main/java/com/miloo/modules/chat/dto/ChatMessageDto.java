package com.miloo.modules.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    @JsonProperty("message_id")
    private UUID messageId;

    @JsonProperty("match_id")
    private UUID matchId;

    @JsonProperty("sender_id")
    private UUID senderId;

    @JsonProperty("recipient_id")
    private UUID recipientId;

    private String content;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("created_at")
    private String createdAt;
}
