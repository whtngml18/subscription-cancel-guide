package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "index";
    }
}