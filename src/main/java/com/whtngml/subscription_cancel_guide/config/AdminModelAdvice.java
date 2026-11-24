package com.whtngml.subscription_cancel_guide.config;

import com.whtngml.subscription_cancel_guide.controller.AdminCategoryController;
import com.whtngml.subscription_cancel_guide.controller.AdminGuideController;
import com.whtngml.subscription_cancel_guide.controller.AdminSubmissionController;
import com.whtngml.subscription_cancel_guide.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 관리자 화면 머리글의 "제보 검토" 대기 개수 배지
@ControllerAdvice(assignableTypes = {AdminGuideController.class, AdminCategoryController.class, AdminSubmissionController.class})
@RequiredArgsConstructor
public class AdminModelAdvice {

    private final SubmissionService submissionService;

    @ModelAttribute("pendingCount")
    public long pendingCount() {
        return submissionService.countPending();
    }
}
