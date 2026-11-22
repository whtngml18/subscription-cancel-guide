package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideSource source = GuideSource.ADMIN;

    // 해지하는 곳 (DB에는 "APP,WEB" 글 한 칸)
    @Convert(converter = ChannelSetConverter.class)
    @Column(length = 40)
    private Set<CancelChannel> channels = EnumSet.noneOf(CancelChannel.class);

    @Column(length = 500)
    private String caution;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    @Column(length = 500)
    private String officialCancelUrl;

    private Integer estimatedMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_id")
    private IconImage icon;

    public Subscription(Category category, String name, String slug, String brandColor,
                        String refundInfo, LocalDate verifiedAt) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.brandColor = brandColor;
        this.refundInfo = refundInfo;
        this.verifiedAt = verifiedAt;
    }

    public void update(Category category, String name, String slug, String brandColor,
                       String refundInfo, LocalDate verifiedAt) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.brandColor = brandColor;
        this.refundInfo = refundInfo;
        this.verifiedAt = verifiedAt;
    }

    public void updateDetails(Set<CancelChannel> channels, String caution, Difficulty difficulty,
                              String officialCancelUrl, Integer estimatedMinutes) {
        changeChannelsAndCaution(channels, caution);
        this.difficulty = difficulty;
        this.officialCancelUrl = officialCancelUrl;
        this.estimatedMinutes = estimatedMinutes;
    }

    public void changeChannelsAndCaution(Set<CancelChannel> channels, String caution) {
        this.channels = channels == null || channels.isEmpty()
                ? EnumSet.noneOf(CancelChannel.class) : EnumSet.copyOf(channels);
        this.caution = caution;
    }

    public void changeIcon(IconImage icon) {
        this.icon = icon;
    }

    public void markAsUserSubmission() {
        this.source = GuideSource.USER_SUBMISSION;
    }
}
