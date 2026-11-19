package com.whtngml.subscription_cancel_guide.ai;

import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;

// 해지 방법 글을 단계로 나눈다. LLM 회사를 바꿔도 이 인터페이스만 구현하면 된다.
public interface StepParser {

    // API 키가 있어서 지금 쓸 수 있는지
    boolean isAvailable();

    // 실패하면 StepParseException
    AiStepsResult parse(String serviceName, String rawText);
}
