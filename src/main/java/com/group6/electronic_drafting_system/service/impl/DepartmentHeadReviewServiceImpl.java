package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.SourceItemDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DraftEntryMapper;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Service của màn THẨM ĐỊNH BÀI VIẾT (workspace review, FL-05). Code thật của interface DepartmentHeadReviewService.
//
// AI GỌI? DepartmentHeadController (6 method: reviewWorkspace, addComment, deleteComment, resolveComment, approve, requestRevision).
//
// QUY TẮC QUYỀN (cách A, giống luồng tập code):
//   XEM bài   (checkCanReview): Topic thuộc PHÒNG mình HOẶC được GIAO cho mình; và mình không phải Lead của bài (BR-34)
//   THAO TÁC  (myOpenReview)  : góp ý / xóa / đánh dấu / duyệt / trả bài thì PHẢI được giao (rule R09 trong DB),
//                               không phải Lead, và bài đang DEPARTMENT_REVIEW
//   Người cùng phòng nhưng không được giao: chỉ xem, không có nút thao tác (reviewable = false).
//
// BẢN ĐỒ CÁC BẢNG LIÊN QUAN (để dễ hình dung):
//   article          : bài viết (status: DEPARTMENT_REVIEW, REVISION_REQUIRED...)
//   article_version  : mỗi lần Lead nộp bài là 1 version (1.0, 1.1, 1.2...). Version KHÔNG được sửa / xóa.
//   article_review   : một LƯỢT DUYỆT của một cấp (DEPARTMENT / SECRETARY) trên MỘT version.
//                      decision = NULL nghĩa là "đang duyệt, chưa quyết"; APPROVED / REQUEST_REVISION là đã chốt.
//   comment          : góp ý, thuộc một lượt duyệt (article_review). Có thể gắn vị trí trong bài (anchor_...).
//
// CÁC "THỢ PHỤ" ĐƯỢC GỌI (repository = hỏi DB, mapper = đổi entity thành DTO):
//   accounts     -> repository/AccountRepository.java         : findByEmailIgnoreCase (dòng 12)
//   articles     -> repository/ArticleRepository.java         : findWithLeadAndTopic (dòng 34), findSources (dòng 38)
//   versions     -> repository/ArticleVersionRepository.java  : findByArticleIdOrderByVersionNumberAsc (dòng 17)
//   articleReviews -> repository/ArticleReviewRepository.java : findOpenReview (dòng 43), save (có sẵn của JpaRepository)
//   comments     -> repository/CommentRepository.java         : findByVersion (dòng 19), findWithReview (dòng 22), save, delete
//   assignments  -> repository/AssignmentRepository.java      : isActiveOnTopic (dòng 38), findActiveByTopicAndRole (dòng 43)
//   mapper       -> mapper/DepartmentHead/ReviewWorkspaceMapper.java:
//                   toVersionView (dòng 52), toComment (dòng 68), toSource (dòng 86), mediaUrls (dòng 102), anchorText (dòng 212)
//   entryMapper  -> mapper/DepartmentHead/DraftEntryMapper.java : initials (dòng 67)
//   (Số dòng ở các file khác có thể lệch nếu sau này file đó bị sửa; khi đó tìm theo tên method.)
//
// @Transactional(readOnly = true) ở class: mặc định mọi method chỉ đọc.
// Method nào GHI vào DB thì có thêm @Transactional riêng (không readOnly) để ghi được.
// Transaction = "làm hết hoặc không làm gì": nếu lỗi giữa chừng, mọi thay đổi trong method bị hủy.
@Service
@Transactional(readOnly = true)
public class DepartmentHeadReviewServiceImpl implements DepartmentHeadReviewService {
    // 8 "dụng cụ", Spring điền vào qua constructor bên dưới (Dependency Injection).
    private final AccountRepository accounts;            // bảng account
    private final ArticleRepository articles;            // bảng article (+ article_source)
    private final ArticleVersionRepository versions;     // bảng article_version
    private final ArticleReviewRepository articleReviews; // bảng article_review
    private final CommentRepository comments;            // bảng comment
    private final AssignmentRepository assignments;      // bảng assignment (ai phụ trách Topic nào)
    private final ReviewWorkspaceMapper mapper;          // đổi version / comment / source thành DTO hiển thị
    private final DraftEntryMapper entryMapper;          // chỉ dùng hàm initials() (chữ viết tắt cho avatar)

