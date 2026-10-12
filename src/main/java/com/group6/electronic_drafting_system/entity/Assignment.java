package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

// Một người - một nhiệm vụ - trên một Topic. Không xóa dòng: gỡ / thay thì đổi status sang REMOVED.
@Entity @Table(name = "assignment", schema = "dbo") @Getter @Setter
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "assignment_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by", nullable = false) private Account assignedBy;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_role", nullable = false, length = 20) private AssignmentRole assignmentRole;
    @Column private OffsetDateTime deadline;
    @Column(columnDefinition = "nvarchar(max)") private String requirements;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private AssignmentStatus status = AssignmentStatus.ASSIGNED;
    @Column(name = "assigned_at", nullable = false, updatable = false) private OffsetDateTime assignedAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "removed_at") private OffsetDateTime removedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "removed_by") private Account removedBy;

    @PrePersist void onCreate() {
        if (assignedAt == null) assignedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
