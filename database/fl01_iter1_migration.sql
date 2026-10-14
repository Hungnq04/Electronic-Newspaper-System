-- =====================================================================
-- FL-01 Iteration 1 – Đề xuất Topic
-- CHỈ DÙNG cho DB đã dựng từ cms_core_sqlserver_v2.sql bản CŨ (trước khi gộp FL-01).
-- File v2 hiện tại đã có sẵn 3 cột này; dựng DB mới thì chỉ cần chạy file v2.
-- Chỉ THÊM (3 cột + 1 ràng buộc vào dbo.topic, bảng topic_feedback, trạng thái tài khoản AWAITING_APPROVAL),
-- không đổi tên / xóa gì.
-- Chạy được nhiều lần: mỗi bước kiểm tra trước khi tạo.
--
-- Chạy: sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl01_iter1_migration.sql
--   (-f 65001: đọc file UTF-8 để giữ đúng chữ có dấu trong câu UPDATE)
-- =====================================================================
SET XACT_ABORT ON;
GO

-- 1. Ban phụ trách Topic (nullable để không vướng dữ liệu cũ)
IF COL_LENGTH('dbo.topic', 'department_id') IS NULL
    ALTER TABLE dbo.topic ADD department_id INT NULL;
GO
IF OBJECT_ID('dbo.topic_department_id_fkey', 'F') IS NULL
    ALTER TABLE dbo.topic ADD CONSTRAINT topic_department_id_fkey
        FOREIGN KEY (department_id) REFERENCES dbo.department (department_id);
GO
-- Tên index giống cms_core_sqlserver_v2.sql (bản chạy trước đó đặt là ix_topic_department_id thì đổi tên)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_topic_department_id' AND object_id = OBJECT_ID('dbo.topic'))
    EXEC sp_rename N'dbo.topic.ix_topic_department_id', N'ix_topic_department', N'INDEX';
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_topic_department' AND object_id = OBJECT_ID('dbo.topic'))
    CREATE INDEX ix_topic_department ON dbo.topic (department_id);
GO

-- 2. Cờ "Trưởng ban yêu cầu sửa" (không phải status; status vẫn DEPARTMENT_REVIEW)
IF COL_LENGTH('dbo.topic', 'awaiting_revision') IS NULL
    ALTER TABLE dbo.topic ADD awaiting_revision BIT NOT NULL
        CONSTRAINT df_topic_awaiting_revision DEFAULT 0;
GO

-- 3. Trưởng ban đã duyệt Topic (dùng từ Iteration 2)
IF COL_LENGTH('dbo.topic', 'reviewed_by_head_id') IS NULL
    ALTER TABLE dbo.topic ADD reviewed_by_head_id INT NULL;
GO
IF OBJECT_ID('dbo.topic_reviewed_by_head_id_fkey', 'F') IS NULL
    ALTER TABLE dbo.topic ADD CONSTRAINT topic_reviewed_by_head_id_fkey
        FOREIGN KEY (reviewed_by_head_id) REFERENCES dbo.account (account_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_topic_reviewed_by_head' AND object_id = OBJECT_ID('dbo.topic'))
    CREATE INDEX ix_topic_reviewed_by_head ON dbo.topic (reviewed_by_head_id);
GO

-- 3b. Cờ yêu cầu sửa chỉ có nghĩa khi Topic còn chờ Trưởng phòng
IF OBJECT_ID('dbo.ck_topic_awaiting_revision', 'C') IS NULL
    ALTER TABLE dbo.topic ADD CONSTRAINT ck_topic_awaiting_revision
        CHECK (awaiting_revision = 0 OR status = 'DEPARTMENT_REVIEW');
GO

-- 3c. Ý kiến trên Topic: các vòng Trưởng phòng yêu cầu sửa (CHANGE_REQUEST), lý do Thư ký từ chối (REJECTION)
IF OBJECT_ID('dbo.topic_feedback', 'U') IS NULL
    CREATE TABLE dbo.topic_feedback (
        topic_feedback_id INT IDENTITY(1,1) NOT NULL,
        topic_id          INT           NOT NULL,
        author_id         INT           NOT NULL,
        feedback_type     VARCHAR(20)   NOT NULL,
        content           NVARCHAR(MAX) NOT NULL,
        created_at        DATETIMEOFFSET(0) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT topic_feedback_pkey PRIMARY KEY (topic_feedback_id),
        CONSTRAINT topic_feedback_type_check CHECK (feedback_type IN ('CHANGE_REQUEST', 'REJECTION')),
        CONSTRAINT ck_topic_feedback_content CHECK (LEN(LTRIM(RTRIM(content))) > 0),
        CONSTRAINT topic_feedback_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES dbo.topic (topic_id),
        CONSTRAINT topic_feedback_author_id_fkey FOREIGN KEY (author_id) REFERENCES dbo.account (account_id)
    );
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_topic_feedback_topic' AND object_id = OBJECT_ID('dbo.topic_feedback'))
    CREATE INDEX ix_topic_feedback_topic ON dbo.topic_feedback (topic_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_topic_feedback_author' AND object_id = OBJECT_ID('dbo.topic_feedback'))
    CREATE INDEX ix_topic_feedback_author ON dbo.topic_feedback (author_id);
GO

-- 3d. Trạng thái tài khoản AWAITING_APPROVAL: đăng ký xong (OTP / Google) chờ Admin duyệt và cấp vai trò
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'account_status_check'
               AND parent_object_id = OBJECT_ID('dbo.account') AND definition LIKE '%AWAITING_APPROVAL%')
BEGIN
    IF OBJECT_ID('dbo.account_status_check', 'C') IS NOT NULL
        ALTER TABLE dbo.account DROP CONSTRAINT account_status_check;
    ALTER TABLE dbo.account ADD CONSTRAINT account_status_check
        CHECK (status IN ('PENDING', 'AWAITING_APPROVAL', 'ACTIVE', 'LOCKED', 'INACTIVE'));
END
GO

-- 4. Gán ban cho Topic cũ theo category ('Kinh tế' -> 'Phòng Kinh tế').
--    Không lấy theo ban của người tạo vì Topic top-down (3, 10) do Thư ký tạo.
UPDATE t
   SET t.department_id = d.department_id
  FROM dbo.topic t
  JOIN dbo.department d ON d.department_name = N'Phòng ' + t.category
 WHERE t.department_id IS NULL;
GO

-- Kiểm tra: Topic nào chưa gán được ban (category không khớp phòng nào)
SELECT topic_id, title, category FROM dbo.topic WHERE department_id IS NULL;
GO
