package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegate.anthropic")
public record AnthropicProperties(
        String apiKey,
        String model,
        String baseUrl,
        String version
) {
}
