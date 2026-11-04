-- =====================================================================
-- CMS CORE - Hệ thống Tòa soạn Điện tử (Phase 1 / MVP)
-- Cơ sở dữ liệu : Microsoft SQL Server 2016 trở lên (T-SQL)
-- Dựa trên      : PRD v2 + bộ UC / flow FL-01..FL-07, FL-AUTH-01/02 của nhóm
-- Phiên bản     : 2.0 (sửa từ bản cms_core_sqlserver.sql của nhóm, giữ nguyên cấu trúc và quy ước cũ)
-- =====================================================================
-- CÁCH CHẠY (SSMS hoặc Azure Data Studio)
--   Mở file này rồi Execute toàn bộ (F5). File tự tạo database cms_core nếu chưa có.
--   Chạy lại nhiều lần được: mọi bảng / view được xóa và tạo lại, dữ liệu mẫu nạp lại từ đầu.
--   Dòng lệnh: sqlcmd -S . -E -I -f 65001 -i cms_core_sqlserver_v2.sql
--   Kiểm tra : SELECT * FROM v_rule_violations;  -- phải trả về 0 dòng
-- File lưu UTF-8 có BOM. Nếu chữ tiếng Việt trong dữ liệu bị lỗi: File > Save As >
--   mũi tên cạnh Save > Save with Encoding > "Unicode (UTF-8 with signature)", rồi chạy lại.
-- MẬT KHẨU DEMO của mọi tài khoản trong dữ liệu mẫu: Cms@2026
--
-- THAY ĐỔI SO VỚI BẢN TRƯỚC (đánh dấu [v2] tại chỗ sửa)
--   1. Bản đang soạn của Article nằm ở chính bảng article (title, subtitle, sapo, content).
--      Save Draft chỉ cập nhật article; article_version CHỈ tạo khi Submit / Resubmit
--      và không bao giờ bị sửa / xóa (trigger chặn) - BR-27, PRD 4.3.
--   2. [FL-01] Duyệt Topic (thay quyết định cũ "bỏ Trưởng phòng yêu cầu sửa Topic"):
--      - Trưởng phòng: DUYỆT (topic_review DEPARTMENT / APPROVED, status -> SECRETARY_REVIEW)
--        hoặc YÊU CẦU SỬA (topic_feedback CHANGE_REQUEST, awaiting_revision = 1, status giữ
--        DEPARTMENT_REVIEW; Reporter sửa và gửi lại thì awaiting_revision = 0, được sửa nhiều vòng).
--        Trưởng phòng KHÔNG được từ chối.
--      - Thư ký tòa soạn: DUYỆT (SECRETARY / APPROVED, status -> ACTIVE) hoặc TỪ CHỐI kèm lý do
--        bắt buộc (SECRETARY / REJECTED, status -> REJECTED). ACTIVE và REJECTED là trạng thái cuối.
--      - Dữ liệu mẫu T9 (Trưởng phòng từ chối) giữ lại từ bản trước.
--   3. article_review: một version có thể có thêm lượt REQUEST_REVISION của Thư ký sau khi
--      đã APPROVED (Return to Lead, Cancel & Return) - FR-17, FR-18. Cột review_kind phân biệt.
--   4. Tài khoản: thêm role ADMIN, trạng thái PENDING (chờ xác minh email), phòng ban có thể
--      để trống (người mới đăng ký, Admin) - FR-13, FR-15.
--   5. Tên trạng thái theo PRD v2: DEPARTMENT_APPROVED (thay SECRETARY_REVIEW),
--      REQUEST_REVISION (thay REVISION_REQUESTED), assignment REMOVED (thay CANCELLED),
--      assignment_role LEAD / SUPPORTING. Assignment bị gỡ lưu removed_at, removed_by (NFR-02).
--   6. Loại Source theo PRD 4.5 (DATA gộp vào DOCUMENT); OTP chỉ VERIFY_EMAIL / RESET_PASSWORD,
--      hết hạn sau 10 phút, tối đa 5 lần nhập sai.
--   7. Thêm quy tắc kiểm tra R18..R23 trong v_rule_violations; v_article_board hiện cả bài DRAFT.
--   8. [FL-01] topic thêm 3 cột: department_id (ban phụ trách = ban của Reporter đề xuất),
--      awaiting_revision (cờ "Trưởng phòng yêu cầu sửa", status vẫn DEPARTMENT_REVIEW),
--      reviewed_by_head_id (Trưởng phòng đã duyệt). Thêm bảng topic_feedback lưu từng vòng yêu cầu sửa
--      (xem mục 2). awaiting_revision / reviewed_by_head_id / topic_feedback dùng từ màn duyệt Topic (Iter2).
--      Tài khoản tự đăng ký (form + OTP hoặc Google) vào AWAITING_APPROVAL, Admin duyệt mới ACTIVE
--      (Admin chỉ cấp REPORTER / DEPARTMENT_HEAD / EDITORIAL_SECRETARY; từ chối -> INACTIVE).
--      DB đã dựng từ bản v2 trước đó: chạy database/fl01_iter1_migration.sql thay vì chạy lại file này.
--   9. [FL-01 Iter2] Thêm bảng notification (thông báo trong hệ thống cho Reporter khi Topic bị yêu cầu
--      sửa / được duyệt ACTIVE / bị REJECTED; chưa gửi email). Reporter chỉ đề xuất Topic cho ban của
--      chính mình, nên topic.department_id luôn = account.department_id của người tạo (khớp R08).
--      DB đã dựng trước đó: chạy thêm database/fl01_iter2_migration.sql (chỉ tạo bảng mới).
-- =====================================================================
-- QUY ƯỚC
--   * Thời gian là DATETIMEOFFSET(0), dữ liệu mẫu dùng giờ Việt Nam (+07:00).
--   * Không dùng ON DELETE CASCADE: giữ lịch sử để truy vết (NFR-02, EC-02).
--     Không xóa người dùng / assignment, chỉ đổi status (INACTIVE / LOCKED / REMOVED).
--   * Comment gắn vị trí: anchor_paragraph đếm từ 1; anchor_start / anchor_end là
--     vị trí ký tự (đếm từ 0) trong đoạn đó, anchor_end không tính.
--   * Số version lưu 1, 2, 3...; hiển thị theo PRD là 1.0, 1.1, 1.2 (cột version_label).
--   * Các quy tắc liên bảng không khai báo được bằng constraint -> view v_rule_violations
--     (phải trả về 0 dòng).
-- =====================================================================

IF DB_ID(N'cms_core') IS NULL
    CREATE DATABASE cms_core;
GO

USE cms_core;
GO

-- Các SET bắt buộc để tạo / ghi vào bảng có filtered index (sqlcmd mặc định tắt QUOTED_IDENTIFIER)
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
GO

DROP VIEW  IF EXISTS v_rule_violations, v_article_timeline, v_article_board,
                     v_public_article, v_topic_team;
DROP TABLE IF EXISTS notification, publication, comment, article_review, article_source,
                     article_version, article, topic_source, source,
                     assignment, topic_feedback, topic_review, topic, account_otp,
                     account, role, department;
GO

-- ---------------------------------------------------------------------
-- 1. DANH MỤC & TÀI KHOẢN
-- ---------------------------------------------------------------------
CREATE TABLE department (
    department_id   INT IDENTITY(1,1) NOT NULL,
    department_name NVARCHAR(100) NOT NULL,
    description     NVARCHAR(MAX) NULL,
    CONSTRAINT department_pkey PRIMARY KEY (department_id),
    CONSTRAINT department_department_name_key UNIQUE (department_name)
);

CREATE TABLE role (
    role_id     INT IDENTITY(1,1) NOT NULL,
    role_name   VARCHAR(30) NOT NULL,
    description NVARCHAR(MAX) NULL,
    CONSTRAINT role_pkey PRIMARY KEY (role_id),
    CONSTRAINT role_role_name_key UNIQUE (role_name),
    -- [v2] thêm ADMIN (PRD 3.6, FR-15)
    CONSTRAINT role_role_name_check
        CHECK (role_name IN ('REPORTER', 'DEPARTMENT_HEAD', 'EDITORIAL_SECRETARY', 'ADMIN'))
);

-- status:
--   PENDING            vừa đăng ký, chưa xác minh email bằng OTP - không đăng nhập được (FR-13)
--   AWAITING_APPROVAL  [FL-01] đã xác minh email (OTP hoặc Google), chờ Admin duyệt và cấp vai trò - không đăng nhập được;
--                      role_id tạm là REPORTER cho tới khi Admin chọn vai trò
--   ACTIVE             đang hoạt động
--   LOCKED             bị Admin khóa (FR-15)
--   INACTIVE           đã nghỉ việc, hoặc [FL-01] bị Admin từ chối khi duyệt đăng ký (giữ lại để truy vết, EC-01)
CREATE TABLE account (
    account_id    INT IDENTITY(1,1) NOT NULL,
    role_id       INT           NOT NULL,                       -- FK
    department_id INT           NULL,                           -- FK [v2] có thể trống: người mới đăng ký, Admin
    username      NVARCHAR(50)  NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,   -- BCrypt, không lưu plaintext (NFR-01)
    email         NVARCHAR(150) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',     -- [v2] mặc định PENDING khi đăng ký
    created_at    DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),   -- [v2]
    CONSTRAINT account_pkey PRIMARY KEY (account_id),
    CONSTRAINT account_username_key UNIQUE (username),
    CONSTRAINT account_email_key UNIQUE (email),
    CONSTRAINT account_status_check CHECK (status IN ('PENDING', 'AWAITING_APPROVAL', 'ACTIVE', 'LOCKED', 'INACTIVE')),   -- [FL-01]
    CONSTRAINT account_role_id_fkey FOREIGN KEY (role_id) REFERENCES role (role_id),
    CONSTRAINT account_department_id_fkey FOREIGN KEY (department_id) REFERENCES department (department_id)
);

-- OTP: chỉ lưu mã băm, hết hạn sau 10 phút, tối đa 5 lần nhập sai, dùng một lần (BR-36, NFR-01).
-- Gửi lại mã: chỉ cho phép khi đã qua 60 giây kể từ last_sent_at (kiểm tra ở ứng dụng).
CREATE TABLE account_otp (
    otp_id        INT IDENTITY(1,1) NOT NULL,
    account_id    INT          NOT NULL,                        -- FK
    purpose       VARCHAR(30)  NOT NULL,
    code_hash     VARCHAR(100) NOT NULL,   -- chỉ lưu mã băm, không lưu mã OTP
    attempt_count INT          NOT NULL DEFAULT 0,
    last_sent_at  DATETIMEOFFSET(0) NOT NULL,
    expires_at    DATETIMEOFFSET(0) NOT NULL,
    used_at       DATETIMEOFFSET(0) NULL,
    CONSTRAINT account_otp_pkey PRIMARY KEY (otp_id),
    -- [v2] bỏ LOGIN_2FA (PRD không có đăng nhập 2 lớp)
    CONSTRAINT account_otp_purpose_check CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    -- [v2] sai quá 5 lần thì mã bị khóa
    CONSTRAINT account_otp_attempt_count_check CHECK (attempt_count BETWEEN 0 AND 5),
    -- [v2] hết hạn đúng 10 phút sau khi gửi
    CONSTRAINT ck_otp_expiry CHECK (expires_at = DATEADD(MINUTE, 10, last_sent_at)),
    CONSTRAINT ck_otp_used CHECK (used_at IS NULL OR (used_at >= last_sent_at AND used_at <= expires_at)),
    CONSTRAINT account_otp_account_id_fkey FOREIGN KEY (account_id) REFERENCES account (account_id)
);

-- ---------------------------------------------------------------------
-- 2. TOPIC & DUYỆT TOPIC
-- ---------------------------------------------------------------------
-- Topic status [v2]:
--   DEPARTMENT_REVIEW  chờ Trưởng phòng duyệt (tạo + gửi là một bước, không dùng PROPOSED);
--                      [FL-01] awaiting_revision = 1: Trưởng phòng yêu cầu sửa, chờ Reporter gửi lại
--   SECRETARY_REVIEW   Trưởng phòng đã duyệt, chờ Thư ký tòa soạn duyệt cuối
--   ACTIVE             dùng được để viết Article (Topic top-down vào thẳng đây)
--   REJECTED           [FL-01] bị Thư ký tòa soạn từ chối (lý do bắt buộc), kết thúc; Reporter đề xuất Topic mới
CREATE TABLE topic (
    topic_id      INT IDENTITY(1,1) NOT NULL,
    created_by    INT           NOT NULL,                       -- FK
    title         NVARCHAR(255) NOT NULL,
    description   NVARCHAR(MAX) NULL,
    category      NVARCHAR(100) NOT NULL,
    rationale     NVARCHAR(MAX) NULL,   -- lý do / căn cứ đề xuất
    expected_date DATE          NULL,
    priority      VARCHAR(10)   NOT NULL DEFAULT 'NORMAL',
    status        VARCHAR(20)   NOT NULL DEFAULT 'DEPARTMENT_REVIEW',
    created_at    DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    updated_at    DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    department_id       INT NULL,                                -- FK [FL-01] ban phụ trách; Topic gửi tới Trưởng phòng của ban này
    awaiting_revision   BIT NOT NULL CONSTRAINT df_topic_awaiting_revision DEFAULT 0,   -- [FL-01] cờ, không phải status
    reviewed_by_head_id INT NULL,                                -- FK [FL-01] Trưởng phòng đã duyệt
    CONSTRAINT topic_pkey PRIMARY KEY (topic_id),
    CONSTRAINT topic_priority_check CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT topic_status_check
        CHECK (status IN ('DEPARTMENT_REVIEW', 'SECRETARY_REVIEW', 'ACTIVE', 'REJECTED')),
    CONSTRAINT ck_topic_dates CHECK (updated_at >= created_at),
    -- [FL-01] cờ yêu cầu sửa chỉ có nghĩa khi Topic còn chờ Trưởng phòng
    CONSTRAINT ck_topic_awaiting_revision CHECK (awaiting_revision = 0 OR status = 'DEPARTMENT_REVIEW'),
    CONSTRAINT topic_created_by_fkey FOREIGN KEY (created_by) REFERENCES account (account_id),
    CONSTRAINT topic_department_id_fkey FOREIGN KEY (department_id) REFERENCES department (department_id),
    CONSTRAINT topic_reviewed_by_head_id_fkey FOREIGN KEY (reviewed_by_head_id) REFERENCES account (account_id)
);

CREATE TABLE topic_review (
    topic_review_id INT IDENTITY(1,1) NOT NULL,
    topic_id        INT          NOT NULL,                      -- FK
    reviewer_id     INT          NOT NULL,                      -- FK
    review_stage    VARCHAR(20)  NOT NULL,
    decision        VARCHAR(20)  NOT NULL,
    feedback        NVARCHAR(MAX) NULL,
    reviewed_at     DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT topic_review_pkey PRIMARY KEY (topic_review_id),
    CONSTRAINT topic_review_review_stage_check CHECK (review_stage IN ('DEPARTMENT', 'SECRETARY')),
    -- [v2] chỉ duyệt hoặc từ chối (FR-03, FR-04). [FL-01] REJECTED chỉ dùng ở cấp SECRETARY
    --      (kiểm tra ở ứng dụng; dữ liệu mẫu T9 cũ còn DEPARTMENT / REJECTED). Yêu cầu sửa -> topic_feedback
    CONSTRAINT topic_review_decision_check CHECK (decision IN ('APPROVED', 'REJECTED')),
    -- [RULE] BR-12: từ chối bắt buộc có feedback
    CONSTRAINT ck_topic_review_feedback
        CHECK (decision = 'APPROVED' OR (feedback IS NOT NULL AND LEN(LTRIM(RTRIM(feedback))) > 0)),
    -- [RULE] mỗi Topic chỉ có một kết quả ở mỗi cấp; [FL-01] các vòng yêu cầu sửa nằm ở topic_feedback
    CONSTRAINT uq_topic_review_stage UNIQUE (topic_id, review_stage),
    CONSTRAINT topic_review_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id),
    CONSTRAINT topic_review_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES account (account_id)
);