    // Constructor: Spring tự gọi lúc khởi động và truyền 8 object vào.
    public DepartmentHeadReviewServiceImpl(AccountRepository accounts, ArticleRepository articles,
                                           ArticleVersionRepository versions, ArticleReviewRepository articleReviews,
                                           CommentRepository comments, AssignmentRepository assignments,
                                           ReviewWorkspaceMapper mapper, DraftEntryMapper entryMapper) {
        this.accounts = accounts;
        this.articles = articles;
        this.versions = versions;
        this.articleReviews = articleReviews;
        this.comments = comments;
        this.assignments = assignments;
        this.mapper = mapper;
        this.entryMapper = entryMapper;
    }

    // =====================================================================================================
    // MỞ TRANG THẨM ĐỊNH. Gọi từ DepartmentHeadController.reviewWorkspace().
    // Trả về ReviewWorkspaceDTO -> controller bỏ vào model tên "workspace" -> workspacereview.html đọc ${workspace...}.
    // =====================================================================================================
    // Không readOnly: lần đầu mở bài sẽ tạo lượt duyệt (article_review, decision = NULL) - CLAUDE.md mục 7 "Review"
    @Override
    @Transactional
    public ReviewWorkspaceDTO openWorkspace(String email, Integer articleId) {
        // ===== BƯỚC 1: kiểm tra quyền XEM =====
        // currentHead (phần "Kiểm tra quyền" bên dưới): tìm tài khoản theo email, không có thì báo lỗi.
        Account head = currentHead(email);
        // findArticle: tìm bài, không có thì lỗi 404.
        Article article = findArticle(articleId);
        // checkCanReview: cùng phòng HOẶC được giao, và không phải Lead. Sai quyền -> lỗi 403 (controller handleError).
        checkCanReview(head, article);

        // ===== BƯỚC 2: lấy các version của bài =====
        // SQL -> ArticleVersionRepository.findByArticleIdOrderByVersionNumberAsc: mọi version, xếp tăng dần [v1.0, v1.1, v1.2...]
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(articleId);
        // Chưa có version nào = Lead chưa nộp bài (bài DRAFT) -> không có gì để duyệt.
        if (allVersions.isEmpty()) throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        // current = version MỚI NHẤT (phần tử cuối danh sách) = bản đang duyệt
        ArticleVersion current = allVersions.get(allVersions.size() - 1);
        // previous = version LIỀN TRƯỚC (phần tử áp chót); null nếu bài mới có 1 version (lần nộp đầu)
        ArticleVersion previous = allVersions.size() > 1 ? allVersions.get(allVersions.size() - 2) : null;

        // ===== BƯỚC 3: mở lượt duyệt (article_review) nếu đủ điều kiện, quyết định "được thao tác hay chỉ xem" =====
        // Chỉ mở lượt duyệt khi CẢ HAI đúng:
        //   - bài đang DEPARTMENT_REVIEW (bài đã duyệt / đã trả thì chỉ xem)
        //   - mình được GIAO Topic này (người cùng phòng nhưng không được giao thì chỉ xem; tạo lượt duyệt cho họ sẽ vi phạm R09)
        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);
        boolean reviewable = false;
        if (article.getStatus() == ArticleStatus.DEPARTMENT_REVIEW && assignedToMe) {
            // Tìm lượt duyệt cấp DEPARTMENT đang mở (decision = NULL) trên bản này. Có rồi thì dùng lại,
            // chưa có thì tạo mới (newDepartmentReview, phần "Hàm phụ") rồi save = INSERT vào article_review.
            // Nhờ vậy mở lại trang nhiều lần (F5) KHÔNG tạo thêm lượt duyệt mới.
            ArticleReview openReview = articleReviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                    .orElseGet(() -> articleReviews.save(newDepartmentReview(current, head)));
            // Được thao tác (góp ý / duyệt / trả bài) chỉ khi lượt duyệt đang mở là CỦA MÌNH.
            reviewable = openReview.getReviewer().getId().equals(head.getId());
        }
        // reviewable = false -> trong HTML, các khối th:if="${workspace.reviewable}" bị ẩn (khung góp ý, nút Duyệt, nút Yêu cầu sửa...)

