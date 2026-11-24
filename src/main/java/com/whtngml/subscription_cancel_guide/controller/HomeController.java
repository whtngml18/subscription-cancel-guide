package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.dto.CategoryGroup;
import com.whtngml.subscription_cancel_guide.service.GuideListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final GuideListService guideListService;

    @GetMapping("/")
    public String home(Model model) {
        List<CategoryGroup> groups = guideListService.groupedByCategory();             // 카테고리별 서비스 목록 (빈 카테고리 없음)
        model.addAttribute("categories", groups.stream().map(CategoryGroup::category).toList());  // 카테고리 탭
        model.addAttribute("groups", groups);
        model.addAttribute("stepCounts", guideListService.stepCounts());
        return "index";
    }
}
