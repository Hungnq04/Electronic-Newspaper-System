-- =====================================================================
-- Thông báo (notification)
-- Chỉ THÊM: 1 bảng mới, 5 index, dữ liệu mẫu. Không đổi tên / sửa / xóa bảng hay dữ liệu cũ.
-- Dùng cho DB đã dựng từ cms_core_sqlserver_v2.sql (dữ liệu mẫu bên dưới khớp dữ liệu mẫu của file đó).
-- Chạy được nhiều lần: mỗi bước kiểm tra trước khi tạo; dữ liệu mẫu chỉ nạp khi bảng còn trống.
--
-- Chạy: sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -I -f 65001 -i database/notification_migration.sql
--   -I       : bật QUOTED_IDENTIFIER (bắt buộc để tạo filtered index ix_notification_unread)
--   -f 65001 : đọc file UTF-8 để giữ đúng chữ có dấu
--
-- Thiết kế:
--   * Một dòng = một thông báo gửi cho MỘT người. Một sự kiện báo cho nhiều người thì tạo nhiều dòng.
--   * notification_type là SỰ KIỆN đã xảy ra (vd ARTICLE_SUBMITTED), không phải status hiện tại của bài / đề tài.
--   * read_at NULL = chưa đọc. actor_id NULL = do hệ thống (vd tự đăng bài theo lịch).
--   * Không lưu link: ứng dụng tự tính link từ notification_type + article_id / topic_id.
--   * Không dùng trigger: thông báo được tạo trong service, cùng @Transactional với thao tác gây ra nó.
--
-- LƯU Ý: bảng này có khóa ngoại tới account, topic, article. Nếu chạy lại cms_core_sqlserver_v2.sql thì
--   phải thêm "notification" vào ĐẦU danh sách DROP TABLE IF EXISTS của file đó, nếu không DROP TABLE account sẽ lỗi.
-- =====================================================================
USE cms_core;
GO
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------
-- 1. Bảng
-- ---------------------------------------------------------------------
-- notification_type:
--   TOPIC_SUBMITTED              Reporter đề xuất đề tài                 -> Trưởng phòng của ban
--   TOPIC_CHANGE_REQUESTED       Trưởng phòng yêu cầu sửa đề tài (FL-01) -> Reporter đề xuất
--   TOPIC_FORWARDED              Trưởng phòng duyệt, chờ Thư ký           -> các Thư ký
--   TOPIC_APPROVED / _REJECTED   Kết quả duyệt đề tài                     -> Reporter đề xuất
--   TOPIC_ASSIGNED               Được giao phụ trách đề tài               -> Trưởng phòng
--   ASSIGNMENT_CREATED / _REMOVED  Được giao / bị gỡ Lead, Supporting     -> Reporter
--   ASSIGNMENT_COMPLETED         Supporting xong phần việc                -> Trưởng phòng
--   ARTICLE_SUBMITTED            Lead nộp / nộp lại bài                   -> Trưởng phòng của Topic
--   ARTICLE_REVISION_REQUESTED   Yêu cầu sửa, Return to Lead, Cancel & Return -> Lead (+ Trưởng phòng nếu Thư ký trả)
--   ARTICLE_DEPARTMENT_APPROVED  Trưởng phòng duyệt cấp phòng             -> các Thư ký
--   ARTICLE_APPROVED / _SCHEDULED / _PUBLISHED  Duyệt cuối, hẹn giờ, đã đăng -> Lead (+ Trưởng phòng khi đăng)
--   ACCOUNT_AWAITING_APPROVAL    Có tài khoản chờ duyệt                   -> Admin
--   ACCOUNT_APPROVED             Tài khoản đã được duyệt                  -> người đăng ký
IF OBJECT_ID('dbo.notification', 'U') IS NULL
CREATE TABLE dbo.notification (
    notification_id   INT IDENTITY(1,1) NOT NULL,
    recipient_id      INT            NOT NULL,                  -- FK người nhận
    actor_id          INT            NULL,                      -- FK người gây ra sự kiện; NULL = hệ thống
    notification_type VARCHAR(40)    NOT NULL,
    topic_id          INT            NULL,                      -- FK đề tài liên quan
    article_id        INT            NULL,                      -- FK bài liên quan
    title             NVARCHAR(255)  NOT NULL,                  -- dòng chính hiện trong danh sách
    message           NVARCHAR(1000) NULL,                      -- chi tiết, vd nhận xét khi trả bài
    created_at        DATETIMEOFFSET(0) NOT NULL CONSTRAINT df_notification_created_at DEFAULT SYSDATETIMEOFFSET(),
    read_at           DATETIMEOFFSET(0) NULL,                   -- NULL = chưa đọc
    CONSTRAINT notification_pkey PRIMARY KEY (notification_id),
    CONSTRAINT notification_type_check CHECK (notification_type IN (
        'TOPIC_SUBMITTED', 'TOPIC_CHANGE_REQUESTED', 'TOPIC_FORWARDED', 'TOPIC_APPROVED', 'TOPIC_REJECTED', 'TOPIC_ASSIGNED',
        'ASSIGNMENT_CREATED', 'ASSIGNMENT_REMOVED', 'ASSIGNMENT_COMPLETED',
        'ARTICLE_SUBMITTED', 'ARTICLE_REVISION_REQUESTED', 'ARTICLE_DEPARTMENT_APPROVED',
        'ARTICLE_APPROVED', 'ARTICLE_SCHEDULED', 'ARTICLE_PUBLISHED',
        'ACCOUNT_AWAITING_APPROVAL', 'ACCOUNT_APPROVED')),
    -- Loại TOPIC_* / ASSIGNMENT_* phải trỏ tới đề tài, ARTICLE_* phải trỏ tới bài
    CONSTRAINT ck_notification_target CHECK (
           (notification_type LIKE 'TOPIC[_]%'      AND topic_id   IS NOT NULL)
        OR (notification_type LIKE 'ASSIGNMENT[_]%' AND topic_id   IS NOT NULL)
        OR (notification_type LIKE 'ARTICLE[_]%'    AND article_id IS NOT NULL)
        OR (notification_type LIKE 'ACCOUNT[_]%')),
    -- Không tự báo cho chính mình
    CONSTRAINT ck_notification_not_self CHECK (actor_id IS NULL OR actor_id <> recipient_id),
    CONSTRAINT ck_notification_read     CHECK (read_at IS NULL OR read_at >= created_at),
    CONSTRAINT ck_notification_title    CHECK (LEN(LTRIM(RTRIM(title))) > 0),
    CONSTRAINT notification_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES dbo.account (account_id),
    CONSTRAINT notification_actor_id_fkey     FOREIGN KEY (actor_id)     REFERENCES dbo.account (account_id),
    CONSTRAINT notification_topic_id_fkey     FOREIGN KEY (topic_id)     REFERENCES dbo.topic (topic_id),
    CONSTRAINT notification_article_id_fkey   FOREIGN KEY (article_id)   REFERENCES dbo.article (article_id)
);
GO

