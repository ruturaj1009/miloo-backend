package com.miloo.common.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {

    @JsonProperty("object_key")
    private String objectKey;

    @JsonProperty("public_url")
    private String publicUrl;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("size_bytes")
    private Long sizeBytes;

    @JsonProperty("original_filename")
    private String originalFilename;
}
