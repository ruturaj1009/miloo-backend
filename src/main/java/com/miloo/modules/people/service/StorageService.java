package com.miloo.modules.people.service;

import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Presigner s3Presigner;

    @Value("${app.storage.r2.bucket-name:miloo-media}")
    private String bucketName;

    @Value("${app.storage.r2.public-domain:https://pub-media.example.com}")
    private String publicDomain;

    @Value("${app.storage.r2.mock-enabled:true}")
    private boolean mockEnabled;

    public PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType) {
        String cleanFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = String.format("users/%s/%d_%s", userId, System.currentTimeMillis(), cleanFilename);

        if (mockEnabled) {
            String mockUploadUrl = String.format("https://r2-mock.storage.cloudflare.com/%s", objectKey);
            String mockPublicUrl = String.format("%s/%s", publicDomain, objectKey);
            log.info("[Storage Mock] Generated presigned upload URL for key: {}", objectKey);
            return PresignedUrlResponseDto.builder()
                    .uploadUrl(mockUploadUrl)
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(mockPublicUrl)
                    .build();
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presigned.url().toString();
            String publicUrl = String.format("%s/%s", publicDomain, objectKey);

            return PresignedUrlResponseDto.builder()
                    .uploadUrl(uploadUrl)
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(publicUrl)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to generate presigned R2 upload URL", ex);
            // Fallback gracefully to mock URL so user experience is not broken
            return PresignedUrlResponseDto.builder()
                    .uploadUrl(String.format("https://r2-mock.storage.cloudflare.com/%s", objectKey))
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(String.format("%s/%s", publicDomain, objectKey))
                    .build();
        }
    }
}
