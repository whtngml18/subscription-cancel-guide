package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideStepRepository extends JpaRepository<GuideStep, Long> {

    List<GuideStep> findAllBySubscriptionOrderByStepOrderAsc(Subscription subscription);

    boolean existsBySubscriptionAndStepOrder(Subscription subscription, int stepOrder);

    boolean existsBySubscriptionAndStepOrderAndIdNot(Subscription subscription, int stepOrder, Long id);

    void deleteAllBySubscription(Subscription subscription);
}
