package com.whtngml.subscription_cancel_guide.service;

import org.springframework.stereotype.Component;

/**
 * 아이콘 파일 검사. 파일 이름·브라우저가 보낸 형식은 믿지 않고 파일 앞부분(매직 바이트)으로 실제 형식을 확인한다.
 * PNG·JPEG·WebP만 받는다. SVG는 스크립트를 담을 수 있어 받지 않는다.
 */
@Component
public class IconValidator {

    static final int MAX_BYTES = 200 * 1024;

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    // "image/png", "image/jpeg", "image/webp" 중 하나를 돌려준다
    public String detectContentType(byte[] bytes) {
        if (bytes.length > MAX_BYTES) {
            throw new IconValidationException("아이콘은 200KB까지 올릴 수 있어요.");
        }
        if (startsWith(bytes, 0, PNG)) {
            return "image/png";
        }
        if (startsWith(bytes, 0, JPEG)) {
            return "image/jpeg";
        }
        if (startsWith(bytes, 0, RIFF) && startsWith(bytes, 8, WEBP)) {
            return "image/webp";
        }
        throw new IconValidationException("아이콘은 PNG, JPEG, WebP 이미지만 올릴 수 있어요.");
    }

    private boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        if (bytes.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
