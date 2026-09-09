package com.techhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.token")
@Getter
@Setter
public class TokenProperties {

    private long verificationExpiration;

    private long passwordResetExpiration;

}