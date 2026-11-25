package com.whtngml.subscription_cancel_guide.service;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifReader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SubmissionScreenshotProcessor {
    public static final int MAX_FILES = 5;
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_EDGE = 1280;
    private static final long MAX_SOURCE_PIXELS = 24_000_000;

    public List<byte[]> process(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        List<MultipartFile> selected = files.stream()
                .filter(file -> !file.isEmpty() || (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()))
                .toList();
        if (selected.size() > MAX_FILES) {
            throw new ScreenshotValidationException("캡처는 최대 5장까지 올릴 수 있어요.");
        }
        if (selected.stream().anyMatch(file -> file.getSize() > MAX_BYTES)) {
            throw new ScreenshotValidationException("캡처는 한 장에 5MB까지 올릴 수 있어요.");
        }
        List<byte[]> screenshots = new ArrayList<>(selected.size());
        for (MultipartFile file : selected) {
            screenshots.add(normalize(file));
        }
        return List.copyOf(screenshots);
    }

    private byte[] normalize(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String contentType = ImageSignatures.contentType(bytes);
            if (contentType == null) {
                throw new ScreenshotValidationException("캡처는 PNG, JPEG, WebP 이미지만 올릴 수 있어요.");
            }
            var readers = ImageIO.getImageReadersByMIMEType(contentType);
            if (!readers.hasNext()) {
                throw unreadable();
            }
            var reader = readers.next();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new ScreenshotValidationException("캡처 해상도가 너무 커요. 크기를 줄여 다시 올려 주세요.");
                }
                int longest = Math.max(width, height);
                var params = reader.getDefaultReadParam();
                int sample = Math.max(1, longest / MAX_EDGE);
                params.setSourceSubsampling(sample, sample, 0, 0);
                var warned = new AtomicBoolean();
                reader.addIIOReadWarningListener((source, warning) -> warned.set(true));
                BufferedImage source = reader.read(0, params);
                if (source == null || warned.get()) {
                    throw unreadable();
                }
                int orientation = "image/jpeg".equals(contentType) ? jpegOrientation(bytes) : 1;
                double scale = Math.min(1.0, (double) MAX_EDGE / longest);
                int scaledWidth = Math.max(1, (int) Math.round(width * scale));
                int scaledHeight = Math.max(1, (int) Math.round(height * scale));
                return encodeJpeg(source, scaledWidth, scaledHeight, orientation);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw unreadable();
        }
    }

    private int jpegOrientation(byte[] bytes) {
        try {
            var metadata = JpegMetadataReader.readMetadata(new ByteArrayInputStream(bytes), List.of(new ExifReader()));
            var exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer orientation = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return orientation != null && orientation >= 1 && orientation <= 8 ? orientation : 1;
        } catch (IOException | JpegProcessingException e) {
            return 1;
        }
    }

    private byte[] encodeJpeg(BufferedImage source, int width, int height, int orientation) throws IOException {
        boolean swapsDimensions = orientation >= 5;
        int outputWidth = swapsDimensions ? height : width;
        int outputHeight = swapsDimensions ? width : height;
        BufferedImage jpeg = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        var graphics = jpeg.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, outputWidth, outputHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.transform(orientationTransform(orientation, width, height));
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(jpeg, "jpeg", output)) {
            throw unreadable();
        }
        return output.toByteArray();
    }

    private AffineTransform orientationTransform(int orientation, int width, int height) {
        return switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
            default -> new AffineTransform();
        };
    }

    private ScreenshotValidationException unreadable() {
        return new ScreenshotValidationException("캡처를 읽을 수 없어요. 파일을 다시 확인해 주세요.");
    }
}
