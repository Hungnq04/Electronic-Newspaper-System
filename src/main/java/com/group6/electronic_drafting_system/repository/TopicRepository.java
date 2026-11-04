package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    // Phóng viên: Topic do chính mình đề xuất
    @EntityGraph(attributePaths = "department")
    List<Topic> findByCreator_IdOrderByCreatedAtDescIdDesc(Integer creatorId);

    // Trưởng ban: Topic thuộc ban của mình
    @EntityGraph(attributePaths = "department")
    List<Topic> findByDepartment_IdOrderByCreatedAtDescIdDesc(Integer departmentId);

    // Thư ký tòa soạn: Topic đang chờ duyệt cuối
    @EntityGraph(attributePaths = "department")
    List<Topic> findByStatusOrderByCreatedAtDescIdDesc(TopicStatus status);

    // Phòng ban của đề tài = phòng ban của người tạo đề tài
    @Query("select t from Topic t join fetch t.creator c "
            + "where t.status = :status and c.department.id = :departmentId order by t.createdAt desc")
    List<Topic> findByStatusAndDepartment(@Param("status") TopicStatus status, @Param("departmentId") Integer departmentId);

    // FL-01 Iter2 – hàng chờ Trưởng ban: Topic của ban mình (topic.department_id), đang chờ duyệt cấp ban
    // và KHÔNG đang chờ phóng viên sửa (awaiting_revision = 0)
    @EntityGraph(attributePaths = {"department", "creator"})
    List<Topic> findByStatusAndDepartment_IdAndAwaitingRevisionFalseOrderByCreatedAtAscIdAsc(TopicStatus status, Integer departmentId);

    // FL-01 Iter2 – hàng chờ Thư ký: Topic SECRETARY_REVIEW của mọi ban, cũ nhất trước
    @EntityGraph(attributePaths = {"department", "creator"})
    List<Topic> findByStatusOrderByCreatedAtAscIdAsc(TopicStatus status);

    @EntityGraph(attributePaths = {"department", "creator", "reviewedByHead"})
    Optional<Topic> findWithPeopleById(Integer id);

    @EntityGraph(attributePaths = {"creator", "creator.role"})
    List<Topic> findByStatusOrderByCreatedAtDesc(TopicStatus status);

    @EntityGraph(attributePaths = {"creator", "creator.role"})
    Optional<Topic> findWithCreatorById(Integer id);

    // Khóa dòng khi đổi trạng thái / giao việc: hai người bấm cùng lúc thì người sau phải chờ, rồi thấy dữ liệu mới
    // (dùng chung cho FL-01 duyệt đề tài và FL-02 giao Topic tòa soạn)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Integer id);

    // Gợi ý chuyên mục khi tạo Topic
    @Query("select distinct t.category from Topic t order by t.category")
    List<String> findDistinctCategories();
}
