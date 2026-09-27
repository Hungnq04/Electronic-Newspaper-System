package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "ARTICLE", schema = "dbo") @Getter @Setter
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "article_id") private Integer id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignment_id", unique = true) private Assignment assignment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false) private Account author;
    @Column(nullable = false, length = 20) private String status = "Draft";
    @Column(length = 100) private String category;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @OneToMany(mappedBy = "article") private List<ArticleVersion> versions;
}
