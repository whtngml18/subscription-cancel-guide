package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GuideStepRepository extends JpaRepository<GuideStep, Long> {

    List<GuideStep> findAllBySubscriptionOrderByStepOrderAsc(Subscription subscription);

    boolean existsBySubscriptionAndStepOrder(Subscription subscription, int stepOrder);

    boolean existsBySubscriptionAndStepOrderAndIdNot(Subscription subscription, int stepOrder, Long id);

    void deleteAllBySubscription(Subscription subscription);

    // 서비스별 단계 수를 한 번에 센다. 각 행: [서비스 id(Long), 단계 수(Long)]
    @Query("select s.subscription.id, count(s) from GuideStep s group by s.subscription.id")
    List<Object[]> countStepsBySubscription();
}
