package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.ai.AiPreviewRateLimiter;
import org.springframework.stereotype.Component;

// 누구나 보낼 수 있는 제보(아이콘 포함)로 DB가 가득 차지 않게, IP 하나당 1시간에 10번까지만 받는다.
// AI 미리보기와 횟수를 따로 센다 (같은 규칙의 다른 기록).
@Component
public class SubmitRateLimiter {

    private final AiPreviewRateLimiter counter = new AiPreviewRateLimiter();

    public boolean tryAcquire(String key) {
        return counter.tryAcquire(key);
    }
}
