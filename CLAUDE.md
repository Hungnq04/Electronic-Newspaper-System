# CLAUDE.md — CMS Core (Hệ thống Tòa soạn Điện tử, Phase 1 / MVP)

Tài liệu này mô tả nghiệp vụ và quy ước của dự án. Hãy đọc kỹ trước khi viết code, viết SQL hoặc đề xuất thiết kế.
Nguồn gốc: `PRD_v2` và `cms_core_sqlserver_v2.sql`. Phần kỹ thuật (tech stack, cấu trúc code, lệnh chạy) ở **mục 13**.

---

## 0. Quy tắc làm việc với tôi

- Trả lời bằng **tiếng Việt**. Tên bảng, cột, enum, biến trong code giữ nguyên tiếng Anh.
- **Tôi là người mới học lập trình. Hãy giữ mọi thứ đơn giản:**
  - Chọn cách làm **dễ hiểu, dễ đọc nhất**, không dùng kỹ thuật cao siêu hay "mẹo" khó hiểu. Nếu có hai cách, ưu tiên cách đơn giản dù dài hơn một chút.
  - **Không over-engineering**: không thêm tầng trừu tượng, design pattern, thư viện, hay cấu trúc phức tạp nếu bài toán chưa cần.
  - Khi viết code, thêm **comment ngắn bằng tiếng Việt** ở những chỗ quan trọng (đặc biệt là chỗ kiểm tra quyền và đổi trạng thái).
  - Gặp thuật ngữ khó (ví dụ transaction, DTO, middleware, JWT...) thì **giải thích ngắn bằng ví dụ** lần đầu xuất hiện, đừng mặc định tôi đã biết.
  - Đặt tên biến, hàm, file **rõ nghĩa**, nhất quán với tên trong DB (ví dụ `article_id`, `assignment_role`).
  - Trước khi sửa nhiều file hoặc đổi cấu trúc, **cho tôi biết kế hoạch** (sửa file nào, vì sao) để tôi hiểu và đồng ý.
  - Khi có lỗi, giải thích **nguyên nhân bằng lời dễ hiểu** rồi mới đưa cách sửa, đừng chỉ dán code đã sửa.
  - Nếu tôi yêu cầu điều gì đó có thể gây lỗi hoặc mất dữ liệu (ví dụ xóa bảng, xóa dữ liệu), hãy **cảnh báo trước**.
- **Không tự bịa nghiệp vụ.** Nếu yêu cầu chưa có trong file này hoặc PRD, hoặc code / SQL mâu thuẫn với file này, hãy nêu giả định rồi **hỏi tôi**, đừng tự chọn một bên.
- Mọi kiểm tra quyền phải làm **ở backend** (controller / service), không chỉ ẩn nút trong template Thymeleaf. Vi phạm quyền thì ném `ApplicationException` với `ErrorCode` phù hợp (HTTP 403).
- Khi viết SQL, dùng **T-SQL (SQL Server 2016+)**, bám đúng tên bảng / cột trong mục 8.
- Khi sửa code liên quan đến trạng thái hoặc quyền, đối chiếu mục 3–6 và flow ở mục 10 trước. Nếu thay đổi vi phạm một Business Rule (BR-xx), hãy cảnh báo trước khi làm.

---

## 1. Tổng quan

CMS Core mô phỏng vòng đời của một bài báo trong tòa soạn:

`Topic → Article → Draft → Review → Revision → Approval → Publish → Public Reading`

Luồng duyệt bắt buộc: **Reporter (Lead) → Department Head → Editorial Secretary → Publish**.
Không có đường tắt: Reporter **không bao giờ** gửi bài thẳng cho Secretary.

Người dùng:
- 4 role nội bộ: `REPORTER`, `DEPARTMENT_HEAD`, `EDITORIAL_SECRETARY`, `ADMIN`.
- `Public Reader`: không cần tài khoản, chỉ đọc bài đã PUBLISHED.

---

## 2. QUY TẮC QUAN TRỌNG NHẤT: Lead Reporter và Supporting Reporter

