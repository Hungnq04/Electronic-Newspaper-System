package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    // Admin - Account List: mọi tài khoản, mới đăng ký lên trước (lấy kèm role + phòng ban)
    @EntityGraph(attributePaths = {"role", "department"})
    List<Account> findAllByOrderByCreatedAtDescIdDesc();

    // Admin - Account Detail
    @EntityGraph(attributePaths = {"role", "department"})
    Optional<Account> findDetailById(Integer id);

    // Tài khoản đã từng tham gia biên tập chưa? (1 = có, 0 = chưa)
    // Các rule R07, R09, R11, R13, R21 trong v_rule_violations kiểm tra role của người dùng trên cả lịch sử,
    // nên người đã có lịch sử thì không được đổi role / phòng ban, nếu không view sẽ báo lỗi.
    @Query(value = """
            SELECT CASE WHEN EXISTS (SELECT 1 FROM assignment     WHERE account_id = :id OR assigned_by = :id)
                          OR EXISTS (SELECT 1 FROM topic          WHERE created_by = :id)
                          OR EXISTS (SELECT 1 FROM topic_review   WHERE reviewer_id = :id)
                          OR EXISTS (SELECT 1 FROM article_review WHERE reviewer_id = :id)
                          OR EXISTS (SELECT 1 FROM publication    WHERE published_by = :id)
                        THEN 1 ELSE 0 END""", nativeQuery = true)
    int hasEditorialHistory(@Param("id") Integer accountId);

    @EntityGraph(attributePaths = "role")
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    // Danh sách chờ Admin duyệt, ai đăng ký trước lên trước
    List<Account> findByStatusOrderByCreatedAtAscIdAsc(AccountStatus status);
}
