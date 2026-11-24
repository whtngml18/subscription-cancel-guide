package com.whtngml.subscription_cancel_guide.ai;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalInfoTest {

    @ParameterizedTest
    @ValueSource(strings = {"me@test.com", "010-1234-5678", "01012345678", "1234 5678 9012 3456"})
    void 개인정보로_본다(String text) {
        assertThat(PersonalInfo.contains(text)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1:1 문의", "2026-11 결제 내역", "고객센터 1588-0000", "멤버십 해지"})
    void 메뉴_이름은_개인정보가_아니다(String text) {
        assertThat(PersonalInfo.contains(text)).isFalse();
    }
}
