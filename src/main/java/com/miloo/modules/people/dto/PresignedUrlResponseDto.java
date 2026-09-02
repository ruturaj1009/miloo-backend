package com.miloo.modules.people.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponseDto {

    @JsonProperty("upload_url")
    private String uploadUrl;

    @JsonProperty("r2_object_key")
    private String r2ObjectKey;

    @JsonProperty("public_media_url")
    private String publicMediaUrl;
}