> **Sau khi Topic được duyệt (ACTIVE), chỉ có DUY NHẤT một người viết bài: Lead Reporter.**
> **Các Reporter khác tham gia Topic là Supporting Reporter, chỉ có chức năng đóng góp tư liệu (Material): ảnh, video, tài liệu, link, ghi chú phỏng vấn.**

Chi tiết:

| | Lead Reporter | Supporting Reporter |
|---|---|---|
| Số lượng trên 1 Topic | **Đúng 1** người đang hoạt động (BR-14) | Nhiều người, hoặc không có |
| Tạo Article | ✅ (BR-21) | ❌ |
| Sửa nội dung Article / Save Draft | ✅ | ❌ |
| Submit / Resubmit Article | ✅ | ❌ |
| Restore Version | ✅ | ❌ |
| Thêm Source / Material vào Topic | ✅ | ✅ |
| Xem Topic được giao | ✅ | ✅ |
| Đánh dấu Assignment COMPLETED | — | ✅ (khi xong phần việc) |
| Chọn Source từ Topic đưa vào Article | ✅ | ❌ |

Hệ quả khi code:
- Lead / Supporting là **vai trò theo từng Topic** (cột `assignment.assignment_role`), **không phải** role tài khoản. Cùng một Reporter có thể là Lead ở Topic này và Supporting ở Topic khác.
- Mọi endpoint tạo / sửa / nộp Article phải kiểm tra: người gọi có `assignment` với `assignment_role = 'LEAD'` **và** `status <> 'REMOVED'` trên Topic của bài đó. Chỉ có `role = REPORTER` là **không đủ**.
- Supporting **không** có quyền sửa Article, kể cả khi họ tự gõ URL / gửi form trực tiếp. Trả về 403.
- Một người **không được giữ hai vai trò** trong cùng một Topic (BR-24, BR-38): không vừa Lead vừa Supporting, và không vừa Department Head vừa Reporter của cùng Topic.
- Nếu một Topic cần nhiều người cùng viết bài: **không** cho Supporting viết. Cách xử lý đã đề xuất: Department Head đổi Lead (EC-01) hoặc tạo Topic mới (OQ-05).
- Vì chỉ Lead được sửa nên không có xung đột "Lead và Supporting cùng sửa một bài" (EC-04). Chỉ còn rủi ro cùng một người mở bài ở nhiều tab / thiết bị.

---

## 3. Vai trò và quyền hạn

**Reporter**: đề xuất Topic; quyền Lead / Supporting xem bảng mục 2; Lead phản hồi comment. Không duyệt, không publish.

**Department Head (Trưởng phòng)**: duyệt Topic ở cấp phòng; nhận Topic Secretary giao xuống; **phân công** Lead và Supporting (mỗi người một nhiệm vụ + deadline riêng); review Article cấp phòng (Inline Comment, Request Revision, Approve cấp phòng). **Không** Final Approve, **không** Publish, **không** sửa trực tiếp nội dung Article (OQ-01), **không** tự tạo Topic (OQ-07).

**Editorial Secretary (Thư ký tòa soạn)**: tạo Editorial Topic (top-down) và giao cho Department Head; duyệt Topic lần cuối (Approve / Reject); review Article sau khi Department Head đã duyệt; Final Approve; Publish hoặc Schedule; Return to Lead; Cancel & Return.

**Admin**: đổi role, gán phòng ban, khóa / mở tài khoản. **Không** tham gia biên tập (không duyệt, không publish; BR-37). Admin không được tạo Topic.

**Public Reader**: chỉ xem bài PUBLISHED. Không thấy Draft, Version History, Comment nội bộ.

Quy tắc chung: **người duyệt không được duyệt Topic / Article do chính mình đề xuất hoặc phụ trách** (BR-34, đề xuất, OQ-14 chưa chốt).

---

## 4. Topic

### 4.1. Hai cách có Topic

