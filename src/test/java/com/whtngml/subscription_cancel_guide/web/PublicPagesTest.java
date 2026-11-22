package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
