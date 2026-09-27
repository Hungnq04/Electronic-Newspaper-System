package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity @Table(name = "ROLE", schema = "dbo") @Getter @Setter
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "role_id") private Integer id;
    @Column(name = "role_name", nullable = false, unique = true, length = 50) private String roleName;
    @Column(length = 255) private String description;
    @OneToMany(mappedBy = "role") private List<Account> accounts;
}
