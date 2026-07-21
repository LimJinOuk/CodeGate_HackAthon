package com.example.codegate.medicalfile.service;

import com.example.codegate.config.AnthropicProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicClaudeClient {

    private static final int OCR_MAX_TOKENS = 4096;
    private static final int ANALYSIS_MAX_TOKENS = 4096;
    private static final int MAX_ANALYSIS_TEXT_CHARS = 120_000;

    private final AnthropicProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public AnthropicClaudeClient(AnthropicProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey() == null ? "" : properties.apiKey())
                .defaultHeader("anthropic-version", properties.version())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String modelName() {
        return properties.model();
    }

    public String extractText(List<OcrImagePayload> images, int startPageNumber) {
        requireApiKey();

        List<Map<String, Object>> content = new ArrayList<>();
        int pageNumber = startPageNumber;
        for (OcrImagePayload image : images) {
            content.add(Map.of("type", "text", "text", image.label() + " / page " + pageNumber));
            content.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", image.mediaType(),
                            "data", image.base64Data()
                    )
            ));
            pageNumber++;
        }
        content.add(Map.of("type", "text", "text", """
                위 건강검진 결과지 이미지에서 보이는 모든 주요 검사 텍스트와 수치를 OCR로 추출해줘.
                표 형태의 항목은 '항목명: 값 (단위, 참고치가 있으면 참고치)' 형태로 정리해.
                추측하지 말고, 흐리거나 읽을 수 없는 값은 '[판독불가]'라고 적어.
                요약이나 추천은 하지 말고 OCR 텍스트만 한국어로 출력해.
                """));

        return createMessage(content, OCR_MAX_TOKENS);
    }

    public MedicalAnalysisResult analyze(String extractedText, Integer patientAge) {
        requireApiKey();

        String ageText = patientAge == null ? "알 수 없음" : patientAge + "세";
        String boundedText = extractedText == null ? "" : extractedText;
        if (boundedText.length() > MAX_ANALYSIS_TEXT_CHARS) {
            boundedText = boundedText.substring(0, MAX_ANALYSIS_TEXT_CHARS);
        }

        List<Map<String, Object>> content = List.of(Map.of("type", "text", "text", """
                너는 건강검진 결과지를 사용자에게 쉽게 설명하는 보조 시스템이다.
                아래 OCR 텍스트를 바탕으로 결과를 요약하고, 식습관과 운동을 추천해라.

                조건:
                - 의학적 진단처럼 단정하지 말 것.
                - 사용자가 이해하기 쉬운 한국어로 작성할 것.
                - 추천 음식은 구체적인 음식/식단 방향을 포함할 것.
                - 추천 운동은 운동 종류, 강도, 빈도 중심으로 작성할 것.
                - 혈압, 혈당, 감마GTP는 사용자 나이 평균 대비 나쁨 정도를 0~100 정수로 산정할 것.
                - 0은 매우 좋음, 100은 매우 나쁨이다.
                - 해당 항목 값을 OCR 텍스트에서 찾을 수 없으면 null로 반환할 것.
                - 혈당은 공복혈당 또는 HbA1c 등 혈당 관련 지표가 있으면 가장 적절한 지표를 사용하되 근거를 요약에 포함할 것.

                반드시 아래 JSON만 반환해라. 마크다운 코드블록을 쓰지 마라.

                {
                  "summary": "요약 문자열",
                  "recommendedFood": "추천 음식/식습관 문자열",
                  "recommendedExercise": "추천 운동 문자열",
                  "bloodPressureScorePercent": 0,
                  "bloodSugarScorePercent": 0,
                  "gammaGtpScorePercent": 0
                }

                사용자 나이: %s

                OCR 텍스트:
                %s
                """.formatted(ageText, boundedText)));

        String responseText = createMessage(content, ANALYSIS_MAX_TOKENS);
        return parseAnalysisResult(responseText);
    }

    private String createMessage(List<Map<String, Object>> content, int maxTokens) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("max_tokens", maxTokens);
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", content
        )));

        String response = webClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(5));

        return extractTextFromAnthropicResponse(response);
    }

    private String extractTextFromAnthropicResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.get("content");
            if (content == null || !content.isArray()) {
                throw new IllegalStateException("Claude 응답에 content가 없습니다.");
            }

            StringBuilder builder = new StringBuilder();
            for (JsonNode node : content) {
                JsonNode textNode = node.get("text");
                if (textNode != null) {
                    builder.append(textNode.asText()).append("\n");
                }
            }
            return builder.toString().trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Claude 응답 파싱에 실패했습니다.", exception);
        }
    }

    private MedicalAnalysisResult parseAnalysisResult(String responseText) {
        try {
            String json = stripCodeFence(responseText);
            JsonNode root = objectMapper.readTree(json);
            return new MedicalAnalysisResult(
                    text(root, "summary"),
                    text(root, "recommendedFood"),
                    text(root, "recommendedExercise"),
                    percent(root, "bloodPressureScorePercent"),
                    percent(root, "bloodSugarScorePercent"),
                    percent(root, "gammaGtpScorePercent")
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Claude 분석 결과 JSON 파싱에 실패했습니다.", exception);
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewLine >= 0 && lastFence > firstNewLine) {
                return trimmed.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Integer percent(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        int percent = value.asInt();
        return Math.max(0, Math.min(100, percent));
    }

    private void requireApiKey() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY 환경변수가 설정되어 있지 않습니다.");
        }
    }
}
