package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.GuideStepRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GuideController {

    private final SubscriptionRepository subscriptionRepository;
    private final GuideStepRepository guideStepRepository;
    private final FeedbackService feedbackService;

    @GetMapping("/guide/{slug}")
    public String guide(@PathVariable String slug, Model model) {
        Subscription subscription = findSubscription(slug);

        List<GuideStep> steps = guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(subscription);

        model.addAttribute("subscription", subscription);
        model.addAttribute("steps", steps);
        model.addAttribute("howTo", howTo(subscription, steps));
        return "guide";
    }

    // "이 방법이 맞았어요 / 달라요" 버튼
    @PostMapping("/guide/{slug}/feedback")
    public String feedback(@PathVariable String slug,
                           @RequestParam boolean helpful,
                           RedirectAttributes redirectAttributes) {
        feedbackService.save(findSubscription(slug), helpful);
        redirectAttributes.addFlashAttribute("feedbackDone", true);
        return "redirect:/guide/" + slug + "#feedback";
    }

    // 검색엔진이 해지 단계를 알아보도록 schema.org HowTo 구조화 데이터를 만든다 (화면에서 JSON-LD로 출력)
    private Map<String, Object> howTo(Subscription subscription, List<GuideStep> steps) {
        List<Map<String, Object>> howToSteps = steps.stream()
                .map(step -> {
                    Map<String, Object> howToStep = new LinkedHashMap<>();
                    howToStep.put("@type", "HowToStep");
                    howToStep.put("position", step.getStepOrder());
                    howToStep.put("name", "[" + step.getScreenName() + "] " + step.getTargetLabel());
                    howToStep.put("text", step.getDescription() != null ? step.getDescription()
                            : step.getScreenName() + " 화면에서 " + step.getTargetLabel() + "을(를) 눌러요.");
                    return howToStep;
                })
                .toList();

        Map<String, Object> howTo = new LinkedHashMap<>();
        howTo.put("@context", "https://schema.org");
        howTo.put("@type", "HowTo");
        howTo.put("name", subscription.getName() + " 해지 방법");
        howTo.put("step", howToSteps);
        return howTo;
    }

    private Subscription findSubscription(String slug) {
        return subscriptionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
