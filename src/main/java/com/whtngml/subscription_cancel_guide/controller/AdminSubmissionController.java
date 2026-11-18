package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.service.GuideAdminService;
import com.whtngml.subscription_cancel_guide.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/submissions")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private static final int PAGE_SIZE = 10;

    private final SubmissionService submissionService;
    private final GuideAdminService guideAdminService;
    private final CategoryRepository categoryRepository;

    // ---------- 검토 대기 목록 ----------

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("page", submissionService.getPendingPage(pageable));
        return "admin/submissions";
    }

    // ---------- 제보 상세 ----------

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Submission submission = submissionService.getSubmission(id);
        SubscriptionForm form = new SubscriptionForm();
        form.setName(submission.getServiceName());
        form.setVerifiedAt(LocalDate.now());
        model.addAttribute("form", form);
        return showDetail(submission, model);
    }

    // ---------- 승인 ----------

    @PostMapping("/{id}/approve")
    public String approveAsNew(@PathVariable Long id,
                               @Valid @ModelAttribute("form") SubscriptionForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Submission submission = submissionService.getSubmission(id);
        if (!bindingResult.hasFieldErrors("categoryId") && !categoryRepository.existsById(form.getCategoryId())) {
            bindingResult.rejectValue("categoryId", "notFound", "없는 카테고리예요. 다시 골라 주세요.");
        }
        if (!bindingResult.hasFieldErrors("slug") && guideAdminService.isSlugTaken(form.getSlug(), null)) {
            bindingResult.rejectValue("slug", "duplicate", "이미 쓰고 있는 slug예요.");
        }
        if (bindingResult.hasErrors()) {
            return showDetail(submission, model);
        }

        Long subscriptionId = submissionService.approveAsNew(id, form);
        redirectAttributes.addFlashAttribute("message", "제보를 승인했어요. 아래 제보 원문을 보고 해지 단계를 추가하세요.");
        return "redirect:/admin/guides/" + subscriptionId + "/edit";
    }

    @PostMapping("/{id}/approve-existing")
    public String approveToExisting(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long subscriptionId = submissionService.approveToExisting(id);
        redirectAttributes.addFlashAttribute("message", "기존 서비스로 승인했어요. 제보 원문을 보고 해지 단계를 고치세요.");
        return "redirect:/admin/guides/" + subscriptionId + "/edit";
    }

    // ---------- 반려 ----------

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        submissionService.reject(id);
        redirectAttributes.addFlashAttribute("message", "제보를 반려했어요.");
        return "redirect:/admin/submissions";
    }

    private String showDetail(Submission submission, Model model) {
        model.addAttribute("submission", submission);
        model.addAttribute("sameName", submissionService.findSameNameSubscription(submission).orElse(null));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        return "admin/submission";
    }
}
