package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.SubmissionStatus;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findAllByStatus(SubmissionStatus status, Pageable pageable);

    long countByStatus(SubmissionStatus status);

    List<Submission> findAllBySubscriptionOrderByCreatedAtDesc(Subscription subscription);
}
