package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionScreenshotRepository;
import com.whtngml.subscription_cancel_guide.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubmissionScreenshotFlowTest {
    @Autowired MockMvc mvc;
    @Autowired SubmissionRepository submissions;
    @Autowired SubmissionScreenshotRepository screenshots;
    @Autowired SubmissionService submissionService;

    @Test
    void storesOrderedScreenshotsForAdminAndRemovesOnRejection() throws Exception {
        mvc.perform(multipart("/submit").file(screenshot(90)).file(screenshot(120))
                        .param("serviceName", "캡처 서비스").param("rawText", "설정에서 구독 관리를 눌러 주세요.")
                        .param("aiStepsJson", """
                                {"service":"캡처 서비스","steps":[{"order":1,"screen":"설정","target":"구독 관리",
                                "position":"middle","image":2,"note":"캡처에는 관리 메뉴로 표시"}]}
                                """).with(csrf()))
                .andExpect(redirectedUrl("/submit/done"));
        var id = submissions.findAll().stream().filter(s -> s.getServiceName().equals("캡처 서비스"))
                .findFirst().orElseThrow().getId();
        String path = "/admin/submissions/" + id + "/screenshots/2";
        mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        mvc.perform(get(path).with(user("regular").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/admin/submissions/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(path)))
                .andExpect(content().string(containsString("캡처에는 관리 메뉴로 표시")));
        mvc.perform(post("/admin/submissions/{id}/reject", id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    void deletesAtCutoffAndKeepsSubmission() throws Exception {
        mvc.perform(multipart("/submit").file(screenshot(90)).param("serviceName", "만료 캡처 서비스")
                        .param("rawText", "설정에서 구독 관리를 눌러 주세요.").with(csrf()))
                .andExpect(redirectedUrl("/submit/done"));
        var submission = submissions.findAll().stream().filter(s -> s.getServiceName().equals("만료 캡처 서비스"))
                .findFirst().orElseThrow();
        submissionService.deleteExpiredScreenshots(submission.getCreatedAt().minusSeconds(1));
        org.assertj.core.api.Assertions.assertThat(screenshots.findOrders(submission.getId())).containsExactly(1);
        submissionService.deleteExpiredScreenshots(submission.getCreatedAt().plusSeconds(1));
        submissionService.deleteExpiredScreenshots(submission.getCreatedAt().plusSeconds(1));
        org.assertj.core.api.Assertions.assertThat(screenshots.findOrders(submission.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(submissions.existsById(submission.getId())).isTrue();
    }

    @Test
    void approvingExistingServiceDeletesItsSubmissionScreenshots() throws Exception {
        mvc.perform(multipart("/submit").file(screenshot(90)).param("serviceName", "넷플릭스")
                        .param("rawText", "설정에서 구독 관리를 눌러 주세요.").with(csrf()))
                .andExpect(redirectedUrl("/submit/done"));
        var submission = submissions.findAll().stream().filter(s -> s.getServiceName().equals("넷플릭스"))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(screenshots.findOrders(submission.getId())).containsExactly(1);
        mvc.perform(post("/admin/submissions/{id}/approve-existing", submission.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.assertj.core.api.Assertions.assertThat(screenshots.findOrders(submission.getId())).isEmpty();
    }

    @Test
    void invalidScreenshotReturnsToForm() throws Exception {
        var invalid = new MockMultipartFile("screenshots", "fake.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/submit").file(invalid).param("serviceName", "캡처 서비스")
                        .param("rawText", "설정에서 구독 관리를 눌러 주세요.").with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "screenshots"))
                .andExpect(content().string(containsString("캡처를 다시 골라 주세요")));
    }

    private MockMultipartFile screenshot(int height) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(60, height, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("screenshots", "screen.png", "image/png", bytes.toByteArray());
    }
}
