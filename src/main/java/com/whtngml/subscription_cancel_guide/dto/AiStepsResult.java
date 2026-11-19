package com.whtngml.subscription_cancel_guide.dto;

import java.util.List;

// AI가 해지 방법 글을 단계로 나눈 결과
public record AiStepsResult(String service, List<AiStep> steps) {
}
