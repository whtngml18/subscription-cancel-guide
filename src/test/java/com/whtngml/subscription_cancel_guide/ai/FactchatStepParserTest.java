package com.whtngml.subscription_cancel_guide.ai;

import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FactchatStepParserTest {

    private static final String URL = "https://ai.test/v1/gateway/chat/completions/";
    private static final String GOOD = """
            {"service":"넷플릭스","steps":[{"order":1,"screen":"홈","target":"프로필","position":"top-right"}]}""";
    private static final String TEXT = "홈에서 오른쪽 위 프로필을 눌러요.";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private FactchatStepParser parser;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://ai.test/v1/gateway")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        parser = new FactchatStepParser(builder.build(), new AiStepsJson(jsonMapper), "test-key", "test-model");
    }

    // OpenAI 호환 응답 본문을 만든다
    private String chatResponse(String content) {
        return jsonMapper.writeValueAsString(Map.of(
                "id", "chatcmpl-1",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", content)))));
    }

    @Test
    void 응답_JSON을_단계로_바꾼다() {
        server.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("넷플릭스")))
                .andRespond(withSuccess(chatResponse("```json\n" + GOOD + "\n```"), MediaType.APPLICATION_JSON));

        AiStepsResult result = parser.parse("넷플릭스", TEXT);

        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().getFirst().target()).isEqualTo("프로필");
        server.verify();
    }

    @Test
    void 형식이_틀리면_한_번_다시_요청한다() {
        server.expect(requestTo(URL)).andRespond(withSuccess(chatResponse("단계를 나눠 봤어요!"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(chatResponse(GOOD), MediaType.APPLICATION_JSON));

        AiStepsResult result = parser.parse("넷플릭스", TEXT);

        assertThat(result.service()).isEqualTo("넷플릭스");
        server.verify();
    }

    @Test
    void 두_번_모두_틀리면_예외() {
        String badPosition = GOOD.replace("top-right", "left");
        server.expect(requestTo(URL)).andRespond(withSuccess(chatResponse(badPosition), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(chatResponse(badPosition), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> parser.parse("넷플릭스", TEXT)).isInstanceOf(StepParseException.class);
        server.verify();
    }

    @Test
    void 서버_오류는_다시_요청하지_않고_예외() {
        server.expect(once(), requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> parser.parse("넷플릭스", TEXT))
                .isInstanceOf(StepParseException.class)
                .hasMessageContaining("연결하지 못했어요");
        server.verify();
    }

    @Test
    void 키가_없으면_쓸_수_없다() {
        FactchatStepParser noKey = new FactchatStepParser(RestClient.create(), new AiStepsJson(jsonMapper), "", "test-model");

        assertThat(noKey.isAvailable()).isFalse();
        assertThatThrownBy(() -> noKey.parse("넷플릭스", TEXT)).isInstanceOf(StepParseException.class);
    }
}
