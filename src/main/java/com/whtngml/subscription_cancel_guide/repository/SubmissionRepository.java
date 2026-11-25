package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.SubmissionStatus;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select submission from Submission submission where submission.id = :id")
    Optional<Submission> lockById(Long id);

    Page<Submission> findAllByStatus(SubmissionStatus status, Pageable pageable);

    long countByStatus(SubmissionStatus status);

    List<Submission> findAllBySubscriptionOrderByCreatedAtDesc(Subscription subscription);
}
