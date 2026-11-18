package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.dto.SubmissionForm;
import com.whtngml.subscription_cancel_guide.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/submit")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    // 검색 결과가 없을 때 /submit?name=검색어 로 들어오면 서비스 이름을 미리 채운다
    @GetMapping
    public String form(@RequestParam(required = false) String name, Model model) {
        SubmissionForm form = new SubmissionForm();
        form.setServiceName(name);
        model.addAttribute("form", form);
        return "submit";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") SubmissionForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "submit";
        }

        submissionService.submit(form);
        return "redirect:/submit/done";
    }

    @GetMapping("/done")
    public String done() {
        return "submit-done";
    }
}
