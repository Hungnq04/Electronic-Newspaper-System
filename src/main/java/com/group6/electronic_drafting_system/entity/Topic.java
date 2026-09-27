package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "TOPIC", schema = "dbo") @Getter @Setter
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "topic_id") private Integer id;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(length = 100) private String category;
    @Column(columnDefinition = "nvarchar(max)") private String rationale;
    @Column(name = "expected_date") private LocalDate expectedDate;
    @Column(name = "source_type", length = 50) private String sourceType;
    @Column(name = "source_notes", length = 255) private String sourceNotes;
    @Column(length = 20) private String priority;
    @Column(nullable = false, length = 20) private String status = "Draft";
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @OneToMany(mappedBy = "topic") private List<TopicReview> reviews;
    @OneToOne(mappedBy = "topic") private PlanItem planItem;
    @ManyToMany @JoinTable(name = "TOPIC_MATERIAL", schema = "dbo", joinColumns = @JoinColumn(name = "topic_id"), inverseJoinColumns = @JoinColumn(name = "material_id")) private List<Material> materials;
}
