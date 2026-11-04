package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DashboardStatDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadDashboardDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DraftEntryDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DraftEntryMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentHeadServiceImpl implements DepartmentHeadService {
    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final TopicRepository topics;
    private final ArticleReviewRepository articleReviews;
    private final DraftEntryMapper entryMapper;

    public DepartmentHeadServiceImpl(AccountRepository accounts, ArticleRepository articles, TopicRepository topics,
                                     ArticleReviewRepository articleReviews, DraftEntryMapper entryMapper) {
        this.accounts = accounts;
        this.articles = articles;
        this.topics = topics;
        this.articleReviews = articleReviews;
        this.entryMapper = entryMapper;
    }

    @Override
    public DepartmentHeadDashboardDTO getDashboard(String email) {
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        Department department = head.getDepartment();
        DepartmentHeadDashboardDTO.DepartmentHeadDashboardDTOBuilder dashboard = DepartmentHeadDashboardDTO.builder()
                .displayName(head.getFullName())
                .userInitials(entryMapper.initials(head.getFullName()));
        // Trưởng ban chưa được gán phòng ban: không có dữ liệu nào để duyệt
        if (department == null) {
            return dashboard.stats(List.of()).entries(List.of()).categories(List.of()).build();
        }
        Integer departmentId = department.getId();

        // Bài: theo assignment DEPARTMENT_HEAD trên Topic. Đề tài DEPARTMENT_REVIEW chưa có assignment nên vẫn lọc theo phòng ban người đề xuất
        List<Article> pendingArticles = articles.findByStatusForDepartmentHead(ArticleStatus.DEPARTMENT_REVIEW, head.getId());
        Map<Integer, Long> sourceCounts = countSources(pendingArticles);
        List<DraftEntryDTO> entries = new ArrayList<>();
        pendingArticles.forEach(article -> entries.add(entryMapper.fromArticle(article, sourceCounts.getOrDefault(article.getId(), 0L))));
        topics.findByStatusAndDepartment(TopicStatus.DEPARTMENT_REVIEW, departmentId)
                .forEach(topic -> entries.add(entryMapper.fromTopic(topic)));

        // Gần hạn chót trước (chưa có hạn xếp cuối); mới nộp = submittedAt giảm dần
        assignOrder(entries, Comparator.comparing(DraftEntryDTO::getExpectedDate, Comparator.nullsLast(Comparator.naturalOrder())),
                DraftEntryDTO::setDeadlineOrder);
        assignOrder(entries, Comparator.comparing(DraftEntryDTO::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())),
                DraftEntryDTO::setCreatedOrder);
        entries.sort(Comparator.comparingInt(DraftEntryDTO::getDeadlineOrder)); // mặc định nút "Gần hạn chót" đang chọn

        long articleCount = pendingArticles.size();
        long topicCount = entries.size() - articleCount;
        long urgentCount = entries.stream().filter(e -> "article".equals(e.getKind()) && e.isUrgent()).count();
        long approvedToday = articleReviews.countDecisionsSince(ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED,
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(), head.getId());
        long revisionCount = articles.countByStatusForDepartmentHead(ArticleStatus.REVISION_REQUIRED, head.getId());

        return dashboard
                .departmentName(department.getDepartmentName())
                .entries(entries)
                .categories(entries.stream().map(DraftEntryDTO::getCategory).distinct().sorted().toList())
                .articleCount(articleCount)
                .topicCount(topicCount)
                .urgentCount(urgentCount)
                .stats(List.of(
                        new DashboardStatDTO("CHỜ DUYỆT BÀI VIẾT", String.valueOf(articleCount), "bản thảo",
                                "Đang chờ Trưởng ban thẩm định", urgentCount > 0 ? urgentCount + " HỎA TỐC" : null, "calendar", "rose"),
                        new DashboardStatDTO("ĐÃ DUYỆT HÔM NAY", String.valueOf(approvedToday), "bài",
                                "Đã chuyển Ban Thư ký tòa soạn", null, "check-circle", "blue"),
                        new DashboardStatDTO("ĐANG TRẢ VỀ SỬA", String.valueOf(revisionCount), "bài",
                                "Chờ phóng viên chỉnh sửa", null, "edit", "amber"),
                        new DashboardStatDTO("ĐỀ TÀI CHỜ DUYỆT", String.valueOf(topicCount), "đề tài",
                                "Đề cương chờ thẩm định", null, "book-open", "slate")))
                .build();
    }

    private Map<Integer, Long> countSources(List<Article> list) {
        if (list.isEmpty()) return Map.of();
        return articles.countSourcesByArticleIds(list.stream().map(Article::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
    }

    // Đánh số thứ tự từ 1 theo comparator (JS coi 0 là "không có thứ tự" nên không bắt đầu từ 0)
    private static void assignOrder(List<DraftEntryDTO> entries, Comparator<DraftEntryDTO> comparator,
                                    ObjIntConsumer<DraftEntryDTO> setter) {
        List<DraftEntryDTO> sorted = entries.stream().sorted(comparator).toList();
        for (int i = 0; i < sorted.size(); i++) setter.accept(sorted.get(i), i + 1);
    }
}
