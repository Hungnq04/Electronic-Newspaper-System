package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DashboardStatDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadDashboardDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DraftEntryDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewHistoryItemDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewHistoryPageDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Topic;
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
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Service của 2 màn CHỈ XEM của Trưởng phòng:
//   - DASHBOARD (getDashboard): lấy dữ liệu từ DB, tính các con số, đóng gói vào DepartmentHeadDashboardDTO.
//   - LỊCH SỬ DUYỆT (getReviewHistory, UC-35): ở cuối file, trả về ReviewHistoryPageDTO.
//
// AI GỌI? DepartmentHeadController.dashboard() gọi departmentHeadService.getDashboard(email).
//   Controller khai báo kiểu interface DepartmentHeadService; Spring đưa vào object thật là class này
//   (vì có "implements DepartmentHeadService" và nhãn @Service).
//
// QUY TẮC LỌC BÀI (cách A): Trưởng phòng thấy bài khi Topic của bài thuộc PHÒNG mình HOẶC được GIAO cho mình.
//   Câu truy vấn nằm ở ArticleRepository.findByStatusForDepartment / countByStatusForDepartment.
//
// SERVICE NÀY GỌI AI? (theo thứ tự chạy trong getDashboard)
//   accounts.findByEmailIgnoreCase(...)        -> tìm tài khoản Trưởng phòng
//   articles.findByStatusForDepartment(...)    -> bài chờ duyệt
//   articles.countSourcesByArticleIds(...)     -> đếm nguồn của các bài (trong hàm phụ countSources)
//   topics.findByStatusAndDepartment(...)      -> đề tài chờ duyệt của phóng viên cùng phòng
//   articleReviews.countDecisionsSince(...)    -> số lượt mình đã duyệt hôm nay
//   articles.countByStatusForDepartment(...)   -> số bài đang trả về sửa
//   entryMapper.fromArticle / fromTopic        -> đổi Article / Topic thành thẻ hiển thị (không phải SQL)
//
// @Transactional(readOnly = true): mọi method chỉ ĐỌC dữ liệu, không ghi gì vào DB.
@Service
@Transactional(readOnly = true)
public class DepartmentHeadServiceImpl implements DepartmentHeadService {
    // Định dạng thời gian cho màn Lịch sử duyệt, ví dụ "09:30 07/11/2026". Hằng số (static final) vì không bao giờ đổi.
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // 6 "dụng cụ" service cần dùng. Spring điền object thật vào qua constructor bên dưới (Dependency Injection).
    private final AccountRepository accounts;              // bảng account
    private final ArticleRepository articles;              // bảng article (và article_source)
    private final TopicRepository topics;                  // bảng topic
    private final ArticleReviewRepository articleReviews;  // bảng article_review
    private final CommentRepository comments;              // bảng comment (đếm góp ý cho màn Lịch sử duyệt)
    private final DraftEntryMapper entryMapper;            // đổi Article / Topic thành thẻ hiển thị DraftEntryDTO

    public DepartmentHeadServiceImpl(AccountRepository accounts, ArticleRepository articles, TopicRepository topics,
                                     ArticleReviewRepository articleReviews, CommentRepository comments,
                                     DraftEntryMapper entryMapper) {
        this.accounts = accounts;
        this.articles = articles;
        this.topics = topics;
        this.articleReviews = articleReviews;
        this.comments = comments;
        this.entryMapper = entryMapper;
    }

