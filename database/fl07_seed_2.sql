-- =====================================================================
-- FL-07 – Thêm 2 bài vào màn "Chờ xuất bản" của Thư ký tòa soạn (status APPROVED).
-- Mỗi bài đi đủ luồng: Submit version 1.0 -> Trưởng phòng Approve -> Thư ký Final Approve -> APPROVED.
--   Bài 1: Topic 1 "AI trong giáo dục phổ thông" (Phòng Giáo dục)
--          Lead: Vũ Thanh Mai (7) – Trưởng phòng: Trần Văn Hùng (3) – Thư ký duyệt cuối: Phạm Quốc Bảo (2)
--   Bài 2: Topic 4 "Phân loại rác thải sinh hoạt tại các khu chung cư" (Phòng Xã hội)
--          Lead: Trịnh Phương Thảo (14) – Trưởng phòng: Võ Ngọc Lan (6) – Thư ký duyệt cuối: Lê Minh Châu (1)
-- Mỗi bài có 1 ảnh đại diện (có chú thích) + 1 nguồn khác để qua kiểm tra FR-07 (rule R19).
-- Chỉ dùng dữ liệu mẫu của cms_core_sqlserver_v2.sql (không cần chạy fl03_seed.sql trước).
-- Chỉ INSERT khi bài cùng tiêu đề chưa tồn tại, nên chạy lại nhiều lần không bị trùng.
-- Chạy SAU cms_core_sqlserver_v2.sql:
--   sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl07_seed_2.sql
--
-- Đăng nhập để test (mật khẩu demo: Cms@2026):
--   chau.le@cmscore.example hoặc bao.pham@cmscore.example – Thư ký tòa soạn -> mục "Chờ xuất bản"
-- =====================================================================
USE cms_core;             -- luôn chạy trên đúng database (SSMS hay mặc định mở ở master)
GO
SET XACT_ABORT ON;        -- lỗi giữa chừng thì hủy toàn bộ transaction
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bắt buộc khi INSERT vào bảng có filtered index (uq_article_featured); sqlcmd mặc định tắt

DECLARE @now DATETIMEOFFSET(0) = SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00');
DECLARE @CRLF2 NVARCHAR(4) = CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10);   -- dòng trống ngăn cách các đoạn
DECLARE @article_id INT, @version_id INT, @image_source_id INT, @other_source_id INT;

BEGIN TRANSACTION;