**(A) Reporter đề xuất (bottom-up):**
```
Reporter tạo Topic ──► DEPARTMENT_REVIEW ──► (TP duyệt) ──► SECRETARY_REVIEW ──► (Thư ký duyệt) ──► ACTIVE
                              │                                    │
                         REJECTED + feedback                  REJECTED + feedback
```
- Bị từ chối ở **bất kỳ cấp nào** → `REJECTED` + feedback, **kết thúc**. Không có vòng "yêu cầu sửa Topic", không gửi lại (OQ-08). Reporter đề xuất Topic mới.
- Khi Secretary approve, **hệ thống tự tạo 2 Assignment**:
  1. Reporter đề xuất → `LEAD` (giao bởi Secretary đã approve; nhiệm vụ và deadline **để trống**, Department Head bổ sung sau).
  2. Department Head đã duyệt ở cấp phòng → `DEPARTMENT_HEAD` của Topic.
- Từ chối bắt buộc có feedback (BR-12).

**(B) Secretary giao xuống (top-down):**
```
Secretary tạo Editorial Topic ──► ACTIVE ngay ──► "Topic chưa giao" ──► chọn Department Head ──► tạo Assignment DEPARTMENT_HEAD
        ──► Department Head xem Topic ──► giao Lead (+ Supporting nếu cần), mỗi người nhiệm vụ + deadline riêng
```
- Không qua duyệt Topic (BR-05).
- Topic ACTIVE chưa có Department Head nằm trong danh sách "Topic chưa giao". Nếu Secretary thoát giữa chừng thì Topic vẫn ở danh sách này (EC-09).
- Khi Department Head bấm Confirm Assignment, hệ thống kiểm tra: **đúng một Lead**; mọi người được chọn là Reporter đang `ACTIVE`; Lead không đồng thời là Supporting.
- Giao lại cho Department Head khác: Assignment cũ → `REMOVED`, tạo Assignment mới.

### 4.2. Trạng thái Topic (theo DB)
`DEPARTMENT_REVIEW` → `SECRETARY_REVIEW` → `ACTIVE`, hoặc `REJECTED`.
Topic không tự đóng khi có bài PUBLISHED (BR-02). Một Topic có nhiều Article (BR-01). Topic cũ "hot" lại thì tạo Article mới trong Topic cũ, không tạo Topic trùng (EC-06).

### 4.3. Tình trạng giao việc không lưu thành status
Suy ra từ `assignment`: chưa có Department Head → chưa có Lead → đã giao đủ. **Topic ACTIVE chưa có Lead thì chưa được tạo Article.**

---

## 5. Assignment

- Mỗi dòng = **một người, một vai trò, một nhiệm vụ, trên một Topic**. Gắn với Topic, **không** gắn với Article (BR-25).
- `assignment_role`: `DEPARTMENT_HEAD` (Secretary giao cho TP), `LEAD`, `SUPPORTING` (TP giao cho Reporter).
- `status`: `ASSIGNED` → `COMPLETED` (xong phần việc), hoặc `REMOVED` (bị thay / gỡ).
- **Không bao giờ xóa dòng Assignment.** Đổi Lead, gỡ Supporting, giao lại TP → đóng dòng cũ bằng `REMOVED` (ghi `removed_at`, `removed_by`) rồi tạo dòng mới.
- Khi đổi Lead: **Article chưa PUBLISHED của Lead cũ chuyển sang Lead mới** (cập nhật `article.lead_id`) (EC-01).
- Khi gỡ Supporting: Source họ đã thêm **được giữ nguyên** (EC-02).
- Ràng buộc đang có trong DB (unique index, chỉ tính dòng `status <> 'REMOVED'`): mỗi người 1 assignment hiệu lực / Topic; 1 Lead / Topic; 1 Department Head / Topic.
- Luôn lọc `status <> 'REMOVED'` khi muốn biết "ai đang phụ trách hiện tại".

---

## 6. Article

