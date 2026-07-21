package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegate.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds
) {
}