    @Override
    public DepartmentHeadDashboardDTO getDashboard(String email) {
        // 00:00 hôm nay. Phải tính BÊN TRONG method: bean chỉ tạo một lần lúc chạy app,
        // nếu để ngoài method (field) thì qua ngày mới vẫn còn giá trị của ngày cũ.
        OffsetDateTime startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        // ===== BƯỚC 1: tìm tài khoản Trưởng phòng =====
        // Optional = hộp "có thể có hoặc không"; orElseThrow: không có thì báo lỗi và DỪNG method.
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        // Phòng ban của Trưởng phòng; null nếu Admin chưa gán phòng ban.
        Department headDepartment = head.getDepartment();

        // Bắt đầu lắp ráp DTO bằng builder (điền dần từng ô, cuối cùng gọi .build()).
        // HTML đọc bằng ${dashboard.displayName}, ${dashboard.userInitials}...
        DepartmentHeadDashboardDTO.DepartmentHeadDashboardDTOBuilder dashboard = DepartmentHeadDashboardDTO.builder()
                .displayName(head.getFullName())
                .userInitials(entryMapper.initials(head.getFullName()));   // chữ viết tắt cho avatar, ví dụ "NH"

        // ===== BƯỚC 2: chưa có phòng ban thì trả hộp rỗng =====
        // List.of() = danh sách rỗng. stats rỗng nên dashboard.html ẩn cả khu hộp thống kê.
        if (headDepartment == null) {
            return dashboard.stats(List.of()).entries(List.of()).categories(List.of()).build();
        }
        Integer departmentId = headDepartment.getId();

        // ===== BƯỚC 3: lấy bài + đề tài chờ duyệt, đổi thành thẻ hiển thị =====
        // Bài chờ duyệt: Topic thuộc phòng mình HOẶC được giao cho mình (cách A).
        List<Article> articleList = articles.findByStatusForDepartment(
                ArticleStatus.DEPARTMENT_REVIEW, departmentId, head.getId());
        // Đề tài chờ duyệt: đề tài DEPARTMENT_REVIEW chưa có assignment, nên lọc theo phòng ban của người đề xuất.
        List<Topic> topicList = topics.findByStatusAndDepartment(TopicStatus.DEPARTMENT_REVIEW, departmentId);

        // Đếm nguồn của MỌI bài trong 1 câu SQL (hàm phụ countSources cuối file).
        // Kết quả là bảng tra "mã bài -> số nguồn", ví dụ {12 -> 3, 15 -> 1}.
        Map<Integer, Long> sourceCountByArticleId = countSources(articleList);

        // cards = danh sách THẺ hiển thị trên dashboard, gồm cả bài viết lẫn đề tài (mỗi thẻ là một DraftEntryDTO).
        List<DraftEntryDTO> cards = new ArrayList<>();
        for (Article article : articleList) {
            long sourceCount = sourceCountByArticleId.getOrDefault(article.getId(), 0L);   // không có trong bảng tra thì 0
            cards.add(entryMapper.fromArticle(article, sourceCount));
        }
        for (Topic topic : topicList) {
            cards.add(entryMapper.fromTopic(topic));
        }

        // ===== BƯỚC 4: đánh số thứ tự để JavaScript sắp xếp khi bấm nút =====
        // assignOrder (cuối file) ghi số 1, 2, 3... vào deadlineOrder / createdOrder của từng thẻ.
        // HTML in ra thành data-deadline-order, data-created-order; department-head-dashboard.js đọc để sắp xếp.
        //   Nút "Gần hạn chót": ngày dự kiến đăng gần nhất xếp trước, thẻ chưa có ngày xếp cuối
        assignOrder(cards, Comparator.comparing(DraftEntryDTO::getExpectedDate, Comparator.nullsLast(Comparator.naturalOrder())),
                DraftEntryDTO::setDeadlineOrder);
        //   Nút "Mới nộp": nộp gần đây nhất xếp trước
        assignOrder(cards, Comparator.comparing(DraftEntryDTO::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())),
                DraftEntryDTO::setCreatedOrder);
        // Lúc mở trang, nút "Gần hạn chót" đang được chọn, nên xếp sẵn theo thứ tự đó.
        cards.sort(Comparator.comparingInt(DraftEntryDTO::getDeadlineOrder));

        // ===== BƯỚC 5: tính các con số =====
        long pendingArticleCount = articleList.size();      // bài chờ duyệt
        long pendingTopicCount = topicList.size();          // đề tài chờ duyệt
        // Bài hỏa tốc: thẻ là bài viết VÀ urgent = true
        long urgentCount = 0;
        for (DraftEntryDTO card : cards) {
            if ("article".equals(card.getKind()) && card.isUrgent()) {
                urgentCount++;
            }
        }
        // Số lượt chính mình bấm Duyệt ở cấp phòng, tính từ 00:00 hôm nay
        long approvedTodayCount = articleReviews.countDecisionsSince(
                ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED, startOfToday, head.getId());
        // Bài đang bị trả về cho phóng viên sửa. Dùng CÙNG quy tắc với danh sách chờ duyệt (cách A)
        // để một bài lúc chờ duyệt được tính thì lúc bị trả về cũng được tính.
        long revisionCount = articles.countByStatusForDepartment(
                ArticleStatus.REVISION_REQUIRED, departmentId, head.getId());

