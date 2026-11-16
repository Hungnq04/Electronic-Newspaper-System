# Ôn hội đồng – Phần TV1 (Tài khoản + FL-01 Đề xuất & duyệt đề tài)

Tài liệu này xếp theo **đúng thứ tự các dòng TV1 trong Project Checking** (dòng 3 → 21, cộng 2 dòng nên thêm).
Mỗi mục trả lời 5 câu: **Bấm gì? → Code chạy qua đâu? → Kiểm tra (validate) ở tầng nào? → DB đổi gì? → Thầy hỏi gì, trả lời sao?**

- Bản giải thích chi tiết từng bước nằm ở `docs/giai-thich-fl01-3-iter.md`. File này là bản **ôn để trả lời**.
- Số dòng code có thể lệch khi sửa file. Khi đó tìm theo **tên method**.
- `P/` viết tắt cho `src/main/java/com/group6/electronic_drafting_system/`.

---

## Mục lục

- [A. Giới thiệu phần mình trong 1 phút](#a-giới-thiệu-phần-mình-trong-1-phút)
- [B. Bản đồ: dòng Project Checking → code](#b-bản-đồ-dòng-project-checking--code)
- [C. Ba ý nền dùng cho mọi câu hỏi](#c-ba-ý-nền-dùng-cho-mọi-câu-hỏi)
- [D. Đi từng dòng Project Checking](#d-đi-từng-dòng-project-checking)
  - [D1. Dòng 3–4: Login + khung thông báo](#d1-dòng-34-login--khung-thông-báo)
  - [D2. Dòng 5: Logout](#d2-dòng-5-logout)
  - [D3. Dòng 6–7 (+ pop-up chờ duyệt): Đăng ký + OTP](#d3-dòng-67--pop-up-chờ-duyệt-đăng-ký--otp)
  - [D4. Dòng 8–9: Quên mật khẩu + đặt lại](#d4-dòng-89-quên-mật-khẩu--đặt-lại)
  - [D5. Dòng nên thêm: Đăng nhập Google](#d5-dòng-nên-thêm-đăng-nhập-google)
  - [D6. Dòng 10: Danh sách đề tài](#d6-dòng-10-danh-sách-đề-tài)
  - [D7. Dòng 11–12: Đề xuất đề tài + modal xác nhận](#d7-dòng-1112-đề-xuất-đề-tài--modal-xác-nhận)
  - [D8. Dòng 14, 16, 17: Trưởng ban duyệt](#d8-dòng-14-16-17-trưởng-ban-duyệt)
  - [D9. Dòng 13, 15: Phóng viên xem góp ý, sửa và gửi lại](#d9-dòng-13-15-phóng-viên-xem-góp-ý-sửa-và-gửi-lại)
  - [D10. Dòng 18, 19, 21: Thư ký duyệt cuối](#d10-dòng-18-19-21-thư-ký-duyệt-cuối)
  - [D11. Dòng 20: Chuông thông báo](#d11-dòng-20-chuông-thông-báo)
- [E. Tính năng khó / ấn tượng: nên chủ động khoe](#e-tính-năng-khó--ấn-tượng-nên-chủ-động-khoe)
- [F. Kịch bản demo + các phép "thử phá"](#f-kịch-bản-demo--các-phép-thử-phá)
- [G. Điểm yếu đã biết: sửa trước hoặc trả lời thật](#g-điểm-yếu-đã-biết-sửa-trước-hoặc-trả-lời-thật)
- [H. Ngân hàng câu hỏi nhanh](#h-ngân-hàng-câu-hỏi-nhanh)
- [I. Chỗ code khác CLAUDE.md / PRD](#i-chỗ-code-khác-claudemd--prd)
- [J. Thuật ngữ (giải thích bằng ví dụ)](#j-thuật-ngữ-giải-thích-bằng-ví-dụ)

---

## A. Giới thiệu phần mình trong 1 phút

> "Em phụ trách hai mảng.
> **Một là tài khoản:** đăng nhập, đăng xuất, đăng ký có xác minh email bằng mã OTP, quên mật khẩu, và đăng nhập bằng Google. Đăng ký xong tài khoản chưa vào được ngay mà phải chờ Admin duyệt và cấp vai trò, vì đây là hệ thống nội bộ của tòa soạn.
> **Hai là FL-01, đề xuất và duyệt đề tài hai cấp:** Phóng viên đề xuất đề tài cho ban của mình. Trưởng ban duyệt, hoặc yêu cầu sửa, được nhiều vòng. Sau đó Thư ký tòa soạn duyệt cuối hoặc từ chối kèm lý do. Khi Thư ký duyệt, hệ thống tự giao Phóng viên làm Lead và Trưởng ban làm người phụ trách đề tài. Phóng viên có chuông thông báo khi có kết quả.
> Ba điểm em đầu tư nhiều nhất: **bảo mật OTP**, **phân quyền hai lớp** (chặn cả việc gõ URL), và **chống duyệt trùng khi hai người bấm cùng lúc**."

Số liệu nên nhớ:

| | |
|---|---|
| Màn chính | 5: Danh sách đề tài, Form đề xuất, Xem góp ý, Trưởng ban duyệt, Thư ký duyệt cuối |
| Màn phụ | khoảng 13: 6 pop-up trên trang `/login`, nút Google, modal xác nhận, các dialog duyệt / yêu cầu sửa / từ chối, chuông |
| Bảng DB thêm cho FL-01 | `topic_feedback`, `notification`, 3 cột mới trên `topic` (`department_id`, `awaiting_revision`, `reviewed_by_head_id`) |
| Test tự động | 13 test cho phần tài khoản (`AuthPopupFlowTests`, `AccountApprovalTests`, `GoogleLoginTests`) |

---

## B. Bản đồ: dòng Project Checking → code

| Dòng | Màn / nút | URL | Controller | Service | Template |
|---|---|---|---|---|---|
| 3, 4 | Pop-up Login, khung báo lỗi | `GET/POST /login` | [AuthController.login](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L96) | [AuthServiceImpl.login](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L70) | `login.html` (section `login`) |
| 5 | Đăng xuất | `POST /logout` | Spring Security tự xử lý | – | `fragment/sidebar.html` |
| 6 | Pop-up Đăng ký | `POST /register` | [register](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L128) | `validateRegistration`, `sendRegistrationCode` | section `register` |
| 7 | Pop-up OTP, gửi lại mã | `POST /otp/verify`, `/otp/resend` | [verifyOtp](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L169), [resendOtp](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L205) | `verifyAndRegister`, `resendCode`, `useOtp`, `issueOtp` | section `otp` |
| *(thêm)* | Pop-up chờ duyệt | – | – | – | section `awaiting` |
| 8 | Pop-up Quên mật khẩu | `POST /forgot-password` | [forgotPassword](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L231) | `sendPasswordResetCode` | section `forgot` |
| 9 | Pop-up Đặt lại mật khẩu | `POST /reset-password` | [resetPassword](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L252) | `verifyResetCode`, `resetPassword` | section `reset` |
| *(thêm)* | Nút Google | `/oauth2/authorization/google` | [GoogleLoginSuccessHandler](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L40) | `registerWithGoogle`, `confirmEmailByGoogle` | nút `lp-google` |
| 10 | Danh sách đề tài | `GET /reporter/topics?status=` | [TopicProposalController.list](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L46) | [TopicServiceImpl.getProposalList](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L58) | `reporter/topic-proposal-list.html` |
| 11, 12 | Form đề xuất + modal xác nhận | `GET /reporter/topics/new`, `POST /reporter/topics` | [create](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L60) | [createProposal](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L88) | `reporter/topic-proposal-create.html` |
| 13 | Xem góp ý | `GET /reporter/topics/{id}/feedback` | [feedback](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L75) | [getFeedbackView](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L107) | `reporter/topic-feedback.html` |
| 15 | Sửa và gửi lại | `GET .../{id}/edit`, `POST .../{id}/resubmit` | [editForm](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L83), [resubmit](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L91) | `getEditForm`, [resubmit](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L134) | form tạo, chế độ sửa |
| 14, 16, 17 | Trưởng ban duyệt / yêu cầu sửa | `GET /department-head/topics?topic=`, `POST .../{id}/approve`, `.../request-changes` | [DepartmentHeadTopicController](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L35) | [headApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L89), [headRequestChanges](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L108) | `department_head/topic-review.html` |
| 18, 19, 21 | Thư ký duyệt / từ chối | `GET /secretary/topic-reviews?department=&topic=`, `POST .../{id}/approve`, `.../reject` | [SecretaryTopicController](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L36) | [secretaryApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L154), [secretaryReject](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L192) | `secretary/topic-final-review.html` |
| 20 | Chuông thông báo | `GET /reporter/notifications/{id}/open` | [openNotification](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L110) | [NotificationServiceImpl](../src/main/java/com/group6/electronic_drafting_system/service/impl/NotificationServiceImpl.java#L32) | topbar trong `topic-proposal-list.html` |

---

## C. Ba ý nền dùng cho mọi câu hỏi

### C1. Một request đi qua đâu (mô hình MVC)

```
Trình duyệt ─► Spring Security ─► Controller ─► Service (@Transactional) ─► Repository (JPA) ─► SQL Server
                (đăng nhập chưa?    (nhận form,    (kiểm tra quyền + trạng thái,
                 URL cần role gì?    gọi service)    đổi dữ liệu)
                 có CSRF token?)          │
                                          ▼
                         Controller bỏ DTO vào Model ─► Thymeleaf ghép layout/app.html ─► HTML
```

- **Controller** chỉ nhận dữ liệu, gọi service, chọn màn trả về. **Không** chứa nghiệp vụ.
- **Service** chứa nghiệp vụ và kiểm tra quyền. Mỗi method ghi dữ liệu là một **transaction**.
- **Repository** là interface Spring Data JPA. Spring tự sinh câu SQL từ tên method.

### C2. Validate 4 tầng (câu hay bị hỏi nhất)

| Tầng | Ở đâu | Ví dụ trong phần em | Vì sao cần |
|---|---|---|---|
| 1. Trình duyệt | HTML `required`, `maxlength`, `type="email"`, JS | ô trống thì JS báo đỏ và không mở modal; ô OTP chỉ nhận số | báo lỗi nhanh, không phải gửi lên server. **Không tin được**: tắt JS hoặc dùng Postman là vượt qua |
| 2. Controller | `@Valid` + Bean Validation (`@NotBlank`, `@Size`) hoặc hàm trả `Map<ô, lỗi>` | form đề tài; form đăng ký | trả lỗi về **đúng ô**, giữ lại dữ liệu đã nhập |
| 3. Service | `requireActiveReporter`, `checkCanEdit`, `lockForHead`, `useOtp`… | đúng người? đúng trạng thái? đúng ban? | **tầng quyết định**. Kiểm tra lại cả những gì tầng 2 đã kiểm, vì service có thể được gọi từ chỗ khác |
| 4. Database | CHECK, UNIQUE, FK | `ck_topic_awaiting_revision`, `uq_topic_review_stage`, `uq_assignment_lead`, `account_otp_attempt_count_check` | lưới an toàn cuối cùng: code có bug thì DB vẫn từ chối dữ liệu sai |

> Câu chốt: **"Tầng 1 để người dùng tiện, tầng 3 để đảm bảo đúng nghiệp vụ, tầng 4 để dữ liệu không bao giờ sai."**

### C3. Phân quyền 2 lớp và cách trả lỗi

| Lớp | Ở đâu | Chặn theo |
|---|---|---|
| 1 | [SecurityConfig](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L48) | **URL**: `/reporter/**` chỉ REPORTER, `/department-head/**` chỉ DEPARTMENT_HEAD, `/secretary/**` chỉ EDITORIAL_SECRETARY, `/admin/**` chỉ ADMIN |
| 2 | Service | **Dữ liệu**: đề tài này có phải của mình không, có đúng ban không, trạng thái còn cho làm không |

- Lớp 1 chỉ biết "bạn là Phóng viên". Nó không biết đề tài số 15 là của ai. Nếu không có lớp 2, Phóng viên B gõ `/reporter/topics/15/feedback` là đọc được đề tài của A. Lỗi này gọi là **IDOR** (đổi id trên URL để xem đồ của người khác).
- **Trả lỗi:** service ném `ApplicationException(ErrorCode.X)`. Mỗi `ErrorCode` có câu tiếng Việt và mã HTTP.
  - Lỗi quyền (403) hoặc không tìm thấy (404): `@ExceptionHandler` gọi `response.sendError(...)`, ra trang `templates/error/403.html`.
  - Lỗi nghiệp vụ nhẹ (400, 409, ví dụ thiếu góp ý hoặc đề tài đã được người khác duyệt): controller bắt lại, gửi `error` qua **flash attribute**, rồi redirect về màn cũ. Nội dung đã nhập vẫn được giữ.
- **PRG (Post → Redirect → Get):** sau mỗi POST thành công, controller `return "redirect:/..."`. Bấm F5 chỉ tải lại trang danh sách, không gửi form lần nữa.

---

## D. Đi từng dòng Project Checking

### D1. Dòng 3–4: Login + khung thông báo

#### Bấm "Đăng nhập": hệ thống làm gì

1. `GET /login`: [loginPage](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L43) quyết định pop-up nào hiện (biến `popup`) rồi trả template `login`.
2. Người dùng điền email, mật khẩu, bấm nút. JS khóa nút và đổi chữ thành "Đang xử lý…" để tránh bấm 2 lần (`static/js/auth-popup.js` dòng 45–48). Form gửi `POST /login`, kèm ô ẩn `_csrf` do Thymeleaf tự chèn.
3. `AuthController.login` gọi `authService.login(email, password)`.
4. [AuthServiceImpl.login](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L70) kiểm tra **theo thứ tự**:
   1. Thiếu email / mật khẩu → `AUTH_INVALID_CREDENTIALS`
   2. Không tìm thấy email (không phân biệt hoa thường) → `AUTH_INVALID_CREDENTIALS`
   3. `encoder.matches(password, passwordHash)` sai → **cùng lỗi trên** ("Email hoặc mật khẩu không đúng.")
   4. Mật khẩu đúng rồi mới xét `status`:

      | Status | Kết quả |
      |---|---|
      | `LOCKED` | khung đỏ "Tài khoản đã bị khóa" |
      | `INACTIVE` | khung đỏ "Tài khoản đã ngừng hoạt động" |
      | `AWAITING_APPROVAL` | khung **xanh** "Đang chờ quản trị viên duyệt" (thông báo, không phải lỗi) |
      | `PENDING` (chưa nhập OTP) | không cho vào; gửi mã mới **nếu mã cũ đã hết hạn**; mở pop-up OTP |
      | `ACTIVE` | đi tiếp |

   5. `authenticationManager.authenticate(...)`: Spring Security tự kiểm tra **lần 2**. Nó gọi [UserDetailsService](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L37) để nạp tài khoản, so lại mật khẩu, kiểm cờ `locked` / `disabled`, rồi gắn quyền `ROLE_<role>`.
   6. Đặt kết quả vào `SecurityContextHolder`.
5. `persistSecurityContext`: lưu thông tin đăng nhập vào **HTTP session**. Trình duyệt giữ cookie `JSESSIONID`, các request sau Spring nhận ra bạn là ai.
6. `redirect:/dashboard`. [DashboardController](../src/main/java/com/group6/electronic_drafting_system/controller/DashboardController.java#L20) tra bảng role → màn chính:

   | Role | Màn chính |
   |---|---|
   | REPORTER | `/reporter/topics` |
   | DEPARTMENT_HEAD | `/department-head/dashboard` |
   | EDITORIAL_SECRETARY | `/secretary/final-reviews` |
   | ADMIN | `/admin/accounts` |

#### Dòng 4: khung thông báo hoạt động thế nào

- Lỗi → controller đặt flash `error` (khung đỏ). Chờ duyệt → flash `message` (khung xanh). Cả hai đều giữ lại `email` trong ô, **không** giữ mật khẩu.
- Lỗi từ Google hoặc Spring gửi qua URL (`/login?error=CODE`) cũng được đổi thành câu tiếng Việt bằng `ErrorCode.fromName`.

#### Validate

| Kiểm tra | Tầng | Kết quả |
|---|---|---|
| Ô trống, sai định dạng email | Trình duyệt (`required`, `type="email"`) | trình duyệt chặn |
| Sai email / sai mật khẩu | Service | câu lỗi chung, không nói rõ sai ô nào |
| Trạng thái tài khoản | Service + `UserDetailsService` | như bảng trên |

#### Thầy hỏi

- **"Sao sai email và sai mật khẩu báo giống nhau?"** → Nếu báo "email không tồn tại", người lạ có thể thử từng email để biết ai có tài khoản (gọi là **dò tài khoản – enumeration**).
- **"Sao kiểm tra mật khẩu trước rồi mới xét trạng thái?"** → Người không biết mật khẩu thì không biết được tài khoản đó đang bị khóa hay đang chờ duyệt.
- **"Mật khẩu lưu thế nào?"** → **BCrypt**: băm một chiều, không giải ngược được. Mỗi lần băm có một **salt** ngẫu nhiên, nên hai người cùng mật khẩu vẫn có hai chuỗi băm khác nhau. Khi đăng nhập dùng `encoder.matches(mật khẩu nhập, chuỗi băm)`.
- **"Sao không dùng trang login mặc định của Spring (`formLogin`)?"** → Cần xử lý riêng `PENDING` (mở pop-up OTP) và `AWAITING_APPROVAL` (thông báo xanh). Trang mặc định chỉ báo được "sai thông tin". Trong `SecurityConfig` vẫn giữ `formLogin(...loginPage("/login"))` để Spring biết chưa đăng nhập thì đẩy về đâu.
- **"Đăng nhập xong sao biết vào màn nào?"** → `DashboardController` đọc quyền `ROLE_...` và redirect theo bảng.
- **"Dùng session hay JWT? Vì sao?"** → Session + cookie. Giao diện render ở server bằng Thymeleaf nên session là cách tự nhiên nhất. JWT hợp với REST API + SPA (React / mobile).
- **"Mật khẩu kiểm tra ở mấy chỗ?"** → Hai: `encoder.matches` trong service, rồi `authenticationManager.authenticate`.
- **"Đăng nhập xong mã phiên có đổi không?"** → Có. `persistSecurityContext` gọi `request.changeSessionId()` trước khi lưu thông tin đăng nhập, để chống session fixation (xem [G2](#g2-đã-sửa-không-đổi-mã-phiên-sau-khi-đăng-nhập-bằng-mật-khẩu-session-fixation)). Dữ liệu trong session vẫn giữ nguyên, chỉ đổi mã.

---

### D2. Dòng 5: Logout

1. Nút "Đăng xuất" ở sidebar là một form `POST /logout`. Trước khi gửi có hộp `confirm('Bạn có chắc muốn đăng xuất?')` (`fragment/sidebar.html` dòng 73–74).
2. Không có controller nào viết cho `/logout`. `LogoutFilter` của Spring Security tự xóa session và `SecurityContext`.
3. `logoutSuccessUrl("/login?logout")` → pop-up Login hiện "Bạn đã đăng xuất."

#### Thầy hỏi

- **"Sao logout phải dùng POST mà không dùng link GET?"** → Chống **CSRF**. Nếu logout là GET, một trang web xấu chỉ cần chèn `<img src="https://.../logout">` là đăng xuất được bạn. POST bắt buộc kèm `_csrf` token mà trang lạ không biết.
- **"Em không viết controller logout à?"** → Không cần. `LogoutFilter` của Spring Security đã làm sẵn.

---

### D3. Dòng 6–7 (+ pop-up chờ duyệt): Đăng ký + OTP

#### Bấm "Đăng ký"

1. Form gửi `POST /register` với `fullName`, `email`, `password`, `confirmPassword`.
2. [AuthController.register](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L128) gọi `validateRegistration`, nhận về `Map<tên ô, câu lỗi>`. Có lỗi thì mở lại pop-up Đăng ký, lỗi hiện dưới từng ô, giữ họ tên + email (**không giữ mật khẩu**).
3. Hợp lệ thì gọi [sendRegistrationCode](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L131):
   - Email đã có tài khoản `PENDING` (đăng ký rồi nhưng chưa nhập OTP): gửi mã mới nếu cần, ném `AUTH_ACCOUNT_NOT_VERIFIED`. Controller bắt lỗi này và **mở pop-up OTP** thay vì báo lỗi.
   - Email đã có tài khoản ở trạng thái khác → `AUTH_ACCOUNT_ALREADY_EXISTS`.
   - **Kiểm tra lại toàn bộ** một lần nữa (không tin form).
   - Tạo `Account`: `username` = phần trước `@` (trùng thì thêm hậu tố ngẫu nhiên), `password_hash` = BCrypt, `role` = REPORTER tạm (cột NOT NULL, Admin cấp role thật sau), **`status = PENDING`**.
   - Gọi `issueOtp(account, VERIFY_EMAIL)`.
4. Controller lưu vào **session**: `OTP_EMAIL`, `OTP_PURPOSE`, `OTP_SENT_AT`. Redirect `/login`, pop-up OTP hiện ra.

#### Validate đăng ký

| Ô | Trình duyệt | Server (`validateRegistration`) |
|---|---|---|
| Họ tên | `required`, `maxlength=100` | không trống, tối đa 100 ký tự |
| Email | `required`, `type="email"` | đúng regex, **chỉ nhận `@gmail.com`**, chưa có tài khoản (trừ `PENDING`) |
| Mật khẩu | `required`, `minlength=8` | ít nhất 8 ký tự, có cả chữ (`\p{L}`, nhận cả chữ có dấu) và số |
| Nhập lại | `required` | khớp mật khẩu |

#### `issueOtp`: tạo và gửi mã (điểm ấn tượng)

[issueOtp](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L349):

1. Tìm dòng `account_otp` **chưa dùng** gần nhất (cùng tài khoản, cùng mục đích). Có thì **ghi đè**, không có thì tạo mới. Vì ghi đè nên **mã cũ mất hiệu lực ngay lập tức**.
2. Sinh mã 6 số bằng `SecureRandom`. `Random` thường có thể đoán được dãy số tiếp theo; `SecureRandom` thì không.
3. **Chỉ lưu hash**: `code_hash = BCrypt(mã)`. Người đọc được DB cũng không biết mã.
4. `attempt_count = 0`, `last_sent_at = bây giờ`, `expires_at = bây giờ + 10 phút` (chỉ để thỏa CHECK `ck_otp_expiry` của DB).
5. **Gửi email sau khi transaction commit** (`registerSynchronization(... afterCommit ...)`). Nếu lưu DB lỗi và rollback thì không có email "ma" chứa mã không tồn tại trong DB.
6. [OtpEmailService.sendVerificationCode](../src/main/java/com/group6/electronic_drafting_system/service/impl/OtpEmailService.java) có `@Async("otpMailExecutor")`: gửi mail ở **luồng khác** (`config/AsyncConfig.java`), người dùng không phải chờ Gmail. Chưa cấu hình SMTP thì mã được in ra console (tiện khi dev / demo).

> Chi tiết khó: `now()` được **làm tròn xuống giây** ([dòng 380](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L380)). Cột DB là `DATETIMEOFFSET(0)` (không có phần lẻ giây). Nếu không làm tròn, Java gửi cả nano giây, SQL Server tự làm tròn từng cột, và CHECK `expires_at = last_sent_at + 10 phút` có thể sai lệch 1 giây và bị từ chối.

#### Bấm "Xác nhận" mã OTP

1. 6 ô input cùng `name="digit"`. Spring gom thành `List<String>`, controller nối lại thành chuỗi 6 số. JS hỗ trợ tự nhảy ô và dán cả mã 6 số một lần.
2. Lấy `email` và `purpose` **từ session**, không lấy từ form. Session hết hạn thì về Login kèm lỗi.
3. [verifyAndRegister](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L159): tài khoản phải đang `PENDING`, rồi gọi [useOtp](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L310). `useOtp` kiểm tra theo thứ tự:

   | Thứ tự | Điều kiện | Kết quả |
   |---|---|---|
   | 1 | đã sai đủ 5 lần | `AUTH_OTP_TOO_MANY_ATTEMPTS`: mã vô hiệu, phải xin mã mới |
   | 2 | quá **5 phút** từ `last_sent_at` | `AUTH_OTP_EXPIRED` |
   | 3 | không đúng 6 chữ số, hoặc `encoder.matches` sai | `attempt_count + 1`, báo lỗi; lần sai thứ 5 báo "sai quá 5 lần" |
   | 4 | đúng | `used_at = bây giờ` (**chỉ dùng được một lần**) |

4. Đúng mã: **`status = AWAITING_APPROVAL`**, xóa thông tin OTP khỏi session, mở pop-up "Đăng ký thành công, đang chờ quản trị viên duyệt".
5. `@Transactional(noRollbackFor = ApplicationException.class)`: khi nhập sai, service ném lỗi nhưng **vẫn lưu** `attempt_count` đã tăng. Mặc định Spring rollback khi có lỗi; nếu rollback thì `attempt_count` không bao giờ tăng và người ta thử sai vô hạn được.

> Liên hệ DB: CHECK `account_otp_attempt_count_check` chỉ cho `attempt_count` từ 0 đến 5. Code dừng ở đúng 5 (kiểm tra `>= 5` trước khi tăng), nên khớp với DB.

#### Bấm "Gửi lại mã"

1. Nút bị khóa và đếm ngược. Số giây do **server tính** từ `OTP_SENT_AT` trong session và gửi xuống (`resendWait`).
2. Backend kiểm tra **2 lớp**: controller xem `OTP_SENT_AT` trong session; service ([resendCode](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L174)) xem `last_sent_at` trong DB. Chưa đủ 60 giây → `AUTH_OTP_RESEND_TOO_SOON`.
3. Đủ 60 giây → `issueOtp` ghi đè mã cũ, báo "Mã mới đã được gửi. Mã cũ không còn hiệu lực."

#### Sau đó: Admin duyệt (màn của TV4, service của em)

[AccountApprovalServiceImpl.approve](../src/main/java/com/group6/electronic_drafting_system/service/impl/AccountApprovalServiceImpl.java#L51):

- Người gọi phải là ADMIN đang ACTIVE.
- Tài khoản phải đang `AWAITING_APPROVAL`.
- Chỉ cấp 3 role REPORTER / DEPARTMENT_HEAD / EDITORIAL_SECRETARY, **không cấp ADMIN** (tránh tự nâng quyền).
- REPORTER và DEPARTMENT_HEAD **bắt buộc chọn ban**.
- Duyệt → `ACTIVE` + gửi email báo. Từ chối → `INACTIVE` (không xóa) + email kèm lý do.

**Vòng đời tài khoản:**

```
Đăng ký form ──► PENDING ──(đúng OTP)──► AWAITING_APPROVAL ──(Admin duyệt)──► ACTIVE
Đăng ký Google ─────────────────────────► AWAITING_APPROVAL ──(Admin từ chối)──► INACTIVE
                                                              Admin khóa ──► LOCKED
```

#### Thầy hỏi

- **"OTP lưu thế nào? Sao không lưu mã luôn?"** → Lưu hash BCrypt. DB bị lộ thì người khác cũng không dùng được mã.
- **"Nhập sai mấy lần thì khóa? Sao phải `noRollbackFor`?"** → 5 lần. Không có `noRollbackFor` thì lỗi làm rollback, số lần sai không được lưu.
- **"Mã hết hạn bao lâu? Sao DB ghi 10 phút?"** → Hiệu lực thực tế 5 phút (`OTP_VALIDITY`). Cột `expires_at` ghi +10 phút chỉ để thỏa CHECK có sẵn trong schema chung của nhóm (nhóm quy ước không sửa schema chung).
- **"Gửi mail lúc nào? Mail chậm thì người dùng có phải chờ không?"** → Sau khi commit, ở luồng riêng (`@Async`). Không phải chờ.
- **"Bấm gửi lại liên tục thì sao?"** → Giao diện khóa nút 60 giây, controller và service đều kiểm tra lại. Sửa HTML bỏ `disabled` vẫn bị server chặn.
- **"Sao đăng ký xong không vào được luôn?"** → Hệ thống nội bộ tòa soạn. Nếu cho tự chọn vai trò thì ai cũng tự làm Thư ký được. Admin cấp vai trò và ban.
- **"Sao chỉ nhận Gmail?"** → Quy ước nhóm. Đăng nhập Google cũng chỉ nhận Gmail nên hai đường thống nhất.
- **"Gõ thẳng `/login?popup=otp` có mở được không?"** → Không. [allowedPopup](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L72) chỉ cho mở `otp` khi session đã có `OTP_EMAIL`, và `reset` khi đã có `RESET_EMAIL`.
- **"Đăng ký lại bằng email đang PENDING thì sao?"** → Không báo trùng mà mở lại pop-up OTP. `sendRegistrationCode` cũng có `noRollbackFor`, để mã mới vừa tạo không bị rollback khi ném `AUTH_ACCOUNT_NOT_VERIFIED`.

---

### D4. Dòng 8–9: Quên mật khẩu + đặt lại

#### 3 bước

| Bước | Người dùng | Hệ thống |
|---|---|---|
| 1. `POST /forgot-password` | nhập email | [sendPasswordResetCode](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L204): sai định dạng thì báo lỗi (lỗi duy nhất được báo). Email không tồn tại, hoặc tài khoản không phải ACTIVE / AWAITING_APPROVAL: **không gửi gì, không báo gì**. Bấm lại trong 60 giây: giữ mã cũ. Còn lại: `issueOtp(RESET_PASSWORD)`. Controller **luôn** hiện cùng một câu "Nếu email đã đăng ký, mã xác nhận đã được gửi." và mở pop-up OTP. |
| 2. `POST /otp/verify` | nhập mã | `purpose = RESET_PASSWORD` → `verifyResetCode` → `useOtp` (5 lần, 5 phút, một lần). Đúng mã: `session.setAttribute(RESET_EMAIL, email)`, mở pop-up "Đặt lại mật khẩu". |
| 3. `POST /reset-password` | nhập mật khẩu mới 2 lần | Lấy email **từ session `RESET_EMAIL`**; không có thì `AUTH_RESET_NOT_ALLOWED`. `validateNewPassword`. `resetPassword` chỉ đổi `password_hash`, **không đổi status, không đổi role**. Xóa `RESET_EMAIL` (đặt lại được đúng một lần). Về Login, điền sẵn email. |

#### Thầy hỏi

- **"Sao email không tồn tại cũng báo đã gửi?"** → Chống dò tài khoản. Người lạ không phân biệt được email nào có trong hệ thống.
- **"Gửi thẳng POST `/reset-password` mà không qua OTP được không?"** → Không. Email lấy từ session, và chỉ có sau khi nhập đúng OTP.
- **"Sao không để email trong ô ẩn của form?"** → Ô ẩn thì người dùng sửa được thành email người khác rồi đặt lại mật khẩu của họ. Session nằm ở server nên không sửa được.
- **"Tài khoản bị khóa quên mật khẩu thì sao?"** → Im lặng, không gửi mã. Không để tài khoản bị khóa tự mở lại bằng đường này.

---

### D5. Dòng nên thêm: Đăng nhập Google

**OAuth2 là gì (ví dụ):** bấm "Đăng nhập bằng Google" → sang trang Google chọn tài khoản → Google báo lại cho hệ thống: "người này là `abc@gmail.com`, email đã xác minh". Hệ thống **không bao giờ thấy mật khẩu Google**.

1. Nút là link `/oauth2/authorization/google`. Spring Security tự chuyển sang Google (đọc `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` từ biến môi trường).
2. Google trả về `/login/oauth2/code/google`. Spring đổi mã lấy thông tin người dùng.
3. [googleRoleMapper](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L86): Google chỉ cho quyền kiểu `OIDC_USER`. Mapper tìm email trong DB và **gắn thêm `ROLE_<role trong DB>`**, để `hasRole()` và sidebar chạy giống đăng nhập mật khẩu.
4. [GoogleLoginSuccessHandler](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L40):

   | Tình huống | Xử lý |
   |---|---|
   | Không có email / `email_verified` khác true | từ chối |
   | Email **chưa có** tài khoản | `registerWithGoogle`: chỉ nhận Gmail, tạo tài khoản `AWAITING_APPROVAL` (bỏ qua OTP vì Google đã xác minh email; mật khẩu là băm của chuỗi ngẫu nhiên), hủy phiên, `/login?registered` |
   | `AWAITING_APPROVAL` | hủy phiên, `/login?awaiting` |
   | `LOCKED` / `INACTIVE` | hủy phiên, báo lỗi |
   | `PENDING` | `confirmEmailByGoogle`: chuyển `AWAITING_APPROVAL`, hủy phiên |
   | `ACTIVE` | `ensureRoleAuthority` (gắn role nếu thiếu), `/dashboard` |

5. "Hủy phiên" (`endSession`): xóa `SecurityContext` và `session.invalidate()`. Người chưa được duyệt **không giữ** được phiên đăng nhập.

#### Thầy hỏi

- **"Tài khoản tạo bằng Google muốn đăng nhập bằng mật khẩu?"** → Dùng Quên mật khẩu để đặt mật khẩu.
- **"Sao phải tự gắn ROLE?"** → Spring không biết role trong DB của mình; Google chỉ cho quyền chung chung.
- **"Sau khi đăng nhập bằng Google, code lấy email người dùng thế nào?"** → Đọc thuộc tính `email` của Google (hàm `currentEmail`). Không dùng `getName()`, vì với Google giá trị đó là mã định danh (`sub`), không phải email. Trước đây màn duyệt của Trưởng ban / Thư ký dùng `getName()` nên bị 403; em đã phát hiện và sửa (xem [G1](#g1-đã-sửa-trưởng-ban--thư-ký-đăng-nhập-bằng-google-bị-403-ở-màn-duyệt-đề-tài)).

---

### D6. Dòng 10: Danh sách đề tài

#### Mở màn `GET /reporter/topics?status=...`

1. **Lớp 1:** `/reporter/**` chỉ REPORTER.
2. [list](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L46): lấy email người đăng nhập (`currentEmail` xử lý cả đăng nhập form lẫn Google), đổi `?status=` thành enum (giá trị lạ thì coi như "Tất cả").
3. [getProposalList](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L58), `@Transactional(readOnly = true)`:
   1. Lấy **chỉ đề tài do mình tạo**: `findByCreator_IdOrderByCreatedAtDescIdDesc(me.getId())`. Spring Data tự sinh SQL từ tên method, tương đương `WHERE creator_id = @me ORDER BY created_at DESC, topic_id DESC`.
   2. `@EntityGraph(attributePaths = "department")`: nạp luôn phòng ban trong cùng câu SQL, tránh lỗi **N+1** (20 đề tài mà chạy 21 câu SQL).
   3. Đếm số đề tài theo từng trạng thái trên **toàn bộ** danh sách. Vì vậy số trên tab và thẻ thống kê luôn đúng, dù đang lọc tab nào.
   4. Lọc theo tab **ở server**, đổi `Topic` (entity) sang `TopicListItemDTO`. Mã hiển thị `TP-<năm>-<id 4 số>` (ví dụ `TP-2026-0012`) được ghép mỗi lần hiển thị, **không lưu DB**.
   5. Lấy số thông báo chưa đọc + 20 thông báo mới nhất cho chuông.
4. Template: thẻ thống kê, tab (mỗi tab là một link, bấm là gửi request mới), bảng. Nút **Sửa** chỉ hiện khi `DEPARTMENT_REVIEW` + `awaitingRevision`. Đây chỉ là ẩn nút; server vẫn kiểm tra lại.
5. Ô tìm kiếm: JS lọc **trên trình duyệt**, ẩn các dòng không chứa từ khóa, không gọi server.

#### Thầy hỏi

- **"Lọc tab và ô tìm kiếm khác nhau thế nào?"** → Tab lọc ở server (đổi URL, chạy lại truy vấn). Tìm kiếm lọc bằng JS trên những dòng đang hiện.
- **"Sao Phóng viên A không thấy đề tài của B?"** → Truy vấn lọc `creator_id = id của A`. Id lấy từ phiên đăng nhập, không lấy từ URL.
- **"Entity khác DTO thế nào? Sao không đưa entity ra view?"** → Entity ánh xạ đúng bảng. DTO chỉ chứa những gì màn hình cần. Đưa entity ra view dễ lộ dữ liệu thừa (ví dụ `password_hash` của người tạo) và dễ gặp lỗi lazy loading khi template đọc quan hệ chưa nạp.
- **"Nhiều đề tài thì sao? Có phân trang không?"** → Chưa phân trang (xem G6). Mỗi Phóng viên chỉ thấy đề tài của mình nên số lượng nhỏ.

---

### D7. Dòng 11–12: Đề xuất đề tài + modal xác nhận

#### Phía trình duyệt (dòng 12 = modal)

1. Nút "Gửi duyệt" có `type="button"` nên **không** submit form. Nó gọi `openConfirm()` (`static/reporter/js/topic-proposal.js` dòng 101).
2. JS kiểm tra tiêu đề và tóm tắt không trống. Sai thì báo đỏ và đặt con trỏ vào ô sai. Có bộ đếm ký tự trực tiếp.
3. Hợp lệ → mở modal "Xác nhận gửi đề tài", hiện lại tiêu đề và tên ban.
4. "Hủy, quay lại chỉnh sửa" → đóng modal, dữ liệu còn nguyên. Bấm nền tối hoặc phím Esc cũng đóng.
5. "Xác nhận gửi" → **khóa cả 2 nút** (tránh bấm 2 lần thành 2 đề tài) rồi `form.submit()`.
6. Nhấn Enter trong ô cũng không gửi thẳng: JS chặn sự kiện submit và mở modal (dòng 144–145).

#### Phía server `POST /reporter/topics`

1. [create](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L60): `@Valid @ModelAttribute TopicCreateRequestDTO` chạy Bean Validation (`@NotBlank`, `@Size(max = 255 / 4000)`). Có lỗi → hiện lại form (không redirect) để `th:errors` in lỗi dưới từng ô.
2. [createProposal](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L88), `@Transactional`:
   1. `requireActiveReporter`: đúng role REPORTER **và** ACTIVE, nếu không → `TOPIC_CREATE_FORBIDDEN` (403).
   2. `validate(request)`: kiểm tra lại độ dài.
   3. Phóng viên **chưa có ban** → `TOPIC_REPORTER_NO_DEPARTMENT`.
   4. Ban phải có **Trưởng ban đang ACTIVE**, nếu không đề tài sẽ không có ai duyệt → `TOPIC_DEPARTMENT_INVALID`.
   5. `mapper.toNewTopic(...)`: creator = mình, department = ban của mình, category = tên ban bỏ chữ "Phòng ", priority = NORMAL, **status = `DEPARTMENT_REVIEW`**, `awaitingRevision = false`.
   6. `topics.save(...)` → `INSERT INTO topic`.
3. Thành công → flash "Đã gửi đề tài thành công…", redirect danh sách. Lỗi nghiệp vụ → hiện lại form kèm câu lỗi đỏ, dữ liệu còn nguyên.

#### Validate

| Kiểm tra | Trình duyệt | Controller | Service | DB |
|---|---|---|---|---|
| Tiêu đề không trống, ≤ 255 | JS + `maxlength` | `@NotBlank @Size` | `validate` | cột NOT NULL, độ dài cột |
| Tóm tắt không trống, ≤ 4000 | JS + `maxlength` | `@NotBlank @Size` | `validate` | |
| Là Phóng viên ACTIVE | – | (SecurityConfig) | `requireActiveReporter` | rule R11 |
| Có ban, ban có Trưởng ban ACTIVE | – | – | có | – |

#### Thầy hỏi

- **"JS kiểm tra rồi sao server còn kiểm tra?"** → JS chỉ để báo nhanh. Người dùng tắt JS hoặc gửi bằng Postman là vượt qua JS.
- **"Kiểm tra 3 lần có thừa không?"** → Mỗi tầng một mục đích (xem C2). `@Valid` để báo lỗi đúng ô; service kiểm lại vì service không biết ai gọi mình.
- **"Sao không cho chọn ban?"** → Quy ước nhóm: đề xuất cho ban của mình để đề tài tới đúng Trưởng ban quản lý mình.
- **"Đề tài mới có trạng thái gì? Có bản nháp không?"** → `DEPARTMENT_REVIEW`, `awaiting_revision = 0`. Tạo và gửi là một bước, không có nháp.
- **"Bấm Xác nhận 2 lần có tạo 2 đề tài không?"** → Không: nút bị khóa ngay khi bấm, và sau POST là redirect (PRG) nên F5 không gửi lại.

---

### D8. Dòng 14, 16, 17: Trưởng ban duyệt

#### Mở màn (dòng 14) `GET /department-head/topics?topic=15`

[getHeadWorkspace](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L66):

1. `requireRole(email, DEPARTMENT_HEAD)`: đúng role, ACTIVE, **có ban**. Sai → `TOPIC_REVIEW_FORBIDDEN` (403).
2. **Hàng chờ** = status `DEPARTMENT_REVIEW` + **ban của mình** + **không** đang chờ phóng viên sửa (`awaiting_revision = 0`), gửi sớm nhất xếp trước.
3. Mỗi đề tài kèm **số vòng đã sửa** (đếm dòng `topic_feedback` loại `CHANGE_REQUEST`) để hiện badge "Đã sửa 1 lần".
4. `pickSelected`: có `?topic=` thì mở đề tài đó, không thì mở đề tài đầu hàng chờ.
5. **Chặn gõ URL:** đề tài được chọn khác ban → `TOPIC_VIEW_FORBIDDEN` (403).
6. `canDecide` = còn `DEPARTMENT_REVIEW` và không chờ sửa. Chỉ khi `true` mới vẽ 2 nút quyết định.
7. `historyOf`: gộp `topic_feedback` + `topic_review` thành dòng thời gian.

Giao diện: 3 thẻ thống kê, hàng chờ bên trái, chi tiết + lịch sử bên phải, nút **Duyệt** và **Yêu cầu sửa**. Hai nút mở `<dialog>` (modal có sẵn của HTML, mở bằng `showModal()`).

#### Bấm "Duyệt" → xác nhận (dòng 17)

[headApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L89), `@Transactional`:

1. `requireRole`.
2. [lockForHead](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L215):
   - `findByIdForUpdate`: đọc **và khóa dòng** Topic (`@Lock(PESSIMISTIC_WRITE)`). Ai muốn đọc để sửa dòng này phải chờ transaction hiện tại xong.
   - Khác ban → 403.
   - **BR-34**: đề tài do chính mình đề xuất → 403.
   - Không còn ở trạng thái chờ mình duyệt → `TOPIC_ALREADY_PROCESSED` (409).
3. `INSERT topic_review`: `review_stage = DEPARTMENT`, `decision = APPROVED`.
4. `topic.reviewed_by_head = mình`, **`status = SECRETARY_REVIEW`**.
5. Commit. Hibernate tự sinh `UPDATE topic` (dirty checking, không cần gọi `save()`).

Kết quả: đề tài rời hàng chờ Trưởng ban, xuất hiện ở hàng chờ Thư ký.

#### Bấm "Yêu cầu sửa" → nhập góp ý (dòng 16)

[headRequestChanges](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L108), `@Transactional`:

1. `requireText`: góp ý **bắt buộc**, tối đa 4000 ký tự.
2. `lockForHead` (như trên).
3. `INSERT topic_feedback`: `type = CHANGE_REQUEST`, `content = góp ý`, `author = mình`.
4. **Status giữ nguyên `DEPARTMENT_REVIEW`**, chỉ bật `awaiting_revision = 1`.
5. `notificationService.notify(phóng viên, ...)` → `INSERT notification`.
6. Cả 3 thao tác ghi trong **một transaction**.

Kết quả: đề tài rời hàng chờ Trưởng ban, Phóng viên thấy chuông đỏ và nút Sửa.

#### Khi lỗi

[backWithError](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L70):
- 403 / 404 → ném tiếp, ra trang lỗi.
- 400 / 409 (thiếu góp ý, đã được xử lý) → flash `error` + **giữ nội dung góp ý đã gõ**, quay lại đúng đề tài. Dialog tự mở lại nhờ `data-reopen`.

#### Validate

| Kiểm tra | Trình duyệt | Service | DB |
|---|---|---|---|
| Góp ý không trống, ≤ 4000 | `textarea required maxlength=4000` | `requireText` | `ck_topic_feedback_content` |
| Đúng ban | – | `lockForHead` | rule R08 |
| Không tự duyệt đề tài của mình | – | `lockForHead` (BR-34) | – |
| Còn chờ duyệt | nút chỉ hiện khi `canDecide` | `lockForHead` sau khi khóa dòng | `ck_topic_awaiting_revision` |
| Mỗi cấp chỉ 1 quyết định | – | – | `uq_topic_review_stage` |

#### Thầy hỏi

- **"Hai Trưởng ban (hoặc 2 tab) cùng bấm Duyệt một lúc thì sao?"** → `findByIdForUpdate` khóa dòng. Người thứ hai phải chờ người thứ nhất commit, sau đó đọc lại thấy status đã là `SECRETARY_REVIEW` → "Đề tài đã được xử lý". Không có 2 dòng duyệt, không đổi trạng thái 2 lần.
- **"Khóa bi quan với khóa lạc quan khác gì? Sao chọn bi quan?"** → Bi quan (pessimistic): khóa trước rồi làm, người sau phải chờ. Lạc quan (optimistic): không khóa, lúc lưu mới so số phiên bản (`@Version`), trùng thì báo lỗi. Thao tác duyệt ít người làm cùng lúc nhưng **không được phép sai**, và bảng `topic` dùng chung không có cột version, nên chọn bi quan cho đơn giản và chắc chắn.
- **"Sao yêu cầu sửa không ghi vào `topic_review` luôn?"** → Bảng `topic_review` có `UNIQUE (topic_id, review_stage)` (`uq_topic_review_stage`): mỗi cấp chỉ được một dòng quyết định. Yêu cầu sửa lặp nhiều vòng nên ghi riêng vào `topic_feedback`.
- **"Sao không thêm trạng thái `REVISION_REQUIRED` cho Topic?"** → Enum trạng thái Topic dùng chung cả nhóm (FL-02, FL-03 đọc nó). Thêm trạng thái mới sẽ ảnh hưởng màn của bạn khác. Dùng cột cờ `awaiting_revision`, và DB có CHECK `ck_topic_awaiting_revision`: cờ chỉ được bật khi status là `DEPARTMENT_REVIEW`.
- **"Trưởng ban có từ chối được không?"** → Không. Đặc tả nhóm chốt ở Iter2: Trưởng ban chỉ Duyệt hoặc Yêu cầu sửa; quyết định loại bỏ đề tài là của Thư ký.
- **"Trưởng ban ban Kinh tế xem đề tài ban Xã hội được không?"** → Không: hàng chờ lọc theo ban, gõ `?topic=` của ban khác bị 403.

---

### D9. Dòng 13, 15: Phóng viên xem góp ý, sửa và gửi lại

#### Dòng 13: Xem góp ý `GET /reporter/topics/{id}/feedback`

1. Vào từ: tên đề tài hoặc nút Góp ý trên danh sách, hoặc bấm một thông báo.
2. [getFeedbackView](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L107): không có đề tài → 404; **không phải chủ đề tài** → `TOPIC_VIEW_FORBIDDEN` (403).
3. `mapper.toHistory`: gộp 2 nguồn thành một dòng thời gian, có nhãn và màu:
   - `topic_feedback` CHANGE_REQUEST → "Trưởng ban yêu cầu sửa"
   - `topic_review` APPROVED → "Trưởng ban đã duyệt" / "Thư ký đã duyệt"
   - `topic_review` REJECTED → "Thư ký từ chối" + lý do
4. `canEdit = DEPARTMENT_REVIEW && awaitingRevision` → mới hiện nút **Sửa đề tài**.

#### Dòng 15: Sửa và gửi lại

**Mở form `GET /reporter/topics/{id}/edit`**
1. [editForm](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L83) gọi `getEditForm` **trước tiên** để kiểm tra quyền: Reporter ACTIVE + `checkCanEdit` (chủ đề tài **và** đang `DEPARTMENT_REVIEW` + `awaiting_revision = 1`). Sai → `TOPIC_EDIT_FORBIDDEN` (403).
2. Dùng lại form tạo mới, nhưng: hiện khung "Góp ý của Trưởng ban" (góp ý mới nhất) phía trên, tiêu đề trang "Sửa đề tài", form trỏ tới `/resubmit`.

**Gửi lại `POST /reporter/topics/{id}/resubmit`**
1. Vẫn đi qua modal xác nhận như khi tạo.
2. `@Valid`. Có lỗi → **vẫn gọi `getEditForm` để kiểm tra quyền** rồi mới hiện lại form.
3. [resubmit](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L134), `@Transactional`: `requireActiveReporter` + `validate` → `findByIdForUpdate` (khóa dòng) → `checkCanEdit` lại → cập nhật `title`, `description` (**ban giữ nguyên**) → `awaiting_revision = 0`.
4. Đề tài quay lại hàng chờ Trưởng ban, có badge "Đã sửa N lần".

#### Thầy hỏi

- **"Gõ `/edit` khi Trưởng ban không yêu cầu sửa thì sao?"** → 403, `checkCanEdit` chặn.
- **"Sửa đề tài của người khác bằng cách đổi id?"** → 403, kiểm tra `creator_id`.
- **"Mở 2 tab và bấm gửi lại 2 lần?"** → Lần 1 tắt cờ `awaiting_revision`. Lần 2 khóa dòng, đọc lại thấy cờ đã tắt → 403. Không bị gửi trùng.
- **"Đề tài bị REJECTED có sửa / gửi lại được không?"** → Không. Phóng viên đề xuất đề tài mới.
- **"Khi sửa có giữ bản cũ không?"** → Không giữ bản cũ của tiêu đề / tóm tắt. Lịch sử lưu ở các vòng góp ý trong `topic_feedback`. (Thầy hỏi tiếp thì trả lời: có thể thêm bảng phiên bản đề tài nếu cần.)

---

### D10. Dòng 18, 19, 21: Thư ký duyệt cuối

#### Mở màn (dòng 18) `GET /secretary/topic-reviews?department=2&topic=15`

URL là `/topic-reviews` vì `/secretary/topics` là màn Topic tòa soạn (FL-02) của bạn khác.

[getSecretaryWorkspace](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L131):
1. `requireRole(EDITORIAL_SECRETARY)`.
2. **Hàng chờ** = `SECRETARY_REVIEW` của **mọi ban**; có `?department=` thì lọc thêm theo ban. Ô chọn ban có `onchange="this.form.submit()"` nên đổi ban là trang tự tải lại.
3. **NFR-03:** đề tài còn `DEPARTMENT_REVIEW` (chưa qua Trưởng ban) mà gõ thẳng `?topic=` → 403.
4. `canDecide` = còn `SECRETARY_REVIEW`.

#### Bấm "Duyệt" → xác nhận (dòng 21): thao tác ghi nhiều bảng nhất

[secretaryApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L154), `@Transactional`:

1. `requireRole` + [lockForSecretary](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L226): khóa dòng; BR-34; phải còn `SECRETARY_REVIEW`, nếu không → `TOPIC_ALREADY_PROCESSED`.
2. Xác định **Trưởng ban đã duyệt**: lấy `topic.reviewed_by_head_id`. Dữ liệu cũ chưa có cột này thì tìm trong `topic_review` cấp DEPARTMENT, APPROVED.
3. Trưởng ban đó **và** Phóng viên đề xuất phải còn ACTIVE, đúng role. Sai → `TOPIC_TEAM_UNAVAILABLE` (ví dụ Admin vừa khóa Trưởng ban).
4. `INSERT topic_review`: SECRETARY, APPROVED.
5. **Tự tạo 2 assignment**, `assigned_by` = Thư ký, nhiệm vụ / deadline để trống (Trưởng ban bổ sung ở FL-03):
   - Phóng viên đề xuất → `LEAD`
   - Trưởng ban đã duyệt → `DEPARTMENT_HEAD`
6. **`status = ACTIVE`**.
7. Thông báo cho Phóng viên: "...đã được duyệt. Bạn là phóng viên viết chính (Lead)."
8. Tất cả trong **một transaction**.

**Vì sao phải đủ các bước trên? Vì mỗi bước giữ một quy tắc của DB:**

| Quy tắc (view `v_rule_violations`) | Đòi hỏi | Bước giữ quy tắc |
|---|---|---|
| R10 | Topic ACTIVE do Reporter đề xuất phải có đủ **2** `topic_review` APPROVED (DEPARTMENT + SECRETARY) | bước 4, và `headApprove` |
| R12 | Topic ACTIVE do Reporter đề xuất phải có assignment `DEPARTMENT_HEAD` | bước 5 |
| R13 | vai trò trong assignment khớp role tài khoản; người giao đúng thẩm quyền | bước 3, `assigned_by` = Thư ký |
| R23 | tài khoản không ACTIVE không được giữ assignment | bước 3 |
| `uq_assignment_lead`, `uq_assignment_dept_head` | mỗi Topic đúng 1 Lead, 1 Trưởng ban đang hiệu lực | khóa dòng ở bước 1 nên không tạo trùng |

#### Bấm "Từ chối" → nhập lý do (dòng 19)

[secretaryReject](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L192):
1. `requireText`: **lý do bắt buộc** (BR-12).
2. `lockForSecretary`.
3. `INSERT topic_review`: SECRETARY, REJECTED, `feedback = lý do`.
4. **`status = REJECTED`**: trạng thái cuối.
5. Thông báo cho Phóng viên kèm "Bấm để xem lý do".

Lý do bắt buộc được kiểm ở **3 tầng**: `textarea required` → `requireText` → CHECK `ck_topic_review_feedback` trong DB (REJECTED thì `feedback` phải có nội dung).

#### Thầy hỏi

- **"Sau khi Thư ký bấm Duyệt, DB thay đổi những gì?"** → 1 dòng `topic_review`, 2 dòng `assignment`, 1 dòng `notification`, `topic.status = ACTIVE`.
- **"Tạo assignment bị lỗi giữa chừng thì sao?"** → Transaction rollback hết: không có đề tài ACTIVE mà thiếu Lead.
- **"Thư ký có thấy đề tài chưa qua Trưởng ban không?"** → Không (NFR-03): hàng chờ chỉ lấy `SECRETARY_REVIEW`, gõ URL cũng bị chặn.
- **"Thư ký có yêu cầu sửa đề tài được không?"** → Không, chỉ Duyệt hoặc Từ chối theo đặc tả.
- **"Kiểm tra dữ liệu đúng chưa bằng cách nào?"** → Chạy `SELECT * FROM v_rule_violations;` phải ra 0 dòng.

---

### D11. Dòng 20: Chuông thông báo

- **Tạo:** [notify](../src/main/java/com/group6/electronic_drafting_system/service/impl/NotificationServiceImpl.java#L32) INSERT một dòng `notification` (`is_read = 0`). Gọi khi Trưởng ban yêu cầu sửa, Thư ký duyệt, Thư ký từ chối. Chạy **trong cùng transaction** với thao tác duyệt: duyệt lỗi thì cũng không có thông báo.
- **Hiện:** `getProposalList` gọi `countUnread` (số đỏ) và `getRecent` (20 cái mới nhất). Chỉ REPORTER có chuông (`sec:authorize="hasRole('REPORTER')"`).
- **Bấm một thông báo** `GET /reporter/notifications/{id}/open`: [open](../src/main/java/com/group6/electronic_drafting_system/service/impl/NotificationServiceImpl.java#L57) kiểm tra thông báo **của mình** → `is_read = 1` → redirect trang góp ý của đề tài.

#### Thầy hỏi

- **"Mở thông báo của người khác bằng cách đổi id?"** → Trả `NOTIFICATION_NOT_FOUND` (404), **cố ý không trả 403**, để người dò không biết id đó có tồn tại hay không.
- **"Thông báo có real-time không?"** → Không. Tải lại trang mới thấy (chưa dùng WebSocket). Phạm vi chỉ là thông báo trong hệ thống, không gửi email.
- **"Trưởng ban / Thư ký có chuông không?"** → Không. Với họ, "được thông báo" nghĩa là đề tài xuất hiện trong hàng chờ.

---

## E. Tính năng khó / ấn tượng: nên chủ động khoe

Xếp theo mức "thầy thấy hay". Mỗi mục: **vấn đề → cách giải → code ở đâu → demo thế nào.**

### E1. Chống duyệt trùng khi 2 người bấm cùng lúc (race condition)
- **Vấn đề:** 2 tab cùng mở 1 đề tài. Cả hai đọc thấy "đang chờ duyệt", cả hai bấm Duyệt → 2 dòng duyệt, hoặc tạo assignment 2 lần.
- **Cách giải:** `findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, [TopicRepository dòng 52](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L52)) khóa dòng, **rồi mới kiểm tra lại trạng thái** trong transaction. Dùng cho Trưởng ban duyệt / yêu cầu sửa, Thư ký duyệt / từ chối, Phóng viên gửi lại. Thêm: nút bị khóa ngay khi bấm, và DB có unique index làm lưới cuối.
- **Demo:** mở 2 tab cùng đề tài → tab 1 Duyệt → tab 2 Duyệt → "Đề tài đã được xử lý."

### E2. Một lần bấm "Duyệt" của Thư ký = một transaction ghi 4 chỗ, giữ đúng quy tắc DB
- Ghi `topic_review`, 2 `assignment`, `notification`, đổi `topic.status`. Lỗi ở đâu thì rollback hết. Giải thích được từng bước giữ rule nào (bảng ở D10).

### E3. Vòng sửa nhiều lần mà không phá schema chung
- Ràng buộc: `topic_review` chỉ 1 dòng / cấp; enum status dùng chung cả nhóm.
- Giải pháp: cờ `awaiting_revision` + bảng `topic_feedback`, CHECK `ck_topic_awaiting_revision` bảo vệ. Chỉ **thêm** cột / bảng, không đổi gì của bạn khác.

### E4. OTP an toàn (6 điểm)
1. Lưu hash BCrypt, không lưu mã.
2. `SecureRandom`.
3. Sai 5 lần vô hiệu mã, **vẫn lưu được số lần sai** nhờ `noRollbackFor`.
4. Gửi lại → mã cũ mất hiệu lực ngay (ghi đè dòng).
5. Chờ 60 giây, kiểm ở cả controller lẫn service.
6. Gửi mail **sau commit** và **bất đồng bộ** (`afterCommit` + `@Async`).

### E5. Chống dò tài khoản (enumeration)
- Login: sai email hay sai mật khẩu đều cùng một câu; kiểm mật khẩu trước rồi mới tới trạng thái.
- Quên mật khẩu: luôn cùng một câu dù email có tồn tại hay không.
- Thông báo của người khác: trả 404 thay vì 403.

### E6. Thông tin nhạy cảm giữ ở session, chặn "nhảy cóc"
- Email đang chờ OTP / được phép đặt lại mật khẩu nằm trong session, không nằm trong form → không đổi được thành email người khác.
- `allowedPopup` chặn mở pop-up OTP / Reset khi chưa tới bước đó.

### E7. Google OAuth2 tích hợp với role trong DB + cổng chờ duyệt
- `googleRoleMapper` gắn `ROLE_<role>` từ DB. Tài khoản mới qua Google cũng phải chờ Admin. Người chưa được duyệt bị hủy phiên ngay.

### E8. Phân quyền 2 lớp + chặn IDOR + BR-34 + NFR-03
- URL (SecurityConfig) + dữ liệu (service). Người duyệt không tự duyệt đề tài của mình. Thư ký không thấy đề tài chưa qua Trưởng ban.

### E9. Có test tự động
- 13 test `@SpringBootTest` cho phần tài khoản, ví dụ `wrongOtpFiveTimesDisablesCode`, `forgotPasswordIsSilentForUnknownOrLockedEmail`, `awaitingAccountCannotLogInButWrongPasswordStaysGeneric`, `newNonGmailIsRejected`. Chạy `./mvnw test`.

---

## F. Kịch bản demo + các phép "thử phá"

**Chuẩn bị trước buổi bảo vệ:**
- Tài khoản mẫu FL-01 (`database/fl01_seed.sql`): `reporter.kinhte@fl01.test` / `Fl01@Test123`, `reporter.giaoduc@fl01.test` / `Fl01@Test123`. Chuẩn bị sẵn 1 Trưởng ban Phòng Kinh tế, 1 Thư ký, 1 Admin trong dữ liệu mẫu của nhóm.
- Mở sẵn console (để đọc OTP nếu chưa cấu hình SMTP) và một cửa sổ SQL để chạy `SELECT * FROM v_rule_violations;`.
- Trưởng ban / Thư ký demo được bằng cả mật khẩu lẫn Google (lỗi [G1](#g1-đã-sửa-trưởng-ban--thư-ký-đăng-nhập-bằng-google-bị-403-ở-màn-duyệt-đề-tài) đã sửa). Muốn demo Google cho các vai trò này thì tài khoản trong DB phải dùng đúng email Google đó.

**Luồng chính (khoảng 7 phút):**

| # | Ai | Làm gì | Nói gì |
|---|---|---|---|
| 1 | Khách | Đăng ký bằng Gmail mới → OTP → nhập sai 1 lần → nhập đúng → pop-up "chờ duyệt" | hash OTP, 5 lần, 5 phút, 60 giây |
| 2 | Khách | Đăng nhập tài khoản vừa tạo | khung xanh "đang chờ duyệt": kiểm mật khẩu trước rồi mới báo trạng thái |
| 3 | Admin (màn TV4) | Duyệt, cấp REPORTER + ban | service của em, không cấp ADMIN |
| 4 | Phóng viên | Đề xuất: để trống → báo đỏ; điền → modal → gửi | 4 tầng validate, khóa nút |
| 5 | Trưởng ban | Yêu cầu sửa: để trống góp ý → lỗi; nhập góp ý → gửi | `topic_feedback`, cờ `awaiting_revision`, transaction |
| 6 | Phóng viên | Chuông đỏ → bấm → trang góp ý → Sửa → gửi lại | IDOR, khóa dòng khi gửi lại |
| 7 | Trưởng ban | Thấy badge "Đã sửa 1 lần" → Duyệt | `SECRETARY_REVIEW`, `reviewed_by_head_id` |
| 8 | Thư ký | Lọc ban → Duyệt (hoặc Từ chối đề tài khác) | 4 bảng / 1 transaction, R10 / R12 |
| 9 | Phóng viên | Chuông "Bạn là Lead" → màn Nhiệm vụ (TV2) có đề tài | nối sang FL-03 / FL-04 |
| 10 | SQL | `SELECT * FROM v_rule_violations;` → 0 dòng | dữ liệu đúng quy tắc |

**Các phép "thử phá" (chọn 2–3 khi thầy hỏi "có chặn được không"):**

| Thử | Kết quả mong đợi | Nhờ đâu |
|---|---|---|
| Phóng viên gõ `/department-head/topics` | 403 | SecurityConfig (lớp 1) |
| Phóng viên B gõ `/reporter/topics/{id của A}/feedback` | 403 | `getFeedbackView` (lớp 2) |
| Trưởng ban gõ `?topic=` của ban khác | 403 | `getHeadWorkspace` |
| 2 tab cùng duyệt một đề tài | tab 2: "Đề tài đã được xử lý" | khóa dòng |
| Quên mật khẩu với email không tồn tại | cùng câu với email có thật | chống dò tài khoản |
| Gõ `/login?popup=reset` | chỉ hiện pop-up Login | `allowedPopup` |
| F12 bỏ `disabled` nút "Gửi lại mã" rồi bấm | "Vui lòng chờ 60 giây" | kiểm tra ở server |
| F12 bỏ `required` ô góp ý rồi gửi trống | "Vui lòng nhập nội dung góp ý" | `requireText` |
| Nhập `<script>alert(1)</script>` vào tiêu đề | hiện nguyên văn, không chạy | Thymeleaf `th:text` tự escape |

---

## G. Điểm yếu đã biết: sửa trước hoặc trả lời thật

> Hội đồng đánh giá cao việc **tự biết điểm yếu và biết cách sửa**. Đừng chối; nói "em biết, hướng sửa là…".

### G1. Đã sửa: Trưởng ban / Thư ký đăng nhập bằng Google bị 403 ở màn duyệt đề tài
- **Lỗi cũ:** [DepartmentHeadTopicController](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L36) và [SecretaryTopicController](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L38) lấy email bằng `principal.getName()`. Đăng nhập bằng mật khẩu thì giá trị này là email. Đăng nhập Google thì nó là **mã số định danh của Google** (`sub`, ví dụ `1034...`), không phải email → `requireRole` không tìm thấy tài khoản → 403.
- **Đã sửa:** thêm hàm `currentEmail(principal)` vào 2 controller, cùng cách với `TopicProposalController.currentEmail`: đăng nhập Google thì đọc thuộc tính `email`, còn lại vẫn dùng `getName()` như cũ. Đăng nhập bằng mật khẩu **không đổi gì**.
- **Nếu thầy hỏi:** "Em phát hiện khi rà lại code: màn của Phóng viên đã xử lý Google, nhưng 2 màn duyệt thì chưa. Em đã thống nhất cách lấy email ở cả 3 controller."

### G2. Đã sửa: không đổi mã phiên sau khi đăng nhập bằng mật khẩu (session fixation)
- **Lỗi cũ:** trước khi đăng nhập, trình duyệt đã có cookie `JSESSIONID` (tạo lúc mở trang `/login`). Đăng nhập xong, `persistSecurityContext` vẫn dùng **đúng phiên đó**. Nếu kẻ xấu cài sẵn được một mã phiên cho nạn nhân, sau khi nạn nhân đăng nhập thì kẻ xấu dùng chung được phiên đã đăng nhập.
- **Đã sửa:** [persistSecurityContext](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L97) gọi `request.changeSessionId()` trước khi lưu thông tin đăng nhập. Hàm này **chỉ đổi mã phiên**, mọi dữ liệu đang có trong session vẫn giữ nguyên.
- Đăng nhập Google vốn không bị, vì filter của Spring Security tự đổi mã phiên.

### G3. Không giới hạn số lần nhập sai mật khẩu khi đăng nhập
- OTP có giới hạn 5 lần, nhưng mật khẩu thì không → có thể thử mật khẩu liên tục (brute force).
- **Trả lời:** "Chưa làm trong phạm vi. Hướng sửa: đếm số lần sai theo tài khoản / IP, khóa tạm vài phút hoặc thêm captcha."

### G4. Đăng ký báo "Email này đã được đăng ký"
- Khác với quên mật khẩu (luôn im lặng), màn đăng ký có cho biết email đã tồn tại.
- **Trả lời:** "Đây là đánh đổi phổ biến: người đăng ký cần biết vì sao không đăng ký được. Hướng chặt hơn: luôn báo 'đã gửi mã', và gửi email 'bạn đã có tài khoản' cho chủ email."

### G5. Hàng chờ đếm số vòng sửa bằng 1 câu SQL cho mỗi đề tài (N+1)
- [toItem](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L250) gọi `countByTopic_IdAndType` cho từng đề tài.
- **Trả lời:** "Hàng chờ mỗi ban nhỏ nên chấp nhận được. Hướng sửa: một câu `GROUP BY topic_id` đếm cho cả hàng chờ."

### G6. Danh sách không phân trang
- **Trả lời:** "Mỗi Phóng viên chỉ thấy đề tài của mình nên ít. Khi cần thì dùng `Pageable` của Spring Data."

### G7. Thông báo không real-time
- **Trả lời:** "Phải tải lại trang. Hướng nâng cấp: WebSocket hoặc gọi định kỳ (polling)."

### G8. Cấu hình / dữ liệu còn sót
- `formLogin(...loginProcessingUrl("/process-login"))` trong SecurityConfig: không form nào gửi tới. Giữ để Spring biết trang login là `/login`.
- `topic_feedback` có loại `REJECTION` nhưng chưa dùng: lý do từ chối lưu ở `topic_review.feedback`.

### G9. Chưa có test tự động cho phần duyệt đề tài
- Phần tài khoản có 13 test. Phần duyệt đề tài đang test thủ công theo kịch bản F + kiểm `v_rule_violations`.
- **Trả lời:** "Hướng bổ sung: test `@SpringBootTest` cho `headApprove`, `secretaryApprove` (kiểm 2 assignment được tạo, gọi lần 2 bị `TOPIC_ALREADY_PROCESSED`)."

---

## H. Ngân hàng câu hỏi nhanh

### Spring / kiến trúc

| Câu hỏi | Trả lời ngắn |
|---|---|
| Sao tách Controller / Service / Repository? | Mỗi lớp một việc: nhận request / nghiệp vụ / truy cập DB. Dễ đọc, dễ test, đổi một lớp không ảnh hưởng lớp khác. |
| Sao service có interface + impl? | Controller chỉ phụ thuộc vào interface. Khi test có thể thay bằng bản giả (mock). |
| Dependency Injection là gì? | Spring tự tạo đối tượng và truyền vào constructor. Ví dụ `AuthController(AuthService authService)`: em không `new AuthServiceImpl()`, Spring đưa vào. |
| `@Transactional` làm gì? | Cả method là "làm hết hoặc không làm gì". Có lỗi giữa chừng thì mọi thay đổi bị hủy (rollback). |
| `readOnly = true` để làm gì? | Báo method chỉ đọc: nhẹ hơn, không theo dõi thay đổi, tránh vô tình ghi. |
| Không thấy gọi `save()` sao vẫn UPDATE? | Dirty checking: entity đọc trong transaction được Hibernate theo dõi; lúc commit thấy khác thì tự sinh `UPDATE`. |
| `@EntityGraph` để làm gì? | Nạp luôn quan hệ (department, creator) trong 1 câu SQL, tránh N+1. |
| Spring Data sinh SQL từ tên method thế nào? | `findBy` + tên thuộc tính + điều kiện + `OrderBy`. Ví dụ `findByStatusOrderByCreatedAtAscIdAsc`. |
| Flash attribute là gì? | Dữ liệu sống qua đúng 1 lần redirect, dùng để hiện "Đã gửi thành công". |
| Layout chung làm thế nào? | Mọi màn trả `layout/app`, đặt `contentView` + `contentFragment`; layout dùng `th:replace` chèn fragment vào. |
| `@ControllerAdvice` dùng ở đâu? | `SidebarAdvice`: chạy kèm mọi controller, thêm menu sidebar đúng role vào model. |
| `@Async` là gì? | Chạy method ở luồng khác (thread pool `otpMailExecutor`), người gọi không phải chờ. |

### Bảo mật

| Câu hỏi | Trả lời ngắn |
|---|---|
| CSRF là gì, chống thế nào? | Trang lạ lừa trình duyệt gửi POST tới hệ thống kèm cookie của bạn. Chống: mọi form có `_csrf` token, Spring từ chối POST thiếu token. |
| CSRF token hết hạn (để trang lâu) thì sao? | `accessDeniedHandler` quay về trang trước thay vì ra 403, người dùng chỉ cần bấm lại. |
| SQL Injection? | Dùng JPA / Spring Data, tham số được truyền riêng (parameter binding), không nối chuỗi SQL. |
| XSS? | Template chỉ dùng `th:text`, Thymeleaf tự escape. Không dùng `th:utext` ở màn của em. |
| BCrypt khác MD5 / SHA thế nào? | BCrypt có salt ngẫu nhiên và cố ý chậm, nên dò mật khẩu hàng loạt rất tốn thời gian. MD5 / SHA rất nhanh nên dễ bị dò. |
| Ẩn nút trên giao diện có đủ chưa? | Chưa. Người dùng vẫn gõ URL hoặc gửi form được. Mọi kiểm tra quyền làm lại ở service. |
| Session fixation? | Đăng nhập thành công thì đổi mã phiên ngay (`request.changeSessionId()`), dữ liệu session giữ nguyên. |
| Admin có bị tự nâng quyền không? | `approve` chỉ cấp 3 role, không cấp ADMIN. |
| Secret Google để đâu? | Biến môi trường `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`; `application.properties` bị gitignore. |

### Database

| Câu hỏi | Trả lời ngắn |
|---|---|
| Sao dùng cờ `awaiting_revision` mà không thêm status? | Enum status dùng chung cả nhóm; thêm cột chỉ là thay đổi "cộng thêm". CHECK đảm bảo cờ chỉ bật ở `DEPARTMENT_REVIEW`. |
| Sao có bảng `topic_feedback` riêng? | `topic_review` chỉ 1 dòng / cấp (`uq_topic_review_stage`); yêu cầu sửa lặp nhiều vòng. |
| Unique index có điều kiện là gì? | `uq_assignment_lead ... WHERE status <> 'REMOVED'`: chỉ tính dòng đang hiệu lực, dòng đã gỡ không tính. |
| Sao không xóa dữ liệu, không `ON DELETE CASCADE`? | Giữ lịch sử để truy vết. Người dùng bị từ chối chuyển `INACTIVE`, không xóa. |
| `v_rule_violations` là gì? | View gom các quy tắc liên bảng mà constraint không khai báo được. Sau mỗi thay đổi phải ra 0 dòng. |
| Sao `reviewed_by_head_id` cần? | Nhớ đúng Trưởng ban đã duyệt để lúc Thư ký duyệt tạo assignment cho đúng người. |
| Thời gian lưu kiểu gì? | `DATETIMEOFFSET(0)` có múi giờ +07:00, làm tròn đến giây. |

### Nghiệp vụ

| Câu hỏi | Trả lời ngắn |
|---|---|
| Ai là Lead của đề tài do Phóng viên đề xuất? | Chính người đề xuất, hệ thống tự giao khi Thư ký duyệt. |
| Nhiệm vụ / deadline của Lead ai nhập? | Để trống, Trưởng ban bổ sung ở FL-03. |
| Phóng viên có xóa / rút đề tài được không? | Không có chức năng xóa. Nếu đề tài bị từ chối thì đề xuất đề tài mới. |
| Đề tài REJECTED gửi lại được không? | Không, đề xuất đề tài mới. |
| Phóng viên chưa có ban thì sao? | Không đề xuất được, báo liên hệ Admin. |
| Ban chưa có Trưởng ban? | Không gửi được đề tài, vì sẽ không có ai duyệt. |
| Người duyệt tự duyệt đề tài của mình? | Bị chặn (BR-34) ở `lockForHead` / `lockForSecretary`. |

---

## I. Chỗ code khác CLAUDE.md / PRD

Nếu thầy so code với tài liệu, trả lời là **"nhóm đã thống nhất theo đặc tả Iter2"**.

| Chỗ | Tài liệu gốc | Code hiện tại |
|---|---|---|
| Trưởng ban từ chối đề tài | từ chối ở mọi cấp | Trưởng ban chỉ Duyệt / Yêu cầu sửa; chỉ Thư ký từ chối |
| Vòng sửa đề tài | không có | có, nhiều vòng (`awaiting_revision` + `topic_feedback`) |
| Hiệu lực OTP | 10 phút | 5 phút (`expires_at` ghi +10 chỉ để thỏa CHECK) |
| Sau khi xác minh OTP | `PENDING` → `ACTIVE` | `PENDING` → `AWAITING_APPROVAL` → Admin duyệt → `ACTIVE` |
| Email đăng ký | không giới hạn | chỉ `@gmail.com` |
| Thông báo | ngoài phạm vi | có chuông trong hệ thống (không gửi email) |

---

## J. Thuật ngữ (giải thích bằng ví dụ)

| Thuật ngữ | Ví dụ dễ hiểu |
|---|---|
| **Transaction** | Chuyển tiền: trừ tài khoản A và cộng tài khoản B phải cùng thành công; lỗi giữa chừng thì hủy cả hai. |
| **Rollback** | Hủy toàn bộ thay đổi của transaction khi có lỗi. |
| **Race condition** | Hai người cùng bấm Duyệt một lúc, kết quả phụ thuộc ai nhanh hơn, có thể bị duyệt 2 lần. |
| **Khóa bi quan (pessimistic lock)** | Vào phòng thì khóa cửa, người sau đứng chờ ngoài. |
| **IDOR** | Đổi `/topics/15` thành `/topics/16` để xem đề tài người khác. |
| **Enumeration (dò tài khoản)** | Thử từng email ở trang quên mật khẩu, xem câu trả lời khác nhau để biết email nào có tài khoản. |
| **CSRF** | Trang web xấu có form ẩn tự gửi `POST /logout` tới hệ thống khi bạn đang đăng nhập. |
| **XSS** | Nhập `<script>` vào tiêu đề, nếu không escape thì script chạy trên máy người xem. |
| **BCrypt / salt** | Băm mật khẩu một chiều; salt là chuỗi ngẫu nhiên trộn vào để 2 mật khẩu giống nhau ra 2 chuỗi băm khác nhau. |
| **OTP** | Mã dùng một lần, 6 số, gửi qua email để chứng minh bạn là chủ email. |
| **OAuth2** | Đăng nhập nhờ Google xác nhận danh tính, hệ thống không biết mật khẩu Google. |
| **Session / cookie** | Server nhớ "phiên này là ai"; trình duyệt giữ mã phiên trong cookie `JSESSIONID`. |
| **Session fixation** | Kẻ xấu cài sẵn mã phiên cho nạn nhân; nạn nhân đăng nhập xong, kẻ xấu dùng chung phiên đó. |
| **DTO** | "Gói dữ liệu" chỉ chứa những gì màn hình cần, khác với entity ánh xạ đúng bảng. |
| **N+1** | Lấy 20 đề tài bằng 1 câu SQL, rồi mỗi đề tài chạy thêm 1 câu lấy phòng ban → 21 câu. |
| **Dirty checking** | Hibernate tự phát hiện entity đã bị sửa và tự sinh `UPDATE` lúc commit. |
| **PRG** | Sau POST thì redirect sang GET, để F5 không gửi lại form. |
| **Flash attribute** | Dữ liệu chỉ sống qua đúng 1 lần redirect (thông báo "thành công"). |
| **`afterCommit`** | Việc chỉ làm sau khi DB đã lưu chắc chắn (gửi email OTP). |
| **`@Async`** | Giao việc cho luồng khác làm, người dùng không phải chờ. |
