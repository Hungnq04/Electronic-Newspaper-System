package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "ASSIGNMENT_REPORTER", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "UQ_ASSIGNMENT_REPORTER", columnNames = {"assignment_id", "reporter_id"})) @Getter @Setter
public class AssignmentReporter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "assignment_reporter_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assignment_id", nullable = false) private Assignment assignment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reporter_id", nullable = false) private Account reporter;
    @Column(length = 30) private String role;
    @Column(nullable = false, length = 20) private String status = "Assigned";
    @Column(name = "assigned_at", nullable = false, insertable = false, updatable = false) private LocalDateTime assignedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