        // ===== BƯỚC 4: chuẩn bị nội dung bài và tư liệu =====
        // Thân bài theo bố cục phóng viên; link ảnh / video trong bài dùng để đánh dấu tư liệu "Có trong bài"
        // mapper.toVersionView (ReviewWorkspaceMapper dòng 52): đổi version thành VersionViewDTO
        //   (tít, sapo, danh sách đoạn để so sánh, danh sách khối nội dung để hiển thị).
        VersionViewDTO currentView = mapper.toVersionView(current);
        // mapper.mediaUrls (dòng 102): gom link mọi ảnh / video đang nằm trong thân bài.
        Set<String> bodyUrls = mapper.mediaUrls(currentView.getBlocks());
        // SQL -> ArticleRepository.findSources: tư liệu gắn vào bài.
        // Mỗi tư liệu đổi thành SourceItemDTO bằng mapper.toSource (dòng 86); inBody = link có nằm trong bodyUrls không.
        List<SourceItemDTO> sources = articles.findSources(articleId).stream()
                .map(source -> mapper.toSource(source, bodyUrls)).toList();
        // Ảnh đại diện: mỗi bài tối đa một (uq_article_featured), luôn là Source loại IMAGE (R16)
        // Lọc tư liệu có featured = true và có link, lấy cái đầu tiên; không có thì null (HTML ẩn khung ảnh đại diện).
        SourceItemDTO featuredImage = sources.stream()
                .filter(source -> source.isFeatured() && source.getUrl() != null).findFirst().orElse(null);
        // SQL -> AssignmentRepository.findActiveByTopicAndRole: các Supporting đang hoạt động trên Topic,
        // ghép tên thành một chuỗi "A, B, C" để hiện ở dòng tác giả.
        String supportingNames = assignments.findActiveByTopicAndRole(article.getTopic().getId(), AssignmentRole.SUPPORTING)
                .stream().map(s -> s.getAccount().getFullName()).collect(Collectors.joining(", "));

