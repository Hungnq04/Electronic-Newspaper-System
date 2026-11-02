package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controller của các màn Trưởng ban. Controller = "cửa nhận" request: nhận yêu cầu, gọi service, trả về tên trang.
//
// @Controller: Spring tự tạo object của class này lúc khởi động và đọc các @GetMapping / @PostMapping bên dưới
//              để lập bảng tra "URL nào -> method nào".
// @RequestMapping("/department-head"): mọi URL trong class đều bắt đầu bằng /department-head.
//   Ví dụ @GetMapping("/dashboard") bên dưới = GET /department-head/dashboard.
// Trước khi tới đây, SecurityConfig.java (dòng "/department-head/**" -> hasRole("DEPARTMENT_HEAD")) đã chặn người không phải Trưởng ban.
@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {
    private static final String NEWSREADER_FONT = "https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,500;0,6..72,600;1,6..72,400;1,6..72,500&display=swap";
    // 2 service controller cần dùng. Khai báo kiểu interface; object thật là DepartmentHeadServiceImpl / DepartmentHeadReviewServiceImpl.
    private final DepartmentHeadService departmentHeadService;   // dữ liệu màn dashboard
    private final DepartmentHeadReviewService reviewService;     // màn thẩm định bài viết (workspace)

    // Constructor: Spring tự gọi lúc khởi động và truyền 2 service vào (Dependency Injection).
    // Đây là lý do trong dashboard() bên dưới, biến departmentHeadService đã có sẵn object để gọi.
    public DepartmentHeadController(DepartmentHeadService departmentHeadService, DepartmentHeadReviewService reviewService) {
        this.departmentHeadService = departmentHeadService;
        this.reviewService = reviewService;
    }

    // ===== Màn dashboard Trưởng ban: GET /department-head/dashboard =====
    // AI ĐƯA REQUEST TỚI ĐÂY?
    //   Đăng nhập xong, AuthController.login() trả "redirect:/dashboard"
    //   -> DashboardController.dashboard() thấy ROLE_DEPARTMENT_HEAD, trả "redirect:/department-head/dashboard"
    //   -> trình duyệt gửi GET /department-head/dashboard -> Spring (DispatcherServlet) tra bảng và gọi method này.
    //   Trước đó SidebarAdvice.addSidebar() đã bỏ menu bên trái (sidebarGroups) vào model.
    //
    // Tham số do Spring TỰ điền, mình không truyền:
    //   authentication = người đang đăng nhập (filter của SecurityConfig đọc từ session)
    //   model          = "cái túi" để gửi dữ liệu sang file HTML
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        // Đọc từ trong ra ngoài:
        //   1. currentEmail(authentication) (cuối file): lấy email người đăng nhập
        //   2. departmentHeadService.getDashboard(email) -> DepartmentHeadServiceImpl.getDashboard():
        //      gọi repository, tính số liệu, trả về DepartmentHeadDashboardDTO
        //   3. model.addAttribute("dashboard", ...): bỏ DTO vào túi với TÊN "dashboard"
        //      -> trong HTML đọc bằng ${dashboard.stats}, ${dashboard.displayName}...
        model.addAttribute("dashboard", departmentHeadService.getDashboard(currentEmail(authentication)));
        // Tiêu đề tab trình duyệt; layout/app.html in vào thẻ <title>.
        model.addAttribute("pageTitle", "Duyệt bản thảo – Trưởng ban");
        // 2 dòng này cho layout/app.html biết chèn phần ruột nào vào giữa trang:
        //   lấy fragment "departmentHeadContent" (dashboard.html dòng 38) trong file templates/department_head/dashboard.html
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");
        // Dùng chung giao diện của màn /reporter/tasks; file dashboard.css chỉ giữ vài chi tiết riêng
        // layout/app.html lặp qua danh sách này để in ra các thẻ <link rel="stylesheet">.
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css", "/department_head/css/department-head-dashboard.css"));
        // File JS riêng của trang (lọc, tìm kiếm, sắp xếp thẻ trên trình duyệt); layout/app.html in ra thẻ <script>.
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-dashboard.js"));
        // Trả về TÊN template: Spring + Thymeleaf mở templates/layout/app.html, điền dữ liệu trong model, gửi HTML về trình duyệt.
        return "layout/app";
    }

    // ---------- Màn thẩm định bài viết (workspace) ----------

    // =====================================================================================================
    // ===== MÀN THẨM ĐỊNH BÀI VIẾT (workspace review, FL-05) =====
    // Gồm 1 method GET (mở trang) + 5 method POST (các nút bấm trên trang). Tất cả gọi DepartmentHeadReviewService
    // (object thật: service/impl/DepartmentHeadReviewServiceImpl.java).
    //
    //   URL                                                        Method              Gọi service
    //   GET  /department-head/articles/{id}/review                 reviewWorkspace()   reviewService.openWorkspace()
    //   POST /department-head/articles/{id}/comments               addComment()        reviewService.addComment()
    //   POST /department-head/articles/{id}/comments/{c}/delete    deleteComment()     reviewService.deleteComment()
    //   POST /department-head/articles/{id}/comments/{c}/resolve   resolveComment()    reviewService.setPreviousCommentResolved()
    //   POST /department-head/articles/{id}/approve                approve()           reviewService.approve()
    //   POST /department-head/articles/{id}/request-revision       requestRevision()   reviewService.requestRevision()
    //
    // CÁCH CÁC NÚT HOẠT ĐỘNG (kiểu "POST rồi redirect về GET"):
    //   1. Trong workspacereview.html, mỗi nút nằm trong một <form method="post" th:action="@{/department-head/articles/...}">
    //   2. Bấm nút -> trình duyệt gửi POST -> Spring gọi method POST tương ứng bên dưới
    //   3. Method POST gọi service làm việc (ghi DB), rồi runAndReturn() trả "redirect:/department-head/articles/{id}/review"
    //   4. Trình duyệt tự gửi GET lại trang -> reviewWorkspace() chạy lại -> trang hiện dữ liệu MỚI + thông báo nhỏ (toast)
    //   Nhờ redirect, bấm F5 sau khi gửi form sẽ không gửi lại form lần nữa.
    // =====================================================================================================

    // Mở trang thẩm định. Ai đưa tới đây? Trên dashboard, bấm tiêu đề bài hoặc nút "Đọc & Thẩm định chi tiết"
    // (link th:href="@{/department-head/articles/{id}/review(...)}" trong department_head/dashboard.html).
    // Màn thẩm định nằm trong layout chung (sidebar + nội dung), giống màn dashboard
    @GetMapping("/articles/{articleId}/review")
    // @PathVariable: lấy số nằm TRONG đường dẫn. Ví dụ URL /department-head/articles/15/review -> articleId = 15
    public String reviewWorkspace(@PathVariable Integer articleId, Authentication authentication, Model model) {
        // Đọc từ trong ra ngoài:
        //   1. currentEmail(authentication): email người đăng nhập (hàm phụ cuối file)
        //   2. reviewService.openWorkspace(email, articleId) -> DepartmentHeadReviewServiceImpl.openWorkspace():
        //      kiểm tra quyền, mở lượt duyệt nếu cần, gom dữ liệu, trả về ReviewWorkspaceDTO
        //      (nếu không có quyền / không thấy bài -> ném ApplicationException -> handleError() bên dưới trả lỗi 403/404)
        //   3. Bỏ DTO vào model với TÊN "workspace" -> workspacereview.html đọc bằng ${workspace...} (trong file gọi tắt là "w")
        model.addAttribute("workspace", reviewService.openWorkspace(currentEmail(authentication), articleId));
        model.addAttribute("pageTitle", "Thẩm định bản thảo – Trưởng ban");
        // layout/app.html chèn fragment "reviewWorkspaceContent" (workspacereview.html dòng có th:fragment) vào giữa trang
        model.addAttribute("contentView", "department_head/workspacereview");
        model.addAttribute("contentFragment", "reviewWorkspaceContent");
        // Phông Newsreader có chữ nghiêng (cho sapo) + giao diện chung của /reporter/tasks + CSS riêng của màn thẩm định
        model.addAttribute("pageCss", List.of(NEWSREADER_FONT, "/reporter/css/reporter-tasks.css", "/department_head/css/department-head-workspace-review.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-workspace-review.js"));
        // Đang thẩm định một bài thì sidebar vẫn sáng mục "Duyệt bản thảo"
        // (ghi đè giá trị activePath mà SidebarAdvice đã đặt sẵn trước khi method này chạy)
        model.addAttribute("activePath", "/department-head/dashboard");
        return "layout/app";
    }

    // ----- Nút "Thêm góp ý" (gắn vị trí) và "Thêm góp ý chung" -----
    // Form gửi tới đây (workspacereview.html):
    //   - Góp ý gắn vị trí: form ẩn id="comment-form". JS (popoverSave trong department-head-workspace-review.js)
    //     điền anchorField / anchorParagraph / anchorStart / anchorEnd / content rồi gửi.
    //   - Góp ý chung: form class="ws-general-form", chỉ có ô content (anchorField để trống).
    // @ModelAttribute CommentRequestDTO: Spring tạo CommentRequestDTO và đổ các ô form vào THEO TÊN
    //   (input name="content" -> setContent(...), name="anchorField" -> setAnchorField(...)...).
    // RedirectAttributes flash: "túi" chứa thông báo sống qua đúng 1 lần redirect (dùng cho toast).
    @PostMapping("/articles/{articleId}/comments")
    public String addComment(@PathVariable Integer articleId, @ModelAttribute CommentRequestDTO request,
                             Authentication authentication, RedirectAttributes flash) {
        // runAndReturn: chạy đoạn "() -> ..." (gọi service), thành công thì toast "Đã lưu góp ý.", lỗi thì toast lỗi,
        // rồi redirect về trang thẩm định.
        // "() -> reviewService.addComment(...)" là lambda = một đoạn code được ĐƯA cho runAndReturn để nó tự chạy bên trong try.
        return runAndReturn(articleId, flash, "Đã lưu góp ý.",
                () -> reviewService.addComment(currentEmail(authentication), articleId, request));
    }

    // ----- Nút "Xóa" trên một thẻ góp ý của vòng này -----
    // Form: <form data-confirm="Xóa góp ý này?" th:action="@{/department-head/articles/{a}/comments/{c}/delete(...)}">
    // JS hỏi lại "Xóa góp ý này?" trước khi gửi. Hai @PathVariable: articleId và commentId đều lấy từ đường dẫn.
    @PostMapping("/articles/{articleId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Integer articleId, @PathVariable Integer commentId,
                                Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã xóa góp ý.",
                () -> reviewService.deleteComment(currentEmail(authentication), articleId, commentId));
    }

    // ----- Nút "Đã sửa" / "Chưa sửa" trên góp ý của VÒNG TRƯỚC (tab "Vòng trước") -----
    // Hai form khác nhau ở tham số ?resolved=true hoặc ?resolved=false trên URL.
    // @RequestParam boolean resolved: lấy giá trị sau dấu ? trong URL.
    @PostMapping("/articles/{articleId}/comments/{commentId}/resolve")
    public String resolveComment(@PathVariable Integer articleId, @PathVariable Integer commentId,
                                 @RequestParam boolean resolved, Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, resolved ? "Đã đánh dấu: Lead đã sửa." : "Đã đánh dấu: Lead chưa sửa.",
                () -> reviewService.setPreviousCommentResolved(currentEmail(authentication), articleId, commentId, resolved));
    }

    // ----- Nút "Xác nhận duyệt" trong hộp thoại "Duyệt bài cấp phòng?" (id="modal-approve") -----
    // @RequestParam(required = false) String feedback: ô ghi chú name="feedback", không bắt buộc.
    // Service đổi bài DEPARTMENT_REVIEW -> DEPARTMENT_APPROVED (chưa phải Final Approve; bài sang hàng chờ Thư ký).
    @PostMapping("/articles/{articleId}/approve")
    public String approve(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                          Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã duyệt cấp phòng. Bài chuyển sang Thư ký tòa soạn.",
                () -> reviewService.approve(currentEmail(authentication), articleId, feedback));
    }

    // ----- Nút "Gửi trả bài" trong hộp thoại "Yêu cầu sửa bài" (id="modal-revise") -----
    // feedback ở đây là BẮT BUỘC (BR-12). Ghi required = false để Spring không tự báo lỗi 400;
    // việc kiểm tra "có nhận xét chưa" do service làm, báo lỗi bằng toast cho dễ hiểu.
    // Service đổi bài DEPARTMENT_REVIEW -> REVISION_REQUIRED (trả về Lead).
    @PostMapping("/articles/{articleId}/request-revision")
    public String requestRevision(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                                  Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã trả bài cho Lead chỉnh sửa.",
                () -> reviewService.requestRevision(currentEmail(authentication), articleId, feedback));
    }

    // Lỗi khi MỞ trang (không có quyền, không tìm thấy bài...): trả đúng mã HTTP của ErrorCode (403, 404...)
    // @ExceptionHandler: nếu BẤT KỲ method nào trong controller này ném ApplicationException mà không tự bắt,
    // Spring gọi method này thay vì hiện trang lỗi 500. Thực tế chỉ xảy ra ở reviewWorkspace(),
    // vì các method POST đã tự bắt lỗi trong runAndReturn().
    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }

    // Chạy một thao tác rồi quay lại trang workspace, kèm thông báo thành công hoặc lỗi (hiện dạng toast)
    // Dùng chung cho cả 5 method POST ở trên để khỏi viết lại try/catch 5 lần.
    //   action         = đoạn code gọi service (lambda truyền vào)
    //   successMessage = câu thông báo khi thành công
    private static String runAndReturn(Integer articleId, RedirectAttributes flash, String successMessage, Runnable action) {
        try {
            // Chạy đoạn code gọi service. Nếu service phát hiện sai (không có quyền, thiếu nhận xét...) nó ném ApplicationException.
            action.run();
            // Thành công: gửi kèm "toast" -> workspacereview.html đọc ${toast} ở thẻ id="toast" (cuối file) -> JS hiện thông báo
            flash.addFlashAttribute("toast", successMessage);
        } catch (ApplicationException ex) {
            // Thất bại: gửi kèm "toastError" (message tiếng Việt lấy từ ErrorCode) -> hiện thông báo màu đỏ
            flash.addFlashAttribute("toastError", ex.getMessage());
        }
        // Dù thành công hay lỗi đều quay lại trang thẩm định -> reviewWorkspace() chạy lại với dữ liệu mới
        return "redirect:/department-head/articles/" + articleId + "/review";
    }

    // Hàm phụ, được gọi ở dòng đầu của dashboard() (và các màn thẩm định) để lấy email người đang đăng nhập.
    // Đăng nhập mật khẩu: getName() là email. Đăng nhập Google: getName() là mã "sub" của Google, email nằm trong thuộc tính "email"
    private static String currentEmail(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) {
            return email.trim();
        }
        return authentication.getName();
    }
}
