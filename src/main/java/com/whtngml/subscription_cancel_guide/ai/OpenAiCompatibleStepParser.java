package com.whtngml.subscription_cancel_guide.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

// OpenAI 호환 Chat Completions 주소(기본 Google Gemini, 학교 factchat도 가능)로 단계를 나눈다
@Slf4j
@Component
public class OpenAiCompatibleStepParser implements StepParser {

    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;
    private final AiStepsJson aiStepsJson;
    private final String apiKey;
    private final String model;
    // 학교 factchat 게이트웨이는 주소 끝에 /가 있어야 한다
    private final boolean trailingSlash;

    @Autowired
    public OpenAiCompatibleStepParser(AiStepsJson aiStepsJson,
                              @Value("${app.ai.base-url}") String baseUrl,
                              @Value("${app.ai.api-key}") String apiKey,
                              @Value("${app.ai.model}") String model,
                              @Value("${app.ai.trailing-slash:false}") boolean trailingSlash) {
        this(createRestClient(baseUrl, apiKey), aiStepsJson, apiKey, model, trailingSlash);
    }

    // 테스트에서 가짜 서버에 연결한 RestClient를 넣을 때 쓴다
    OpenAiCompatibleStepParser(RestClient restClient, AiStepsJson aiStepsJson, String apiKey, String model,
                               boolean trailingSlash) {
        this.restClient = restClient;
        this.aiStepsJson = aiStepsJson;
        this.apiKey = apiKey;
        this.model = model;
        this.trailingSlash = trailingSlash;
    }

    private static RestClient createRestClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiStepsResult parse(String serviceName, String rawText) {
        if (!isAvailable()) {
            throw new StepParseException("지금은 AI 변환을 쓸 수 없어요.");
        }

        ChatRequest request = new ChatRequest(model, List.of(
                new RequestMessage("system", StepPrompt.system()),
                new RequestMessage("user", List.of(
                        new TextPart("text", "서비스: " + serviceName + "\n해지 방법 글:\n" + rawText)))
        ), 0.0, 2000);

        StepParseException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String content = call(request);
            try {
                return aiStepsJson.read(stripCodeFence(content));
            } catch (StepParseException e) {
                log.warn("AI 응답 형식 오류 ({}번째): {} / 응답: {}", attempt, e.getMessage(), content);
                lastError = e;
            }
        }
        throw new StepParseException("AI가 글을 단계로 나누지 못했어요. 글만 보내도 괜찮아요.", lastError);
    }

    private String call(ChatRequest request) {
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri(trailingSlash ? "/chat/completions/" : "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            log.warn("AI 서버 호출 실패: {}", e.getMessage());
            throw new StepParseException("AI 서버에 연결하지 못했어요. 잠시 뒤 다시 해 보세요.", e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new StepParseException("AI 서버가 빈 응답을 보냈어요.");
        }
        String content = response.choices().getFirst().message().content();
        return content == null ? "" : content;
    }

    // 모델이 ```json ... ``` 으로 감싸서 답하는 경우가 있어 벗겨 낸다
    static String stripCodeFence(String content) {
        String text = content.strip();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd > 0 && lastFence > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFence).strip();
            }
        }
        return text;
    }

    // ---------- OpenAI 호환 요청·응답 형식 ----------

    record ChatRequest(String model, List<RequestMessage> messages, double temperature,
                       @JsonProperty("max_tokens") int maxTokens) {
    }

    // 보내는 메시지. content는 문자열(system) 또는 내용 조각 목록(user: 글, 2부에서 이미지)
    record RequestMessage(String role, Object content) {
    }

    record TextPart(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }
}
