package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.SubmissionStatus;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.SubmissionForm;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GuideAdminService guideAdminService;

    @Transactional
    public Long submit(SubmissionForm form) {
        Submission submission = new Submission(form.getServiceName().strip(), form.getRawText().strip());
        return submissionRepository.save(submission).getId();
    }

    // ---------- 관리자 검토 ----------

    public Page<Submission> getPendingPage(Pageable pageable) {
        return submissionRepository.findAllByStatus(SubmissionStatus.PENDING, pageable);
    }

    public long countPending() {
        return submissionRepository.countByStatus(SubmissionStatus.PENDING);
    }

    public Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // 제보한 서비스 이름과 같은 서비스가 이미 있는지
    public Optional<Subscription> findSameNameSubscription(Submission submission) {
        return subscriptionRepository.findFirstByNameIgnoreCase(submission.getServiceName());
    }

    // 서비스 수정 화면에 보여줄 제보 원문
    public List<Submission> getApprovedSubmissions(Subscription subscription) {
        return submissionRepository.findAllBySubscriptionOrderByCreatedAtDesc(subscription);
    }

    // 새 서비스를 만들고 승인한다. 서비스 생성과 승인이 함께 성공하거나 함께 취소된다.
    @Transactional
    public Long approveAsNew(Long submissionId, SubscriptionForm form) {
        Submission submission = getPendingSubmission(submissionId);
        Long subscriptionId = guideAdminService.createSubscription(form);
        Subscription subscription = guideAdminService.getSubscription(subscriptionId);
        subscription.markAsUserSubmission();
        submission.approve(subscription);
        return subscriptionId;
    }

    // 같은 이름의 기존 서비스에 연결해 승인한다
    @Transactional
    public Long approveToExisting(Long submissionId) {
        Submission submission = getPendingSubmission(submissionId);
        Subscription subscription = findSameNameSubscription(submission)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 이름의 서비스가 없어요."));
        submission.approve(subscription);
        return subscription.getId();
    }

    @Transactional
    public void reject(Long submissionId) {
        getPendingSubmission(submissionId).reject();
    }

    private Submission getPendingSubmission(Long id) {
        Submission submission = getSubmission(id);
        if (!submission.isPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리한 제보예요.");
        }
        return submission;
    }
}
