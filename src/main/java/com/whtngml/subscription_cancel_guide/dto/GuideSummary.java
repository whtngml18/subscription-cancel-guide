package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;

import java.util.ArrayList;
import java.util.List;

/**
 * 가이드 화면 위쪽 요약.
 * sentence: "3단계로 해지할 수 있어요." 같은 한 줄 요약
 * route: 지나가는 화면을 › 로 이은 경로 (예: "홈 › 프로필 › 계정 › 멤버십 해지"). 단계가 없으면 null
 */
public record GuideSummary(String sentence, String route) {

    private static final String SEPARATOR = " › ";

    public static GuideSummary of(List<GuideStep> steps) {
        if (steps.isEmpty()) {
            return new GuideSummary("아직 등록된 해지 단계가 없어요.", null);
        }

        List<String> parts = new ArrayList<>();
        for (GuideStep step : steps) {
            // 같은 화면에서 버튼을 여러 번 누르면 화면 이름은 한 번만
            if (parts.isEmpty() || !parts.get(parts.size() - 1).equals(step.getScreenName())) {
                parts.add(step.getScreenName());
            }
        }
        parts.add(steps.get(steps.size() - 1).getTargetLabel());

        return new GuideSummary(steps.size() + "단계로 해지할 수 있어요.", String.join(SEPARATOR, parts));
    }
}
