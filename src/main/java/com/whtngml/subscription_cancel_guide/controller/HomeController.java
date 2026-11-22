package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.service.GuideListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryRepository categoryRepository;
    private final GuideListService guideListService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("id")));  // 카테고리 탭
        model.addAttribute("groups", guideListService.groupedByCategory());          // 카테고리별 서비스 목록
        model.addAttribute("stepCounts", guideListService.stepCounts());
        return "index";
    }
}