-- [FL-01] Ý kiến trên Topic, mỗi lần một dòng (không sửa / xóa):
--   CHANGE_REQUEST  Trưởng phòng yêu cầu sửa (bật topic.awaiting_revision), được nhiều vòng
--   REJECTION       lý do Thư ký tòa soạn từ chối (đi cùng topic_review SECRETARY / REJECTED)
CREATE TABLE topic_feedback (
    topic_feedback_id INT IDENTITY(1,1) NOT NULL,
    topic_id          INT           NOT NULL,                   -- FK
    author_id         INT           NOT NULL,                   -- FK
    feedback_type     VARCHAR(20)   NOT NULL,
    content           NVARCHAR(MAX) NOT NULL,
    created_at        DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT topic_feedback_pkey PRIMARY KEY (topic_feedback_id),
    CONSTRAINT topic_feedback_type_check CHECK (feedback_type IN ('CHANGE_REQUEST', 'REJECTION')),
    -- [RULE] yêu cầu sửa / lý do từ chối bắt buộc có nội dung
    CONSTRAINT ck_topic_feedback_content CHECK (LEN(LTRIM(RTRIM(content))) > 0),
    CONSTRAINT topic_feedback_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id),
    CONSTRAINT topic_feedback_author_id_fkey FOREIGN KEY (author_id) REFERENCES account (account_id)
);

-- [FL-01 Iter2] Thông báo trong hệ thống (chuông trên header của phóng viên)
CREATE TABLE notification (
    notification_id INT IDENTITY(1,1) NOT NULL,
    account_id      INT           NOT NULL,                     -- FK, người nhận
    topic_id        INT           NOT NULL,                     -- FK, bấm thông báo thì mở Topic này
    message         NVARCHAR(500) NOT NULL,
    is_read         BIT           NOT NULL CONSTRAINT df_notification_is_read DEFAULT 0,
    created_at      DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_notification_created_at DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT notification_pkey PRIMARY KEY (notification_id),
    CONSTRAINT notification_account_id_fkey FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT notification_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id)
);
CREATE INDEX ix_notification_account ON notification (account_id, is_read);
CREATE INDEX ix_notification_topic ON notification (topic_id);

-- ---------------------------------------------------------------------
-- 3. ASSIGNMENT (một người - một nhiệm vụ - trên một Topic)
-- ---------------------------------------------------------------------
-- assignment_role [v2 đổi tên theo PRD 14]:
--   DEPARTMENT_HEAD  Thư ký giao Topic cho Trưởng phòng (OQ-04)
--   LEAD             đúng một người đang hoạt động trên mỗi Topic (BR-14)
--   SUPPORTING       nhiều người, chỉ cung cấp Material (BR-22)
-- status [v2 theo PRD 4.6]: ASSIGNED (đang thực hiện) -> COMPLETED (xong phần việc),
--                           hoặc REMOVED (bị thay / gỡ khỏi Topic; không xóa dòng)
CREATE TABLE assignment (
    assignment_id   INT IDENTITY(1,1) NOT NULL,
    topic_id        INT          NOT NULL,                      -- FK
    account_id      INT          NOT NULL,                      -- FK người nhận việc
    assigned_by     INT          NOT NULL,                      -- FK người giao việc
    assignment_role VARCHAR(20)  NOT NULL,
    deadline        DATETIMEOFFSET(0) NULL,   -- có thể để trống lúc đầu (PRD OQ-06)
    requirements    NVARCHAR(MAX) NULL,       -- nhiệm vụ riêng của người này
    status          VARCHAR(15)  NOT NULL DEFAULT 'ASSIGNED',
    assigned_at     DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    completed_at    DATETIMEOFFSET(0) NULL,
    removed_at      DATETIMEOFFSET(0) NULL,   -- [v2] thời điểm gỡ / thay (PRD 14 "thời điểm giao và đóng")
    removed_by      INT          NULL,        -- [v2] FK ai gỡ / thay (NFR-02)
    CONSTRAINT assignment_pkey PRIMARY KEY (assignment_id),
    CONSTRAINT assignment_assignment_role_check
        CHECK (assignment_role IN ('DEPARTMENT_HEAD', 'LEAD', 'SUPPORTING')),
    CONSTRAINT assignment_status_check CHECK (status IN ('ASSIGNED', 'COMPLETED', 'REMOVED')),
    CONSTRAINT ck_assignment_not_self    CHECK (account_id <> assigned_by),
    CONSTRAINT ck_assignment_deadline    CHECK (deadline IS NULL OR deadline > assigned_at),
    -- COMPLETED <=> có completed_at
    CONSTRAINT ck_assignment_completed
        CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL)
            OR (status <> 'COMPLETED' AND completed_at IS NULL)),
    -- [v2] REMOVED <=> có removed_at và removed_by
    CONSTRAINT ck_assignment_removed
        CHECK ((status = 'REMOVED' AND removed_at IS NOT NULL AND removed_by IS NOT NULL)
            OR (status <> 'REMOVED' AND removed_at IS NULL AND removed_by IS NULL)),
    CONSTRAINT ck_assignment_removed_at CHECK (removed_at IS NULL OR removed_at >= assigned_at),
    CONSTRAINT assignment_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id),
    CONSTRAINT assignment_account_id_fkey FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT assignment_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES account (account_id),
    CONSTRAINT assignment_removed_by_fkey FOREIGN KEY (removed_by) REFERENCES account (account_id)
);
-- [RULE] BR-24: mỗi người chỉ có một assignment đang hiệu lực trên mỗi Topic (BR-38: không giữ hai vai trò)
CREATE UNIQUE INDEX uq_assignment_person
    ON assignment (topic_id, account_id) WHERE status <> 'REMOVED';
-- [RULE] BR-14: đúng một Lead đang hoạt động trên mỗi Topic
CREATE UNIQUE INDEX uq_assignment_lead
    ON assignment (topic_id) WHERE assignment_role = 'LEAD' AND status <> 'REMOVED';
-- [RULE] BR-38: một Topic chỉ có một Trưởng phòng đang phụ trách
CREATE UNIQUE INDEX uq_assignment_dept_head
    ON assignment (topic_id) WHERE assignment_role = 'DEPARTMENT_HEAD' AND status <> 'REMOVED';

-- ---------------------------------------------------------------------
-- 4. SOURCE / MATERIAL (nguồn tin, tài liệu, hình ảnh)
-- ---------------------------------------------------------------------
-- source_type theo PRD 4.5. INTERVIEW (ghi chú phỏng vấn) và INTERNAL_DOC chỉ dùng nội bộ,
-- không hiện trên Public Site (BR-35). Chỉ người thêm (added_by) được sửa / xóa Source.
CREATE TABLE source (
    source_id   INT IDENTITY(1,1) NOT NULL,
    added_by    INT           NOT NULL,                         -- FK
    title       NVARCHAR(255) NOT NULL,
    source_type VARCHAR(15)   NOT NULL,
    file_url    NVARCHAR(500) NULL,       -- URL trang web hoặc URL file đã upload (Cloudinary)
    description NVARCHAR(MAX) NULL,       -- với ảnh: ghi rõ nguồn / tác giả ảnh; với phỏng vấn: ghi chú
    uploaded_at DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT source_pkey PRIMARY KEY (source_id),
    CONSTRAINT source_source_type_check
        CHECK (source_type IN ('IMAGE', 'DOCUMENT', 'WEBSITE', 'INTERVIEW', 'INTERNAL_DOC')),
    CONSTRAINT ck_source_url CHECK (source_type NOT IN ('WEBSITE', 'IMAGE') OR file_url IS NOT NULL),
    CONSTRAINT source_added_by_fkey FOREIGN KEY (added_by) REFERENCES account (account_id)
);

CREATE TABLE topic_source (
    topic_source_id INT IDENTITY(1,1) NOT NULL,
    topic_id        INT NOT NULL,                               -- FK
    source_id       INT NOT NULL,                               -- FK
    attached_at     DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    note            NVARCHAR(MAX) NULL,
    CONSTRAINT topic_source_pkey PRIMARY KEY (topic_source_id),
    CONSTRAINT uq_topic_source UNIQUE (topic_id, source_id),
    CONSTRAINT topic_source_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id),
    CONSTRAINT topic_source_source_id_fkey FOREIGN KEY (source_id) REFERENCES source (source_id)
);

-- ---------------------------------------------------------------------
-- 5. ARTICLE & VERSION
-- ---------------------------------------------------------------------
-- Article status [v2 theo PRD mục 9]:
--   DRAFT                Lead đang soạn, chưa có version
--   DEPARTMENT_REVIEW    chờ / đang được Trưởng phòng duyệt
--   REVISION_REQUIRED    bị trả về Lead (bởi Trưởng phòng hoặc Thư ký - xem article_review mới nhất)
--   DEPARTMENT_APPROVED  Trưởng phòng đã duyệt, chờ / đang được Thư ký duyệt cuối
--   APPROVED             Thư ký đã duyệt cuối, chờ quyết định xuất bản
--   SCHEDULED            đã hẹn giờ
--   PUBLISHED            đã xuất bản
-- [v2] Bản đang soạn nằm ở chính bảng này (title, subtitle, sapo, content).
--      Save Draft chỉ UPDATE các cột này; nội dung chỉ sửa được ở DRAFT / REVISION_REQUIRED (BR-28).
CREATE TABLE article (
    article_id   INT IDENTITY(1,1) NOT NULL,
    topic_id     INT           NOT NULL,                        -- FK
    lead_id      INT           NOT NULL,                        -- FK Lead phụ trách bài (đổi khi đổi Lead, EC-01)
    title        NVARCHAR(255) NOT NULL,                        -- bản đang soạn
    subtitle     NVARCHAR(255) NULL,                            -- [v2] bản đang soạn
    sapo         NVARCHAR(MAX) NULL,                            -- [v2] bản đang soạn
    content      NVARCHAR(MAX) NOT NULL DEFAULT N'',            -- [v2] bản đang soạn; các đoạn cách nhau bằng dòng trống
    category     NVARCHAR(100) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    restored_from_version_id INT NULL,   -- [v2] version_id vừa được khôi phục vào bản đang soạn (để ghi chú ở lần nộp sau);
                                         --      không khai báo FK để tránh vòng khóa ngoại article <-> article_version, kiểm tra ở R22
    created_at   DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    updated_at   DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),   -- [v2] lần Save Draft gần nhất
    submitted_at DATETIMEOFFSET(0) NULL,   -- lần nộp GẦN NHẤT (nộp lại sẽ ghi đè)
    completed_at DATETIMEOFFSET(0) NULL,   -- lúc Thư ký duyệt cuối (APPROVED)
    CONSTRAINT article_pkey PRIMARY KEY (article_id),
    CONSTRAINT article_status_check
        CHECK (status IN ('DRAFT', 'DEPARTMENT_REVIEW', 'REVISION_REQUIRED',
                          'DEPARTMENT_APPROVED', 'APPROVED', 'SCHEDULED', 'PUBLISHED')),
    CONSTRAINT ck_article_dates CHECK (updated_at >= created_at),
    CONSTRAINT ck_article_submitted CHECK (status = 'DRAFT' OR submitted_at IS NOT NULL),
    -- completed_at có giá trị <=> bài ở APPROVED / SCHEDULED / PUBLISHED
    CONSTRAINT ck_article_completed
        CHECK ((status IN ('APPROVED', 'SCHEDULED', 'PUBLISHED') AND completed_at IS NOT NULL)
            OR (status NOT IN ('APPROVED', 'SCHEDULED', 'PUBLISHED') AND completed_at IS NULL)),
    -- [v2] chỉ khôi phục khi còn sửa được bài
    CONSTRAINT ck_article_restore
        CHECK (restored_from_version_id IS NULL OR status IN ('DRAFT', 'REVISION_REQUIRED')),
    CONSTRAINT article_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES topic (topic_id),
    CONSTRAINT article_lead_id_fkey FOREIGN KEY (lead_id) REFERENCES account (account_id)
);

-- [v2] Bản chụp cố định, CHỈ tạo khi Submit / Resubmit (BR-27). Không UPDATE / DELETE (trigger chặn).
CREATE TABLE article_version (
    version_id     INT IDENTITY(1,1) NOT NULL,
    article_id     INT           NOT NULL,                      -- FK
    created_by     INT           NOT NULL,                      -- FK Lead nộp bản này
    version_number INT           NOT NULL,                      -- 1, 2, 3...
    version_label  AS (CONCAT('1.', version_number - 1)),       -- [v2] hiển thị theo PRD: 1.0, 1.1, 1.2...
    title          NVARCHAR(255) NOT NULL,
    subtitle       NVARCHAR(255) NULL,
    sapo           NVARCHAR(MAX) NOT NULL,                      -- [v2] bắt buộc khi nộp (FR-07)
    content        NVARCHAR(MAX) NOT NULL,                      -- các đoạn cách nhau bằng dòng trống
    change_summary NVARCHAR(MAX) NULL,                          -- "đã sửa gì"; vd "Sửa theo góp ý của TP", "Khôi phục từ version 1.0"
    created_at     DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT article_version_pkey PRIMARY KEY (version_id),
    CONSTRAINT article_version_version_number_check CHECK (version_number >= 1),
    -- [v2] FR-07: phải có tít, sapo, nội dung
    CONSTRAINT ck_version_required
        CHECK (LEN(LTRIM(RTRIM(title))) > 0 AND LEN(LTRIM(RTRIM(sapo))) > 0 AND LEN(LTRIM(RTRIM(content))) > 0),
    -- [v2] Resubmit phải có ghi chú "đã sửa gì" (PRD 7.3)
    CONSTRAINT ck_version_change_note
        CHECK (version_number = 1 OR (change_summary IS NOT NULL AND LEN(LTRIM(RTRIM(change_summary))) > 0)),
    CONSTRAINT uq_version_number UNIQUE (article_id, version_number),   -- BR-17, BR-18
    CONSTRAINT uq_version_article UNIQUE (article_id, version_id),      -- đích của khóa ngoại kép ở publication
    CONSTRAINT article_version_article_id_fkey FOREIGN KEY (article_id) REFERENCES article (article_id),
    CONSTRAINT article_version_created_by_fkey FOREIGN KEY (created_by) REFERENCES account (account_id)
);

CREATE TABLE article_source (
    article_source_id INT IDENTITY(1,1) NOT NULL,
    article_id        INT NOT NULL,                             -- FK
    source_id         INT NOT NULL,                             -- FK
    attached_at       DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    note              NVARCHAR(MAX) NULL,
    caption           NVARCHAR(300) NULL,                       -- chú thích ảnh
    is_featured       BIT NOT NULL DEFAULT 0,                   -- ảnh đại diện
    display_order     INT NULL,
    CONSTRAINT article_source_pkey PRIMARY KEY (article_source_id),
    CONSTRAINT article_source_display_order_check CHECK (display_order IS NULL OR display_order >= 1),
    CONSTRAINT uq_article_source UNIQUE (article_id, source_id),
    CONSTRAINT article_source_article_id_fkey FOREIGN KEY (article_id) REFERENCES article (article_id),
    CONSTRAINT article_source_source_id_fkey FOREIGN KEY (source_id) REFERENCES source (source_id)
);
-- [RULE] mỗi bài chỉ có một ảnh đại diện; thứ tự hiển thị không trùng
CREATE UNIQUE INDEX uq_article_featured ON article_source (article_id) WHERE is_featured = 1;
CREATE UNIQUE INDEX uq_article_source_order
    ON article_source (article_id, display_order) WHERE display_order IS NOT NULL;

