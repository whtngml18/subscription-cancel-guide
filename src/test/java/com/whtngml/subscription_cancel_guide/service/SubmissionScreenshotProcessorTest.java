package com.whtngml.subscription_cancel_guide.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Base64;
import java.util.List;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionScreenshotProcessorTest {
    private final SubmissionScreenshotProcessor processor = new SubmissionScreenshotProcessor();

    @Test
    void normalizesImagesInOrderAndSkipsEmptyBrowserPart() throws Exception {
        var wide = image(1600, 800, "png");
        var tall = image(450, 900, "jpg");
        var results = processor.process(List.of(wide, new MockMultipartFile("screenshots", new byte[0]), tall));
        assertThat(results).hasSize(2);
        var first = ImageIO.read(new ByteArrayInputStream(results.getFirst()));
        assertThat(first.getWidth()).isEqualTo(1280);
        assertThat(first.getHeight()).isEqualTo(640);
        assertThat(first.getRGB(0, 0)).isEqualTo(0xffffffff);
        assertThat(ImageIO.read(new ByteArrayInputStream(results.getLast())).getHeight()).isEqualTo(900);
        assertThat(results.getFirst()).startsWith((byte) 0xff, (byte) 0xd8);
    }

    @Test
    void appliesJpegOrientationBeforeReencoding() throws Exception {
        byte[] original = image(20, 50, "jpg").getBytes();
        byte[] exif = HexFormat.of().parseHex("ffe100224578696600004d4d002a00000008000101120003000000010006000000000000");
        var oriented = new ByteArrayOutputStream();
        oriented.write(original, 0, 2);
        oriented.write(exif);
        oriented.write(original, 2, original.length - 2);

        byte[] jpeg = processor.process(List.of(new MockMultipartFile("screenshots", oriented.toByteArray())))
                .getFirst();
        var converted = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(converted.getWidth()).isEqualTo(50);
        assertThat(converted.getHeight()).isEqualTo(20);
        assertThat(new String(jpeg, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    @Test
    void decodesRealWebpAndRejectsOverlargePixelDimensions() throws Exception {
        byte[] webp = Base64.getDecoder().decode("UklGRhwAAABXRUJQVlA4TA8AAAAvC8AEAAcQ/Y/+ByKi/wEA");
        byte[] jpeg = processor.process(List.of(new MockMultipartFile("screenshots", "screen.webp", "image/webp", webp)))
                .getFirst();
        var converted = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(converted.getWidth()).isEqualTo(12);
        assertThat(converted.getHeight()).isEqualTo(20);

        byte[] png = image(20, 20, "png").getBytes();
        // PNG IHDR에 기록된 가로 크기를 바꿔 전체 픽셀 수 검사에 걸리게 한다.
        java.nio.ByteBuffer.wrap(png).putInt(16, 1_500_000);
        assertThatThrownBy(() -> processor.process(List.of(new MockMultipartFile("screenshots", png))))
                .isInstanceOf(ScreenshotValidationException.class).hasMessageContaining("해상도");
    }

    @Test
    void rejectsTooManyTooLargeAndInvalidImages() throws Exception {
        var valid = image(20, 30, "png");
        assertThat(processor.process(Collections.nCopies(5, valid))).hasSize(5);
        assertThatThrownBy(() -> processor.process(Collections.nCopies(6, valid)))
                .isInstanceOf(ScreenshotValidationException.class);
        var oversized = new MockMultipartFile("screenshots", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> processor.process(List.of(oversized)))
                .isInstanceOf(ScreenshotValidationException.class);
        var invalid = new MockMultipartFile("screenshots", "fake.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.process(List.of(invalid)))
                .isInstanceOf(ScreenshotValidationException.class);
        var truncated = new MockMultipartFile("screenshots", java.util.Arrays.copyOf(valid.getBytes(), 20));
        assertThatThrownBy(() -> processor.process(List.of(truncated)))
                .isInstanceOf(ScreenshotValidationException.class);
    }

    private MockMultipartFile image(int width, int height, String format) throws Exception {
        int type = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, type), format, bytes);
        return new MockMultipartFile("screenshots", "image." + format, "application/octet-stream", bytes.toByteArray());
    }
}
