package com.whtngml.subscription_cancel_guide.web;

import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.domain.Submission;
import com.whtngml.subscription_cancel_guide.repository.IconImageRepository;
import com.whtngml.subscription_cancel_guide.repository.SubmissionRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import com.whtngml.subscription_cancel_guide.service.IconValidatorTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IconControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    IconImageRepository iconImageRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    private IconImage saveIcon() {
        return iconImageRepository.save(new IconImage(IconValidatorTest.PNG, "image/png"));
    }

    @Test
    void linkedIconIsPublicAndCached() throws Exception {
        IconImage icon = saveIcon();
        subscriptionRepository.findBySlug("netflix").orElseThrow().changeIcon(icon);
        subscriptionRepository.flush();

        mvc.perform(get("/icons/{id}", icon.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    @Test
    void unlinkedIconIsNotPublic() throws Exception {
        IconImage icon = saveIcon();
        submissionRepository.save(new Submission("왓챠", "설정에서 해지해요 열 글자 이상", null, Set.of(), null, icon));

        mvc.perform(get("/icons/{id}", icon.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSeeUnlinkedIcon() throws Exception {
        IconImage icon = saveIcon();
        mvc.perform(get("/admin/icons/{id}", icon.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void staticIconsStillServed() throws Exception {
        mvc.perform(get("/icons/favicon-32.png")).andExpect(status().isOk());
        mvc.perform(get("/icons/icon-192.png")).andExpect(status().isOk());
    }
}
