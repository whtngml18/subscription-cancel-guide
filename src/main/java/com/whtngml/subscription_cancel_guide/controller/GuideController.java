package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.GuideStepRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class GuideController {

    private final SubscriptionRepository subscriptionRepository;
    private final GuideStepRepository guideStepRepository;

    @GetMapping("/guide/{slug}")
    public String guide(@PathVariable String slug, Model model) {
        Subscription subscription = subscriptionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<GuideStep> steps = guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(subscription);

        model.addAttribute("subscription", subscription);
        model.addAttribute("steps", steps);
        return "guide";
    }
}
