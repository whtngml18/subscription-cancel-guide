package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 50)
    private String screenName;

    @Column(nullable = false, length = 50)
    private String targetLabel;

    @Column(length = 20)
    private String position;

    @Column(length = 500)
    private String description;

    public GuideStep(Subscription subscription, int stepOrder, String screenName,
                     String targetLabel, String position, String description) {
        this.subscription = subscription;
        this.stepOrder = stepOrder;
        this.screenName = screenName;
        this.targetLabel = targetLabel;
        this.position = position;
        this.description = description;
    }

    public void update(int stepOrder, String screenName, String targetLabel,
                       String position, String description) {
        this.stepOrder = stepOrder;
        this.screenName = screenName;
        this.targetLabel = targetLabel;
        this.position = position;
        this.description = description;
    }
}