-- ---------------------------------------------------------------------
-- 2. Index
-- ---------------------------------------------------------------------
-- Danh sách thông báo của một người, mới nhất trước
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_recipient' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_recipient ON dbo.notification (recipient_id, created_at DESC);
-- Đếm số chưa đọc (chấm đỏ trên chuông): chỉ index các dòng chưa đọc
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_unread' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_unread ON dbo.notification (recipient_id) WHERE read_at IS NULL;
-- Index cho khóa ngoại (SQL Server không tự tạo)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_actor' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_actor ON dbo.notification (actor_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_topic' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_topic ON dbo.notification (topic_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_notification_article' AND object_id = OBJECT_ID('dbo.notification'))
    CREATE INDEX ix_notification_article ON dbo.notification (article_id);
GO

-- ---------------------------------------------------------------------
-- 3. Dữ liệu mẫu: khớp đúng lịch sử có sẵn trong cms_core_sqlserver_v2.sql
--    (đúng người, đúng bài / đề tài, đúng mốc thời gian). Chỉ nạp khi bảng còn trống.
--    Tài khoản: 1 chau.le, 2 bao.pham (Thư ký); 3 hung.tran, 4 ha.nguyen, 5 vinh.dang, 6 lan.vo (Trưởng phòng);
--               7 mai.vu, 8 huy.hoang, 9 long.phan, 10 anh.bui, 11 tuan.mai, 12 linh.ngo, 13 khoi.do,
--               14 thao.trinh, 15 nam.ly, 16 dung.cao (Reporter)
-- ---------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM dbo.notification)
BEGIN
    BEGIN TRANSACTION;
    INSERT INTO dbo.notification (recipient_id, actor_id, notification_type, topic_id, article_id, title, message, created_at, read_at) VALUES
      -- Đề tài
      (3, 1, 'TOPIC_ASSIGNED', 1, NULL, N'Được giao phụ trách đề tài: AI trong giáo dục phổ thông', N'Đề tài đã được Thư ký tòa soạn duyệt.', '2026-08-27T14:20:00+07:00', '2026-08-27T16:00:00+07:00'),
      (7, 1, 'TOPIC_APPROVED', 1, NULL, N'Đề tài được duyệt: AI trong giáo dục phổ thông', N'Bạn là Lead Reporter của đề tài.', '2026-08-27T14:20:00+07:00', '2026-08-27T15:00:00+07:00'),
      (15, 6, 'TOPIC_REJECTED', 9, NULL, N'Đề tài bị từ chối: Tin đồn về thay đổi lịch thi tốt nghiệp', N'Thông tin chưa kiểm chứng, chưa có nguồn chính thống.', '2026-09-06T09:20:00+07:00', '2026-09-06T10:00:00+07:00'),
      (9, 1, 'TOPIC_REJECTED', 8, NULL, N'Đề tài bị từ chối: Đánh giá điện thoại gập thế hệ mới', N'Tòa soạn không đăng bài đánh giá sản phẩm thương mại. Gợi ý đổi thành phân tích thị trường điện thoại gập.', '2026-09-18T14:00:00+07:00', '2026-09-18T15:10:00+07:00'),
      (3, 8, 'TOPIC_SUBMITTED', 6, NULL, N'Đề tài mới chờ duyệt: Học phí đại học và vay ưu đãi', N'Hoàng Gia Huy đề xuất cho Phòng Giáo dục.', '2026-10-01T10:30:00+07:00', NULL),
      (1, 5, 'TOPIC_FORWARDED', 7, NULL, N'Đề tài chờ duyệt cuối: Giá vàng và tâm lý nhà đầu tư cá nhân', N'Trưởng phòng Kinh tế đã duyệt cấp phòng.', '2026-10-03T09:40:00+07:00', NULL),
      (2, 5, 'TOPIC_FORWARDED', 7, NULL, N'Đề tài chờ duyệt cuối: Giá vàng và tâm lý nhà đầu tư cá nhân', N'Trưởng phòng Kinh tế đã duyệt cấp phòng.', '2026-10-03T09:40:00+07:00', NULL),
      (3, 2, 'TOPIC_ASSIGNED', 10, NULL, N'Được giao phụ trách đề tài: Ngày hội việc làm sinh viên 2026', N'Cần phân công Lead và nhóm hỗ trợ.', '2026-10-05T09:05:00+07:00', NULL),
      (4, 10, 'TOPIC_SUBMITTED', 5, NULL, N'Đề tài mới chờ duyệt: Trạm sạc xe điện ở chung cư: ai đầu tư, ai vận hành?', N'Bùi Ngọc Anh đề xuất cho Phòng Công nghệ.', '2026-10-06T15:20:00+07:00', NULL),
      -- Phân công
      (8, 3, 'ASSIGNMENT_CREATED', 1, NULL, N'Bạn được giao hỗ trợ đề tài: AI trong giáo dục phổ thông', N'Phỏng vấn 2 giáo viên và 1 phụ huynh về việc dùng AI trong lớp. Hạn: 05/09/2026.', '2026-08-28T08:30:00+07:00', '2026-08-28T09:00:00+07:00'),
      (3, 8, 'ASSIGNMENT_COMPLETED', 1, NULL, N'Hoàng Gia Huy đã hoàn thành phần việc hỗ trợ', N'Đề tài: AI trong giáo dục phổ thông.', '2026-09-04T16:00:00+07:00', '2026-09-04T17:00:00+07:00'),
      (11, 4, 'ASSIGNMENT_REMOVED', 3, NULL, N'Bạn đã được gỡ khỏi đề tài: Chuyển đổi số trong ngân hàng', NULL, '2026-09-15T10:00:00+07:00', '2026-09-15T11:00:00+07:00'),
      (14, 6, 'ASSIGNMENT_CREATED', 4, NULL, N'Bạn được giao viết chính đề tài: Phân loại rác thải sinh hoạt tại các khu chung cư', N'Tiếp quản bài của đồng nghiệp đã nghỉ. Hạn: 12/10/2026.', '2026-09-22T09:00:00+07:00', '2026-09-22T09:30:00+07:00'),
      -- Bài A1: trọn vòng nộp, 2 lần sửa, duyệt cuối, xuất bản
      (3, 7, 'ARTICLE_SUBMITTED', NULL, 1, N'Bài mới chờ duyệt: AI vào lớp học: Cơ hội và những lằn ranh', N'Phiên bản 1.0.', '2026-09-08T10:00:00+07:00', '2026-09-09T14:00:00+07:00'),
      (7, 3, 'ARTICLE_REVISION_REQUESTED', NULL, 1, N'Bài cần sửa: AI vào lớp học: Cơ hội và những lằn ranh', N'Số liệu khảo sát chưa rõ nguồn, tít hơi giật gân, đoạn 4 chưa có ý kiến phụ huynh cụ thể. Sửa theo các góp ý trong bài rồi nộp lại.', '2026-09-09T14:30:00+07:00', '2026-09-09T15:00:00+07:00'),
      (3, 7, 'ARTICLE_SUBMITTED', NULL, 1, N'Bài nộp lại chờ duyệt: AI vào lớp học: Cơ hội và những lằn ranh', N'Phiên bản 1.1: Sửa theo góp ý của Trưởng phòng.', '2026-09-10T09:30:00+07:00', '2026-09-10T14:30:00+07:00'),
      (1, 3, 'ARTICLE_DEPARTMENT_APPROVED', NULL, 1, N'Bài chờ duyệt cuối: AI vào lớp học: Cơ hội và những lằn ranh', N'Trưởng phòng Giáo dục đã duyệt phiên bản 1.1.', '2026-09-10T15:00:00+07:00', '2026-09-11T09:30:00+07:00'),
      (7, 1, 'ARTICLE_REVISION_REQUESTED', NULL, 1, N'Bài cần sửa: AI vào lớp học: Cơ hội và những lằn ranh', N'Đoạn 4 mới có ý kiến một chiều; sapo dùng "hàng chục trường" nhưng chưa có số liệu. Sửa theo góp ý rồi nộp lại.', '2026-09-11T10:00:00+07:00', '2026-09-11T10:20:00+07:00'),
      (3, 1, 'ARTICLE_REVISION_REQUESTED', NULL, 1, N'Thư ký trả bài về Lead: AI vào lớp học: Cơ hội và những lằn ranh', N'Bài nộp lại sẽ quay về Trưởng phòng duyệt trước.', '2026-09-11T10:00:00+07:00', '2026-09-11T11:00:00+07:00'),
      (3, 7, 'ARTICLE_SUBMITTED', NULL, 1, N'Bài nộp lại chờ duyệt: AI vào lớp học: Cơ hội và những lằn ranh', N'Phiên bản 1.2: Sửa theo góp ý của Thư ký tòa soạn.', '2026-09-11T16:00:00+07:00', '2026-09-12T08:45:00+07:00'),
      (1, 3, 'ARTICLE_DEPARTMENT_APPROVED', NULL, 1, N'Bài chờ duyệt cuối: AI vào lớp học: Cơ hội và những lằn ranh', N'Trưởng phòng Giáo dục đã duyệt phiên bản 1.2.', '2026-09-12T09:00:00+07:00', '2026-09-12T15:00:00+07:00'),
      (7, 1, 'ARTICLE_APPROVED', NULL, 1, N'Bài đã được duyệt cuối: AI vào lớp học: Cơ hội và những lằn ranh', NULL, '2026-09-12T15:30:00+07:00', '2026-09-12T15:45:00+07:00'),
      (7, 1, 'ARTICLE_PUBLISHED', NULL, 1, N'Bài đã xuất bản: AI vào lớp học: Cơ hội và những lằn ranh', NULL, '2026-09-12T16:10:00+07:00', '2026-09-12T16:30:00+07:00'),
      (3, 1, 'ARTICLE_PUBLISHED', NULL, 1, N'Bài đã xuất bản: AI vào lớp học: Cơ hội và những lằn ranh', NULL, '2026-09-12T16:10:00+07:00', '2026-09-12T17:00:00+07:00'),
      -- Các bài khác
      (16, NULL, 'ARTICLE_PUBLISHED', NULL, 8, N'Bài đã tự động xuất bản theo lịch: Bản đồ điểm nóng rác thải: 10 khu dân cư bị phản ánh nhiều nhất', NULL, '2026-08-26T07:00:05+07:00', '2026-08-26T08:00:00+07:00'),
      (16, 6, 'ARTICLE_REVISION_REQUESTED', NULL, 7, N'Bài cần sửa: Phân loại rác tại nguồn: Chuyện chưa dễ ở các khu chung cư', N'Cần ghi rõ nguồn và thời điểm của số liệu khảo sát, bổ sung ý kiến của đơn vị thu gom.', '2026-09-18T10:15:00+07:00', NULL),
      (7, 1, 'ARTICLE_PUBLISHED', NULL, 9, N'Bài đã xuất bản: Phụ huynh nói gì về việc con dùng trợ lý AI để học?', NULL, '2026-09-21T17:30:00+07:00', '2026-09-21T18:00:00+07:00'),
      (4, 9, 'ARTICLE_SUBMITTED', NULL, 5, N'Bài mới chờ duyệt: Ngân hàng số: Bao giờ người dùng bỏ hẳn tiền mặt?', N'Phiên bản 1.0.', '2026-10-05T10:00:00+07:00', '2026-10-05T14:00:00+07:00'),
      (12, 2, 'ARTICLE_SCHEDULED', NULL, 3, N'Bài đã được hẹn giờ đăng: Giá điện bậc thang: Hóa đơn của hộ gia đình đô thị thay đổi ra sao?', N'Đăng lúc 08:00 ngày 28/11/2026.', '2026-10-05T10:00:00+07:00', NULL),
      (1, 4, 'ARTICLE_DEPARTMENT_APPROVED', NULL, 5, N'Bài chờ duyệt cuối: Ngân hàng số: Bao giờ người dùng bỏ hẳn tiền mặt?', N'Trưởng phòng Công nghệ đã duyệt phiên bản 1.0.', '2026-10-06T09:00:00+07:00', '2026-10-07T09:30:00+07:00'),
      (2, 4, 'ARTICLE_DEPARTMENT_APPROVED', NULL, 5, N'Bài chờ duyệt cuối: Ngân hàng số: Bao giờ người dùng bỏ hẳn tiền mặt?', N'Trưởng phòng Công nghệ đã duyệt phiên bản 1.0.', '2026-10-06T09:00:00+07:00', NULL),
      (12, 2, 'ARTICLE_APPROVED', NULL, 4, N'Bài đã được duyệt cuối: Ba cách giảm hóa đơn tiền điện mùa nóng cho gia đình', N'Chờ Thư ký tòa soạn quyết định thời điểm đăng.', '2026-10-06T16:40:00+07:00', NULL),
      (4, 9, 'ARTICLE_SUBMITTED', NULL, 6, N'Bài mới chờ duyệt: An ninh mạng ngân hàng: Người dùng cần làm gì?', N'Phiên bản 1.0.', '2026-10-06T17:00:00+07:00', NULL);
    COMMIT TRANSACTION;
