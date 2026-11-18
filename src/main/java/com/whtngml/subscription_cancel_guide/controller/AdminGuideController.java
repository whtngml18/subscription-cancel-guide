package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.dto.GuideStepForm;
import com.whtngml.subscription_cancel_guide.dto.SubscriptionForm;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.FeedbackService;
import com.whtngml.subscription_cancel_guide.service.GuideAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/guides")
@RequiredArgsConstructor
public class AdminGuideController {

    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    private final GuideAdminService guideAdminService;
    private final FeedbackService feedbackService;

    // ---------- 서비스 목록 ----------

    @GetMapping
    public String list(Model model) {
        model.addAttribute("subscriptions", subscriptionRepository.findAll(Sort.by("name")));
        model.addAttribute("feedbackCounts", feedbackService.countAllBySubscription());
        return "admin/guides";
    }

    // ---------- 서비스 등록 ----------

    @GetMapping("/new")
    public String newSubscriptionForm(Model model) {
        model.addAttribute("form", new SubscriptionForm());
        return showSubscriptionForm(null, model);
    }

    @PostMapping("/new")
    public String createSubscription(@Valid @ModelAttribute("form") SubscriptionForm form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        checkCategory(form, bindingResult);
        checkSlug(form, null, bindingResult);
        if (bindingResult.hasErrors()) {
            return showSubscriptionForm(null, model);
        }

        Long id = guideAdminService.createSubscription(form);
        redirectAttributes.addFlashAttribute("message", "서비스를 등록했어요. 이제 해지 단계를 추가하세요.");
        return "redirect:/admin/guides/" + id + "/edit";
    }

    // ---------- 서비스 수정 ----------

    @GetMapping("/{id}/edit")
    public String editSubscriptionForm(@PathVariable Long id, Model model) {
        Subscription subscription = guideAdminService.getSubscription(id);
        model.addAttribute("form", SubscriptionForm.from(subscription));
        return showSubscriptionForm(subscription, model);
    }

    @PostMapping("/{id}/edit")
    public String updateSubscription(@PathVariable Long id,
                                     @Valid @ModelAttribute("form") SubscriptionForm form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Subscription subscription = guideAdminService.getSubscription(id);
        checkCategory(form, bindingResult);
        checkSlug(form, id, bindingResult);
        if (bindingResult.hasErrors()) {
            return showSubscriptionForm(subscription, model);
        }

        guideAdminService.updateSubscription(id, form);
        redirectAttributes.addFlashAttribute("message", "서비스 정보를 저장했어요.");
        return "redirect:/admin/guides/" + id + "/edit";
    }

    // ---------- 서비스 삭제 ----------

    @PostMapping("/{id}/delete")
    public String deleteSubscription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String name = guideAdminService.getSubscription(id).getName();
        guideAdminService.deleteSubscription(id);
        redirectAttributes.addFlashAttribute("message", "'" + name + "'을(를) 삭제했어요.");
        return "redirect:/admin/guides";
    }

    // ---------- 해지 단계 등록 ----------

    @GetMapping("/{id}/steps/new")
    public String newStepForm(@PathVariable Long id, Model model) {
        Subscription subscription = guideAdminService.getSubscription(id);
        GuideStepForm form = new GuideStepForm();
        form.setStepOrder(guideAdminService.getSteps(subscription).size() + 1);
        model.addAttribute("form", form);
        return showStepForm(subscription, null, model);
    }

    @PostMapping("/{id}/steps/new")
    public String createStep(@PathVariable Long id,
                             @Valid @ModelAttribute("form") GuideStepForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Subscription subscription = guideAdminService.getSubscription(id);
        checkStepOrder(id, form, null, bindingResult);
        if (bindingResult.hasErrors()) {
            return showStepForm(subscription, null, model);
        }

        guideAdminService.createStep(id, form);
        redirectAttributes.addFlashAttribute("message", form.getStepOrder() + "단계를 추가했어요.");
        return "redirect:/admin/guides/" + id + "/edit";
    }

    // ---------- 해지 단계 수정 ----------

    @GetMapping("/{id}/steps/{stepId}/edit")
    public String editStepForm(@PathVariable Long id, @PathVariable Long stepId, Model model) {
        Subscription subscription = guideAdminService.getSubscription(id);
        GuideStep step = guideAdminService.getStep(id, stepId);
        model.addAttribute("form", GuideStepForm.from(step));
        return showStepForm(subscription, stepId, model);
    }

    @PostMapping("/{id}/steps/{stepId}/edit")
    public String updateStep(@PathVariable Long id,
                             @PathVariable Long stepId,
                             @Valid @ModelAttribute("form") GuideStepForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Subscription subscription = guideAdminService.getSubscription(id);
        guideAdminService.getStep(id, stepId);
        checkStepOrder(id, form, stepId, bindingResult);
        if (bindingResult.hasErrors()) {
            return showStepForm(subscription, stepId, model);
        }

        guideAdminService.updateStep(id, stepId, form);
        redirectAttributes.addFlashAttribute("message", form.getStepOrder() + "단계를 저장했어요.");
        return "redirect:/admin/guides/" + id + "/edit";
    }

    // ---------- 해지 단계 삭제 ----------

    @PostMapping("/{id}/steps/{stepId}/delete")
    public String deleteStep(@PathVariable Long id, @PathVariable Long stepId,
                             RedirectAttributes redirectAttributes) {
        guideAdminService.deleteStep(id, stepId);
        redirectAttributes.addFlashAttribute("message", "단계를 삭제했어요.");
        return "redirect:/admin/guides/" + id + "/edit";
    }

    // ---------- 화면에 필요한 값 담기 ----------

    private String showSubscriptionForm(Subscription subscription, Model model) {
        model.addAttribute("subscription", subscription);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));
        if (subscription != null) {
            model.addAttribute("steps", guideAdminService.getSteps(subscription));
        }
        return "admin/subscription-form";
    }

    private String showStepForm(Subscription subscription, Long stepId, Model model) {
        model.addAttribute("subscription", subscription);
        model.addAttribute("stepId", stepId);
        return "admin/step-form";
    }

    private void checkCategory(SubscriptionForm form, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("categoryId") && !categoryRepository.existsById(form.getCategoryId())) {
            bindingResult.rejectValue("categoryId", "notFound", "없는 카테고리예요. 다시 골라 주세요.");
        }
    }

    private void checkSlug(SubscriptionForm form, Long excludeId, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("slug") && guideAdminService.isSlugTaken(form.getSlug(), excludeId)) {
            bindingResult.rejectValue("slug", "duplicate", "이미 쓰고 있는 slug예요.");
        }
    }

    private void checkStepOrder(Long id, GuideStepForm form, Long excludeStepId, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("stepOrder")
                && guideAdminService.isStepOrderTaken(id, form.getStepOrder(), excludeStepId)) {
            bindingResult.rejectValue("stepOrder", "duplicate", "이미 있는 단계 번호예요.");
        }
    }
}
