package com.whtngml.subscription_cancel_guide.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 해지하는 곳 (가이드 칩, 요약 문장, 제보 체크박스에 표시)
@Getter
@RequiredArgsConstructor
public enum CancelChannel {

    APP("앱"),
    WEB("웹"),
    PHONE("전화"),
    OTHER("기타");

    private final String label;
}
