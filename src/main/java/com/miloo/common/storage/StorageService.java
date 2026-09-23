package com.miloo.common.storage;

import com.miloo.common.storage.dto.FileUploadResponseDto;
import com.miloo.modules.people.dto.PresignedUrlResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Centralized interface for media and image storage.
 * Supported providers: Cloudflare R2, AWS S3, Cloudinary.
 */
public interface StorageService {

    /**
     * Generates a presigned URL or direct upload signed metadata for client-side uploads.
     *
     * @param userId      user requesting the upload
     * @param filename    original file name
     * @param contentType MIME type of the file
     * @return PresignedUrlResponseDto containing upload URL and media metadata
     */
    PresignedUrlResponseDto generateUploadUrl(UUID userId, String filename, String contentType);

    /**
     * Directly uploads a MultipartFile from the server.
     *
     * @param userId user uploading the file
     * @param file   the MultipartFile to upload
     * @return FileUploadResponseDto containing uploaded media details
     */
    FileUploadResponseDto uploadFile(UUID userId, MultipartFile file);

    /**
     * Directly uploads raw byte content with filename and contentType.
     *
     * @param userId      user uploading the file
     * @param data        byte array of the file
     * @param filename    file name
     * @param contentType MIME type
     * @return FileUploadResponseDto containing uploaded media details
     */
    FileUploadResponseDto uploadFile(UUID userId, byte[] data, String filename, String contentType);

    /**
     * Deletes a file by its object key or public ID.
     *
     * @param objectKey the key or public ID of the resource to delete
     */
    void deleteFile(String objectKey);

    /**
     * Resolves the public accessible URL for a given resource key.
     *
     * @param objectKey the key or public ID
     * @return public CDN or cloud URL
     */
    String getPublicUrl(String objectKey);

    /**
     * Returns the provider type associated with this storage service.
     *
     * @return StorageProvider enum (R2, S3, CLOUDINARY)
     */
    StorageProvider getProviderType();
}
