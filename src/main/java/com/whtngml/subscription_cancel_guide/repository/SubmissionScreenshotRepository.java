package com.whtngml.subscription_cancel_guide.repository;

import com.whtngml.subscription_cancel_guide.domain.SubmissionScreenshot;
import com.whtngml.subscription_cancel_guide.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionScreenshotRepository extends JpaRepository<SubmissionScreenshot, Long> {
    Optional<SubmissionScreenshot> findBySubmissionIdAndImageOrder(Long submissionId, int imageOrder);

    @Query("""
            select screenshot.imageOrder from SubmissionScreenshot screenshot
            where screenshot.submission.id = :submissionId order by screenshot.imageOrder
            """)
    List<Integer> findOrders(Long submissionId);

    @Query("""
            select distinct submission.id from SubmissionScreenshot screenshot
            join screenshot.submission submission
            where submission.status = :status and submission.createdAt <= :cutoff
            order by submission.id
            """)
    List<Long> findExpiredSubmissionIds(SubmissionStatus status, LocalDateTime cutoff);

    @Modifying
    @Query("delete from SubmissionScreenshot screenshot where screenshot.submission.id = :submissionId")
    int deleteForSubmission(Long submissionId);
}
