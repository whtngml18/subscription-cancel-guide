package com.whtngml.subscription_cancel_guide.dto;

import java.util.List;

// AI가 나눈 해지 단계 하나. position은 top-left, top-right, middle, bottom 중 하나.
// otherMenus는 그 화면에 보이는 다른 메뉴(글에 나온 이름만).
// image(맞는 캡처 번호)와 note(캡처와 다른 점)는 제보 검토용이라 단계로 옮기지 않는다. 캡처가 없으면 null.
public record AiStep(int order, String screen, String target, String position, List<String> otherMenus,
                     Integer image, String note) {

    // 휴대폰 도식(fragments/phone)과 단계 저장에 쓰는 폼으로 바꾼다
    public GuideStepForm toForm() {
        GuideStepForm form = new GuideStepForm();
        form.setStepOrder(order);
        form.setScreenName(screen);
        form.setTargetLabel(target);
        form.setPosition(position);
        form.setOtherMenus(otherMenus == null || otherMenus.isEmpty() ? null : String.join("\n", otherMenus));
        return form;
    }
}
