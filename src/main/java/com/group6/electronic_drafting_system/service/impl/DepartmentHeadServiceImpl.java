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

// Service của màn dashboard Trưởng ban: lấy dữ liệu từ DB, tính số liệu, đóng gói vào DepartmentHeadDashboardDTO.
//
// AI GỌI SERVICE NÀY?
//   DepartmentHeadController.dashboard() gọi departmentHeadService.getDashboard(email).
//   Controller khai báo kiểu interface DepartmentHeadService (service/DepartmentHeadService.java dòng 6),
//   Spring đưa vào object thật là class này (vì có "implements DepartmentHeadService").
//
// SERVICE NÀY GỌI AI? (6 câu SQL, theo thứ tự chạy trong getDashboard)
//   SQL #1 accounts.findByEmailIgnoreCase(...)                 -> repository/AccountRepository.java dòng 12
//   SQL #2 articles.findByStatusForDepartmentHead(...)         -> repository/ArticleRepository.java dòng 24
//   SQL #3 articles.countSourcesByArticleIds(...)              -> repository/ArticleRepository.java dòng 31 (gọi trong countSources)
//   SQL #4 topics.findByStatusAndDepartment(...)               -> repository/TopicRepository.java dòng 27
//   SQL #5 articleReviews.countDecisionsSince(...)             -> repository/ArticleReviewRepository.java dòng 37
//   SQL #6 articles.countByStatusForDepartmentHead(...)        -> repository/ArticleRepository.java dòng 27
//   Không phải SQL: entryMapper.initials / fromArticle / fromTopic -> mapper/DepartmentHead/DraftEntryMapper.java dòng 67 / 23 / 47
//   (Số dòng ở các file khác có thể lệch nếu sau này file đó bị sửa.)
//
// @Service: Spring tự tạo sẵn một object của class này lúc khởi động app.
// @Transactional(readOnly = true): mọi method chạy trong transaction CHỈ ĐỌC (service này không ghi gì vào DB).
@Service
@Transactional(readOnly = true)
public class DepartmentHeadServiceImpl implements DepartmentHeadService {
    // 5 "dụng cụ" service cần dùng. Ban đầu là ô trống, Spring điền object thật vào qua constructor bên dưới.
    // Repository chỉ là interface (không có code SQL viết tay cho các method đặt tên kiểu findBy...);
    // Spring Data JPA tự tạo class cài đặt lúc khởi động, đọc tên method hoặc @Query để biết chạy SQL gì.
    private final AccountRepository accounts;              // hỏi bảng account
    private final ArticleRepository articles;              // hỏi bảng article (và article_source)
    private final TopicRepository topics;                  // hỏi bảng topic
    private final ArticleReviewRepository articleReviews;  // hỏi bảng article_review
    private final DraftEntryMapper entryMapper;            // đổi Article / Topic thành thẻ hiển thị DraftEntryDTO

    // Constructor: Spring tự gọi lúc khởi động và truyền 5 object vào (Dependency Injection).
    // Nhờ vậy trong getDashboard() các biến accounts, articles... đã có object thật để gọi.
    public DepartmentHeadServiceImpl(AccountRepository accounts, ArticleRepository articles, TopicRepository topics,
                                     ArticleReviewRepository articleReviews, DraftEntryMapper entryMapper) {
        this.accounts = accounts;
        this.articles = articles;
        this.topics = topics;
        this.articleReviews = articleReviews;
        this.entryMapper = entryMapper;
    }

