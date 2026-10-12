package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity @Table(name = "topic", schema = "dbo") @Getter @Setter
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "topic_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(nullable = false, length = 100) private String category;
    @Column(columnDefinition = "nvarchar(max)") private String rationale;
    @Column(name = "expected_date") private LocalDate expectedDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Priority priority = Priority.NORMAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TopicStatus status = TopicStatus.DEPARTMENT_REVIEW;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "topic") private List<TopicReview> reviews;
    @OneToMany(mappedBy = "topic") private List<Assignment> assignments;
    @OneToMany(mappedBy = "topic") private List<TopicSource> sources;
    @OneToMany(mappedBy = "topic") private List<Article> articles;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS); }
}
