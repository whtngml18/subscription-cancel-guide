package com.whtngml.subscription_cancel_guide.ai;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

// AI 키는 AI_API_KEY → GEMINI_API_KEY → FACTCHAT_API_KEY 순서로 읽는다
class AiKeyPropertyTest {

    @Nested
    @SpringBootTest(properties = {"GEMINI_API_KEY=g-key", "FACTCHAT_API_KEY=f-key"})
    class GeminiOverFactchat {

        @Autowired
        Environment env;

        @Test
        void Gemini_키를_쓴다() {
            assertThat(env.getProperty("app.ai.api-key")).isEqualTo("g-key");
        }
    }

    @Nested
    @SpringBootTest(properties = {"AI_API_KEY=a-key", "GEMINI_API_KEY=g-key"})
    class AiKeyFirst {

        @Autowired
        Environment env;

        @Test
        void AI_API_KEY가_가장_먼저() {
            assertThat(env.getProperty("app.ai.api-key")).isEqualTo("a-key");
        }
    }

    @Nested
    @SpringBootTest(properties = "AI_API_KEY=")   // 개발 PC에 실제 키가 있어도 "키 없음"을 재현
    class NoKey {

        @Autowired
        StepParser stepParser;

        @Test
        void 키가_없으면_AI를_쓰지_않는다() {
            assertThat(stepParser.isAvailable()).isFalse();
        }
    }
}
