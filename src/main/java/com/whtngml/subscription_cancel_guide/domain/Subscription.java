package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(length = 7)
    private String brandColor;

    @Column(length = 1000)
    private String refundInfo;

    private LocalDate verifiedAt;

    public Subscription(Category category, String name, String slug, String brandColor,
                        String refundInfo, LocalDate verifiedAt) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.brandColor = brandColor;
        this.refundInfo = refundInfo;
        this.verifiedAt = verifiedAt;
    }
}