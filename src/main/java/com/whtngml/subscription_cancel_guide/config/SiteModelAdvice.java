package com.whtngml.subscription_cancel_guide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 모든 화면에서 ${baseUrl}로 사이트 주소를 쓸 수 있게 한다
@ControllerAdvice
public class SiteModelAdvice {

    private final String baseUrl;

    public SiteModelAdvice(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @ModelAttribute("baseUrl")
    public String baseUrl() {
        return baseUrl;
    }
}
