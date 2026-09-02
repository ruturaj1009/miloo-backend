package com.miloo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Value("${app.storage.r2.account-id:dummy_account}")
    private String accountId;

    @Value("${app.storage.r2.access-key:dummy_key}")
    private String accessKey;

    @Value("${app.storage.r2.secret-key:dummy_secret}")
    private String secretKey;

    @Value("${app.storage.r2.mock-enabled:true}")
    private boolean mockEnabled;

    @Bean
    public S3Presigner s3Presigner() {
        if (mockEnabled || "dummy_key".equals(accessKey)) {
            // Provide a lightweight dummy presigner in mock mode
            return S3Presigner.builder()
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock_access_key", "mock_secret_key")
                    ))
                    .endpointOverride(URI.create("https://dummy.r2.cloudflarestorage.com"))
                    .build();
        }

        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
        return S3Presigner.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}
