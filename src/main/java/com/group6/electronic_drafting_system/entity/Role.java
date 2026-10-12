package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity @Table(name = "role", schema = "dbo") @Getter @Setter
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "role_id") private Integer id;
    @Enumerated(EnumType.STRING) @Column(name = "role_name", nullable = false, unique = true, length = 30) private RoleName roleName;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @OneToMany(mappedBy = "role") private List<Account> accounts;
}
