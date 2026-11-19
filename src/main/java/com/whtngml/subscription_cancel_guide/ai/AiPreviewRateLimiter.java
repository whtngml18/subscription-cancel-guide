package com.whtngml.subscription_cancel_guide.ai;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

// 누구나 누를 수 있는 AI 미리보기 버튼으로 크레딧이 바닥나지 않게, IP 하나당 1시간에 10번까지만 허용한다
@Component
public class AiPreviewRateLimiter {

    static final int LIMIT = 10;
    static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> history = new HashMap<>();
    private final Clock clock;

    public AiPreviewRateLimiter() {
        this(Clock.systemUTC());
    }

    AiPreviewRateLimiter(Clock clock) {
        this.clock = clock;
    }

    // 쓸 수 있으면 한 번 쓴 것으로 기록하고 true. 호출이 많지 않아 메서드 전체를 잠근다.
    public synchronized boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Instant from = now.minus(WINDOW);
        history.values().forEach(times -> {
            while (!times.isEmpty() && times.peekFirst().isBefore(from)) {
                times.pollFirst();
            }
        });
        history.values().removeIf(Deque::isEmpty);

        Deque<Instant> times = history.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (times.size() >= LIMIT) {
            return false;
        }
        times.addLast(now);
        return true;
    }
}
