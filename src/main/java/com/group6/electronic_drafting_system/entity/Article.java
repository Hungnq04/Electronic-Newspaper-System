package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// title / subtitle / sapo / content là bản đang soạn: Save Draft chỉ cập nhật các cột này, không tạo version.
@Entity @Table(name = "article", schema = "dbo") @Getter @Setter
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "article_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lead_id", nullable = false) private Account lead;
    @Column(nullable = false, length = 255) private String title;
    @Column(length = 255) private String subtitle;
    @Column(columnDefinition = "nvarchar(max)") private String sapo;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String content = "";
    @Column(nullable = false, length = 100) private String category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ArticleStatus status = ArticleStatus.DRAFT;
    // Không có FK trong DB (tránh vòng article <-> article_version)
    @Column(name = "restored_from_version_id") private Integer restoredFromVersionId;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "submitted_at") private OffsetDateTime submittedAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @OneToMany(mappedBy = "article") @OrderBy("versionNumber") private List<ArticleVersion> versions;
    @OneToMany(mappedBy = "article") private List<ArticleSource> sources;
    @OneToOne(mappedBy = "article") private Publication publication;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if (updatedAt == null) updatedAt = createdAt;
    }
}
