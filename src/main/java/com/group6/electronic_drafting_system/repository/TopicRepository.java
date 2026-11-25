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

// [TV1-0.3] Nói với thầy: "Chỗ này là các hàm đọc / ghi bảng topic. Em chỉ đặt tên hàm theo quy tắc (findBy...,
//   OrderBy...), Spring tự sinh câu SQL."
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

    // FL-01 Iter2 – hàng chờ Trưởng ban: Topic của ban mình (topic.department_id), đang chờ duyệt cấp ban
    // và KHÔNG đang chờ phóng viên sửa (awaiting_revision = 0)
    // [TV1-1.9] Nói với thầy: "Chỗ này em lấy hàng chờ của Trưởng ban: đề tài đúng trạng thái, đúng ban, không đang
    //   chờ phóng viên sửa, gửi trước nằm trên, lấy kèm ban và người tạo."
    @EntityGraph(attributePaths = {"department", "creator"})
    List<Topic> findByStatusAndDepartment_IdAndAwaitingRevisionFalseOrderByCreatedAtAscIdAsc(TopicStatus status, Integer departmentId);

    // FL-01 Iter2 – hàng chờ Thư ký: Topic SECRETARY_REVIEW của mọi ban, cũ nhất trước
    @EntityGraph(attributePaths = {"department", "creator"})
    List<Topic> findByStatusOrderByCreatedAtAscIdAsc(TopicStatus status);

    // [TV1-1.8b] Nói với thầy: "Chỗ này em lấy một đề tài theo id cho cột chi tiết, lấy kèm ban, người tạo và Trưởng
    //   ban đã duyệt."
    @EntityGraph(attributePaths = {"department", "creator", "reviewedByHead"})
    Optional<Topic> findWithPeopleById(Integer id);

    @EntityGraph(attributePaths = {"creator", "creator.role"})
    List<Topic> findByStatusOrderByCreatedAtDesc(TopicStatus status);

    @EntityGraph(attributePaths = {"creator", "creator.role"})
    Optional<Topic> findWithCreatorById(Integer id);

    // Khóa dòng khi đổi trạng thái / giao việc: hai người bấm cùng lúc thì người sau phải chờ, rồi thấy dữ liệu mới
    // (dùng chung cho FL-01 duyệt đề tài và FL-02 giao Topic tòa soạn)
    // [TV1-1.16b] Nói với thầy: "Chỗ này em lấy đề tài theo id và khóa dòng đó tới khi xử lý xong, để hai người không
    //   cùng duyệt một đề tài."
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Integer id);

    // Gợi ý chuyên mục khi tạo Topic
    @Query("select distinct t.category from Topic t order by t.category")
    List<String> findDistinctCategories();
}
