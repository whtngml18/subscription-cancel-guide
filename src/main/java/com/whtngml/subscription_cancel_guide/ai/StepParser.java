package com.whtngml.subscription_cancel_guide.ai;

import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;

import java.util.List;

// 해지 방법 글을 단계로 나눈다. LLM 회사를 바꿔도 이 인터페이스만 구현하면 된다.
public interface StepParser {

    // API 키가 있어서 지금 쓸 수 있는지
    boolean isAvailable();

    default String providerName() {
        return "설정된 AI 서비스";
    }

    // 실패하면 StepParseException
    default AiStepsResult parse(String serviceName, String rawText) {
        return parse(serviceName, rawText, List.of());
    }

    AiStepsResult parse(String serviceName, String rawText, List<byte[]> screenshots);
}