### 6.1. Trạng thái
| Trạng thái | Ý nghĩa | Ai giữ bài | Sửa nội dung? |
|---|---|---|---|
| `DRAFT` | Lead đang soạn, **chưa có version** | Lead | ✅ |
| `DEPARTMENT_REVIEW` | Chờ / đang được TP duyệt | Department Head | ❌ khóa |
| `REVISION_REQUIRED` | Bị trả về Lead (bởi TP hoặc Thư ký) | Lead | ✅ |
| `DEPARTMENT_APPROVED` | TP đã duyệt, chờ Thư ký | Secretary | ❌ |
| `APPROVED` | Thư ký đã Final Approve, chờ quyết định xuất bản | Secretary | ❌ |
| `SCHEDULED` | Đã hẹn giờ đăng | Secretary | ❌ |
| `PUBLISHED` | Đã đăng | — | ❌ |

Không có trạng thái `CANCELLED` / `REJECTED` cho Article. Mọi bài không được đăng đều **trả về Lead kèm lý do**.
Hai trạng thái `SUBMITTED_TO_DEPARTMENT` và `SECRETARY_REVIEW` **không dùng** (OQ-13).

### 6.2. Bảng chuyển trạng thái (chỉ được đi đúng các đường này)
| Từ → Đến | Hành động | Ai | Điều kiện |
|---|---|---|---|
| (mới) → `DRAFT` | Create Article | **Lead** | Topic `ACTIVE`, Lead đang hoạt động |
| `DRAFT` → `DEPARTMENT_REVIEW` | Submit | Lead | Qua kiểm tra bắt buộc; tạo version 1.0 |
| `DEPARTMENT_REVIEW` → `REVISION_REQUIRED` | Request Revision | Department Head | Feedback bắt buộc |
| `DEPARTMENT_REVIEW` → `DEPARTMENT_APPROVED` | Approve | Department Head | |
| `DEPARTMENT_APPROVED` → `REVISION_REQUIRED` | Request Revision | Secretary | Feedback bắt buộc; **trả thẳng Lead** |
| `DEPARTMENT_APPROVED` → `APPROVED` | Final Approve | Secretary | |
| `REVISION_REQUIRED` → `DEPARTMENT_REVIEW` | Resubmit | Lead | Nội dung **khác** version mới nhất; tạo version mới |
| `APPROVED` → `PUBLISHED` | Publish now | Secretary | |
| `APPROVED` → `SCHEDULED` | Schedule | Secretary | Thời điểm ở **tương lai** |
| `SCHEDULED` → `PUBLISHED` | Đến giờ | System | Bài **vẫn còn** `SCHEDULED` |
| `APPROVED` → `REVISION_REQUIRED` | Return to Lead | Secretary | Lý do bắt buộc |
| `SCHEDULED` → `REVISION_REQUIRED` | Cancel & Return | Secretary | Trước giờ đăng; lý do bắt buộc |

### 6.3. Các điểm dễ sai
- **Secretary Request Revision → bài trả thẳng Lead, nhưng sau khi Lead sửa và Resubmit, bài phải đi lại Department Head rồi mới tới Secretary** (BR-11). Không được nhảy thẳng về Secretary.
- Department Head approve **không phải** Final Approval (EC-05).
- Secretary chỉ thấy bài đã `DEPARTMENT_APPROVED` (NFR-03: bài chưa qua TP không được xuất hiện ở hàng chờ Secretary).
- Bài `DEPARTMENT_REVIEW` chờ Department Head nào? → Department Head có `assignment_role = 'DEPARTMENT_HEAD'`, `status <> 'REMOVED'` trên Topic của bài.
- Department Head **không sửa trực tiếp** Article; chỉ Comment / Request Revision.
- Một Lead có thể làm nhiều Article cùng lúc (BR-16). Mỗi Article có workflow độc lập.
- Bài đã PUBLISHED: **chưa** có chức năng gỡ hoặc đính chính (BR-33).

### 6.4. Kiểm tra bắt buộc khi Submit / Resubmit (FR-07)
Phải có: tít, sapo, nội dung, **ít nhất một Source**, và **mọi ảnh trong bài có chú thích** (caption). Resubmit phải kèm ghi chú "đã sửa gì" (`change_summary`) và nội dung phải khác version mới nhất.

