package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuideSummaryTest {

    private static GuideStep step(int order, String screen, String target) {
        return new GuideStep(null, order, screen, target, "middle", null);
    }

    @Test
    void sentenceAndRoute() {
        GuideSummary summary = GuideSummary.of(List.of(
                step(1, "홈", "프로필"), step(2, "프로필", "계정"), step(3, "계정", "멤버십 해지")));
        assertEquals("3단계로 해지할 수 있어요.", summary.sentence());
        assertEquals("홈 › 프로필 › 계정 › 멤버십 해지", summary.route());
    }

    @Test
    void repeatedScreenAppearsOnce() {
        GuideSummary summary = GuideSummary.of(List.of(step(1, "설정", "더보기"), step(2, "설정", "해지")));
        assertEquals("설정 › 해지", summary.route());
    }

    @Test
    void noSteps() {
        GuideSummary summary = GuideSummary.of(List.of());
        assertEquals("아직 등록된 해지 단계가 없어요.", summary.sentence());
        assertNull(summary.route());
    }
}
