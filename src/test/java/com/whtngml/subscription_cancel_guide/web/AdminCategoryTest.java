package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 카테고리 관리 페이지
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminCategoryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categoryService;

    @Autowired
    CategoryRepository categoryRepository;

    private Category category(String slug) {
        return categoryRepository.findBySlug(slug).orElseThrow();
    }

    @Test
    void listShowsRowsAndAddForm() throws Exception {
        mvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("쇼핑 멤버십")))
                .andExpect(content().string(containsString("서비스 ")))
                .andExpect(content().string(containsString("action=\"/admin/categories\"")));
    }

    @Test
    void addsCategory() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "배달").param("slug", "delivery"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "카테고리를 추가했어요."));
        assertTrue(categoryService.isSlugTaken("delivery"));
    }

    @Test
    void rejectsBadOrDuplicateSlug() throws Exception {
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "배달").param("slug", "Delivery"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("주소는 영어 소문자, 숫자, -로 30자까지 써 주세요.")));
        mvc.perform(post("/admin/categories").with(csrf()).param("name", "배달").param("slug", "ott"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 있는 주소예요.")));
    }

    @Test
    void renamesKeepingSlug() throws Exception {
        Long id = category("etc").getId();
        mvc.perform(get("/admin/categories/" + id + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("기타")));
        mvc.perform(post("/admin/categories/" + id + "/edit").with(csrf()).param("name", "그 밖"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "카테고리 이름을 고쳤어요."));
        assertEquals("그 밖", categoryService.get(id).getName());
        assertEquals("etc", categoryService.get(id).getSlug());
    }

    @Test
    void deletesOnlyEmpty() throws Exception {
        mvc.perform(post("/admin/categories/" + category("etc").getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "카테고리를 지웠어요."));
        assertFalse(categoryService.isSlugTaken("etc"));

        mvc.perform(post("/admin/categories/" + category("ott").getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "서비스가 있는 카테고리는 지울 수 없어요."));
        assertTrue(categoryService.isSlugTaken("ott"));
    }

    @Test
    @WithAnonymousUser
    void requiresLogin() throws Exception {
        mvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection());
    }
}
