package com.miloo.modules.interaction.dto;

import com.miloo.modules.people.dto.UserProfileDto;
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
public class MatchSummaryDto {

    @JsonProperty("match_id")
    private UUID matchId;

    @JsonProperty("user_one_id")
    private UUID userOneId;

    @JsonProperty("user_two_id")
    private UUID userTwoId;

    @JsonProperty("partner_profile")
    private UserProfileDto partnerProfile;

    @JsonProperty("last_message")
    private String lastMessage;

    @JsonProperty("last_message_at")
    private String lastMessageAt;

    @JsonProperty("unread_count")
    private Integer unreadCount;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private String createdAt;
}
