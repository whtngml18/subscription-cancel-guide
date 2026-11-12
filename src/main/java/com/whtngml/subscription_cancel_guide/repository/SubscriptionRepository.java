package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySlug(String slug);

    List<Subscription> findAllByCategory(Category category);

    List<Subscription> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug);
}

