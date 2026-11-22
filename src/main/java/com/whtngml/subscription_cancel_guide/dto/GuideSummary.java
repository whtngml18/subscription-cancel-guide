package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.GuideStep;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 가이드 화면 위쪽 요약.
 * sentence: "앱·웹에서 3단계로 해지할 수 있어요." 같은 한 줄 요약
 * route: 지나가는 화면을 › 로 이은 경로 (예: "홈 › 프로필 › 계정 › 멤버십 해지"). 단계가 없으면 null
 */
public record GuideSummary(String sentence, String route) {

    private static final String SEPARATOR = " › ";

    public static GuideSummary of(List<GuideStep> steps, Set<CancelChannel> channels) {
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

        String where = channelText(channels);
        String sentence = (where == null ? "" : where + "에서 ") + steps.size() + "단계로 해지할 수 있어요.";
        return new GuideSummary(sentence, String.join(SEPARATOR, parts));
    }

    // 해지하는 곳을 "앱·웹"처럼 잇는다 (없으면 null)
    public static String channelText(Set<CancelChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(channels).stream()
                .map(CancelChannel::getLabel)
                .collect(Collectors.joining("·"));
    }

    // 목록 한 줄의 보조 글: "웹 · 3단계 · 약 3분" (없는 칸은 뺀다)
    public static String rowMeta(Set<CancelChannel> channels, long stepCount, Integer estimatedMinutes) {
        List<String> parts = new ArrayList<>();
        String where = channelText(channels);
        if (where != null) {
            parts.add(where);
        }
        parts.add(stepCount > 0 ? stepCount + "단계" : "단계 준비 중");
        if (estimatedMinutes != null) {
            parts.add("약 " + estimatedMinutes + "분");
        }
        return String.join(" · ", parts);
    }
}
