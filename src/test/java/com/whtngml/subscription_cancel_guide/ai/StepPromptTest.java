package com.whtngml.subscription_cancel_guide.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepPromptTest {

    @Test
    void 글_우선과_지어내기_금지_위치표_예시가_들어간다() {
        String prompt = StepPrompt.system();

        assertThat(prompt)
                .contains("사용자 글에 있는 내용만")
                .contains("절대 만들지 마")
                .contains("오른쪽 위/우측 상단 → top-right")
                .contains("카드 번호")
                .contains("\"image\":null")
                .contains("구매 및 멤버십")
                .doesNotContain("캡처");
    }
}
