package com.example.codegate.auth.client;

import com.example.codegate.auth.dto.KakaoUserInfo;
import com.example.codegate.config.KakaoOAuthProperties;
import com.example.codegate.global.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

@Component
public class KakaoOAuthClient {

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public KakaoOAuthClient(KakaoOAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String createLoginUrl(String redirectUri, String state) {
        validateClientId();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.authorizeUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", redirectUri);

        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }

        return builder.build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    public KakaoUserInfo getUserInfoByAuthorizationCode(String code, String redirectUri) {
        validateClientId();
        String accessToken = requestAccessToken(code, redirectUri);
        return requestUserInfo(accessToken);
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }

        JsonNode response = restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "KAKAO_TOKEN_FAILED", "카카오 토큰 발급 응답이 올바르지 않습니다.");
        }
        return response.get("access_token").asText();
    }

    private KakaoUserInfo requestUserInfo(String accessToken) {
        JsonNode response = restClient.get()
                .uri(properties.userInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.get("id") == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "KAKAO_USER_INFO_FAILED", "카카오 사용자 정보 응답이 올바르지 않습니다.");
        }

        String email = null;
        JsonNode kakaoAccount = response.get("kakao_account");
        if (kakaoAccount != null && kakaoAccount.get("email") != null) {
            email = kakaoAccount.get("email").asText();
        }
        return new KakaoUserInfo(response.get("id").asText(), email);
    }

    private void validateClientId() {
        if (!StringUtils.hasText(properties.clientId())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "KAKAO_CLIENT_ID_MISSING", "KAKAO_REST_API_KEY 환경 변수가 필요합니다.");
        }
    }
}
