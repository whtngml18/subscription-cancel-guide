package com.whtngml.subscription_cancel_guide.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenshotAiStepsTest {
    private final AiStepsJson json = new AiStepsJson(new JsonMapper());

    @Test
    void keepsValidReferencesAndFiltersNotesAndMenus() {
        String input = """
                {"service":"테스트","steps":[
                  {"order":1,"screen":"홈","target":"계정","position":"middle","otherMenus":["a","b","c","d","e"],
                   "image":2,"note":"캡처에는 설정 표시"},
                  {"order":2,"screen":"계정","target":"해지","position":"bottom","image":3,"note":"test@example.com"}
                ]}
                """;
        var steps = json.read(input, 2).steps();
        assertThat(steps.getFirst().image()).isEqualTo(2);
        assertThat(steps.getFirst().note()).isEqualTo("캡처에는 설정 표시");
        assertThat(steps.getFirst().otherMenus()).containsExactly("a", "b", "c", "d");
        assertThat(steps.getLast().image()).isNull();
        assertThat(steps.getLast().note()).isNull();
        assertThat(json.read(input).steps().getFirst().image()).isNull();
        assertThat(json.read(input).steps().getFirst().note()).isNull();
    }

    @Test
    void includesScreenshotInstructionsOnlyWhenNeeded() {
        assertThat(StepPrompt.system(true)).contains("[캡처 읽기]", "otherMenus", "note");
        assertThat(StepPrompt.system(false)).doesNotContain("[캡처 읽기]");
    }
}
