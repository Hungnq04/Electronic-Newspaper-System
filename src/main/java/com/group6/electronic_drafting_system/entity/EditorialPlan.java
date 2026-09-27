package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "EDITORIAL_PLAN", schema = "dbo") @Getter @Setter
public class EditorialPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "plan_id") private Integer id;
    @Column(name = "plan_name", nullable = false, length = 150) private String planName;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(nullable = false, length = 20) private String status = "Draft";
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private Account creator;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submitted_by") private Account submitter;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @OneToMany(mappedBy = "plan") private List<PlanItem> items;
}
