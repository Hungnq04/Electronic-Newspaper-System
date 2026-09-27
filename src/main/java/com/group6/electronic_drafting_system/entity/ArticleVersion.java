package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "ARTICLE_VERSION", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "UQ_ARTICLE_VERSION", columnNames = {"article_id", "version_number"})) @Getter @Setter
public class ArticleVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "version_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "article_id", nullable = false) private Article article;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    @Column(length = 255) private String title;
    @Column(length = 255) private String subtitle;
    @Column(columnDefinition = "nvarchar(max)") private String lead;
    @Column(columnDefinition = "nvarchar(max)") private String content;
    @Column(name = "change_summary", length = 255) private String changeSummary;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "version") private List<ArticleReview> reviews;
    @OneToOne(mappedBy = "version") private Publication publication;
    @ManyToMany @JoinTable(name = "ARTICLE_MATERIAL", schema = "dbo", joinColumns = @JoinColumn(name = "version_id"), inverseJoinColumns = @JoinColumn(name = "material_id")) private List<Material> materials;
}
