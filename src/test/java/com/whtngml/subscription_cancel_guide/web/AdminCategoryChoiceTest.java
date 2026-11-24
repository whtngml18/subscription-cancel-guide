package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 서비스 등록·제보 승인 폼에서 고르는 "+ 새 카테고리"
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminCategoryChoiceTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    // 새 서비스 "왓챠"를 등록하는 요청 (카테고리는 각 테스트가 채운다)
    private MockMultipartHttpServletRequestBuilder newWatcha(String slug) {
        MockMultipartHttpServletRequestBuilder request = multipart("/admin/guides/new");
        request.param("name", "왓챠").param("slug", slug).with(csrf());
        return request;
    }

    private String ottId() {
        return categoryRepository.findBySlug("ott").orElseThrow().getId().toString();
    }

    @Test
    void formShowsNewCategoryChoice() throws Exception {
        mvc.perform(get("/admin/guides/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"0\"")))
                .andExpect(content().string(containsString("id=\"newCategoryFields\"")));
    }

    @Test
    void createsServiceWithNewCategory() throws Exception {
        mvc.perform(newWatcha("watcha")
                        .param("categoryId", "0")
                        .param("newCategoryName", "배달")
                        .param("newCategorySlug", "delivery"))
                .andExpect(status().is3xxRedirection());

        assertTrue(categoryRepository.existsBySlug("delivery"));
        assertEquals("delivery", subscriptionRepository.findBySlug("watcha").orElseThrow().getCategory().getSlug());
    }

    @Test
    void duplicateNewCategorySlugKeepsInput() throws Exception {
        mvc.perform(newWatcha("watcha")
                        .param("categoryId", "0")
                        .param("newCategoryName", "배달")
                        .param("newCategorySlug", "ott"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 있는 주소예요.")))
                .andExpect(content().string(containsString("value=\"배달\"")));

        assertFalse(subscriptionRepository.existsBySlug("watcha"));
    }

    @Test
    void serviceErrorDoesNotCreateCategory() throws Exception {
        mvc.perform(newWatcha("netflix")
                        .param("categoryId", "0")
                        .param("newCategoryName", "배달")
                        .param("newCategorySlug", "delivery"))
                .andExpect(status().isOk());

        assertFalse(categoryRepository.existsBySlug("delivery"));
    }

    @Test
    void existingCategoryIgnoresNewFields() throws Exception {
        mvc.perform(newWatcha("watcha")
                        .param("categoryId", ottId())
                        .param("newCategoryName", "배달")
                        .param("newCategorySlug", "delivery"))
                .andExpect(status().is3xxRedirection());

        assertEquals("ott", subscriptionRepository.findBySlug("watcha").orElseThrow().getCategory().getSlug());
        assertFalse(categoryRepository.existsBySlug("delivery"));
    }

    @Test
    void approvesSubmissionWithNewCategory() throws Exception {
        Submission submission = submissionRepository.save(new Submission("왓챠", "설정에서 해지해요. 열 글자 이상",
                null, EnumSet.of(CancelChannel.APP), null, null));

        mvc.perform(multipart("/admin/submissions/{id}/approve", submission.getId())
                        .param("name", "왓챠")
                        .param("slug", "watcha")
                        .param("categoryId", "0")
                        .param("newCategoryName", "배달")
                        .param("newCategorySlug", "delivery")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertEquals("delivery", subscriptionRepository.findBySlug("watcha").orElseThrow().getCategory().getSlug());
    }
}
