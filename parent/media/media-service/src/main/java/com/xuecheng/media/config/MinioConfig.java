package com.xuecheng.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    /**
     * minio:
     * endpoint: http://192.168.101.65:9000
     * accessKey: minioadmin
     * secretKey: minioadmin
     * bucket:
     * files: mediafiles
     * videofiles: video
     */

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().
                endpoint(endpoint).
                credentials(accessKey, secretKey).
                build();
    }

}
