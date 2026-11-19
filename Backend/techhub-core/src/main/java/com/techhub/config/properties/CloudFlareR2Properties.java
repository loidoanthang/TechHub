package com.techhub.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudflare.r2")
public class CloudFlareR2Properties {
    private String accountId;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicUrl;
}