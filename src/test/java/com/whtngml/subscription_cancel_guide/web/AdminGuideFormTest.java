package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Difficulty;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import com.whtngml.subscription_cancel_guide.repository.GuideStepRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.IconValidatorTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 서비스 수정 폼의 새 칸과 아이콘 업로드
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminGuideFormTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    GuideStepRepository guideStepRepository;

    private Subscription netflix() {
        return subscriptionRepository.findBySlug("netflix").orElseThrow();
    }

    // 넷플릭스 수정 폼에 기존 필수 칸을 채운 요청
    private MockMultipartHttpServletRequestBuilder editNetflix() {
        Subscription netflix = netflix();
        MockMultipartHttpServletRequestBuilder request = multipart("/admin/guides/{id}/edit", netflix.getId());
        request.param("categoryId", netflix.getCategory().getId().toString())
                .param("name", "넷플릭스")
                .param("slug", "netflix")
                .param("brandColor", "#E50914")
                .with(csrf());
        return request;
    }

    private Subscription reload() {
        entityManager.flush();
        entityManager.clear();
        return netflix();
    }

    @Test
    void savesNewDetails() throws Exception {
        mvc.perform(editNetflix()
                        .param("channels", "PHONE", "APP")
                        .param("caution", "주의")
                        .param("difficulty", "HARD")
                        .param("officialCancelUrl", "https://ex.com/cancel")
                        .param("estimatedMinutes", "10"))
                .andExpect(status().is3xxRedirection());

        Subscription saved = reload();
        assertEquals(EnumSet.of(CancelChannel.APP, CancelChannel.PHONE), saved.getChannels());
        assertEquals("주의", saved.getCaution());
        assertEquals(Difficulty.HARD, saved.getDifficulty());
        assertEquals("https://ex.com/cancel", saved.getOfficialCancelUrl());
        assertEquals(10, saved.getEstimatedMinutes());
    }

    @Test
    void rejectsJavascriptUrl() throws Exception {
        mvc.perform(editNetflix().param("officialCancelUrl", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("공식 해지 링크는 https://로 시작")));
        assertEquals("https://www.netflix.com/cancelplan", reload().getOfficialCancelUrl());
    }

    @Test
    void rejectsMinutesOutOfRange() throws Exception {
        mvc.perform(editNetflix().param("estimatedMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1분에서 120분")));
    }

    @Test
    void uploadsIcon() throws Exception {
        mvc.perform(editNetflix().file(new MockMultipartFile("iconFile", "a.png", "image/png", IconValidatorTest.PNG)))
                .andExpect(status().is3xxRedirection());
        assertNotNull(reload().getIcon());
    }

    @Test
    void rejectsSvgIconOnAdminForm() throws Exception {
        mvc.perform(editNetflix().file(new MockMultipartFile("iconFile", "a.png", "image/png", IconValidatorTest.SVG)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PNG, JPEG, WebP 이미지만")));
        assertNull(reload().getIcon());
    }

    private java.util.Optional<GuideStep> stepNine() {
        entityManager.flush();
        entityManager.clear();
        return guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(netflix()).stream()
                .filter(step -> step.getStepOrder() == 9).findFirst();
    }

    @Test
    void savesStepOtherMenus() throws Exception {
        mvc.perform(post("/admin/guides/{id}/steps/new", netflix().getId())
                        .param("stepOrder", "9")
                        .param("screenName", "설정")
                        .param("targetLabel", "멤버십 해지")
                        .param("position", "bottom")
                        .param("otherMenus", "프로필\n알림")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertEquals("프로필\n알림", stepNine().orElseThrow().getOtherMenus());
    }

    @Test
    void rejectsTooLongOtherMenus() throws Exception {
        mvc.perform(post("/admin/guides/{id}/steps/new", netflix().getId())
                        .param("stepOrder", "9")
                        .param("screenName", "설정")
                        .param("targetLabel", "멤버십 해지")
                        .param("otherMenus", "가".repeat(301))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("다른 메뉴는 300자까지 쓸 수 있어요.")));

        assertTrue(stepNine().isEmpty());
    }

    @Test
    void newStepPreviewHasNoNull() throws Exception {
        mvc.perform(get("/admin/guides/{id}/steps/new", netflix().getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("null"))));
    }
}
