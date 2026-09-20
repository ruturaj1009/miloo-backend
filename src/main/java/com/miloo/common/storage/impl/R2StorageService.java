package com.miloo.common.storage.impl;

import com.miloo.common.storage.StorageProperties;
import com.miloo.common.storage.StorageProvider;
import com.miloo.common.storage.StorageService;
import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service("r2StorageService")
@RequiredArgsConstructor
public class R2StorageService implements StorageService {

    private final StorageProperties properties;
    private final S3Presigner r2Presigner;
    private final S3Client r2S3Client;

    @Override
    public PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType) {
        String cleanFilename = (filename != null ? filename : "upload.jpg").replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = String.format("users/%s/%d_%s", userId, System.currentTimeMillis(), cleanFilename);
        String publicDomain = properties.getR2().getPublicDomain();
        String bucketName = properties.getR2().getBucketName();

        if (properties.isMockEnabled() || isDummyKey(properties.getR2().getAccessKey())) {
            String mockUploadUrl = String.format("https://r2-mock.storage.cloudflare.com/%s", objectKey);
            String mockPublicUrl = String.format("%s/%s", publicDomain, objectKey);
            log.info("[R2 Mock] Generated presigned upload URL for key: {}", objectKey);
            return PresignedUrlResponseDto.builder()
                    .uploadUrl(mockUploadUrl)
                    .objectKey(objectKey)
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(mockPublicUrl)
                    .provider(StorageProvider.R2.name())
                    .build();
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType != null ? contentType : "image/jpeg")
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presigned = r2Presigner.presignPutObject(presignRequest);
            String uploadUrl = presigned.url().toString();
            String publicUrl = String.format("%s/%s", publicDomain, objectKey);

            return PresignedUrlResponseDto.builder()
                    .uploadUrl(uploadUrl)
                    .objectKey(objectKey)
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(publicUrl)
                    .provider(StorageProvider.R2.name())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to generate presigned R2 upload URL for key {}: {}", objectKey, ex.getMessage());
            return PresignedUrlResponseDto.builder()
                    .uploadUrl(String.format("https://r2-mock.storage.cloudflare.com/%s", objectKey))
                    .objectKey(objectKey)
                    .r2ObjectKey(objectKey)
                    .publicMediaUrl(String.format("%s/%s", publicDomain, objectKey))
                    .provider(StorageProvider.R2.name())
                    .build();
        }
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, MultipartFile file) {
        try {
            return uploadFile(userId, file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes for upload", e);
        }
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, byte[] data, String filename, String contentType) {
        String cleanFilename = (filename != null ? filename : "upload.jpg").replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = String.format("users/%s/%d_%s", userId, System.currentTimeMillis(), cleanFilename);
        String publicDomain = properties.getR2().getPublicDomain();
        String bucketName = properties.getR2().getBucketName();
        String publicUrl = String.format("%s/%s", publicDomain, objectKey);

        if (!properties.isMockEnabled() && !isDummyKey(properties.getR2().getAccessKey()) && r2S3Client != null) {
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType != null ? contentType : "image/jpeg")
                        .contentLength((long) data.length)
                        .build();

                r2S3Client.putObject(putRequest, RequestBody.fromBytes(data));
                log.info("[R2] Uploaded file successfully to {}", objectKey);
            } catch (Exception e) {
                log.error("Failed to upload file to R2: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload to Cloudflare R2", e);
            }
        } else {
            log.info("[R2 Mock] Simulating direct upload for key: {}", objectKey);
        }

        return FileUploadResponseDto.builder()
                .objectKey(objectKey)
                .publicUrl(publicUrl)
                .provider(StorageProvider.R2.name())
                .contentType(contentType)
                .sizeBytes((long) data.length)
                .originalFilename(cleanFilename)
                .build();
    }

    @Override
    public void deleteFile(String objectKey) {
        if (!properties.isMockEnabled() && !isDummyKey(properties.getR2().getAccessKey()) && r2S3Client != null) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(properties.getR2().getBucketName())
                        .key(objectKey)
                        .build();
                r2S3Client.deleteObject(deleteRequest);
                log.info("[R2] Deleted object: {}", objectKey);
            } catch (Exception e) {
                log.error("Failed to delete object from R2: {}", e.getMessage());
            }
        } else {
            log.info("[R2 Mock] Simulating file deletion for: {}", objectKey);
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return String.format("%s/%s", properties.getR2().getPublicDomain(), objectKey);
    }

    @Override
    public StorageProvider getProviderType() {
        return StorageProvider.R2;
    }

    private boolean isDummyKey(String key) {
        return key == null || key.isBlank() || key.contains("dummy");
    }
}
