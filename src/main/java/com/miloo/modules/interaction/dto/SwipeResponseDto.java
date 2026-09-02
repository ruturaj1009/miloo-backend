package com.miloo.modules.interaction.dto;

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
public class SwipeResponseDto {

    @JsonProperty("swipe_id")
    private UUID swipeId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("is_match")
    private Boolean isMatch;

    @JsonProperty("match")
    private MatchSummaryDto match;
}
