package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // true: 이 방법이 맞았어요, false: 달라요
    @Column(nullable = false)
    private boolean helpful;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Feedback(Subscription subscription, boolean helpful) {
        this.subscription = subscription;
        this.helpful = helpful;
        this.createdAt = LocalDateTime.now();
    }
}
