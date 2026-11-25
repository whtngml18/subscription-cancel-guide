package com.whtngml.subscription_cancel_guide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class SubmissionScreenshotCleanup {
    private final SubmissionService submissions;

    @Scheduled(cron = "0 0 3 * * *")
    void deleteExpired() {
        submissions.deleteExpiredScreenshots(LocalDateTime.now().minusDays(30));
    }
}
