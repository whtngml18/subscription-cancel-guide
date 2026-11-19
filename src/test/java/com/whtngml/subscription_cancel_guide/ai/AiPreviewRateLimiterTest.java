package com.whtngml.subscription_cancel_guide.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AiPreviewRateLimiterTest {

    // 테스트에서 시간을 앞으로 돌릴 수 있는 시계
    static class FakeClock extends Clock {
        Instant now = Instant.parse("2026-11-19T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void 한_시간에_10번까지만_허용하고_시간이_지나면_다시_허용한다() {
        FakeClock clock = new FakeClock();
        AiPreviewRateLimiter limiter = new AiPreviewRateLimiter(clock);

        for (int i = 0; i < AiPreviewRateLimiter.LIMIT; i++) {
            assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        }
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();

        clock.now = clock.now.plus(AiPreviewRateLimiter.WINDOW).plusSeconds(1);
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    }
}
