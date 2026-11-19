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

    // AI가 나눈 단계 JSON (AI를 안 썼거나 실패했으면 null)
    @Column(length = 5000)
    private String aiStepsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 승인하면 연결되는 서비스 (검토 대기·반려일 때는 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    public Submission(String serviceName, String rawText, String aiStepsJson) {
        this.serviceName = serviceName;
        this.rawText = rawText;
        this.aiStepsJson = aiStepsJson;
        this.status = SubmissionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == SubmissionStatus.PENDING;
    }

    public void approve(Subscription subscription) {
        checkPending();
        this.status = SubmissionStatus.APPROVED;
        this.subscription = subscription;
    }

    public void reject() {
        checkPending();
        this.status = SubmissionStatus.REJECTED;
    }

    // 서비스가 삭제될 때 제보는 남기고 연결만 끊는다
    public void unlink() {
        this.subscription = null;
    }

    private void checkPending() {
        if (!isPending()) {
            throw new IllegalStateException("이미 처리한 제보예요.");
        }
    }
}
