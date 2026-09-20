package com.miloo.common.storage;

import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Primary routing facade for StorageService.
 * Automatically delegates all calls to the active provider selected in application.yml.
 */
@Slf4j
@Primary
@Service("storageService")
@RequiredArgsConstructor
public class StorageServiceRouter implements StorageService {

    private final StorageServiceFactory factory;

    private StorageService getDelegate() {
        return factory.getActiveService();
    }

    @Override
    public PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType) {
        return getDelegate().generateUploadUrl(userId, filename, contentType);
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, MultipartFile file) {
        return getDelegate().uploadFile(userId, file);
    }

    @Override
    public FileUploadResponseDto uploadFile(UUID userId, byte[] data, String filename, String contentType) {
        return getDelegate().uploadFile(userId, data, filename, contentType);
    }

    @Override
    public void deleteFile(String objectKey) {
        getDelegate().deleteFile(objectKey);
    }

    @Override
    public String getPublicUrl(String objectKey) {
        return getDelegate().getPublicUrl(objectKey);
    }

    @Override
    public StorageProvider getProviderType() {
        return getDelegate().getProviderType();
    }
}
