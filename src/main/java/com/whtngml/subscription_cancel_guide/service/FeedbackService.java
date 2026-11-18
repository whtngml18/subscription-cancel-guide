package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Feedback;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.FeedbackCount;
import com.whtngml.subscription_cancel_guide.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public void save(Subscription subscription, boolean helpful) {
        feedbackRepository.save(new Feedback(subscription, helpful));
    }

    // key: 서비스 id. 피드백이 없는 서비스는 Map에 없다.
    public Map<Long, FeedbackCount> countAllBySubscription() {
        return feedbackRepository.countAllBySubscription().stream()
                .collect(Collectors.toMap(FeedbackCount::subscriptionId, Function.identity()));
    }
}
