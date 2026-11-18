package com.whtngml.subscription_cancel_guide.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 가이드를 누가 만들었는지 (가이드 상세의 출처 배지에 표시)
@Getter
@RequiredArgsConstructor
public enum GuideSource {

    ADMIN("관리자 작성"),
    USER_SUBMISSION("사용자 제보");

    private final String label;
}
