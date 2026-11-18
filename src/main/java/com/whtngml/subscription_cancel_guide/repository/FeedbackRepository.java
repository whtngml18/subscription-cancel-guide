package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Feedback;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.FeedbackCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 서비스별로 맞았어요·달라요 개수를 한 번에 센다
    @Query("""
            select new com.whtngml.subscription_cancel_guide.dto.FeedbackCount(
                f.subscription.id,
                sum(case when f.helpful = true then 1 else 0 end),
                sum(case when f.helpful = false then 1 else 0 end))
            from Feedback f
            group by f.subscription.id
            """)
    List<FeedbackCount> countAllBySubscription();

    void deleteAllBySubscription(Subscription subscription);
}
