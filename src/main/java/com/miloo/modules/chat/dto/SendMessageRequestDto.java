package com.miloo.modules.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDto {

    @NotNull(message = "match_id is required")
    @JsonProperty("match_id")
    private UUID matchId;

    @NotNull(message = "recipient_id is required")
    @JsonProperty("recipient_id")
    private UUID recipientId;

    @NotBlank(message = "content cannot be empty")
    private String content;

    @JsonProperty("media_url")
    private String mediaUrl;
}
