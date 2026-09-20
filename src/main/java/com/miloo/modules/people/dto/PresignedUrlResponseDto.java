package com.miloo.modules.people.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresignedUrlResponseDto {

    @JsonProperty("upload_url")
    private String uploadUrl;

    /**
     * Backward-compatible key name for clients expecting R2 key
     */
    @JsonProperty("r2_object_key")
    private String r2ObjectKey;

    /**
     * Standard provider-agnostic object key or Cloudinary public_id
     */
    @JsonProperty("object_key")
    private String objectKey;

    @JsonProperty("public_media_url")
    private String publicMediaUrl;

    @JsonProperty("provider")
    private String provider;

    /**
     * Optional provider-specific parameters (e.g. signature, api_key, timestamp for Cloudinary direct client uploads)
     */
    @JsonProperty("additional_params")
    private Map<String, Object> additionalParams;
}
