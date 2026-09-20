package com.miloo.common.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.miloo.common.storage.StorageProperties;
import com.miloo.common.storage.StorageProvider;
import com.miloo.common.storage.StorageService;
import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("cloudinaryStorageService")
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private final StorageProperties properties;
    private final Cloudinary cloudinary;

    @Override
    public PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType) {
        String cleanFilename = (filename != null ? filename : "upload.jpg").replaceAll("[^a-zA-Z0-9._-]", "_");
        String publicId = String.format("%d_%s", System.currentTimeMillis(), cleanFilename.replaceFirst("[.][^.]+$", ""));
        String folder = String.format("%s/%s", properties.getCloudinary().getFolder(), userId);
        String fullPublicId = String.format("%s/%s", folder, publicId);

        String cloudName = properties.getCloudinary().getCloudName();
        String apiKey = properties.getCloudinary().getApiKey();
        String apiSecret = properties.getCloudinary().getApiSecret();

        if (properties.isMockEnabled() || isDummyKey(apiKey)) {
            String mockUploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName);
            String mockPublicUrl = String.format("https://res.cloudinary.com/%s/image/upload/v1/%s", cloudName, fullPublicId);
            log.info("[Cloudinary Mock] Generated signed upload URL for id: {}", fullPublicId);

            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("api_key", "mock_key");
            additionalParams.put("timestamp", System.currentTimeMillis() / 1000L);
            additionalParams.put("public_id", publicId);
            additionalParams.put("folder", folder);
            additionalParams.put("signature", "mock_signature");

            return PresignedUrlResponseDto.builder()
                    .uploadUrl(mockUploadUrl)
                    .objectKey(fullPublicId)
                    .r2ObjectKey(fullPublicId)
                    .publicMediaUrl(mockPublicUrl)
                    .provider(StorageProvider.CLOUDINARY.name())
                    .additionalParams(additionalParams)
                    .build();
        }

        try {
            long timestamp = System.currentTimeMillis() / 1000L;
            Map<String, Object> paramsToSign = new HashMap<>();
            paramsToSign.put("folder", folder);
            paramsToSign.put("public_id", publicId);
            paramsToSign.put("timestamp", timestamp);

            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);
            String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName);
            String publicUrl = cloudinary.url().secure(true).generate(fullPublicId);

            Map<String, Object> additionalParams = new HashMap<>(paramsToSign);
            additionalParams.put("api_key", apiKey);
            additionalParams.put("signature", signature);

            return PresignedUrlResponseDto.builder()
                    .uploadUrl(uploadUrl)
                    .objectKey(fullPublicId)
                    .r2ObjectKey(fullPublicId)
                    .publicMediaUrl(publicUrl)
                    .provider(StorageProvider.CLOUDINARY.name())
                    .additionalParams(additionalParams)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to generate Cloudinary upload signature for {}: {}", fullPublicId, ex.getMessage());
            String mockUploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName);
            String mockPublicUrl = String.format("https://res.cloudinary.com/%s/image/upload/v1/%s", cloudName, fullPublicId);
            return PresignedUrlResponseDto.builder()
                    .uploadUrl(mockUploadUrl)
                    .objectKey(fullPublicId)
                    .r2ObjectKey(fullPublicId)
                    .publicMediaUrl(mockPublicUrl)
                    .provider(StorageProvider.CLOUDINARY.name())
                    .build();
        }
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, MultipartFile file) {
        try {
            return uploadFile(userId, file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes for Cloudinary upload", e);
        }
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, byte[] data, String filename, String contentType) {
        String cleanFilename = (filename != null ? filename : "upload.jpg").replaceAll("[^a-zA-Z0-9._-]", "_");
        String publicId = String.format("%d_%s", System.currentTimeMillis(), cleanFilename.replaceFirst("[.][^.]+$", ""));
        String folder = String.format("%s/%s", properties.getCloudinary().getFolder(), userId);
        String fullPublicId = String.format("%s/%s", folder, publicId);

        String publicUrl;
        if (!properties.isMockEnabled() && !isDummyKey(properties.getCloudinary().getApiKey()) && cloudinary != null) {
            try {
                @SuppressWarnings("rawtypes")
                Map uploadResult = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                        "public_id", publicId,
                        "folder", folder,
                        "resource_type", "auto"
                ));
                publicUrl = (String) uploadResult.get("secure_url");
                if (uploadResult.containsKey("public_id")) {
                    fullPublicId = (String) uploadResult.get("public_id");
                }
                log.info("[Cloudinary] Uploaded file successfully to {}", fullPublicId);
            } catch (Exception e) {
                log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload to Cloudinary", e);
            }
        } else {
            publicUrl = String.format("https://res.cloudinary.com/%s/image/upload/v1/%s",
                    properties.getCloudinary().getCloudName(), fullPublicId);
            log.info("[Cloudinary Mock] Simulating upload for key: {}", fullPublicId);
        }

        return FileUploadResponseDto.builder()
                .objectKey(fullPublicId)
                .publicUrl(publicUrl)
                .provider(StorageProvider.CLOUDINARY.name())
                .contentType(contentType)
                .sizeBytes((long) data.length)
                .originalFilename(cleanFilename)
                .build();
    }

    @Override
    public void deleteFile(String objectKey) {
        if (!properties.isMockEnabled() && !isDummyKey(properties.getCloudinary().getApiKey()) && cloudinary != null) {
            try {
                cloudinary.uploader().destroy(objectKey, ObjectUtils.emptyMap());
                log.info("[Cloudinary] Destroyed resource: {}", objectKey);
            } catch (Exception e) {
                log.error("Failed to delete resource from Cloudinary: {}", e.getMessage());
            }
        } else {
            log.info("[Cloudinary Mock] Simulating resource deletion for: {}", objectKey);
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        if (!properties.isMockEnabled() && !isDummyKey(properties.getCloudinary().getApiKey()) && cloudinary != null) {
            return cloudinary.url().secure(true).generate(objectKey);
        }
        return String.format("https://res.cloudinary.com/%s/image/upload/v1/%s",
                properties.getCloudinary().getCloudName(), objectKey);
    }

    @Override
    public StorageProvider getProviderType() {
        return StorageProvider.CLOUDINARY;
    }

    private boolean isDummyKey(String key) {
        return key == null || key.isBlank() || key.contains("dummy");
    }
}
