package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySlug(String slug);

    // 홈 전체 목록: 카테고리를 함께 읽어 서비스마다 추가 조회가 생기지 않게 한다
    @EntityGraph(attributePaths = "category")
    List<Subscription> findAllByOrderByNameAsc();

    Page<Subscription> findAllByCategory(Category category, Pageable pageable);

    Page<Subscription> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug, Pageable pageable);

    Optional<Subscription> findFirstByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
