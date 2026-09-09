package com.techhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long accessExpiration;
    private long refreshExpiration;
}
