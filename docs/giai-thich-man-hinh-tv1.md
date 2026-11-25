# Giải thích từng màn hình – Phần TV1 (FL-01 + Tài khoản)

Mục đích: với **mỗi màn hình**, chỉ ra **mỗi vùng trên màn hình** do dòng nào ở **front-end** (template Thymeleaf / JS) vẽ ra, và **back-end** (controller → service → repository → DB) đã làm gì để có dữ liệu đó.

- Link dạng `Tên file:dòng` → **Ctrl+click** trong VS Code (hoặc Ctrl+Shift+V mở Preview rồi bấm) để nhảy tới đúng dòng.
- Số dòng đúng với code ở commit `af4a540`. Code đổi thì tìm theo **tên method / tên fragment**.
- File này bổ sung cho [lan-theo-code-tv1.md](lan-theo-code-tv1.md) (lần theo từng **nút bấm**). File này đi theo từng **màn hình**.

---

## 0. Ý nền: một màn hình được "vẽ" ra như thế nào

Dự án dùng **Thymeleaf render phía server**. Nghĩa là: **server tạo sẵn toàn bộ HTML rồi mới gửi cho trình duyệt**. Trình duyệt không tự đi lấy dữ liệu.

```
Trình duyệt gõ / bấm link  GET /department-head/topics?topic=13
        │
        ▼
SecurityConfig: URL này có đúng role không?  (sai role → 403)
        │
        ▼
Controller: đọc tham số trên URL (topic=13), lấy email người đăng nhập
        │  gọi service
        ▼
Service: kiểm tra quyền, gọi Repository lấy dữ liệu từ DB,
         gói lại thành 1 DTO (DTO = "hộp" chỉ chứa đúng dữ liệu màn hình cần)
        │
        ▼
Controller: model.addAttribute("ws", dto)  → return "layout/app"
        │
        ▼
Thymeleaf: lấy layout/app.html, chèn template nội dung vào giữa,
           thay mọi th:text="${ws....}" bằng dữ liệu thật  → ra HTML hoàn chỉnh
        │
        ▼
Trình duyệt nhận HTML và hiển thị. JS nhỏ chỉ lo: mở/đóng hộp thoại, tìm nhanh.
```

