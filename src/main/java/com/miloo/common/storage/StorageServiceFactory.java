package com.miloo.common.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry and factory for looking up storage provider services at runtime.
 */
@Component
public class StorageServiceFactory {

    private final StorageService r2StorageService;
    private final StorageService s3StorageService;
    private final StorageService cloudinaryStorageService;
    private final StorageProperties properties;
    private final Map<StorageProvider, StorageService> providerMap = new HashMap<>();

    public StorageServiceFactory(
            @Qualifier("r2StorageService") StorageService r2StorageService,
            @Qualifier("s3StorageService") StorageService s3StorageService,
            @Qualifier("cloudinaryStorageService") StorageService cloudinaryStorageService,
            StorageProperties properties
    ) {
        this.r2StorageService = r2StorageService;
        this.s3StorageService = s3StorageService;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.properties = properties;

        providerMap.put(StorageProvider.R2, r2StorageService);
        providerMap.put(StorageProvider.S3, s3StorageService);
        providerMap.put(StorageProvider.CLOUDINARY, cloudinaryStorageService);
    }

    /**
     * Returns the currently active storage service configured in application.yml.
     */
    public StorageService getActiveService() {
        return getService(properties.getProviderEnum());
    }

    /**
     * Returns the storage service matching the specified provider enum.
     */
    public StorageService getService(StorageProvider provider) {
        if (provider == null) {
            provider = properties.getProviderEnum();
        }
        StorageService service = providerMap.get(provider);
        return service != null ? service : r2StorageService;
    }

    /**
     * Returns the storage service matching the specified provider name string.
     */
    public StorageService getService(String providerName) {
        return getService(StorageProvider.fromString(providerName));
    }
}
