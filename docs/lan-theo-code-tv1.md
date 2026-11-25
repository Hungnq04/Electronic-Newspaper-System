# Lần theo code từng chức năng – Phần TV1

Mục đích: khi thầy hỏi "chức năng này chạy thế nào", em **chỉ vào code theo đúng thứ tự**, từ cái nút trên màn hình tới DB rồi quay về màn hình.

- Mỗi bước có **link `Tên file:dòng`**. Trong VS Code: mở file này, **Ctrl+click vào link** (hoặc mở Preview bằng Ctrl+Shift+V rồi bấm link) là nhảy tới đúng dòng.
- Bên dưới mỗi bước: **Chỉ** (dòng nào cần chỉ cho thầy), **Nói** (câu giải thích ngắn), **Sang bước sau** (bấm phím gì).
- Số dòng đúng với code hiện tại (sau commit `af4a540`). Sửa code thì số dòng có thể lệch; khi đó tìm theo **tên method**.
- Với controller, link thường trỏ vào dòng **`@GetMapping` / `@PostMapping`** (dòng có URL); tên method nằm **ngay dòng dưới**. Chỉ cả hai dòng: "URL này → method này".

---

## 0. Cách nhảy trong code (đọc kỹ phần này trước)

### 0.1. Phím tắt VS Code cần thuộc

| Phím | Làm gì | Dùng khi |
|---|---|---|
| **Ctrl+click** (hoặc F12) | Nhảy tới **chỗ khai báo** của method / class / biến | đi từ lời gọi `abc.xyz()` sang code của `xyz` |
| **Ctrl+F12** | Nhảy tới **chỗ cài đặt** (implementation) | đang đứng ở **interface** (ví dụ `AuthService`) muốn sang **impl** (`AuthServiceImpl`) |
| **Alt+←** | **Quay lại** chỗ vừa đứng | nhảy sâu quá, muốn về bước trước |
| **Ctrl+Shift+F** | Tìm chữ trong **toàn dự án** | những chỗ Spring tự nối, không Ctrl+click được (xem 0.2) |
| **Ctrl+P** | Mở file theo tên | gõ `login.html`, `AuthController`… |
| **Ctrl+G** | Nhảy tới số dòng | đã mở file, gõ số dòng |
| **Ctrl+Shift+O** | Danh sách method trong file | tìm nhanh `useOtp`, `lockForHead`… |
| **Shift+F12** | Ai đang gọi method này | thầy hỏi "method này được gọi ở đâu?" |

> Ctrl+click với file Java cần cài **Extension Pack for Java** trong VS Code. Nếu Ctrl+click không nhảy, kiểm tra extension này.

### 0.2. Ba kiểu "nhảy" trong dự án

| Kiểu | Ví dụ | Cách nhảy |
|---|---|---|
| **A. Gọi method bình thường** | `topicService.createProposal(...)` | **Ctrl+click** vào tên method |
| **B. Gọi qua interface** | Ctrl+click `authService.login` → vào `AuthService.java` (interface, chỉ có khai báo, không có code) | đứng ở tên method trong interface, bấm **Ctrl+F12** → vào `AuthServiceImpl` |
| **C. Spring tự nối** (không Ctrl+click được) | form `th:action="@{/login}"` → method `@PostMapping("/login")`; `return "layout/app"` → file template; `successHandler(...)` → Spring tự gọi | **Ctrl+Shift+F** tìm chuỗi (ví dụ `"/login"`), hoặc **Ctrl+P** mở file template |

> Câu nói khi tới kiểu C: *"Chỗ này Spring tự nối: Spring đọc URL trong form, tìm method có `@PostMapping` cùng URL rồi gọi. Em tìm bằng Ctrl+Shift+F."*

### 0.3. Khuôn chung của mọi chức năng

```
Template (nút / form)  ──[C: Spring nối URL]──►  Controller  ──[A Ctrl+click]──►  Service (interface)
                                                                                     │ [B Ctrl+F12]
                                                                                     ▼
Template (kết quả)  ◄──[C: return "tên view"]──  Controller  ◄────────────  ServiceImpl ──[A]──► Repository / Mapper
```

Repository (ví dụ `AccountRepository`) là **interface không có code**: Spring Data tự sinh câu SQL từ **tên method**. Ctrl+click vào sẽ thấy dòng khai báo; tới đó thì nói *"Spring tự sinh SQL từ tên method"* rồi **Alt+←** quay lại.

---

## 1. Mở trang đăng nhập (dòng 3)

1. **[SecurityConfig.java:49](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L49)**
   - Chỉ: `"/login", "/register", ... "/otp/**" ... permitAll()`.
   - Nói: "Các URL tài khoản ai cũng vào được, chưa cần đăng nhập."
   - Sang bước sau: **Ctrl+Shift+F** tìm `@GetMapping("/login")`.
2. **[AuthController.java:43](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L43)** · `loginPage(...)`
   - Chỉ: dòng 49–54 đổi tham số URL (`?error`, `?logout`, `?awaiting`, `?registered`) thành câu thông báo; dòng 57–58 lấy pop-up cần mở (flash trước, `?popup=` sau).
   - Chỉ: dòng 59 `allowedPopup(popup, session)` → **Ctrl+click** `allowedPopup`.
3. **[AuthController.java:72](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L72)** · `allowedPopup`
   - Chỉ: dòng 76–77: chỉ mở `otp` khi session có `OTP_EMAIL`, chỉ mở `reset` khi có `RESET_EMAIL`.
   - Nói: "Chặn gõ thẳng `/login?popup=reset` khi chưa qua bước OTP."
   - **Alt+←** quay lại dòng 68 `return "login"` → **Ctrl+P** gõ `login.html`.
