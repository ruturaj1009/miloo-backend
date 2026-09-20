package com.miloo.modules.people.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.common.storage.StorageService;
import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDto> uploadDirectFile(
            @RequestParam("file") MultipartFile file
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        FileUploadResponseDto response = storageService.uploadFile(currentUserId, file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMedia(
            @RequestParam("objectKey") String objectKey
    ) {
        storageService.deleteFile(objectKey);
        return ResponseEntity.noContent().build();
    }
}
