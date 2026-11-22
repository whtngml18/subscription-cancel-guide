package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.IconImageRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// 제보를 승인할 때 해지하는 곳·주의할 점·아이콘을 가져오는지
@SpringBootTest
@Transactional
class SubmissionApproveTest {

    @Autowired
    SubmissionService submissionService;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    IconImageRepository iconImageRepository;

    private IconImage newIcon() {
        return iconImageRepository.save(new IconImage(IconValidatorTest.PNG, "image/png"));
    }

    private Submission submission(String name, String caution, IconImage icon) {
        return submissionRepository.save(new Submission(name, "설정에서 해지해요. 열 글자 이상",
                null, EnumSet.of(CancelChannel.APP), caution, icon));
    }

    private Subscription netflix() {
        return subscriptionRepository.findBySlug("netflix").orElseThrow();
    }

    private SubscriptionForm watchaForm() {
        SubscriptionForm form = new SubscriptionForm();
        form.setCategoryId(categoryRepository.findBySlug("ott").orElseThrow().getId());
        form.setName("왓챠");
        form.setSlug("watcha");
        return form;
    }

    @Test
    void approveExistingWithImportCopiesValues() {
        IconImage icon = newIcon();
        Submission s = submission("넷플릭스", "스토어 결제 주의", icon);

        submissionService.approveToExisting(s.getId(), false, true, true);

        assertEquals(EnumSet.of(CancelChannel.APP), netflix().getChannels());
        assertEquals("스토어 결제 주의", netflix().getCaution());
        assertEquals(icon.getId(), netflix().getIcon().getId());
    }

    @Test
    void approveExistingWithoutImportKeepsValues() {
        String caution = netflix().getCaution();
        Submission s = submission("넷플릭스", "스토어 결제 주의", newIcon());

        submissionService.approveToExisting(s.getId(), false, false, false);

        assertEquals(EnumSet.of(CancelChannel.WEB), netflix().getChannels());
        assertEquals(caution, netflix().getCaution());
        assertNull(netflix().getIcon());
    }

    @Test
    void importSkipsEmptyFields() {
        String caution = netflix().getCaution();
        Submission s = submission("넷플릭스", null, null);

        submissionService.approveToExisting(s.getId(), false, true, true);

        assertEquals(EnumSet.of(CancelChannel.APP), netflix().getChannels());
        assertEquals(caution, netflix().getCaution());
    }

    @Test
    void approveAsNewImportsDetailsAndIcon() {
        IconImage icon = newIcon();
        Submission s = submission("왓챠", "스토어 결제 주의", icon);

        Long id = submissionService.approveAsNew(s.getId(), watchaForm(), null, false, true, true);

        Subscription watcha = subscriptionRepository.findById(id).orElseThrow();
        assertEquals(EnumSet.of(CancelChannel.APP), watcha.getChannels());
        assertEquals("스토어 결제 주의", watcha.getCaution());
        assertEquals(icon.getId(), watcha.getIcon().getId());
    }

    @Test
    void uploadedIconWinsOverSubmissionIcon() {
        IconImage uploaded = newIcon();
        Submission s = submission("왓챠", null, newIcon());

        Long id = submissionService.approveAsNew(s.getId(), watchaForm(), uploaded, false, true, true);

        assertEquals(uploaded.getId(), subscriptionRepository.findById(id).orElseThrow().getIcon().getId());
    }
}