4. **[login.html:31](../src/main/resources/templates/login.html#L31)**
   - Chỉ: 6 `<section data-lp-popup="...">` (dòng 31, 60, 95, 121, 131, 147), mỗi cái có `th:hidden="${popup != '...'}"`.
   - Nói: "Server quyết định pop-up nào hiện. Chuyển Login ↔ Đăng ký ↔ Quên mật khẩu thì JS làm ngay trên trình duyệt."
   - (Nếu hỏi JS) **[auth-popup.js:22](../src/main/resources/static/js/auth-popup.js#L22)**: bấm link `data-lp-open` thì gọi `openPopup` ([dòng 5](../src/main/resources/static/js/auth-popup.js#L5)).

---

## 2. Đăng nhập bằng mật khẩu + khung thông báo (dòng 3–4)

1. **[login.html:36](../src/main/resources/templates/login.html#L36)** · `<form method="post" th:action="@{/login}">`
   - Chỉ: ô `email`, `password`; nút "Đăng nhập" dòng 48.
   - Nói: "Bấm nút thì form gửi `POST /login`, Thymeleaf tự chèn ô ẩn `_csrf`."
   - (Nếu hỏi) **[auth-popup.js:46](../src/main/resources/static/js/auth-popup.js#L46)**: khóa nút sau khi bấm để không gửi 2 lần.
   - Sang bước sau (kiểu C): **Ctrl+Shift+F** tìm `@PostMapping("/login")`.
2. **[AuthController.java:96](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L96)** · `login(...)`
   - Chỉ: dòng 100 `authService.login(email, password)`.
   - Sang bước sau: **Ctrl+click** `login` → nhảy vào [AuthService.java:9](../src/main/java/com/group6/electronic_drafting_system/service/AuthService.java#L9) (interface) → **Ctrl+F12**.
3. **[AuthServiceImpl.java:70](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L70)** · `login`
   - Chỉ dòng 72: `accounts.findByEmailIgnoreCase(...)` → không có thì lỗi `AUTH_INVALID_CREDENTIALS`.
     - (Nếu hỏi) **Ctrl+click** `findByEmailIgnoreCase` → [AccountRepository.java:35](../src/main/java/com/group6/electronic_drafting_system/repository/AccountRepository.java#L35): "Spring Data tự sinh `SELECT ... WHERE email = ?`" → **Alt+←**.
   - Chỉ dòng 75: `encoder.matches(password, passwordHash)` → sai thì **cùng lỗi** trên.
     - (Nếu hỏi `encoder` ở đâu ra) mở [SecurityConfig.java:35](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L35): `new BCryptPasswordEncoder()`.
   - Chỉ dòng 77–89: `switch (account.getStatus())`:
     - 78 `LOCKED`, 79 `INACTIVE` → lỗi (khung đỏ)
     - 81 `AWAITING_APPROVAL` → thông báo chờ duyệt (khung xanh)
     - 82–87 `PENDING` → dòng 85 `issueOtpIfNoValidCode` (gửi mã nếu mã cũ hết hạn), trả về để controller mở pop-up OTP
     - 88 `ACTIVE` → đi tiếp
   - Chỉ dòng 90: `authenticationManager.authenticate(...)`.
     - Nói: "Spring Security kiểm tra lần 2." Mở [SecurityConfig.java:37](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L37) `userDetailsService`: dòng 40 gắn role, 41 `accountLocked`, 42 `disabled` nếu không ACTIVE.
   - Chỉ dòng 92: lưu kết quả vào `SecurityContextHolder`.
   - **Alt+←** về controller.
4. **[AuthController.java:101](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L101)**
   - Chỉ dòng 101–108: nếu `PENDING` → `startOtp(...)` (dòng 103) + flash `popup = "otp"` → về `/login`, hiện pop-up OTP.
   - Chỉ dòng 109: `authService.persistSecurityContext(request)` → **Ctrl+click** → **Ctrl+F12**.
5. **[AuthServiceImpl.java:97](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L97)** · `persistSecurityContext`
   - Chỉ dòng 100: `request.changeSessionId()` (đổi mã phiên, chống session fixation).
   - Chỉ dòng 101–102: lưu `SecurityContext` vào session.
   - **Alt+←** về controller.
6. **[AuthController.java:110](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L110)** · `return "redirect:/dashboard"`
   - Sang bước sau: **Ctrl+Shift+F** tìm `@GetMapping("/dashboard")`.
7. **[DashboardController.java:19](../src/main/java/com/group6/electronic_drafting_system/controller/DashboardController.java#L19)** · `dashboard`
   - Chỉ dòng 13–17: bảng role → màn chính (`ROLE_REPORTER` → `/reporter/topics`…).
   - Nói: "Đọc quyền của người đăng nhập rồi chuyển tới màn chính của vai trò đó."

**Khi đăng nhập lỗi (dòng 4 – khung thông báo):**

8. **[AuthController.java:111](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L111)** · `catch (ApplicationException ex)`
   - Chỉ dòng 112: giữ lại email; dòng 113–117: chờ duyệt → `message` (xanh), còn lại → `error` (đỏ); dòng 118 về `/login`.
9. **[login.html:27](../src/main/resources/templates/login.html#L27)**
   - Chỉ dòng 27: khung đỏ `th:if="${error}"`; dòng 28: khung xanh `th:if="${message}"`.

---

## 3. Đăng xuất (dòng 5)

1. **[sidebar.html:73](../src/main/resources/templates/fragment/sidebar.html#L73)** · `<form th:action="@{/logout}" method="post" onsubmit="return confirm(...)">`
   - Nói: "Hỏi xác nhận, rồi gửi `POST /logout` kèm CSRF."
   - Sang bước sau: **Ctrl+Shift+F** tìm `logoutSuccessUrl`.
2. **[SecurityConfig.java:69](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L69)** · `.logout(logout -> logout.logoutSuccessUrl("/login?logout"))`
   - Nói: "Không có controller logout. `LogoutFilter` của Spring Security tự xóa session, rồi chuyển về `/login?logout`."
3. **[AuthController.java:50](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L50)**
   - Chỉ: `if (logout != null) ... "Bạn đã đăng xuất."` → hiện ở khung xanh `login.html:28`.

---

## 4. Đăng ký (dòng 6)

1. **[login.html:65](../src/main/resources/templates/login.html#L65)** · `<form th:action="@{/register}">`
   - Chỉ: các ô `fullName`, `email`, `password`, `confirmPassword`; nút dòng 89.
   - Sang bước sau: **Ctrl+Shift+F** tìm `@PostMapping("/register")`.
2. **[AuthController.java:128](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L128)** · `register(...)`
   - Chỉ dòng 134: `authService.validateRegistration(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[AuthServiceImpl.java:109](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L109)** · `validateRegistration`
   - Chỉ dòng 111–112 họ tên; 114 `emailError` (định dạng); 116–117 chỉ nhận `@gmail.com`; 120–122 email đã có tài khoản (trừ PENDING); 125 `validateNewPassword`.
   - (Nếu hỏi mật khẩu) **Ctrl+click** `validateNewPassword` → [AuthServiceImpl.java:226](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L226): ≥ 8 ký tự, có chữ và số, nhập lại khớp.
   - **Alt+←** về controller.
4. **[AuthController.java:135](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L135)**
   - Chỉ: có lỗi → `backToRegister` ([dòng 157](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L157)): mở lại pop-up Đăng ký, giữ họ tên + email, **không giữ mật khẩu**.
   - Chỉ dòng 137: `authService.sendRegistrationCode(...)` → **Ctrl+click** → **Ctrl+F12**.
5. **[AuthServiceImpl.java:131](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L131)** · `sendRegistrationCode`
   - Chỉ dòng 133–141: email đã có: `PENDING` → gửi mã nếu cần (137) rồi báo "chưa xác thực"; khác → "đã được đăng ký".
   - Chỉ dòng 143: **kiểm tra lại** toàn bộ (không tin form).
   - Chỉ dòng 146 `createUsername`, 149 `encoder.encode(password)` (BCrypt), 151 role tạm REPORTER, **152 `status = PENDING`**, 153 lưu DB.
   - Chỉ dòng 154: `issueOtp(account, VERIFY_EMAIL)` → **Ctrl+click** `issueOtp`.
6. **[AuthServiceImpl.java:349](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L349)** · `issueOtp` (điểm ấn tượng)
   - Chỉ dòng 351: tìm mã chưa dùng gần nhất → **ghi đè** (mã cũ mất hiệu lực).
   - Chỉ dòng 358: sinh 6 số bằng `SecureRandom`; dòng 360: **chỉ lưu hash**; 361–363 đặt lại số lần sai, thời điểm gửi; 364 lưu.
   - Chỉ dòng 368–371: gửi mail **sau khi commit** (`afterCommit`).
   - **Ctrl+click** `sendVerificationCode` (dòng 371) → [OtpEmailService.java:24](../src/main/java/com/group6/electronic_drafting_system/service/impl/OtpEmailService.java#L24): `@Async("otpMailExecutor")` gửi ở luồng khác; dòng 31: chưa cấu hình SMTP thì in mã ra console.
   - (Nếu hỏi luồng ở đâu ra) [AsyncConfig.java:12](../src/main/java/com/group6/electronic_drafting_system/config/AsyncConfig.java#L12).
   - **Alt+←** (2–3 lần) về controller.
7. **[AuthController.java:138](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L138)** · `startOtp(...)` → **Ctrl+click**
   - Tới [AuthController.java:283](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L283): lưu `OTP_EMAIL`, `OTP_PURPOSE`, `OTP_SENT_AT` vào **session**.
   - Nói: "Email nằm ở session (server), không nằm trong form, nên người dùng không sửa được."
8. **[AuthController.java:153](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L153)** · flash `popup = "otp"` → về `/login` → pop-up OTP ([login.html:95](../src/main/resources/templates/login.html#L95)).

---

## 5. Nhập mã OTP + pop-up chờ duyệt (dòng 7)

1. **[login.html:103](../src/main/resources/templates/login.html#L103)** · `<form th:action="@{/otp/verify}">`
   - Chỉ dòng 105: 6 ô cùng `name="digit"`.
   - (Nếu hỏi JS) [auth-popup.js:58](../src/main/resources/static/js/auth-popup.js#L58): tự nhảy ô, dán cả mã 6 số.
   - Sang bước sau: **Ctrl+Shift+F** tìm `@PostMapping("/otp/verify")`.
2. **[AuthController.java:169](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L169)** · `verifyOtp(...)`
   - Chỉ dòng 172–173: lấy email + mục đích **từ session**; 174–178 không có thì về Login.
   - Chỉ dòng 180: nối 6 ô thành chuỗi 6 số.
   - Chỉ dòng 182–183: mục đích `VERIFY_EMAIL` → `authService.verifyAndRegister(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[AuthServiceImpl.java:159](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L159)** · `verifyAndRegister`
   - Chỉ dòng 158 (ngay trên): `@Transactional(noRollbackFor = ApplicationException.class)`.
     - Nói: "Nhập sai thì vẫn lưu số lần sai, không bị rollback."
   - Chỉ dòng 164: `useOtp(...)` → **Ctrl+click**.
4. **[AuthServiceImpl.java:310](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L310)** · `useOtp`
   - Chỉ theo thứ tự: 314–315 sai đủ 5 lần; 316–317 quá 5 phút; 318–322 sai mã → `attempt_count + 1` (319) và lưu (320); 325–326 đúng → `used_at` (dùng 1 lần).
   - **Alt+←** về `verifyAndRegister`: dòng 166 **`status = AWAITING_APPROVAL`**.
   - **Alt+←** về controller.
5. **[AuthController.java:184](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L184)**
   - Chỉ dòng 184 `clearOtp`, dòng 186 flash `popup = "awaiting"` → pop-up "Đăng ký thành công, đang chờ duyệt" ([login.html:121](../src/main/resources/templates/login.html#L121)).
   - Chỉ dòng 194–196: sai mã → mở lại pop-up OTP, lỗi dưới 6 ô.

**Gửi lại mã:**

6. **[login.html:114](../src/main/resources/templates/login.html#L114)** · form `/otp/resend`, nút có `data-wait=${resendWait}`.
   - (JS) [auth-popup.js:98](../src/main/resources/static/js/auth-popup.js#L98): đếm ngược, khóa nút.
7. **[AuthController.java:205](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L205)** · `resendOtp`
   - Chỉ dòng 212: kiểm tra 60 giây **lần 1** (theo session).
   - Chỉ dòng 217: `authService.resendCode(...)` → **Ctrl+click** → **Ctrl+F12** → [AuthServiceImpl.java:174](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L174).
   - Chỉ dòng 186: kiểm tra 60 giây **lần 2** (theo DB); dòng 188 `issueOtp` (ghi đè mã cũ).

---

## 6. Quên mật khẩu + đặt lại (dòng 8–9)

1. **[login.html:136](../src/main/resources/templates/login.html#L136)** · form `/forgot-password` → **Ctrl+Shift+F** `@PostMapping("/forgot-password")`.
2. **[AuthController.java:231](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L231)** · `forgotPassword`
   - Chỉ dòng 235: `authService.sendPasswordResetCode(email)` → **Ctrl+click** → **Ctrl+F12**.
3. **[AuthServiceImpl.java:204](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L204)** · `sendPasswordResetCode`
   - Chỉ dòng 205–206: chỉ báo lỗi khi sai định dạng email.
   - Chỉ dòng 209: email không có / không ACTIVE, AWAITING_APPROVAL → **im lặng return**.
   - Chỉ dòng 211: bấm lại trong 60 giây → giữ mã cũ; dòng 212 `issueOtp(..., RESET_PASSWORD)`.
   - **Alt+←** về controller.
4. **[AuthController.java:246](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L246)**
   - Chỉ dòng 246–248: **luôn** mở pop-up OTP với **cùng một câu** "Nếu email đã đăng ký, mã xác nhận đã được gửi."
   - Nói: "Chống dò tài khoản."
5. Nhập OTP → lại vào **[AuthController.java:169](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L169)** `verifyOtp`, nhưng lần này rẽ nhánh `else`:
   - Chỉ dòng 188: `authService.verifyResetCode(...)` (→ [AuthServiceImpl.java:217](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L217), dòng 222 lại gọi `useOtp`).
   - Chỉ dòng 191: `session.setAttribute(RESET_EMAIL, email)`; dòng 192 mở pop-up Đặt lại mật khẩu.
6. **[login.html:152](../src/main/resources/templates/login.html#L152)** · form `/reset-password` → **Ctrl+Shift+F** `@PostMapping("/reset-password")`.
7. **[AuthController.java:252](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L252)** · `resetPassword`
   - Chỉ dòng 256–261: email lấy **từ session `RESET_EMAIL`**, không có thì chặn.
   - Chỉ dòng 262: `validateNewPassword`; dòng 269: `authService.resetPassword(...)` → **Ctrl+click** → **Ctrl+F12** → [AuthServiceImpl.java:239](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L239): dòng 246 chỉ đổi `password_hash`, không đổi status / role.
   - **Alt+←**: dòng 270 xóa `RESET_EMAIL` (chỉ đặt lại được một lần).

---

## 7. Đăng nhập bằng Google (dòng nên thêm)

1. **[login.html:52](../src/main/resources/templates/login.html#L52)** · `<a href="/oauth2/authorization/google">`
   - Nói: "Link này do Spring Security xử lý sẵn: chuyển sang trang Google chọn tài khoản."
   - Sang bước sau: **Ctrl+Shift+F** tìm `oauth2Login`.
2. **[SecurityConfig.java:61](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L61)** · `.oauth2Login(...)`
   - Chỉ dòng 62: `userAuthoritiesMapper(googleRoleMapper(accounts))`; dòng 63: `successHandler(googleSuccessHandler)`; dòng 65–68: `failureHandler`.
   - Nói: "Google trả về thì Spring gọi 2 thứ em cấu hình: mapper gắn quyền, rồi handler xử lý sau khi thành công."
   - **Ctrl+click** `googleRoleMapper` (dòng 62).
3. **[SecurityConfig.java:86](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L86)** · `googleRoleMapper`
   - Chỉ dòng 91: lấy `email` từ Google; dòng 95: tìm trong DB; dòng 96: **gắn `ROLE_<role trong DB>`**.
   - Nói: "Google chỉ cho quyền chung chung, em gắn thêm role trong DB để `hasRole()` và sidebar chạy."
   - **Alt+←** về dòng 63 → **Ctrl+click** `googleSuccessHandler` (hoặc **Ctrl+P** `GoogleLoginSuccessHandler`).
4. **[GoogleLoginSuccessHandler.java:40](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L40)** · `onAuthenticationSuccess`
   - Chỉ dòng 43–48: không có email / `email_verified` khác true → từ chối.
   - Chỉ dòng 50: tìm tài khoản theo email.
   - Chỉ dòng 51–61: **chưa có tài khoản** → dòng 54 `authService.registerWithGoogle(...)`.
     - **Ctrl+click** `registerWithGoogle` → **Ctrl+F12** → [AuthServiceImpl.java:254](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L254): dòng 258 chỉ nhận Gmail; 268 mật khẩu là băm chuỗi ngẫu nhiên; **271 `AWAITING_APPROVAL`** (bỏ qua OTP vì Google đã xác minh email). **Alt+←**.
     - Dòng 59 `endSession` (hủy phiên), dòng 60 về `/login?registered`.
   - Chỉ dòng 64–67: `AWAITING_APPROVAL` → hủy phiên, `/login?awaiting`.
   - Chỉ dòng 69–72: `LOCKED` / `INACTIVE` → báo lỗi.
   - Chỉ dòng 74–79: `PENDING` → dòng 76 `confirmEmailByGoogle` ([AuthServiceImpl.java:277](../src/main/java/com/group6/electronic_drafting_system/service/impl/AuthServiceImpl.java#L277)) → chuyển `AWAITING_APPROVAL`.
   - Chỉ dòng 81: `ensureRoleAuthority` ([dòng 88](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L88)); dòng 83 → `/dashboard` (như mục 2 bước 7).
   - Chỉ [dòng 110](../src/main/java/com/group6/electronic_drafting_system/config/GoogleLoginSuccessHandler.java#L110) `endSession`: xóa context + `session.invalidate()`.
5. (Nếu hỏi Google đăng nhập rồi vào màn duyệt thế nào) **[DepartmentHeadTopicController.java:81](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L81)** · `currentEmail(principal)`
   - Nói: "Đăng nhập Google thì `getName()` là mã `sub` của Google, nên em đọc thuộc tính `email`."

---

## 8. Admin duyệt tài khoản (màn của TV4, service của em)

1. **[AdminAccountController.java:113](../src/main/java/com/group6/electronic_drafting_system/controller/AdminAccountController.java#L113)** · `@PostMapping("/{id}/approve")` (file của TV4)
   - Chỉ dòng 118: `accountApprovalService.approve(...)` → **Ctrl+click** → **Ctrl+F12**.
2. **[AccountApprovalServiceImpl.java:51](../src/main/java/com/group6/electronic_drafting_system/service/impl/AccountApprovalServiceImpl.java#L51)** · `approve`
   - Chỉ dòng 52 `requireAdmin` (phải là ADMIN đang ACTIVE); 53 `awaitingAccount` (phải đang chờ duyệt); 54 chỉ cấp 3 role (`GRANTABLE_ROLES`, [dòng 24](../src/main/java/com/group6/electronic_drafting_system/service/impl/AccountApprovalServiceImpl.java#L24)); 64 `ACTIVE`; 66 gửi email.
   - Từ chối: [dòng 72](../src/main/java/com/group6/electronic_drafting_system/service/impl/AccountApprovalServiceImpl.java#L72) `reject` → dòng 76 `INACTIVE` (không xóa), dòng 78 email kèm lý do.

---

## 9. Danh sách đề tài (dòng 10)

1. **[SidebarService.java:25](../src/main/java/com/group6/electronic_drafting_system/service/SidebarService.java#L25)** · menu "Danh sách đề tài" → `/reporter/topics`
   - Nói: "Menu do `SidebarAdvice` ([dòng 14](../src/main/java/com/group6/electronic_drafting_system/config/SidebarAdvice.java#L14), `@ControllerAdvice`) thêm vào mọi trang, theo role."
   - **[SecurityConfig.java:55](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L55)**: `/reporter/**` chỉ REPORTER (lớp phân quyền 1).
   - Sang bước sau: **Ctrl+Shift+F** tìm `@GetMapping("/topics")`.
2. **[TopicProposalController.java:45](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L45)** · `list(...)`
   - Chỉ dòng 47: `currentEmail(auth)` ([dòng 155](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L155): xử lý cả form lẫn Google), `parseStatus(status)` ([dòng 149](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L149): giá trị lạ → "Tất cả").
   - Chỉ dòng 47: `topicService.getProposalList(...)` → **Ctrl+click** → [TopicService.java:11](../src/main/java/com/group6/electronic_drafting_system/service/TopicService.java#L11) → **Ctrl+F12**.
3. **[TopicServiceImpl.java:58](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L58)** · `getProposalList`
   - Chỉ dòng 60: `visibleTopics(me)` → **Ctrl+click** → [dòng 176](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L176): REPORTER → `findByCreator_Id...` (chỉ đề tài **của mình**).
     - **Ctrl+click** `findByCreator_IdOrderByCreatedAtDescIdDesc` → [TopicRepository.java:16–17](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L16): `@EntityGraph` nạp luôn phòng ban (tránh N+1). **Alt+←** ×2.
   - Chỉ dòng 62: đếm theo trạng thái; 63–67: tạo tab; 69–72: lọc theo tab **ở server** + `mapper::toListItem`.
     - (Nếu hỏi mã TP-2026-0012) [TopicMapper.java:89](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L89) `codeOf`: ghép khi hiển thị, không lưu DB.
   - Chỉ dòng 74–77: lấy số thông báo chưa đọc + 20 thông báo cho chuông.
   - **Alt+←** về controller.
4. **[TopicProposalController.java:48](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L48)** · `return page(...)` → **Ctrl+click** `page` → [dòng 130](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L130)
   - Chỉ: đặt `contentView`, `contentFragment`, CSS/JS, `return "layout/app"`.
   - Sang bước sau: **Ctrl+P** `layout/app.html`.
5. **[app.html:31](../src/main/resources/templates/layout/app.html#L31)** · `th:replace="~{__${contentView}__ :: __${contentFragment}__}"`
   - Nói: "Layout chung chèn nội dung của từng màn vào đây; dòng 29 chèn sidebar."
   - **Ctrl+P** `topic-proposal-list.html`.
6. **[topic-proposal-list.html:67](../src/main/resources/templates/reporter/topic-proposal-list.html#L67)** · fragment `topicProposalListContent`
   - Chỉ dòng 124: tab (mỗi tab là link `?status=`); dòng 153: `th:each` từng đề tài; dòng 157: tên đề tài → trang góp ý; dòng 179: nút **Sửa** (chỉ hiện khi đang được yêu cầu sửa).
   - Chỉ dòng 132: ô tìm kiếm → [topic-proposal.js:11](../src/main/resources/static/reporter/js/topic-proposal.js#L11): lọc **trên trình duyệt**, không gọi server.

---

## 10. Đề xuất đề tài + modal xác nhận (dòng 11–12)

**Mở form:**

1. **[topic-proposal-list.html:86](../src/main/resources/templates/reporter/topic-proposal-list.html#L86)** · nút "Đề xuất" → `/reporter/topics/new`.
2. **[TopicProposalController.java:52](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L52)** · `createForm`
   - Chỉ dòng 54: tạo `TopicCreateRequestDTO` rỗng; dòng 55: `formPage(auth, model, null)` → **Ctrl+click**.
3. **[TopicProposalController.java:116](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L116)** · `formPage`
   - Chỉ dòng 118: `getMyDepartment` (ban của phóng viên, chỉ đọc); dòng 119: `editTopicId = null` = chế độ tạo mới.
4. **[topic-proposal-create.html:37](../src/main/resources/templates/reporter/topic-proposal-create.html#L37)** · `<form th:action="${editTopicId == null} ? @{/reporter/topics} : ...">`
   - Chỉ dòng 43 ô tiêu đề (`th:field`), 46 chỗ in lỗi (`th:errors`), 63 ô tóm tắt; dòng 74 nút "Gửi duyệt" có `type="button"`.

**Bấm "Gửi duyệt" (trình duyệt, dòng 12 = modal):**

5. **[topic-proposal.js:125](../src/main/resources/static/reporter/js/topic-proposal.js#L125)** · `openButton.addEventListener('click', openConfirm)`
   - **Ctrl+click** không chạy trong JS thuần, dùng **Ctrl+Shift+O** gõ `openConfirm` → [dòng 101](../src/main/resources/static/reporter/js/topic-proposal.js#L101): kiểm tra ô trống ([`validate` dòng 84](../src/main/resources/static/reporter/js/topic-proposal.js#L84)), điền tiêu đề + ban vào modal ([topic-proposal-create.html:83](../src/main/resources/templates/reporter/topic-proposal-create.html#L83)).
   - Chỉ [dòng 137–140](../src/main/resources/static/reporter/js/topic-proposal.js#L137): bấm "Xác nhận gửi" → **khóa 2 nút** → `form.submit()`.
   - Chỉ [dòng 144](../src/main/resources/static/reporter/js/topic-proposal.js#L144): Enter cũng bị chặn, phải qua modal.

**Server (POST):**

6. **[TopicProposalController.java:59](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L59)** · `create(@Valid ... TopicCreateRequestDTO ..., BindingResult binding, ...)`
   - (Nếu hỏi `@Valid` kiểm gì) **Ctrl+click** `TopicCreateRequestDTO` → [dòng 15–21](../src/main/java/com/group6/electronic_drafting_system/dto/Topic/TopicCreateRequestDTO.java#L15): `@NotBlank`, `@Size(max = 255 / 4000)`. **Alt+←**.
   - Chỉ dòng 62: có lỗi → hiện lại form (không redirect).
   - Chỉ dòng 64: `topicService.createProposal(...)` → **Ctrl+click** → **Ctrl+F12**.
7. **[TopicServiceImpl.java:88](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L88)** · `createProposal`
   - Chỉ dòng 90 `requireActiveReporter` ([dòng 159](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L159): đúng REPORTER **và** ACTIVE); 91 `validate` ([dòng 165](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L165): kiểm lại độ dài); 94 chưa có ban → lỗi; 97–100 ban phải có Trưởng ban ACTIVE (`selectableDepartments`, [dòng 185](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L185)).
   - Chỉ dòng 102: `mapper.toNewTopic(...)` → **Ctrl+click** → [TopicMapper.java:69](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L69): `status = DEPARTMENT_REVIEW`, `awaitingRevision = false`… **Alt+←**; `topics.save(...)` = INSERT.
   - **Alt+←** về controller.
8. **[TopicProposalController.java:69](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L69)** · flash "Đã gửi đề tài thành công" + dòng 70 `redirect:/reporter/topics` (PRG).

---

## 11. Xem góp ý (dòng 13)

1. **[topic-proposal-list.html:157](../src/main/resources/templates/reporter/topic-proposal-list.html#L157)** · tên đề tài → `/reporter/topics/{id}/feedback`.
2. **[TopicProposalController.java:74](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L74)** · `feedback`
   - Chỉ dòng 77: `topicService.getFeedbackView(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicServiceImpl.java:107](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L107)** · `getFeedbackView`
   - Chỉ dòng 109: không có → 404; **dòng 111: không phải chủ đề tài → 403** (chặn đổi id trên URL).
   - Chỉ dòng 114–115: `mapper.toHistory(...)` → **Ctrl+click** → [TopicMapper.java:45](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L45): gộp `topic_feedback` + `topic_review` thành dòng thời gian. **Alt+←**.
   - Chỉ dòng 116: `isWaitingForReporter` = có hiện nút Sửa không.
4. **[topic-feedback.html:25](../src/main/resources/templates/reporter/topic-feedback.html#L25)** · nút Sửa `th:if="${view.canEdit()}"`; dòng 61: `th:each` lịch sử.

---

## 12. Sửa và gửi lại (dòng 15)

1. **[topic-feedback.html:25](../src/main/resources/templates/reporter/topic-feedback.html#L25)** (hoặc [topic-proposal-list.html:179](../src/main/resources/templates/reporter/topic-proposal-list.html#L179)) → `/reporter/topics/{id}/edit`.
2. **[TopicProposalController.java:82](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L82)** · `editForm`
   - Chỉ dòng 85: gọi `getEditForm` **trước tiên** để kiểm tra quyền → **Ctrl+click** → **Ctrl+F12** → [TopicServiceImpl.java:121](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L121): dòng 124 `checkCanEdit` → [dòng 149](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L149): phải là chủ đề tài **và** đang được yêu cầu sửa. **Alt+←**.
   - Chỉ dòng 87: `formPage(auth, model, topicId)` → [dòng 121–123](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L121): chế độ sửa thì lấy góp ý mới nhất hiện phía trên form ([topic-proposal-create.html:28](../src/main/resources/templates/reporter/topic-proposal-create.html#L28)).
3. Form giờ trỏ tới `/resubmit` ([topic-proposal-create.html:37](../src/main/resources/templates/reporter/topic-proposal-create.html#L37)), vẫn qua modal như mục 10.
4. **[TopicProposalController.java:90](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L90)** · `resubmit`
   - Chỉ dòng 93–95: có lỗi form → **vẫn kiểm tra quyền** trước khi hiện lại.
   - Chỉ dòng 98: `topicService.resubmit(...)` → **Ctrl+click** → **Ctrl+F12**.
5. **[TopicServiceImpl.java:134](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L134)** · `resubmit`
   - Chỉ dòng 138: `findByIdForUpdate` (**khóa dòng**); 139 `checkCanEdit` lại; 142–143 cập nhật tiêu đề, tóm tắt; **145 `setAwaitingRevision(false)`** → quay lại hàng chờ Trưởng ban.
   - (Nếu hỏi khóa dòng) **Ctrl+click** `findByIdForUpdate` → [TopicRepository.java:52](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L52): `@Lock(PESSIMISTIC_WRITE)`.

---

## 13. Trưởng ban mở màn duyệt (dòng 14)

1. **[SidebarService.java:18](../src/main/java/com/group6/electronic_drafting_system/service/SidebarService.java#L18)** · menu "Duyệt đề tài" → `/department-head/topics`.
   - [SecurityConfig.java:51](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L51): `/department-head/**` chỉ DEPARTMENT_HEAD.
   - **Ctrl+P** `DepartmentHeadTopicController`.
2. **[DepartmentHeadTopicController.java:34](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L34)** · `workspace`
   - Chỉ dòng 25 (`@RequestMapping("/department-head/topics")`) + dòng 34 `@GetMapping` = URL.
   - Chỉ dòng 36: `getHeadWorkspace(currentEmail(principal), topic)` → **Ctrl+click** → [TopicReviewService.java:9](../src/main/java/com/group6/electronic_drafting_system/service/TopicReviewService.java#L9) → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:66](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L66)** · `getHeadWorkspace`
   - Chỉ dòng 67: `requireRole` → [dòng 235](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L235): đúng role, ACTIVE, **có ban** (lớp phân quyền 2). **Alt+←**.
   - Chỉ dòng 71–73: **hàng chờ** → **Ctrl+click** tên method repository → [TopicRepository.java:35](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L35): `DEPARTMENT_REVIEW` + đúng ban + `AwaitingRevisionFalse`, cũ nhất trước. **Alt+←**.
   - Chỉ `toItem` ([dòng 250](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L250)): đếm số vòng đã sửa để hiện badge.
   - Chỉ dòng 75: `pickSelected` ([dòng 244](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L244)): có `?topic=` thì mở đề tài đó.
   - Chỉ **dòng 77–79: khác ban → 403** (chặn gõ URL).
   - Chỉ dòng 80: `canDecide`; dòng 82 `historyOf` ([dòng 255](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L255)).
   - **Alt+←** về controller dòng 38 `contentView = "department_head/topic-review"` → **Ctrl+P** `department_head/topic-review.html`.
4. **[topic-review.html (department_head):35](../src/main/resources/templates/department_head/topic-review.html#L35)** · hàng chờ (fragment dùng chung [fragment/topic-review.html:28](../src/main/resources/templates/fragment/topic-review.html#L28))
   - Chỉ dòng 48: 2 nút chỉ hiện khi `ws.canDecide()`; dòng 49 nút Duyệt `data-open="approve-dialog"`; dòng 52 nút Yêu cầu sửa.
   - Chỉ [fragment/topic-review.html:108](../src/main/resources/templates/fragment/topic-review.html#L108): bấm nút → `showModal()` mở `<dialog>`.

---

## 14. Trưởng ban bấm "Duyệt" (dòng 17)

1. **[topic-review.html (department_head):71–72](../src/main/resources/templates/department_head/topic-review.html#L71)** · `<dialog id="approve-dialog">` + form `POST /department-head/topics/{id}/approve`.
2. **[DepartmentHeadTopicController.java:45](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L45)** · `approve`
   - Chỉ dòng 48: `headApprove(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:89](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L89)** · `headApprove`
   - Chỉ dòng 91: `lockForHead` → **Ctrl+click** → [dòng 215](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L215):
     - 216 `findByIdForUpdate` = **khóa dòng**
     - 217 khác ban → 403
     - 219 **BR-34**: đề tài của chính mình → 403
     - 221 không còn chờ duyệt → "Đề tài đã được xử lý" (409)
     - Nói: "Khóa dòng rồi mới kiểm tra lại, nên 2 người bấm cùng lúc thì người sau nhận lỗi."
   - **Alt+←**: dòng 94–99 ghi `topic_review` (DEPARTMENT, APPROVED); dòng 102 nhớ Trưởng ban đã duyệt; **dòng 103 `status = SECRETARY_REVIEW`**.
   - Nói: "Không gọi `save()` cho topic: Hibernate tự UPDATE lúc commit (dirty checking)."
4. **[DepartmentHeadTopicController.java:49–50](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L49)** · flash "Đã duyệt…" + redirect về hàng chờ.

---

## 15. Trưởng ban bấm "Yêu cầu sửa" (dòng 16)

1. **[topic-review.html (department_head):85–89](../src/main/resources/templates/department_head/topic-review.html#L85)** · `<dialog id="changes-dialog">`, `textarea name="feedback" required maxlength="4000"`.
2. **[DepartmentHeadTopicController.java:56](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L56)** · `requestChanges`
   - Chỉ dòng 60: `headRequestChanges(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:108](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L108)** · `headRequestChanges`
   - Chỉ dòng 110: `requireText` ([dòng 283](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L283): bắt buộc, ≤ 4000).
   - Chỉ dòng 111: `lockForHead` (như mục 14).
   - Chỉ dòng 114–119: ghi `topic_feedback` loại `CHANGE_REQUEST`.
   - Chỉ **dòng 122: `setAwaitingRevision(true)`** (status vẫn `DEPARTMENT_REVIEW`).
   - Chỉ dòng 123: `notificationService.notify(...)` → **Ctrl+click** → **Ctrl+F12** → [NotificationServiceImpl.java:32](../src/main/java/com/group6/electronic_drafting_system/service/impl/NotificationServiceImpl.java#L32): INSERT `notification`. **Alt+←**.
   - Nói: "Ba thao tác ghi nằm trong một `@Transactional` (dòng 107)."
4. Khi lỗi: **[DepartmentHeadTopicController.java:64](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L64)** → `backWithError` ([dòng 70](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L70)): 403/404 ném tiếp; lỗi nhẹ → giữ góp ý đã gõ, mở lại dialog (`data-reopen`, [fragment/topic-review.html:112](../src/main/resources/templates/fragment/topic-review.html#L112)).

---

## 16. Thư ký mở màn duyệt cuối + lọc ban (dòng 18)

1. **[SidebarService.java:34](../src/main/java/com/group6/electronic_drafting_system/service/SidebarService.java#L34)** · "Duyệt đề tài cuối" → `/secretary/topic-reviews`; [SecurityConfig.java:53](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L53).
2. **[SecretaryTopicController.java:35](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L35)** · `workspace`
   - Chỉ dòng 38: `getSecretaryWorkspace(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:131](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L131)** · `getSecretaryWorkspace`
   - Chỉ dòng 135–138: hàng chờ `SECRETARY_REVIEW` mọi ban, lọc theo `?department=`.
   - Chỉ **dòng 142–144: NFR-03**: đề tài chưa qua Trưởng ban → 403.
   - Chỉ dòng 145 `canDecide`; dòng 148 danh sách ban cho ô chọn.
4. **[topic-final-review.html:28–30](../src/main/resources/templates/secretary/topic-final-review.html#L28)** · form GET + `<select name="department" onchange="this.form.submit()">`
   - Nói: "Đổi ban là trang tự gửi lại, không cần nút."

---

## 17. Thư ký bấm "Duyệt" (dòng 21) – thao tác ghi nhiều bảng nhất

1. **[topic-final-review.html:80–81](../src/main/resources/templates/secretary/topic-final-review.html#L80)** · `approve-dialog` → `POST /secretary/topic-reviews/{id}/approve`.
2. **[SecretaryTopicController.java:47](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L47)** · `approve`
   - Chỉ dòng 50: `secretaryApprove(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:154](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L154)** · `secretaryApprove`
   - Chỉ dòng 156: `lockForSecretary` → [dòng 226](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L226): khóa dòng, BR-34, phải còn `SECRETARY_REVIEW`. **Alt+←**.
   - Chỉ dòng 159–165: lấy Trưởng ban đã duyệt (`reviewed_by_head_id`, dữ liệu cũ thì tìm trong `topic_review`).
   - Chỉ dòng 168–170: Trưởng ban và Phóng viên phải còn ACTIVE (rule R13, R23).
   - Chỉ dòng 172–177: ghi `topic_review` (SECRETARY, APPROVED) → giữ rule R10.
   - Chỉ **dòng 181–182: tự tạo 2 assignment** `LEAD` + `DEPARTMENT_HEAD` → giữ rule R12.
   - Chỉ **dòng 185: `status = ACTIVE`**; dòng 186: thông báo "Bạn là Lead".
   - Nói: "Bốn chỗ ghi trong một transaction (dòng 153): lỗi ở đâu thì hủy hết."

---

## 18. Thư ký bấm "Từ chối" (dòng 19)

1. **[topic-final-review.html:95–99](../src/main/resources/templates/secretary/topic-final-review.html#L95)** · `reject-dialog`, `textarea name="reason" required`.
2. **[SecretaryTopicController.java:58](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L58)** · `reject` → dòng 62 `secretaryReject(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[TopicReviewServiceImpl.java:192](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L192)** · `secretaryReject`
   - Chỉ dòng 194: lý do bắt buộc (`requireText`); 195 khóa dòng; 198–204 ghi `topic_review` REJECTED + lý do; **207 `status = REJECTED`**; 208 thông báo.
   - Nói: "Lý do bắt buộc kiểm ở 3 tầng: `textarea required`, `requireText`, CHECK `ck_topic_review_feedback` trong DB."

---

## 19. Chuông thông báo (dòng 20)

1. **[topic-proposal-list.html:37–47](../src/main/resources/templates/reporter/topic-proposal-list.html#L37)** · `sec:authorize="hasRole('REPORTER')"`, mỗi thông báo là link `/reporter/notifications/{id}/open`.
   - (JS mở / đóng chuông) [topic-proposal.js:27](../src/main/resources/static/reporter/js/topic-proposal.js#L27).
   - (Số đỏ lấy ở đâu) [TopicServiceImpl.java:76–77](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L76) → `countUnread`, `getRecent`.
2. **[TopicProposalController.java:109](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L109)** · `openNotification`
   - Chỉ dòng 111: `notificationService.open(...)` → **Ctrl+click** → **Ctrl+F12**.
3. **[NotificationServiceImpl.java:57](../src/main/java/com/group6/electronic_drafting_system/service/impl/NotificationServiceImpl.java#L57)** · `open`
   - Chỉ dòng 61–63: không phải của mình → `NOTIFICATION_NOT_FOUND` (404, cố ý không 403); dòng 64 `setRead(true)`.
   - **Alt+←**: dòng 112 redirect sang trang góp ý (mục 11).

---

## 20. Khi có lỗi quyền (dùng chung mọi màn)

1. Service ném `ApplicationException(ErrorCode.X)` (ví dụ [TopicServiceImpl.java:111](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L111)).
   - **Ctrl+click** `TOPIC_VIEW_FORBIDDEN` → vào `ErrorCode.java`: mỗi mã có câu tiếng Việt + `HttpStatus`.
2. **[TopicProposalController.java:142](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L142)** · `@ExceptionHandler` → dòng 145 `response.sendError(403, ...)`.
3. Spring Boot tự tìm `templates/error/403.html` (**Ctrl+P** `403.html`).
4. Token CSRF hết hạn: [SecurityConfig.java:71](../src/main/java/com/group6/electronic_drafting_system/config/SecurityConfig.java#L71) quay về trang trước thay vì ra 403.

---

## Mẹo luyện

1. Mỗi ngày chọn 3 mục, tự đi lại bằng phím tắt, **không bấm link trong file này**: bắt đầu từ `Ctrl+P` mở template, rồi chỉ dùng Ctrl+click / Ctrl+F12 / Ctrl+Shift+F / Alt+←.
2. Tập nói to câu ở dòng **Nói** khi đang chỉ.
3. Khi thầy hỏi "method này được gọi ở đâu", đứng ở tên method bấm **Shift+F12**.
