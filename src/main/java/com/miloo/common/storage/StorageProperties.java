package com.miloo.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Active storage provider: r2 | s3 | cloudinary
     */
    private String provider = "r2";

    /**
     * Whether mock mode is enabled (for zero-friction local development without real cloud credentials)
     */
    private boolean mockEnabled = true;

    private R2Properties r2 = new R2Properties();
    private S3Properties s3 = new S3Properties();
    private CloudinaryProperties cloudinary = new CloudinaryProperties();

    public StorageProvider getProviderEnum() {
        return StorageProvider.fromString(provider);
    }

    @Data
    public static class R2Properties {
        private String accountId = "dummy_account";
        private String accessKey = "dummy_key";
        private String secretKey = "dummy_secret";
        private String bucketName = "miloo-media";
        private String publicDomain = "https://pub-media.example.com";
    }

    @Data
    public static class S3Properties {
        private String region = "us-east-1";
        private String accessKey = "dummy_key";
        private String secretKey = "dummy_secret";
        private String bucketName = "miloo-media";
        private String publicDomain = "https://miloo-media.s3.amazonaws.com";
    }

    @Data
    public static class CloudinaryProperties {
        private String cloudName = "dummy_cloud";
        private String apiKey = "dummy_key";
        private String apiSecret = "dummy_secret";
        private String folder = "miloo/users";
    }
}
