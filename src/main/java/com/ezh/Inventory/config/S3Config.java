package com.ezh.Inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Wires up the AWS SDK v2 {@link S3Client} and {@link S3Presigner} beans.
 *
 * <p>Configuration properties (all resolved from env-vars with defaults):
 * <pre>
 *   aws.s3.access-key-id     = ${AWS_ACCESS_KEY_ID}
 *   aws.s3.secret-access-key = ${AWS_SECRET_ACCESS_KEY}
 *   aws.s3.region            = ${AWS_REGION:ap-south-1}
 *   aws.s3.bucket            = ${AWS_S3_BUCKET}
 * </pre>
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key-id}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
