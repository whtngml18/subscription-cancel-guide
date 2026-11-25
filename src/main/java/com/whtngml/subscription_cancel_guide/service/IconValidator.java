package com.whtngml.subscription_cancel_guide.service;

import org.springframework.stereotype.Component;

/**
 * 아이콘 파일 검사. 파일 이름·브라우저가 보낸 형식은 믿지 않고 파일 앞부분(매직 바이트)으로 실제 형식을 확인한다.
 * PNG·JPEG·WebP만 받는다. SVG는 스크립트를 담을 수 있어 받지 않는다.
 */
@Component
public class IconValidator {

    static final int MAX_BYTES = 200 * 1024;


    // "image/png", "image/jpeg", "image/webp" 중 하나를 돌려준다
    public String detectContentType(byte[] bytes) {
        if (bytes.length > MAX_BYTES) {
            throw new IconValidationException("아이콘은 200KB까지 올릴 수 있어요.");
        }
        String contentType = ImageSignatures.contentType(bytes);
        if (contentType == null) {
            throw new IconValidationException("아이콘은 PNG, JPEG, WebP 이미지만 올릴 수 있어요.");
        }
        return contentType;
    }
}
