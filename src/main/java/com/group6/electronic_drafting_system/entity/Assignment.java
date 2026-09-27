package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "ASSIGNMENT", schema = "dbo") @Getter @Setter
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "assignment_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_item_id", nullable = false) private PlanItem planItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by", nullable = false) private Account assignedBy;
    @Column(nullable = false) private LocalDateTime deadline;
    @Column(columnDefinition = "nvarchar(max)") private String requirements;
    @Column(nullable = false, length = 20) private String status = "Assigned";
    @Column(name = "assigned_at", nullable = false, insertable = false, updatable = false) private LocalDateTime assignedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @OneToMany(mappedBy = "assignment") private List<AssignmentReporter> reporters;
    @OneToOne(mappedBy = "assignment") private Article article;
}
