package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// anchor_paragraph đếm từ 1; anchor_start / anchor_end là vị trí ký tự (từ 0) trong đoạn, anchor_end không tính.
@Entity @Table(name = "comment", schema = "dbo") @Getter @Setter
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "comment_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "review_id", nullable = false) private ArticleReview review;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_comment_id") private Comment parent;
    @Enumerated(EnumType.STRING) @Column(name = "anchor_field", length = 10) private AnchorField anchorField;
    @Column(name = "anchor_paragraph") private Integer anchorParagraph;
    @Column(name = "anchor_start") private Integer anchorStart;
    @Column(name = "anchor_end") private Integer anchorEnd;
    @Column(name = "quoted_text", columnDefinition = "nvarchar(max)") private String quotedText;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private CommentStatus status = CommentStatus.OPEN;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "parent") private List<Comment> replies;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
