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

    static final String SYSTEM_PROMPT = """
            너는 구독 서비스 해지 방법 글을 휴대폰 화면 단계로 나누는 도우미야.
            규칙:
            1. 글에 있는 내용만 단계로 나눠. 글에 없는 화면, 버튼, 내용을 더하지 마.
            2. 한 단계는 "어느 화면에서 어떤 버튼을 누르는지" 하나야.
            3. screen은 그 단계의 화면 이름, target은 누를 버튼이나 메뉴 이름이야. 둘 다 글에 나온 표현을 쓰고 50자를 넘기지 마.
            4. position은 버튼이 화면 어디쯤 있는지야. top-left, top-right, middle, bottom 중 하나만 써. 글에 위치가 없으면 middle.
            5. otherMenus는 그 화면에 target 말고 보이는 다른 메뉴 이름 목록이야. 글에 실제로 나온 이름만 넣고 지어내지 마. 없으면 [].
            6. order는 1부터 차례대로 매겨. 단계는 20개까지야.
            7. 아래 형식의 JSON으로만 답해. 설명이나 코드 블록 표시는 쓰지 마.
            {"service":"서비스 이름","steps":[{"order":1,"screen":"화면 이름","target":"누를 버튼","position":"top-right","otherMenus":["다른 메뉴"]}]}
            """;

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
                new RequestMessage("system", SYSTEM_PROMPT),
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