---

## 7. Version, Review, Comment, Publication

### Version
- **Save Draft chỉ cập nhật bản đang soạn nằm ngay trên bảng `article`** (title, subtitle, sapo, content). **Không tạo version.**
- Version (`article_version`) **chỉ tạo khi Submit / Resubmit**. Số lưu 1, 2, 3...; hiển thị 1.0, 1.1, 1.2... (`version_label`).
- Version **bất biến**: không UPDATE, không DELETE (trigger `trg_article_version_immutable` chặn). Đừng viết code sửa / xóa version.
- Restore: chép nội dung version cũ vào bản đang soạn, **chỉ khi** Article ở `DRAFT` hoặc `REVISION_REQUIRED`; không xóa lịch sử; version mới chỉ tạo ở lần Submit / Resubmit kế tiếp, ghi chú "Khôi phục từ version X".

### Review (lượt duyệt)
- Một dòng `article_review` = một lượt duyệt của một cấp (`DEPARTMENT` / `SECRETARY`) trên **một version**.
- Review được tạo khi reviewer **mở bài** (`decision = NULL`, nghĩa là đang xem). Quyết định: `APPROVED` hoặc `REQUEST_REVISION`.
- `review_kind`: `REVIEW` (bình thường), `RETURN_TO_LEAD` (Secretary trả bài đã APPROVED), `CANCEL_SCHEDULE` (Secretary hủy lịch). Hai loại sau luôn là cấp `SECRETARY` + `REQUEST_REVISION`.
- Request Revision / Return / Cancel **bắt buộc có feedback**.
- Final Approve = Review cấp `SECRETARY` có `decision = 'APPROVED'`.

### Comment (Inline Comment)
- Thuộc một Review, gắn với một version cố định (nên vị trí không bị lệch khi Lead sửa tiếp).
- Vị trí: `anchor_field` (`TITLE` / `SUBTITLE` / `SAPO` / `CONTENT`), `anchor_paragraph` (đếm từ **1**), `anchor_start` / `anchor_end` (ký tự, đếm từ **0**, `anchor_end` không tính), `quoted_text`.
- Trạng thái `OPEN` / `RESOLVED`. Khi duyệt lại, reviewer thấy các comment chưa RESOLVED của vòng trước và so sánh với version trước.
- Comment gốc do chính reviewer của lượt duyệt viết. Lead có thể trả lời (reply) trong cùng lượt review (OQ-11 chưa chốt hẳn).

### Publication
- Mỗi Article tối đa **một** Publication. Luôn trỏ đúng **version đã Final Approve**.
- `IMMEDIATE` (đăng ngay) hoặc `SCHEDULED` (hẹn giờ, `scheduled_at` phải ở tương lai).
- Hủy lịch đăng thì **xóa dòng Publication** (BR-31, OQ-10).
- Trang công khai chỉ đọc view `v_public_article` (chỉ bài PUBLISHED, đúng version đã duyệt).

---

## 8. Cơ sở dữ liệu (SQL Server) — `cms_core_sqlserver_v2.sql`

15 bảng: `role`, `department`, `account`, `account_otp`, `topic`, `topic_review`, `assignment`, `source`, `topic_source`, `article_source`, `article`, `article_version`, `article_review`, `comment`, `publication`.

Views: `v_topic_team`, `v_article_board`, `v_article_timeline`, `v_public_article`, `v_rule_violations`.

