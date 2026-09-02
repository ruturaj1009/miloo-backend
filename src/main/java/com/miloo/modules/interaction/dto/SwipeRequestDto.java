package com.miloo.modules.interaction.dto;

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
public class SwipeRequestDto {

    @NotNull(message = "target_user_id is required")
    @JsonProperty("target_user_id")
    private UUID targetUserId;

    @NotBlank(message = "action is required")
    @JsonProperty("action")
    private String action; // LIKE, PASS, SUPERLIKE
}
