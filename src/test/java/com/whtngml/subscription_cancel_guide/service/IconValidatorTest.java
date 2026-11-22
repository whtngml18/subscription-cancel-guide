package com.whtngml.subscription_cancel_guide.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IconValidatorTest {

    // 각 형식의 파일 앞부분(매직 바이트)
    public static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};
    public static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    public static final byte[] SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);

    private final IconValidator validator = new IconValidator();

    @Test
    void acceptsPngJpegWebp() {
        assertEquals("image/png", validator.detectContentType(PNG));
        assertEquals("image/jpeg", validator.detectContentType(JPEG));
        assertEquals("image/webp", validator.detectContentType(WEBP));
    }

    @Test
    void rejectsSvg() {
        IconValidationException e = assertThrows(IconValidationException.class, () -> validator.detectContentType(SVG));
        assertEquals("아이콘은 PNG, JPEG, WebP 이미지만 올릴 수 있어요.", e.getMessage());
    }

    @Test
    void rejectsRenamedText() {
        assertThrows(IconValidationException.class, () -> validator.detectContentType("hello".getBytes()));
    }

    @Test
    void rejectsOver200KB() {
        byte[] big = Arrays.copyOf(PNG, 204801);
        IconValidationException e = assertThrows(IconValidationException.class, () -> validator.detectContentType(big));
        assertEquals("아이콘은 200KB까지 올릴 수 있어요.", e.getMessage());
    }

    @Test
    void accepts200KBExactly() {
        assertEquals("image/png", validator.detectContentType(Arrays.copyOf(PNG, 204800)));
    }
}
