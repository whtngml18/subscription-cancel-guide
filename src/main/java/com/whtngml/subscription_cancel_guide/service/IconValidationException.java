package com.whtngml.subscription_cancel_guide.service;

// 아이콘 파일이 형식·크기 규칙에 맞지 않을 때. 메시지는 화면에 그대로 보여 준다.
public class IconValidationException extends RuntimeException {

    public IconValidationException(String message) {
        super(message);
    }
}
