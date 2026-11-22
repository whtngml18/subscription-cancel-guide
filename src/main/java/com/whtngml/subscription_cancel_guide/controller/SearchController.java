package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.GuideListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private static final int MAX_PAGE_SIZE = 10;

    private final SubscriptionRepository subscriptionRepository;
    private final GuideListService guideListService;

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         Model model) {
        String keyword = q.strip();
        if (keyword.isEmpty()) {
            return "redirect:/";
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("name"));
        Page<Subscription> result = subscriptionRepository
                .findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(keyword, keyword, pageable);

        model.addAttribute("keyword", keyword);
        model.addAttribute("page", result);
        model.addAttribute("stepCounts", guideListService.stepCounts());
        return "search";
    }
}
