package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
