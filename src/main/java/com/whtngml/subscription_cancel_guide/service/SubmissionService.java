package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.ai.AiStepsJson;
import com.whtngml.subscription_cancel_guide.ai.StepParseException;
import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.SubmissionStatus;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.AiStep;
import com.whtngml.subscription_cancel_guide.dto.GuideStepForm;
import com.whtngml.subscription_cancel_guide.dto.SubmissionForm;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GuideAdminService guideAdminService;
    private final AiStepsJson aiStepsJson;

    @Transactional
    public Long submit(SubmissionForm form) {
        Submission submission = new Submission(form.getServiceName().strip(), form.getRawText().strip(),
                checkAiStepsJson(form.getAiStepsJson()), EnumSet.noneOf(CancelChannel.class), null, null);
        return submissionRepository.save(submission).getId();
    }

    // 화면에서 다시 보낸 AI 단계는 바뀌었을 수 있어서 한 번 더 검사한다. 틀리면 AI 단계 없이 글만 저장한다.
    private String checkAiStepsJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return aiStepsJson.write(aiStepsJson.read(json));
        } catch (StepParseException e) {
            log.warn("제보의 AI 단계가 형식에 맞지 않아 버림: {}", e.getMessage());
            return null;
        }
    }

    // 휴대폰 도식으로 보여 주거나 단계로 저장할 AI 단계 (없으면 빈 목록)
    public List<GuideStepForm> getAiSteps(Submission submission) {
        if (submission.getAiStepsJson() == null) {
            return List.of();
        }
        try {
            return aiStepsJson.read(submission.getAiStepsJson()).steps().stream().map(AiStep::toForm).toList();
        } catch (StepParseException e) {
            log.warn("저장된 AI 단계를 읽지 못함 (제보 {}): {}", submission.getId(), e.getMessage());
            return List.of();
        }
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
    public Long approveAsNew(Long submissionId, SubscriptionForm form, boolean importAiSteps) {
        Submission submission = getPendingSubmission(submissionId);
        Long subscriptionId = guideAdminService.createSubscription(form);
        Subscription subscription = guideAdminService.getSubscription(subscriptionId);
        subscription.markAsUserSubmission();
        submission.approve(subscription);
        importAiSteps(submission, subscriptionId, importAiSteps);
        return subscriptionId;
    }

    // 같은 이름의 기존 서비스에 연결해 승인한다
    @Transactional
    public Long approveToExisting(Long submissionId, boolean importAiSteps) {
        Submission submission = getPendingSubmission(submissionId);
        Subscription subscription = findSameNameSubscription(submission)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 이름의 서비스가 없어요."));
        submission.approve(subscription);
        importAiSteps(submission, subscription.getId(), importAiSteps);
        return subscription.getId();
    }

    // 체크했고 AI 단계가 있으면 서비스의 해지 단계를 AI 단계로 바꾼다
    private void importAiSteps(Submission submission, Long subscriptionId, boolean importAiSteps) {
        List<GuideStepForm> steps = getAiSteps(submission);
        if (importAiSteps && !steps.isEmpty()) {
            guideAdminService.replaceSteps(subscriptionId, steps);
        }
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
