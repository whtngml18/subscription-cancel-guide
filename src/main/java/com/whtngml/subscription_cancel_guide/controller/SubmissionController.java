package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.ai.AiPreviewRateLimiter;
import com.whtngml.subscription_cancel_guide.ai.AiStepsJson;
import com.whtngml.subscription_cancel_guide.ai.StepParseException;
import com.whtngml.subscription_cancel_guide.ai.StepParser;
import com.whtngml.subscription_cancel_guide.dto.AiStep;
import com.whtngml.subscription_cancel_guide.dto.AiStepsResult;
import com.whtngml.subscription_cancel_guide.dto.SubmissionForm;
import com.whtngml.subscription_cancel_guide.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final StepParser stepParser;
    private final AiStepsJson aiStepsJson;
    private final AiPreviewRateLimiter rateLimiter;

    @ModelAttribute("aiAvailable")
    public boolean aiAvailable() {
        return stepParser.isAvailable();
    }

    // 검색 결과가 없을 때 /submit?name=검색어 로 들어오면 서비스 이름을 미리 채운다
    @GetMapping
    public String form(@RequestParam(required = false) String name, Model model) {
        SubmissionForm form = new SubmissionForm();
        form.setServiceName(name);
        model.addAttribute("form", form);
        return "submit";
    }

    // AI로 글을 단계로 나눠 휴대폰 도식으로 미리 보여 준다. 저장은 하지 않는다.
    @PostMapping("/preview")
    public String preview(@Valid @ModelAttribute("form") SubmissionForm form, BindingResult bindingResult,
                          HttpServletRequest request, Model model) {
        form.setAiStepsJson(null);
        if (bindingResult.hasErrors()) {
            return "submit";
        }
        if (!stepParser.isAvailable()) {
            model.addAttribute("aiError", "지금은 AI 미리보기를 쓸 수 없어요. 글만 보내도 괜찮아요.");
            return "submit";
        }
        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            model.addAttribute("aiError", "AI 미리보기는 1시간에 10번까지 쓸 수 있어요. 글만 보내도 괜찮아요.");
            return "submit";
        }

        try {
            AiStepsResult result = stepParser.parse(form.getServiceName().strip(), form.getRawText().strip());
            form.setAiStepsJson(aiStepsJson.write(result));
            model.addAttribute("aiSteps", result.steps().stream().map(AiStep::toForm).toList());
        } catch (StepParseException e) {
            model.addAttribute("aiError", e.getMessage());
        }
        return "submit";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") SubmissionForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            form.setAiStepsJson(null);
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