**Layout chung** cho mọi màn: [app.html:31](../src/main/resources/templates/layout/app.html#L31)
`th:replace="~{__${contentView}__ :: __${contentFragment}__}"` → controller nói "chèn fragment nào của file nào" qua 2 biến `contentView`, `contentFragment`. Sidebar bên trái do [SidebarAdvice](../src/main/java/com/group6/electronic_drafting_system/config/SidebarAdvice.java) tự thêm vào mọi trang, mục menu nằm ở [SidebarService.java:18](../src/main/java/com/group6/electronic_drafting_system/service/SidebarService.java#L18).

**Câu nói ngắn khi thầy hỏi chung:** *"Màn hình của em là server-side render: controller lấy dữ liệu qua service, đưa vào model, Thymeleaf điền dữ liệu vào template rồi gửi HTML hoàn chỉnh về trình duyệt. JS chỉ dùng cho hộp thoại và tìm nhanh."*

---

## Danh sách màn hình phần của em

| # | Màn hình | URL | Role | Template |
|---|---|---|---|---|
| 1 | **Trưởng ban duyệt đề tài** (ảnh thầy hỏi) | `GET /department-head/topics` | DEPARTMENT_HEAD | `department_head/topic-review.html` |
| 2 | Thư ký duyệt đề tài cuối | `GET /secretary/topic-reviews` | EDITORIAL_SECRETARY | `secretary/topic-final-review.html` |
| 3 | Danh sách đề tài của phóng viên | `GET /reporter/topics` | REPORTER | `reporter/topic-proposal-list.html` |
| 4 | Đề xuất đề tài / Sửa và gửi lại (cùng 1 form) | `GET /reporter/topics/new`, `/reporter/topics/{id}/edit` | REPORTER | `reporter/topic-proposal-create.html` |
| 5 | Xem góp ý đề tài | `GET /reporter/topics/{id}/feedback` | REPORTER | `reporter/topic-feedback.html` |
| 6 | Trang Login + các pop-up (Đăng ký, OTP, Chờ duyệt, Quên / Đặt lại mật khẩu) | `GET /login` | ai cũng vào | `login.html` |

Màn 1 và 2 dùng **chung** các mảnh giao diện (fragment) trong [fragment/topic-review.html](../src/main/resources/templates/fragment/topic-review.html) và **chung** service [TopicReviewServiceImpl](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java). Hiểu kỹ màn 1 là hiểu 80% màn 2.

---

## 1. Màn "Duyệt đề tài" của Trưởng ban (ảnh thầy hỏi)

### 1.1. Bản đồ: vùng nào trên màn hình ↔ code nào

```
┌─ Header: "Hệ Thống Tòa Soạn Số" ............................. Đặng Quang Vinh / TRƯỞNG BAN · PHÒNG KINH TẾ ─┐  ← (A) tpTopbar
├─ 3 thẻ thống kê (bị cắt ở mép trên ảnh): Đang chờ / Gửi lần đầu / Đã sửa gửi lại ──────────────────────────┤  ← (B) stats
├─ Hàng chờ  [3 đề tài]   Gửi sớm nhất trước ─┐ ┌─ Chi tiết đề tài ─────────────────────────────────────────┤
│  TP-2026-0013  Phòng Kinh tế                │ │ TP-2026-0013  ● CHỜ TRƯỞNG BAN DUYỆT                      │  ← (D) detail
│  Giá nhà tăng hơn so với thị trường  ◄đang chọn│ │ Giá nhà tăng hơn so với thị trường                        │
│  Ngô Thùy Linh · 28/10/2026 13:19           │ │ Giá nhà tăng cao                                           │
│  ĐÃ SỬA 2 LẦN                               │ │ BAN | PHÓNG VIÊN ĐỀ XUẤT | GỬI LÚC | CẬP NHẬT | SỐ VÒNG SỬA│
│  TP-2026-0020 ... Kinh tế tăng              │ │ [Duyệt đề tài] [Yêu cầu sửa]                               │  ← (E) nút
│  TP-2026-0021 ... Giá vàng tăng             │ └────────────────────────────────────────────────────────────┤
│                                  (C) queue  │ ┌─ Lịch sử góp ý và duyệt [2 mục] ───────────────────────────┤  ← (F) history
└─────────────────────────────────────────────┘ │ Trưởng ban yêu cầu sửa · Đặng Quang Vinh · 31/10/2026 11:22│
                                                │ Trưởng ban yêu cầu sửa · Đặng Quang Vinh · 12/11/2026 10:34│
                                                └────────────────────────────────────────────────────────────┘
```

| Vùng | Front-end (vẽ ở đâu) | Lấy dữ liệu từ đâu (back-end → DB) |
|---|---|---|
| (A) Header tên + chức danh | [topic-proposal-list.html:30](../src/main/resources/templates/reporter/topic-proposal-list.html#L30) fragment `tpTopbar`, dòng 56–60 in `initials`, `fullName`, `roleLabel` | [TopicReviewServiceImpl.java:83](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L83): `head.getFullName()`, `"Trưởng ban · " + tên ban` (bảng `account` + `department`) |
| (B) 3 thẻ thống kê | [fragment/topic-review.html:7](../src/main/resources/templates/fragment/topic-review.html#L7) fragment `stats` | Không gọi DB thêm: đếm ngay trên list `ws.queue()` (dòng 11: tổng; dòng 16: `revisionRounds() == 0`; dòng 21: `> 0`) |
| (C) Hàng chờ bên trái | [fragment/topic-review.html:28](../src/main/resources/templates/fragment/topic-review.html#L28) fragment `queue`, vòng lặp `th:each` dòng 39 | `ws.queue()` ← [TopicReviewServiceImpl.java:71](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L71) (bảng `topic`) |
| (D) Chi tiết đề tài bên phải | [fragment/topic-review.html:61](../src/main/resources/templates/fragment/topic-review.html#L61) fragment `detail` | `ws.selected()` ← [TopicReviewServiceImpl.java:75](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L75) `pickSelected(...)` |
| (E) Nút Duyệt / Yêu cầu sửa | [department_head/topic-review.html:48](../src/main/resources/templates/department_head/topic-review.html#L48) `th:if="${ws.canDecide()}"` | `canDecide` ← [TopicReviewServiceImpl.java:80](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L80) |
| (F) Lịch sử góp ý và duyệt | [fragment/topic-review.html:79](../src/main/resources/templates/fragment/topic-review.html#L79) fragment `history` | `ws.history()` ← [TopicReviewServiceImpl.java:255](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L255) (bảng `topic_feedback` + `topic_review`) |

Tất cả dữ liệu trên nằm trong **một hộp** `ws` kiểu [TopicReviewWorkspaceDTO.java:10](../src/main/java/com/group6/electronic_drafting_system/dto/Topic/TopicReviewWorkspaceDTO.java#L10):
`queue` (hàng chờ), `selected` (đề tài đang mở), `history` (lịch sử), `canDecide` (có hiện nút không), `fullName / roleLabel / initials` (header).

---

### 1.2. ⭐ Câu thầy hỏi: "Bấm đề tài 1 thì bên phải hiện đề tài 1, bấm đề tài 2 thì bên phải hiện đề tài 2 — tại sao?"

**Trả lời một câu:** *"Mỗi đề tài bên trái là một đường link có gắn id đề tài trên URL (`?topic=13`). Bấm vào thì trình duyệt gửi request mới lên server; controller đọc id đó, service lấy đúng đề tài có id đó trong DB, rồi Thymeleaf vẽ lại cả trang với đề tài đó ở cột phải. Không phải JS ẩn/hiện, mà là tải lại trang với tham số khác."*

Chỉ code theo đúng 5 bước:

**Bước 1 — Mỗi dòng hàng chờ là một thẻ `<a>` có `?topic=<id>`**
[fragment/topic-review.html:39-41](../src/main/resources/templates/fragment/topic-review.html#L39-L41)

```html
<li th:each="t : ${ws.queue()}" ...>                                  <!-- lặp qua từng đề tài trong hàng chờ -->
  <a th:href="... @{${baseUrl}(topic=${t.id()})} ..."                 <!-- link = /department-head/topics?topic=<id> -->
     th:classappend="${ws.selected() != null and ws.selected().id() == t.id()} ? 'is-active'">  <!-- tô hồng dòng đang chọn -->
```

- `th:each` = vòng lặp: mỗi đề tài trong `ws.queue()` sinh ra một `<li>`.
- `@{${baseUrl}(topic=${t.id()})}` → Thymeleaf sinh ra `href="/department-head/topics?topic=13"`. `baseUrl` được truyền vào từ [department_head/topic-review.html:35](../src/main/resources/templates/department_head/topic-review.html#L35) là `'/department-head/topics'`.
- Vì vậy sau khi render, HTML gửi về trình duyệt trông như sau (bấm **Ctrl+U** trên trình duyệt để cho thầy xem):
  ```html
  <a href="/department-head/topics?topic=13" class="is-active"> ... Giá nhà tăng hơn so với thị trường ...</a>
  <a href="/department-head/topics?topic=20"> ... Kinh tế tăng ...</a>
  <a href="/department-head/topics?topic=21"> ... Giá vàng tăng ...</a>
  ```
- Id `13` cũng chính là đuôi mã `TP-2026-0013`: mã này **không lưu trong DB**, được ghép từ năm tạo + id ở [TopicMapper.java:89-92](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L89-L92) (`"TP-%d-%04d"`).

> **Mẹo demo:** bấm đề tài thứ 2 và chỉ lên **thanh địa chỉ**: URL đổi từ `?topic=13` thành `?topic=20`. Đó là bằng chứng rõ nhất.

**Bước 2 — Controller đọc `topic` trên URL**
[DepartmentHeadTopicController.java:34-36](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L34-L36)

```java
@GetMapping
public String workspace(@RequestParam(required = false) Integer topic, Principal principal, Model model) {
    model.addAttribute("ws", topicReviewService.getHeadWorkspace(currentEmail(principal), topic));
```

- `@RequestParam Integer topic` = Spring tự lấy giá trị `?topic=20` trên URL bỏ vào biến `topic` (= 20).
- `required = false`: lần đầu mở menu "Duyệt đề tài" thì URL **không có** `?topic=` → `topic = null`.
- `principal` = người đang đăng nhập; `currentEmail(...)` ([dòng 81](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L81)) lấy email (đăng nhập Google thì đọc thuộc tính `email`).
- Truyền cả hai xuống service. **Ctrl+click** `getHeadWorkspace` → vào interface → **Ctrl+F12** sang impl.

**Bước 3 — Service chọn đề tài để hiện bên phải**
[TopicReviewServiceImpl.java:75](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L75) gọi `pickSelected(selectedTopicId, queue)` → nhảy tới [dòng 244-248](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L244-L248):

```java
private Topic pickSelected(Integer selectedTopicId, List<TopicReviewItemDTO> queue) {
    // Có ?topic= trên URL thì lấy id đó; không có thì lấy đề tài ĐẦU hàng chờ
    Integer id = selectedTopicId != null ? selectedTopicId : (queue.isEmpty() ? null : queue.get(0).id());
    if (id == null) return null;                       // hàng chờ trống → bên phải hiện "Chọn một đề tài..."
    return topics.findWithPeopleById(id).orElseThrow(...);   // SELECT topic theo id (kèm department, creator)
}
```

- Đây là lý do **lần đầu vào trang** (chưa bấm gì) bên phải đã hiện đề tài **đầu tiên**: `selectedTopicId == null` → lấy `queue.get(0)`.
- Bấm đề tài thứ 2 → `selectedTopicId = 20` → lấy đề tài id 20.
- `findWithPeopleById` ([TopicRepository.java:41-42](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L41-L42)): Spring Data tự sinh SQL từ tên method; `@EntityGraph` nạp luôn ban, người tạo, người duyệt trong cùng một câu SQL.

**Bước 4 — Kiểm tra quyền rồi gói vào DTO**
[TopicReviewServiceImpl.java:77-84](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L77-L84)

- Dòng 77–79: nếu đề tài **không thuộc ban của Trưởng ban** (ví dụ gõ tay `?topic=5` của ban khác) → ném `TOPIC_VIEW_FORBIDDEN` → 403. *"Kiểm tra quyền ở backend, không tin URL."*
- Dòng 80: `canDecide` = đề tài còn `DEPARTMENT_REVIEW` và **không** đang chờ phóng viên sửa ([dòng 270](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L270)).
- Dòng 81–84: `new TopicReviewWorkspaceDTO(..., queue, toItem(selected), historyOf(selected), canDecide, ...)`. Lịch sử cũng lấy **theo đề tài được chọn**, nên đổi đề tài thì lịch sử bên dưới cũng đổi theo.

**Bước 5 — Thymeleaf vẽ cột phải từ `ws.selected()`**
- [department_head/topic-review.html:40-45](../src/main/resources/templates/department_head/topic-review.html#L40-L45): `selected == null` → hiện ô trống; khác null → chèn fragment `detail`.
- [fragment/topic-review.html:61-75](../src/main/resources/templates/fragment/topic-review.html#L61-L75): `th:text="${ws.selected().title()}"`, `...summary()`, `...reporterName()`... → điền đúng đề tài vừa chọn.
- Cùng lúc đó, ở cột trái, điều kiện `ws.selected().id() == t.id()` ở [dòng 41](../src/main/resources/templates/fragment/topic-review.html#L41) đúng với dòng id 20 → dòng đó có class `is-active` (nền hồng, viền đỏ). Vì vậy dòng được tô màu luôn khớp với chi tiết bên phải.

**Nếu thầy hỏi tiếp:**
- *"Sao không dùng JavaScript cho nhanh?"* → "Thymeleaf render phía server nên em dùng link thường cho đơn giản, mỗi lần bấm đều đọc dữ liệu mới nhất trong DB và kiểm tra quyền lại. Ngoài ra URL có `?topic=` nên F5 hay gửi link vẫn mở đúng đề tài."
- *"Gõ tay `?topic=` của ban khác thì sao?"* → chỉ [dòng 77](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L77): trả 403.
- *"Gõ `?topic=99999` không tồn tại?"* → [dòng 247](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L247): `TOPIC_NOT_FOUND` → 404 (controller trả lỗi ở [dòng 87-90](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L87-L90)).
- *"Ô tìm kiếm có gửi lên server không?"* → Không. [fragment/topic-review.html:116-130](../src/main/resources/templates/fragment/topic-review.html#L116-L130): JS chỉ **ẩn** những `<li>` không chứa từ khóa, dựa vào thuộc tính `data-tp-search-text` được server ghi sẵn ở [dòng 39](../src/main/resources/templates/fragment/topic-review.html#L39).

---

### 1.3. Hàng chờ bên trái lấy từ đâu, sắp xếp thế nào?

[TopicReviewServiceImpl.java:66-73](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L66-L73)

1. `requireRole(email, DEPARTMENT_HEAD)` ([dòng 235](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L235)): người đăng nhập phải là Trưởng ban, đang `ACTIVE`, đã có ban. Không thì 403.
2. Lấy `departmentId` của Trưởng ban.
3. Gọi [TopicRepository.java:35](../src/main/java/com/group6/electronic_drafting_system/repository/TopicRepository.java#L35)
   `findByStatusAndDepartment_IdAndAwaitingRevisionFalseOrderByCreatedAtAscIdAsc(DEPARTMENT_REVIEW, departmentId)`
   Spring Data đọc tên method và tự sinh SQL tương đương:
   ```sql
   SELECT * FROM topic
   WHERE status = 'DEPARTMENT_REVIEW'      -- đang chờ duyệt cấp ban
     AND department_id = @departmentId     -- đúng ban của mình
     AND awaiting_revision = 0             -- không đang chờ phóng viên sửa
   ORDER BY created_at ASC, topic_id ASC;  -- gửi sớm nhất lên trước ("Gửi sớm nhất trước")
   ```
4. Mỗi `Topic` được đổi thành `TopicReviewItemDTO` qua `toItem` ([dòng 250-253](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L250-L253)) → [TopicMapper.toReviewItem](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L36).

Từng chữ trên một dòng hàng chờ:

| Trên màn hình | Template | Nguồn dữ liệu |
|---|---|---|
| `TP-2026-0013` | dòng 43 `t.code()` | Ghép từ năm `created_at` + `topic_id` ([TopicMapper:89](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L89)) |
| `Phòng Kinh tế` | dòng 44 `t.departmentName()` | `department.department_name` |
| `Giá nhà tăng hơn so với thị trường` | dòng 46 `t.title()` | `topic.title` |
| `Ngô Thùy Linh · 28/10/2026 13:19` | dòng 47 `reporterName()` + `#temporals.format(createdAt)` | `account.full_name` của người tạo + `topic.created_at` |
| `ĐÃ SỬA 2 LẦN` | dòng 48-49, chỉ hiện khi `revisionRounds() > 0` | Đếm dòng `topic_feedback` loại `CHANGE_REQUEST` của đề tài ([TopicFeedbackRepository.countByTopic_IdAndType](../src/main/java/com/group6/electronic_drafting_system/repository/TopicFeedbackRepository.java#L15)) |
| Badge `3 đề tài` | dòng 31 `#lists.size(ws.queue())` | Đếm số phần tử list |

---

### 1.4. Cột phải "Chi tiết đề tài"

[fragment/topic-review.html:61-75](../src/main/resources/templates/fragment/topic-review.html#L61-L75), dữ liệu = `ws.selected()` (cũng là `TopicReviewItemDTO`, cùng hàm `toItem`):

| Trên màn hình | Dòng | Nguồn |
|---|---|---|
| Badge `● CHỜ TRƯỞNG BAN DUYỆT` | 64 `statusLabel()`, màu theo `statusKey()` | `topic.status` đổi sang chữ tiếng Việt ở `TopicListItemDTO.labelOf` |
| Tiêu đề lớn | 67 `title()` | `topic.title` |
| `Giá nhà tăng cao` | 68 `summary()` | `topic.description` |
| BAN / PHÓNG VIÊN ĐỀ XUẤT / GỬI LÚC | 70-72 | `department_name`, `full_name`, `created_at` |
| CẬP NHẬT | 73, không có thì `—` | `topic.updated_at` (đổi khi phóng viên gửi lại) |
| SỐ VÒNG SỬA `2` | 74 `revisionRounds()` | như "Đã sửa 2 lần" |

**Hai nút Duyệt / Yêu cầu sửa** — [department_head/topic-review.html:48-58](../src/main/resources/templates/department_head/topic-review.html#L48-L58):
- `th:if="${ws.canDecide()}"` → nút **chỉ được server đưa vào HTML** khi đề tài còn chờ Trưởng ban. Ngược lại hiện câu "Đề tài đang chờ phóng viên sửa...".
- Bấm nút **không gửi gì lên server**, chỉ mở hộp thoại: `data-open="approve-dialog"` → JS ở [fragment/topic-review.html:107-108](../src/main/resources/templates/fragment/topic-review.html#L107-L108) gọi `showModal()`.
- Hộp thoại chứa `<form method="post">`:
  - Duyệt: [dòng 72](../src/main/resources/templates/department_head/topic-review.html#L72) → `POST /department-head/topics/13/approve` → [controller dòng 45](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L45) → [headApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L89): khóa dòng topic, kiểm tra lại, ghi `topic_review` (DEPARTMENT, APPROVED), đổi status → `SECRETARY_REVIEW`.
  - Yêu cầu sửa: [dòng 86](../src/main/resources/templates/department_head/topic-review.html#L86) → `POST .../request-changes` → [headRequestChanges](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L108): góp ý bắt buộc, ghi 1 dòng `topic_feedback`, bật `awaiting_revision = 1`, gửi thông báo cho phóng viên.
  - Xong thì `redirect:/department-head/topics` → trang tải lại, đề tài đã biến khỏi hàng chờ (vì không còn thỏa điều kiện SQL ở 1.3).
- Ẩn nút chỉ là giao diện. **Backend vẫn kiểm tra lại** ở `lockForHead` ([dòng 215-223](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L215-L223)): đúng ban, không duyệt đề tài của chính mình (BR-34), còn đang chờ duyệt.
- Chi tiết từng bước bấm nút: xem [lan-theo-code-tv1.md mục 14–15](lan-theo-code-tv1.md#14-trưởng-ban-bấm-duyệt-dòng-17).

---

### 1.5. "Lịch sử góp ý và duyệt"

- Front-end: [fragment/topic-review.html:79-100](../src/main/resources/templates/fragment/topic-review.html#L79-L100). Mỗi `h` trong `ws.history()` là một ô; `tone-revision / approved / rejected` quyết định màu viền trái.
- Back-end: [historyOf](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L255-L259) lấy **2 bảng** của **đề tài đang chọn**:
  - `topic_feedback` (các lần "Trưởng ban yêu cầu sửa", có thể nhiều lần),
  - `topic_review` (kết quả duyệt từng cấp: "Trưởng ban đã duyệt", "Thư ký từ chối"...).
- Gộp và sắp theo thời gian ở [TopicMapper.toHistory:45-59](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L45-L59). Chữ "Trưởng ban yêu cầu sửa" được đặt ở [dòng 49](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L49).
- Trong ảnh: 2 dòng = 2 dòng `topic_feedback` loại `CHANGE_REQUEST` (31/10 và 12/11) → khớp với "Số vòng sửa: 2".

### 1.6. Thông báo xanh / đỏ phía trên

[department_head/topic-review.html:11-12](../src/main/resources/templates/department_head/topic-review.html#L11-L12): `${message}` / `${error}` là **flash attribute** (dữ liệu chỉ sống qua đúng 1 lần chuyển trang) do controller đặt ở [dòng 49](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L49) và [dòng 73](../src/main/java/com/group6/electronic_drafting_system/controller/DepartmentHeadTopicController.java#L73). Lỗi thiếu góp ý → quay lại `?topic=<id>`, giữ nội dung đã gõ, hộp thoại tự mở lại ([dòng 85](../src/main/resources/templates/department_head/topic-review.html#L85) `data-reopen` + JS [dòng 112-113](../src/main/resources/templates/fragment/topic-review.html#L112-L113)).

---

## 2. Màn "Duyệt đề tài cuối" của Thư ký

Giao diện **gần y hệt màn 1** (dùng chung fragment `stats`, `queue`, `detail`, `history`). Chỉ khác 3 điểm:

| Điểm khác | Front-end | Back-end |
|---|---|---|
| **Hàng chờ** là đề tài `SECRETARY_REVIEW` của **mọi ban** | fragment `queue` như màn 1 | [TopicReviewServiceImpl.java:135-138](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L135-L138) `findByStatusOrderByCreatedAtAscIdAsc(SECRETARY_REVIEW)` |
| Có **ô chọn Ban** để lọc | [topic-final-review.html:28-32](../src/main/resources/templates/secretary/topic-final-review.html#L28-L32): `<form method="get">` + `<select name="department" onchange="this.form.submit()">` → chọn ban là gửi `GET ?department=<id>` | Controller [SecretaryTopicController.java:36](../src/main/java/com/group6/electronic_drafting_system/controller/SecretaryTopicController.java#L36) nhận `department`; service lọc bằng `.filter(...)` ở dòng 136–137 |
| Nút là **Duyệt / Từ chối** (không có Yêu cầu sửa) | [topic-final-review.html:57-61](../src/main/resources/templates/secretary/topic-final-review.html#L57-L61), form ở dòng 81 và 96 | [secretaryApprove](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L154) (tạo 2 assignment LEAD + DEPARTMENT_HEAD, status → `ACTIVE`), [secretaryReject](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L192) (lý do bắt buộc, status → `REJECTED`) |

**Bấm đề tài trong hàng chờ** chạy đúng như mục 1.2, chỉ khác link **giữ lại bộ lọc ban**: fragment `queue` nhận thêm tham số `department` ([topic-final-review.html:44](../src/main/resources/templates/secretary/topic-final-review.html#L44)), và [fragment/topic-review.html:40](../src/main/resources/templates/fragment/topic-review.html#L40) sinh link `?topic=13&department=2`. Nhờ vậy chọn đề tài xong không bị mất bộ lọc.

**Quyền:** [dòng 142-144](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicReviewServiceImpl.java#L142-L144): đề tài chưa qua Trưởng ban (`DEPARTMENT_REVIEW`) thì Thư ký gõ tay `?topic=` cũng bị 403 (NFR-03).

---

## 3. Màn "Danh sách đề tài" của Phóng viên

`GET /reporter/topics?status=...` → [TopicProposalController.java:45-48](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L45-L48) → [TopicServiceImpl.getProposalList:58-78](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L58-L78) → model `list` ([TopicProposalListDTO](../src/main/java/com/group6/electronic_drafting_system/dto/Topic/TopicProposalListDTO.java#L9)).

| Vùng | Front-end | Back-end |
|---|---|---|
| Header + **chuông thông báo** | [topic-proposal-list.html:30-62](../src/main/resources/templates/reporter/topic-proposal-list.html#L30-L62); chuông chỉ hiện với `sec:authorize="hasRole('REPORTER')"`; mở/đóng bằng JS [topic-proposal.js:24-39](../src/main/resources/static/reporter/js/topic-proposal.js#L24-L39) | `countUnread`, `getRecent` ở [TopicServiceImpl.java:76-77](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L76-L77) (bảng `notification`). Bấm 1 thông báo → [controller dòng 109](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L109): đánh dấu đã đọc rồi chuyển sang trang góp ý |
| Nút **Đề xuất đề tài mới** | [dòng 86](../src/main/resources/templates/reporter/topic-proposal-list.html#L86) link `/reporter/topics/new` | sang màn 4 |
| 5 thẻ thống kê (Tất cả, Chờ TB, Chờ TK, Đã duyệt, Từ chối) | [dòng 93-120](../src/main/resources/templates/reporter/topic-proposal-list.html#L93) `list.countOf('...')` | Đếm theo status bằng `groupingBy` ở [TopicServiceImpl.java:62](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L62) |
| **Tab lọc trạng thái** | [dòng 124-125](../src/main/resources/templates/reporter/topic-proposal-list.html#L124-L125): mỗi tab là link `?status=DEPARTMENT_REVIEW`... | **Cùng nguyên lý với mục 1.2**: controller đọc `status` trên URL ([parseStatus dòng 149](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L149)), service `.filter(...)` ở [dòng 69-72](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L69-L72) |
| Bảng đề tài | [dòng 153](../src/main/resources/templates/reporter/topic-proposal-list.html#L153) `th:each="t : ${list.topics()}"` | Chỉ đề tài **do chính mình tạo**: [TopicServiceImpl.java:176](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L176) `findByCreator_Id...` (mới nhất trước) |
| Badge **Cần sửa** + nút **Sửa và gửi lại** | [dòng 169](../src/main/resources/templates/reporter/topic-proposal-list.html#L169), [dòng 178-179](../src/main/resources/templates/reporter/topic-proposal-list.html#L178-L179): chỉ hiện khi `DEPARTMENT_REVIEW` và `awaitingRevision()` | cột `topic.awaiting_revision` (Trưởng ban bật khi Yêu cầu sửa) |
| Ô tìm nhanh | `data-tp-search` | Chỉ JS ẩn dòng, không gọi server ([topic-proposal.js:7-21](../src/main/resources/static/reporter/js/topic-proposal.js#L7-L21)) |

---

## 4. Màn "Đề xuất đề tài mới" / "Sửa và gửi lại"

**Một template dùng cho 2 việc**, phân biệt bằng biến `editTopicId` (null = tạo mới) — [TopicProposalController.formPage:116-128](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L116-L128).

| Vùng | Front-end | Back-end |
|---|---|---|
| Hộp **"Góp ý của Trưởng ban"** (chỉ khi sửa) | [topic-proposal-create.html:28-32](../src/main/resources/templates/reporter/topic-proposal-create.html#L28-L32) `th:if="${latestFeedback != null}"` | [controller dòng 121-123](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L121-L123): lấy góp ý yêu cầu sửa gần nhất |
| Form gửi đi đâu | [dòng 37](../src/main/resources/templates/reporter/topic-proposal-create.html#L37): `editTopicId == null` → `POST /reporter/topics`, ngược lại `POST /reporter/topics/{id}/resubmit` | `create` [dòng 59](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L59) / `resubmit` [dòng 90](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L90) |
| Ô Tiêu đề, Tóm tắt + bộ đếm ký tự | [dòng 38-66](../src/main/resources/templates/reporter/topic-proposal-create.html#L38-L66), `th:field="*{title}"` gắn với `topicForm`; bộ đếm do JS [topic-proposal.js:55-61](../src/main/resources/static/reporter/js/topic-proposal.js#L55-L61) | Giới hạn 255 / 4000 lấy từ hằng số `TITLE_MAX`, `SUMMARY_MAX` trong [TopicCreateRequestDTO](../src/main/java/com/group6/electronic_drafting_system/dto/Topic/TopicCreateRequestDTO.java#L12) |
| **Ban phụ trách** (chỉ đọc, không chọn được) | [dòng 54-55](../src/main/resources/templates/reporter/topic-proposal-create.html#L54-L55) `myDepartment.name()` | [TopicServiceImpl.getMyDepartment:82](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L82): ban của chính phóng viên |
| Nút **Gửi đề tài** → **hộp xác nhận** | Nút là `type="button"` ([dòng 74](../src/main/resources/templates/reporter/topic-proposal-create.html#L74)), nên **chưa gửi**. JS [openConfirm:101-116](../src/main/resources/static/reporter/js/topic-proposal.js#L101-L116) kiểm tra rỗng, chép tiêu đề + ban vào modal. Bấm "Xác nhận" → [dòng 137-141](../src/main/resources/static/reporter/js/topic-proposal.js#L137-L141) khóa nút rồi `form.submit()` | — |
| Lỗi đỏ dưới ô | `th:errors` [dòng 46](../src/main/resources/templates/reporter/topic-proposal-create.html#L46), [dòng 66](../src/main/resources/templates/reporter/topic-proposal-create.html#L66) | `@Valid` + `@NotBlank/@Size` → `BindingResult` ([controller dòng 60-62](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L60-L62)); service kiểm tra lại lần nữa ở `validate` [dòng 165](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L165) |

Lưu xuống DB:
- Tạo mới: [createProposal:88-103](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L88-L103) → [TopicMapper.toNewTopic:69-80](../src/main/java/com/group6/electronic_drafting_system/mapper/Topic/TopicMapper.java#L69-L80): status `DEPARTMENT_REVIEW`, `awaiting_revision = 0` → **vào ngay hàng chờ màn 1**.
- Gửi lại: [resubmit:134-146](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L134-L146): sửa tiêu đề / tóm tắt, tắt `awaiting_revision` → đề tài **quay lại hàng chờ màn 1** với badge "Đã sửa N lần".

---

## 5. Màn "Xem góp ý đề tài" của Phóng viên

`GET /reporter/topics/{id}/feedback` → [controller dòng 74-79](../src/main/java/com/group6/electronic_drafting_system/controller/TopicProposalController.java#L74-L79) → [getFeedbackView:107-117](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L107-L117).

| Vùng | Front-end | Back-end |
|---|---|---|
| Thẻ "Thông tin đề tài" (mã, ban, ngày, tiêu đề, tóm tắt, badge) | [topic-feedback.html:31-49](../src/main/resources/templates/reporter/topic-feedback.html#L31-L49) `view.topic()...` | `mapper.toListItem(topic)` |
| Nút **Sửa và gửi lại** | [dòng 25](../src/main/resources/templates/reporter/topic-feedback.html#L25) `th:if="${view.canEdit()}"` | `isWaitingForReporter(topic)` [dòng 155](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L155) |
| Lịch sử góp ý / kết quả duyệt | [dòng 51-66](../src/main/resources/templates/reporter/topic-feedback.html#L51-L66) | **Dùng lại đúng** `mapper.toHistory(...)` như màn 1 (mục 1.5) |

**Quyền:** [dòng 111](../src/main/java/com/group6/electronic_drafting_system/service/impl/TopicServiceImpl.java#L111): không phải người tạo đề tài → 403, kể cả gõ thẳng URL.

---

## 6. Trang Login + các pop-up tài khoản

Chỉ có **một file** [login.html](../src/main/resources/templates/login.html) chứa 6 pop-up, mỗi pop-up là một `<section data-lp-popup="...">`:

| Pop-up | Dòng | Form gửi tới | Controller |
|---|---|---|---|
| Đăng nhập | [31](../src/main/resources/templates/login.html#L31) | `POST /login` ([dòng 36](../src/main/resources/templates/login.html#L36)) | [AuthController.java:96](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L96) |
| Đăng ký | [60](../src/main/resources/templates/login.html#L60) | `POST /register` | [AuthController.java:128](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L128) |
| Nhập OTP | [95](../src/main/resources/templates/login.html#L95) | `POST /otp/verify`, `/otp/resend` | [169](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L169), [205](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L205) |
| Chờ Admin duyệt | [121](../src/main/resources/templates/login.html#L121) | (chỉ thông báo) | — |
| Quên mật khẩu | [131](../src/main/resources/templates/login.html#L131) | `POST /forgot-password` | [231](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L231) |
| Đặt lại mật khẩu | [147](../src/main/resources/templates/login.html#L147) | `POST /reset-password` | [252](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L252) |

**Pop-up nào hiện ra?** Mỗi section có `th:hidden="${popup != '...'}"`. Biến `popup` do **server** quyết định ở [AuthController.loginPage:43](../src/main/java/com/group6/electronic_drafting_system/controller/AuthController.java#L43) (từ flash hoặc `?popup=` trên URL). Đây là **cùng ý tưởng với mục 1.2**: URL / dữ liệu server gửi về quyết định phần nào hiện. Bấm qua lại Login ↔ Đăng ký ↔ Quên mật khẩu thì JS [auth-popup.js](../src/main/resources/static/js/auth-popup.js) đổi ngay trên trình duyệt.

Chi tiết từng nút bấm (đăng nhập, OTP, quên mật khẩu, Google): [lan-theo-code-tv1.md mục 1–7](lan-theo-code-tv1.md#1-mở-trang-đăng-nhập-dòng-3).

---

## 7. Bảng tóm tắt: phần nào chạy ở server, phần nào chạy ở trình duyệt

| Việc | Chạy ở đâu | Có gọi DB không |
|---|---|---|
| Bấm đề tài trong hàng chờ, bấm tab trạng thái, chọn ban lọc | **Server** (link / form GET → tải lại trang) | Có |
| Bấm Xác nhận duyệt / Gửi yêu cầu sửa / Gửi đề tài | **Server** (form POST → redirect) | Có (ghi) |
| Mở / đóng hộp thoại, đếm ký tự, tìm nhanh, mở chuông | **Trình duyệt** (JS) | Không |
| Ẩn / hiện nút theo quyền | **Server** quyết định có đưa nút vào HTML hay không (`th:if`) | — |
| Kiểm tra quyền thật sự | **Server** (SecurityConfig theo URL + service kiểm tra lại) | Có |

**Câu chốt khi thầy hỏi về bất kỳ vùng nào trên màn hình:** *"Vùng này là fragment X trong template, nó đọc `${ws.abc()}`. `abc` được service điền vào DTO ở dòng Y, lấy từ bảng Z qua repository."* → rồi Ctrl+click theo đúng thứ tự trong bảng của màn đó.
