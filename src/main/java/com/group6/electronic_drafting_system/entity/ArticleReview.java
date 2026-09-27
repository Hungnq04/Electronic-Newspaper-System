package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "ARTICLE_REVIEW", schema = "dbo") @Getter @Setter
public class ArticleReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "review_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "version_id", nullable = false) private ArticleVersion version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id", nullable = false) private Account reviewer;
    @Column(name = "review_stage", nullable = false, length = 30) private String reviewStage;
    @Column(nullable = false, length = 20) private String decision;
    @Column(columnDefinition = "nvarchar(max)") private String comment;
    @Column(name = "reviewed_at", nullable = false, insertable = false, updatable = false) private LocalDateTime reviewedAt;
}
