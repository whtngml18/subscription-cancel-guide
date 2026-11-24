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
                new AiStep(1, "홈", "프로필", "top-right", List.of(), null, null),
                new AiStep(2, "계정", "멤버십 해지", "middle", List.of(), null, null));
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
        AiStepsResult result = new AiStepsResult("넷플릭스", List.of(new AiStep(1, "홈", "프로필", "top-right", List.of(), null, null)));

        assertThat(aiStepsJson.read(aiStepsJson.write(result))).isEqualTo(result);
    }

    @Test
    void otherMenus를_정리해서_읽는다() {
        String json = """
                {"steps":[{"order":1,"screen":"설정","target":"멤버십 해지","position":"bottom","otherMenus":[" 프로필 ","","알림"]}]}
                """;

        assertThat(aiStepsJson.read(json).steps().get(0).otherMenus()).containsExactly("프로필", "알림");
    }

    @Test
    void otherMenus가_없거나_null이면_빈_목록() {
        String missing = """
                {"steps":[{"order":1,"screen":"설정","target":"해지","position":"middle"}]}
                """;
        String nullMenus = """
                {"steps":[{"order":1,"screen":"설정","target":"해지","position":"middle","otherMenus":null}]}
                """;

        assertThat(aiStepsJson.read(missing).steps().get(0).otherMenus()).isEmpty();
        assertThat(aiStepsJson.read(nullMenus).steps().get(0).otherMenus()).isEmpty();
    }

    @Test
    void 긴_메뉴는_50자로_자르고_합쳐서_300자를_넘지_않는다() {
        String menus = String.join(",", java.util.Collections.nCopies(10, "\"" + "가".repeat(60) + "\""));
        String json = "{\"steps\":[{\"order\":1,\"screen\":\"설정\",\"target\":\"해지\",\"position\":\"middle\",\"otherMenus\":[" + menus + "]}]}";

        List<String> result = aiStepsJson.read(json).steps().get(0).otherMenus();

        assertThat(result.get(0)).hasSize(50);
        assertThat(String.join("\n", result).length()).isLessThanOrEqualTo(300);
    }

    @Test
    void 폼으로_바꿀_때_메뉴를_줄바꿈으로_잇는다() {
        assertThat(new AiStep(1, "설정", "해지", "middle", List.of("프로필", "알림"), null, null).toForm().getOtherMenus())
                .isEqualTo("프로필\n알림");
        assertThat(new AiStep(1, "설정", "해지", "middle", List.of(), null, null).toForm().getOtherMenus()).isNull();
    }

    @Test
    void image와_note는_1부에서_null() {
        AiStepsResult result = aiStepsJson.read("""
                {"steps":[{"order":1,"screen":"홈","target":"프로필","position":"top-right","image":2,"note":"다름"}]}""");

        assertThat(result.steps().getFirst().image()).isNull();
        assertThat(result.steps().getFirst().note()).isNull();
    }

    @Test
    void 개인정보_메뉴는_빼고_단계는_뺀다() {
        AiStepsResult result = aiStepsJson.read("""
                {"steps":[
                  {"order":1,"screen":"홈","target":"프로필","position":"top-right","otherMenus":["계정","me@test.com"]},
                  {"order":2,"screen":"계정","target":"010-1234-5678","position":"middle"}
                ]}""");

        assertThat(result.steps()).containsExactly(new AiStep(1, "홈", "프로필", "top-right", List.of("계정"), null, null));
    }

    @Test
    void 모두_개인정보면_거절() {
        assertThatThrownBy(() -> aiStepsJson.read("""
                {"steps":[{"order":1,"screen":"me@test.com","target":"해지","position":"middle"}]}"""))
                .isInstanceOf(StepParseException.class);
    }
}
