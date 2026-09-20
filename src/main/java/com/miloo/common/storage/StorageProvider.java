package com.miloo.common.storage;

public enum StorageProvider {
    R2,
    S3,
    CLOUDINARY;

    public static StorageProvider fromString(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return R2;
        }
        try {
            return StorageProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return R2;
        }
    }
}
