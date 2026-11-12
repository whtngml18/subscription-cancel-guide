package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/guides")
    public String guides(Model model) {
        List<Subscription> subscriptions = subscriptionRepository.findAll(Sort.by("name"));
        model.addAttribute("subscriptions", subscriptions);
        return "admin/guides";
    }
}