    // Được gọi từ DepartmentHeadController.dashboard() với email người đang đăng nhập.
    // Trả về DepartmentHeadDashboardDTO = "cái hộp" chứa toàn bộ dữ liệu trang (dto/DepartmentHead/DepartmentHeadDashboardDTO.java).
    // @Override: method này cài đặt method getDashboard trong interface DepartmentHeadService.
    @Override
    public DepartmentHeadDashboardDTO getDashboard(String email) {
        // ===== BƯỚC 1: tìm tài khoản Trưởng ban =====
        // SQL #1 -> AccountRepository.findByEmailIgnoreCase (AccountRepository.java dòng 12).
        // Kết quả là Optional = hộp "có thể có hoặc không". orElseThrow: không có tài khoản thì ném lỗi và DỪNG method.
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        // Phòng ban của Trưởng ban; null nếu Admin chưa gán phòng ban.
        Department department = head.getDepartment();
        // Bắt đầu lắp ráp DTO bằng builder (giống điền dần từng ô của một tờ phiếu, cuối cùng gọi .build()).
        // .displayName(...) điền vào field displayName -> HTML đọc ${dashboard.displayName}
        // .userInitials(...) gọi DraftEntryMapper.initials (DraftEntryMapper.java dòng 67) để ra chữ viết tắt, ví dụ "NH".
        DepartmentHeadDashboardDTO.DepartmentHeadDashboardDTOBuilder dashboard = DepartmentHeadDashboardDTO.builder()
                .displayName(head.getFullName())
                .userInitials(entryMapper.initials(head.getFullName()));

        // ===== BƯỚC 2: chưa có phòng ban thì trả hộp rỗng =====
        // Trưởng ban chưa được gán phòng ban: không có dữ liệu nào để duyệt.
        // List.of() = danh sách rỗng. stats rỗng nên dashboard.html ẩn cả khu 4 hộp thống kê (th:if ở dòng 82).
        if (department == null) {
            return dashboard.stats(List.of()).entries(List.of()).categories(List.of()).build();
        }
        Integer departmentId = department.getId();

        // ===== BƯỚC 3: lấy bài viết + đề tài chờ duyệt, đổi thành thẻ =====
        // Bài: theo assignment DEPARTMENT_HEAD trên Topic. Đề tài DEPARTMENT_REVIEW chưa có assignment nên vẫn lọc theo phòng ban người đề xuất
        // SQL #2 -> ArticleRepository.findByStatusForDepartmentHead (ArticleRepository.java dòng 24):
        //          bài đang DEPARTMENT_REVIEW thuộc Topic mà Trưởng ban này phụ trách.
        List<Article> pendingArticles = articles.findByStatusForDepartmentHead(ArticleStatus.DEPARTMENT_REVIEW, head.getId());
        // SQL #3 (bên trong countSources, cuối file): đếm số nguồn của MỌI bài trong 1 câu SQL.
        // Kết quả là bảng tra "mã bài -> số nguồn", ví dụ {12 -> 3, 15 -> 1}.
        Map<Integer, Long> sourceCounts = countSources(pendingArticles);
        // Danh sách rỗng để chứa các thẻ hiển thị (DraftEntryDTO).
        List<DraftEntryDTO> entries = new ArrayList<>();
        // Với MỖI bài: tra số nguồn (không có thì 0), gọi DraftEntryMapper.fromArticle (DraftEntryMapper.java dòng 23)
        // để đổi Article thành một thẻ, rồi thêm vào entries.
        pendingArticles.forEach(article -> entries.add(entryMapper.fromArticle(article, sourceCounts.getOrDefault(article.getId(), 0L))));
        // SQL #4 -> TopicRepository.findByStatusAndDepartment (TopicRepository.java dòng 27): đề tài chờ duyệt của người cùng phòng.
        // Với MỖI đề tài: gọi DraftEntryMapper.fromTopic (DraftEntryMapper.java dòng 47) rồi thêm vào CÙNG danh sách entries.
        topics.findByStatusAndDepartment(TopicStatus.DEPARTMENT_REVIEW, departmentId)
                .forEach(topic -> entries.add(entryMapper.fromTopic(topic)));
        // Lúc này entries = [thẻ bài..., thẻ đề tài...]

        // ===== BƯỚC 4: đánh số thứ tự để JavaScript sắp xếp =====
        // Gần hạn chót trước (chưa có hạn xếp cuối); mới nộp = submittedAt giảm dần
        // assignOrder (cuối file) ghi số 1, 2, 3... vào deadlineOrder / createdOrder của từng thẻ.
        // HTML in ra thành data-deadline-order, data-created-order; department-head-dashboard.js dùng để sắp xếp khi bấm nút.
        assignOrder(entries, Comparator.comparing(DraftEntryDTO::getExpectedDate, Comparator.nullsLast(Comparator.naturalOrder())),
                DraftEntryDTO::setDeadlineOrder);
        assignOrder(entries, Comparator.comparing(DraftEntryDTO::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())),
                DraftEntryDTO::setCreatedOrder);
        entries.sort(Comparator.comparingInt(DraftEntryDTO::getDeadlineOrder)); // mặc định nút "Gần hạn chót" đang chọn

