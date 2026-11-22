package com.whtngml.subscription_cancel_guide.web;

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
}
