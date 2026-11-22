package com.whtngml.subscription_cancel_guide.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 해지 난이도 (가이드·목록의 색 칩)
@Getter
@RequiredArgsConstructor
public enum Difficulty {

    EASY("쉬움"),
    NORMAL("보통"),
    HARD("어려움");

    private final String label;
}
