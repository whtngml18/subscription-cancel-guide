package com.whtngml.subscription_cancel_guide.service;

import java.util.Arrays;

final class ImageSignatures {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private ImageSignatures() {}

    static String contentType(byte[] bytes) {
        if (matches(bytes, 0, PNG)) {
            return "image/png";
        }
        if (matches(bytes, 0, JPEG)) {
            return "image/jpeg";
        }
        if (matches(bytes, 0, RIFF) && matches(bytes, 8, WEBP)) {
            return "image/webp";
        }
        return null;
    }

    private static boolean matches(byte[] bytes, int offset, byte[] prefix) {
        return bytes.length >= offset + prefix.length
                && Arrays.equals(bytes, offset, offset + prefix.length, prefix, 0, prefix.length);
    }
}
