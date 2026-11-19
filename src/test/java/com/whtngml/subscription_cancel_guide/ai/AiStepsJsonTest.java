package com.whtngml.subscription_cancel_guide.ai;

import com.whtngml.subscription_cancel_guide.dto.AiStep;
import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiStepsJsonTest {

    private final AiStepsJson aiStepsJson = new AiStepsJson(JsonMapper.builder().build());

    @Test
    void 올바른_JSON은_순서대로_정리해서_읽는다() {
        String json = """
                {"service":" 넷플릭스 ","steps":[
                  {"order":2,"screen":"계정","target":" 멤버십 해지 ","position":"middle"},
                  {"order":1,"screen":"홈","target":"프로필","position":"top-right"}
                ]}
                """;

        AiStepsResult result = aiStepsJson.read(json);

        assertThat(result.service()).isEqualTo("넷플릭스");
        assertThat(result.steps()).containsExactly(
                new AiStep(1, "홈", "프로필", "top-right"),
                new AiStep(2, "계정", "멤버십 해지", "middle"));
    }

    @Test
    void 정해지지_않은_위치는_거절한다() {
        String json = """
                {"service":"넷플릭스","steps":[{"order":1,"screen":"홈","target":"프로필","position":"left"}]}
                """;

        assertThatThrownBy(() -> aiStepsJson.read(json)).isInstanceOf(StepParseException.class);
    }

    @Test
    void 비어_있거나_긴_이름은_거절한다() {
        String blank = """
                {"steps":[{"order":1,"screen":" ","target":"프로필","position":"middle"}]}
                """;
        String tooLong = """
                {"steps":[{"order":1,"screen":"홈","target":"%s","position":"middle"}]}
                """.formatted("가".repeat(51));

        assertThatThrownBy(() -> aiStepsJson.read(blank)).isInstanceOf(StepParseException.class);
        assertThatThrownBy(() -> aiStepsJson.read(tooLong)).isInstanceOf(StepParseException.class);
    }

    @Test
    void 단계가_없거나_JSON이_아니면_거절한다() {
        assertThatThrownBy(() -> aiStepsJson.read("{\"steps\":[]}")).isInstanceOf(StepParseException.class);
        assertThatThrownBy(() -> aiStepsJson.read("단계를 나눠 봤어요")).isInstanceOf(StepParseException.class);
    }

    @Test
    void 쓴_JSON을_다시_읽으면_같다() {
        AiStepsResult result = new AiStepsResult("넷플릭스", List.of(new AiStep(1, "홈", "프로필", "top-right")));

        assertThat(aiStepsJson.read(aiStepsJson.write(result))).isEqualTo(result);
    }
}
