package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.TopicReviewDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

// Mỗi Topic chỉ có một kết quả ở mỗi cấp; từ chối bắt buộc có feedback.
@Entity @Table(name = "topic_review", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "uq_topic_review_stage", columnNames = {"topic_id", "review_stage"})) @Getter @Setter
public class TopicReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "topic_review_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id", nullable = false) private Account reviewer;
    @Enumerated(EnumType.STRING) @Column(name = "review_stage", nullable = false, length = 20) private ReviewStage reviewStage;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TopicReviewDecision decision;
    @Column(columnDefinition = "nvarchar(max)") private String feedback;
    @Column(name = "reviewed_at", nullable = false) private OffsetDateTime reviewedAt;

    @PrePersist void onCreate() {
        if (reviewedAt == null) reviewedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
