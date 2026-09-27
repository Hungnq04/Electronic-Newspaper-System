package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity @Table(name = "PLAN_ITEM", schema = "dbo") @Getter @Setter
public class PlanItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "plan_item_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) private EditorialPlan plan;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false, unique = true) private Topic topic;
    @Column(name = "sequence_no") private Integer sequenceNo;
    @Column(length = 20) private String priority;
    @Column(name = "reporter_count") private Integer reporterCount = 1;
    @Column(columnDefinition = "nvarchar(max)") private String requirements;
    @Column(nullable = false, length = 20) private String status = "Pending";
    @Column(columnDefinition = "nvarchar(max)") private String notes;
    @OneToMany(mappedBy = "planItem") private List<Assignment> assignments;
}