Quy ước:
- Thời gian dùng `DATETIMEOFFSET(0)`, giờ Việt Nam `+07:00`.
- **Không dùng `ON DELETE CASCADE`**. Không xóa người dùng / assignment; chỉ đổi `status` (`INACTIVE` / `LOCKED` / `REMOVED`). Mục đích: giữ lịch sử để truy vết (NFR-02).
- Bài đã nộp (`status <> 'DRAFT'`) phải có ít nhất một version (rule R18). Bài `DRAFT` chưa có version.
- Quy tắc liên bảng không khai báo được bằng constraint nằm ở view `v_rule_violations`. **Sau mỗi thay đổi dữ liệu / schema, `SELECT * FROM v_rule_violations;` phải trả về 0 dòng.**
- Source: loại `IMAGE`, `DOCUMENT`, `WEBSITE`, `INTERVIEW`, `INTERNAL_DOC`. Quan hệ N–N với Topic (`topic_source`) và Article (`article_source`). `INTERVIEW` và `INTERNAL_DOC` **không** hiện trên Public Site. Chỉ người thêm (`source.added_by`) được sửa / xóa Source. Mỗi Article tối đa một ảnh đại diện (`is_featured`), ảnh đại diện phải là Source loại `IMAGE`.
- Ảnh / tài liệu upload lên **Cloudinary**; DB chỉ lưu URL.

Mẫu truy vấn "bài đang chờ Department Head X duyệt":
```sql
-- Bài DEPARTMENT_REVIEW, thuộc Topic mà @dept_head_id đang là DEPARTMENT_HEAD (assignment chưa REMOVED),
-- lấy version mới nhất của bài.
DECLARE @dept_head_id INT = 4;
SELECT a.article_id, a.title, t.title AS topic_title, v.version_label, v.created_at AS submitted_at
FROM article a
JOIN topic t       ON t.topic_id = a.topic_id
JOIN assignment dh ON dh.topic_id = a.topic_id
                  AND dh.assignment_role = 'DEPARTMENT_HEAD'
                  AND dh.status <> 'REMOVED'
                  AND dh.account_id = @dept_head_id
JOIN article_version v ON v.article_id = a.article_id
                      AND v.version_number = (SELECT MAX(version_number) FROM article_version WHERE article_id = a.article_id)
WHERE a.status = 'DEPARTMENT_REVIEW'
ORDER BY v.created_at;
```

---

## 9. Tài khoản & bảo mật

- Đăng ký (username, họ tên, email, mật khẩu) → role `REPORTER`, status `PENDING`. Xác minh OTP 6 số qua email → `ACTIVE`. **`PENDING` không đăng nhập được.**
- OTP: chỉ lưu **hash**; hết hạn **10 phút**; sai tối đa **5 lần** thì khóa mã; dùng **một lần**; gửi lại sau **60 giây**. Mục đích chỉ có `VERIFY_EMAIL` và `RESET_PASSWORD` (không có đăng nhập 2 lớp).
- Quên mật khẩu: nhập email → hệ thống trả lời **giống nhau** dù email có tồn tại hay không.
- Mật khẩu lưu **BCrypt**, không bao giờ lưu plaintext.
- Tài khoản không còn `ACTIVE` (LOCKED / INACTIVE) thì không được giữ assignment đang hiệu lực (rule R23).
- Department Head phải được gán phòng ban (rule R20).
- Người dùng không được truy cập trang / chức năng của role khác bằng cách đổi URL (NFR-01).
- **Đăng nhập Google OAuth2** đã có trong code (`config/GoogleLoginSuccessHandler.java`) nhưng PRD chưa mô tả. _TODO (tôi sẽ bổ sung): email Google chưa có tài khoản thì xử lý sao? trạng thái / role mặc định?_ Chưa rõ thì hỏi tôi.

---

## 10. Sơ đồ luồng (swimlane) — `SWP_Flows_PRDv2.drawio`

File draw.io gồm 9 sơ đồ. Khi cần thứ tự bước, ai làm gì, rẽ nhánh ra sao thì **dùng các flow này làm chuẩn**; khi cần tên cột / giá trị enum thì **theo DB** (mục 8).

