package com.techhub.modules.s3cloudflare.service;

import com.techhub.config.properties.CloudFlareR2Properties;
import com.techhub.modules.s3cloudflare.dto.PresignedUrlResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3CloudFlareR2Service {

    private final S3Presigner presigner;
    private final CloudFlareR2Properties properties;

    public S3CloudFlareR2Service(S3Presigner presigner, CloudFlareR2Properties properties) {
        this.presigner = presigner;
        this.properties = properties;
    }

    public PresignedUrlResponse generateUploadUrl(String folder, String originalFileName, String contentType) {
        return generateUploadUrl(folder, null, originalFileName, contentType);
    }

    public PresignedUrlResponse generateUploadUrl(String folder, String prefix, String originalFileName, String contentType) {
        // 1. Tách đuôi file và sinh key duy nhất (tránh ghi đè file nhau)
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            int dotIndex = originalFileName.lastIndexOf(".");
            if (dotIndex >= 0) {
                extension = originalFileName.substring(dotIndex);
            }
        }

        String prefixPart = (prefix != null && !prefix.isBlank()) ? prefix + "_" : "";
        String uniqueFileName = prefixPart + UUID.randomUUID() + extension;
        String key = (folder != null && !folder.isBlank()) ? folder + "/" + uniqueFileName : uniqueFileName;

        // 2. Tạo Presign PUT Request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = presigner.presignPutObject(presignRequest).url().toString();
        String filePublicUrl = buildPublicUrl(key);

        return new PresignedUrlResponse(uploadUrl, filePublicUrl, key);
    }

    public String buildPublicUrl(String key) {
        if (properties.getPublicUrl() == null || properties.getPublicUrl().isBlank()) {
            return key;
        }
        String base = properties.getPublicUrl().endsWith("/")
                ? properties.getPublicUrl().substring(0, properties.getPublicUrl().length() - 1)
                : properties.getPublicUrl();
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + cleanKey;
    }
}
