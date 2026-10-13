package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    @EntityGraph(attributePaths = "role")
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"role", "department"})
    List<Account> findByRoleRoleNameAndStatusOrderByFullNameAsc(RoleName roleName, AccountStatus status);

    @EntityGraph(attributePaths = {"role", "department"})
    Optional<Account> findWithRoleAndDepartmentById(Integer id);
}
