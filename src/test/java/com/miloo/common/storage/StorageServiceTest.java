package com.miloo.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.common.storage.impl.CloudinaryStorageService;
import com.miloo.common.storage.impl.R2StorageService;
import com.miloo.common.storage.impl.S3StorageService;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private StorageProperties properties;
    private R2StorageService r2StorageService;
    private S3StorageService s3StorageService;
    private CloudinaryStorageService cloudinaryStorageService;
    private StorageServiceFactory factory;
    private StorageServiceRouter router;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setMockEnabled(true);

        // Dummy/mock AWS Presigner & Client
        S3Presigner mockPresigner = null;
        S3Client mockS3Client = null;

        // Dummy Cloudinary instance
        Cloudinary dummyCloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "test-cloud",
                "api_key", "test-key",
                "api_secret", "test-secret"
        ));

        r2StorageService = new R2StorageService(properties, mockPresigner, mockS3Client);
        s3StorageService = new S3StorageService(properties, mockPresigner, mockS3Client);
        cloudinaryStorageService = new CloudinaryStorageService(properties, dummyCloudinary);

        factory = new StorageServiceFactory(r2StorageService, s3StorageService, cloudinaryStorageService, properties);
        router = new StorageServiceRouter(factory);
    }

    @Test
    @DisplayName("R2StorageService generates valid presigned URL and response DTO")
    void testR2StorageService_generateUploadUrl() {
        UUID userId = UUID.randomUUID();
        PresignedUrlResponseDto response = r2StorageService.generateUploadUrl(userId, "avatar.png", "image/png");

        assertNotNull(response);
        assertEquals("R2", response.getProvider());
        assertNotNull(response.getUploadUrl());
        assertTrue(response.getUploadUrl().contains("r2-mock.storage.cloudflare.com"));
        assertNotNull(response.getObjectKey());
        assertEquals(response.getObjectKey(), response.getR2ObjectKey(), "r2_object_key must match object_key for backward compatibility");
        assertTrue(response.getObjectKey().startsWith("users/" + userId + "/"));
        assertTrue(response.getPublicMediaUrl().contains(response.getObjectKey()));
    }

    @Test
    @DisplayName("S3StorageService generates valid presigned URL and response DTO")
    void testS3StorageService_generateUploadUrl() {
        UUID userId = UUID.randomUUID();
        PresignedUrlResponseDto response = s3StorageService.generateUploadUrl(userId, "photo.jpg", "image/jpeg");

        assertNotNull(response);
        assertEquals("S3", response.getProvider());
        assertNotNull(response.getUploadUrl());
        assertTrue(response.getUploadUrl().contains("s3.amazonaws.com"));
        assertNotNull(response.getObjectKey());
        assertEquals(response.getObjectKey(), response.getR2ObjectKey());
        assertTrue(response.getObjectKey().startsWith("users/" + userId + "/"));
    }

    @Test
    @DisplayName("CloudinaryStorageService generates signed upload parameters and endpoint")
    void testCloudinaryStorageService_generateUploadUrl() {
        UUID userId = UUID.randomUUID();
        PresignedUrlResponseDto response = cloudinaryStorageService.generateUploadUrl(userId, "profile.webp", "image/webp");

        assertNotNull(response);
        assertEquals("CLOUDINARY", response.getProvider());
        assertNotNull(response.getUploadUrl());
        assertTrue(response.getUploadUrl().contains("cloudinary.com"));
        assertNotNull(response.getObjectKey());
        assertNotNull(response.getAdditionalParams());
        assertTrue(response.getAdditionalParams().containsKey("signature"));
        assertTrue(response.getAdditionalParams().containsKey("api_key"));
    }

    @Test
    @DisplayName("StorageServiceFactory retrieves correct bean by enum and string")
    void testStorageServiceFactory() {
        assertSame(r2StorageService, factory.getService(StorageProvider.R2));
        assertSame(s3StorageService, factory.getService(StorageProvider.S3));
        assertSame(cloudinaryStorageService, factory.getService(StorageProvider.CLOUDINARY));

        assertSame(r2StorageService, factory.getService("r2"));
        assertSame(s3StorageService, factory.getService("s3"));
        assertSame(cloudinaryStorageService, factory.getService("cloudinary"));
    }

    @Test
    @DisplayName("StorageServiceRouter dynamically delegates according to app.storage.provider flag")
    void testStorageServiceRouter_dynamicDelegation() {
        UUID userId = UUID.randomUUID();

        // 1. Provider = R2
        properties.setProvider("r2");
        assertEquals(StorageProvider.R2, router.getProviderType());
        PresignedUrlResponseDto r2Resp = router.generateUploadUrl(userId, "test.png", "image/png");
        assertEquals("R2", r2Resp.getProvider());

        // 2. Provider = S3
        properties.setProvider("s3");
        assertEquals(StorageProvider.S3, router.getProviderType());
        PresignedUrlResponseDto s3Resp = router.generateUploadUrl(userId, "test.png", "image/png");
        assertEquals("S3", s3Resp.getProvider());

        // 3. Provider = Cloudinary
        properties.setProvider("cloudinary");
        assertEquals(StorageProvider.CLOUDINARY, router.getProviderType());
        PresignedUrlResponseDto cloudResp = router.generateUploadUrl(userId, "test.png", "image/png");
        assertEquals("CLOUDINARY", cloudResp.getProvider());
    }

    @Test
    @DisplayName("Direct multipart upload succeeds for all providers")
    void testDirectUpload() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test_photo.jpg", "image/jpeg", "image-content".getBytes()
        );

        // R2 upload
        FileUploadResponseDto r2Result = r2StorageService.uploadFile(userId, file);
        assertNotNull(r2Result);
        assertEquals("R2", r2Result.getProvider());
        assertNotNull(r2Result.getObjectKey());
        assertNotNull(r2Result.getPublicUrl());

        // S3 upload
        FileUploadResponseDto s3Result = s3StorageService.uploadFile(userId, file);
        assertNotNull(s3Result);
        assertEquals("S3", s3Result.getProvider());
        assertNotNull(s3Result.getObjectKey());

        // Cloudinary upload
        FileUploadResponseDto cloudResult = cloudinaryStorageService.uploadFile(userId, file);
        assertNotNull(cloudResult);
        assertEquals("CLOUDINARY", cloudResult.getProvider());
        assertNotNull(cloudResult.getObjectKey());
    }

    @Test
    @DisplayName("File deletion operates safely without exceptions across all providers")
    void testDeleteFile() {
        assertDoesNotThrow(() -> r2StorageService.deleteFile("users/dummy/123_photo.jpg"));
        assertDoesNotThrow(() -> s3StorageService.deleteFile("users/dummy/123_photo.jpg"));
        assertDoesNotThrow(() -> cloudinaryStorageService.deleteFile("miloo/users/dummy/123_photo"));
    }
}