END
GO

-- ---------------------------------------------------------------------
-- 4. Kiểm tra
-- ---------------------------------------------------------------------
-- Toàn bộ thông báo, gom theo người nhận
SELECT n.notification_id, r.full_name AS recipient, a.full_name AS actor, n.notification_type, n.title,
       CASE WHEN n.read_at IS NULL THEN N'Chưa đọc' ELSE N'Đã đọc' END AS read_state, n.created_at
FROM dbo.notification n
JOIN dbo.account r      ON r.account_id = n.recipient_id
LEFT JOIN dbo.account a ON a.account_id = n.actor_id
ORDER BY n.recipient_id, n.created_at DESC;

-- Số thông báo chưa đọc của từng người (con số trên chuông). Kỳ vọng: 33 thông báo, 10 chưa đọc
--   Trần Văn Hùng 2, Nguyễn Thu Hà 2, Ngô Thùy Linh 2, Lê Minh Châu 1, Phạm Quốc Bảo 2, Cao Văn Dũng 1
SELECT r.full_name, COUNT(*) AS unread
FROM dbo.notification n JOIN dbo.account r ON r.account_id = n.recipient_id
WHERE n.read_at IS NULL
GROUP BY r.full_name
ORDER BY unread DESC;

-- Bảng mới không ảnh hưởng các quy tắc cũ: vẫn phải 0 dòng
SELECT * FROM v_rule_violations;
GO
