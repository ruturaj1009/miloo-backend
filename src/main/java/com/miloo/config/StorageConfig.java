package com.miloo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.miloo.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties properties;

    private boolean isDummy(String key) {
        return key == null || key.isBlank() || key.contains("dummy");
    }

    @Bean(name = "r2Presigner")
    @Primary
    public S3Presigner r2Presigner() {
        StorageProperties.R2Properties r2 = properties.getR2();
        if (properties.isMockEnabled() || isDummy(r2.getAccessKey())) {
            return S3Presigner.builder()
                    .region(Region.of("auto"))
                    .endpointOverride(URI.create("https://dummy.r2.cloudflarestorage.com"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock_access_key", "mock_secret_key")
                    ))
                    .build();
        }

        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2.getAccountId());
        return S3Presigner.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey())
                ))
                .build();
    }

    @Bean(name = "r2S3Client")
    public S3Client r2S3Client() {
        StorageProperties.R2Properties r2 = properties.getR2();
        if (properties.isMockEnabled() || isDummy(r2.getAccessKey())) {
            return S3Client.builder()
                    .region(Region.of("auto"))
                    .endpointOverride(URI.create("https://dummy.r2.cloudflarestorage.com"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock_access_key", "mock_secret_key")
                    ))
                    .build();
        }

        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2.getAccountId());
        return S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey())
                ))
                .build();
    }

    @Bean(name = "awsS3Presigner")
    public S3Presigner awsS3Presigner() {
        StorageProperties.S3Properties s3 = properties.getS3();
        Region region = Region.of(s3.getRegion() != null ? s3.getRegion() : "us-east-1");

        if (properties.isMockEnabled() || isDummy(s3.getAccessKey())) {
            return S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock_access_key", "mock_secret_key")
                    ))
                    .build();
        }

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())
                ))
                .build();
    }

    @Bean(name = "awsS3Client")
    @Primary
    public S3Client awsS3Client() {
        StorageProperties.S3Properties s3 = properties.getS3();
        Region region = Region.of(s3.getRegion() != null ? s3.getRegion() : "us-east-1");

        if (properties.isMockEnabled() || isDummy(s3.getAccessKey())) {
            return S3Client.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock_access_key", "mock_secret_key")
                    ))
                    .build();
        }

        return S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())
                ))
                .build();
    }

    @Bean(name = "cloudinary")
    public Cloudinary cloudinary() {
        StorageProperties.CloudinaryProperties c = properties.getCloudinary();
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", c.getCloudName(),
                "api_key", c.getApiKey(),
                "api_secret", c.getApiSecret(),
                "secure", true
        ));
    }
}
