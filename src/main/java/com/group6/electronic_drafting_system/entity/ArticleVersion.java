package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Bản chụp cố định, chỉ tạo khi Submit / Resubmit; DB có trigger chặn UPDATE / DELETE.
@Entity @Immutable @Table(name = "article_version", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "uq_version_number", columnNames = {"article_id", "version_number"})) @Getter @Setter
public class ArticleVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "version_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "article_id", nullable = false) private Article article;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    // Cột tính toán trong DB: 1.0, 1.1, 1.2...
    @Column(name = "version_label", insertable = false, updatable = false) private String versionLabel;
    @Column(nullable = false, length = 255) private String title;
    @Column(length = 255) private String subtitle;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String sapo;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String content;
    @Column(name = "change_summary", columnDefinition = "nvarchar(max)") private String changeSummary;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "version") private List<ArticleReview> reviews;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
