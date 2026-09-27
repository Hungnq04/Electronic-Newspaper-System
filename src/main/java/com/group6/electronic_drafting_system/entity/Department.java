package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity @Table(name = "DEPARTMENT", schema = "dbo") @Getter @Setter
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "department_id") private Integer id;
    @Column(name = "department_name", nullable = false, unique = true, length = 100) private String departmentName;
    @Column(length = 255) private String description;
    @OneToMany(mappedBy = "department") private List<Account> accounts;
    @OneToMany(mappedBy = "department") private List<Topic> topics;
    @OneToMany(mappedBy = "department") private List<EditorialPlan> editorialPlans;
}
