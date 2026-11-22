package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 화면 새 디자인
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPagesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubmissionRepository submissionRepository;

    Submission saveSubmission(String name) {
        return submissionRepository.save(new Submission(name, "설정에서 해지를 눌러요 열 글자", null, Set.of(), null, null));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPagesUseSiteCss() throws Exception {
        for (String path : new String[]{"/admin/guides", "/admin/submissions"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/css/site.css")))
                    .andExpect(content().string(not(containsString("bootstrap"))));
        }
    }

    @Test
    void loginPageUsesSiteCss() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("login-box")))
                .andExpect(content().string(not(containsString("bootstrap"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void badgeShowsPendingCount() throws Exception {
        submissionRepository.findAll().stream().filter(Submission::isPending).forEach(Submission::reject);
        saveSubmission("왓챠");
        mvc.perform(get("/admin/guides"))
                .andExpect(content().string(containsString("class=\"badge\"")))
                .andExpect(content().string(containsString(">1<")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void badgeHiddenWhenNoPending() throws Exception {
        submissionRepository.findAll().stream().filter(Submission::isPending).forEach(Submission::reject);
        mvc.perform(get("/admin/guides"))
                .andExpect(content().string(not(containsString("class=\"badge\""))));
    }
}
