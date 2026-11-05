# Kiến thức Spring Boot: ghi chép phỏng vấn
Ngày: 2026-11-04 · Mục tiêu: kiểm tra những kiến thức Spring Boot đã trao đổi, tìm chỗ đã vững và chỗ cần ôn lại
Trạng thái: đang làm

## Tóm tắt các quyết định
- Đã vững: DispatcherServlet gọi controller; luồng AuthController → AuthService → DashboardController → DepartmentHeadController.
- Đã vững thêm: SecurityFilterChain chặn trước controller; bean được tạo lúc khởi động, method chỉ chạy khi có request; đường đi dữ liệu Service → DTO → model → `${...}`.
- Cần ôn lại: DispatcherServlet biết gọi method nào nhờ đâu (bảng tra từ @GetMapping lúc chạy app); redirect do trình duyệt gửi request mới; khác nhau giữa chưa đăng nhập (về /login) và sai role (403); Dependency Injection (Spring tạo Impl lúc chạy app, truyền qua constructor); @Transactional không liên quan tới việc khai báo biến; ý nghĩa transaction: làm hết hoặc không làm gì (commit / rollback).

## Các câu hỏi

### Câu 1 · Ai gọi controller
- Hỏi: Khi bạn gõ `/department-head/dashboard` trên trình duyệt, ai là người gọi method trong `DepartmentHeadController`, và làm sao nó biết phải gọi đúng method đó?
- Đã chốt: Bạn trả lời: "DispatchServlet sẽ gọi đến method get trong controller". Mô tả đúng luồng đăng nhập: GET login vào AuthController, POST dùng AuthService kiểm tra, thành công về /dashboard, DashboardController xem role rồi redirect sang /department-head/dashboard. Bạn tự nhận còn thiếu SecurityConfig.
- Đánh giá: Đúng phần "ai gọi". Thiếu phần "làm sao biết": lúc chạy app Spring đọc @GetMapping, @PostMapping để lập bảng tra URL → method. Chưa nói rõ redirect là trình duyệt tự gửi request mới.
- Còn bỏ ngỏ: Vai trò của SecurityConfig trong luồng -> hỏi ở câu 2.

### Câu 2 · SecurityConfig nằm ở đâu trong luồng
- Hỏi: Một phóng viên đã đăng nhập tự gõ `/department-head/dashboard`. Chuyện gì xảy ra, phần nào xử lý, và method trong DepartmentHeadController có chạy không? Vì sao?
- Đã chốt: Bạn trả lời: SecurityConfig kiểm tra bằng SecurityFilterChain, chưa đăng nhập thì đẩy về /login. DepartmentHeadController đã được tạo lúc khởi động "nhưng chưa gọi đến thôi".
- Đánh giá: Đúng: SecurityFilterChain xử lý, method không chạy, bean có từ lúc khởi động, và trường hợp chưa đăng nhập. Thiếu: phóng viên ĐÃ đăng nhập nhưng sai role thì nhận 403 (luật hasRole("DEPARTMENT_HEAD")), không bị đẩy về /login. Chỉnh cách nói: Spring tạo object (bean) từ class, không phải "bean tạo class".
- Còn bỏ ngỏ:

### Câu 3 · Bean và Dependency Injection
- Hỏi: Controller có dòng `private final DepartmentHeadService departmentHeadService;` và không có chữ `new` nào. Object thật trong biến đó thuộc class nào, ai tạo, vào lúc nào, và nó vào biến đó bằng đường nào?
- Đã chốt: Bạn trả lời: interface cho biết có những method nào; @Service nằm trong DepartmentHeadServiceImpl; "nhờ có @Transactional, việc khai báo sẽ gọn gàng hơn".
- Đánh giá: Đúng: interface liệt kê hành động, DepartmentHeadServiceImpl có @Service. Sai: @Transactional không liên quan tới việc khai báo gọn; object không nằm trong interface. Thiếu: Spring tạo object từ DepartmentHeadServiceImpl lúc chạy app nhờ @Service, rồi truyền vào qua constructor của controller.
- Còn bỏ ngỏ:

### Câu 4 · Transaction
- Hỏi: Trong `approve()`, code đổi 2 bảng: chốt lượt duyệt ở `article_review` và đổi trạng thái ở `article`. @Transactional dùng để làm gì ở đây? Nếu lỗi xảy ra sau khi đổi bảng thứ nhất thì dữ liệu ra sao?
- Đã chốt: Bạn trả lời: "để đổi status ở trong 2 bảng 1 bảng ArticleReview 1 bảng Article thì phải", tự nhận không nhớ rõ.
- Đánh giá: Nhớ đúng là liên quan tới 2 bảng, nhưng code đổi dữ liệu mới là phần đổi status. @Transactional gom các thay đổi thành "làm hết hoặc không làm gì"; lỗi giữa chừng thì rollback, cả 2 bảng giữ như cũ.
- Còn bỏ ngỏ:

### Câu 5 · Model, DTO và Thymeleaf
- Hỏi: Trong HTML có `${dashboard.fullName}`. Chữ `dashboard` lấy từ đâu, chữ `fullName` lấy từ đâu, và giá trị thật do code nào điền vào?
- Đã chốt: Bạn trả lời: `dashboard` từ `model.addAttribute("dashboard", departmentHeadService.getDashboard(email))`; `fullName` từ DTO; giá trị thật lấy từ account rồi `DashboardDTO.builder().fullName(head.getFullName())` điền vào.
- Đánh giá: Đúng. Chỉnh nhỏ: họ tên lấy bằng `getFullName()`, còn `getUsername()` là tên tài khoản.
- Còn bỏ ngỏ:

### Câu 6 · Khung layout
- Hỏi: Controller `return "layout/app"`, vậy vì sao trang vẫn hiện được nội dung trong `department_head/dashboard.html`? Hai dòng `contentView` và `contentFragment` có vai trò gì?
- Đã chốt: (chưa trả lời)
- Còn bỏ ngỏ:

## Việc còn bỏ ngỏ
