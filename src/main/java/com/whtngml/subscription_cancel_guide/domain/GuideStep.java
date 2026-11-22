package com.whtngml.subscription_cancel_guide.domain;

import com.whtngml.subscription_cancel_guide.dto.PhoneRows;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    // 이 화면에 보이는 다른 메뉴, 한 줄에 하나 (그림에 쓴다)
    @Column(length = 300)
    private String otherMenus;

    public GuideStep(Subscription subscription, int stepOrder, String screenName,
                     String targetLabel, String position, String description, String otherMenus) {
        this.subscription = subscription;
        this.stepOrder = stepOrder;
        this.screenName = screenName;
        this.targetLabel = targetLabel;
        this.position = position;
        this.description = description;
        this.otherMenus = otherMenus;
    }

    public void update(int stepOrder, String screenName, String targetLabel,
                       String position, String description, String otherMenus) {
        this.stepOrder = stepOrder;
        this.screenName = screenName;
        this.targetLabel = targetLabel;
        this.position = position;
        this.description = description;
        this.otherMenus = otherMenus;
    }

    public List<PhoneRows.PhoneRow> getPhoneRows() {
        return PhoneRows.of(position, targetLabel, otherMenus);
    }
}
