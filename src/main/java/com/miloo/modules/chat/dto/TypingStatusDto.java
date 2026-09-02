package com.miloo.modules.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingStatusDto {
    private String matchId;
    private String userId;
    private Boolean isTyping;
}
