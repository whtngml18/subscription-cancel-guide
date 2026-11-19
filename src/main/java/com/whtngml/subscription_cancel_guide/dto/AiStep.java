package com.whtngml.subscription_cancel_guide.dto;

// AI가 나눈 해지 단계 하나. position은 top-left, top-right, middle, bottom 중 하나.
public record AiStep(int order, String screen, String target, String position) {

    // 휴대폰 도식(fragments/phone)과 단계 저장에 쓰는 폼으로 바꾼다
    public GuideStepForm toForm() {
        GuideStepForm form = new GuideStepForm();
        form.setStepOrder(order);
        form.setScreenName(screen);
        form.setTargetLabel(target);
        form.setPosition(position);
        return form;
    }
}
