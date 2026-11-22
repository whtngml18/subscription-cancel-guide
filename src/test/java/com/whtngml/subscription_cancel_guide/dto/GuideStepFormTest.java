package com.whtngml.subscription_cancel_guide.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuideStepFormTest {

    @Test
    void 다른_메뉴의_줄바꿈은_LF로_맞춘다() {
        GuideStepForm form = new GuideStepForm();
        form.setOtherMenus("프로필\r\n알림\r설정");
        assertEquals("프로필\n알림\n설정", form.getOtherMenus());
    }

    @Test
    void 다른_메뉴가_null이면_그대로_null() {
        GuideStepForm form = new GuideStepForm();
        form.setOtherMenus(null);
        assertNull(form.getOtherMenus());
    }
}
