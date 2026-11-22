package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.IconImageRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.IconValidatorTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 사용자 화면이 새 디자인으로 그려지는지 확인한다 (예시 데이터 data.sql: 넷플릭스 3단계, 티빙 0단계, 멜론 3단계)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 테스트에서 저장한 서비스·피드백은 끝나면 되돌린다
class PublicPagesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    IconImageRepository iconImageRepository;

    @Test
    void userPagesUseSiteCss() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/site.css")))
                .andExpect(content().string(containsString("pretendard")))
                .andExpect(content().string(not(containsString("bootstrap"))));
    }

    @Test
    void adminPagesKeepBootstrap() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("bootstrap")));
    }

    @Test
    void homeListsAllServicesWithStepCount() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("넷플릭스")))
                .andExpect(content().string(containsString("멜론")))
                .andExpect(content().string(containsString("3단계")))
                .andExpect(content().string(containsString("단계 준비 중")))   // 티빙
                .andExpect(content().string(containsString("찾는 서비스가 없나요?")));
    }

    @Test
    void homeTabsAreLinks() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("href=\"/category/music\"")))
                .andExpect(content().string(containsString("/js/home-filter.js")));
    }

    @Test
    void categoryPageHasBreadcrumbAndRows() throws Exception {
        mvc.perform(get("/category/ott")).andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"위치\"")))
                .andExpect(content().string(containsString("href=\"/guide/netflix\"")))
                .andExpect(content().string(containsString("svc-icon")));
    }

    @Test
    void searchEscapesKeyword() throws Exception {
        mvc.perform(get("/search").param("q", "<script>x</script>")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>x</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(containsString("가이드가 아직 없어요")))
                .andExpect(content().string(containsString("/submit?name=")));
    }

    @Test
    void iconWithoutBrandColor() throws Exception {
        Category ott = categoryRepository.findBySlug("ott").orElseThrow();
        subscriptionRepository.save(new Subscription(ott, "왓챠", "watcha", null, null, null));
        mvc.perform(get("/category/ott")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/guide/watcha\"")))
                .andExpect(content().string(containsString("svc-icon")))
                .andExpect(content().string(not(containsString("--brand: null"))))
                .andExpect(content().string(not(containsString("--brand: ;"))));
    }

    @Test
    void guideShowsSummaryFirstAndStepsOnce() throws Exception {
        mvc.perform(get("/guide/netflix")).andExpect(status().isOk())
                .andExpect(content().string(containsString("넷플릭스 해지 방법")))
                .andExpect(content().string(containsString("웹에서 3단계로 해지할 수 있어요.")))
                .andExpect(content().string(containsString("class=\"steps\"")))
                .andExpect(content().string(not(containsString("phone-list"))))
                .andExpect(content().string(containsString("aria-label=\"위치\"")))
                .andExpect(content().string(containsString("application/ld+json")));   // SEO 유지
    }

    @Test
    void refundInfoHidesMissingNotice() throws Exception {
        // 멜론은 환불 정보만 있으므로 "아직 확인된 정보가 없어요"는 해지하는 곳, 난이도·시간 두 군데만
        String html = mvc.perform(get("/guide/melon")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("해지하면 다음 결제일부터 요금이 나가지 않아요."));
        assertEquals(2, html.split("아직 확인된 정보가 없어요", -1).length - 1);
    }

    @Test
    void guideShowsNewDetails() throws Exception {
        mvc.perform(get("/guide/netflix")).andExpect(status().isOk())
                .andExpect(content().string(containsString("쉬움")))
                .andExpect(content().string(containsString("chip-easy")))
                .andExpect(content().string(containsString("약 3분")))
                .andExpect(content().string(containsString("공식 해지 페이지 열기")))
                .andExpect(content().string(containsString("rel=\"noopener noreferrer\"")))
                .andExpect(content().string(containsString("앱스토어나 구글플레이로 결제했다면")));
    }

    @Test
    void guideWithoutDetailsShowsMissing() throws Exception {
        mvc.perform(get("/guide/melon")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("class=\"caution\""))))
                .andExpect(content().string(not(containsString("공식 해지 페이지 열기"))))
                .andExpect(content().string(containsString("아직 확인된 정보가 없어요")));
    }

    @Test
    void homeRowShowsDetails() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("웹 · 3단계 · 약 3분")))
                .andExpect(content().string(containsString("앱·웹 · 단계 준비 중 · 약 5분")));
    }

    @Test
    void uploadedIconShownOnHome() throws Exception {
        IconImage icon = iconImageRepository.save(new IconImage(IconValidatorTest.PNG, "image/png"));
        subscriptionRepository.findBySlug("netflix").orElseThrow().changeIcon(icon);
        subscriptionRepository.flush();
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("src=\"/icons/" + icon.getId() + "\"")))
                .andExpect(content().string(containsString("<img class=\"svc-icon\"")));
    }

    @Test
    void guideWithoutSteps() throws Exception {
        mvc.perform(get("/guide/tving")).andExpect(status().isOk())
                .andExpect(content().string(containsString("아직 등록된 해지 단계가 없어요.")))
                .andExpect(content().string(not(containsString("0단계"))))
                .andExpect(content().string(not(containsString("application/ld+json"))));
    }

    @Test
    void submitErrorsAreLinkedToFields() throws Exception {
        mvc.perform(post("/submit").param("serviceName", "").param("rawText", "짧음").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"serviceName-error\"")))
                .andExpect(content().string(containsString("id=\"serviceName-error\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    }

    @Test
    void submitSavesDetails() throws Exception {
        mvc.perform(multipart("/submit")
                        .file(new MockMultipartFile("iconFile", "w.png", "image/png", IconValidatorTest.PNG))
                        .param("serviceName", "왓챠")
                        .param("rawText", "설정에서 멤버십 해지를 눌러요.")
                        .param("channels", "APP", "WEB")
                        .param("caution", "스토어 결제는 스토어에서")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Submission saved = submissionRepository.findAll().stream()
                .filter(s -> s.getServiceName().equals("왓챠")).findFirst().orElseThrow();
        assertEquals(EnumSet.of(CancelChannel.APP, CancelChannel.WEB), saved.getChannels());
        assertEquals("스토어 결제는 스토어에서", saved.getCaution());
        assertNotNull(saved.getIcon());
    }

    @Test
    void submitRejectsSvgIcon() throws Exception {
        long before = submissionRepository.count();
        mvc.perform(multipart("/submit")
                        .file(new MockMultipartFile("iconFile", "w.png", "image/png", IconValidatorTest.SVG))
                        .param("serviceName", "왓챠")
                        .param("rawText", "설정에서 멤버십 해지를 눌러요.")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PNG, JPEG, WebP 이미지만")))
                .andExpect(content().string(containsString("aria-describedby=\"iconFile-error iconFile-help\"")));
        assertEquals(before, submissionRepository.count());
    }

    @Test
    void submitPageHasMoreSection() throws Exception {
        mvc.perform(get("/submit"))
                .andExpect(content().string(containsString("더 알려 주실 수 있나요?")))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")));
    }

    @Test
    void submitDoneOffersNextActions() throws Exception {
        mvc.perform(get("/submit/done"))
                .andExpect(content().string(containsString("다른 서비스 제보")));
    }

    @Test
    void guideShowsFeedbackCount() throws Exception {
        mvc.perform(post("/guide/netflix/feedback").param("helpful", "true").with(csrf()));
        mvc.perform(get("/guide/netflix"))
                .andExpect(content().string(containsString("1명 중 1명이 됐다고 했어요")));
    }
}
