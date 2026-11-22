package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서비스 아이콘 이미지. 서비스 표와 따로 두어 목록을 불러올 때 이미지 바이트를 읽지 않는다.
 * 아이콘을 바꾸면 이 행을 고치지 않고 새 행을 만든다 → id가 바뀌므로 /icons/{id} 주소도 바뀌어 캐시 문제가 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IconImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, length = 1_000_000)   // MySQL에서 mediumblob이 되도록 (200KB를 넘게 담을 수 있게)
    private byte[] data;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public IconImage(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
        this.updatedAt = LocalDateTime.now();
    }
}
