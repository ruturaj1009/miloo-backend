package com.miloo.modules.people.service;

import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Legacy bridge service delegating to the centralized {@link com.miloo.common.storage.StorageService}.
 * Retained for backward compatibility with existing module callers.
 */
@Service("peopleStorageServiceLegacy")
@RequiredArgsConstructor
public class StorageService {

    private final com.miloo.common.storage.StorageService storageService;

    public PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType) {
        return storageService.generateUploadUrl(userId, filename, contentType);
    }
}
