package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 사용자가 글로 보낸 해지 방법 제보. 관리자가 승인해야 가이드로 공개된다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String serviceName;

    @Column(nullable = false, length = 1000)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Submission(String serviceName, String rawText) {
        this.serviceName = serviceName;
        this.rawText = rawText;
        this.status = SubmissionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}
