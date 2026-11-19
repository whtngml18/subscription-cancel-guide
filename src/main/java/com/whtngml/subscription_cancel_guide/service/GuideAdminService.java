package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.GuideStepForm;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.FeedbackRepository;
import com.whtngml.subscription_cancel_guide.repository.GuideStepRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideAdminService {

    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GuideStepRepository guideStepRepository;
    private final FeedbackRepository feedbackRepository;
    private final SubmissionRepository submissionRepository;

    // ---------- 서비스 ----------

    public Subscription getSubscription(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public boolean isSlugTaken(String slug, Long excludeId) {
        if (excludeId == null) {
            return subscriptionRepository.existsBySlug(slug);
        }
        return subscriptionRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    @Transactional
    public Long createSubscription(SubscriptionForm form) {
        Subscription subscription = new Subscription(
                getCategory(form.getCategoryId()),
                form.getName(),
                form.getSlug(),
                form.getBrandColor(),
                form.getRefundInfo(),
                form.getVerifiedAt()
        );
        return subscriptionRepository.save(subscription).getId();
    }

    @Transactional
    public void updateSubscription(Long id, SubscriptionForm form) {
        Subscription subscription = getSubscription(id);
        subscription.update(
                getCategory(form.getCategoryId()),
                form.getName(),
                form.getSlug(),
                form.getBrandColor(),
                form.getRefundInfo(),
                form.getVerifiedAt()
        );
    }

    @Transactional
    public void deleteSubscription(Long id) {
        Subscription subscription = getSubscription(id);
        guideStepRepository.deleteAllBySubscription(subscription);
        feedbackRepository.deleteAllBySubscription(subscription);
        submissionRepository.findAllBySubscriptionOrderByCreatedAtDesc(subscription)
                .forEach(Submission::unlink);
        subscriptionRepository.delete(subscription);
    }

    // ---------- 해지 단계 ----------

    public List<GuideStep> getSteps(Subscription subscription) {
        return guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(subscription);
    }

    public GuideStep getStep(Long subscriptionId, Long stepId) {
        return guideStepRepository.findById(stepId)
                .filter(step -> step.getSubscription().getId().equals(subscriptionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public boolean isStepOrderTaken(Long subscriptionId, int stepOrder, Long excludeStepId) {
        Subscription subscription = getSubscription(subscriptionId);
        if (excludeStepId == null) {
            return guideStepRepository.existsBySubscriptionAndStepOrder(subscription, stepOrder);
        }
        return guideStepRepository.existsBySubscriptionAndStepOrderAndIdNot(subscription, stepOrder, excludeStepId);
    }

    @Transactional
    public void createStep(Long subscriptionId, GuideStepForm form) {
        GuideStep step = new GuideStep(
                getSubscription(subscriptionId),
                form.getStepOrder(),
                form.getScreenName(),
                form.getTargetLabel(),
                form.getPosition(),
                form.getDescription()
        );
        guideStepRepository.save(step);
    }

    // 기존 단계를 모두 지우고 AI가 나눈 단계로 바꾼다
    @Transactional
    public void replaceSteps(Long subscriptionId, List<GuideStepForm> forms) {
        guideStepRepository.deleteAllBySubscription(getSubscription(subscriptionId));
        forms.forEach(form -> createStep(subscriptionId, form));
    }

    @Transactional
    public void updateStep(Long subscriptionId, Long stepId, GuideStepForm form) {
        GuideStep step = getStep(subscriptionId, stepId);
        step.update(
                form.getStepOrder(),
                form.getScreenName(),
                form.getTargetLabel(),
                form.getPosition(),
                form.getDescription()
        );
    }

    @Transactional
    public void deleteStep(Long subscriptionId, Long stepId) {
        guideStepRepository.delete(getStep(subscriptionId, stepId));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("없는 카테고리예요."));
    }
}
