package com.whtngml.subscription_cancel_guide.ai;

import com.whtngml.subscription_cancel_guide.dto.AiStep;
import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// AI 단계 JSON을 읽고 검사한다. AI 응답, 제보 화면에서 다시 보낸 값, DB에 저장된 값 모두 이걸로 읽는다.
@Component
public class AiStepsJson {

    public static final Set<String> POSITIONS = Set.of("top-left", "top-right", "middle", "bottom");
    public static final int MAX_STEPS = 20;
    private static final int MAX_LABEL_LENGTH = 50;

    private final JsonMapper jsonMapper;

    public AiStepsJson(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 형식이 틀리면 StepParseException. 앞뒤 공백을 지우고 order 순서로 정렬해 1부터 다시 번호를 매긴다.
    public AiStepsResult read(String json) {
        AiStepsResult result;
        try {
            result = jsonMapper.readValue(json, AiStepsResult.class);
        } catch (JacksonException e) {
            throw new StepParseException("AI 응답이 JSON 형식이 아니에요.", e);
        }

        if (result == null || result.steps() == null || result.steps().isEmpty()) {
            throw new StepParseException("AI가 단계를 하나도 만들지 못했어요.");
        }
        if (result.steps().size() > MAX_STEPS) {
            throw new StepParseException("단계가 " + MAX_STEPS + "개보다 많아요.");
        }

        List<AiStep> sorted = new ArrayList<>(result.steps());
        sorted.sort(Comparator.comparingInt(AiStep::order));

        List<AiStep> steps = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            AiStep step = sorted.get(i);
            String screen = checkLabel(step.screen(), "화면 이름");
            String target = checkLabel(step.target(), "버튼 이름");
            if (step.position() == null || !POSITIONS.contains(step.position())) {
                throw new StepParseException("버튼 위치가 정해진 값이 아니에요: " + step.position());
            }
            steps.add(new AiStep(i + 1, screen, target, step.position()));
        }
        return new AiStepsResult(result.service() == null ? null : result.service().strip(), steps);
    }

    public String write(AiStepsResult result) {
        return jsonMapper.writeValueAsString(result);
    }

    private String checkLabel(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new StepParseException(name + "이 비어 있어요.");
        }
        String stripped = value.strip();
        if (stripped.length() > MAX_LABEL_LENGTH) {
            throw new StepParseException(name + "이 " + MAX_LABEL_LENGTH + "자보다 길어요.");
        }
        return stripped;
    }
}
