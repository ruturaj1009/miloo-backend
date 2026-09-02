package com.miloo.modules.people.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import com.miloo.modules.people.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final StorageService storageService;

    @GetMapping("/upload-url")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUploadUrl(
            @RequestParam(value = "filename", defaultValue = "photo.jpg") String filename,
            @RequestParam(value = "contentType", defaultValue = "image/jpeg") String contentType
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        PresignedUrlResponseDto response = storageService.generateUploadUrl(currentUserId, filename, contentType);
        return ResponseEntity.ok(response);
    }
}
