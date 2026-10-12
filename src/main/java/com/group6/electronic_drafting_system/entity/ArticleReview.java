package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;

// Một lượt duyệt của một cấp trên một version. decision / reviewed_at NULL = đang review.
@Entity @Table(name = "article_review", schema = "dbo") @Getter @Setter
public class ArticleReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "review_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "version_id", nullable = false) private ArticleVersion version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id", nullable = false) private Account reviewer;
    @Enumerated(EnumType.STRING) @Column(name = "review_stage", nullable = false, length = 20) private ReviewStage reviewStage;
    @Enumerated(EnumType.STRING) @Column(name = "review_kind", nullable = false, length = 20) private ReviewKind reviewKind = ReviewKind.REVIEW;
    @Enumerated(EnumType.STRING) @Column(length = 20) private ArticleReviewDecision decision;
    @Column(columnDefinition = "nvarchar(max)") private String feedback;
    @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;
    @OneToMany(mappedBy = "review") private List<Comment> comments;
}
