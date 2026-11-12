package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         @PageableDefault(size = 10, sort = "name") Pageable pageable,
                         Model model) {
        String keyword = q.strip();
        if (keyword.isEmpty()) {
            return "redirect:/";
        }

        Page<Subscription> page = subscriptionRepository
                .findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(keyword, keyword, pageable);

        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);
        return "search";
    }
}
