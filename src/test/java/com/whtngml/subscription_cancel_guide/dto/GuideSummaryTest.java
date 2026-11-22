package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuideSummaryTest {

    private static final EnumSet<CancelChannel> NONE = EnumSet.noneOf(CancelChannel.class);

    private static List<GuideStep> threeSteps() {
        return List.of(step(1, "홈", "프로필"), step(2, "프로필", "계정"), step(3, "계정", "멤버십 해지"));
    }

    private static GuideStep step(int order, String screen, String target) {
        return new GuideStep(null, order, screen, target, "middle", null);
    }

    @Test
    void sentenceAndRoute() {
        GuideSummary summary = GuideSummary.of(List.of(
                step(1, "홈", "프로필"), step(2, "프로필", "계정"), step(3, "계정", "멤버십 해지")), NONE);
        assertEquals("3단계로 해지할 수 있어요.", summary.sentence());
        assertEquals("홈 › 프로필 › 계정 › 멤버십 해지", summary.route());
    }

    @Test
    void repeatedScreenAppearsOnce() {
        GuideSummary summary = GuideSummary.of(List.of(step(1, "설정", "더보기"), step(2, "설정", "해지")), NONE);
        assertEquals("설정 › 해지", summary.route());
    }

    @Test
    void noSteps() {
        GuideSummary summary = GuideSummary.of(List.of(), NONE);
        assertEquals("아직 등록된 해지 단계가 없어요.", summary.sentence());
        assertNull(summary.route());
    }

    @Test
    void sentenceWithChannels() {
        assertEquals("앱·웹에서 3단계로 해지할 수 있어요.",
                GuideSummary.of(threeSteps(), EnumSet.of(CancelChannel.WEB, CancelChannel.APP)).sentence());
    }

    @Test
    void noStepsIgnoresChannels() {
        assertEquals("아직 등록된 해지 단계가 없어요.",
                GuideSummary.of(List.of(), EnumSet.of(CancelChannel.APP)).sentence());
    }

    @Test
    void rowMetaSkipsEmptyParts() {
        assertEquals("웹 · 3단계 · 약 3분", GuideSummary.rowMeta(EnumSet.of(CancelChannel.WEB), 3, 3));
        assertEquals("단계 준비 중", GuideSummary.rowMeta(NONE, 0, null));
    }
}