        // ===== BƯỚC 5: tính các con số =====
        // Số bài chờ duyệt = độ dài danh sách bài (không hỏi DB).
        long articleCount = pendingArticles.size();
        // Số đề tài = tổng số thẻ trừ số bài (không hỏi DB).
        long topicCount = entries.size() - articleCount;
        // Số bài hỏa tốc: lọc thẻ là bài viết VÀ urgent = true rồi đếm (không hỏi DB).
        long urgentCount = entries.stream().filter(e -> "article".equals(e.getKind()) && e.isUrgent()).count();
        // SQL #5 -> ArticleReviewRepository.countDecisionsSince (ArticleReviewRepository.java dòng 37):
        //          đếm lượt Trưởng ban này APPROVED ở cấp DEPARTMENT, tính từ 00:00 hôm nay.
        long approvedToday = articleReviews.countDecisionsSince(ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED,
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(), head.getId());
        // SQL #6 -> ArticleRepository.countByStatusForDepartmentHead (ArticleRepository.java dòng 27):
        //          đếm bài đang REVISION_REQUIRED (bị trả về cho phóng viên sửa) thuộc Topic mình phụ trách.
        long revisionCount = articles.countByStatusForDepartmentHead(ArticleStatus.REVISION_REQUIRED, head.getId());

        // ===== BƯỚC 6: điền nốt vào DTO và trả về controller =====
        // Mỗi .tenField(...) điền vào field cùng tên trong DepartmentHeadDashboardDTO; HTML đọc bằng ${dashboard.tenField}.
        // stats: 4 object DashboardStatDTO = 4 hộp thống kê. Thứ tự tham số: label, value, unit, note, badge, icon, tone
        //        (xem dto/DepartmentHead/DashboardStatDTO.java). HTML lặp bằng th:each ở dashboard.html dòng 83.
        // .build() tạo ra object hoàn chỉnh; return trả nó về DepartmentHeadController.dashboard().
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

    // Hàm phụ, được gọi ở BƯỚC 3 của getDashboard. Đếm số nguồn của nhiều bài trong 1 câu SQL (tránh 20 bài = 20 câu SQL).
    private Map<Integer, Long> countSources(List<Article> list) {
        // Không có bài nào thì trả bảng rỗng, khỏi hỏi DB.
        if (list.isEmpty()) return Map.of();
        // 1. list.stream().map(Article::getId).toList(): lấy danh sách mã bài, ví dụ [12, 15, 20]
        // 2. SQL #3 -> ArticleRepository.countSourcesByArticleIds (ArticleRepository.java dòng 31): trả các dòng [mã bài, số nguồn]
        // 3. Collectors.toMap: đổi thành bảng tra {12 -> 3, 15 -> 1}; row[0] = cột mã bài, row[1] = cột số nguồn
        return articles.countSourcesByArticleIds(list.stream().map(Article::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));
    }

    // Hàm phụ, được gọi 2 lần ở BƯỚC 4 của getDashboard.
    // Đánh số thứ tự từ 1 theo comparator (JS coi 0 là "không có thứ tự" nên không bắt đầu từ 0)
    //   comparator = quy tắc so sánh (ví dụ "ngày gần hơn đứng trước")
    //   setter     = ô để ghi số vào (setDeadlineOrder hoặc setCreatedOrder của DraftEntryDTO)
    private static void assignOrder(List<DraftEntryDTO> entries, Comparator<DraftEntryDTO> comparator,
                                    ObjIntConsumer<DraftEntryDTO> setter) {
        // Tạo bản sao đã sắp xếp theo quy tắc (danh sách gốc không đổi thứ tự).
        List<DraftEntryDTO> sorted = entries.stream().sorted(comparator).toList();
        // Thẻ đứng đầu ghi số 1, thẻ thứ hai ghi số 2... (i bắt đầu từ 0 nên ghi i + 1).
        for (int i = 0; i < sorted.size(); i++) setter.accept(sorted.get(i), i + 1);
    }
}
