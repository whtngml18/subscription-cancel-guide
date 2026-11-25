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
    private static final int MAX_MENUS_LENGTH = 300;
    private static final int MAX_MENUS = 4;
    private static final int MAX_NOTE_LENGTH = 100;

    private final JsonMapper jsonMapper;

    public AiStepsJson(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 형식이 틀리면 StepParseException. 앞뒤 공백을 지우고 order 순서로 정렬해 1부터 다시 번호를 매긴다.
    public AiStepsResult read(String json) {
        return read(json, 0);
    }

    public AiStepsResult read(String json, int screenshotCount) {
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
        if (sorted.contains(null)) {
            throw new StepParseException("AI 단계가 비어 있어요.");
        }
        sorted.sort(Comparator.comparingInt(AiStep::order));

        List<AiStep> steps = new ArrayList<>();
        for (AiStep step : sorted) {
            String screen = checkLabel(step.screen(), "화면 이름");
            String target = checkLabel(step.target(), "버튼 이름");
            if (step.position() == null || !POSITIONS.contains(step.position())) {
                throw new StepParseException("버튼 위치가 정해진 값이 아니에요.");
            }
            // 화면·버튼 이름에 개인정보가 섞인 단계는 통째로 뺀다
            if (PersonalInfo.contains(screen) || PersonalInfo.contains(target)) {
                continue;
            }
            Integer image = step.image();
            if (image != null && (image < 1 || image > screenshotCount)) {
                image = null;
            }
            String note = screenshotCount > 0 ? cleanNote(step.note()) : null;
            steps.add(new AiStep(steps.size() + 1, screen, target, step.position(), cleanMenus(step.otherMenus()),
                    image, note));
        }
        if (steps.isEmpty()) {
            throw new StepParseException("AI가 단계를 하나도 만들지 못했어요.");
        }
        String service = result.service();
        return new AiStepsResult(service == null || PersonalInfo.contains(service) ? null : service.strip(), steps);
    }

    public String write(AiStepsResult result) {
        return jsonMapper.writeValueAsString(result);
    }

    // 다른 메뉴는 그림을 채우는 보조 정보라 틀려도 거절하지 않고 다듬는다: 빈 값 제거, 50자 자르기, 줄바꿈으로 이어 300자까지
    private List<String> cleanMenus(List<String> menus) {
        if (menus == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        int length = 0;
        for (String menu : menus) {
            if (menu == null || menu.isBlank() || PersonalInfo.contains(menu)) {
                continue;
            }
            String stripped = menu.strip();
            if (stripped.length() > MAX_LABEL_LENGTH) {
                stripped = stripped.substring(0, MAX_LABEL_LENGTH);
            }
            int added = cleaned.isEmpty() ? stripped.length() : stripped.length() + 1;
            if (length + added > MAX_MENUS_LENGTH) {
                break;
            }
            cleaned.add(stripped);
            length += added;
            if (cleaned.size() == MAX_MENUS) {
                break;
            }
        }
        return cleaned;
    }

    private String cleanNote(String note) {
        if (note == null || note.isBlank() || PersonalInfo.contains(note)) {
            return null;
        }
        String stripped = note.strip();
        return stripped.length() <= MAX_NOTE_LENGTH ? stripped : stripped.substring(0, MAX_NOTE_LENGTH);
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
