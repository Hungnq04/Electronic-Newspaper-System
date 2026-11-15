# Giải thích luồng các màn hình FL-01 (Iter 1, 2, 3)

Tài liệu ôn tập để trả lời câu hỏi kiểu: **"Bấm nút này xong thì hệ thống đã làm những gì để ra được màn kia?"**

Mỗi màn có: URL, file liên quan, từng bước chạy (trình duyệt → Security → Controller → Service → Repository → DB → Thymeleaf), kiểm tra quyền, đổi trạng thái, và câu thầy có thể hỏi.

> Đường dẫn tính từ thư mục gốc dự án. Số dòng có thể lệch khi file bị sửa; khi đó tìm theo **tên method**.
> Package gốc: `src/main/java/com/group6/electronic_drafting_system/` (dưới đây viết tắt là `.../`).

---

## Mục lục

- [Phần 0. Khung chung mà màn nào cũng đi qua](#phần-0-khung-chung-mà-màn-nào-cũng-đi-qua)
- **Iter 1**
  - [1.1 Login (pop-up) → vào màn chính theo vai trò](#11-login-pop-up--vào-màn-chính-theo-vai-trò)
  - [1.2 Danh sách đề tài (Topic Proposal List)](#12-danh-sách-đề-tài-topic-proposal-list)
  - [1.3 Đề xuất đề tài mới + modal xác nhận](#13-đề-xuất-đề-tài-mới--modal-xác-nhận)
  - [1.4 Logout](#14-logout)
- **Iter 2**
  - [2.1 Trưởng ban duyệt đề tài (Topic Review Workspace)](#21-trưởng-ban-duyệt-đề-tài-topic-review-workspace)
  - [2.2 Phóng viên xem góp ý (Topic Feedback Viewer)](#22-phóng-viên-xem-góp-ý-topic-feedback-viewer)
  - [2.3 Phóng viên sửa và gửi lại đề tài (Edit & Resubmit)](#23-phóng-viên-sửa-và-gửi-lại-đề-tài-edit--resubmit)
  - [2.4 Thư ký duyệt cuối (Final Topic Review)](#24-thư-ký-duyệt-cuối-final-topic-review)
  - [2.5 Chuông thông báo](#25-chuông-thông-báo)
- **Iter 3**
  - [3.1 Trang /login chứa nhiều pop-up](#31-trang-login-chứa-nhiều-pop-up)
  - [3.2 Đăng ký + OTP](#32-đăng-ký--otp)
  - [3.3 Gửi lại mã OTP](#33-gửi-lại-mã-otp)
  - [3.4 Quên mật khẩu + Đặt lại mật khẩu](#34-quên-mật-khẩu--đặt-lại-mật-khẩu)
  - [3.5 Đăng nhập / đăng ký bằng Google](#35-đăng-nhập--đăng-ký-bằng-google)
  - [3.6 Trang 403](#36-trang-403)
- [Phần 4. Sơ đồ trạng thái tổng hợp](#phần-4-sơ-đồ-trạng-thái-tổng-hợp)
- [Phần 5. Câu hỏi khó và cách trả lời](#phần-5-câu-hỏi-khó-và-cách-trả-lời)
- [Phần 6. Những chỗ code khác tài liệu (nên biết trước)](#phần-6-những-chỗ-code-khác-tài-liệu-nên-biết-trước)

---

## Phần 0. Khung chung mà màn nào cũng đi qua

Hiểu phần này thì giải thích màn nào cũng theo đúng một khuôn.

### 0.1. Một request đi qua những đâu

```
Trình duyệt  ──HTTP──►  Spring Security (filter chain)  ──►  DispatcherServlet  ──►  Controller
                         - đã đăng nhập chưa?                  - tìm method có         │
                         - URL này cần role gì?                  @GetMapping/@PostMapping khớp URL
                         - có CSRF token chưa (POST)?                                   ▼
                                                                              Service (@Transactional)
                                                                              - kiểm tra quyền LẦN 2
                                                                              - kiểm tra trạng thái
                                                                              - đổi dữ liệu
                                                                                        ▼
                                                                              Repository (JPA) ──► SQL Server
                                                                                        ▼
                                                     Controller nhận DTO, bỏ vào Model, trả tên template
                                                                                        ▼
                                                     Thymeleaf ghép layout/app.html + fragment → HTML
```

### 0.2. Kiểm tra quyền 2 lớp

| Lớp | Ở đâu | Chặn cái gì |
|---|---|---|
| Lớp 1 | `config/SecurityConfig.java` | Theo **URL**: `/reporter/**` chỉ `REPORTER`, `/department-head/**` chỉ `DEPARTMENT_HEAD`, `/secretary/**` chỉ `EDITORIAL_SECRETARY`, `/admin/**` chỉ `ADMIN`. Sai role thì 403. |
| Lớp 2 | Trong Service | Theo **dữ liệu**: đề tài này có phải của mình không, có đúng ban không, trạng thái còn cho làm không. Sai thì ném `ApplicationException(ErrorCode.XXX)`. |

**Tại sao cần lớp 2?** Lớp 1 chỉ biết "bạn là Phóng viên". Nó không biết đề tài số 15 là của Phóng viên A hay B. Nếu không có lớp 2, Phóng viên B gõ `/reporter/topics/15/edit` là sửa được đề tài của A.

### 0.3. Lỗi được trả về thế nào

- Service ném `ApplicationException` kèm `ErrorCode` (trong `.../exception/ErrorCode.java`). Mỗi mã có câu tiếng Việt và `HttpStatus` (403, 404, 409, 400...).
- Mỗi controller có `@ExceptionHandler(ApplicationException.class)`, gọi `response.sendError(mã, câu)`. Trình duyệt nhận trang lỗi đúng mã HTTP.
- Lỗi "nghiệp vụ nhẹ" (thiếu góp ý, đề tài đã được người khác xử lý) thì **không** ra trang lỗi. Controller bắt lại, gửi `error` qua flash attribute, rồi redirect về màn cũ.

### 0.4. Layout chung

Mọi màn sau khi đăng nhập đều `return "layout/app"` và đặt sẵn vào model:

| Biến model | Ý nghĩa |
|---|---|
| `contentView` | file template chứa nội dung, ví dụ `reporter/topic-proposal-list` |
| `contentFragment` | tên `th:fragment` trong file đó, ví dụ `topicProposalListContent` |
| `pageCss`, `pageJs` | CSS / JS riêng của màn |
| `pageTitle` | tiêu đề tab |
| `activePath` | để sidebar tô sáng mục đang mở |

`layout/app.html` dòng 31: `th:replace="~{__${contentView}__ :: __${contentFragment}__}"`. Nghĩa là "lấy fragment tên X trong file Y, chèn vào đây".

`config/SidebarAdvice.java` là `@ControllerAdvice`: **chạy trước mọi controller**, đọc role của người đăng nhập và thêm `sidebarGroups` (menu) vào model. Menu lấy từ `service/SidebarService.java`.

### 0.5. Ba công cụ hay dùng

- **Redirect sau POST (mẫu PRG: Post → Redirect → Get).** Sau khi submit form, controller không trả HTML ngay mà `return "redirect:/..."`. Nhờ vậy bấm F5 không gửi lại form lần nữa.
- **Flash attribute** (`redirect.addFlashAttribute("success", "...")`): dữ liệu sống qua **đúng một** lần redirect, dùng để hiện thông báo "Đã gửi thành công".
- **`@Transactional`**: một method service = một transaction. Lỗi giữa chừng thì mọi thay đổi bị hủy. Trong transaction, chỉ cần `topic.setStatus(...)` là đủ, không cần gọi `save()`: khi commit, Hibernate tự thấy object đã đổi và sinh câu `UPDATE` (gọi là *dirty checking*).

---

# ITER 1

## 1.1. Login (pop-up) → vào màn chính theo vai trò

**URL:** `GET /login` (hiện trang), `POST /login` (gửi form)
**File:** `templates/login.html`, `controller/AuthController.java` (`loginPage`, `login`), `service/impl/AuthServiceImpl.java` (`login`, `persistSecurityContext`), `config/SecurityConfig.java`, `controller/DashboardController.java`

### Các bước

1. **Mở trang.** Trình duyệt gọi `GET /login`. URL này `permitAll()` trong SecurityConfig nên ai cũng vào được. `AuthController.loginPage()` đặt `popup = "login"` vào model và trả template `login`. Pop-up Đăng nhập hiện ra.
2. **Submit form.** Form `<form method="post" th:action="@{/login}">` gửi `email` và `password`. Thymeleaf tự chèn thêm ô ẩn `_csrf`, nên Spring Security không chặn.
3. **Controller** [`AuthController.login()`](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L97) gom 2 ô vào `LoginRequestDTO` rồi gọi `authService.login(email, password)`.
4. **Service** [`AuthServiceImpl.login()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L70):
   1. `accounts.findByEmailIgnoreCase(email)`: không thấy thì ném `AUTH_INVALID_CREDENTIALS`.
   2. `encoder.matches(password, account.getPasswordHash())`: so mật khẩu bằng **BCrypt**. DB chỉ lưu chuỗi băm, không lưu mật khẩu gốc. Sai thì ném lỗi y hệt trên ("Email hoặc mật khẩu không đúng"), để người lạ không đoán được email nào có tồn tại.
   3. **Mật khẩu đúng rồi** mới xét `status`:
      - `LOCKED` → "Tài khoản đã bị khóa"
      - `INACTIVE` → "Tài khoản đã ngừng hoạt động"
      - `AWAITING_APPROVAL` → "Đang chờ quản trị viên duyệt"
      - `PENDING` (chưa nhập OTP) → không cho vào, gửi mã OTP nếu mã cũ hết hạn, trả về để controller mở pop-up OTP
      - `ACTIVE` → đi tiếp
   4. `authenticationManager.authenticate(...)`: Spring Security tự xác thực lại. Nó gọi `UserDetailsService` trong SecurityConfig để nạp user và gắn quyền `ROLE_REPORTER` / `ROLE_DEPARTMENT_HEAD`... (lấy từ bảng `role`).
   5. Đặt kết quả vào `SecurityContextHolder`.
5. **Lưu đăng nhập vào session.** `persistSecurityContext(request)` lưu `SecurityContext` vào HTTP session. Trình duyệt giữ cookie `JSESSIONID`. Từ request sau, Spring Security đọc session và biết bạn là ai.
6. Controller `return "redirect:/dashboard"`.
7. **Chuyển theo role.** [`DashboardController`](../src/main/java/com/group6/electronic_drafting_system/controller/DashboardController.java) có bảng:

   | Quyền | Đi tới |
   |---|---|
   | `ROLE_REPORTER` | `/reporter/topics` (Danh sách đề tài) |
   | `ROLE_DEPARTMENT_HEAD` | `/department-head/dashboard` |
   | `ROLE_EDITORIAL_SECRETARY` | `/secretary/final-reviews` |
   | `ROLE_ADMIN` | `/admin/accounts` |

8. Trình duyệt gọi `GET /reporter/topics`. Từ đây sang **mục 1.2**.

### Khi lỗi

Controller bắt `ApplicationException`, gửi `error` (hoặc `message` nếu là "chờ duyệt") và `email` qua flash rồi `redirect:/login`. Pop-up Đăng nhập hiện lại, email vẫn còn trong ô, mật khẩu thì không giữ.

### Câu hỏi có thể gặp

- **"Sao không dùng `formLogin` mặc định của Spring?"** Vì cần xử lý riêng `PENDING` (mở pop-up OTP) và `AWAITING_APPROVAL` (thông báo màu xanh, không phải lỗi). Màn mặc định chỉ báo được "sai thông tin". Trong SecurityConfig vẫn còn `formLogin(...loginProcessingUrl("/process-login"))`, nhưng thực tế chỉ dùng phần `loginPage("/login")` để Spring biết chưa đăng nhập thì đẩy về đâu. Form không gửi tới `/process-login`.
- **"Mật khẩu kiểm tra ở đâu?"** Hai lần: `encoder.matches` trong service, rồi `authenticationManager.authenticate`.
- **"Sao kiểm tra mật khẩu trước rồi mới tới trạng thái?"** Để người không biết mật khẩu thì không biết được tài khoản đó đang bị khóa hay đang chờ duyệt.

---

## 1.2. Danh sách đề tài (Topic Proposal List)

**URL:** `GET /reporter/topics` và `GET /reporter/topics?status=REJECTED` (lọc theo tab)
**File:** `controller/TopicProposalController.java` (`list`), `service/impl/TopicServiceImpl.java` (`getProposalList`, `visibleTopics`), `repository/TopicRepository.java`, `mapper/Topic/TopicMapper.java`, `dto/Topic/TopicProposalListDTO.java`, `dto/Topic/TopicListItemDTO.java`, `templates/reporter/topic-proposal-list.html`, `static/reporter/js/topic-proposal.js`

### Các bước

1. **Security:** `/reporter/**` yêu cầu `hasRole("REPORTER")`. Trưởng ban gõ URL này thì nhận 403.
2. **Controller** [`list()`](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L46):
   - `currentEmail(auth)` lấy email người đăng nhập. Đăng nhập form: `auth.getName()` chính là email. Đăng nhập Google: lấy thuộc tính `email` của `OAuth2User`.
   - `parseStatus(status)` đổi chữ trên URL thành enum `TopicStatus`. Giá trị lạ (ví dụ `?status=abc`) thì coi như tab "Tất cả".
   - Gọi `topicService.getProposalList(email, status)` rồi bỏ kết quả vào model với tên `list`.
   - `page(...)` đặt `contentView`, `contentFragment`, CSS/JS và trả `"layout/app"`.
3. **Service** [`getProposalList()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L58), `@Transactional(readOnly = true)` vì chỉ đọc:
   1. Tìm account theo email. Không thấy thì ném `TOPIC_ACCESS_DENIED` (403).
   2. `visibleTopics(me)`: Reporter gọi `topics.findByCreator_IdOrderByCreatedAtDescIdDesc(me.getId())`, tức là **chỉ lấy đề tài do chính mình tạo**, mới nhất trước.
      - Spring Data JPA tự sinh SQL từ tên method: `findBy` + `Creator_Id` (cột `creator_id`) + `OrderBy CreatedAt Desc, Id Desc`. Tương đương:
        ```sql
        SELECT t.* FROM topic t LEFT JOIN department d ON d.department_id = t.department_id
        WHERE t.creator_id = @me ORDER BY t.created_at DESC, t.topic_id DESC;
        ```
      - `@EntityGraph(attributePaths = "department")` nạp luôn phòng ban trong cùng câu SQL, tránh mỗi dòng lại chạy thêm 1 câu (lỗi "N+1").
   3. **Đếm theo trạng thái** (`groupingBy`) trên **toàn bộ** đề tài, không phụ thuộc tab đang chọn. Vì vậy số trên các tab và 5 thẻ thống kê luôn đúng.
   4. Tạo danh sách tab: "Tất cả" + 4 trạng thái (`DEPARTMENT_REVIEW`, `SECRETARY_REVIEW`, `ACTIVE`, `REJECTED`). Tab nào trùng `statusFilter` thì `active = true`.
   5. **Lọc theo tab ở server**, rồi chuyển từng `Topic` (entity) sang `TopicListItemDTO` bằng `mapper.toListItem`. Mapper tạo mã hiển thị `TP-<năm>-<id 4 số>`, ví dụ `TP-2026-0012`.
   6. Lấy số thông báo chưa đọc + 20 thông báo mới nhất cho chuông (xem **mục 2.5**).
   7. Trả `TopicProposalListDTO` (record gồm tên, chức danh, chữ viết tắt avatar, tabs, topics, thông báo).
4. **Thymeleaf** (`topic-proposal-list.html`, fragment `topicProposalListContent`):
   - Topbar dùng chung (`tpTopbar`): chuông, avatar, tên. Chuông có `sec:authorize="hasRole('REPORTER')"`.
   - 5 thẻ thống kê: `list.countOf('department-review')`...
   - Tab: mỗi tab là một link `<a href="/reporter/topics?status=...">`. **Bấm tab là gửi request mới lên server.**
   - Bảng: `th:each="t : ${list.topics()}"`, badge màu theo `t.statusKey()` (ví dụ `tp-badge--department-review`).
   - Nút **Sửa** chỉ hiện khi `status == DEPARTMENT_REVIEW` **và** `awaitingRevision` (Trưởng ban đang yêu cầu sửa). Đây chỉ là ẩn nút; server vẫn kiểm tra lại (mục 2.3).
5. **JS** (`topic-proposal.js`): ô tìm nhanh lọc **trên trình duyệt**, ẩn các dòng không chứa từ khóa (dựa vào `data-tp-search-text`). Không gọi server.

### Câu hỏi có thể gặp

- **"Lọc tab với tìm kiếm khác nhau thế nào?"** Tab lọc ở server (đổi URL, chạy lại query). Tìm kiếm lọc bằng JS trên những dòng đang hiện.
- **"Sao Phóng viên A không thấy đề tài của B?"** Vì query lọc `creator_id = id của A`, và id lấy từ phiên đăng nhập chứ không lấy từ URL.
- **"Entity với DTO khác gì?"** Entity `Topic` ánh xạ đúng bảng `topic` trong DB. DTO `TopicListItemDTO` chỉ chứa những gì màn hình cần (mã, tiêu đề, tên ban, nhãn trạng thái). Không đưa entity thẳng ra view để tránh lộ dữ liệu thừa và lỗi lazy loading.

---

## 1.3. Đề xuất đề tài mới + modal xác nhận

**URL:** `GET /reporter/topics/new` (mở form), `POST /reporter/topics` (gửi)
**File:** `TopicProposalController` (`createForm`, `create`, `formPage`), `TopicServiceImpl.createProposal`, `TopicMapper.toNewTopic`, `dto/Topic/TopicCreateRequestDTO.java`, `templates/reporter/topic-proposal-create.html`, `topic-proposal.js`

### Mở form (GET)

1. `createForm()` tạo một `TopicCreateRequestDTO` rỗng tên `topicForm`, rồi gọi `formPage(auth, model, null)`. `null` nghĩa là chế độ **tạo mới** (khác chế độ sửa ở mục 2.3).
2. `formPage` đưa vào model: `list` (cho topbar), `myDepartment` (ban của phóng viên), `titleMax = 255`, `summaryMax = 4000`.
3. Template hiện ô **Tiêu đề**, **Ban phụ trách** (chỉ đọc, không cho chọn) và **Tóm tắt**. Ô có `th:field="*{title}"` nên Thymeleaf tự gắn `name`, `id`, `value`.

### Bấm "Gửi duyệt" (phía trình duyệt)

1. Nút có `type="button"` nên **không** submit form, chỉ gọi `openConfirm()` trong JS.
2. JS kiểm tra 2 ô không được trống. Thiếu thì hiện lỗi đỏ và đặt con trỏ vào ô sai.
3. Hợp lệ thì mở **modal "Xác nhận gửi đề tài"**, hiện lại tiêu đề và tên ban.
4. Bấm **"Hủy, quay lại chỉnh sửa"**: đóng modal, dữ liệu trên form còn nguyên.
5. Bấm **"Xác nhận gửi"**: khóa 2 nút (tránh bấm 2 lần gửi trùng) rồi `form.submit()`. Lúc này mới có `POST /reporter/topics`.

### Server xử lý (POST)

1. **Controller** [`create()`](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L60):
   - `@Valid @ModelAttribute("topicForm") TopicCreateRequestDTO`: Spring gom dữ liệu form vào DTO và chạy **Bean Validation** theo annotation trong DTO: `@NotBlank` (không trống), `@Size(max = 255)` / `@Size(max = 4000)`.
   - `BindingResult binding` chứa lỗi. Có lỗi thì hiện lại form (không redirect) để `th:errors` in câu lỗi dưới từng ô.
2. **Service** [`createProposal()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L88), `@Transactional`:
   1. `requireActiveReporter`: phải đúng role `REPORTER` **và** `status = ACTIVE`, nếu không thì ném `TOPIC_CREATE_FORBIDDEN` (403). Kiểm tra lại dù SecurityConfig đã chặn URL.
   2. `validate(request)`: kiểm tra lại độ dài lần nữa, vì không tin dữ liệu gửi lên.
   3. Phóng viên **chưa được gán ban** thì ném `TOPIC_REPORTER_NO_DEPARTMENT`.
   4. Ban của phóng viên **phải có Trưởng ban đang ACTIVE** (`departments.findHavingAccountWithRole(DEPARTMENT_HEAD, ACTIVE)`). Nếu không, đề tài gửi lên sẽ không có ai duyệt, nên ném `TOPIC_DEPARTMENT_INVALID`.
   5. `mapper.toNewTopic(...)` tạo entity `Topic`:
      - `creator` = phóng viên, `department` = ban của phóng viên
      - `category` = tên ban bỏ chữ "Phòng " (ví dụ "Phòng Kinh tế" thành "Kinh tế")
      - `priority = NORMAL`
      - **`status = DEPARTMENT_REVIEW`** (đi thẳng vào hàng chờ Trưởng ban, không có bước nháp)
      - `awaitingRevision = false`
   6. `topics.save(...)`: Hibernate chạy `INSERT INTO topic ...`.
3. Thành công thì flash `success` = "Đã gửi đề tài thành công. Đề tài đang chờ Trưởng ban duyệt." và `redirect:/reporter/topics`. Danh sách hiện đề tài mới ở đầu, badge "Chờ Trưởng ban duyệt".
4. Lỗi nghiệp vụ (chưa có ban, ban không có Trưởng ban) thì hiện lại form kèm câu lỗi đỏ, dữ liệu đã nhập vẫn còn.

### Câu hỏi có thể gặp

- **"Kiểm tra form ở JS rồi sao server còn kiểm tra?"** JS chỉ để báo lỗi nhanh. Người dùng có thể tắt JS hoặc gửi request bằng công cụ khác, nên server phải kiểm tra lại (`@Valid` + `validate()`).
- **"Sao không cho chọn ban?"** Quy ước nhóm: phóng viên chỉ đề xuất cho ban của mình, để đề tài tới đúng Trưởng ban quản lý mình.
- **"Đề tài mới có trạng thái gì?"** `DEPARTMENT_REVIEW`, cột `awaiting_revision = 0`.

---

## 1.4. Logout

**File:** `templates/fragment/sidebar.html` (form logout), `SecurityConfig` (`.logout(...)`)

1. Nút "Đăng xuất" ở sidebar là `<form method="post" th:action="@{/logout}">`. Dùng **POST** kèm CSRF token để trang web khác không lừa trình duyệt đăng xuất bạn.
2. Spring Security tự xử lý `/logout` (không cần viết controller): xóa session, xóa `SecurityContext`.
3. `logoutSuccessUrl("/login?logout")`: về trang login. `loginPage()` thấy tham số `logout` nên hiện "Bạn đã đăng xuất."

---

# ITER 2

Iter 2 là phần **duyệt đề tài 2 cấp** (Trưởng ban, rồi Thư ký) và vòng **yêu cầu sửa**.

**Bảng dữ liệu liên quan:**

| Bảng / cột | Dùng để |
|---|---|
| `topic.status` | `DEPARTMENT_REVIEW` / `SECRETARY_REVIEW` / `ACTIVE` / `REJECTED` |
| `topic.awaiting_revision` (BIT) | `1` = Trưởng ban đang chờ phóng viên sửa (status **vẫn** là `DEPARTMENT_REVIEW`) |
| `topic.reviewed_by_head_id` | nhớ Trưởng ban nào đã duyệt (để lúc Thư ký duyệt thì tạo assignment cho đúng người) |
| `topic_feedback` | mỗi lần Trưởng ban **yêu cầu sửa** = 1 dòng (`type = CHANGE_REQUEST`), được nhiều vòng |
| `topic_review` | **quyết định** duyệt / từ chối của từng cấp (`review_stage = DEPARTMENT / SECRETARY`) |
| `assignment` | Thư ký duyệt xong thì tạo 2 dòng: `LEAD` và `DEPARTMENT_HEAD` |
| `notification` | thông báo trong chuông của phóng viên |

> **Vì sao yêu cầu sửa không ghi vào `topic_review`?** Trong DB, `topic_review` mỗi cấp chỉ được 1 quyết định (rule R10/R12). Yêu cầu sửa có thể lặp nhiều vòng nên ghi riêng vào `topic_feedback`.

---

## 2.1. Trưởng ban duyệt đề tài (Topic Review Workspace)

**URL:** `GET /department-head/topics` và `GET /department-head/topics?topic=15` (chọn đề tài), `POST /department-head/topics/{id}/approve`, `POST /department-head/topics/{id}/request-changes`
**File:** `controller/DepartmentHeadTopicController.java`, `service/impl/TopicReviewServiceImpl.java`, `templates/department_head/topic-review.html`, `templates/fragment/topic-review.html` (phần dùng chung với Thư ký)
**Sidebar:** mục "Duyệt đề tài"

### Mở màn (GET)

1. **Security:** `/department-head/**` chỉ cho `DEPARTMENT_HEAD`.
2. **Controller** `workspace()` gọi `topicReviewService.getHeadWorkspace(principal.getName(), topic)`. `principal.getName()` là email.
3. **Service** [`getHeadWorkspace()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L66):
   1. `requireRole(email, DEPARTMENT_HEAD)`: phải đúng role, đang `ACTIVE` **và có ban**. Sai thì ném `TOPIC_REVIEW_FORBIDDEN` (403).
   2. **Hàng chờ** = `findByStatusAndDepartment_IdAndAwaitingRevisionFalseOrderByCreatedAtAscIdAsc(DEPARTMENT_REVIEW, banCuaToi)`, tức là:
      - status `DEPARTMENT_REVIEW`
      - đúng **ban của mình**
      - **không** đang chờ phóng viên sửa (`awaiting_revision = 0`)
      - gửi **sớm nhất trước** (Asc)
   3. Mỗi đề tài chuyển sang `TopicReviewItemDTO`, kèm **số vòng đã sửa** (`countByTopic_IdAndType(..., CHANGE_REQUEST)`).
   4. `pickSelected`: có `?topic=` trên URL thì lấy đề tài đó, không có thì lấy đề tài đầu hàng chờ.
   5. **Chặn gõ URL:** đề tài được chọn không cùng ban thì ném `TOPIC_VIEW_FORBIDDEN` (403).
   6. `canDecide` = đề tài còn `DEPARTMENT_REVIEW` và không chờ sửa. Chỉ khi `true` mới hiện nút quyết định.
   7. `historyOf(selected)`: gộp `topic_feedback` + `topic_review`, sắp theo thời gian (dòng thời gian góp ý).
4. **Giao diện:** 3 thẻ thống kê (đang chờ / gửi lần đầu / đã sửa gửi lại), hàng chờ bên trái, chi tiết + lịch sử bên phải, 2 nút **Duyệt** và **Yêu cầu sửa**. Hai nút này mở `<dialog>` (modal có sẵn của HTML).

### Bấm "Duyệt" → xác nhận (POST approve)

1. Dialog "Xác nhận duyệt" có form `POST /department-head/topics/{id}/approve`.
2. **Controller** `approve()` gọi `topicReviewService.headApprove(email, topicId)`.
3. **Service** [`headApprove()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L89), `@Transactional`:
   1. `requireRole` (như trên).
   2. [`lockForHead(topicId, head)`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L215):
      - `topics.findByIdForUpdate(id)`: đọc **và khóa dòng** Topic (`@Lock(PESSIMISTIC_WRITE)`). Ai đọc dòng này sau phải chờ transaction hiện tại xong.
      - Không cùng ban thì `TOPIC_REVIEW_FORBIDDEN`.
      - **BR-34:** đề tài do chính mình đề xuất thì `TOPIC_REVIEW_FORBIDDEN`.
      - Không còn ở trạng thái chờ mình duyệt thì `TOPIC_ALREADY_PROCESSED` (409).
   3. Tạo `TopicReview`: `review_stage = DEPARTMENT`, `decision = APPROVED`, `reviewer = head`, rồi `save` (INSERT).
   4. `topic.setReviewedByHead(head)` và **`topic.setStatus(SECRETARY_REVIEW)`**.
   5. Commit: Hibernate tự `UPDATE topic ...`.
4. Flash "Đã duyệt đề tài. Đề tài được chuyển lên Thư ký tòa soạn." và redirect về hàng chờ. Đề tài biến mất khỏi hàng chờ Trưởng ban vì status đã đổi.

### Bấm "Yêu cầu sửa" → nhập góp ý (POST request-changes)

1. Dialog có `<textarea name="feedback">` → `POST /department-head/topics/{id}/request-changes`.
2. **Service** [`headRequestChanges()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L108), `@Transactional`:
   1. `requireText(feedback, TOPIC_FEEDBACK_REQUIRED)`: góp ý **bắt buộc**, tối đa 4000 ký tự.
   2. `lockForHead` (kiểm tra như trên).
   3. Tạo `TopicFeedback`: `type = CHANGE_REQUEST`, `content = góp ý`, `author = head`, rồi INSERT.
   4. **Status giữ nguyên `DEPARTMENT_REVIEW`**, chỉ bật `topic.setAwaitingRevision(true)`.
   5. `notificationService.notify(phóng viên, topic, "Trưởng ban yêu cầu sửa đề tài ...")`: INSERT vào `notification`.
   6. Tất cả trong **một transaction**: lỗi ở bước nào thì không bước nào được lưu.
3. Kết quả: đề tài **rời hàng chờ Trưởng ban** (vì `awaiting_revision = 1`), phóng viên thấy chuông có số đỏ và nút **Sửa** trên danh sách.

### Khi lỗi

`backWithError()`:
- Lỗi 403 / 404 thì ném tiếp, ra trang lỗi.
- Lỗi khác (thiếu góp ý, đã xử lý rồi) thì flash `error` + `feedback` (nội dung đã gõ) và redirect `?topic=id`. JS thấy `data-reopen="true"` nên **mở lại dialog** với nội dung cũ, không phải gõ lại.

### Câu hỏi có thể gặp

- **"Hai Trưởng ban cùng bấm duyệt một lúc thì sao?"** `findByIdForUpdate` khóa dòng. Người thứ hai phải chờ người thứ nhất commit, sau đó đọc lại thấy status đã là `SECRETARY_REVIEW`, nên nhận lỗi "Đề tài đã được xử lý".
- **"Trưởng ban có từ chối được không?"** Ở code hiện tại thì **không**. Trưởng ban chỉ có Duyệt hoặc Yêu cầu sửa. Từ chối chỉ có ở cấp Thư ký. (Xem Phần 6.)
- **"Trưởng ban ban Kinh tế có xem được đề tài ban Xã hội không?"** Không: hàng chờ lọc theo ban, và gõ `?topic=` của ban khác thì nhận 403.

---

## 2.2. Phóng viên xem góp ý (Topic Feedback Viewer)

**URL:** `GET /reporter/topics/{id}/feedback`
**File:** `TopicProposalController.feedback`, `TopicServiceImpl.getFeedbackView`, `TopicMapper.toHistory`, `templates/reporter/topic-feedback.html`

1. Vào từ: bấm tên đề tài hoặc nút **Góp ý** trên danh sách, hoặc bấm một thông báo.
2. **Controller** nạp `list` (cho topbar) và `view = topicService.getFeedbackView(email, topicId)`.
3. **Service** [`getFeedbackView()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L107):
   1. `findWithPeopleById`: không có thì `TOPIC_NOT_FOUND` (404).
   2. **Chỉ chủ đề tài** (`creator_id == me`) được xem, nếu không thì `TOPIC_VIEW_FORBIDDEN` (403). Gõ id của người khác vào URL cũng bị chặn.
   3. `mapper.toHistory(feedbacks, reviews)`: gộp 2 nguồn thành một dòng thời gian, mỗi mục có nhãn và màu:
      - `topic_feedback` CHANGE_REQUEST → "Trưởng ban yêu cầu sửa" (màu `revision`)
      - `topic_review` APPROVED → "Trưởng ban đã duyệt" / "Thư ký đã duyệt" (màu `approved`)
      - `topic_review` REJECTED → "Thư ký từ chối" + lý do (màu `rejected`)
   4. `canEdit` = `DEPARTMENT_REVIEW && awaitingRevision`.
4. **Giao diện:** thông tin đề tài, badge trạng thái, dòng thời gian. Nút **Sửa đề tài** chỉ hiện khi `canEdit`.

---

## 2.3. Phóng viên sửa và gửi lại đề tài (Edit & Resubmit)

**URL:** `GET /reporter/topics/{id}/edit`, `POST /reporter/topics/{id}/resubmit`
**File:** `TopicProposalController` (`editForm`, `resubmit`), `TopicServiceImpl` (`getEditForm`, `resubmit`, `checkCanEdit`). Dùng lại template `topic-proposal-create.html`.

### Mở form sửa (GET)

1. `editForm()` gọi `topicService.getEditForm(email, id)` **trước tiên**, để kiểm tra quyền:
   - `requireActiveReporter`: phải là Reporter `ACTIVE`.
   - `checkCanEdit`: phải là **chủ đề tài** **và** đề tài đang `DEPARTMENT_REVIEW` + `awaiting_revision = 1`. Sai thì `TOPIC_EDIT_FORBIDDEN` (403).
   - Hợp lệ thì trả DTO đã điền sẵn tiêu đề và tóm tắt hiện tại.
2. `formPage(auth, model, topicId)`: lần này `editTopicId` khác `null`, nên:
   - Hiện khung **"Góp ý của Trưởng ban"** (góp ý yêu cầu sửa gần nhất, lấy từ `latestChangeRequest()`) phía trên form.
   - Tiêu đề trang là "Sửa & gửi lại", nút là "Gửi lại", form `action` trỏ tới `/resubmit`.

### Gửi lại (POST)

1. Vẫn đi qua modal xác nhận giống tạo mới.
2. **Controller** `resubmit()`: `@Valid` kiểm tra form. Có lỗi thì vẫn gọi `getEditForm` để kiểm tra quyền rồi mới hiện lại form.
3. **Service** [`resubmit()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L134), `@Transactional`:
   1. `requireActiveReporter` + `validate`.
   2. `findByIdForUpdate`: **khóa dòng** rồi `checkCanEdit` lại. Tránh trường hợp bấm gửi 2 lần ở 2 tab.
   3. Cập nhật `title`, `description`. **Ban giữ nguyên.**
   4. `topic.setAwaitingRevision(false)`: đề tài **quay lại hàng chờ Trưởng ban**. Status vẫn `DEPARTMENT_REVIEW`.
4. Flash "Đã gửi lại đề tài. Đề tài quay về hàng chờ Trưởng ban duyệt." và về danh sách.
5. Bên Trưởng ban: đề tài xuất hiện lại trong hàng chờ, có badge "Đã sửa 1 lần" (đếm số dòng `CHANGE_REQUEST`).

### Câu hỏi có thể gặp

- **"Sao không tạo trạng thái mới kiểu REVISION_REQUIRED cho Topic?"** Bảng `topic` trong DB chỉ có 4 trạng thái, và nhóm quy ước không đổi enum dùng chung. Vì vậy dùng thêm cột cờ `awaiting_revision`.
- **"Đề tài bị REJECTED có sửa / gửi lại được không?"** Không (OQ-08). `checkCanEdit` chỉ cho sửa khi `DEPARTMENT_REVIEW + awaiting_revision`. Phóng viên phải đề xuất đề tài mới.

---

## 2.4. Thư ký duyệt cuối (Final Topic Review)

**URL:** `GET /secretary/topic-reviews` (`?department=2&topic=15`), `POST /secretary/topic-reviews/{id}/approve`, `POST /secretary/topic-reviews/{id}/reject`
**File:** `controller/SecretaryTopicController.java`, `TopicReviewServiceImpl` (`getSecretaryWorkspace`, `secretaryApprove`, `secretaryReject`, `lockForSecretary`), `templates/secretary/topic-final-review.html`
**Sidebar:** mục "Duyệt đề tài cuối". URL là `/topic-reviews` vì `/secretary/topics` đã là màn Topic tòa soạn (FL-02) của bạn khác.

### Mở màn (GET)

1. **Security:** `/secretary/**` chỉ cho `EDITORIAL_SECRETARY`.
2. **Service** [`getSecretaryWorkspace()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L131):
   1. `requireRole(email, EDITORIAL_SECRETARY)`.
   2. **Hàng chờ** = `findByStatusOrderByCreatedAtAscIdAsc(SECRETARY_REVIEW)`: đề tài **mọi ban** đã qua Trưởng ban. Nếu có `?department=` thì lọc thêm theo ban.
   3. **NFR-03:** đề tài còn `DEPARTMENT_REVIEW` (chưa qua Trưởng ban) mà bị gõ thẳng `?topic=` thì `TOPIC_VIEW_FORBIDDEN`.
   4. `canDecide` = còn `SECRETARY_REVIEW`.
   5. Nạp danh sách ban cho ô chọn lọc.
3. **Giao diện:** ô `<select name="department" onchange="this.form.submit()">`. Đổi ban là form GET tự gửi, trang tải lại với hàng chờ đã lọc. Phần còn lại giống màn Trưởng ban, nút là **Duyệt** và **Từ chối**.

### Bấm "Duyệt" (POST approve)

[`secretaryApprove()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L154), `@Transactional`. Đây là thao tác **ghi nhiều bảng nhất**:

1. `requireRole` + [`lockForSecretary`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L226): khóa dòng; BR-34 (không duyệt đề tài của chính mình); phải còn `SECRETARY_REVIEW`, nếu không thì `TOPIC_ALREADY_PROCESSED`.
2. Xác định **Trưởng ban đã duyệt**: lấy `topic.reviewed_by_head_id`. Dữ liệu cũ chưa có cột này thì tìm trong `topic_review` cấp `DEPARTMENT` có `APPROVED`.
3. Kiểm tra Trưởng ban đó **và** phóng viên đề xuất vẫn `ACTIVE`, đúng role (rule R13, R23: tài khoản không ACTIVE thì không được giữ assignment). Sai thì `TOPIC_TEAM_UNAVAILABLE`.
4. INSERT `topic_review`: `review_stage = SECRETARY`, `decision = APPROVED`.
5. **Tự tạo 2 assignment** (CLAUDE.md mục 4.1-A):
   - phóng viên đề xuất → `assignment_role = LEAD`
   - Trưởng ban đã duyệt → `assignment_role = DEPARTMENT_HEAD`
   - `assigned_by` = Thư ký. Nhiệm vụ và deadline **để trống**, Trưởng ban bổ sung sau ở FL-03 (OQ-06).
6. **`topic.setStatus(ACTIVE)`**.
7. Thông báo cho phóng viên: "Đề tài ... đã được duyệt. Bạn là phóng viên viết chính (Lead)."
8. Tất cả trong **một transaction**: lỗi khi tạo assignment thì review và status cũng không được lưu.

### Bấm "Từ chối" (POST reject)

[`secretaryReject()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L192), `@Transactional`:

1. `requireText(reason, TOPIC_REJECT_REASON_REQUIRED)`: **lý do bắt buộc** (BR-12).
2. `lockForSecretary`.
3. INSERT `topic_review`: `SECRETARY`, `REJECTED`, `feedback = lý do`.
4. **`topic.setStatus(REJECTED)`**: trạng thái cuối, không gửi lại được.
5. Thông báo cho phóng viên: "... đã bị Thư ký tòa soạn từ chối. Bấm để xem lý do."

### Câu hỏi có thể gặp

- **"Sau khi Thư ký duyệt thì DB thay đổi những gì?"** 1 dòng mới trong `topic_review`, 2 dòng mới trong `assignment` (LEAD + DEPARTMENT_HEAD), 1 dòng mới trong `notification`, và `topic.status` thành `ACTIVE`.
- **"Vì sao cần `@Transactional` ở đây?"** Vì ghi 4 chỗ. Nếu tạo assignment lỗi mà status đã thành `ACTIVE` thì sẽ có đề tài ACTIVE không có Lead, sai nghiệp vụ.
- **"Thư ký có thấy đề tài chưa qua Trưởng ban không?"** Không: hàng chờ chỉ lấy `SECRETARY_REVIEW`, và gõ URL cũng bị chặn (NFR-03).

---

## 2.5. Chuông thông báo

**File:** `entity/Notification.java`, `repository/NotificationRepository.java`, `service/impl/NotificationServiceImpl.java`, fragment `tpTopbar` trong `topic-proposal-list.html`, `TopicProposalController.openNotification`

- **Tạo:** `notify(recipient, topic, message)` INSERT một dòng `notification` (`is_read = 0`). Được gọi khi Trưởng ban yêu cầu sửa, Thư ký duyệt, Thư ký từ chối.
- **Hiện:** `getProposalList` gọi `countUnread` (con số đỏ) và `getRecent` (20 thông báo mới nhất). JS bấm chuông thì mở / đóng khung.
- **Bấm một thông báo:** link `GET /reporter/notifications/{id}/open`:
  1. `notificationService.open(email, id)`: thông báo **phải là của mình**, nếu không thì `NOTIFICATION_NOT_FOUND` (giả như không tồn tại).
  2. `n.setRead(true)` (transaction tự UPDATE).
  3. Trả `topicId`, controller `redirect:/reporter/topics/{topicId}/feedback` (mục 2.2).
- **Lưu ý:** đây chỉ là thông báo **trong hệ thống**, không gửi email (CLAUDE.md OQ-12: chưa làm thông báo thật).

---

# ITER 3

Iter 3 làm lại toàn bộ phần **tài khoản** thành pop-up trên một trang `/login`: Đăng ký, OTP, Quên mật khẩu, Đặt lại mật khẩu, Google, trang 403.

## 3.1. Trang /login chứa nhiều pop-up

**File:** `templates/login.html`, `static/js/auth-popup.js`, `AuthController.loginPage` + `allowedPopup`

- `login.html` có 6 `<section data-lp-popup="...">`: `login`, `register`, `otp`, `awaiting`, `forgot`, `reset`. Mỗi section có `th:hidden="${popup != 'xxx'}"`, nên **server quyết định** pop-up nào hiện.
- `loginPage()` lấy `popup` theo thứ tự: flash attribute (sau khi submit form), rồi `?popup=` trên URL.
- [`allowedPopup()`](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L72) **chặn nhảy cóc**:
  - `otp` chỉ mở được khi session đã có `OTP_EMAIL` (đã bấm gửi mã).
  - `reset` chỉ mở được khi session đã có `RESET_EMAIL` (đã nhập đúng OTP quên mật khẩu).
  - Gõ `/login?popup=reset` khi chưa qua OTP thì chỉ hiện pop-up Login.
- `auth-popup.js`: chuyển Login ↔ Register ↔ Forgot **ngay trên trình duyệt** (không tải lại trang), hiện / ẩn mật khẩu, 6 ô OTP tự nhảy ô, đếm ngược 60 giây cho nút "Gửi lại mã". **Mọi kiểm tra thật vẫn ở server.**
- Route cũ `/register`, `/forgot-password`... chỉ redirect về `/login?popup=...`.

**Session lưu gì (và tại sao không để trong form):**

| Khóa session | Ý nghĩa |
|---|---|
| `OTP_EMAIL` | email đang chờ nhập OTP |
| `OTP_PURPOSE` | `VERIFY_EMAIL` hay `RESET_PASSWORD` |
| `OTP_SENT_AT` | lúc gửi mã (để đếm 60 giây) |
| `RESET_EMAIL` | chỉ có sau khi nhập đúng OTP quên mật khẩu |

Nếu để email trong ô ẩn của form, người dùng có thể sửa thành email người khác rồi đặt lại mật khẩu của họ. Session nằm ở server nên không sửa được.

---

## 3.2. Đăng ký + OTP

**URL:** `POST /register`, `POST /otp/verify`
**File:** `AuthController` (`register`, `verifyOtp`, `startOtp`), `AuthServiceImpl` (`validateRegistration`, `sendRegistrationCode`, `verifyAndRegister`, `useOtp`, `issueOtp`), `OtpEmailService`

### Bấm "Đăng ký" (POST /register)

1. **Controller** [`register()`](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L129):
   1. `authService.validateRegistration(...)` trả `Map<tên ô, câu lỗi>`:
      - Họ tên: không trống, tối đa 100 ký tự
      - Email: đúng định dạng, **chỉ nhận `@gmail.com`**, chưa có tài khoản (trừ tài khoản `PENDING`)
      - Mật khẩu: ít nhất 8 ký tự, có cả chữ và số; nhập lại phải khớp
   2. Có lỗi thì `backToRegister`: mở lại pop-up Đăng ký, giữ họ tên + email (**không giữ mật khẩu**), lỗi hiện dưới từng ô.
2. **Service** [`sendRegistrationCode()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L128):
   1. Email đã tồn tại:
      - `PENDING` (đăng ký rồi nhưng chưa nhập OTP): gửi mã mới nếu mã cũ hết hạn, rồi ném `AUTH_ACCOUNT_NOT_VERIFIED`. Controller bắt lỗi này và **mở pop-up OTP** thay vì báo lỗi.
      - Trạng thái khác: `AUTH_ACCOUNT_ALREADY_EXISTS`.
   2. Kiểm tra lại toàn bộ (không tin form).
   3. Tạo `Account`: `username` lấy từ phần trước @, `password_hash = BCrypt(mật khẩu)`, `role = REPORTER` (tạm, vì cột NOT NULL), **`status = PENDING`**.
   4. `issueOtp(account, VERIFY_EMAIL)` (xem bên dưới).
3. Controller `startOtp(session, email, VERIFY_EMAIL, true)`: lưu email, mục đích, thời điểm gửi vào session. Flash `popup = "otp"`, redirect `/login`. Pop-up OTP hiện ra.

### `issueOtp`: gửi mã như thế nào

[`issueOtp()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L346):
1. Tìm dòng `account_otp` **chưa dùng** gần nhất (cùng tài khoản, cùng mục đích). Có thì **ghi đè**, không có thì tạo mới. Ghi đè nên **mã cũ mất hiệu lực ngay**.
2. Sinh mã 6 số bằng `SecureRandom` (an toàn hơn `Random` thường).
3. **Chỉ lưu hash**: `code_hash = BCrypt(mã)`. Người đọc được DB cũng không biết mã.
4. `attempt_count = 0`, `last_sent_at = bây giờ`.
5. **Gửi email sau khi transaction commit** (`registerSynchronization(... afterCommit ...)`): nếu lưu DB lỗi và rollback thì không gửi mã "ma" cho người dùng.
6. `OtpEmailService.sendVerificationCode` có `@Async`: gửi mail ở **luồng khác**, người dùng không phải chờ Gmail. Chưa cấu hình SMTP thì mã được in ra console (tiện khi dev).

### Nhập mã OTP (POST /otp/verify)

1. 6 ô input cùng `name="digit"`, Spring gom thành `List<String>`. Controller ghép lại thành chuỗi 6 số.
2. Lấy `email` và `purpose` **từ session**. Không có (session hết hạn) thì về Login kèm lỗi.
3. `purpose = VERIFY_EMAIL` → [`verifyAndRegister()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L156):
   1. Tài khoản phải đang `PENDING`.
   2. [`useOtp()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L307), theo thứ tự:
      - sai đủ **5 lần** → `AUTH_OTP_TOO_MANY_ATTEMPTS` (mã bị vô hiệu, phải xin mã mới)
      - quá **5 phút** từ `last_sent_at` → `AUTH_OTP_EXPIRED`
      - sai mã → `attempt_count + 1` rồi báo lỗi
      - đúng → `used_at = bây giờ` (**dùng một lần**)
   3. **`status = AWAITING_APPROVAL`**: email đã xác minh nhưng **vẫn chưa vào được hệ thống**, phải chờ Admin duyệt.
   4. `@Transactional(noRollbackFor = ApplicationException.class)`: khi nhập sai thì service ném lỗi, nhưng **vẫn lưu** `attempt_count` tăng lên. Nếu rollback thì người ta thử sai vô hạn được.
4. Thành công: `clearOtp(session)`, mở pop-up **"Đang chờ duyệt"**.
5. Sai: mở lại pop-up OTP, lỗi hiện dưới 6 ô.

### Sau đó: Admin duyệt (màn Admin của bạn khác, service là của mình)

`AccountApprovalServiceImpl.approve(accountId, role, departmentId, adminEmail)`: chỉ cấp 3 role `REPORTER` / `DEPARTMENT_HEAD` / `EDITORIAL_SECRETARY` (không cấp `ADMIN`, tránh tự nâng quyền). Reporter và Trưởng ban **bắt buộc chọn ban**. Duyệt thì `status = ACTIVE` và gửi email báo. Từ chối thì `status = INACTIVE` (không xóa) và gửi email kèm lý do.

**Vòng đời tài khoản:** `PENDING` → (đúng OTP) → `AWAITING_APPROVAL` → (Admin duyệt) → `ACTIVE`, hoặc (Admin từ chối) → `INACTIVE`.

---

## 3.3. Gửi lại mã OTP

**URL:** `POST /otp/resend` · **Method:** `AuthController.resendOtp`, `AuthServiceImpl.resendCode`

1. Giao diện khóa nút "Gửi lại mã" và đếm ngược `resendWait` giây. Số giây này server tính từ `OTP_SENT_AT` trong session.
2. **Backend kiểm tra lại 2 lớp**:
   - controller: `resendWaitSeconds(session) > 0` thì báo "Vui lòng chờ 60 giây"
   - service: tính từ `last_sent_at` trong DB
3. Đủ 60 giây thì `issueOtp` (ghi đè mã cũ), cập nhật `OTP_SENT_AT`, báo "Mã mới đã được gửi. Mã cũ không còn hiệu lực."
4. Với mục đích `RESET_PASSWORD`: email không tồn tại thì **im lặng** (không báo lỗi), để không lộ email nào có tài khoản.

---

## 3.4. Quên mật khẩu + Đặt lại mật khẩu

**URL:** `POST /forgot-password` → `POST /otp/verify` → `POST /reset-password`
**Method:** `AuthController` (`forgotPassword`, `verifyOtp`, `resetPassword`), `AuthServiceImpl` (`sendPasswordResetCode`, `verifyResetCode`, `resetPassword`)

### Bước 1: nhập email (POST /forgot-password)

1. Service [`sendPasswordResetCode()`](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L201):
   - Email **sai định dạng** thì báo lỗi (lỗi duy nhất được báo).
   - Email **không tồn tại**, hoặc tài khoản không phải `ACTIVE` / `AWAITING_APPROVAL`: **không gửi gì, không báo gì**.
   - Bấm liên tục trong 60 giây: giữ mã cũ.
   - Còn lại: `issueOtp(..., RESET_PASSWORD)`.
2. Controller **luôn** hiện cùng một câu: *"Nếu email đã đăng ký, mã xác nhận đã được gửi."* và luôn mở pop-up OTP.
   → **Tại sao?** Nếu báo "email không tồn tại", kẻ xấu có thể dò ra email nào có tài khoản trong hệ thống (FL-AUTH-02).

### Bước 2: nhập OTP (POST /otp/verify, purpose = RESET_PASSWORD)

1. `verifyResetCode()`: email không hợp lệ thì báo lỗi chung "Mã không đúng". Còn lại gọi `useOtp` (5 lần, 5 phút, dùng một lần).
2. Đúng mã: `clearOtp`, rồi **`session.setAttribute(RESET_EMAIL, email)`**, mở pop-up "Đặt lại mật khẩu".

### Bước 3: đặt mật khẩu mới (POST /reset-password)

1. Lấy email **từ session `RESET_EMAIL`**, không có thì `AUTH_RESET_NOT_ALLOWED`. Gửi thẳng POST mà chưa qua OTP cũng bị chặn.
2. `validateNewPassword`: ít nhất 8 ký tự, có chữ và số, nhập lại khớp.
3. `resetPassword()`: `password_hash = BCrypt(mới)`. **Không đổi status, không đổi role.**
4. Xóa `RESET_EMAIL` khỏi session (chỉ đặt lại được một lần), báo "Đặt lại mật khẩu thành công", điền sẵn email ở pop-up Login.

---

## 3.5. Đăng nhập / đăng ký bằng Google

**File:** `config/GoogleLoginSuccessHandler.java`, `SecurityConfig` (`oauth2Login`, `googleRoleMapper`), `AuthServiceImpl` (`registerWithGoogle`, `confirmEmailByGoogle`)

**OAuth2 là gì (ví dụ):** thay vì nhập mật khẩu của hệ thống, bạn bấm "Đăng nhập bằng Google", sang trang Google chọn tài khoản. Google trả về cho hệ thống: "người này là `abc@gmail.com`, email đã xác minh". Hệ thống không bao giờ thấy mật khẩu Google.

### Các bước

1. Nút Google là link `/oauth2/authorization/google`. Spring Security tự chuyển sang trang Google (cấu hình bằng `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`).
2. Google trả về `/login/oauth2/code/google`. Spring đổi mã lấy thông tin người dùng.
3. `googleRoleMapper` (trong SecurityConfig): Google chỉ cho quyền kiểu `OIDC_USER`. Mapper tìm email trong DB và **gắn thêm `ROLE_<role trong DB>`**, để `hasRole()` và sidebar chạy giống đăng nhập mật khẩu.
4. [`GoogleLoginSuccessHandler.onAuthenticationSuccess()`](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L40):

   | Tình huống | Xử lý |
   |---|---|
   | Không có email / `email_verified` khác true | từ chối, `/login?error=AUTH_ACCOUNT_NOT_REGISTERED` |
   | Email **chưa có** tài khoản | `registerWithGoogle`: tạo tài khoản `AWAITING_APPROVAL` (bỏ qua OTP vì Google đã xác minh email; mật khẩu là băm của chuỗi ngẫu nhiên), hủy phiên, `/login?registered` |
   | `AWAITING_APPROVAL` | hủy phiên, `/login?awaiting` |
   | `LOCKED` / `INACTIVE` | hủy phiên, báo lỗi |
   | `PENDING` | `confirmEmailByGoogle`: chuyển thành `AWAITING_APPROVAL`, hủy phiên, `/login?awaiting` |
   | `ACTIVE` | `ensureRoleAuthority` (gắn role nếu thiếu), rồi `redirect /dashboard` → màn chính theo role (như 1.1) |

5. Lỗi ở phía Google (bấm hủy...) thì `failureHandler` ghi log và chuyển `/login?error=AUTH_GOOGLE_LOGIN_FAILED`.
6. "Hủy phiên" (`endSession`): xóa `SecurityContext` và `session.invalidate()`, để người chưa được duyệt **không giữ** phiên đăng nhập.

**Câu hỏi có thể gặp:** *"Tài khoản tạo bằng Google muốn đăng nhập bằng mật khẩu thì sao?"* Dùng Quên mật khẩu để đặt mật khẩu mới.

---

## 3.6. Trang 403

**File:** `templates/error/403.html`

Spring Boot tự tìm template theo mã lỗi trong `templates/error/`. Khi có `sendError(403)` (từ SecurityConfig hoặc `@ExceptionHandler`), trang "403 – Không có quyền" hiện ra thay cho trang lỗi trắng mặc định.

Riêng lỗi **CSRF hết hạn** (để trang mở quá lâu, hoặc restart app làm mất session): `accessDeniedHandler` trong SecurityConfig **không** ra 403 mà quay về trang trước, để người dùng chỉ cần bấm lại.

---

## Phần 4. Sơ đồ trạng thái tổng hợp

### Topic (FL-01)

```
Phóng viên gửi
      │
      ▼
DEPARTMENT_REVIEW (awaiting_revision = 0)  ◄────────────────┐
      │                                                      │ Phóng viên sửa & gửi lại
      ├── Trưởng ban "Yêu cầu sửa" ──► DEPARTMENT_REVIEW (awaiting_revision = 1)
      │        (+ topic_feedback, + notification)
      │
      └── Trưởng ban "Duyệt" (+ topic_review DEPARTMENT APPROVED)
                 │
                 ▼
          SECRETARY_REVIEW
                 │
                 ├── Thư ký "Duyệt" ──► ACTIVE
                 │      (+ topic_review SECRETARY APPROVED, + 2 assignment LEAD/DEPARTMENT_HEAD, + notification)
                 │
                 └── Thư ký "Từ chối" (lý do bắt buộc) ──► REJECTED (kết thúc)
                        (+ topic_review SECRETARY REJECTED, + notification)
```

### Account

```
Đăng ký form ──► PENDING ──(đúng OTP)──► AWAITING_APPROVAL ──(Admin duyệt)──► ACTIVE
Đăng ký Google ─────────────────────────► AWAITING_APPROVAL ──(Admin từ chối)──► INACTIVE
                                                              Admin khóa ──► LOCKED
```

### Bảng "ai thấy gì"

| Màn | Ai vào được | Thấy dữ liệu nào |
|---|---|---|
| `/reporter/topics` | REPORTER | đề tài `creator_id` = mình |
| `/reporter/topics/{id}/feedback` | REPORTER | chỉ đề tài của mình |
| `/reporter/topics/{id}/edit` | REPORTER | của mình **và** đang được yêu cầu sửa |
| `/department-head/topics` | DEPARTMENT_HEAD | `DEPARTMENT_REVIEW`, đúng ban, không chờ sửa |
| `/secretary/topic-reviews` | EDITORIAL_SECRETARY | `SECRETARY_REVIEW` của mọi ban |

---

## Phần 5. Câu hỏi khó và cách trả lời

**Q: Nếu Phóng viên tự gửi POST tới `/department-head/topics/5/approve` thì sao?**
A: Lớp 1 (SecurityConfig) chặn, vì `/department-head/**` cần `ROLE_DEPARTMENT_HEAD`, nên trả 403. Giả sử lớp 1 bị cấu hình sai thì lớp 2 `requireRole(..., DEPARTMENT_HEAD)` trong service vẫn ném `TOPIC_REVIEW_FORBIDDEN`.

**Q: Ẩn nút trên giao diện có đủ chưa?**
A: Chưa. `th:if` chỉ không vẽ nút, người dùng vẫn gõ URL hoặc gửi form được. Vì vậy mọi kiểm tra quyền đều làm lại ở service (CLAUDE.md mục 0).

**Q: CSRF là gì, dự án chống thế nào?**
A: Ví dụ: bạn đang đăng nhập, rồi mở một trang web xấu. Trang đó có form ẩn tự gửi `POST /logout` hoặc `POST .../approve` tới hệ thống, và trình duyệt tự kèm cookie của bạn. Chống: mọi form POST đều có ô ẩn `_csrf` do Thymeleaf tự chèn, và Spring Security từ chối POST không có token đúng. Trang web xấu không biết token này.

**Q: Hibernate lưu thay đổi status thế nào khi không thấy gọi `save()`?**
A: Entity lấy ra trong `@Transactional` được Hibernate theo dõi. Lúc commit, nó so với lúc đọc, thấy `status` khác nên tự sinh `UPDATE` (dirty checking).

**Q: `@Transactional(readOnly = true)` để làm gì?**
A: Báo cho Spring / Hibernate biết method chỉ đọc: không cần dirty checking, nhẹ hơn, và tránh vô tình ghi dữ liệu.

**Q: Khóa bi quan (`PESSIMISTIC_WRITE`) dùng ở đâu, để làm gì?**
A: `TopicRepository.findByIdForUpdate`, dùng khi Trưởng ban / Thư ký duyệt và khi phóng viên gửi lại. SQL Server khóa dòng đó tới khi transaction xong. Hai người bấm cùng lúc thì người sau chờ, đọc lại thấy trạng thái đã đổi, nhận "Đề tài đã được xử lý". Không bị duyệt hai lần, không tạo assignment trùng.

**Q: Tại sao OTP lưu hash chứ không lưu mã?**
A: Nếu DB bị lộ, người khác cũng không đọc được mã để xác minh thay. Khi kiểm tra thì dùng `encoder.matches(mã nhập, hash)`.

**Q: Vì sao sai OTP mà vẫn phải lưu `attempt_count`, trong khi service ném lỗi?**
A: Mặc định ném lỗi thì transaction rollback, `attempt_count` sẽ không tăng và người ta thử sai vô hạn được. Vì vậy dùng `noRollbackFor = ApplicationException.class`.

**Q: `@ControllerAdvice` là gì, dự án dùng ở đâu?**
A: Là class mà Spring chạy kèm **mọi** controller. `SidebarAdvice` dùng `@ModelAttribute` để mọi trang đều có menu sidebar đúng role, mà không controller nào phải tự thêm.

**Q: Redirect với forward / trả view khác gì? Sao sau POST lại redirect?**
A: Trả view: server render HTML ngay, URL trên trình duyệt vẫn là URL của POST, bấm F5 sẽ gửi lại form (có thể tạo đề tài trùng). Redirect: server bảo trình duyệt "hãy GET địa chỉ này", F5 chỉ GET lại danh sách. Riêng trường hợp form có lỗi validate thì trả view luôn để giữ lỗi và dữ liệu đã nhập.

**Q: Mã đề tài `TP-2026-0012` có lưu trong DB không?**
A: Không. `TopicMapper.codeOf()` ghép từ năm tạo + `topic_id` mỗi khi hiển thị.

---

## Phần 6. Những chỗ code khác tài liệu (nên biết trước)

Thầy có thể so code với CLAUDE.md / PRD. Đây là các chỗ đang khác. Nên biết và trả lời được là "nhóm đã thống nhất như vậy", hoặc ghi nhận là việc cần sửa.

| Chỗ | CLAUDE.md / PRD ghi | Code hiện tại |
|---|---|---|
| Trưởng ban từ chối đề tài | Bị từ chối ở bất kỳ cấp nào → `REJECTED` | Trưởng ban **chỉ** có Duyệt / Yêu cầu sửa; chỉ Thư ký từ chối được |
| Vòng sửa Topic | "Không có vòng yêu cầu sửa Topic" (OQ-08) | Có vòng sửa ở cấp Trưởng ban (cờ `awaiting_revision` + bảng `topic_feedback`), theo yêu cầu Iter 2 |
| Thời hạn OTP | 10 phút | Code kiểm tra **5 phút** (`OTP_VALIDITY`). Cột `expires_at` vẫn ghi +10 phút chỉ để thỏa CHECK của DB |
| Sau khi xác minh OTP | `PENDING` → `ACTIVE` | `PENDING` → `AWAITING_APPROVAL` → Admin duyệt → `ACTIVE` |
| Email đăng ký | không giới hạn | chỉ nhận `@gmail.com` |
| Bảng `notification` | Ngoài phạm vi (OQ-12) | Có chuông thông báo trong hệ thống (không gửi email), theo yêu cầu Iter 2 |
| `/process-login` trong SecurityConfig | — | Cấu hình còn sót, không form nào dùng |
