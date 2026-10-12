package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity @Table(name = "article_source", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "uq_article_source", columnNames = {"article_id", "source_id"})) @Getter @Setter
public class ArticleSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "article_source_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "article_id", nullable = false) private Article article;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_id", nullable = false) private Source source;
    @Column(name = "attached_at", nullable = false, updatable = false) private OffsetDateTime attachedAt;
    @Column(columnDefinition = "nvarchar(max)") private String note;
    @Column(length = 300) private String caption;
    @Column(name = "is_featured", nullable = false) private Boolean featured = false;
    @Column(name = "display_order") private Integer displayOrder;

    @PrePersist void onCreate() {
        if (attachedAt == null) attachedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
