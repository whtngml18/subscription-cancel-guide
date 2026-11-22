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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    com.whtngml.subscription_cancel_guide.repository.IconImageRepository iconImageRepository;

    @Autowired
    com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository subscriptionRepository;

    @Autowired
    com.whtngml.subscription_cancel_guide.repository.GuideStepRepository guideStepRepository;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void guidesListShowsMissingInfo() throws Exception {
        mvc.perform(get("/admin/guides")).andExpect(status().isOk())
                .andExpect(content().string(containsString("missing-list")))
                .andExpect(content().string(containsString("단계 수")))
                .andExpect(content().string(containsString("<li>난이도</li>")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void guidesListSearch() throws Exception {
        mvc.perform(get("/admin/guides").param("q", "넷"))
                .andExpect(content().string(containsString("넷플릭스")))
                .andExpect(content().string(not(containsString("멜론"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void guidesTableIsWrapped() throws Exception {
        mvc.perform(get("/admin/guides"))
                .andExpect(content().string(containsString("class=\"table-wrap\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void serviceFormHasThreeSections() throws Exception {
        mvc.perform(get("/admin/guides/1/edit")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<legend>기본 정보</legend>")))
                .andExpect(content().string(containsString("<legend>해지 정보</legend>")))
                .andExpect(content().string(containsString("<legend>확인</legend>")))
                .andExpect(content().string(containsString("mini-phone")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void serviceFormErrorsLinked() throws Exception {
        mvc.perform(multipart("/admin/guides/1/edit").param("categoryId", "1").param("name", "넷플릭스")
                        .param("slug", "잘못").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"slug-error slug-help\"")))
                .andExpect(content().string(containsString("id=\"slug-error\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stepFormHasPreview() throws Exception {
        mvc.perform(get("/admin/guides/1/steps/new")).andExpect(status().isOk())
                .andExpect(content().string(containsString("mini-phone")))
                .andExpect(content().string(not(containsString("form-control"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteStepStillWorks() throws Exception {
        var netflix = subscriptionRepository.findBySlug("netflix").orElseThrow();
        var steps = guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(netflix);
        mvc.perform(get("/admin/guides/{id}/edit", netflix.getId()))
                .andExpect(content().string(containsString("/steps/" + steps.get(0).getId() + "/delete")));
        mvc.perform(post("/admin/guides/{id}/steps/{stepId}/delete", netflix.getId(), steps.get(0).getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(steps.size() - 1,
                guideStepRepository.findAllBySubscriptionOrderByStepOrderAsc(netflix).size());
    }

    Submission saveRichSubmission(String name) {
        var icon = iconImageRepository.save(new com.whtngml.subscription_cancel_guide.domain.IconImage(
                com.whtngml.subscription_cancel_guide.service.IconValidatorTest.PNG, "image/png"));
        return submissionRepository.save(new Submission(name, "설정에서 해지를 눌러요 열 글자", null,
                java.util.EnumSet.of(com.whtngml.subscription_cancel_guide.domain.CancelChannel.APP), "주의", icon));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void submissionsListShowsAttachments() throws Exception {
        saveRichSubmission("왓챠");
        mvc.perform(get("/admin/submissions")).andExpect(status().isOk())
                .andExpect(content().string(containsString(">아이콘<")))
                .andExpect(content().string(containsString(">추가 정보<")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void submissionDetailIsTwoColumns() throws Exception {
        Submission s = saveRichSubmission("왓챠");
        mvc.perform(get("/admin/submissions/{id}", s.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("cols-2")))
                .andExpect(content().string(containsString("보낸 내용")))
                .andExpect(content().string(not(containsString("phone-list"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveCheckboxDefaultsKept() throws Exception {
        Submission s = saveRichSubmission("넷플릭스");
        String html = mvc.perform(get("/admin/submissions/{id}", s.getId())).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(inputTag(html, "importDetails").contains("checked"));
        org.junit.jupiter.api.Assertions.assertTrue(inputTag(html, "importIcon").contains("checked"));
        org.junit.jupiter.api.Assertions.assertFalse(inputTag(html, "importDetailsExisting").contains("checked"));
        org.junit.jupiter.api.Assertions.assertFalse(inputTag(html, "importIconExisting").contains("checked"));
    }

    // id가 같은 <input ...> 태그 하나를 잘라 낸다
    private static String inputTag(String html, String id) {
        int at = html.indexOf("id=\"" + id + "\"");
        int start = html.lastIndexOf("<input", at);
        return html.substring(start, html.indexOf('>', at) + 1);
    }

    @Test
    void oldCssRemoved() throws Exception {
        mvc.perform(get("/css/phone.css")).andExpect(status().isNotFound());
        mvc.perform(get("/css/app.css")).andExpect(status().isNotFound());
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/resources"))) {
            for (var file : files.filter(java.nio.file.Files::isRegularFile).filter(f -> f.toString().matches(".*[.](html|css|js|json|yaml)$")).toList()) {
                String text = java.nio.file.Files.readString(file);
                org.junit.jupiter.api.Assertions.assertFalse(
                        text.contains("phone.css") || text.contains("app.css") || text.contains("bootstrap"),
                        file + "에 옛 스타일 참조가 남아 있어요");
            }
        }
    }
}