-- ---------------------------------------------------------------------
-- 6. REVIEW BÀI & COMMENT
-- ---------------------------------------------------------------------
-- Một dòng = một lượt duyệt của một cấp trên một version.
-- decision NULL = reviewer đang đọc và ghi comment, chưa quyết định (Review tạo khi mở bài, PRD 4.7).
-- review_kind [v2]:
--   REVIEW            lượt duyệt bình thường (Trưởng phòng / Thư ký)
--   RETURN_TO_LEAD    Thư ký trả bài APPROVED không đăng nữa (FR-17)
--   CANCEL_SCHEDULE   Thư ký hủy lịch đăng và trả bài (FR-18)
CREATE TABLE article_review (
    review_id    INT IDENTITY(1,1) NOT NULL,
    version_id   INT          NOT NULL,                         -- FK
    reviewer_id  INT          NOT NULL,                         -- FK
    review_stage VARCHAR(20)  NOT NULL,
    review_kind  VARCHAR(20)  NOT NULL DEFAULT 'REVIEW',        -- [v2]
    decision     VARCHAR(20)  NULL,
    feedback     NVARCHAR(MAX) NULL,
    reviewed_at  DATETIMEOFFSET(0) NULL,
    CONSTRAINT article_review_pkey PRIMARY KEY (review_id),
    CONSTRAINT article_review_review_stage_check CHECK (review_stage IN ('DEPARTMENT', 'SECRETARY')),
    CONSTRAINT article_review_review_kind_check
        CHECK (review_kind IN ('REVIEW', 'RETURN_TO_LEAD', 'CANCEL_SCHEDULE')),
    -- [v2] tên theo PRD 4.7
    CONSTRAINT article_review_decision_check CHECK (decision IN ('APPROVED', 'REQUEST_REVISION')),
    -- decision và reviewed_at cùng trống (đang review) hoặc cùng có giá trị
    CONSTRAINT ck_review_decided
        CHECK ((decision IS NULL AND reviewed_at IS NULL)
            OR (decision IS NOT NULL AND reviewed_at IS NOT NULL)),
    -- [RULE] BR-12, BR-30: yêu cầu sửa / trả bài bắt buộc có feedback
    CONSTRAINT ck_review_feedback
        CHECK (decision IS NULL OR decision <> 'REQUEST_REVISION'
               OR (feedback IS NOT NULL AND LEN(LTRIM(RTRIM(feedback))) > 0)),
    -- [v2] trả bài / hủy lịch: chỉ Thư ký, kết quả luôn REQUEST_REVISION
    CONSTRAINT ck_review_kind
        CHECK (review_kind = 'REVIEW'
               OR (review_stage = 'SECRETARY' AND decision = 'REQUEST_REVISION')),
    CONSTRAINT article_review_version_id_fkey FOREIGN KEY (version_id) REFERENCES article_version (version_id),
    CONSTRAINT article_review_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES account (account_id)
);
-- [v2] thay cho UNIQUE (version_id, review_stage) của bản trước:
--   mỗi version, mỗi cấp chỉ có một lượt đang mở và một lượt APPROVED;
--   Thư ký vẫn ghi được lượt trả bài / hủy lịch sau khi đã APPROVED.
CREATE UNIQUE INDEX uq_review_open
    ON article_review (version_id, review_stage) WHERE decision IS NULL;
CREATE UNIQUE INDEX uq_review_approved
    ON article_review (version_id, review_stage) WHERE decision = 'APPROVED';

CREATE TABLE comment (
    comment_id        INT IDENTITY(1,1) NOT NULL,
    review_id         INT NOT NULL,                             -- FK
    created_by        INT NOT NULL,                             -- FK người viết (FR-11)
    parent_comment_id INT NULL,                                 -- FK trả lời (PRD 3.1 "Phản hồi Comment"; OQ-11 chưa chốt)
    anchor_field      VARCHAR(10) NULL,
    anchor_paragraph  INT NULL,
    anchor_start      INT NULL,
    anchor_end        INT NULL,
    quoted_text       NVARCHAR(MAX) NULL,   -- đoạn văn được đánh dấu, để định vị lại khi sang version mới
    content           NVARCHAR(MAX) NOT NULL,
    status            VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at        DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT comment_pkey PRIMARY KEY (comment_id),
    CONSTRAINT comment_anchor_field_check CHECK (anchor_field IN ('TITLE', 'SUBTITLE', 'SAPO', 'CONTENT')),
    CONSTRAINT comment_anchor_paragraph_check CHECK (anchor_paragraph IS NULL OR anchor_paragraph >= 1),
    CONSTRAINT comment_anchor_start_check CHECK (anchor_start IS NULL OR anchor_start >= 0),
    CONSTRAINT comment_status_check CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_comment_anchor_range
        CHECK (anchor_start IS NULL OR anchor_end IS NULL OR anchor_end > anchor_start),
    -- comment chung (không gắn vị trí) thì các cột anchor đều NULL; gắn vị trí thì có anchor_field
    CONSTRAINT ck_comment_anchor_field
        CHECK (anchor_field IS NOT NULL OR
               (anchor_paragraph IS NULL AND anchor_start IS NULL AND anchor_end IS NULL)),
    CONSTRAINT comment_review_id_fkey FOREIGN KEY (review_id) REFERENCES article_review (review_id),
    CONSTRAINT comment_created_by_fkey FOREIGN KEY (created_by) REFERENCES account (account_id),
    CONSTRAINT comment_parent_comment_id_fkey FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id)
);

-- ---------------------------------------------------------------------
-- 7. XUẤT BẢN
-- ---------------------------------------------------------------------
-- Mỗi bài có tối đa một publication (article_id UNIQUE). Hủy lịch đăng thì XÓA dòng (BR-31, OQ-10).
-- version_id ghim đúng bản được Final Approve; trang công khai đọc bản này.
-- published_by với bài hẹn giờ = Thư ký đã đặt lịch (hệ thống tự đăng khi đến giờ, nếu bài vẫn SCHEDULED).
CREATE TABLE publication (
    publication_id   INT IDENTITY(1,1) NOT NULL,
    article_id       INT         NOT NULL,                      -- FK (UNIQUE)
    version_id       INT         NOT NULL,                      -- FK
    published_by     INT         NOT NULL,                      -- FK
    publication_type VARCHAR(10) NOT NULL,
    scheduled_at     DATETIMEOFFSET(0) NULL,
    published_at     DATETIMEOFFSET(0) NULL,
    status           VARCHAR(10) NOT NULL,
    CONSTRAINT publication_pkey PRIMARY KEY (publication_id),
    CONSTRAINT publication_article_id_key UNIQUE (article_id),
    CONSTRAINT publication_publication_type_check CHECK (publication_type IN ('IMMEDIATE', 'SCHEDULED')),
    CONSTRAINT publication_status_check CHECK (status IN ('SCHEDULED', 'PUBLISHED')),
    -- hẹn giờ <=> có scheduled_at
    CONSTRAINT ck_publication_schedule
        CHECK ((publication_type = 'SCHEDULED' AND scheduled_at IS NOT NULL)
            OR (publication_type <> 'SCHEDULED' AND scheduled_at IS NULL)),
    -- PUBLISHED <=> có published_at
    CONSTRAINT ck_publication_published
        CHECK ((status = 'PUBLISHED' AND published_at IS NOT NULL)
            OR (status <> 'PUBLISHED' AND published_at IS NULL)),
    CONSTRAINT ck_publication_immediate CHECK (publication_type = 'SCHEDULED' OR status = 'PUBLISHED'),
    CONSTRAINT ck_publication_order
        CHECK (published_at IS NULL OR scheduled_at IS NULL OR published_at >= scheduled_at),
    CONSTRAINT publication_article_id_fkey FOREIGN KEY (article_id) REFERENCES article (article_id),
    -- khóa ngoại kép: version phải thuộc đúng article này
    CONSTRAINT fk_publication_version
        FOREIGN KEY (article_id, version_id) REFERENCES article_version (article_id, version_id),
    CONSTRAINT publication_published_by_fkey FOREIGN KEY (published_by) REFERENCES account (account_id)
);
GO

-- [v2] BR-27: version không bao giờ bị sửa hoặc xóa
CREATE TRIGGER trg_article_version_immutable ON article_version
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 50027, N'article_version là bản chụp cố định: không được sửa hoặc xóa (BR-27).', 1;
END;
GO

-- ---------------------------------------------------------------------
-- 8. INDEX CHO KHÓA NGOẠI (SQL Server không tự tạo)
-- ---------------------------------------------------------------------
CREATE INDEX ix_account_role          ON account (role_id);
CREATE INDEX ix_account_department    ON account (department_id);
CREATE INDEX ix_otp_account           ON account_otp (account_id);
CREATE INDEX ix_topic_created_by      ON topic (created_by);
CREATE INDEX ix_topic_status          ON topic (status);
CREATE INDEX ix_topic_department      ON topic (department_id);         -- [FL-01]
CREATE INDEX ix_topic_reviewed_by_head ON topic (reviewed_by_head_id);  -- [FL-01]
CREATE INDEX ix_topic_feedback_topic  ON topic_feedback (topic_id);     -- [FL-01]
CREATE INDEX ix_topic_feedback_author ON topic_feedback (author_id);    -- [FL-01]
CREATE INDEX ix_topic_review_reviewer ON topic_review (reviewer_id);
CREATE INDEX ix_assignment_account    ON assignment (account_id);
CREATE INDEX ix_assignment_assigned_by ON assignment (assigned_by);
CREATE INDEX ix_source_added_by       ON source (added_by);
CREATE INDEX ix_topic_source_source   ON topic_source (source_id);
CREATE INDEX ix_article_topic         ON article (topic_id);
CREATE INDEX ix_article_lead          ON article (lead_id);
CREATE INDEX ix_article_status        ON article (status);
CREATE INDEX ix_version_created_by    ON article_version (created_by);
CREATE INDEX ix_article_source_source ON article_source (source_id);
CREATE INDEX ix_review_version        ON article_review (version_id);
CREATE INDEX ix_review_reviewer       ON article_review (reviewer_id);
CREATE INDEX ix_comment_review        ON comment (review_id);
CREATE INDEX ix_comment_parent        ON comment (parent_comment_id);
CREATE INDEX ix_comment_created_by    ON comment (created_by);
CREATE INDEX ix_publication_version   ON publication (version_id);
CREATE INDEX ix_publication_by        ON publication (published_by);
GO

-- ---------------------------------------------------------------------
-- 9. GHI CHÚ CHO CÔNG CỤ XEM DB (SSMS: Properties > Extended Properties; Azure Data Studio)
-- ---------------------------------------------------------------------
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Giao việc trên Topic: Thư ký -> Trưởng phòng (DEPARTMENT_HEAD), Trưởng phòng -> Reporter (LEAD / SUPPORTING)',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'assignment';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Người giao. Topic đề xuất được duyệt: hệ thống tự tạo Lead (người đề xuất) và DEPARTMENT_HEAD (Trưởng phòng đã duyệt), assigned_by = Thư ký đã duyệt',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'assignment', @level2type = N'COLUMN', @level2name = N'assigned_by';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'UC Mark Assignment Completed: Supporting đánh dấu xong phần việc',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'assignment', @level2type = N'COLUMN', @level2name = N'completed_at';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Bản đang soạn của Article; Save Draft chỉ cập nhật cột này, không tạo version',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'article', @level2type = N'COLUMN', @level2name = N'content';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Quy ước: lúc Thư ký duyệt cuối (status APPROVED); xóa về NULL khi bài bị trả về',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'article', @level2type = N'COLUMN', @level2name = N'completed_at';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Lần nộp gần nhất; lịch sử nộp xem ở article_version.created_at',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'article', @level2type = N'COLUMN', @level2name = N'submitted_at';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Các đoạn văn cách nhau bằng một dòng trống; anchor_paragraph của comment đếm theo đoạn này',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'article_version', @level2type = N'COLUMN', @level2name = N'content';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'NULL = đang review, chưa quyết định',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'article_review', @level2type = N'COLUMN', @level2name = N'decision';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Bản sao đoạn văn được đánh dấu; dùng để định vị lại khi bài sang version mới',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'comment', @level2type = N'COLUMN', @level2name = N'quoted_text';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Bản được xuất bản; trang công khai đọc bản này, không đọc bản đang soạn',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'publication', @level2type = N'COLUMN', @level2name = N'version_id';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'Thư ký xuất bản; với bài hẹn giờ là Thư ký đã đặt lịch',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'publication', @level2type = N'COLUMN', @level2name = N'published_by';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'[FL-01] Ban phụ trách do Reporter chọn khi đề xuất; category = tên ban bỏ chữ "Phòng "',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'topic', @level2type = N'COLUMN', @level2name = N'department_id';
EXEC sys.sp_addextendedproperty @name = N'MS_Description', @value = N'[FL-01] 1 = Trưởng phòng yêu cầu sửa, chờ Reporter gửi lại; status vẫn DEPARTMENT_REVIEW',
     @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'topic', @level2type = N'COLUMN', @level2name = N'awaiting_revision';
GO

-- =====================================================================
-- VIEW (T-SQL). Mỗi CREATE VIEW phải là câu lệnh đầu tiên của một batch => có GO.
-- =====================================================================

-- Đội của từng Topic
CREATE VIEW v_topic_team AS
SELECT t.topic_id, t.title, t.status AS topic_status,
       dh.full_name AS department_head, ld.full_name AS lead_reporter,
       (SELECT COUNT(*) FROM assignment s
         WHERE s.topic_id = t.topic_id AND s.assignment_role = 'SUPPORTING'
           AND s.status <> 'REMOVED') AS supporting_reporters,
       (SELECT COUNT(*) FROM article x WHERE x.topic_id = t.topic_id) AS articles
FROM topic t
LEFT JOIN assignment adh ON adh.topic_id = t.topic_id
                        AND adh.assignment_role = 'DEPARTMENT_HEAD' AND adh.status <> 'REMOVED'
LEFT JOIN account dh     ON dh.account_id = adh.account_id
LEFT JOIN assignment ald ON ald.topic_id = t.topic_id
                        AND ald.assignment_role = 'LEAD' AND ald.status <> 'REMOVED'
LEFT JOIN account ld     ON ld.account_id = ald.account_id;
GO

-- Bảng điều phối bài viết (NFR-04: bài ở bước nào, ai đang giữ, version hiện tại, số vòng sửa)
-- [v2] LEFT JOIN version để hiện cả bài DRAFT (chưa có version)
CREATE VIEW v_article_board AS
SELECT a.article_id, a.title, t.title AS topic_title, a.status,
       v.version_number AS latest_version_number, v.version_label AS latest_version,
       CASE WHEN a.status IN ('DRAFT', 'REVISION_REQUIRED')                          THEN 'Lead Reporter'
            WHEN a.status = 'DEPARTMENT_REVIEW'                                      THEN 'Department Head'
            WHEN a.status IN ('DEPARTMENT_APPROVED', 'APPROVED', 'SCHEDULED')        THEN 'Editorial Secretary'
            ELSE '-' END AS held_by_role,
       CASE WHEN a.status IN ('DRAFT', 'REVISION_REQUIRED') THEN ldr.full_name
            WHEN a.status = 'DEPARTMENT_REVIEW'             THEN dh.full_name END AS held_by_name,
       lr.review_stage AS last_review_stage, lr.review_kind AS last_review_kind,
       lr.decision AS last_decision, lr.reviewed_at AS last_reviewed_at,
       (SELECT COUNT(*)
          FROM article_review r
          JOIN article_version av ON av.version_id = r.version_id
         WHERE av.article_id = a.article_id AND r.decision = 'REQUEST_REVISION') AS revision_rounds,
       (SELECT COUNT(*)
          FROM comment c
          JOIN article_review r   ON r.review_id = c.review_id
          JOIN article_version av ON av.version_id = r.version_id
         WHERE av.article_id = a.article_id
           AND c.parent_comment_id IS NULL AND c.status = 'OPEN') AS open_comments,
       a.updated_at AS draft_saved_at
FROM article a
JOIN topic t       ON t.topic_id = a.topic_id
JOIN account ldr   ON ldr.account_id = a.lead_id
LEFT JOIN article_version v ON v.article_id = a.article_id
     AND v.version_number = (SELECT MAX(version_number) FROM article_version WHERE article_id = a.article_id)
LEFT JOIN assignment adh ON adh.topic_id = a.topic_id
                        AND adh.assignment_role = 'DEPARTMENT_HEAD' AND adh.status <> 'REMOVED'
LEFT JOIN account dh ON dh.account_id = adh.account_id
LEFT JOIN article_review lr ON lr.review_id =
     (SELECT MAX(r.review_id)
        FROM article_review r
        JOIN article_version av ON av.version_id = r.version_id
       WHERE av.article_id = a.article_id AND r.decision IS NOT NULL);
GO