-- ---------------------------------------------------------------------
-- BÀI 1: Topic 1 – Phòng Giáo dục
-- ---------------------------------------------------------------------
DECLARE @title1    NVARCHAR(255) = N'[TEST] Học sinh dùng AI làm bài tập: Thầy cô chấm điểm thế nào?';
DECLARE @subtitle1 NVARCHAR(255) = N'Nhiều trường xây dựng lại cách ra đề và chấm bài trong năm học 2026-2027';
DECLARE @sapo1     NVARCHAR(MAX) = N'Khi trợ lý AI có thể viết bài văn trong vài giây, giáo viên buộc phải thay đổi cách giao bài tập về nhà và cách đánh giá học sinh. Một số trường đã chuyển sang chấm quá trình thay vì chỉ chấm sản phẩm cuối.';
DECLARE @content1  NVARCHAR(MAX) = N'Từ đầu năm học, nhiều giáo viên phát hiện bài tập về nhà của học sinh có văn phong giống nhau một cách bất thường, đặc biệt ở các môn Ngữ văn và Lịch sử.' + @CRLF2
    + N'Thay vì cấm hoàn toàn, một số trường THCS và THPT đã yêu cầu học sinh nộp kèm dàn ý viết tay, nhật ký các bước làm bài, hoặc trình bày lại bài làm trước lớp.' + @CRLF2
    + N'Cô giáo dạy Ngữ văn tại một trường THPT cho biết, khi học sinh phải giải thích vì sao chọn luận điểm này, em nào thật sự hiểu bài sẽ thể hiện rõ ngay.' + @CRLF2
    + N'Các chuyên gia giáo dục cho rằng AI nên được xem là công cụ hỗ trợ học tập, nhưng nhà trường cần có quy định rõ ràng về việc khi nào được dùng và phải ghi chú việc sử dụng ra sao.';

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title1)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 ghi chú phỏng vấn
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (7, N'Ảnh học sinh làm bài trên máy tính', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Vũ Thanh Mai', DATEADD(DAY, -4, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (7, N'Ghi chú phỏng vấn giáo viên Ngữ văn', 'INTERVIEW', NULL,
            N'Phỏng vấn trực tiếp ngày 10/11/2026', DATEADD(DAY, -4, @now));
    SET @other_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (1, @image_source_id, DATEADD(DAY, -4, @now)),
           (1, @other_source_id, DATEADD(DAY, -4, @now));

    -- 2) Bài viết: trạng thái APPROVED => bắt buộc có submitted_at và completed_at (ck_article_completed)
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (1, 7, @title1, @subtitle1, @sapo1, @content1, N'Giáo dục', 'APPROVED',
            DATEADD(DAY, -4, @now), DATEADD(DAY, -3, @now), DATEADD(DAY, -3, @now), DATEADD(HOUR, -5, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Lead chọn Source từ Topic đưa vào bài; ảnh phải có chú thích (FR-07)
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -3, @now),
            N'Học sinh lớp 11 làm bài tập nhóm có sự hỗ trợ của trợ lý AI.', 1, 1),
           (@article_id, @other_source_id, DATEADD(DAY, -3, @now), NULL, 0, 2);

    -- 4) Submit lần đầu => version 1.0 (version_number = 1)
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, 7, 1, @title1, @subtitle1, @sapo1, @content1, NULL, DATEADD(DAY, -3, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt cấp phòng, rồi Thư ký Final Approve trên CÙNG version (R03, R05)
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, 3, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Có ví dụ thực tế, góc nhìn cân bằng. Chuyển Thư ký.', DATEADD(DAY, -2, @now)),
           (@version_id, 2, 'SECRETARY',  'REVIEW', 'APPROVED',
            N'Duyệt đăng. Có thể đăng ngay trong tuần.', DATEADD(HOUR, -5, @now));

    PRINT N'Đã thêm bài 1 (APPROVED), article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài 1 đã tồn tại, bỏ qua.';

-- ---------------------------------------------------------------------
-- BÀI 2: Topic 4 – Phòng Xã hội
-- ---------------------------------------------------------------------
DECLARE @title2    NVARCHAR(255) = N'[TEST] Chung cư mẫu về phân loại rác: Bí quyết nằm ở ban quản trị';
DECLARE @subtitle2 NVARCHAR(255) = N'Ghi nhận tại một khu chung cư đạt tỷ lệ phân loại rác trên 80%';
DECLARE @sapo2     NVARCHAR(MAX) = N'Trong khi nhiều nơi còn loay hoay, một khu chung cư tại TP.HCM đã duy trì việc phân loại rác tại nguồn suốt nửa năm nhờ cách tổ chức đơn giản: điểm tập kết rõ ràng, lịch thu gom cố định và nhắc nhở thường xuyên.';
DECLARE @content2  NVARCHAR(MAX) = N'Mỗi tầng của khu chung cư được bố trí ba thùng rác có màu khác nhau, kèm bảng hướng dẫn bằng hình ảnh dán ngay phía trên.' + @CRLF2
    + N'Ban quản trị cho biết, tháng đầu tiên tỷ lệ phân loại đúng chỉ khoảng 40%. Sau khi lập nhóm tình nguyện viên hướng dẫn vào buổi tối, con số này tăng lên hơn 80%.' + @CRLF2
    + N'Rác tái chế được bán lại cho đơn vị thu mua, số tiền thu được dùng để mua thêm thùng rác và quà tặng cho các hộ gia đình làm tốt.' + @CRLF2
    + N'Đại diện đơn vị thu gom nhận xét, mô hình này dễ nhân rộng vì không đòi hỏi chi phí lớn, chủ yếu cần sự kiên trì của ban quản trị và cư dân.';

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title2)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 tài liệu
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (14, N'Ảnh khu tập kết rác phân loại tại chung cư', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Trịnh Phương Thảo', DATEADD(DAY, -5, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (14, N'Báo cáo kết quả phân loại rác 6 tháng của ban quản trị', 'DOCUMENT', NULL,
            N'Tài liệu do ban quản trị chung cư cung cấp', DATEADD(DAY, -5, @now));
    SET @other_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (4, @image_source_id, DATEADD(DAY, -5, @now)),
           (4, @other_source_id, DATEADD(DAY, -5, @now));

    -- 2) Bài viết: trạng thái APPROVED => bắt buộc có submitted_at và completed_at (ck_article_completed)
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (4, 14, @title2, @subtitle2, @sapo2, @content2, N'Xã hội', 'APPROVED',
            DATEADD(DAY, -5, @now), DATEADD(DAY, -4, @now), DATEADD(DAY, -4, @now), DATEADD(HOUR, -3, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Lead chọn Source từ Topic đưa vào bài; ảnh phải có chú thích (FR-07)
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -4, @now),
            N'Ba thùng rác phân loại đặt tại mỗi tầng của khu chung cư.', 1, 1),
           (@article_id, @other_source_id, DATEADD(DAY, -4, @now), NULL, 0, 2);

    -- 4) Submit lần đầu => version 1.0 (version_number = 1)
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, 14, 1, @title2, @subtitle2, @sapo2, @content2, NULL, DATEADD(DAY, -4, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt cấp phòng, rồi Thư ký Final Approve trên CÙNG version (R03, R05)
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, 6, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Bài có số liệu và ý kiến đơn vị thu gom, đạt yêu cầu.', DATEADD(DAY, -1, @now)),
           (@version_id, 1, 'SECRETARY',  'REVIEW', 'APPROVED',
            N'Duyệt đăng. Nên hẹn giờ đăng sáng cuối tuần.', DATEADD(HOUR, -3, @now));

    PRINT N'Đã thêm bài 2 (APPROVED), article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài 2 đã tồn tại, bỏ qua.';

COMMIT TRANSACTION;

-- Kiểm tra: phải trả về 0 dòng
SELECT * FROM dbo.v_rule_violations;

-- Danh sách bài đang chờ xuất bản (màn "Chờ xuất bản" của Thư ký đọc đúng các bài này)
SELECT a.article_id, a.title, t.title AS topic_title, ld.full_name AS lead_name, a.completed_at
FROM dbo.article a
JOIN dbo.topic t    ON t.topic_id = a.topic_id
JOIN dbo.account ld ON ld.account_id = a.lead_id
WHERE a.status = 'APPROVED'
ORDER BY a.completed_at;
