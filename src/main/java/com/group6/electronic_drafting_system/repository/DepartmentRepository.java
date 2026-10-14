package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    // Ban có ít nhất một tài khoản mang role này đang hoạt động (dùng để lọc ban có Trưởng ban,
    // tự loại "Ban Thư ký tòa soạn" mà không phải so tên)
    @Query("""
            select d from Department d
            where exists (select 1 from Account a
                          where a.department = d and a.role.roleName = :role and a.status = :status)
            order by d.departmentName""")
    List<Department> findHavingAccountWithRole(@Param("role") RoleName role, @Param("status") AccountStatus status);
}
