package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q, Model model) {
        String keyword = q.strip();
        if (keyword.isEmpty()) {
            return "redirect:/";
        }

        List<Subscription> results =
                subscriptionRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(keyword, keyword);

        model.addAttribute("keyword", keyword);
        model.addAttribute("results", results);
        return "search";
    }
}