package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id", "image_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionScreenshot {
    public static final String CONTENT_TYPE = "image/jpeg";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, length = 5_242_880)
    private byte[] data;

    @Column(nullable = false, length = 20)
    private String contentType;

    public SubmissionScreenshot(Submission submission, int imageOrder, byte[] data) {
        this.submission = submission;
        this.imageOrder = imageOrder;
        this.data = data;
        this.contentType = CONTENT_TYPE;
    }
}