-- Dòng thời gian của bài
CREATE VIEW v_article_timeline AS
SELECT e.article_id, e.event_at, acc.full_name AS actor, r.role_name AS actor_role, e.[event], e.detail
FROM (
    SELECT a.article_id, a.created_at AS event_at, a.lead_id AS actor_id,
           CAST('CREATED' AS NVARCHAR(100)) AS [event], CAST(N'Tạo bài từ Topic' AS NVARCHAR(MAX)) AS detail
      FROM article a
    UNION ALL
    SELECT v.article_id, v.created_at, v.created_by,
           CASE WHEN v.version_number = 1 THEN 'SUBMITTED' ELSE 'RESUBMITTED' END,
           CONCAT('v', v.version_label, ': ', COALESCE(v.change_summary, ''))
      FROM article_version v
    UNION ALL
    SELECT v.article_id, rv.reviewed_at, rv.reviewer_id,
           CASE WHEN rv.review_kind = 'REVIEW' THEN CONCAT('REVIEW_', rv.review_stage, '_', rv.decision)
                ELSE rv.review_kind END,
           CONCAT('v', v.version_label, ': ', COALESCE(rv.feedback, ''))
      FROM article_review rv
      JOIN article_version v ON v.version_id = rv.version_id
     WHERE rv.decision IS NOT NULL
    UNION ALL
    SELECT v.article_id, c.created_at, c.created_by,
           CASE WHEN c.parent_comment_id IS NULL THEN 'COMMENT' ELSE 'REPLY' END,
           c.content
      FROM comment c
      JOIN article_review rv  ON rv.review_id = c.review_id
      JOIN article_version v  ON v.version_id = rv.version_id
    UNION ALL
    SELECT p.article_id, p.scheduled_at, p.published_by,
           'PUBLISH_SCHEDULED_FOR', N'Hẹn xuất bản'
      FROM publication p
     WHERE p.scheduled_at IS NOT NULL
    UNION ALL
    SELECT p.article_id, p.published_at, p.published_by,
           'PUBLISHED', CASE p.publication_type WHEN 'IMMEDIATE' THEN N'Xuất bản ngay' ELSE N'Tự động xuất bản theo lịch' END
      FROM publication p
     WHERE p.published_at IS NOT NULL
) e
JOIN account acc ON acc.account_id = e.actor_id
JOIN role r      ON r.role_id = acc.role_id;
GO

-- Trang công khai: chỉ bài PUBLISHED, đọc đúng version đã Final Approve (BR-20, BR-31)
CREATE VIEW v_public_article AS
SELECT a.article_id, v.version_id, v.title, v.subtitle, v.sapo, v.content, a.category, p.published_at,
       ld.full_name AS author, img.file_url AS featured_image_url, asr.caption AS featured_image_caption
FROM publication p
JOIN article a          ON a.article_id = p.article_id AND a.status = 'PUBLISHED'
JOIN article_version v  ON v.version_id = p.version_id
JOIN account ld         ON ld.account_id = a.lead_id
LEFT JOIN article_source asr ON asr.article_id = a.article_id AND asr.is_featured = 1
LEFT JOIN source img         ON img.source_id = asr.source_id
WHERE p.status = 'PUBLISHED';
GO

-- Kiểm tra quy tắc liên bảng (phải trả về 0 dòng)
CREATE VIEW v_rule_violations AS
-- R01 BR-21 / EC-01: bài chỉ do Lead của Topic phụ trách; bài chưa đăng phải thuộc Lead đang hoạt động
--     (đổi Lead thì chuyển các bài chưa PUBLISHED sang Lead mới)
SELECT 'R01' AS rule_code, N'Article không do Lead (đang hoạt động) của Topic phụ trách' AS description,
       'article' AS table_name, a.article_id AS row_id
  FROM article a
 WHERE NOT EXISTS (SELECT 1 FROM assignment s
                    WHERE s.topic_id = a.topic_id AND s.account_id = a.lead_id
                      AND s.assignment_role = 'LEAD')
    OR (a.status <> 'PUBLISHED'
        AND NOT EXISTS (SELECT 1 FROM assignment s
                         WHERE s.topic_id = a.topic_id AND s.account_id = a.lead_id
                           AND s.assignment_role = 'LEAD' AND s.status <> 'REMOVED'))
UNION ALL
-- R02: Topic chưa ACTIVE thì chưa được có Article (PRD mục 10)
SELECT 'R02', N'Article thuộc Topic chưa ACTIVE', 'article', a.article_id
  FROM article a JOIN topic t ON t.topic_id = a.topic_id
 WHERE t.status <> 'ACTIVE'
UNION ALL
-- R03 BR-07 / FR-09: Thư ký chỉ review bản đã được Trưởng phòng duyệt
SELECT 'R03', N'Thư ký review bản chưa được Trưởng phòng duyệt', 'article_review', r.review_id
  FROM article_review r
 WHERE r.review_stage = 'SECRETARY'
   AND NOT EXISTS (SELECT 1 FROM article_review d
                    WHERE d.version_id = r.version_id AND d.review_stage = 'DEPARTMENT'
                      AND d.decision = 'APPROVED')
UNION ALL
-- R04 NFR-03: bài DEPARTMENT_APPROVED phải có bản mới nhất đã được Trưởng phòng duyệt
SELECT 'R04', N'DEPARTMENT_APPROVED nhưng bản mới nhất chưa được Trưởng phòng duyệt', 'article', a.article_id
  FROM article a
 WHERE a.status = 'DEPARTMENT_APPROVED'
   AND NOT EXISTS (SELECT 1 FROM article_review d JOIN article_version v ON v.version_id = d.version_id
                    WHERE v.article_id = a.article_id AND d.review_stage = 'DEPARTMENT' AND d.decision = 'APPROVED'
                      AND v.version_number = (SELECT MAX(version_number) FROM article_version WHERE article_id = a.article_id))
UNION ALL
-- R05 BR-19 / NFR-03: bài APPROVED/SCHEDULED/PUBLISHED phải có Thư ký duyệt cuối đúng bản sẽ đăng
SELECT 'R05', N'Bài đã duyệt/hẹn giờ/xuất bản nhưng bản được đăng chưa có duyệt cuối của Thư ký', 'article', a.article_id
  FROM article a
  LEFT JOIN publication p ON p.article_id = a.article_id
 WHERE a.status IN ('APPROVED', 'SCHEDULED', 'PUBLISHED')
   AND NOT EXISTS (SELECT 1 FROM article_review r
                    WHERE r.review_stage = 'SECRETARY' AND r.decision = 'APPROVED'
                      AND r.version_id = COALESCE(p.version_id,
                          (SELECT version_id FROM article_version v
                            WHERE v.article_id = a.article_id
                              AND v.version_number = (SELECT MAX(version_number) FROM article_version
                                                       WHERE article_id = a.article_id))))
UNION ALL
-- R06: trạng thái Article và Publication phải khớp nhau
SELECT 'R06', N'Trạng thái Article và Publication không khớp', 'article', a.article_id
  FROM article a
  LEFT JOIN publication p ON p.article_id = a.article_id
 WHERE (a.status = 'SCHEDULED' AND COALESCE(p.status, '-') <> 'SCHEDULED')
    OR (a.status = 'PUBLISHED' AND COALESCE(p.status, '-') <> 'PUBLISHED')
    OR (a.status NOT IN ('SCHEDULED', 'PUBLISHED') AND p.publication_id IS NOT NULL)
UNION ALL
-- R07: người duyệt Topic đúng vai trò (Trưởng phòng ở cấp phòng, Thư ký ở cấp cuối)
SELECT 'R07', N'Người duyệt Topic không đúng vai trò của cấp duyệt', 'topic_review', tr.topic_review_id
  FROM topic_review tr JOIN account ac ON ac.account_id = tr.reviewer_id JOIN role ro ON ro.role_id = ac.role_id
 WHERE (tr.review_stage = 'DEPARTMENT' AND ro.role_name <> 'DEPARTMENT_HEAD')
    OR (tr.review_stage = 'SECRETARY'  AND ro.role_name <> 'EDITORIAL_SECRETARY')
UNION ALL
-- R08 [đề xuất, PRD OQ-09 chưa chốt]: Trưởng phòng chỉ duyệt Topic của phòng mình
SELECT 'R08', N'Trưởng phòng duyệt Topic của phòng khác', 'topic_review', tr.topic_review_id
  FROM topic_review tr
  JOIN account rv ON rv.account_id = tr.reviewer_id
  JOIN topic t    ON t.topic_id = tr.topic_id
  JOIN account cr ON cr.account_id = t.created_by
 WHERE tr.review_stage = 'DEPARTMENT' AND rv.department_id <> cr.department_id
UNION ALL
-- R09: người duyệt Article đúng vai trò; Trưởng phòng phải là người phụ trách Topic đó
SELECT 'R09', N'Người duyệt Article không đúng vai trò / không phụ trách Topic', 'article_review', r.review_id
  FROM article_review r
  JOIN article_version v ON v.version_id = r.version_id
  JOIN article a         ON a.article_id = v.article_id
  JOIN account ac        ON ac.account_id = r.reviewer_id
  JOIN role ro           ON ro.role_id = ac.role_id
 WHERE (r.review_stage = 'SECRETARY' AND ro.role_name <> 'EDITORIAL_SECRETARY')
    OR (r.review_stage = 'DEPARTMENT'
        AND (ro.role_name <> 'DEPARTMENT_HEAD'
             OR NOT EXISTS (SELECT 1 FROM assignment s
                             WHERE s.topic_id = a.topic_id AND s.account_id = r.reviewer_id
                               AND s.assignment_role = 'DEPARTMENT_HEAD')))
UNION ALL
-- R10 BR-04 / BR-05 / BR-26: Topic ACTIVE hợp lệ (đề xuất: đủ 2 cấp duyệt; top-down: do Thư ký tạo)
SELECT 'R10', N'Topic ACTIVE không có đường đi hợp lệ (thiếu duyệt 2 cấp hoặc không do Thư ký tạo)', 'topic', t.topic_id
  FROM topic t
  JOIN account cr ON cr.account_id = t.created_by
  JOIN role ro    ON ro.role_id = cr.role_id
 WHERE t.status = 'ACTIVE'
   AND ro.role_name = 'REPORTER'
   AND NOT (EXISTS (SELECT 1 FROM topic_review x WHERE x.topic_id = t.topic_id
                     AND x.review_stage = 'DEPARTMENT' AND x.decision = 'APPROVED')
        AND EXISTS (SELECT 1 FROM topic_review x WHERE x.topic_id = t.topic_id
                     AND x.review_stage = 'SECRETARY' AND x.decision = 'APPROVED'))
UNION ALL
-- R11: người tạo Topic chỉ là Reporter hoặc Thư ký (OQ-07; Admin không tham gia biên tập - BR-37)
SELECT 'R11', N'Topic do người không phải Reporter / Thư ký tạo', 'topic', t.topic_id
  FROM topic t JOIN account cr ON cr.account_id = t.created_by JOIN role ro ON ro.role_id = cr.role_id
 WHERE ro.role_name NOT IN ('REPORTER', 'EDITORIAL_SECRETARY')
UNION ALL
-- R12 BR-26: mọi Topic ACTIVE phải có Trưởng phòng phụ trách (ngoại lệ: Topic top-down còn trong
--     danh sách "Topic chưa giao" - do Thư ký tạo và chưa có ai; PRD 7.2, EC-09)
SELECT 'R12', N'Topic ACTIVE do Reporter đề xuất nhưng chưa có Trưởng phòng phụ trách', 'topic', t.topic_id
  FROM topic t
  JOIN account cr ON cr.account_id = t.created_by JOIN role ro ON ro.role_id = cr.role_id
 WHERE t.status = 'ACTIVE' AND ro.role_name = 'REPORTER'
   AND NOT EXISTS (SELECT 1 FROM assignment s WHERE s.topic_id = t.topic_id
                    AND s.assignment_role = 'DEPARTMENT_HEAD' AND s.status <> 'REMOVED')
UNION ALL
-- R13: vai trò trong Assignment khớp vai trò tài khoản; người giao đúng thẩm quyền
SELECT 'R13', N'Assignment sai vai trò người nhận hoặc người giao', 'assignment', s.assignment_id
  FROM assignment s
  JOIN account rcv ON rcv.account_id = s.account_id JOIN role rr ON rr.role_id = rcv.role_id
  JOIN account gv  ON gv.account_id = s.assigned_by JOIN role gr ON gr.role_id = gv.role_id
 WHERE (s.assignment_role = 'DEPARTMENT_HEAD' AND (rr.role_name <> 'DEPARTMENT_HEAD' OR gr.role_name <> 'EDITORIAL_SECRETARY'))
    OR (s.assignment_role IN ('LEAD', 'SUPPORTING')
        AND (rr.role_name <> 'REPORTER' OR gr.role_name NOT IN ('DEPARTMENT_HEAD', 'EDITORIAL_SECRETARY')))
UNION ALL
-- R14 BR-22: chỉ người từng là Lead của Topic mới nộp version (Supporting không sửa bài)
SELECT 'R14', N'Version do người không phải Lead của Topic tạo', 'article_version', v.version_id
  FROM article_version v JOIN article a ON a.article_id = v.article_id
 WHERE NOT EXISTS (SELECT 1 FROM assignment s
                    WHERE s.topic_id = a.topic_id AND s.account_id = v.created_by
                      AND s.assignment_role = 'LEAD')
UNION ALL
-- R15 BR-17: số version liên tục 1..n
SELECT 'R15', N'Số version của Article không liên tục', 'article', v.article_id
  FROM article_version v
 GROUP BY v.article_id
HAVING MAX(v.version_number) <> COUNT(*)
UNION ALL
-- R16: ảnh đại diện phải là Source loại IMAGE
SELECT 'R16', N'Ảnh đại diện không phải Source loại IMAGE', 'article_source', x.article_source_id
  FROM article_source x JOIN source s ON s.source_id = x.source_id
 WHERE x.is_featured = 1 AND s.source_type <> 'IMAGE'
UNION ALL
-- R17: comment gốc do chính người review viết; trả lời phải nằm cùng lượt review với comment gốc
SELECT 'R17', N'Comment sai người viết hoặc trả lời sang lượt review khác', 'comment', c.comment_id
  FROM comment c
  JOIN article_review r ON r.review_id = c.review_id
  LEFT JOIN comment p   ON p.comment_id = c.parent_comment_id
 WHERE (c.parent_comment_id IS NULL AND c.created_by <> r.reviewer_id)
    OR (c.parent_comment_id IS NOT NULL AND p.review_id <> c.review_id)
UNION ALL
-- R18 [v2] BR-27 / PRD 9: bài DRAFT chưa có version; bài đã nộp phải có ít nhất một version
SELECT 'R18', N'DRAFT đã có version, hoặc bài đã nộp nhưng chưa có version', 'article', a.article_id
  FROM article a
 WHERE (a.status = 'DRAFT' AND EXISTS (SELECT 1 FROM article_version v WHERE v.article_id = a.article_id))
    OR (a.status <> 'DRAFT' AND NOT EXISTS (SELECT 1 FROM article_version v WHERE v.article_id = a.article_id))
UNION ALL
-- R19 [v2] FR-07: bài đã nộp phải có ít nhất một Source và mọi ảnh trong bài có chú thích
SELECT 'R19', N'Bài đã nộp nhưng không có Source, hoặc có ảnh thiếu chú thích', 'article', a.article_id
  FROM article a
 WHERE a.status <> 'DRAFT'
   AND (NOT EXISTS (SELECT 1 FROM article_source x WHERE x.article_id = a.article_id)
        OR EXISTS (SELECT 1 FROM article_source x JOIN source s ON s.source_id = x.source_id
                    WHERE x.article_id = a.article_id AND s.source_type = 'IMAGE'
                      AND (x.caption IS NULL OR LEN(LTRIM(RTRIM(x.caption))) = 0)))
UNION ALL
-- R20 [v2]: Trưởng phòng phải thuộc một phòng ban (Admin gán phòng ban - FR-15)
SELECT 'R20', N'Trưởng phòng chưa được gán phòng ban', 'account', ac.account_id
  FROM account ac JOIN role ro ON ro.role_id = ac.role_id
 WHERE ro.role_name = 'DEPARTMENT_HEAD' AND ac.department_id IS NULL
