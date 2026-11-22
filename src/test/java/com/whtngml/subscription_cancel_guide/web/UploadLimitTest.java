package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.config.UploadLimitAdvice;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 최종 검토에서 나온 업로드 문제: 2MB를 넘는 파일, 제보 무제한 저장
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UploadLimitTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    UploadLimitAdvice uploadLimitAdvice;

    @Test
    void tooLargeUploadGoesBackToForm() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/submit");
        request.addHeader("Referer", "http://localhost:8080/submit?name=x");
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        String view = uploadLimitAdvice.tooLarge(new MaxUploadSizeExceededException(2_000_000), request, flash);

        assertEquals("redirect:/submit?name=x", view);
        assertEquals("아이콘은 200KB까지 올릴 수 있어요.", flash.getFlashAttributes().get("uploadError"));
    }

    @Test
    void tooLargeUploadIgnoresOtherSiteReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/submit");
        request.addHeader("Referer", "https://evil.example/phish");
        assertEquals("redirect:/submit",
                uploadLimitAdvice.tooLarge(new MaxUploadSizeExceededException(2_000_000), request, new RedirectAttributesModelMap()));
    }

    @Test
    void submitIsRateLimitedPerIp() throws Exception {
        long before = submissionRepository.count();
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/submit").param("serviceName", "왓챠").param("rawText", "설정에서 해지를 눌러요 열 글자")
                            .with(csrf()).with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                    .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/submit").param("serviceName", "왓챠").param("rawText", "설정에서 해지를 눌러요 열 글자")
                        .with(csrf()).with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("제보는 1시간에 10번까지")));
        assertEquals(before + 10, submissionRepository.count());
    }
}
