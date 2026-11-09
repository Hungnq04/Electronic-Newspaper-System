# Tổng hợp kiến thức Spring Boot qua dự án CMS Core

Tài liệu này gom lại mọi câu hỏi đã hỏi, đi **từ cơ bản lên**, có code thật của dự án làm ví dụ.
Đọc lần lượt từ trên xuống. Phần nào chưa hiểu thì dừng lại ở phần đó, chưa cần đọc tiếp.

> Đường dẫn file trong tài liệu tính từ thư mục gốc dự án.
> Số dòng có thể lệch nếu sau này file bị sửa; khi đó hãy tìm theo **tên method**.

---

## Mục lục

1. [Bức tranh lớn: 4 nhân vật](#1-bức-tranh-lớn-4-nhân-vật)
2. [Class, object, annotation](#2-class-object-annotation)
3. [Bean và Dependency Injection](#3-bean-và-dependency-injection)
4. [Ba tầng: Controller, Service, Repository](#4-ba-tầng-controller-service-repository)
5. [Entity và DTO](#5-entity-và-dto)
6. [Request, GET/POST, form, redirect](#6-request-getpost-form-redirect)
7. [Hai giai đoạn: lúc chạy app và lúc có request](#7-hai-giai-đoạn-lúc-chạy-app-và-lúc-có-request)
8. [Filter chain, SecurityConfig, session](#8-filter-chain-securityconfig-session)
9. [DispatcherServlet và @ControllerAdvice](#9-dispatcherservlet-và-controlleradvice)
10. [Thymeleaf: từ model ra HTML](#10-thymeleaf-từ-model-ra-html)
11. [Transaction](#11-transaction)
12. [Luồng đăng nhập từ đầu tới dashboard](#12-luồng-đăng-nhập-từ-đầu-tới-dashboard)
13. [Màn dashboard Trưởng phòng](#13-màn-dashboard-trưởng-phòng)
14. [Màn thẩm định bài viết (workspace review)](#14-màn-thẩm-định-bài-viết-workspace-review)
15. [Trang công khai /news](#15-trang-công-khai-news)
16. [Bảng tra nhanh: file nào làm gì](#16-bảng-tra-nhanh-file-nào-làm-gì)
17. [Câu thầy hay hỏi + trả lời ngắn](#17-câu-thầy-hay-hỏi--trả-lời-ngắn)
18. [Tự kiểm chứng bằng mắt](#18-tự-kiểm-chứng-bằng-mắt)

---

## 1. Bức tranh lớn: 4 nhân vật

```
[Trình duyệt]  ──gửi request──►  [Spring]  ──gọi──►  [Code của nhóm]  ──hỏi──►  [Database]
   (Chrome)    ◄──trả response──  (có sẵn)  ◄──trả──   (nhóm viết)     ◄──trả──  (SQL Server)
```

| Nhân vật | Là gì | Ai viết |
|---|---|---|
| Trình duyệt | Hiện trang, gửi yêu cầu khi bấm link / nút | Có sẵn |
| Spring | Bộ khung nhận yêu cầu, kiểm tra quyền, chuyển tới đúng chỗ | Có sẵn (thư viện) |
| Code của nhóm | Controller, Service, Repository, DTO, file HTML | Nhóm viết |
| Database | Nơi lưu dữ liệu | Nhóm tạo bằng file SQL |

**Ý quan trọng nhất:** code của nhóm **không tự chạy**. Nó chỉ chạy khi **trình duyệt gửi yêu cầu** và **Spring gọi tới**.

---

## 2. Class, object, annotation

**Class** = bản thiết kế. **Object** = đồ vật thật làm ra từ bản thiết kế. Muốn gọi method thì phải có object.

```java
public class DepartmentHeadServiceImpl { ... }   // class: bản vẽ cái máy
new DepartmentHeadServiceImpl(...)                // object: cái máy thật, chạy được
```

**Annotation** (`@...`) = **nhãn dán** lên code để Spring biết đoạn code đó dùng làm gì.
Nhãn **tự nó không chạy gì cả**. Mỗi bộ phận của Spring được viết sẵn để **chỉ đọc một loại nhãn**.

| Nhãn | Bộ phận nào đọc | Nó làm gì |
|---|---|---|
| `@Controller`, `@Service`, `@Component` | Phần lõi Spring | Tạo bean từ class |
| `@GetMapping("/x")`, `@PostMapping("/x")` | Bảng tra URL | Ghi "URL /x → method này" |
| `@ControllerAdvice` + `@ModelAttribute` | Bộ phận gọi controller | Chạy method này **trước** mọi controller |
| `@ExceptionHandler` | Bộ phận xử lý lỗi | Chạy method này **khi có lỗi** |
| `@Transactional` | Bộ phận transaction | Bọc method trong transaction |
| `@Getter`, `@Setter`, `@Builder` | Lombok (lúc biên dịch) | Tự sinh getXxx, setXxx, builder |

---

## 3. Bean và Dependency Injection

### Bean là gì

**Bean là object mà Spring tự tạo và quản lý.** Bạn không viết `new` cho controller, service, repository.

```java
@Service                                         // NHÃN: "Spring ơi, tạo bean từ class này"
public class DepartmentHeadServiceImpl { ... }   // CLASS: bản vẽ

// Lúc chạy app, Spring tự làm (bạn không thấy dòng này):
new DepartmentHeadServiceImpl(...)               // BEAN: object thật, cất trong kho của Spring
```

- `@Service` **không phải** là bean. Nó là **nhãn** khiến Spring tạo bean.
- Xóa `@Service` → không có bean → controller cần service này sẽ báo lỗi **ngay lúc khởi động app**.

**Cái gì là bean trong dự án:**

| Cách thành bean | Ví dụ |
|---|---|
| `@Controller` | `DepartmentHeadController`, `AuthController`, `DashboardController` |
| `@Service` | `DepartmentHeadServiceImpl`, `AuthServiceImpl` |
| `@Component` | `DraftEntryMapper`, `ReviewWorkspaceMapper` |
| `@ControllerAdvice` | `SidebarAdvice` |
| Interface `extends JpaRepository` | `ArticleRepository`, `AccountRepository` |
| Method `@Bean` trong class `@Configuration` | `passwordEncoder()`, `securityFilterChain(...)` trong `SecurityConfig` |

**Không phải bean:** DTO, Entity (chúng là hộp dữ liệu, được tạo mới liên tục).

**3 đặc điểm của bean:**
1. Spring tạo, không phải bạn.
2. Tạo **một lần** lúc chạy app; mọi người dùng **chung**.
3. Được **tự cắm vào nhau** qua constructor.

> Vì mọi người dùng chung một bean, service **không được lưu dữ liệu riêng của từng người vào field**.
> Field chỉ chứa "dụng cụ" (repository, mapper); dữ liệu riêng nằm trong biến cục bộ của method.

### Dependency Injection (tiêm phụ thuộc)

Class **không tự `new`** thứ nó cần, chỉ **khai báo "tôi cần cái này"** trong constructor, Spring **tự đưa vào**.

`src/main/java/.../controller/DepartmentHeadController.java`:

```java
// ① KHAI BÁO Ô TRỐNG: chưa có gì, KHÔNG phải "gọi", KHÔNG tạo object
private final DepartmentHeadService departmentHeadService;
private final DepartmentHeadReviewService reviewService;

// ② Spring GỌI constructor này lúc chạy app và TRUYỀN 2 bean vào
public DepartmentHeadController(DepartmentHeadService departmentHeadService,
                                DepartmentHeadReviewService reviewService) {
    this.departmentHeadService = departmentHeadService;   // cất bean vào ô ①
    this.reviewService = reviewService;
}

// ③ Khi có request: lấy ra dùng
model.addAttribute("dashboard", departmentHeadService.getDashboard(...));
```

- Bạn vẫn phải viết ① và ② vì Spring cần **biết controller cần gì**. Cái bạn **không** phải viết là chữ `new`.
- Kiểu khai báo là **interface** (`DepartmentHeadService`). Spring tìm class cài đặt nó (`DepartmentHeadServiceImpl`) để đưa vào.
- `final` = chỉ gán **một lần** trong constructor, sau đó không đổi được.

Ví dụ đời thường: bàn làm việc có **khay trống** ghi "máy khoan" (①). Ngày đầu, quản đốc (Spring) đọc phiếu (②) và **đặt máy thật vào khay**. Khi làm việc, bạn **lấy máy trong khay ra dùng** (③).

### Repository: interface không có code mà vẫn chạy SQL

```java
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmailIgnoreCase(String email);   // chỉ có TÊN, không có thân { }
}
```

Lúc chạy app, **Spring Data JPA tự tạo class ẩn** cài đặt interface này. SQL được suy ra từ:

| Cách | Ví dụ | Spring hiểu là |
|---|---|---|
| Đọc **tên method** | `findByEmailIgnoreCase(email)` | `SELECT ... FROM account WHERE LOWER(email) = LOWER(?)` |
| Đọc **`@Query`** viết sẵn | `@Query("select count(a) from Article a where ...")` | Chạy đúng câu đã viết |

Khi service gọi `accounts.findByEmailIgnoreCase(email)`: Hibernate dịch thành SQL → gửi SQL Server → nhận các dòng → đổi thành object `Account` → trả về.

---

## 4. Ba tầng: Controller, Service, Repository

| Tầng | Vai trò | Ví dụ đời thường (quán cơm) |
|---|---|---|
| **Controller** | **Cửa nhận** request: nhận yêu cầu, gọi service, trả về tên trang | Người phục vụ nhận order |
| **Service** | **Làm việc thật**: kiểm tra quyền, nghiệp vụ, tính toán | Đầu bếp nấu |
| **Repository** | **Hỏi database** (chạy SQL) | Kho lấy nguyên liệu |

Thứ tự gọi: **Controller → Service → Repository → Database**, rồi kết quả đi ngược lại.

**Interface + Impl** (`DepartmentHeadService` + `DepartmentHeadServiceImpl`):
- Interface = **menu** (liệt kê có việc gì). Impl = **công thức nấu** (làm thế nào).
- Spring không bắt buộc tách đôi (ví dụ `SidebarService` chỉ là 1 class), nhưng đây là **quy ước của nhóm** (CLAUDE.md mục 13), có ích khi viết test.

---

## 5. Entity và DTO

| | Entity | DTO |
|---|---|---|
| Là gì | Dữ liệu **thô, đúng như trong DB** | Dữ liệu **đã chuẩn bị sẵn để hiển thị** (hoặc dữ liệu form gửi lên) |
| Ví dụ | `Account`, `Article`, `Topic` | `DepartmentHeadDashboardDTO`, `ReviewWorkspaceDTO`, `CommentRequestDTO` |
| Ai tạo | Hibernate đọc từ DB | Service (hoặc Spring đổ từ form) |

**DTO không được "gọi"**, vì nó không làm gì. DTO chỉ là **cái hộp**.

```
Controller ──gọi──► Service ──gọi──► Repository ──► Database
                       │◄──trả Entity─────┘
                       │  Service TẠO DTO, ĐIỀN dữ liệu vào
Controller ◄──trả DTO──┘
    │  model.addAttribute("dashboard", DTO)
    ▼
Template đọc ${dashboard.xxx}
```

Ví dụ: Entity có `fullName = "Nguyễn Thu Hà"`, DTO thêm `userInitials = "NH"` cho avatar.
Entity có danh sách `Article`, DTO có `stats` = 4 con số đã đếm sẵn.

**Hai chiều của DTO:**
- **Server → trình duyệt** (hiển thị): `ReviewWorkspaceDTO`, `ReviewCommentDTO`... service tạo bằng `builder()...build()`.
- **Trình duyệt → server** (form gửi lên): `LoginRequestDTO`, `CommentRequestDTO`... Spring tạo và đổ dữ liệu form vào **theo tên** (`name="email"` → `setEmail(...)`).

---

## 6. Request, GET/POST, form, redirect

| Khái niệm | Hiểu đơn giản |
|---|---|
| **Request** | Trình duyệt gửi lên: "cho tôi trang X" hoặc "đây là dữ liệu form" |
| **Response** | Server trả về: trang HTML, hoặc "hãy đi tới trang khác" |
| **GET** | **Xin xem** (mở trang, bấm link) |
| **POST** | **Gửi dữ liệu** (bấm nút submit form) |
| **Redirect** | Server không tự mở trang khác được; nó trả lời *"hãy đi tới X"*, rồi trình duyệt **tự gửi request mới** |

**Ai gửi request?** Không có dòng Java nào "gọi" controller. **Trình duyệt tự gửi**, vì đó là tính năng có sẵn của HTML:

```html
<!-- Bấm link → trình duyệt gửi GET /login -->
<a th:href="@{/login}">Đăng nhập</a>

<!-- Bấm nút submit → trình duyệt gửi POST /login kèm email, password -->
<form method="post" th:action="@{/login}">
    <input name="email">
    <input name="password">
    <button type="submit">Đăng nhập</button>
</form>
```

Phía Java, method nhận request phải **khớp cả cách gửi lẫn địa chỉ**:

```java
@GetMapping("/login")    public String loginPage(...) { return "login"; }               // mở form
@PostMapping("/login")   public String login(@ModelAttribute LoginRequestDTO r, ...) { ... }  // xử lý form
```

Cùng địa chỉ `/login` nhưng **GET** vào `loginPage()`, **POST** vào `login()`.

**Lấy dữ liệu từ request:**

| Cách | Lấy từ | Ví dụ |
|---|---|---|
| `@PathVariable` | **Trong** đường dẫn | `/department-head/articles/15/review` → `articleId = 15` |
| `@RequestParam` | Sau dấu `?` hoặc 1 ô form | `/news?page=2` → `page = 2` |
| `@ModelAttribute XxxDTO` | Nhiều ô form, đổ vào DTO theo tên | `name="email"` → `loginRequest.setEmail(...)` |

---

## 7. Hai giai đoạn: lúc chạy app và lúc có request

### Giai đoạn 1: lúc chạy app (`./mvnw spring-boot:run`), chỉ **một lần**

```
Spring đọc toàn bộ code, nhìn các nhãn:
  ├─ @Configuration (SecurityConfig)  → dựng "bảo vệ cổng" + tạo PasswordEncoder...
  ├─ Repository, @Service, @Component → tạo bean
  ├─ @Controller                      → tạo bean, CẮM service vào qua constructor
  ├─ @GetMapping / @PostMapping       → lập BẢNG TRA "URL nào → method nào"
  └─ @Transactional                   → đặt "người gác cửa" (proxy) trước service
```

Lúc này annotation được **đọc**, nhưng **code bên trong method chưa chạy dòng nào**.
Giống ghi số vào **danh bạ**; khi có request mới là lúc **gọi điện thật**.

### Giai đoạn 2: mỗi khi có request (lặp lại mỗi lần bấm)

```
Trình duyệt gửi request
 → Filter chain (SecurityConfig): đã đăng nhập chưa? đúng quyền không?
 → DispatcherServlet: tra bảng URL → method nào
 → @ModelAttribute của các @ControllerAdvice (ví dụ SidebarAdvice)
 → Controller → Service → Repository → Database
 → Controller return "tên trang" hoặc "redirect:..."
 → Thymeleaf dựng HTML → gửi về trình duyệt
```

| | Lúc chạy app | Lúc có request |
|---|---|---|
| Annotation | Được đọc, ghi vào bảng | Spring tra bảng |
| Bean | Được tạo, nối với nhau | Dùng lại bean đã có |
| Code trong method | Chưa chạy | Chạy |
| Transaction | Chưa có, chỉ đặt "người gác cửa" | Mở mới mỗi lần gọi method `@Transactional` |

---

## 8. Filter chain, SecurityConfig, session

### Filter chain = bảo vệ ở mọi cửa

Mọi request đều qua filter **trước** khi tới controller. Cấu hình trong `config/SecurityConfig.java`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/register", "/css/**", ...).permitAll()    // ai cũng vào được
    .requestMatchers("/department-head/**").hasRole("DEPARTMENT_HEAD")     // chỉ trưởng phòng
    .requestMatchers("/secretary/**").hasRole("EDITORIAL_SECRETARY")       // chỉ thư ký
    .requestMatchers("/reporter/**").hasRole("REPORTER")                   // chỉ phóng viên
    .anyRequest().authenticated())                                         // còn lại: phải đăng nhập
.formLogin(form -> form.loginPage("/login") ...)    // chưa đăng nhập mà vào trang cần đăng nhập → về /login
```

Luật xét **từ trên xuống**, gặp luật khớp đầu tiên thì dừng. Vì vậy `anyRequest()` phải nằm cuối.

**SecurityConfig làm gì:**
1. Ở **mọi request**: đọc session để biết bạn là ai, kiểm tra URL theo luật trên.
2. Chưa đăng nhập → tự chuyển về `/login`. Sai role → lỗi 403.
3. **Cung cấp công cụ** cho `AuthServiceImpl`: `PasswordEncoder` (BCrypt), `AuthenticationManager`.
4. Đăng xuất, đăng nhập Google, chống CSRF.

### Session + cookie

- Đăng nhập thành công, server **lưu thông tin đăng nhập vào session** và cấp cho trình duyệt cookie `JSESSIONID`.
- Mỗi request sau, trình duyệt **tự gửi kèm** cookie đó; filter dùng nó tìm lại session → biết bạn là ai.
- Giống **số thẻ gửi xe**: server giữ xe, bạn chỉ cầm số thẻ.

### Ba nhân vật lúc đăng nhập làm việc với nhau thế nào

| | Ví dụ tòa nhà | Làm gì | Khi nào |
|---|---|---|---|
| `AuthController` + `AuthServiceImpl` | Quầy cấp thẻ | Kiểm tra mật khẩu, **ghi** session | Một lần |
| `DashboardController` | Người chỉ đường | Xem role, chỉ tới trang riêng | Một lần |
| `SecurityConfig` | Bảo vệ ở mọi cửa | **Đọc** session, chặn nếu sai quyền | **Mọi request** |

Chúng nối với nhau qua **session** (AuthController ghi, SecurityConfig đọc), qua tham số **`Authentication auth`** (Spring lấy từ session đưa vào controller), và qua **redirect**. Controller **không bao giờ gọi nhau trực tiếp**.

### Trang công khai có filter chain riêng

`config/PublicSecurityConfig.java` có chuỗi thứ hai, `@Order(1)` nên được xét **trước**:

```java
@Bean @Order(1)
SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(... GET "/", GET "/news", GET "/news/{id}", GET "/uploads/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())   // ai cũng xem được
        .build();
}
```

Spring dùng **chuỗi đầu tiên có `securityMatcher` khớp**. `/news` khớp chuỗi công khai → cho qua; các URL khác rơi xuống chuỗi của `SecurityConfig`.

---

## 9. DispatcherServlet và @ControllerAdvice

### DispatcherServlet = lễ tân

Là **một class Java do Spring viết sẵn** (thư viện `spring-webmvc`, không nằm trong `src/`). Mọi request đã qua filter đều đi vào nó. Nó:
1. **Tra bảng URL** (lập lúc chạy app từ `@GetMapping` / `@PostMapping`) → ra method nào.
2. Chạy `@ModelAttribute` của các advice.
3. **Gọi method controller**.
4. Nhận kết quả `return`: tên template → nhờ Thymeleaf dựng HTML; `"redirect:..."` → bảo trình duyệt chuyển trang.

Thứ tự "filter → advice → controller → HTML" là do **code của Spring viết sẵn** (trong `DispatcherServlet.doDispatch()`). Từ lúc vào controller trở đi, thứ tự là do **code của bạn**: dòng nào ở trên chạy trước.

### @ControllerAdvice

Method `@ModelAttribute` trong class `@ControllerAdvice` chạy **trước mọi method controller**, nhưng **chỉ khi request tới được controller** (filter không chặn, URL có trong bảng, không phải file tĩnh).

`config/SidebarAdvice.java`: thêm menu bên trái cho mọi trang.

```java
@ControllerAdvice
public class SidebarAdvice {
    @ModelAttribute
    public void addSidebar(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return;   // chưa đăng nhập: thoát sớm
        ...
        model.addAttribute("sidebarGroups", sidebarService.menuFor(role));
        model.addAttribute("activePath", ...);
    }
}
```

- Method `@ExceptionHandler` trong advice thì **chỉ chạy khi có lỗi** (ví dụ `UploadLimitAdvice`).
- Đặt `@ModelAttribute` **trong một controller** thì chỉ chạy cho controller đó (ví dụ `categories()` trong `PublicArticleController`).

---

## 10. Thymeleaf: từ model ra HTML

### Model và `${A.B}`

Controller bỏ dữ liệu vào **model** ("cái túi") với một **tên**. Template đọc bằng `${tên.field}`.

```
${ dashboard . stats }
   └───┬───┘   └─┬─┘
       │         └── tên FIELD trong class DTO (Thymeleaf gọi getStats(), do @Getter sinh ra)
       └──────────── tên CONTROLLER đặt: model.addAttribute("dashboard", ...)
```

Đi trọn một vòng với `displayName`:

```
SERVICE     .displayName(head.getFullName())           → điền "Nguyễn Thu Hà" vào field displayName
              ↓ .build()
CONTROLLER  model.addAttribute("dashboard", dto)       → đặt tên là "dashboard"
              ↓
TEMPLATE    ${dashboard.displayName}                   → gọi dto.getDisplayName()
              ↓
HTML        <b>Nguyễn Thu Hà</b>
```

Đổi tên field trong DTO thì phải sửa **cả 3 chỗ**: DTO, service, template.

### Các lệnh Thymeleaf hay gặp

| Lệnh | Nghĩa | Ví dụ |
|---|---|---|
| `th:text` | Thay chữ mẫu bằng dữ liệu | `<b th:text="${dashboard.displayName}">Tên mẫu</b>` |
| `th:each` | Vòng lặp: in thẻ này nhiều lần | `<article th:each="stat : ${dashboard.stats}">` |
| `th:if` / `th:unless` | Có / không in thẻ này | `th:if="${w.reviewable}"` |
| `th:href`, `th:action` | Tạo URL | `th:action="@{/login}"` (tự chèn CSRF token) |
| `th:replace` | Chèn fragment từ file khác | `th:replace="~{department_head/dashboard :: dhTopbar(...)}"` |
| `th:utext` | In nguyên HTML (chỉ dùng khi đã lọc an toàn) | `th:utext="${b.html}"` |

Chữ viết sẵn trong thẻ (`Tên mẫu`, `12`, `3 HỎA TỐC`...) chỉ là **chữ mẫu**, lúc chạy bị thay bằng dữ liệu thật.

### Layout chung `layout/app.html`

Mọi trang nhân viên dùng **chung một khung** (sidebar, header). Controller cho biết chèn phần ruột nào:

```java
model.addAttribute("contentView", "department_head/dashboard");     // file
model.addAttribute("contentFragment", "departmentHeadContent");     // tên fragment trong file
model.addAttribute("pageCss", List.of(...));                        // CSS riêng
model.addAttribute("pageJs", List.of(...));                         // JS riêng
return "layout/app";                                                // → templates/layout/app.html
```

```html
<!-- layout/app.html -->
<th:block th:replace="~{__${contentView}__ :: __${contentFragment}__}"></th:block>
<!-- thành: ~{department_head/dashboard :: departmentHeadContent} -->
```

`return "layout/app"` → Spring tìm file `templates/` + `layout/app` + `.html`.

---

## 11. Transaction

**Transaction = nhóm thao tác database "làm hết hoặc không làm gì".**

| Từ | Nghĩa |
|---|---|
| Transaction | Một khối thao tác "tất cả hoặc không" |
| Commit | Mọi thứ ổn → **lưu thật** xuống DB |
| Rollback | Có lỗi → **hủy hết**, DB như cũ |

Ví dụ chuyển tiền: trừ tiền A rồi cộng tiền B. Mất điện giữa chừng mà không có transaction thì tiền "bốc hơi".

Trong dự án, nút **"Duyệt"** đổi **2 bảng** cùng lúc
(`service/impl/DepartmentHeadReviewServiceImpl.java`):

```java
@Transactional
public void approve(String email, Integer articleId, String feedback) {
    ...
    closeReview(review, ArticleReviewDecision.APPROVED, note);   // 1. article_review: chốt lượt duyệt
    article.setStatus(ArticleStatus.DEPARTMENT_APPROVED);         // 2. article: đổi trạng thái
}   // ← tới đây mới commit cả 2
```

Không có transaction mà lỗi giữa bước 1 và 2 → lượt duyệt ghi "đã duyệt" nhưng bài vẫn kẹt ở trưởng phòng.

**Bạn không tự viết begin/commit/rollback**, chỉ dán `@Transactional`:

```
LÚC CHẠY APP: Spring đặt "người gác cửa" (proxy) trước service. CHƯA có transaction nào.

MỖI LẦN GỌI approve():
  người gác cửa MỞ transaction → approve() chạy → xong:
     chạy êm    → COMMIT
     ném lỗi ra → ROLLBACK
```

Hai người cùng bấm "Duyệt": dùng **chung một bean**, nhưng **mỗi người một transaction riêng**.

**Hai kiểu trong dự án:**

```java
@Transactional(readOnly = true)   // ở class: mặc định mọi method CHỈ ĐỌC
public class DepartmentHeadReviewServiceImpl {
    @Transactional                // method nào GHI thì thêm nhãn riêng (không readOnly)
    public void approve(...) { ... }
}
```

**Không cần gọi `save`** khi chỉ đổi object lấy từ DB: trong transaction, Hibernate theo dõi object đó và tự `UPDATE` khi commit.

---

## 12. Luồng đăng nhập từ đầu tới dashboard

### Mở form đăng nhập: luôn là `GET /login`

Có 2 cách để tới đó:
- **Cách A**: ở trang chủ bấm link "Đăng nhập" (`templates/public/layout.html`: `<a th:href="@{/login}">`).
- **Cách B**: chưa đăng nhập mà gõ thẳng trang cần đăng nhập → filter tự chuyển về `/login` (nhờ `loginPage("/login")`).

`login.html` gọi là "pop-up" nhưng thật ra là **một trang riêng**, chỉ có CSS làm cho trông giống hộp thoại.

### 4 lượt gửi / trả (trưởng phòng)

| Lượt | Người dùng làm | Trình duyệt gửi | Code chạy | Trả về |
|---|---|---|---|---|
| ① | Bấm link "Đăng nhập" | `GET /login` | `AuthController.loginPage()` | trang `login.html` |
| ② | Điền form, bấm nút | `POST /login` | `AuthController.login()` → `AuthServiceImpl.login()` | `redirect:/dashboard` + cookie |
| ③ | *(tự động)* | `GET /dashboard` | `DashboardController.dashboard()` | `redirect:/department-head/dashboard` |
| ④ | *(tự động)* | `GET /department-head/dashboard` | `DepartmentHeadController.dashboard()` → service → repository | HTML dashboard |

> Lưu ý: form gửi tới **`POST /login`** và do **`AuthController`** xử lý. Dòng `/process-login` trong `SecurityConfig` có cấu hình nhưng form không dùng.
> Đăng nhập **Google** không qua `AuthController` mà qua `GoogleLoginSuccessHandler`, rồi cũng về `/dashboard`.

### ② Xử lý đăng nhập

`controller/AuthController.java`:

```java
@PostMapping("/login")
public String login(@ModelAttribute LoginRequestDTO loginRequest, HttpServletRequest request, RedirectAttributes redirect) {
    try {
        var account = authService.login(loginRequest.getEmail(), loginRequest.getPassword());  // giao service kiểm tra
        if (account.getStatus() == AccountStatus.PENDING) { return "redirect:/register/verify?email=..."; }
        authService.persistSecurityContext(request);     // lưu "đã đăng nhập" vào session
        return "redirect:/dashboard";                    // bảo trình duyệt đi tới /dashboard
    } catch (ApplicationException ex) {
        return "redirect:/login?error=" + ex.getErrorCode().name();   // lỗi → về lại form, hiện lỗi
    }
}
```

`AuthServiceImpl.login()` kiểm tra lần lượt: email có tồn tại không → mật khẩu đúng không (BCrypt) → tài khoản có bị khóa / ngưng / chờ duyệt / chưa xác minh OTP không → nếu ổn thì ghi nhận đã đăng nhập.

### ③ Chỉ đường theo vai trò

`controller/DashboardController.java`:

```java
private static final Map<String, String> DASHBOARD_BY_ROLE = Map.of(   // bảng tra: role → trang
        "ROLE_REPORTER",            "/reporter/topics",
        "ROLE_DEPARTMENT_HEAD",     "/department-head/dashboard",
        "ROLE_EDITORIAL_SECRETARY", "/secretary/final-reviews");

@GetMapping("/dashboard")
public String dashboard(Authentication auth) {
    if (auth != null) {                                                     // có người đăng nhập
        for (GrantedAuthority authority : auth.getAuthorities()) {          // xét từng quyền
            String target = DASHBOARD_BY_ROLE.get(authority.getAuthority()); // tra bảng; không có → null
            if (target != null) return "redirect:" + target;                // tìm thấy → chuyển, THOÁT method
        }
    }
    return "redirect:/";      // đường lui: không tìm thấy (Admin chưa có màn riêng) → trang chủ
}
```

- `getAuthorities()` (có **s**) = cả danh sách quyền; `getAuthority()` = tên một quyền.
- Phải lặp vì một người có thể có nhiều quyền (đăng nhập Google: `OIDC_USER`, `SCOPE_email`, `ROLE_REPORTER`).
- `return` thoát method ngay. Dòng cuối chỉ chạy khi vòng lặp không tìm thấy gì. Java bắt buộc mọi đường đi đều phải `return`.
- `DashboardController` chỉ **chỉ đường**, **không chặn** được ai tự gõ URL. Việc chặn là của `SecurityConfig`.

---

## 13. Màn dashboard Trưởng phòng

### Controller

`controller/DepartmentHeadController.java`:

```java
@GetMapping("/dashboard")   // ghép với @RequestMapping("/department-head") ở đầu class
public String dashboard(Authentication authentication, Model model) {
    // đọc từ trong ra: lấy email → service lấy dữ liệu → bỏ vào túi tên "dashboard"
    model.addAttribute("dashboard", departmentHeadService.getDashboard(currentEmail(authentication)));
    model.addAttribute("pageTitle", "Duyệt bản thảo – Trưởng ban");
    model.addAttribute("contentView", "department_head/dashboard");
    model.addAttribute("contentFragment", "departmentHeadContent");
    model.addAttribute("pageCss", List.of(...));
    model.addAttribute("pageJs", List.of("/department_head/js/department-head-dashboard.js"));
    return "layout/app";
}
```

`authentication` và `model` do **Spring tự điền**. `currentEmail(...)`: đăng nhập mật khẩu thì `getName()` là email; đăng nhập Google thì email nằm trong thuộc tính `"email"`.

### Service: 6 bước, 6 câu SQL

`service/impl/DepartmentHeadServiceImpl.java`, method `getDashboard(email)`:

```
BƯỚC 1  Tìm tài khoản theo email                       → SQL #1 AccountRepository.findByEmailIgnoreCase
BƯỚC 2  Chưa có phòng ban → trả hộp rỗng, DỪNG
BƯỚC 3  Bài chờ duyệt (Topic mình phụ trách)           → SQL #2 ArticleRepository.findByStatusForDepartmentHead
        Đếm nguồn của các bài (1 câu cho mọi bài)      → SQL #3 ArticleRepository.countSourcesByArticleIds
        Đề tài chờ duyệt của người cùng phòng          → SQL #4 TopicRepository.findByStatusAndDepartment
        Đổi tất cả thành thẻ DraftEntryDTO (mapper)
BƯỚC 4  Đánh số thứ tự cho thẻ để JS sắp xếp (assignOrder)
BƯỚC 5  Tính số: articleCount, topicCount, urgentCount (đếm từ danh sách có sẵn)
        approvedToday                                   → SQL #5 ArticleReviewRepository.countDecisionsSince
        revisionCount                                   → SQL #6 ArticleRepository.countByStatusForDepartmentHead
BƯỚC 6  Đóng gói vào DepartmentHeadDashboardDTO, trả về controller
```

Mọi bước chạy **lần lượt** trong một luồng (thread): mỗi câu SQL xong mới sang dòng tiếp. Chỉ những gì code gọi tới mới chạy.

**Vì sao bài và đề tài lọc khác nhau?** Bài thuộc Topic đã `ACTIVE` nên có assignment `DEPARTMENT_HEAD` để biết ai phụ trách. Đề tài đang `DEPARTMENT_REVIEW` **chưa có assignment** (chỉ tạo khi Thư ký approve) nên lọc theo phòng ban người đề xuất.

### 4 hộp thống kê: từ DB ra màn hình

| Hộp | Biến | Cách tính |
|---|---|---|
| CHỜ DUYỆT BÀI VIẾT | `articleCount` | `pendingArticles.size()` |
| ĐÃ DUYỆT HÔM NAY | `approvedToday` | SQL #5: lượt APPROVED cấp DEPARTMENT của mình từ 00:00 hôm nay |
| ĐANG TRẢ VỀ SỬA | `revisionCount` | SQL #6: bài REVISION_REQUIRED thuộc Topic mình phụ trách |
| ĐỀ TÀI CHỜ DUYỆT | `topicCount` | `entries.size() - articleCount` (tổng thẻ trừ số bài) |
| Nhãn "x HỎA TỐC" | `urgentCount` | lọc thẻ bài viết có `urgent = true` rồi đếm |

Câu SQL #5 (`repository/ArticleReviewRepository.java`) viết bằng **JPQL**: dùng **tên class / field Java**, Hibernate dịch sang tên bảng / cột:

```java
@Query("select count(r) from ArticleReview r where r.reviewStage = :stage and r.decision = :decision "
        + "and r.reviewedAt >= :from and r.reviewer.id = :reviewerId")
long countDecisionsSince(@Param("stage") ReviewStage stage, ...);
// :stage, :decision... là chỗ trống; @Param("stage") điền giá trị vào :stage (an toàn, chống SQL injection)
```

Cần đủ 4 điều kiện vì bảng `article_review` chứa lượt duyệt của **mọi người, mọi cấp, mọi ngày**.

Service đóng gói mỗi hộp thành một `DashboardStatDTO` (7 field theo đúng thứ tự):

```java
.stats(List.of(
    new DashboardStatDTO("CHỜ DUYỆT BÀI VIẾT", String.valueOf(articleCount), "bản thảo",
            "Đang chờ Trưởng ban thẩm định", urgentCount > 0 ? urgentCount + " HỎA TỐC" : null, "calendar", "rose"),
    ... 3 hộp nữa ...))
```

Template lặp 4 lần (`templates/department_head/dashboard.html`):

```html
<section class="rp-stats" th:if="${!#lists.isEmpty(dashboard.stats)}">      <!-- rỗng thì ẩn cả khu -->
    <article class="rp-stat" th:each="stat : ${dashboard.stats}">            <!-- 4 phần tử → 4 hộp -->
        <span th:text="${stat.label}">CHỜ DUYỆT BÀI VIẾT</span>
        <th:block th:text="${stat.value}">12</th:block>
        <span th:if="${stat.badge != null}" th:text="${stat.badge}">3 HỎA TỐC</span>
    </article>
</section>
```

### JavaScript của dashboard

`static/department_head/js/department-head-dashboard.js` **chỉ lọc, tìm kiếm, sắp xếp** các thẻ đã có sẵn trên trình duyệt, **không gửi gì về server**.

HTML không "gọi" JS; **JS đi tìm HTML** theo thuộc tính:

```js
const tabButtons = Array.from(root.querySelectorAll('[data-dh-tab]'));   // tìm 2 nút tab
tabButtons.forEach(button => button.addEventListener('click', () => {
  state.tab = button.dataset.dhTab;    // đọc data-dh-tab="article" hoặc "topic"
  update();                            // ẩn / hiện thẻ có data-kind khớp
}));
```

Con số trên tab (`th:text="${dashboard.articleCount}"`) do **Thymeleaf in trên server**, JS không đụng tới.

---

## 14. Màn thẩm định bài viết (workspace review)

### 6 URL

| URL | Method controller | Gọi service | Nút trên giao diện |
|---|---|---|---|
| `GET .../articles/{id}/review` | `reviewWorkspace()` | `openWorkspace()` | Bấm tiêu đề bài trên dashboard |
| `POST .../articles/{id}/comments` | `addComment()` | `addComment()` | Bôi đen + "Thêm góp ý", hoặc "Thêm góp ý chung" |
| `POST .../comments/{c}/delete` | `deleteComment()` | `deleteComment()` | "Xóa" trên thẻ góp ý |
| `POST .../comments/{c}/resolve` | `resolveComment()` | `setPreviousCommentResolved()` | "Đã sửa" / "Chưa sửa" (vòng trước) |
| `POST .../articles/{id}/approve` | `approve()` | `approve()` | "Xác nhận duyệt" |
| `POST .../articles/{id}/request-revision` | `requestRevision()` | `requestRevision()` | "Gửi trả bài" |

### Kiểu "POST rồi redirect về GET"

```
Bấm nút trong <form method="post">
 → method POST trong controller → service ghi DB
 → runAndReturn() trả "redirect:/department-head/articles/{id}/review" + thông báo (toast)
 → trình duyệt tự GET lại trang → reviewWorkspace() chạy lại → dữ liệu MỚI
```

Nhờ redirect, bấm F5 sau khi gửi form sẽ không gửi lại lần nữa.

`runAndReturn` gom chung try/catch cho cả 5 nút:

```java
private static String runAndReturn(Integer articleId, RedirectAttributes flash, String successMessage, Runnable action) {
    try {
        action.run();                                        // chạy đoạn code gọi service (lambda truyền vào)
        flash.addFlashAttribute("toast", successMessage);    // thành công → toast xanh
    } catch (ApplicationException ex) {
        flash.addFlashAttribute("toastError", ex.getMessage());   // lỗi → toast đỏ
    }
    return "redirect:/department-head/articles/" + articleId + "/review";
}
```

### Bốn bảng liên quan

| Bảng | Ý nghĩa |
|---|---|
| `article` | Bài viết, có `status` |
| `article_version` | Mỗi lần Lead nộp là 1 version (1.0, 1.1...). **Không được sửa / xóa** |
| `article_review` | Một **lượt duyệt** của một cấp trên **một version**. `decision = NULL` = đang duyệt |
| `comment` | Góp ý, thuộc một lượt duyệt, có thể gắn vị trí trong bài |

### Trạng thái bài đổi khi nào

```
① Lead bấm Submit (ArticleServiceImpl.submitVersion)
   → tạo article_version → article.status = DEPARTMENT_REVIEW      ← trạng thái đổi Ở ĐÂY

② Trưởng phòng mở bài (openWorkspace)
   → bài đang DEPARTMENT_REVIEW? có → tìm lượt duyệt đang mở, chưa có thì TẠO
   → article_review: stage = DEPARTMENT, decision = NULL
   → article.status KHÔNG đổi

③ Bấm Duyệt / Gửi trả bài
   → article_review: decision = APPROVED / REQUEST_REVISION, reviewed_at = giờ bấm
   → article.status = DEPARTMENT_APPROVED / REVISION_REQUIRED      ← đổi lần nữa
```

**Mở bài nhưng chưa quyết định gì?** Lượt duyệt cứ ở `decision = NULL`, bài vẫn `DEPARTMENT_REVIEW`. Mở lại thì **dùng lại** lượt cũ (không tạo thêm), comment đã viết vẫn còn. Không có hạn giờ, không tự đóng.

> ⚠️ Rủi ro về sau: lượt duyệt ghi tên người mở. Nếu sau này làm chức năng "giao Topic cho trưởng phòng khác" mà lượt cũ còn mở, trưởng phòng mới sẽ bị `ARTICLE_REVIEW_TAKEN`. Hiện chưa xảy ra vì chức năng đó chưa làm; khi làm cần hỏi nhóm cách xử lý.

### `openWorkspace` 5 bước

```
BƯỚC 1  Kiểm tra quyền: currentHead (tìm tài khoản) + articleOfHead
        (phải là DEPARTMENT_HEAD của Topic, và không phải Lead của bài) → sai: 403
BƯỚC 2  Lấy mọi version; current = mới nhất, previous = liền trước (null nếu mới nộp lần đầu)
BƯỚC 3  Bài DEPARTMENT_REVIEW → tìm / tạo lượt duyệt; reviewable = lượt đó là của mình
BƯỚC 4  Đổi version thành VersionViewDTO, lấy tư liệu (SourceItemDTO), ảnh đại diện, tên Supporting
BƯỚC 5  Đóng gói ReviewWorkspaceDTO
```

`reviewable = false` → mọi khối `th:if="${w.reviewable}"` bị ẩn (khung góp ý, nút Duyệt...), trang thành **chỉ xem**.

### Các DTO của màn này

```
ReviewWorkspaceDTO (cả trang, HTML gọi tắt là w)
 ├─ current / previous / versions : VersionViewDTO (một phiên bản)
 │     └─ blocks : ContentBlockDTO (đoạn chữ / ảnh / video / bảng)
 ├─ sources / featuredImage : SourceItemDTO (tư liệu)
 └─ comments / previousComments : ReviewCommentDTO (góp ý, có replies)

CommentRequestDTO : dữ liệu form "Thêm góp ý" GỬI LÊN (chiều ngược lại)
```

### Góp ý gắn vị trí hoạt động thế nào

```
1. Bôi đen chữ trong phần tử có data-field (tít, sapo, đoạn thân bài)
2. JS đọc data-field + data-paragraph, đếm vị trí ký tự bắt đầu / kết thúc
3. JS hiện khung #comment-popover
4. Gõ góp ý, bấm "Thêm góp ý" → JS điền vào form ẩn #comment-form rồi gửi POST
5. addComment(): Spring đổ form vào CommentRequestDTO theo tên ô
6. service.addComment(): KIỂM TRA LẠI vị trí, cắt quotedText từ nội dung trong DB (không tin chữ gửi lên), lưu
```

Mọi thao tác ghi đều gọi `myOpenReview()` trước: bài phải `DEPARTMENT_REVIEW` và lượt duyệt đang mở phải là của mình. **Kiểm tra quyền ở backend**, nên tự gõ URL hay tự gửi form cũng bị chặn.

---

## 15. Trang công khai /news

`GET /news?q=bao&category=Kinh tế&page=2`:

```
① Filter: PublicSecurityConfig (@Order 1) khớp → permitAll (vẫn đọc session nếu có)
② DispatcherServlet → PublicArticleController.list()
③ Advice chung + @ModelAttribute riêng của controller: currentUser(), categories() → SQL #1
④ list(): @RequestParam đọc q, category, page từ URL
⑤ PublicArticleServiceImpl.search(): dọn từ khóa (blankToNull, escapeLike), PageRequest(page-1, 9, mới nhất trước)
⑥ PublicArticleRepository.search() → SQL #2 (9 bài) + SQL #3 (đếm tổng, để làm phân trang)
⑦ Đọc từ VIEW v_public_article (@Immutable, chỉ bài PUBLISHED, đúng version đã duyệt)
⑧ Thymeleaf: public/article-list.html + mảnh từ public/layout.html
```

Khác dashboard: không kiểm tra quyền, không có DTO / mapper (entity của view đã đủ), lọc và phân trang **do server làm** qua URL.

---

## 16. Bảng tra nhanh: file nào làm gì

| File | Vai trò | Chạy khi nào |
|---|---|---|
| `config/SecurityConfig.java` | Bảo vệ cổng: ai vào URL nào; công cụ mật khẩu | Mọi request |
| `config/PublicSecurityConfig.java` | Cổng riêng cho trang công khai | `/`, `/news`... |
| `config/SidebarAdvice.java` | Thêm menu trái | Trước mọi controller |
| `controller/AuthController.java` | Mở form (GET) + xử lý đăng nhập (POST) | Đăng nhập |
| `service/impl/AuthServiceImpl.java` | Kiểm tra email, mật khẩu, trạng thái | Đăng nhập |
| `controller/DashboardController.java` | Chỉ đường theo vai trò | Ngay sau đăng nhập |
| `controller/DepartmentHeadController.java` | Dashboard + màn thẩm định | Trưởng phòng dùng |
| `service/impl/DepartmentHeadServiceImpl.java` | Dữ liệu dashboard | Mở dashboard |
| `service/impl/DepartmentHeadReviewServiceImpl.java` | Mở bài, góp ý, duyệt, trả bài | Màn thẩm định |
| `repository/*Repository.java` | Chạy SQL | Khi service cần dữ liệu |
| `dto/DepartmentHead/*.java` | Hộp dữ liệu | Service tạo, template đọc |
| `templates/layout/app.html` | Khung chung (sidebar, header) | Mọi trang nhân viên |
| `templates/department_head/dashboard.html` | Ruột dashboard | Mở dashboard |
| `templates/department_head/workspacereview.html` | Ruột màn thẩm định | Mở bài |
| `static/department_head/js/*.js` | Lọc / sắp xếp / bôi đen góp ý trên trình duyệt | Sau khi trang hiện ra |

**Thứ tự đọc code một màn bất kỳ:**
URL → controller có `@GetMapping` khớp → service mà controller gọi → repository mà service gọi → template mà controller `return`.

---

## 17. Câu thầy hay hỏi + trả lời ngắn

| Câu hỏi | Trả lời |
|---|---|
| Ai gọi controller? | **Spring** (DispatcherServlet), sau khi trình duyệt gửi request và filter cho qua |
| Spring biết gọi controller nào bằng cách nào? | Lúc chạy app, Spring đọc `@GetMapping` / `@PostMapping` và lập **bảng tra URL → method** |
| Controller gọi service kiểu gì? | Như gọi method bình thường; service là **bean** Spring đưa vào qua constructor |
| Sao đăng nhập xong tự vào dashboard? | Controller trả `redirect:`, trình duyệt **tự gửi request mới** (2 lần: `/dashboard` rồi trang của role) |
| Phần nào gửi lên, phần nào trả về? | Trình duyệt gửi qua `<form>` / link; controller trả qua `return` |
| Bean là gì? | Object Spring **tự tạo** lúc chạy app từ class có nhãn (`@Service`...), dùng chung, tự cắm vào nhau |
| Transaction là gì? | Nhóm thao tác DB "làm hết hoặc không gì cả"; `@Transactional` tự commit / rollback |
| DTO là gì? | Hộp dữ liệu: service tạo và điền, controller chuyển, template đọc |
| `${dashboard.stats}` lấy từ đâu? | `dashboard` = tên trong `model.addAttribute`; `stats` = field trong DTO, đọc bằng `getStats()` |
| Kiểm tra quyền ở đâu? | Ở **filter** (`SecurityConfig`) và ở **service**, không chỉ ẩn nút trên giao diện |
| SecurityConfig làm gì khi AuthController đã lo đăng nhập? | AuthController **cấp thẻ** một lần; SecurityConfig **kiểm tra thẻ** ở mọi request |

---

## 18. Tự kiểm chứng bằng mắt

1. **F12 → tab Network**, rồi đăng nhập. Sẽ thấy đúng 4 lượt: `login` (POST, 302), `dashboard` (302), `dashboard` của `/department-head/` (200), rồi các file CSS / JS. (302 = chuyển hướng, 200 = thành công.)

2. **Bật log**: thêm vào `src/main/resources/application.properties` (file bị gitignore, chỉ ảnh hưởng máy bạn):
   ```properties
   logging.level.org.springframework.web=DEBUG
   ```
   Console in ra mỗi request đi vào controller nào. Xem xong thì xóa dòng này.

3. **Breakpoint**: đặt chấm đỏ ở dòng đầu `AuthController.login()`, `DashboardController.dashboard()`, `DepartmentHeadController.dashboard()`. Chạy Debug, đăng nhập: code dừng lần lượt theo thứ tự. Nhìn khung **Call Stack** sẽ thấy `Tomcat → FilterChainProxy → DispatcherServlet → controller`.

4. **Đọc comment trong code**: các file controller, service, DTO, template của dashboard và màn thẩm định đã có chú thích tiếng Việt chi tiết, ghi rõ gọi tới file nào, method nào.
