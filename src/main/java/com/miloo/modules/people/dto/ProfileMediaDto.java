package com.miloo.modules.people.dto;

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
public class ProfileMediaDto {

    @JsonProperty("media_id")
    private UUID mediaId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("r2_object_key")
    private String r2ObjectKey;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("created_at")
    private String createdAt;
}
