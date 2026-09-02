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
public class ConversationDto {

    @JsonProperty("match_id")
    private UUID matchId;

    @JsonProperty("partner_id")
    private UUID partnerId;

    @JsonProperty("partner_name")
    private String partnerName;

    @JsonProperty("partner_avatar")
    private String partnerAvatar;

    @JsonProperty("is_online")
    private Boolean isOnline;

    @JsonProperty("last_seen")
    private String lastSeen;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    @JsonProperty("last_message")
    private String lastMessage;

    @JsonProperty("last_message_time")
    private String lastMessageTime;

    @JsonProperty("unread_count")
    private Integer unreadCount;
}
