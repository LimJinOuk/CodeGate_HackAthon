package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegate.crypto")
public record CryptoProperties(String secret) {
}