        // ===== BƯỚC 5: đóng gói vào ReviewWorkspaceDTO (xem từng field trong dto/DepartmentHead/ReviewWorkspaceDTO.java) =====
        // Mỗi .tenField(...) điền vào field cùng tên; HTML đọc bằng ${workspace.tenField}.
        return ReviewWorkspaceDTO.builder()
                .articleId(article.getId())                                   // dùng để ghép URL các form POST
                .code("BT-" + article.getId())                                // mã hiển thị, ví dụ "BT-15"
                .topicTitle(article.getTopic().getTitle())
                .category(article.getCategory())
                .statusText(statusText(article.getStatus()))                  // đổi enum thành chữ tiếng Việt (hàm phụ cuối file)
                .reviewable(reviewable)                                       // từ BƯỚC 3
                .reviewerName(head.getFullName())                             // tên Trưởng ban (thanh trên cùng)
                .reviewerInitials(entryMapper.initials(head.getFullName()))
                .departmentName(head.getDepartment() != null ? head.getDepartment().getDepartmentName() : null)
                .leadName(article.getLead().getFullName())                    // tên Lead (dòng tác giả)
                .leadInitials(entryMapper.initials(article.getLead().getFullName()))
                .supportingNames(supportingNames)
                // countWords (hàm phụ) đếm số từ; String.format kiểu Việt Nam: 1840 -> "1.840 từ"
                .wordCount(String.format(Locale.forLanguageTag("vi-VN"), "%,d từ", countWords(current.getContent())))
                .imageCount((int) sources.stream().filter(SourceItemDTO::isImage).count())
                .current(currentView)                                         // bản đang duyệt
                .previous(previous != null ? mapper.toVersionView(previous) : null)   // bản liền trước (để so sánh)
                .versions(allVersions.stream().map(mapper::toVersionView).toList())   // mọi version (ô chọn "So với")
                .sources(sources)
                .featuredImage(featuredImage)
                .comments(rootComments(current))                              // góp ý vòng này (hàm phụ rootComments)
                .previousComments(earlierComments(allVersions))               // góp ý các vòng trước (hàm phụ earlierComments)
                .build();
    }

    // =====================================================================================================
    // THÊM GÓP Ý. Gọi từ DepartmentHeadController.addComment(). Ghi 1 dòng mới vào bảng comment.
    // =====================================================================================================
    @Override
    @Transactional
    public void addComment(String email, Integer articleId, CommentRequestDTO commentRequest) {
        // Kiểm tra quyền: được giao Topic + đang có lượt duyệt mở của chính mình (myOpenReview).
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, findArticle(articleId));
        // Nội dung góp ý (ô "Nội dung góp ý" / "Góp ý chung"). trim() bỏ khoảng trắng hai đầu; rỗng thì báo lỗi.
        String content = commentRequest.getContent() == null ? "" : commentRequest.getContent().trim();
        if (content.isEmpty()) throw new ApplicationException(ErrorCode.COMMENT_INVALID);

        // Tạo object Comment mới (chưa lưu) và điền thông tin.
        Comment comment = new Comment();
        comment.setReview(review);   // góp ý thuộc lượt duyệt đang mở
        comment.setCreator(head); // R17: comment gốc phải do chính người duyệt viết
        comment.setContent(content);

        // Góp ý gắn vị trí: kiểm tra vị trí có thật trong version đang duyệt, quoted_text lấy từ DB (không tin chữ gửi lên)
        // anchorField để trống = góp ý chung -> bỏ qua cả khối if này.
        if (commentRequest.getAnchorField() != null && !commentRequest.getAnchorField().isBlank()) {
            // "CONTENT" (chữ) -> AnchorField.CONTENT (enum); chữ lạ thì báo lỗi (hàm phụ parseAnchorField)
            AnchorField field = parseAnchorField(commentRequest.getAnchorField());
            int paragraph = commentRequest.getAnchorParagraph() == null ? 0 : commentRequest.getAnchorParagraph();
            // mapper.anchorText (ReviewWorkspaceMapper dòng 212): lấy chữ THẬT của đúng ô đó (tít / sapo / đoạn số mấy) trong version
            String text = mapper.anchorText(review.getVersion(), field, paragraph);
            Integer anchorStart = commentRequest.getAnchorStart();
            Integer anchorEnd = commentRequest.getAnchorEnd();
            // Vị trí ký tự phải hợp lệ: 0 <= anchorStart < anchorEnd <= độ dài đoạn. Sai thì báo lỗi.
            if (text == null || anchorStart == null || anchorEnd == null
                    || anchorStart < 0 || anchorEnd <= anchorStart || anchorEnd > text.length()) {
                throw new ApplicationException(ErrorCode.COMMENT_INVALID);
            }
            comment.setAnchorField(field);
            comment.setAnchorParagraph(paragraph);
            comment.setAnchorStart(anchorStart);
            comment.setAnchorEnd(anchorEnd);
            // Cắt đúng đoạn chữ được bôi đen từ nội dung trong DB (không dùng chữ trình duyệt gửi lên).
            comment.setQuotedText(text.substring(anchorStart, anchorEnd));
        }
        // SQL: INSERT vào bảng comment
        comments.save(comment);
    }

    // =====================================================================================================
    // XÓA GÓP Ý. Gọi từ DepartmentHeadController.deleteComment(). Xóa 1 dòng trong bảng comment.
    // =====================================================================================================
    @Override
    @Transactional
    public void deleteComment(String email, Integer articleId, Integer commentId) {
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, findArticle(articleId));
        // SQL -> CommentRepository.findWithReview: tìm góp ý theo id (lấy kèm lượt duyệt của nó)
        Comment comment = comments.findWithReview(commentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COMMENT_NOT_FOUND));
        // Chỉ xóa được góp ý của chính mình, trong lượt duyệt chưa chốt và chưa có ai trả lời
        //   - comment.getReview() khác lượt đang mở     -> góp ý của vòng khác, không được xóa
        //   - comment.getCreator() không phải mình      -> góp ý của người khác
        //   - comment.getReplies() không rỗng           -> đã có người trả lời, xóa sẽ mất cuộc trao đổi
        if (!comment.getReview().getId().equals(review.getId()) || !comment.getCreator().getId().equals(head.getId())
                || !comment.getReplies().isEmpty()) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
        // SQL: DELETE dòng comment
        comments.delete(comment);
    }

    // =====================================================================================================
    // ĐÁNH DẤU GÓP Ý VÒNG TRƯỚC "ĐÃ SỬA" / "CHƯA SỬA". Gọi từ DepartmentHeadController.resolveComment().
    // =====================================================================================================
    @Override
    @Transactional
    public void setPreviousCommentResolved(String email, Integer articleId, Integer commentId, boolean resolved) {
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, findArticle(articleId));
        Comment comment = comments.findWithReview(commentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COMMENT_NOT_FOUND));
        // Góp ý phải thuộc một version CŨ của chính bài này (vòng trước)
        //   - version của góp ý phải thuộc bài này
        //   - KHÔNG được là version đang duyệt (góp ý vòng này thì không có nút "Đã sửa")
        //   - phải là góp ý gốc (parent == null), không phải câu trả lời
        ArticleVersion version = comment.getReview().getVersion();
        if (!version.getArticle().getId().equals(articleId) || version.getId().equals(review.getVersion().getId())
                || comment.getParent() != null) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
        // Chỉ cần đổi giá trị trong object: vì đang trong @Transactional, Hibernate tự UPDATE xuống DB khi method kết thúc
        // (không cần gọi save).
        comment.setStatus(resolved ? CommentStatus.RESOLVED : CommentStatus.OPEN);
    }

    // =====================================================================================================
    // DUYỆT CẤP PHÒNG. Gọi từ DepartmentHeadController.approve().
    // =====================================================================================================
    // DEPARTMENT_REVIEW -> DEPARTMENT_APPROVED (CLAUDE.md mục 6.2). Đây KHÔNG phải Final Approve; bài sang hàng chờ Thư ký
    @Override
    @Transactional
    public void approve(String email, Integer articleId, String feedback) {
        Account head = currentHead(email);
        Article article = findArticle(articleId);
        // myOpenReview: kiểm tra được giao, không phải Lead, bài đang DEPARTMENT_REVIEW; rồi trả về lượt duyệt của mình.
        ArticleReview review = myOpenReview(head, article);
        // Ghi chú không bắt buộc: để trống thì lưu null.
        String note = feedback == null || feedback.isBlank() ? null : feedback.trim();
        // Chốt lượt duyệt: decision = APPROVED, feedback, reviewed_at = bây giờ (hàm phụ closeReview)
        closeReview(review, ArticleReviewDecision.APPROVED, note);
        // Đổi trạng thái bài. Cả 2 thay đổi (review + article) được Hibernate UPDATE cùng lúc khi method kết thúc,
        // trong CÙNG một transaction: lỗi giữa chừng thì không thay đổi nào được lưu.
        article.setStatus(ArticleStatus.DEPARTMENT_APPROVED);
    }

    // =====================================================================================================
    // TRẢ BÀI CHO LEAD SỬA. Gọi từ DepartmentHeadController.requestRevision().
    // =====================================================================================================
    // DEPARTMENT_REVIEW -> REVISION_REQUIRED, bắt buộc có nhận xét (BR-12, ràng buộc ck_review_feedback)
    @Override
    @Transactional
    public void requestRevision(String email, Integer articleId, String feedback) {
        // Kiểm tra nhận xét TRƯỚC: thiếu thì báo lỗi ngay (controller hiện toast lỗi), không cần hỏi DB.
        if (feedback == null || feedback.isBlank()) throw new ApplicationException(ErrorCode.REVIEW_FEEDBACK_REQUIRED);
        Account head = currentHead(email);
        Article article = findArticle(articleId);
        ArticleReview review = myOpenReview(head, article);
        // Chốt lượt duyệt: decision = REQUEST_REVISION
        closeReview(review, ArticleReviewDecision.REQUEST_REVISION, feedback.trim());
        // Bài quay về Lead
        article.setStatus(ArticleStatus.REVISION_REQUIRED);
        article.setCompletedAt(null); // ck_article_completed: bài chưa APPROVED thì completed_at phải NULL
    }

    // ---------- Kiểm tra quyền ----------
    // Các hàm dưới đây được gọi ở ĐẦU mọi method public bên trên. Kiểm tra quyền ở backend
    // nên dù ai đó tự gõ URL hay tự gửi form cũng bị chặn.

    // Tìm tài khoản đang đăng nhập theo email. SQL -> AccountRepository.findByEmailIgnoreCase
    private Account currentHead(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
    }

    // Tìm bài kèm Lead và Topic. Không có -> lỗi 404.
    private Article findArticle(Integer articleId) {
        return articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
    }

    // Quyền XEM bài (cách A): Topic thuộc phòng mình HOẶC được giao cho mình; và mình không phải Lead (BR-34).
    // Dùng cho openWorkspace (mở trang). Sai quyền -> lỗi 403.
    private void checkCanReview(Account head, Article article) {
        // Câu hỏi 1: Topic của bài có được GIAO cho tôi không? (bảng assignment)
        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);

        // Câu hỏi 2: Topic có thuộc PHÒNG của tôi không? (cột topic.department_id)
        // Kiểm tra null trước: Topic do Thư ký tạo không có phòng ban; Trưởng phòng có thể chưa được gán phòng.
        Department headDepartment = head.getDepartment();
        Department topicDepartment = article.getTopic().getDepartment();
        boolean sameDepartment = headDepartment != null && topicDepartment != null
                && headDepartment.getId().equals(topicDepartment.getId());

        // Câu hỏi 3: tôi có phải Lead của bài không? Người duyệt không được duyệt bài mình phụ trách.
        boolean isLead = article.getLead().getId().equals(head.getId());

        // Không được giao VÀ không cùng phòng, HOẶC là Lead -> lỗi 403
        if ((!assignedToMe && !sameDepartment) || isLead) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_FORBIDDEN);
        }
    }

    // Quyền THAO TÁC + lượt duyệt đang mở trên version mới nhất, phải là của người đang thao tác.
    // Dùng cho mọi thao tác GHI (góp ý, xóa, đánh dấu, duyệt, trả bài).
    private ArticleReview myOpenReview(Account head, Article article) {
        // Thao tác thì PHẢI được giao (rule R09), cùng phòng thôi là chưa đủ; và không phải Lead (BR-34)
        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);
        boolean isLead = article.getLead().getId().equals(head.getId());
        if (!assignedToMe || isLead) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_FORBIDDEN);
        }
        // Bài phải đang chờ Trưởng phòng duyệt
        if (article.getStatus() != ArticleStatus.DEPARTMENT_REVIEW) {
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_IN_DEPARTMENT_REVIEW);
        }
        // Lấy version mới nhất (phần tử cuối danh sách)
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(article.getId());
        if (allVersions.isEmpty()) throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        ArticleVersion current = allVersions.get(allVersions.size() - 1);
        // Tìm lượt duyệt đang mở; chưa có thì tạo (giống BƯỚC 3 của openWorkspace)
        ArticleReview review = articleReviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                .orElseGet(() -> articleReviews.save(newDepartmentReview(current, head)));
        // Lượt duyệt đang mở là của người khác -> không được thao tác
        if (!review.getReviewer().getId().equals(head.getId())) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_TAKEN);
        }
        return review;
    }

    // ---------- Hàm phụ ----------

    // Tạo object lượt duyệt cấp DEPARTMENT mới (chưa lưu; nơi gọi sẽ articleReviews.save). decision để trống = "đang duyệt".
    private static ArticleReview newDepartmentReview(ArticleVersion version, Account head) {
        ArticleReview review = new ArticleReview();
        review.setVersion(version);
        review.setReviewer(head);
        review.setReviewStage(ReviewStage.DEPARTMENT);
        review.setReviewKind(ReviewKind.REVIEW);
        return review;
    }

    // Chốt một lượt duyệt: ghi quyết định, nhận xét và thời điểm quyết định.
    // decision và reviewed_at phải cùng có giá trị (ck_review_decided)
    private static void closeReview(ArticleReview review, ArticleReviewDecision decision, String feedback) {
        review.setDecision(decision);
        review.setFeedback(feedback);
        review.setReviewedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    // Góp ý gốc của mọi lượt duyệt trên một version, mỗi góp ý kèm các câu trả lời của nó
    private List<ReviewCommentDTO> rootComments(ArticleVersion version) {
        // SQL -> CommentRepository.findByVersion: mọi comment (cả góp ý gốc lẫn câu trả lời) trên version này
        List<Comment> all = comments.findByVersion(version.getId());
        return all.stream()
                // Giữ lại góp ý GỐC (không có parent)
                .filter(c -> c.getParent() == null)
                // Với mỗi góp ý gốc: tìm các câu trả lời của nó (parent = góp ý gốc này),
                // rồi mapper.toComment (ReviewWorkspaceMapper dòng 68) đổi thành ReviewCommentDTO kèm danh sách replies
                .map(root -> mapper.toComment(root, all.stream()
                        .filter(c -> c.getParent() != null && c.getParent().getId().equals(root.getId())).toList()))
                .toList();
    }

    // Góp ý của các vòng trước cho tab "Vòng trước" (UC-21, FR-08 "các Comment chưa RESOLVED"):
    //   - version LIỀN TRƯỚC: lấy hết (cả đã sửa lẫn chưa sửa) để Trưởng ban đối chiếu với bản mới
    //   - các version CŨ HƠN: chỉ lấy góp ý còn OPEN (chưa sửa), để không bị sót qua nhiều vòng
    // allVersions xếp tăng dần [v1.0, v1.1, ..., bản đang duyệt]; version đang duyệt không tính.
    private List<ReviewCommentDTO> earlierComments(List<ArticleVersion> allVersions) {
        List<ReviewCommentDTO> result = new ArrayList<>();
        int previousIndex = allVersions.size() - 2;   // vị trí version liền trước; < 0 nghĩa là bài mới nộp 1 lần
        if (previousIndex < 0) return result;
        result.addAll(rootComments(allVersions.get(previousIndex)));
        // Đi lùi từ version cũ hơn liền kề về v1.0
        for (int i = previousIndex - 1; i >= 0; i--) {
            for (ReviewCommentDTO comment : rootComments(allVersions.get(i))) {
                if (!comment.isResolved()) result.add(comment);
            }
        }
        return result;
    }

    // Đổi chữ "TITLE" / "SAPO" / "CONTENT"... (form gửi lên) thành enum AnchorField; chữ lạ thì báo lỗi.
    private static AnchorField parseAnchorField(String value) {
        try {
            return AnchorField.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
    }

    // Nội dung có thể là HTML từ trình soạn thảo: bỏ thẻ trước rồi mới đếm, để không đếm "<p>", "<img ...>" thành chữ
    private static int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        // Jsoup.parse(...).text(): bỏ hết thẻ HTML, chỉ giữ chữ
        String text = Jsoup.parse(content).text().trim();
        // Tách theo khoảng trắng rồi đếm số mảnh
        return text.isEmpty() ? 0 : text.split("\\s+").length;
    }

    // Đổi trạng thái bài (enum) thành chữ tiếng Việt cho thanh trạng thái (${workspace.statusText}).
    private static String statusText(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case DEPARTMENT_REVIEW -> "Chờ Trưởng phòng duyệt";
            case REVISION_REQUIRED -> "Đã trả Lead chỉnh sửa";
            case DEPARTMENT_APPROVED -> "Đã duyệt cấp phòng · chờ Thư ký";
            case APPROVED -> "Thư ký đã duyệt cuối";
            case SCHEDULED -> "Đã hẹn giờ đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }
}