| Mã | Nội dung | Lane | Ý chính |
|---|---|---|---|
| FL-01 | Reporter đề xuất Topic (PRD Workflow A) | Reporter, TP, Thư ký, System | Như mục 4.1 (A). Tiếp tục ở FL-03. |
| FL-02 | Secretary tạo và giao Editorial Topic (Workflow B) | Thư ký, System, TP | Như mục 4.1 (B): giao ngay hoặc để sau (EC-09). |
| FL-03 | Department Head phân công đội + Supporting cung cấp Material (Workflow B / B2) | TP, System, Reporter | Topic đã có Lead (do Reporter đề xuất) thì TP **chỉ nhập nhiệm vụ + deadline** cho Lead; chưa có thì chọn Lead. Validate như mục 4.1 (B). **Lead sang FL-04**; **Supporting** thêm Material → `COMPLETED`. |
| FL-04 | Lead viết và nộp Article (Workflow C) | Lead, System | Mục 6.2, 6.4, 7-Version: `DRAFT` → Save Draft (không tạo version) → Submit → version 1.0 → `DEPARTMENT_REVIEW`, khóa nội dung. |
| FL-05 | Department Head duyệt và vòng sửa (Workflow D) | Lead, TP, System | Mở bài → tạo Review `DEPARTMENT` → comment → Approve (sang FL-06) / Request Revision. Lead xem feedback, có thể Restore, sửa, nhập change note, Resubmit (1.x). |
| FL-06 | Secretary duyệt cuối (Workflow E) | Lead, TP, Thư ký, System | Final Approve (sang FL-07) / Request Revision trả **thẳng Lead** (TP được báo) → Resubmit **đi lại FL-05** (BR-11). |
| FL-07 | Xuất bản và đọc công khai (Workflow F) | Thư ký, System, Public Reader | Mục 6.2 + 7-Publication: Return to Lead / Publish now / Schedule / Cancel & Return. |
| FL-AUTH-01 | Đăng ký và kích hoạt tài khoản | Guest, System, Admin | `PENDING` → OTP → `ACTIVE`; sai OTP đến giới hạn thì vô hiệu mã, chờ 60 giây để xin mã mới; Admin gán phòng ban / đổi role nếu cần. |
| FL-AUTH-02 | Quên mật khẩu bằng OTP | User, System | Luôn hiện cùng một thông báo dù email có tồn tại hay không; đặt lại mật khẩu bằng BCrypt; OTP dùng một lần. |

### Lưu ý khi cài đặt (flow không ghi rõ)

1. **Trả bài về `REVISION_REQUIRED` (Return to Lead, Cancel & Return, Request Revision của Thư ký)**: phải đặt `article.completed_at = NULL`, nếu không sẽ vi phạm constraint `ck_article_completed`.
2. **Cancel & Return**: phải **xóa dòng `publication`** trong cùng một transaction với việc đổi trạng thái bài, nếu không `v_rule_violations` sẽ báo R06.
3. **"Notify" / "Add to review queue" trong flow** chỉ nghĩa là hiển thị trong danh sách chờ của người nhận. **Chưa làm thông báo thật** (OQ-12).
4. **Tên gọi**: flow ghi `level = DEPARTMENT / SECRETARY`, trong DB là cột `review_stage`. Code dùng tên cột trong DB.
5. **Flow không thể hiện BR-34** (người duyệt không được duyệt Topic / Article do mình đề xuất hoặc phụ trách). Vẫn phải kiểm tra ở backend.
6. Node ghi chú điều kiện (ví dụ "Valid = ...") chỉ là ghi chú của hệ thống, không phải bước do người ở lane đó thực hiện.

### Các phần chưa có flow riêng

Hãy theo PRD và các mục trên của file này, và **hỏi tôi nếu còn mơ hồ**: Login / Logout; Admin khóa / mở tài khoản; đổi Lead hoặc gỡ Supporting (EC-01, EC-02); giao lại Topic cho Department Head khác; xem Version History, so sánh, Restore ngoài luồng sửa bài; Reply Comment (OQ-11); Edit Topic.

---

## 11. Ngoài phạm vi Phase 1 (đừng tự làm)

AI viết bài / kiểm fact, video / podcast, mobile app, quảng cáo, thanh toán / subscription, tài khoản và comment của độc giả, analytics / SEO nâng cao, đăng mạng xã hội tự động, đa ngôn ngữ, workflow designer, gỡ / đính chính bài đã PUBLISHED, gửi lại Topic đã REJECTED, thông báo (NOTIFICATION) thật. Các bước "thông báo" hiện chỉ là **hiển thị danh sách chờ** (OQ-12).

