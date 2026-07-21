package com.example.codegate.auth.service;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.config.JwtProperties;
import com.example.codegate.global.BusinessException;
import com.example.codegate.user.entity.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String createToken(Long userId, UserRole role) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = Map.of(
                    "sub", String.valueOf(userId),
                    "role", role.name(),
                    "iat", now,
                    "exp", now + properties.expirationSeconds()
            );

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + base64Url(hmacSha256(signingInput));
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_CREATE_FAILED", "토큰 생성에 실패했습니다.");
        }
    }

    public AuthenticatedUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = base64Url(hmacSha256(signingInput));
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw unauthorized();
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long exp = payload.get("exp").asLong();
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다.");
            }

            return new AuthenticatedUser(
                    Long.parseLong(payload.get("sub").asText()),
                    UserRole.valueOf(payload.get("role").asText())
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    public AuthenticatedUser parseAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw unauthorized();
        }
        return parse(authorizationHeader.substring("Bearer ".length()));
    }

    private BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효한 인증 정보가 필요합니다.");
    }

    private byte[] hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            diff |= leftBytes[i] ^ rightBytes[i];
        }
        return diff == 0;
    }
}
