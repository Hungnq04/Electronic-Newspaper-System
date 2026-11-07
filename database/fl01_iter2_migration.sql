-- =====================================================================
-- FL-01 Iteration 2 – Duyệt Topic + Thông báo cho phóng viên
-- Chỉ THÊM bảng notification (không đổi tên / xóa gì).
-- File cms_core_sqlserver_v2.sql đã có sẵn bảng này; dựng DB mới thì chỉ cần chạy file v2.
-- Chạy được nhiều lần: kiểm tra trước khi tạo.
--
-- Chạy: sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl01_iter2_migration.sql
-- =====================================================================
SET XACT_ABORT ON;
GO

-- Thông báo trong hệ thống (chuông trên header của phóng viên). Không gửi email.
IF OBJECT_ID('dbo.notification', 'U') IS NULL
    CREATE TABLE dbo.notification (
        notification_id INT IDENTITY(1,1) NOT NULL,
        account_id      INT           NOT NULL,                   -- người nhận
        topic_id        INT           NOT NULL,                   -- bấm thông báo thì mở Topic này
        message         NVARCHAR(500) NOT NULL,
        is_read         BIT           NOT NULL CONSTRAINT df_notification_is_read DEFAULT 0,
        created_at      DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_notification_created_at DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT notification_pkey PRIMARY KEY (notification_id),
        CONSTRAINT notification_account_id_fkey FOREIGN KEY (account_id) REFERENCES dbo.account (account_id),
        CONSTRAINT notification_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES dbo.topic (topic_id)
    );
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_account' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_account ON dbo.notification (account_id, is_read);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_topic' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_topic ON dbo.notification (topic_id);
GO

-- Kiểm tra: phải ra 0 dòng
SELECT * FROM v_rule_violations;
GO