---

## 12. Câu hỏi còn mở (mặc định đang dùng, hãy hỏi tôi nếu việc của bạn chạm vào đây)

| Mã | Vấn đề | Mặc định hiện tại |
|---|---|---|
| OQ-05 | Topic cần nhiều người cùng viết bài | Đổi Lead hoặc tạo Topic mới; **không** cho Supporting viết |
| OQ-06 | Ai nhập nhiệm vụ / deadline của Lead tự động | Để trống ban đầu, Department Head bổ sung |
| OQ-07 | Department Head tự tạo Topic | Không |
| OQ-08 | Gửi lại Topic bị REJECTED | Không, đề xuất Topic mới |
| OQ-09 | Lọc đề xuất Topic theo phòng ban | Chưa lọc ở nghiệp vụ (DB có rule R08 mang tính đề xuất) |
| OQ-11 | Reporter phản hồi Comment | DB đã có `parent_comment_id`, chưa chốt UI |
| OQ-14 | Người duyệt ≠ người phụ trách (BR-34) | Áp dụng |

---

## 13. Kỹ thuật

**Tech stack:** Java 17, Spring Boot 4.1.1, Maven (`mvnw`), Spring Data JPA + SQL Server (`mssql-jdbc`), Spring Security (form login + Google OAuth2), Spring Mail (gửi OTP qua Gmail SMTP), Bean Validation, Lombok.
Giao diện: **Thymeleaf render HTML phía server** (+ `thymeleaf-extras-springsecurity6`) — **không phải** REST API + SPA. Controller trả về tên template, form submit bằng POST.

**Cấu trúc** (`src/main/java/com/group6/electronic_drafting_system/`):
- `controller/` → `service/` (interface) + `service/impl/` → `repository/` → `entity/` (+ `entity/enums/`)
- `dto/<Feature>/`, `mapper/<Feature>/` (ví dụ `dto/DepartmentHead/`)
- `exception/`: `ApplicationException` + enum `ErrorCode` (mã lỗi, message tiếng Việt, `HttpStatus`)
- `config/`: `SecurityConfig`, `SidebarAdvice`, `GoogleLoginSuccessHandler`...
- Template: `templates/<role>/`, layout chung `templates/layout/app.html`, fragment `templates/fragment/`. CSS/JS: `static/<role>/css|js`.

**Lệnh:** chạy `./mvnw spring-boot:run`; test `./mvnw test`.

**Cấu hình:** `src/main/resources/application.properties` bị gitignore → mỗi người tự tạo bản local. Google OAuth đọc `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` từ biến môi trường. **Không commit mật khẩu / secret.**

**Quy ước JPA:**
- `spring.jpa.hibernate.ddl-auto=none`: schema do file SQL quản lý (có trigger, view, unique index có điều kiện). **Không** để Hibernate tự tạo / sửa bảng; đổi schema thì sửa file SQL.
- Enum map bằng `@Enumerated(EnumType.STRING)`, tên hằng Java trùng đúng giá trị trong DB.
- Thao tác ghi nhiều bảng phải nằm trong **một** method `@Transactional` ở service (transaction = "làm hết hoặc không làm gì": lỗi giữa chừng thì mọi thay đổi bị hủy). Ví dụ: Secretary approve Topic + tạo 2 Assignment; Submit + tạo version; Cancel & Return + xóa `publication`; đổi Lead + chuyển `article.lead_id`.

**Branch / commit:** làm trên branch riêng, PR vào `main`. _(Quy ước đặt tên commit: tôi sẽ bổ sung.)_

**Tài liệu tham chiếu:** `PRD_v2.md`, `cms_core_sqlserver_v2.sql`, `SWP_Flows_PRDv2.drawio` — **hiện chưa có trong repo**; cần thì hỏi tôi.
