package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
        String clientId,
        String clientSecret,
        String authorizeUri,
        String tokenUri,
        String userInfoUri
) {
}
