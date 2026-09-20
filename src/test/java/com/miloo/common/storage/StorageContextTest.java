package com.miloo.common.storage;

import com.miloo.common.storage.impl.CloudinaryStorageService;
import com.miloo.common.storage.impl.R2StorageService;
import com.miloo.common.storage.impl.S3StorageService;
import com.miloo.config.StorageConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        StorageConfig.class,
        StorageServiceFactory.class,
        StorageServiceRouter.class,
        R2StorageService.class,
        S3StorageService.class,
        CloudinaryStorageService.class
})
@TestPropertySource(properties = {
        "app.storage.provider=cloudinary",
        "app.storage.mock-enabled=true"
})
class StorageContextTest {

    @Autowired
    private StorageService primaryStorageService;

    @Autowired
    @Qualifier("r2StorageService")
    private StorageService r2StorageService;

    @Autowired
    @Qualifier("s3StorageService")
    private StorageService s3StorageService;

    @Autowired
    @Qualifier("cloudinaryStorageService")
    private StorageService cloudinaryStorageService;

    @Autowired
    private StorageProperties properties;

    @Test
    @DisplayName("Spring Context loads all storage provider beans and dynamically binds primary to flag")
    void testBeansInjectedAndFlagRespected() {
        assertNotNull(primaryStorageService);
        assertNotNull(r2StorageService);
        assertNotNull(s3StorageService);
        assertNotNull(cloudinaryStorageService);

        assertEquals("cloudinary", properties.getProvider());
        assertEquals(StorageProvider.CLOUDINARY, primaryStorageService.getProviderType());
    }
}
