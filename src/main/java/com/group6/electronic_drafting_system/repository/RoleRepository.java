package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
}
