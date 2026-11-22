package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.IconImage;
import com.whtngml.subscription_cancel_guide.repository.IconImageRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * 아이콘 이미지 내보내기.
 * 주소는 숫자만 받는다({id:[0-9]+}) — static/icons/favicon-32.png 같은 정적 파일과 겹치지 않게.
 */
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IconController {

    private final IconImageRepository iconImageRepository;
    private final SubscriptionRepository subscriptionRepository;

    // 공개: 서비스에 연결된 아이콘만. 아이콘을 바꾸면 id가 바뀌므로 오래 저장해도 된다.
    @GetMapping("/icons/{id:[0-9]+}")
    public ResponseEntity<byte[]> icon(@PathVariable Long id) {
        if (!subscriptionRepository.existsByIcon_Id(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return image(find(id), CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }

    // 관리자: 승인 전 제보의 아이콘도 본다
    @GetMapping("/admin/icons/{id:[0-9]+}")
    public ResponseEntity<byte[]> adminIcon(@PathVariable Long id) {
        return image(find(id), CacheControl.noStore());
    }

    private IconImage find(Long id) {
        return iconImageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ResponseEntity<byte[]> image(IconImage icon, CacheControl cacheControl) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(icon.getContentType()))
                .cacheControl(cacheControl)
                .header("X-Content-Type-Options", "nosniff")
                .body(icon.getData());
    }
}
