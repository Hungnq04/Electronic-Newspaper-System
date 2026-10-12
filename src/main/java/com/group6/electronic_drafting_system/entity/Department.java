package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity @Table(name = "department", schema = "dbo") @Getter @Setter
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "department_id") private Integer id;
    @Column(name = "department_name", nullable = false, unique = true, length = 100) private String departmentName;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @OneToMany(mappedBy = "department") private List<Account> accounts;
}
