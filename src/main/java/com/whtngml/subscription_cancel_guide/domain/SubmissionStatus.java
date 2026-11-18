package com.whtngml.subscription_cancel_guide.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubmissionStatus {

    PENDING("검토 대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String label;
}