UNION ALL
-- R21 [v2] FR-12: chỉ Thư ký tòa soạn được xuất bản / đặt lịch
SELECT 'R21', N'Người xuất bản không phải Thư ký tòa soạn', 'publication', p.publication_id
  FROM publication p JOIN account ac ON ac.account_id = p.published_by JOIN role ro ON ro.role_id = ac.role_id
 WHERE ro.role_name <> 'EDITORIAL_SECRETARY'
UNION ALL
-- R22 [v2]: version được khôi phục phải là version của chính bài đó
SELECT 'R22', N'restored_from_version_id không phải version của bài này', 'article', a.article_id
  FROM article a
 WHERE a.restored_from_version_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM article_version v
                    WHERE v.version_id = a.restored_from_version_id AND v.article_id = a.article_id)
UNION ALL
-- R23 [v2] EC-01: tài khoản không còn ACTIVE thì không được giữ assignment đang hiệu lực
SELECT 'R23', N'Tài khoản không ACTIVE vẫn còn assignment đang hiệu lực', 'assignment', s.assignment_id
  FROM assignment s JOIN account ac ON ac.account_id = s.account_id
 WHERE s.status = 'ASSIGNED' AND ac.status <> 'ACTIVE';
GO

-- ---------------------------------------------------------------------
-- 12. DỮ LIỆU MẪU (hư cấu, ngày giờ quanh 07/10/2026, giờ Việt Nam +07:00)
-- Mỗi dòng có chú thích khóa tượng trưng ở cuối để dễ đối chiếu giữa các bảng.
-- Câu chuyện chính:
--   T1  Đề xuất -> TP duyệt -> Thư ký duyệt -> ACTIVE; 3 Article: A1 xuất bản sau 2 vòng sửa (v1.0 -> v1.2),
--       A2 đang nháp (chưa có version), A9 xuất bản thẳng
--   T2  Đề xuất được duyệt; A3 đã hẹn giờ (28/11/2026), A4 đã duyệt chờ xuất bản
--   T3  Top-down do Thư ký tạo; A5 chờ Thư ký (Thư ký đang mở review), A6 chờ Trưởng phòng;
--       1 Supporting bị gỡ khỏi Topic
--   T4  Lead cũ nghỉ việc, Lead mới tiếp quản; A7 đang bị trả về (Lead mới đang sửa bản nháp), A8 hẹn giờ rồi tự xuất bản
--   T5, T6 chờ Trưởng phòng duyệt (T5: phòng Công nghệ, T6: phòng Giáo dục); T7 chờ Thư ký;
--   T8 bị Thư ký từ chối; T9 bị Trưởng phòng từ chối
--   T10 Top-down vừa giao xuống, Trưởng phòng chưa phân công Lead
--   Tài khoản: admin (ADMIN), quang.ton vừa đăng ký (PENDING), nam.ly bị khóa, dung.cao đã nghỉ
-- ---------------------------------------------------------------------
SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- Phòng ban
SET IDENTITY_INSERT department ON;
INSERT INTO department (department_id, department_name, description) VALUES
  (1, N'Ban Thư ký tòa soạn', N'Duyệt cuối, lên lịch và xuất bản; giao Topic cấp tòa soạn'),  -- Ban Thư ký tòa soạn
  (2, N'Phòng Giáo dục', N'Tin và bài về giáo dục, đào tạo, tuyển sinh'),  -- Phòng Giáo dục
  (3, N'Phòng Công nghệ', N'Tin và bài về công nghệ, chuyển đổi số, an ninh mạng'),  -- Phòng Công nghệ
  (4, N'Phòng Kinh tế', N'Tin và bài về kinh tế, tài chính, thị trường'),  -- Phòng Kinh tế
  (5, N'Phòng Xã hội', N'Tin và bài về đời sống, đô thị, môi trường');
SET IDENTITY_INSERT department OFF;

-- Vai trò
SET IDENTITY_INSERT role ON;
INSERT INTO role (role_id, role_name, description) VALUES
  (1, 'REPORTER', N'Phóng viên: đề xuất Topic, viết bài khi là Lead, cung cấp Material khi là Supporting'),  -- REPORTER
  (2, 'DEPARTMENT_HEAD', N'Trưởng phòng: duyệt Topic, phân công Reporter, duyệt Article cấp phòng'),  -- DEPARTMENT_HEAD
  (3, 'EDITORIAL_SECRETARY', N'Thư ký tòa soạn: duyệt cuối, giao Topic xuống phòng, xuất bản'),
  (4, 'ADMIN', N'Quản trị tài khoản: đổi role, gán phòng ban, khóa / mở tài khoản; không tham gia biên tập');  -- ADMIN [v2]
SET IDENTITY_INSERT role OFF;

