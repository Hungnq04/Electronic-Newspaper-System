-- =====================================================================
-- FL-07 – Dữ liệu test màn "Chờ xuất bản" của Thư ký tòa soạn (Publish now / Schedule).
-- Tạo 1 Article ở trạng thái APPROVED (Thư ký đã Final Approve, chờ quyết định xuất bản)
-- trên Topic 21 "Lãi suất tiết kiệm..." (Phòng Kinh tế):
--   Lead: Đỗ Minh Khôi (13) – Trưởng phòng: Đặng Quang Vinh (5) – Thư ký duyệt cuối: Lê Minh Châu (1)
-- Bài đi đủ luồng: Submit version 1.0 -> TP Approve -> Thư ký Final Approve -> APPROVED.
-- Có 1 ảnh đại diện (có chú thích) + 1 nguồn website để qua kiểm tra FR-07 (rule R19).
-- Chỉ INSERT khi bài cùng tiêu đề chưa tồn tại, nên chạy lại nhiều lần không bị trùng.
-- Chạy SAU cms_core_sqlserver_v2.sql:
--   sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl07_seed.sql
--
-- Đăng nhập để test (mật khẩu demo: Cms@2026):
--   chau.le@cmscore.example – Thư ký tòa soạn -> mục "Chờ xuất bản" -> Hẹn giờ đăng
-- =====================================================================
SET XACT_ABORT ON;        -- lỗi giữa chừng thì hủy toàn bộ transaction
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bắt buộc khi INSERT vào bảng có filtered index (uq_article_featured); sqlcmd mặc định tắt

DECLARE @secretary_id INT = 1;    -- Lê Minh Châu (Thư ký tòa soạn)
DECLARE @dept_head_id INT = 5;    -- Đặng Quang Vinh (Trưởng phòng Kinh tế)
DECLARE @lead_id      INT = 13;   -- Đỗ Minh Khôi (Lead của Topic 21)
DECLARE @topic_id     INT = 21;   -- Lãi suất tiết kiệm: Gửi ở đâu để an tâm?
DECLARE @now DATETIMEOFFSET(0) = SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00');
DECLARE @title NVARCHAR(255) = N'[TEST] Lãi suất tiết kiệm cuối năm: Ngân hàng nào đang trả cao nhất?';
DECLARE @sapo NVARCHAR(MAX) = N'Cuộc đua lãi suất huy động cuối năm đang nóng lên khi nhiều ngân hàng nhỏ niêm yết mức trên 6%/năm cho kỳ hạn 12 tháng. Người gửi tiền nên chọn thế nào để vừa có lãi vừa an toàn?';
DECLARE @content NVARCHAR(MAX) = N'Từ đầu tháng 11, hàng loạt ngân hàng thương mại cổ phần đã điều chỉnh tăng lãi suất huy động ở các kỳ hạn từ 6 đến 13 tháng, mức tăng phổ biến từ 0,2 đến 0,5 điểm phần trăm.' + CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10)
    + N'Theo khảo sát của phóng viên, nhóm ngân hàng quy mô nhỏ đang dẫn đầu với mức lãi suất trên 6%/năm cho kỳ hạn 12 tháng, trong khi nhóm ngân hàng lớn vẫn giữ quanh mức 4,7 - 5%/năm.' + CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10)
    + N'Chuyên gia tài chính khuyên người gửi tiền không nên chỉ nhìn vào con số lãi suất, mà cần xem xét uy tín ngân hàng, điều kiện rút trước hạn và các chương trình khuyến mãi đi kèm.' + CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10)
    + N'Với khoản tiền lớn, có thể chia nhỏ thành nhiều sổ tiết kiệm ở các kỳ hạn khác nhau để vừa linh hoạt khi cần tiền, vừa tận dụng được lãi suất cao.';
DECLARE @article_id INT, @version_id INT, @image_source_id INT, @web_source_id INT;

BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 trang web
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Ảnh quầy giao dịch ngân hàng', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Đỗ Minh Khôi', DATEADD(DAY, -3, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Biểu lãi suất huy động công bố trên website ngân hàng', 'WEBSITE',
            N'https://www.sbv.gov.vn', N'Tổng hợp biểu lãi suất ngày 05/11', DATEADD(DAY, -3, @now));
    SET @web_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (@topic_id, @image_source_id, DATEADD(DAY, -3, @now)),
           (@topic_id, @web_source_id,   DATEADD(DAY, -3, @now));

    -- 2) Bài viết: trạng thái APPROVED => bắt buộc có submitted_at và completed_at (ck_article_completed)
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (@topic_id, @lead_id, @title, N'Khảo sát lãi suất huy động tháng 11/2026', @sapo, @content,
            N'Kinh tế', 'APPROVED',
            DATEADD(DAY, -3, @now), DATEADD(DAY, -2, @now), DATEADD(DAY, -2, @now), DATEADD(HOUR, -2, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Lead chọn Source từ Topic đưa vào bài; ảnh phải có chú thích (FR-07)
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -2, @now),
            N'Người dân giao dịch tại quầy tiết kiệm của một ngân hàng ở TP.HCM.', 1, 1),
           (@article_id, @web_source_id, DATEADD(DAY, -2, @now), NULL, 0, 2);

    -- 4) Submit lần đầu => version 1.0 (version_number = 1)
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, @lead_id, 1, @title, N'Khảo sát lãi suất huy động tháng 11/2026', @sapo, @content,
            NULL, DATEADD(DAY, -2, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt cấp phòng, rồi Thư ký Final Approve trên CÙNG version (R03, R05)
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, @dept_head_id, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Số liệu rõ ràng, chuyển Thư ký.', DATEADD(DAY, -1, @now)),
           (@version_id, @secretary_id, 'SECRETARY',  'REVIEW', 'APPROVED',
            N'Duyệt đăng. Nên lên lịch đăng buổi sáng.', DATEADD(HOUR, -2, @now));

    PRINT N'Đã thêm bài APPROVED, article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài test đã tồn tại, bỏ qua.';

COMMIT TRANSACTION;

-- Kiểm tra: phải trả về 0 dòng
SELECT * FROM dbo.v_rule_violations;
