package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.Subscription;

import java.util.ArrayList;
import java.util.List;

// 관리자 가이드 목록의 "빈 정보" 칸: 아직 채우지 않은 항목 이름 (주의할 점은 없는 게 정상일 수 있어 넣지 않는다)
public final class MissingInfo {

    private MissingInfo() {
    }

    public static List<String> of(Subscription s, long stepCount) {
        List<String> missing = new ArrayList<>();
        if (stepCount == 0) {
            missing.add("단계");
        }
        if (s.getIcon() == null) {
            missing.add("아이콘");
        }
        if (s.getChannels().isEmpty()) {
            missing.add("해지하는 곳");
        }
        if (s.getDifficulty() == null) {
            missing.add("난이도");
        }
        if (s.getEstimatedMinutes() == null) {
            missing.add("예상 시간");
        }
        if (isBlank(s.getOfficialCancelUrl())) {
            missing.add("공식 링크");
        }
        if (isBlank(s.getRefundInfo())) {
            missing.add("환불");
        }
        return missing;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