-- Tài khoản (mật khẩu demo: Cms@2026)
SET IDENTITY_INSERT account ON;
INSERT INTO account (account_id, role_id, department_id, username, full_name, password_hash, email, status, created_at) VALUES
  (1, 3, 1, 'chau.le', N'Lê Minh Châu', '$2a$10$ADqTMOJVfbWKeROKkQkCDe9AJ8cSyYIEFbGU9J00B6oqgClLidMpO', 'chau.le@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- es_chau
  (2, 3, 1, 'bao.pham', N'Phạm Quốc Bảo', '$2a$10$6rXb6z64z/LqEvw3hzssJe26ww6Vqy2xuQCqUP1BFKcl59zqu/P0S', 'bao.pham@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- es_bao
  (3, 2, 2, 'hung.tran', N'Trần Văn Hùng', '$2a$10$6EvHUn.nHUJnup05rTYlN.qFzFwz9dknLdZvuyHGaLuMTT76ROmam', 'hung.tran@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- dh_hung
  (4, 2, 3, 'ha.nguyen', N'Nguyễn Thu Hà', '$2a$10$TtMdQ.3i6kBCZK3kM1vkbeAY/a87ikKOquH4Bhb4JtbRckKP2DCbe', 'ha.nguyen@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- dh_ha
  (5, 2, 4, 'vinh.dang', N'Đặng Quang Vinh', '$2a$10$qlndY1SLe23LGcPhPtCix.adUux0urPYDoOIb81LLUM2tnspSBc8e', 'vinh.dang@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- dh_vinh
  (6, 2, 5, 'lan.vo', N'Võ Ngọc Lan', '$2a$10$owiKB50Vm6MMvHZY.hMyLupeGSaaLcusGiPOjwNreDHxpXCCM7t7a', 'lan.vo@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- dh_lan
  (7, 1, 2, 'mai.vu', N'Vũ Thanh Mai', '$2a$10$KbBU/Z4rzjG9GWVVjKe1ceqQ6i.rw4pCpYPDC3uVXZaWOOay3rU1.', 'mai.vu@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_mai
  (8, 1, 2, 'huy.hoang', N'Hoàng Gia Huy', '$2a$10$BIEiVx/CyyzVqWXvRPM.QubleXhKuwfQX4LC0Qkk8hB0Ijcs50mBC', 'huy.hoang@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_huy
  (9, 1, 3, 'long.phan', N'Phan Đức Long', '$2a$10$B9ZSlU7yRR11.FZBBXh7..QZwUXyGd5kKp3HpApROrvo7Acjhziey', 'long.phan@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_long
  (10, 1, 3, 'anh.bui', N'Bùi Ngọc Anh', '$2a$10$bdreyri01gD5JLnPJ4dg2uQJz3iXchLrlVA7oR0P4hOwPJjUn9JIK', 'anh.bui@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_anh
  (11, 1, 3, 'tuan.mai', N'Mai Anh Tuấn', '$2a$10$ZRamF6ym/82pVTT2o/RPJ.vpHJrFdP/EKNrsX7O9NyANdU7HneLSe', 'tuan.mai@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_tuan
  (12, 1, 4, 'linh.ngo', N'Ngô Thùy Linh', '$2a$10$6bwxOrP/4/3MP3svjs0njODZevAra4NOpq2yX0ppEwcK7T6zkCzZe', 'linh.ngo@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_linh
  (13, 1, 4, 'khoi.do', N'Đỗ Minh Khôi', '$2a$10$A0fM1W7R3lq0Z7agPxyERuRVxtAvHcNuCL/RUeI.Py5KHxOChOmdK', 'khoi.do@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- rp_khoi
  (14, 1, 5, 'thao.trinh', N'Trịnh Phương Thảo', '$2a$10$4VjkFIQXBev1juWgnY27cOjdNsTPhyMu8RvdjOrjExJL/IRCGkdJC', 'thao.trinh@cmscore.example', 'ACTIVE', '2026-09-22T08:30:00+07:00'),  -- rp_thao
  (15, 1, 5, 'nam.ly', N'Lý Hoàng Nam', '$2a$10$NmtCYnWCS5pm1GWQN2CcJOS7MEMdN5KIxc/oxmklBCoDgxEBkOVzm', 'nam.ly@cmscore.example', 'LOCKED', '2026-07-01T08:00:00+07:00'),  -- rp_nam
  (16, 1, 5, 'dung.cao', N'Cao Văn Dũng', '$2a$10$CC65eBVZEPMfN8KVBCl05O/uS0tsnO9ZNodhVgOw36WOnn7/uiyr2', 'dung.cao@cmscore.example', 'INACTIVE', '2026-07-01T08:00:00+07:00'),
  (17, 4, NULL, 'admin', N'Quản trị hệ thống', '$2a$10$ADqTMOJVfbWKeROKkQkCDe9AJ8cSyYIEFbGU9J00B6oqgClLidMpO', 'admin@cmscore.example', 'ACTIVE', '2026-07-01T08:00:00+07:00'),  -- admin [v2]
  (18, 1, NULL, 'quang.ton', N'Tôn Nhật Quang', '$2a$10$KbBU/Z4rzjG9GWVVjKe1ceqQ6i.rw4pCpYPDC3uVXZaWOOay3rU1.', 'quang.ton@cmscore.example', 'PENDING', '2026-10-10T20:00:00+07:00');  -- rp_quang: vừa đăng ký, chưa xác minh email, chưa có phòng ban [v2]
SET IDENTITY_INSERT account OFF;

-- OTP (hết hạn sau 10 phút)
SET IDENTITY_INSERT account_otp ON;
INSERT INTO account_otp (otp_id, account_id, purpose, code_hash, attempt_count, last_sent_at, expires_at, used_at) VALUES
  (1, 18, 'VERIFY_EMAIL', '48290cf691c41cbc99b2396d2e5313ccfba91987b384e6d8f08b951fa5045e83', 0, '2026-10-10T20:00:00+07:00', '2026-10-10T20:10:00+07:00', NULL),  -- otp1: quang.ton chưa nhập mã, mã đã hết hạn -> cần gửi lại [v2]
  (2, 3, 'RESET_PASSWORD', '1b8092a83b535db2a2c0c15ff5f7ef5ad21dd7a25bd784a6eebba317076f9423', 2, '2026-09-30T14:20:00+07:00', '2026-09-30T14:30:00+07:00', '2026-09-30T14:23:00+07:00'),  -- otp2
  (3, 15, 'RESET_PASSWORD', '6c588ae4788dffe89fedf56f117f90ebbb65779528d6f62c95d958955de7fb7f', 5, '2026-09-12T09:10:00+07:00', '2026-09-12T09:20:00+07:00', NULL),  -- otp3
  (4, 14, 'VERIFY_EMAIL', '3a0c3160ba9aa6e7c56f564490d790e9e681f98c4997b123d5e74ecf6423f39e', 1, '2026-09-22T08:30:00+07:00', '2026-09-22T08:40:00+07:00', '2026-09-22T08:33:00+07:00'),  -- otp4
  (5, 10, 'RESET_PASSWORD', '4a073e4022ecfeda8dd7dd7b41f1f3a775134c3f9ce73a21e62746c2479a0f3b', 1, '2026-10-07T12:58:00+07:00', '2026-10-07T13:08:00+07:00', NULL);
SET IDENTITY_INSERT account_otp OFF;

-- Topic
SET IDENTITY_INSERT topic ON;
INSERT INTO topic (topic_id, created_by, title, description, category, rationale, expected_date, priority, status, created_at, updated_at, department_id) VALUES  -- [FL-01] department_id theo category
  (1, 7, N'AI trong giáo dục phổ thông', N'Theo dõi việc đưa trợ lý AI vào lớp học: lợi ích, rủi ro và quy định của nhà trường.', N'Giáo dục', N'Năm học mới nhiều trường thí điểm trợ lý AI; độc giả quan tâm đến tác động với học sinh.', '2026-09-30', 'HIGH', 'ACTIVE', '2026-08-24T09:15:00+07:00', '2026-08-27T14:20:00+07:00', 2),  -- T1 AI trong giáo dục phổ thông
  (2, 12, N'Giá điện bậc thang và hộ gia đình đô thị', N'Tác động của biểu giá điện bậc thang tới hóa đơn của hộ gia đình ở đô thị.', N'Kinh tế', N'Hóa đơn điện mùa nóng tăng mạnh, nhiều bạn đọc phản ánh cách tính bậc thang khó hiểu.', '2026-09-20', 'NORMAL', 'ACTIVE', '2026-09-01T08:40:00+07:00', '2026-09-03T16:00:00+07:00', 4),  -- T2 Giá điện bậc thang
  (3, 1, N'Chuyển đổi số trong ngân hàng', N'Loạt 3 bài: thanh toán không tiền mặt, an ninh mạng, trải nghiệm người dùng.', N'Công nghệ', N'Định hướng của Ban biên tập cho chuyên đề tháng 10.', '2026-10-15', 'HIGH', 'ACTIVE', '2026-09-10T10:00:00+07:00', '2026-09-11T09:10:00+07:00', 3),  -- T3 Chuyển đổi số ngân hàng
  (4, 16, N'Phân loại rác thải sinh hoạt tại các khu chung cư', N'Thực trạng phân loại rác tại nguồn ở chung cư và những rào cản trong vận hành.', N'Xã hội', N'Quy định phân loại rác bắt đầu áp dụng nhưng nhiều khu chung cư chưa có điểm tập kết riêng.', '2026-08-30', 'NORMAL', 'ACTIVE', '2026-07-20T10:00:00+07:00', '2026-07-23T10:00:00+07:00', 5),  -- T4 Phân loại rác
  (5, 10, N'Trạm sạc xe điện ở chung cư: ai đầu tư, ai vận hành?', N'Nhu cầu sạc xe điện tại chung cư cũ và mô hình đầu tư trạm sạc.', N'Công nghệ', N'Số xe điện cá nhân tăng nhanh, ban quản lý chung cư lúng túng khi lắp trạm sạc.', '2026-10-25', 'NORMAL', 'DEPARTMENT_REVIEW', '2026-10-06T15:20:00+07:00', '2026-10-06T15:20:00+07:00', 3),  -- T5 Trạm sạc xe điện
  (6, 8, N'Học phí đại học và vay ưu đãi', N'So sánh học phí các nhóm ngành và khả năng vay ưu đãi của sinh viên.', N'Giáo dục', N'Mùa nhập học, nhiều gia đình hỏi về chi phí và nguồn vay.', '2026-10-20', 'NORMAL', 'DEPARTMENT_REVIEW', '2026-10-01T10:30:00+07:00', '2026-10-01T10:30:00+07:00', 2),  -- T6 Học phí đại học
  (7, 13, N'Giá vàng và tâm lý nhà đầu tư cá nhân', N'Vì sao nhiều hộ gia đình mua vàng khi giá biến động mạnh.', N'Kinh tế', N'Giá vàng biến động liên tục trong hai tuần qua.', '2026-10-18', 'HIGH', 'SECRETARY_REVIEW', '2026-10-02T09:00:00+07:00', '2026-10-03T09:40:00+07:00', 4),  -- T7 Giá vàng
  (8, 9, N'Đánh giá điện thoại gập thế hệ mới', N'Trải nghiệm thực tế các mẫu điện thoại gập mới ra mắt.', N'Công nghệ', N'Nhiều độc giả hỏi về độ bền và giá bán.', '2026-09-30', 'LOW', 'REJECTED', '2026-09-15T11:00:00+07:00', '2026-09-18T14:00:00+07:00', 3),  -- T8 Điện thoại gập
  (9, 15, N'Tin đồn về thay đổi lịch thi tốt nghiệp', N'Tổng hợp thông tin đang lan truyền trên mạng xã hội về lịch thi.', N'Xã hội', N'Thông tin lan truyền nhanh trong phụ huynh.', '2026-09-10', 'LOW', 'REJECTED', '2026-09-05T16:00:00+07:00', '2026-09-06T09:20:00+07:00', 5),  -- T9 Tin đồn lịch thi
  (10, 2, N'Ngày hội việc làm sinh viên 2026', N'Chuẩn bị loạt tin và bài trước, trong và sau ngày hội việc làm.', N'Giáo dục', N'Sự kiện thường niên, lượng độc giả cao.', '2026-10-25', 'NORMAL', 'ACTIVE', '2026-10-05T09:00:00+07:00', '2026-10-05T09:05:00+07:00', 2);
SET IDENTITY_INSERT topic OFF;

-- Duyệt Topic (chỉ APPROVED / REJECTED)
SET IDENTITY_INSERT topic_review ON;
INSERT INTO topic_review (topic_review_id, topic_id, reviewer_id, review_stage, decision, feedback, reviewed_at) VALUES
  (1, 1, 3, 'DEPARTMENT', 'APPROVED', N'Đồng ý. Nên có thêm góc nhìn của phụ huynh.', '2026-08-25T10:05:00+07:00'),  -- T1 DH duyệt
  (2, 1, 1, 'SECRETARY', 'APPROVED', N'Duyệt. Ưu tiên triển khai trước giữa tháng 9.', '2026-08-27T14:20:00+07:00'),  -- T1 TK duyệt
  (3, 2, 5, 'DEPARTMENT', 'APPROVED', N'Phạm vi hộ gia đình đô thị phù hợp, có căn cứ rõ ràng.', '2026-09-02T09:30:00+07:00'),  -- T2 TP duyệt
  (4, 2, 2, 'SECRETARY', 'APPROVED', NULL, '2026-09-03T16:00:00+07:00'),  -- T2 TK duyệt
  (5, 4, 6, 'DEPARTMENT', 'APPROVED', N'Đề tài thiết thực.', '2026-07-21T09:00:00+07:00'),  -- T4 TP duyệt
  (6, 4, 2, 'SECRETARY', 'APPROVED', NULL, '2026-07-23T10:00:00+07:00'),  -- T4 TK duyệt
  (7, 7, 5, 'DEPARTMENT', 'APPROVED', N'Đề tài thời sự, chuyển Thư ký tòa soạn.', '2026-10-03T09:40:00+07:00'),  -- T7 TP duyệt
  (8, 8, 4, 'DEPARTMENT', 'APPROVED', NULL, '2026-09-16T10:00:00+07:00'),  -- T8 TP duyệt
  (9, 8, 1, 'SECRETARY', 'REJECTED', N'Tòa soạn không đăng bài đánh giá sản phẩm thương mại. Gợi ý đổi thành phân tích thị trường điện thoại gập.', '2026-09-18T14:00:00+07:00'),  -- T8 TK từ chối
  (10, 9, 6, 'DEPARTMENT', 'REJECTED', N'Thông tin chưa kiểm chứng, chưa có nguồn chính thống.', '2026-09-06T09:20:00+07:00');
SET IDENTITY_INSERT topic_review OFF;

-- Phân công
SET IDENTITY_INSERT assignment ON;
INSERT INTO assignment (assignment_id, topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at, completed_at, removed_at, removed_by) VALUES
  (1, 1, 3, 1, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', '2026-08-27T14:20:00+07:00', NULL, NULL, NULL),  -- T1 TP phụ trách
  (2, 1, 7, 1, 'LEAD', '2026-09-15T17:00:00+07:00', N'Bài chính về AI trong trường học, có số liệu khảo sát và ít nhất 2 trường hợp thực tế.', 'ASSIGNED', '2026-08-27T14:20:00+07:00', NULL, NULL, NULL),  -- T1 Lead mai
  (3, 1, 8, 3, 'SUPPORTING', '2026-09-05T17:00:00+07:00', N'Phỏng vấn 2 giáo viên và 1 phụ huynh về việc dùng AI trong lớp.', 'COMPLETED', '2026-08-28T08:30:00+07:00', '2026-09-04T16:00:00+07:00', NULL, NULL),  -- T1 Supporting huy
  (4, 2, 5, 2, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', '2026-09-03T16:00:00+07:00', NULL, NULL, NULL),  -- T2 TP phụ trách
  (5, 2, 12, 2, 'LEAD', '2026-09-28T17:00:00+07:00', N'Bài chính: tác động của giá điện bậc thang; kèm 1 bài hướng dẫn cách đọc hóa đơn.', 'ASSIGNED', '2026-09-03T16:00:00+07:00', NULL, NULL, NULL),  -- T2 Lead linh
  (6, 2, 13, 5, 'SUPPORTING', '2026-09-10T17:00:00+07:00', N'Thu thập biểu giá điện và hóa đơn mẫu của 3 hộ gia đình.', 'COMPLETED', '2026-09-04T08:30:00+07:00', '2026-09-09T15:00:00+07:00', NULL, NULL),  -- T2 Supporting khoi
  (7, 3, 4, 1, 'DEPARTMENT_HEAD', '2026-10-15T17:00:00+07:00', N'Triển khai loạt 3 bài: thanh toán không tiền mặt, an ninh mạng, trải nghiệm người dùng.', 'ASSIGNED', '2026-09-10T10:05:00+07:00', NULL, NULL, NULL),  -- T3 TP phụ trách
  (8, 3, 9, 4, 'LEAD', '2026-10-06T17:00:00+07:00', N'Bài chính: lộ trình chuyển đổi số của các ngân hàng; tổng hợp tư liệu từ nhóm hỗ trợ.', 'ASSIGNED', '2026-09-11T09:00:00+07:00', NULL, NULL, NULL),  -- T3 Lead long
  (9, 3, 10, 4, 'SUPPORTING', '2026-09-20T17:00:00+07:00', N'Phỏng vấn chuyên gia an ninh mạng.', 'COMPLETED', '2026-09-11T09:05:00+07:00', '2026-09-19T17:30:00+07:00', NULL, NULL),  -- T3 Supporting anh
  (10, 3, 11, 4, 'SUPPORTING', '2026-09-22T17:00:00+07:00', N'Tìm số liệu giao dịch không tiền mặt giai đoạn 2025-2026.', 'REMOVED', '2026-09-11T09:10:00+07:00', NULL, '2026-09-15T10:00:00+07:00', 4),  -- T3 Supporting tuan
  (11, 4, 6, 2, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', '2026-07-23T10:00:00+07:00', NULL, NULL, NULL),  -- T4 TP phụ trách
  (12, 4, 16, 2, 'LEAD', '2026-08-20T17:00:00+07:00', NULL, 'REMOVED', '2026-07-23T10:00:00+07:00', NULL, '2026-09-22T09:00:00+07:00', 6),  -- T4 Lead dung (cũ)
  (13, 4, 14, 6, 'LEAD', '2026-10-12T17:00:00+07:00', N'Tiếp quản bài của đồng nghiệp đã nghỉ; sửa theo góp ý của Trưởng phòng và cập nhật số liệu.', 'ASSIGNED', '2026-09-22T09:00:00+07:00', NULL, NULL, NULL),  -- T4 Lead thao (mới)
  (14, 10, 3, 2, 'DEPARTMENT_HEAD', '2026-10-25T17:00:00+07:00', N'Phân công Lead và nhóm hỗ trợ; chốt kế hoạch tin bài.', 'ASSIGNED', '2026-10-05T09:05:00+07:00', NULL, NULL, NULL);
SET IDENTITY_INSERT assignment OFF;

-- Source / Material
SET IDENTITY_INSERT source ON;
INSERT INTO source (source_id, added_by, title, source_type, file_url, description, uploaded_at) VALUES
  (1, 8, N'Biên bản phỏng vấn cô Hạnh (giáo viên Ngữ văn THPT)', 'INTERVIEW', '/uploads/interviews/hanh-gv-ngu-van.docx', N'Phỏng vấn ngày 02/09/2026, tại trường.', '2026-09-03T10:00:00+07:00'),  -- S1
  (2, 7, N'Khảo sát việc dùng AI của học sinh THPT', 'DOCUMENT', '/uploads/docs/khao-sat-ai-thpt.pdf', N'400 phiếu, chọn ngẫu nhiên theo lớp, tháng 9/2026.', '2026-09-01T15:30:00+07:00'),  -- S2
  (3, 8, N'Ảnh học sinh dùng trợ lý AI trong tiết Ngữ văn', 'IMAGE', '/uploads/images/lop-hoc-ai-01.jpg', N'Ảnh: Hoàng Gia Huy.', '2026-09-04T09:00:00+07:00'),  -- S3
  (4, 7, N'Hướng dẫn dùng AI trong nhà trường (bản tham khảo)', 'WEBSITE', 'https://example.org/huong-dan-ai-nha-truong', N'Tài liệu tham khảo công khai.', '2026-09-01T16:00:00+07:00'),  -- S4
  (5, 13, N'Biểu giá bán lẻ điện (bản trình bày)', 'DOCUMENT', '/uploads/docs/bieu-gia-dien.pdf', NULL, '2026-09-08T10:00:00+07:00'),  -- S5
  (6, 13, N'Hóa đơn tiền điện mẫu của 3 hộ gia đình', 'DOCUMENT', '/uploads/data/hoa-don-dien-mau.xlsx', N'Đã ẩn thông tin cá nhân.', '2026-09-09T14:30:00+07:00'),  -- S6
  (7, 12, N'Ảnh công tơ điện chung cư', 'IMAGE', '/uploads/images/cong-to-dien.jpg', N'Ảnh: Ngô Thùy Linh.', '2026-09-12T08:30:00+07:00'),  -- S7
  (8, 10, N'Phỏng vấn chuyên gia an ninh mạng', 'INTERVIEW', '/uploads/interviews/chuyen-gia-an-ninh-mang.mp3', N'Ghi âm 25 phút, đã gỡ băng.', '2026-09-19T17:00:00+07:00'),  -- S8
  (9, 11, N'Bảng số liệu giao dịch không tiền mặt quý 2', 'DOCUMENT', '/uploads/data/giao-dich-khong-tien-mat-q2.xlsx', N'Do Mai Anh Tuấn nộp trước khi rời Topic.', '2026-09-14T11:00:00+07:00'),  -- S9
  (10, 9, N'Ảnh giao dịch qua ứng dụng ngân hàng', 'IMAGE', '/uploads/images/ung-dung-ngan-hang.jpg', N'Ảnh minh họa: Phan Đức Long.', '2026-09-25T09:00:00+07:00'),  -- S10
  (11, 16, N'Ảnh điểm tập kết rác tại chung cư', 'IMAGE', '/uploads/images/diem-tap-ket-rac.jpg', N'Ảnh: Cao Văn Dũng.', '2026-08-05T08:00:00+07:00'),  -- S11
  (12, 16, N'Báo cáo khảo sát phân loại rác 10 chung cư', 'DOCUMENT', '/uploads/docs/khao-sat-rac-10-chung-cu.pdf', NULL, '2026-08-02T16:00:00+07:00'),  -- S12
  (13, 16, N'Dữ liệu điểm nóng rác thải', 'DOCUMENT', '/uploads/data/diem-nong-rac-thai.csv', N'Tọa độ và số lần phản ánh theo khu dân cư.', '2026-08-08T10:00:00+07:00'),  -- S13
  (14, 7, N'Phỏng vấn nhóm phụ huynh (3 người)', 'INTERVIEW', '/uploads/interviews/nhom-phu-huynh.docx', N'Phỏng vấn ngày 12/09/2026.', '2026-09-13T09:00:00+07:00');
SET IDENTITY_INSERT source OFF;

-- Material gắn vào Topic
SET IDENTITY_INSERT topic_source ON;
INSERT INTO topic_source (topic_source_id, topic_id, source_id, attached_at, note) VALUES
  (1, 1, 1, '2026-09-03T10:05:00+07:00', N'Phỏng vấn giáo viên'),  -- T1-S1
  (2, 1, 2, '2026-09-01T15:35:00+07:00', N'Số liệu khảo sát'),  -- T1-S2
  (3, 1, 3, '2026-09-04T09:05:00+07:00', NULL),  -- T1-S3
  (4, 1, 4, '2026-09-01T16:05:00+07:00', N'Tài liệu tham khảo'),  -- T1-S4
  (5, 1, 14, '2026-09-13T09:05:00+07:00', N'Ý kiến phụ huynh'),  -- T1-S14
  (6, 2, 5, '2026-09-08T10:05:00+07:00', NULL),  -- T2-S5
  (7, 2, 6, '2026-09-09T14:35:00+07:00', N'Dữ liệu minh họa'),  -- T2-S6
  (8, 2, 7, '2026-09-12T08:35:00+07:00', NULL),  -- T2-S7
  (9, 3, 8, '2026-09-19T17:05:00+07:00', N'Trả lời của chuyên gia'),  -- T3-S8
  (10, 3, 9, '2026-09-14T11:05:00+07:00', N'Số liệu quý 2'),  -- T3-S9
  (11, 3, 10, '2026-09-25T09:05:00+07:00', NULL),  -- T3-S10
  (12, 4, 11, '2026-08-05T08:05:00+07:00', NULL),  -- T4-S11
  (13, 4, 12, '2026-08-02T16:05:00+07:00', NULL),  -- T4-S12
  (14, 4, 13, '2026-08-08T10:05:00+07:00', NULL);
SET IDENTITY_INSERT topic_source OFF;

-- Article (title / subtitle / sapo / content = bản đang soạn)
SET IDENTITY_INSERT article ON;
INSERT INTO article (article_id, topic_id, lead_id, title, subtitle, sapo, content, category, status, restored_from_version_id, created_at, updated_at, submitted_at, completed_at) VALUES
  (1, 1, 7, N'AI vào lớp học: Cơ hội và những lằn ranh', N'Nhiều trường phổ thông thí điểm trợ lý AI trong năm học mới', N'Từ đầu năm học 2026-2027, một số trường phổ thông đưa trợ lý AI vào giảng dạy. Giáo viên nhẹ gánh soạn bài, nhưng không ít phụ huynh lo học sinh phụ thuộc vào máy.', N'Tại một trường THPT ở Hà Nội, tiết Ngữ văn hôm nay bắt đầu bằng một thao tác lạ: cô giáo nhờ trợ lý AI gợi ý ba dàn ý khác nhau cho đề nghị luận xã hội, rồi cho học sinh chọn một dàn ý để tranh luận.

Theo khảo sát của nhóm phóng viên với 400 học sinh THPT tại hai thành phố lớn (chọn ngẫu nhiên theo lớp, tháng 9/2026), có 62% em đã dùng AI để làm bài tập ít nhất một lần mỗi tuần.

Cô Hạnh, giáo viên Ngữ văn, cho biết thời gian soạn giáo án giảm gần một nửa, nhưng cô vẫn đọc lại từng gợi ý của máy trước khi dùng.

Phụ huynh thì lo ngại. Anh Minh, có con học lớp 10, cho rằng AI sẽ khiến con lười suy nghĩ. Ngược lại, nhà trường khẳng định học sinh chỉ được dùng trợ lý AI trong tiết học có giáo viên giám sát.

Câu hỏi còn lại không phải là có nên dùng AI hay không, mà là dùng đến đâu. Các trường đang xây dựng quy định riêng, và cuộc tranh luận chắc chắn chưa dừng lại.', N'Giáo dục', 'PUBLISHED', NULL, '2026-09-02T15:00:00+07:00', '2026-09-11T16:00:00+07:00', '2026-09-11T16:00:00+07:00', '2026-09-12T15:30:00+07:00'),  -- A1
  (2, 1, 7, N'Giáo viên nói gì về chấm bài bằng AI?', NULL, N'Chấm bài bằng AI giúp giáo viên tiết kiệm thời gian, nhưng câu hỏi về trách nhiệm vẫn chưa có lời đáp.', N'Nhiều giáo viên bắt đầu thử dùng trợ lý AI để chấm bài trắc nghiệm và nhận xét bài viết ngắn.

Cô Hạnh cho biết công cụ giúp chấm nhanh hơn, nhưng các bài văn vẫn cần giáo viên đọc và cho điểm.

Vấn đề lớn nhất là công bằng: ai chịu trách nhiệm khi máy chấm sai?', N'Giáo dục', 'DRAFT', NULL, '2026-09-20T14:30:00+07:00', '2026-09-25T09:00:00+07:00', NULL, NULL),  -- A2
  (3, 2, 12, N'Giá điện bậc thang: Hóa đơn của hộ gia đình đô thị thay đổi ra sao?', N'Cách tính bậc thang và những lỗi dễ gặp trên hóa đơn', N'Biểu giá điện bậc thang khiến cùng một mức tiêu thụ nhưng hóa đơn các hộ chênh nhau đáng kể. Bài viết giải thích cách tính và những tình huống dễ bị tính sai.', N'Mỗi tháng, hộ gia đình dùng 350 kWh sẽ phải trả tiền theo nhiều mức giá khác nhau thay vì một đơn giá chung.

Với ví dụ trong bài, một hộ dùng 350 kWh được chia thành bốn bậc: các bậc đầu có đơn giá thấp, các bậc sau cao dần.

Theo ba hóa đơn mẫu nhóm phóng viên thu thập, chênh lệch lớn nhất đến từ tháng nắng nóng khi điều hòa chạy nhiều giờ.

Bạn đọc nên kiểm tra chỉ số công tơ đầu kỳ và cuối kỳ, đối chiếu với hóa đơn để phát hiện sai sót.', N'Kinh tế', 'SCHEDULED', NULL, '2026-09-20T09:00:00+07:00', '2026-09-28T14:00:00+07:00', '2026-09-28T14:00:00+07:00', '2026-10-05T10:00:00+07:00'),  -- A3
  (4, 2, 12, N'Ba cách giảm hóa đơn tiền điện mùa nóng cho gia đình', NULL, N'Chỉnh nhiệt độ điều hòa, thay thế thiết bị cũ và dùng điện lệch giờ cao điểm là ba việc làm được ngay.', N'Đặt điều hòa ở mức 26-27 độ C và dùng thêm quạt giúp giảm điện năng tiêu thụ đáng kể mà vẫn dễ chịu.

Thiết bị cũ như tủ lạnh trên mười năm tuổi thường tốn điện hơn nhiều so với các mẫu mới.

Dồn việc giặt, sấy sang buổi tối muộn giúp tránh giờ cao điểm, nhất là với các hộ dùng công tơ điện tử.', N'Kinh tế', 'APPROVED', NULL, '2026-09-29T09:00:00+07:00', '2026-09-30T11:00:00+07:00', '2026-09-30T11:00:00+07:00', '2026-10-06T16:40:00+07:00'),  -- A4
  (5, 3, 9, N'Ngân hàng số: Bao giờ người dùng bỏ hẳn tiền mặt?', N'Lộ trình chuyển đổi số của các ngân hàng trong ba năm tới', N'Thanh toán bằng mã QR và ví điện tử đã quen thuộc, nhưng tiền mặt vẫn chưa biến mất. Bài viết xem các ngân hàng đang đi đến đâu.', N'Chỉ vài năm trước, mở tài khoản ngân hàng vẫn đồng nghĩa với việc xếp hàng tại quầy. Nay phần lớn thao tác đã nằm gọn trong ứng dụng.

Theo bảng số liệu quý 2 mà nhóm phóng viên tổng hợp, tỷ lệ giao dịch không tiền mặt tăng đều ở cả khu vực thành thị và nông thôn.

Các ngân hàng cho biết hướng đi tiếp theo là cá nhân hóa dịch vụ và tăng cường xác thực sinh trắc học.

Tuy vậy, người cao tuổi và vùng sâu vẫn cần kênh giao dịch truyền thống, nên tiền mặt khó biến mất trong ngắn hạn.', N'Công nghệ', 'DEPARTMENT_APPROVED', NULL, '2026-09-29T15:00:00+07:00', '2026-10-05T10:00:00+07:00', '2026-10-05T10:00:00+07:00', NULL),  -- A5
  (6, 3, 9, N'An ninh mạng ngân hàng: Người dùng cần làm gì?', NULL, N'Lừa đảo qua tin nhắn và cuộc gọi giả danh ngày càng tinh vi. Chuyên gia khuyến cáo ba nguyên tắc bảo vệ tài khoản.', N'Theo chuyên gia an ninh mạng được phỏng vấn, phần lớn vụ mất tiền xuất phát từ việc người dùng tự cung cấp mã xác thực.

Nguyên tắc đầu tiên là không chia sẻ mã OTP với bất kỳ ai, kể cả người tự xưng là nhân viên ngân hàng.

Hai nguyên tắc còn lại là bật thông báo biến động số dư và chỉ cài ứng dụng từ kho ứng dụng chính thức.', N'Công nghệ', 'DEPARTMENT_REVIEW', NULL, '2026-10-05T14:00:00+07:00', '2026-10-06T17:00:00+07:00', '2026-10-06T17:00:00+07:00', NULL),  -- A6
  (7, 4, 14, N'Phân loại rác tại nguồn: Chuyện chưa dễ ở các khu chung cư', NULL, N'Nhiều chung cư đã có thùng rác phân loại, nhưng rác vẫn bị đổ chung ở khâu thu gom.', N'Ở một khu chung cư tại Hà Nội, mỗi tầng có ba thùng rác màu khác nhau, nhưng cuối ngày tất cả vẫn được đổ vào một xe.

Cư dân cho biết họ sẵn sàng phân loại nếu biết rác sau đó được xử lý riêng.

Ban quản lý thì cho rằng chi phí thuê đơn vị thu gom riêng là rào cản lớn nhất.

Theo khảo sát 10 chung cư của nhóm phóng viên, đến quý 3/2026 chỉ có 4 nơi có điểm tập kết riêng cho rác tái chế.', N'Xã hội', 'REVISION_REQUIRED', NULL, '2026-09-07T09:00:00+07:00', '2026-10-02T10:00:00+07:00', '2026-09-15T16:30:00+07:00', NULL),  -- A7
  (8, 4, 16, N'Bản đồ điểm nóng rác thải: 10 khu dân cư bị phản ánh nhiều nhất', NULL, N'Dữ liệu phản ánh của người dân cho thấy rác thải tập trung ở một số khu vực nhất định.', N'Từ dữ liệu phản ánh mà nhóm phóng viên tổng hợp, mười khu dân cư chiếm hơn một nửa số lượt phản ánh về rác thải.

Nguyên nhân chung là điểm tập kết quá nhỏ so với dân số và tần suất thu gom chưa đáp ứng nhu cầu.

Người dân đề xuất tăng chuyến thu gom và bố trí thêm thùng rác công cộng ở các tuyến phố đông người.', N'Xã hội', 'PUBLISHED', NULL, '2026-08-12T09:00:00+07:00', '2026-08-20T15:00:00+07:00', '2026-08-20T15:00:00+07:00', '2026-08-24T11:00:00+07:00'),  -- A8
  (9, 1, 7, N'Phụ huynh nói gì về việc con dùng trợ lý AI để học?', NULL, N'Ba phụ huynh chia sẻ cách họ đặt giới hạn cho con khi dùng trợ lý AI ở nhà.', N'Chị Hoa, mẹ của một học sinh lớp 8, cho phép con dùng trợ lý AI để giải thích bài khó, nhưng yêu cầu con tự làm lại bài vào vở.

Anh Tuấn đặt quy tắc không dùng AI khi làm bài kiểm tra và luôn hỏi con đã hiểu bài chưa trước khi cho dùng.

Cả ba phụ huynh cho rằng điều quan trọng là trò chuyện thường xuyên với con thay vì cấm hoàn toàn.', N'Giáo dục', 'PUBLISHED', NULL, '2026-09-16T09:00:00+07:00', '2026-09-18T10:00:00+07:00', '2026-09-18T10:00:00+07:00', '2026-09-21T15:00:00+07:00');
SET IDENTITY_INSERT article OFF;

-- Phiên bản Article (chỉ các lần Submit / Resubmit)
SET IDENTITY_INSERT article_version ON;
INSERT INTO article_version (version_id, article_id, created_by, version_number, title, subtitle, sapo, content, change_summary, created_at) VALUES
  (1, 1, 7, 1, N'AI vào lớp học: Thầy cô được gì, học sinh mất gì?', N'Nhiều trường phổ thông thí điểm trợ lý AI trong năm học mới', N'Từ đầu năm học 2026-2027, hàng chục trường phổ thông đưa trợ lý AI vào giảng dạy. Giáo viên nhẹ gánh soạn bài, nhưng không ít phụ huynh lo học sinh phụ thuộc vào máy.', N'Tại một trường THPT ở Hà Nội, tiết Ngữ văn hôm nay bắt đầu bằng một thao tác lạ: cô giáo nhờ trợ lý AI gợi ý ba dàn ý khác nhau cho đề nghị luận xã hội, rồi cho học sinh chọn một dàn ý để tranh luận.

Theo khảo sát của nhóm phóng viên với 400 học sinh, có 62% em đã dùng AI để làm bài tập ít nhất một lần mỗi tuần.

Cô Hạnh, giáo viên Ngữ văn, cho biết thời gian soạn giáo án giảm gần một nửa, nhưng cô vẫn đọc lại từng gợi ý của máy trước khi dùng.

Phụ huynh thì lo ngại. Nhiều người cho rằng AI sẽ khiến con lười suy nghĩ.

Câu hỏi còn lại không phải là có nên dùng AI hay không, mà là dùng đến đâu. Các trường đang xây dựng quy định riêng, và cuộc tranh luận chắc chắn chưa dừng lại.', N'Nộp lần đầu', '2026-09-08T10:00:00+07:00'),  -- A1 v1.0
  (2, 1, 7, 2, N'AI vào lớp học: Cơ hội và những lằn ranh', N'Nhiều trường phổ thông thí điểm trợ lý AI trong năm học mới', N'Từ đầu năm học 2026-2027, hàng chục trường phổ thông đưa trợ lý AI vào giảng dạy. Giáo viên nhẹ gánh soạn bài, nhưng không ít phụ huynh lo học sinh phụ thuộc vào máy.', N'Tại một trường THPT ở Hà Nội, tiết Ngữ văn hôm nay bắt đầu bằng một thao tác lạ: cô giáo nhờ trợ lý AI gợi ý ba dàn ý khác nhau cho đề nghị luận xã hội, rồi cho học sinh chọn một dàn ý để tranh luận.

Theo khảo sát của nhóm phóng viên với 400 học sinh THPT tại hai thành phố lớn (chọn ngẫu nhiên theo lớp, tháng 9/2026), có 62% em đã dùng AI để làm bài tập ít nhất một lần mỗi tuần.

Cô Hạnh, giáo viên Ngữ văn, cho biết thời gian soạn giáo án giảm gần một nửa, nhưng cô vẫn đọc lại từng gợi ý của máy trước khi dùng.

Phụ huynh thì lo ngại. Anh Minh, có con học lớp 10, cho rằng AI sẽ khiến con lười suy nghĩ.

Câu hỏi còn lại không phải là có nên dùng AI hay không, mà là dùng đến đâu. Các trường đang xây dựng quy định riêng, và cuộc tranh luận chắc chắn chưa dừng lại.', N'Sửa theo góp ý của Trưởng phòng', '2026-09-10T09:30:00+07:00'),  -- A1 v1.1
  (3, 1, 7, 3, N'AI vào lớp học: Cơ hội và những lằn ranh', N'Nhiều trường phổ thông thí điểm trợ lý AI trong năm học mới', N'Từ đầu năm học 2026-2027, một số trường phổ thông đưa trợ lý AI vào giảng dạy. Giáo viên nhẹ gánh soạn bài, nhưng không ít phụ huynh lo học sinh phụ thuộc vào máy.', N'Tại một trường THPT ở Hà Nội, tiết Ngữ văn hôm nay bắt đầu bằng một thao tác lạ: cô giáo nhờ trợ lý AI gợi ý ba dàn ý khác nhau cho đề nghị luận xã hội, rồi cho học sinh chọn một dàn ý để tranh luận.

Theo khảo sát của nhóm phóng viên với 400 học sinh THPT tại hai thành phố lớn (chọn ngẫu nhiên theo lớp, tháng 9/2026), có 62% em đã dùng AI để làm bài tập ít nhất một lần mỗi tuần.

Cô Hạnh, giáo viên Ngữ văn, cho biết thời gian soạn giáo án giảm gần một nửa, nhưng cô vẫn đọc lại từng gợi ý của máy trước khi dùng.

Phụ huynh thì lo ngại. Anh Minh, có con học lớp 10, cho rằng AI sẽ khiến con lười suy nghĩ. Ngược lại, nhà trường khẳng định học sinh chỉ được dùng trợ lý AI trong tiết học có giáo viên giám sát.

Câu hỏi còn lại không phải là có nên dùng AI hay không, mà là dùng đến đâu. Các trường đang xây dựng quy định riêng, và cuộc tranh luận chắc chắn chưa dừng lại.', N'Sửa theo góp ý của Thư ký tòa soạn', '2026-09-11T16:00:00+07:00'),  -- A1 v1.2
  (4, 3, 12, 1, N'Giá điện bậc thang: Hóa đơn của hộ gia đình đô thị thay đổi ra sao?', N'Cách tính bậc thang và những lỗi dễ gặp trên hóa đơn', N'Biểu giá điện bậc thang khiến cùng một mức tiêu thụ nhưng hóa đơn các hộ chênh nhau đáng kể. Bài viết giải thích cách tính và những tình huống dễ bị tính sai.', N'Mỗi tháng, hộ gia đình dùng 350 kWh sẽ phải trả tiền theo nhiều mức giá khác nhau thay vì một đơn giá chung.

Với ví dụ trong bài, một hộ dùng 350 kWh được chia thành bốn bậc: các bậc đầu có đơn giá thấp, các bậc sau cao dần.

Theo ba hóa đơn mẫu nhóm phóng viên thu thập, chênh lệch lớn nhất đến từ tháng nắng nóng khi điều hòa chạy nhiều giờ.

Bạn đọc nên kiểm tra chỉ số công tơ đầu kỳ và cuối kỳ, đối chiếu với hóa đơn để phát hiện sai sót.', N'Nộp lần đầu', '2026-09-28T14:00:00+07:00'),  -- A3 v1.0
  (5, 4, 12, 1, N'Ba cách giảm hóa đơn tiền điện mùa nóng cho gia đình', NULL, N'Chỉnh nhiệt độ điều hòa, thay thế thiết bị cũ và dùng điện lệch giờ cao điểm là ba việc làm được ngay.', N'Đặt điều hòa ở mức 26-27 độ C và dùng thêm quạt giúp giảm điện năng tiêu thụ đáng kể mà vẫn dễ chịu.

Thiết bị cũ như tủ lạnh trên mười năm tuổi thường tốn điện hơn nhiều so với các mẫu mới.

Dồn việc giặt, sấy sang buổi tối muộn giúp tránh giờ cao điểm, nhất là với các hộ dùng công tơ điện tử.', N'Nộp lần đầu', '2026-09-30T11:00:00+07:00'),  -- A4 v1.0
  (6, 5, 9, 1, N'Ngân hàng số: Bao giờ người dùng bỏ hẳn tiền mặt?', N'Lộ trình chuyển đổi số của các ngân hàng trong ba năm tới', N'Thanh toán bằng mã QR và ví điện tử đã quen thuộc, nhưng tiền mặt vẫn chưa biến mất. Bài viết xem các ngân hàng đang đi đến đâu.', N'Chỉ vài năm trước, mở tài khoản ngân hàng vẫn đồng nghĩa với việc xếp hàng tại quầy. Nay phần lớn thao tác đã nằm gọn trong ứng dụng.

Theo bảng số liệu quý 2 mà nhóm phóng viên tổng hợp, tỷ lệ giao dịch không tiền mặt tăng đều ở cả khu vực thành thị và nông thôn.

Các ngân hàng cho biết hướng đi tiếp theo là cá nhân hóa dịch vụ và tăng cường xác thực sinh trắc học.

Tuy vậy, người cao tuổi và vùng sâu vẫn cần kênh giao dịch truyền thống, nên tiền mặt khó biến mất trong ngắn hạn.', N'Nộp lần đầu', '2026-10-05T10:00:00+07:00'),  -- A5 v1.0
  (7, 6, 9, 1, N'An ninh mạng ngân hàng: Người dùng cần làm gì?', NULL, N'Lừa đảo qua tin nhắn và cuộc gọi giả danh ngày càng tinh vi. Chuyên gia khuyến cáo ba nguyên tắc bảo vệ tài khoản.', N'Theo chuyên gia an ninh mạng được phỏng vấn, phần lớn vụ mất tiền xuất phát từ việc người dùng tự cung cấp mã xác thực.

Nguyên tắc đầu tiên là không chia sẻ mã OTP với bất kỳ ai, kể cả người tự xưng là nhân viên ngân hàng.

Hai nguyên tắc còn lại là bật thông báo biến động số dư và chỉ cài ứng dụng từ kho ứng dụng chính thức.', N'Nộp lần đầu', '2026-10-06T17:00:00+07:00'),  -- A6 v1.0
  (8, 7, 16, 1, N'Phân loại rác tại nguồn: Chuyện chưa dễ ở các khu chung cư', NULL, N'Nhiều chung cư đã có thùng rác phân loại, nhưng rác vẫn bị đổ chung ở khâu thu gom.', N'Ở một khu chung cư tại Hà Nội, mỗi tầng có ba thùng rác màu khác nhau, nhưng cuối ngày tất cả vẫn được đổ vào một xe.

Cư dân cho biết họ sẵn sàng phân loại nếu biết rác sau đó được xử lý riêng.

Ban quản lý thì cho rằng chi phí thuê đơn vị thu gom riêng là rào cản lớn nhất.

Theo khảo sát 10 chung cư của nhóm phóng viên, chỉ có 3 nơi có điểm tập kết riêng cho rác tái chế.', N'Nộp lần đầu', '2026-09-15T16:30:00+07:00'),  -- A7 v1.0
  (9, 8, 16, 1, N'Bản đồ điểm nóng rác thải: 10 khu dân cư bị phản ánh nhiều nhất', NULL, N'Dữ liệu phản ánh của người dân cho thấy rác thải tập trung ở một số khu vực nhất định.', N'Từ dữ liệu phản ánh mà nhóm phóng viên tổng hợp, mười khu dân cư chiếm hơn một nửa số lượt phản ánh về rác thải.

Nguyên nhân chung là điểm tập kết quá nhỏ so với dân số và tần suất thu gom chưa đáp ứng nhu cầu.

Người dân đề xuất tăng chuyến thu gom và bố trí thêm thùng rác công cộng ở các tuyến phố đông người.', N'Nộp lần đầu', '2026-08-20T15:00:00+07:00'),  -- A8 v1.0
  (10, 9, 7, 1, N'Phụ huynh nói gì về việc con dùng trợ lý AI để học?', NULL, N'Ba phụ huynh chia sẻ cách họ đặt giới hạn cho con khi dùng trợ lý AI ở nhà.', N'Chị Hoa, mẹ của một học sinh lớp 8, cho phép con dùng trợ lý AI để giải thích bài khó, nhưng yêu cầu con tự làm lại bài vào vở.

Anh Tuấn đặt quy tắc không dùng AI khi làm bài kiểm tra và luôn hỏi con đã hiểu bài chưa trước khi cho dùng.

Cả ba phụ huynh cho rằng điều quan trọng là trò chuyện thường xuyên với con thay vì cấm hoàn toàn.', N'Nộp lần đầu', '2026-09-18T10:00:00+07:00');  -- A9 v1.0
SET IDENTITY_INSERT article_version OFF;

-- Source gắn vào Article
SET IDENTITY_INSERT article_source ON;
INSERT INTO article_source (article_source_id, article_id, source_id, attached_at, note, caption, is_featured, display_order) VALUES
  (1, 1, 3, '2026-09-05T10:00:00+07:00', NULL, N'Học sinh dùng trợ lý AI trong tiết Ngữ văn (ảnh minh họa).', 1, 1),  -- A1-S3
  (2, 1, 2, '2026-09-05T10:05:00+07:00', N'Số liệu khảo sát ở đoạn 2', NULL, 0, NULL),  -- A1-S2
  (3, 1, 1, '2026-09-05T10:06:00+07:00', N'Trích lời cô Hạnh ở đoạn 3', NULL, 0, NULL),  -- A1-S1
  (4, 1, 4, '2026-09-05T10:07:00+07:00', N'Tài liệu tham khảo', NULL, 0, NULL),  -- A1-S4
  (5, 2, 1, '2026-09-20T15:10:00+07:00', N'Trích lời cô Hạnh về chấm bài', NULL, 0, NULL),  -- A2-S1
  (6, 3, 7, '2026-09-26T10:00:00+07:00', NULL, N'Công tơ điện tại một khu chung cư (ảnh minh họa).', 1, 1),  -- A3-S7
  (7, 3, 5, '2026-09-26T10:05:00+07:00', N'Biểu giá dùng để tính ví dụ', NULL, 0, NULL),  -- A3-S5
  (8, 3, 6, '2026-09-26T10:06:00+07:00', N'Ba hóa đơn mẫu ở đoạn 3', NULL, 0, NULL),  -- A3-S6
  (9, 4, 7, '2026-09-29T09:30:00+07:00', NULL, N'Công tơ điện (ảnh minh họa).', 1, 1),  -- A4-S7
  (10, 4, 5, '2026-09-29T09:35:00+07:00', N'Căn cứ biểu giá', NULL, 0, NULL),  -- A4-S5
  (11, 5, 10, '2026-09-30T16:10:00+07:00', NULL, N'Giao dịch qua ứng dụng ngân hàng (ảnh minh họa).', 1, 1),  -- A5-S10
  (12, 5, 9, '2026-09-30T16:12:00+07:00', N'Bảng số liệu quý 2 ở đoạn 2', NULL, 0, NULL),  -- A5-S9
  (13, 6, 10, '2026-10-06T16:30:00+07:00', NULL, N'Ứng dụng ngân hàng trên điện thoại (ảnh minh họa).', 1, 1),  -- A6-S10
  (14, 6, 8, '2026-10-06T16:32:00+07:00', N'Trích lời chuyên gia ở đoạn 1', NULL, 0, NULL),  -- A6-S8
  (15, 7, 11, '2026-09-08T11:10:00+07:00', NULL, N'Điểm tập kết rác tại một chung cư (ảnh minh họa).', 1, 1),  -- A7-S11
  (16, 7, 12, '2026-09-15T16:00:00+07:00', N'Khảo sát 10 chung cư ở đoạn 4', NULL, 0, NULL),  -- A7-S12
  (17, 8, 11, '2026-08-12T10:00:00+07:00', NULL, N'Điểm tập kết rác quá tải (ảnh minh họa).', 1, 1),  -- A8-S11
  (18, 8, 13, '2026-08-12T10:05:00+07:00', N'Dữ liệu phản ánh theo khu dân cư', NULL, 0, NULL),  -- A8-S13
  (19, 9, 3, '2026-09-16T09:30:00+07:00', NULL, N'Phụ huynh cùng con tìm hiểu cách dùng trợ lý AI (ảnh minh họa).', 1, 1),  -- A9-S3
  (20, 9, 14, '2026-09-16T09:35:00+07:00', N'Phỏng vấn ba phụ huynh', NULL, 0, NULL);
SET IDENTITY_INSERT article_source OFF;

-- Duyệt Article
SET IDENTITY_INSERT article_review ON;
INSERT INTO article_review (review_id, version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at) VALUES
  (1, 1, 3, 'DEPARTMENT', 'REVIEW', 'REQUEST_REVISION', N'Số liệu khảo sát chưa rõ nguồn, tít hơi giật gân, đoạn 4 chưa có ý kiến phụ huynh cụ thể. Sửa theo các góp ý trong bài rồi nộp lại.', '2026-09-09T14:30:00+07:00'),  -- A1 v1.0 TP
  (2, 2, 3, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Đã sửa đạt yêu cầu cấp phòng.', '2026-09-10T15:00:00+07:00'),  -- A1 v1.1 TP
  (3, 2, 1, 'SECRETARY', 'REVIEW', 'REQUEST_REVISION', N'Đoạn 4 mới có ý kiến một chiều; sapo dùng "hàng chục trường" nhưng chưa có số liệu. Sửa theo góp ý rồi nộp lại.', '2026-09-11T10:00:00+07:00'),  -- A1 v1.1 TK
  (4, 3, 3, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Đã bổ sung đủ ý kiến.', '2026-09-12T09:00:00+07:00'),  -- A1 v1.2 TP
  (5, 3, 1, 'SECRETARY', 'REVIEW', 'APPROVED', N'Duyệt xuất bản.', '2026-09-12T15:30:00+07:00'),  -- A1 v1.2 TK
  (6, 4, 5, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Đạt yêu cầu cấp phòng.', '2026-09-29T09:15:00+07:00'),  -- A3 v1.0 TP
  (7, 4, 2, 'SECRETARY', 'REVIEW', 'APPROVED', N'Duyệt, hẹn đăng sáng thứ Bảy.', '2026-10-05T10:00:00+07:00'),  -- A3 v1.0 TK
  (8, 5, 5, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Gọn, dễ đọc.', '2026-10-01T10:00:00+07:00'),  -- A4 v1.0 TP
  (9, 5, 2, 'SECRETARY', 'REVIEW', 'APPROVED', NULL, '2026-10-06T16:40:00+07:00'),  -- A4 v1.0 TK
  (10, 6, 4, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Đạt yêu cầu cấp phòng, chuyển Thư ký.', '2026-10-06T09:00:00+07:00'),  -- A5 v1.0 TP
  (11, 6, 1, 'SECRETARY', 'REVIEW', NULL, NULL, NULL),  -- A5 v1.0 TK (đang review)
  (12, 8, 6, 'DEPARTMENT', 'REVIEW', 'REQUEST_REVISION', N'Cần ghi rõ nguồn và thời điểm của số liệu khảo sát, bổ sung ý kiến của đơn vị thu gom.', '2026-09-18T10:15:00+07:00'),  -- A7 v1.0 TP
  (13, 9, 6, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Dữ liệu rõ ràng.', '2026-08-21T09:30:00+07:00'),  -- A8 v1.0 TP
  (14, 9, 2, 'SECRETARY', 'REVIEW', 'APPROVED', N'Duyệt, hẹn đăng 7h sáng 26/8.', '2026-08-24T11:00:00+07:00'),  -- A8 v1.0 TK
  (15, 10, 3, 'DEPARTMENT', 'REVIEW', 'APPROVED', N'Tốt.', '2026-09-19T10:00:00+07:00'),  -- A9 v1.0 TP
  (16, 10, 1, 'SECRETARY', 'REVIEW', 'APPROVED', NULL, '2026-09-21T15:00:00+07:00');  -- A9 v1.0 TK
SET IDENTITY_INSERT article_review OFF;

-- Comment
SET IDENTITY_INSERT comment ON;
INSERT INTO comment (comment_id, review_id, created_by, parent_comment_id, anchor_field, anchor_paragraph, anchor_start, anchor_end, quoted_text, content, status, created_at) VALUES
  (1, 1, 3, NULL, 'CONTENT', 2, 52, 112, N'có 62% em đã dùng AI để làm bài tập ít nhất một lần mỗi tuần', N'Cần nêu rõ nguồn và cách chọn mẫu của khảo sát này.', 'RESOLVED', '2026-09-09T14:10:00+07:00'),  -- c1
  (2, 1, 3, NULL, 'TITLE', NULL, 16, 49, N'Thầy cô được gì, học sinh mất gì?', N'Tít hơi giật gân, đề nghị bỏ vế "học sinh mất gì".', 'RESOLVED', '2026-09-09T14:12:00+07:00'),  -- c2
  (3, 1, 3, NULL, 'CONTENT', 4, 23, 73, N'Nhiều người cho rằng AI sẽ khiến con lười suy nghĩ', N'Chưa có phụ huynh nào được trích dẫn cụ thể. Bổ sung ít nhất một ý kiến có tên.', 'RESOLVED', '2026-09-09T14:20:00+07:00'),  -- c3
  (4, 1, 7, 3, NULL, NULL, NULL, NULL, NULL, N'Em đã bổ sung ý kiến anh Minh ở đoạn 4.', 'RESOLVED', '2026-09-10T09:10:00+07:00'),  -- c3r
  (5, 1, 3, NULL, NULL, NULL, NULL, NULL, NULL, N'Bố cục bài ổn. Sửa các điểm trên rồi nộp lại.', 'RESOLVED', '2026-09-09T14:28:00+07:00'),  -- c0
  (6, 3, 1, NULL, 'CONTENT', 4, 52, 90, N'cho rằng AI sẽ khiến con lười suy nghĩ', N'Ý kiến một chiều, cần thêm phản hồi từ nhà trường để cân bằng.', 'RESOLVED', '2026-09-11T09:50:00+07:00'),  -- c4
  (7, 3, 1, NULL, 'SAPO', NULL, 26, 52, N'hàng chục trường phổ thông', N'Chưa có số liệu cho "hàng chục". Dùng số cụ thể hoặc đổi thành "một số trường".', 'RESOLVED', '2026-09-11T09:52:00+07:00'),  -- c5
  (8, 3, 7, 6, NULL, NULL, NULL, NULL, NULL, N'Đã bổ sung ý kiến của nhà trường ở cuối đoạn 4.', 'RESOLVED', '2026-09-11T15:40:00+07:00'),  -- c4r
  (9, 11, 1, NULL, 'CONTENT', 2, 53, 128, N'tỷ lệ giao dịch không tiền mặt tăng đều ở cả khu vực thành thị và nông thôn', N'Cần dẫn số liệu cụ thể và ghi nguồn (bảng quý 2).', 'OPEN', '2026-10-07T09:40:00+07:00'),  -- cE
  (10, 12, 6, NULL, 'CONTENT', 4, 47, 97, N'chỉ có 3 nơi có điểm tập kết riêng cho rác tái chế', N'Số liệu cần ghi rõ thời điểm khảo sát và tên các chung cư (hoặc ẩn danh có chú thích).', 'OPEN', '2026-09-18T10:05:00+07:00'),  -- cA
  (11, 12, 6, NULL, 'CONTENT', 2, 19, 74, N'sẵn sàng phân loại nếu biết rác sau đó được xử lý riêng', N'Cần thêm ý kiến của đơn vị thu gom để bài đủ hai chiều.', 'OPEN', '2026-09-18T10:10:00+07:00'),  -- cB
  (12, 12, 14, 10, NULL, NULL, NULL, NULL, NULL, N'Em đã tiếp quản bài và cập nhật số liệu quý 3 ở bản v3; tên các chung cư sẽ bổ sung trước khi nộp lại.', 'OPEN', '2026-10-02T10:30:00+07:00');
SET IDENTITY_INSERT comment OFF;

-- Xuất bản
SET IDENTITY_INSERT publication ON;
INSERT INTO publication (publication_id, article_id, version_id, published_by, publication_type, scheduled_at, published_at, status) VALUES
  (1, 1, 3, 1, 'IMMEDIATE', NULL, '2026-09-12T16:10:00+07:00', 'PUBLISHED'),  -- A1
  (2, 3, 4, 2, 'SCHEDULED', '2026-11-28T08:00:00+07:00', NULL, 'SCHEDULED'),  -- A3: hẹn đăng sáng thứ Bảy 28/11 (để còn demo hủy lịch)
  (3, 8, 9, 2, 'SCHEDULED', '2026-08-26T07:00:00+07:00', '2026-08-26T07:00:05+07:00', 'PUBLISHED'),  -- A8
  (4, 9, 10, 1, 'IMMEDIATE', NULL, '2026-09-21T17:30:00+07:00', 'PUBLISHED');
SET IDENTITY_INSERT publication OFF;

-- (SQL Server tự cập nhật bộ đếm IDENTITY khi dùng IDENTITY_INSERT)
COMMIT TRANSACTION;
GO

-- ---------------------------------------------------------------------
-- 13. THỬ NHANH
-- ---------------------------------------------------------------------
SELECT * FROM v_topic_team ORDER BY topic_id;
SELECT * FROM v_article_board ORDER BY article_id;
SELECT * FROM v_article_timeline WHERE article_id = 1 ORDER BY event_at;   -- bài A1: 2 vòng sửa
SELECT * FROM v_public_article ORDER BY published_at DESC;                -- trang công khai
SELECT * FROM v_rule_violations;                                          -- phải trả về 0 dòng