        // ===== BƯỚC 6: điền nốt vào DTO và trả về controller =====
        // Mỗi .tenField(...) điền vào field cùng tên trong DepartmentHeadDashboardDTO; HTML đọc bằng ${dashboard.tenField}.
        // stats = 4 hộp thống kê. Thứ tự tham số của DashboardStatDTO: label, value, unit, note, badge, icon, tone.
        return dashboard
                .departmentName(headDepartment.getDepartmentName())
                .entries(cards)
                .categories(cards.stream().map(DraftEntryDTO::getCategory).distinct().sorted().toList())   // chuyên mục, không trùng, A->Z
                .articleCount(pendingArticleCount)
                .topicCount(pendingTopicCount)
                .urgentCount(urgentCount)
                .stats(List.of(
                        new DashboardStatDTO("CHỜ DUYỆT BÀI VIẾT", String.valueOf(pendingArticleCount), "bản thảo",
                                "Đang chờ Trưởng ban thẩm định", urgentCount > 0 ? urgentCount + " HỎA TỐC" : null, "calendar", "rose"),
                        new DashboardStatDTO("ĐÃ DUYỆT HÔM NAY", String.valueOf(approvedTodayCount), "bài",
                                "Đã chuyển Ban Thư ký tòa soạn", null, "check-circle", "blue"),
                        new DashboardStatDTO("ĐANG TRẢ VỀ SỬA", String.valueOf(revisionCount), "bài",
                                "Chờ phóng viên chỉnh sửa", null, "edit", "amber"),
                        new DashboardStatDTO("ĐỀ TÀI CHỜ DUYỆT", String.valueOf(pendingTopicCount), "đề tài",
                                "Đề cương chờ thẩm định", null, "book-open", "slate")))
                .build();
    }

    // =====================================================================================================
    // MÀN "LỊCH SỬ DUYỆT" (UC-35): GET /department-head/review-history
    // =====================================================================================================
    // AI GỌI? DepartmentHeadController.reviewHistory() gọi departmentHeadService.getReviewHistory(email).
    //
    // PHẠM VI (đã chốt): chỉ các lượt duyệt CẤP PHÒNG mà CHÍNH Trưởng ban này đã chốt (Duyệt hoặc Yêu cầu sửa).
    //   Lượt đang duyệt dở (decision = NULL) không hiện. Màn này chỉ ĐỌC, không ghi gì vào DB.
    //
    // SERVICE NÀY GỌI AI? (theo thứ tự chạy)
    //   accounts.findByEmailIgnoreCase(...)                -> tìm tài khoản Trưởng ban
    //   articleReviews.findDecidedByReviewer(...)          -> các lượt duyệt đã chốt (kèm version, bài, đề tài)
    //   comments.countRootCommentsByReviewIds(...)         -> số góp ý của từng lượt (trong hàm phụ countComments)
    //   toHistoryItem(...)                                 -> đổi mỗi lượt duyệt thành 1 dòng của bảng (không phải SQL)
    @Override
    public ReviewHistoryPageDTO getReviewHistory(String email) {
        // ===== BƯỚC 1: tìm tài khoản Trưởng ban =====
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));

        // ===== BƯỚC 2: lấy các lượt duyệt cấp phòng mình đã chốt, mới nhất trước =====
        List<ArticleReview> myReviews = articleReviews.findDecidedByReviewer(head.getId(), ReviewStage.DEPARTMENT);

        // ===== BƯỚC 3: đếm góp ý của mọi lượt trong 1 câu SQL -> bảng tra "mã lượt duyệt -> số góp ý" =====
        Map<Integer, Long> commentCountByReviewId = countComments(myReviews);

        // ===== BƯỚC 4: đổi từng lượt duyệt thành 1 dòng của bảng, đồng thời đếm số lượt Duyệt / Yêu cầu sửa =====
        List<ReviewHistoryItemDTO> rows = new ArrayList<>();
        long approvedCount = 0;
        long revisionCount = 0;
        for (ArticleReview review : myReviews) {
            long commentCount = commentCountByReviewId.getOrDefault(review.getId(), 0L);   // không có trong bảng tra thì 0
            ReviewHistoryItemDTO row = toHistoryItem(review, commentCount);
            rows.add(row);
            if (row.isApproved()) {
                approvedCount++;
            } else {
                revisionCount++;
            }
        }

        // ===== BƯỚC 5: đóng gói vào ReviewHistoryPageDTO và trả về controller =====
        return ReviewHistoryPageDTO.builder()
                .displayName(head.getFullName())
                .departmentName(head.getDepartment() != null ? head.getDepartment().getDepartmentName() : null)
                .userInitials(entryMapper.initials(head.getFullName()))
                .items(rows)
                // Chuyên mục có trong bảng (không trùng, xếp A -> Z) cho ô lọc "Chuyên mục"
                .categories(rows.stream().map(ReviewHistoryItemDTO::getCategory).distinct().sorted().toList())
                .totalCount(rows.size())
                .approvedCount(approvedCount)
                .revisionCount(revisionCount)
                .build();
    }

    // Hàm phụ: đổi MỘT lượt duyệt (article_review) thành MỘT dòng của bảng lịch sử.
    private static ReviewHistoryItemDTO toHistoryItem(ArticleReview review, long commentCount) {
        ArticleVersion version = review.getVersion();   // phiên bản đã duyệt
        Article article = version.getArticle();          // bài của phiên bản đó
        boolean approved = review.getDecision() == ArticleReviewDecision.APPROVED;
        return ReviewHistoryItemDTO.builder()
                .articleId(article.getId())
                .articleCode("BT-" + article.getId())
                .articleTitle(version.getTitle())          // tít của bản đã duyệt, không phải bản đang soạn
                .topicTitle(article.getTopic().getTitle())
                .category(article.getCategory())
                .versionLabel(version.getVersionLabel())
                .approved(approved)
                .decisionText(approved ? "Đã duyệt" : "Yêu cầu sửa")
                .feedback(review.getFeedback())
                .commentCount(commentCount)
                .reviewedAtText(review.getReviewedAt() != null ? review.getReviewedAt().format(HISTORY_TIME_FORMAT) : "")
                .articleStatusText(ArticleEditorDTO.labelOf(article.getStatus()))   // dùng chung chữ trạng thái với màn phóng viên
                .articleStatusGroup(statusGroupOf(article.getStatus()))
                .build();
    }

    // Hàm phụ: nhóm màu của nhãn trạng thái bài (class rp-stage--doing / review / done trong reporter-tasks.css)
    private static String statusGroupOf(ArticleStatus status) {
        return switch (status) {
            case DRAFT, REVISION_REQUIRED -> "doing";                 // đang ở tay phóng viên
            case DEPARTMENT_REVIEW, DEPARTMENT_APPROVED -> "review";  // đang chờ duyệt
            case APPROVED, SCHEDULED, PUBLISHED -> "done";            // đã qua duyệt cuối
        };
    }

    // Hàm phụ: đếm góp ý của nhiều lượt duyệt trong 1 câu SQL, trả về bảng tra {mã lượt duyệt -> số góp ý}.
    private Map<Integer, Long> countComments(List<ArticleReview> reviewList) {
        if (reviewList.isEmpty()) return Map.of();   // không có lượt nào thì khỏi hỏi DB
        List<Integer> reviewIds = reviewList.stream().map(ArticleReview::getId).toList();
        // row[0] = mã lượt duyệt, row[1] = số góp ý
        return comments.countRootCommentsByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
    }

    // Hàm phụ: đếm số nguồn của nhiều bài trong 1 câu SQL (tránh 20 bài = 20 câu SQL).
    private Map<Integer, Long> countSources(List<Article> articleList) {
        // Không có bài nào thì trả bảng rỗng, khỏi hỏi DB.
        if (articleList.isEmpty()) return Map.of();
        // 1. Lấy danh sách mã bài, ví dụ [12, 15, 20]
        List<Integer> articleIds = articleList.stream().map(Article::getId).toList();
        // 2. Hỏi DB, nhận về các dòng [mã bài, số nguồn]
        // 3. Đổi thành bảng tra {12 -> 3, 15 -> 1}; row[0] = cột mã bài, row[1] = cột số nguồn
        return articles.countSourcesByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
    }

    // Hàm phụ: đánh số thứ tự 1, 2, 3... cho các thẻ theo một quy tắc so sánh.
    // Bắt đầu từ 1 vì JS coi 0 là "không có thứ tự".
    //   comparator = quy tắc so sánh (ví dụ "ngày gần hơn đứng trước")
    //   setter     = ô để ghi số vào (setDeadlineOrder hoặc setCreatedOrder của DraftEntryDTO)
    private static void assignOrder(List<DraftEntryDTO> cards, Comparator<DraftEntryDTO> comparator,
                                    ObjIntConsumer<DraftEntryDTO> setter) {
        // Tạo bản sao đã sắp xếp (danh sách gốc không đổi thứ tự).
        List<DraftEntryDTO> sortedCards = cards.stream().sorted(comparator).toList();
        // Thẻ đứng đầu ghi số 1, thẻ thứ hai ghi số 2... (i bắt đầu từ 0 nên ghi i + 1).
        for (int i = 0; i < sortedCards.size(); i++) {
            setter.accept(sortedCards.get(i), i + 1);
        }
    }
}
