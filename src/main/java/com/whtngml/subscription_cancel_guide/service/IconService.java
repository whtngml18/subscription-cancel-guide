package com.whtngml.subscription_cancel_guide.service;

import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.repository.IconImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IconService {

    private final IconImageRepository iconImageRepository;
    private final IconValidator iconValidator;

    // 파일을 고르지 않았으면 비어 있음. 규칙에 맞지 않으면 IconValidationException.
    @Transactional
    public Optional<IconImage> store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String contentType = iconValidator.detectContentType(bytes);
        return Optional.of(iconImageRepository.save(new IconImage(bytes, contentType)));
    }
}
