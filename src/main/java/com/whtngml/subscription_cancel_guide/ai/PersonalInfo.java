package com.whtngml.subscription_cancel_guide.ai;

import java.util.List;
import java.util.regex.Pattern;

// AI 결과에 개인정보 모양 글자(이메일, 휴대폰 번호, 카드 번호)가 섞였는지 본다.
// "1:1 문의", "고객센터 1588-0000" 같은 메뉴 이름은 걸리지 않게 휴대폰(01x)과 13자리 이상 숫자만 본다.
final class PersonalInfo {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"),
            Pattern.compile("01[016789][-. ]?\\d{3,4}[-. ]?\\d{4}"),
            Pattern.compile("\\d(?:[ -]?\\d){12,18}")
    );

    private PersonalInfo() {
    }

    static boolean contains(String text) {
        return text != null && PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
    }
}
