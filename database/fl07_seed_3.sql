-- =====================================================================
-- FL-07 – Thêm 3 bài của Lead Vũ Thanh Mai vào màn "Chờ xuất bản" của Thư ký tòa soạn (status APPROVED).
-- Cả 3 bài thuộc Topic 1 "AI trong giáo dục phổ thông" (Phòng Giáo dục) – topic Mai Vũ đang là Lead:
--   Lead: Vũ Thanh Mai (7) – Trưởng phòng: Trần Văn Hùng (3)
--   Thư ký duyệt cuối: Lê Minh Châu (1) cho bài 1 và 3, Phạm Quốc Bảo (2) cho bài 2
-- Mỗi bài đi đủ luồng: Submit version 1.0 -> Trưởng phòng Approve -> Thư ký Final Approve -> APPROVED.
-- Mỗi bài có 1 ảnh đại diện (có chú thích) + 1 nguồn khác để qua kiểm tra FR-07 (rule R19).
-- Tít bắt đầu bằng "[TEST] Mai Vũ" để dễ tìm.
-- Chỉ dùng dữ liệu mẫu của cms_core_sqlserver_v2.sql.
-- Chỉ INSERT khi bài cùng tiêu đề chưa tồn tại, nên chạy lại nhiều lần không bị trùng.
-- Chạy SAU cms_core_sqlserver_v2.sql (SSMS: mở file rồi F5), hoặc:
--   sqlcmd -S localhost,1433 -U sa -P <mật khẩu> -C -f 65001 -i database/fl07_seed_3.sql
--
-- Đăng nhập để test (mật khẩu demo: Cms@2026):
--   chau.le@cmscore.example hoặc bao.pham@cmscore.example – Thư ký tòa soạn -> mục "Chờ xuất bản"
-- =====================================================================
USE cms_core;             -- luôn chạy trên đúng database (SSMS hay mặc định mở ở master)
GO
SET XACT_ABORT ON;        -- lỗi giữa chừng thì hủy toàn bộ transaction
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bắt buộc khi INSERT vào bảng có filtered index (uq_article_featured); sqlcmd mặc định tắt

DECLARE @lead_id      INT = 7;   -- Vũ Thanh Mai (Lead của Topic 1)
DECLARE @dept_head_id INT = 3;   -- Trần Văn Hùng (Trưởng phòng Giáo dục, phụ trách Topic 1)
DECLARE @topic_id     INT = 1;   -- AI trong giáo dục phổ thông
DECLARE @now DATETIMEOFFSET(0) = SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00');
DECLARE @CRLF2 NVARCHAR(4) = CHAR(13) + CHAR(10) + CHAR(13) + CHAR(10);   -- dòng trống ngăn cách các đoạn
DECLARE @article_id INT, @version_id INT, @image_source_id INT, @other_source_id INT;

BEGIN TRANSACTION;

-- ---------------------------------------------------------------------
-- BÀI 1: Thư ký Lê Minh Châu (1) duyệt cuối
-- ---------------------------------------------------------------------
DECLARE @title1    NVARCHAR(255) = N'[TEST] Mai Vũ – Trợ lý AI chấm bài tập về nhà: Giáo viên được gì, mất gì?';
DECLARE @subtitle1 NVARCHAR(255) = N'Một số trường thí điểm dùng AI chấm sơ bộ bài tập trắc nghiệm và tự luận ngắn';
DECLARE @sapo1     NVARCHAR(MAX) = N'Công cụ chấm bài tự động giúp giáo viên tiết kiệm hàng giờ mỗi tuần, nhưng cũng đặt ra câu hỏi về độ chính xác và vai trò của người thầy trong việc nhận xét bài làm của học sinh.';
DECLARE @content1  NVARCHAR(MAX) = N'Tại một trường THCS ở Hà Nội, giáo viên Toán đã dùng trợ lý AI để chấm sơ bộ bài tập về nhà của 5 lớp trong suốt học kỳ một.' + @CRLF2
    + N'Theo các thầy cô, thời gian chấm bài giảm khoảng một nửa, phần thời gian còn lại được dành cho việc phụ đạo những học sinh còn yếu.' + @CRLF2
    + N'Tuy nhiên, với bài tự luận, AI đôi khi chấm sai những cách giải sáng tạo không giống đáp án mẫu, nên giáo viên vẫn phải xem lại từng bài.' + @CRLF2
    + N'Nhà trường quy định điểm do AI gợi ý chỉ mang tính tham khảo, điểm chính thức luôn do giáo viên quyết định và ghi nhận xét.';

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title1)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 ghi chú phỏng vấn
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Ảnh giáo viên xem kết quả chấm bài trên máy tính', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Vũ Thanh Mai', DATEADD(DAY, -6, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Ghi chú phỏng vấn giáo viên Toán THCS', 'INTERVIEW', NULL,
            N'Phỏng vấn trực tiếp tại trường', DATEADD(DAY, -6, @now));
    SET @other_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (@topic_id, @image_source_id, DATEADD(DAY, -6, @now)),
           (@topic_id, @other_source_id, DATEADD(DAY, -6, @now));

    -- 2) Bài viết: trạng thái APPROVED => bắt buộc có submitted_at và completed_at (ck_article_completed)
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (@topic_id, @lead_id, @title1, @subtitle1, @sapo1, @content1, N'Giáo dục', 'APPROVED',
            DATEADD(DAY, -6, @now), DATEADD(DAY, -5, @now), DATEADD(DAY, -5, @now), DATEADD(HOUR, -8, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Lead chọn Source từ Topic đưa vào bài; ảnh phải có chú thích (FR-07)
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -5, @now),
            N'Giáo viên kiểm tra lại điểm do trợ lý AI gợi ý trước khi trả bài.', 1, 1),
           (@article_id, @other_source_id, DATEADD(DAY, -5, @now), NULL, 0, 2);

    -- 4) Submit lần đầu => version 1.0 (version_number = 1)
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, @lead_id, 1, @title1, @subtitle1, @sapo1, @content1, NULL, DATEADD(DAY, -5, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt cấp phòng, rồi Thư ký Final Approve trên CÙNG version (R03, R05)
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, @dept_head_id, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Có số liệu thời gian tiết kiệm, nêu được cả mặt hạn chế. Chuyển Thư ký.', DATEADD(DAY, -3, @now)),
           (@version_id, 1, 'SECRETARY', 'REVIEW', 'APPROVED',
            N'Duyệt đăng.', DATEADD(HOUR, -8, @now));

    PRINT N'Đã thêm bài 1 (APPROVED), article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài 1 đã tồn tại, bỏ qua.';

-- ---------------------------------------------------------------------
-- BÀI 2: Thư ký Phạm Quốc Bảo (2) duyệt cuối
-- ---------------------------------------------------------------------
DECLARE @title2    NVARCHAR(255) = N'[TEST] Mai Vũ – Dạy học sinh hỏi AI đúng cách: Môn học mới trong giờ sinh hoạt lớp';
DECLARE @subtitle2 NVARCHAR(255) = N'Học sinh được hướng dẫn kiểm chứng thông tin và ghi nguồn khi dùng trợ lý AI';
DECLARE @sapo2     NVARCHAR(MAX) = N'Thay vì cấm, một số trường THPT chọn cách dạy học sinh đặt câu hỏi cho AI, đối chiếu câu trả lời với sách giáo khoa và ghi rõ phần nào có sự hỗ trợ của công cụ.';
DECLARE @content2  NVARCHAR(MAX) = N'Mỗi tuần một tiết, học sinh lớp 10 của một trường THPT tại Đà Nẵng được thực hành đặt câu hỏi cho trợ lý AI về chính bài học trên lớp.' + @CRLF2
    + N'Giáo viên chủ nhiệm cố tình chọn những câu hỏi mà AI dễ trả lời sai, sau đó yêu cầu học sinh tìm ra chỗ sai bằng sách giáo khoa và tài liệu chính thống.' + @CRLF2
    + N'Nhiều em cho biết trước đây thường chép nguyên câu trả lời của AI, giờ mới nhận ra công cụ này có thể đưa thông tin không chính xác một cách rất tự tin.' + @CRLF2
    + N'Ban giám hiệu dự kiến mở rộng mô hình cho toàn khối 11 và 12 từ học kỳ hai.';

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title2)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 trang web
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Ảnh tiết sinh hoạt lớp thực hành với trợ lý AI', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Vũ Thanh Mai', DATEADD(DAY, -5, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Hướng dẫn sử dụng AI trong trường học trên cổng thông tin Bộ GD&ĐT', 'WEBSITE',
            N'https://moet.gov.vn', N'Tài liệu tham khảo chính thống', DATEADD(DAY, -5, @now));
    SET @other_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (@topic_id, @image_source_id, DATEADD(DAY, -5, @now)),
           (@topic_id, @other_source_id, DATEADD(DAY, -5, @now));

    -- 2) Bài viết
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (@topic_id, @lead_id, @title2, @subtitle2, @sapo2, @content2, N'Giáo dục', 'APPROVED',
            DATEADD(DAY, -5, @now), DATEADD(DAY, -4, @now), DATEADD(DAY, -4, @now), DATEADD(HOUR, -6, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Source đưa vào bài; ảnh có chú thích
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -4, @now),
            N'Học sinh lớp 10 đối chiếu câu trả lời của AI với sách giáo khoa.', 1, 1),
           (@article_id, @other_source_id, DATEADD(DAY, -4, @now), NULL, 0, 2);

    -- 4) Version 1.0
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, @lead_id, 1, @title2, @subtitle2, @sapo2, @content2, NULL, DATEADD(DAY, -4, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt, rồi Thư ký Final Approve trên cùng version
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, @dept_head_id, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Góc tiếp cận hay, có trích dẫn học sinh. Chuyển Thư ký.', DATEADD(DAY, -2, @now)),
           (@version_id, 2, 'SECRETARY', 'REVIEW', 'APPROVED',
            N'Duyệt đăng. Có thể đăng cùng chuyên đề AI trong giáo dục.', DATEADD(HOUR, -6, @now));

    PRINT N'Đã thêm bài 2 (APPROVED), article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài 2 đã tồn tại, bỏ qua.';

-- ---------------------------------------------------------------------
-- BÀI 3: Thư ký Lê Minh Châu (1) duyệt cuối
-- ---------------------------------------------------------------------
DECLARE @title3    NVARCHAR(255) = N'[TEST] Mai Vũ – Phụ huynh lo con phụ thuộc AI: Nhà trường cần làm gì?';
DECLARE @subtitle3 NVARCHAR(255) = N'Khảo sát nhanh tại 3 trường tiểu học cho thấy phần lớn phụ huynh muốn có quy định rõ ràng';
DECLARE @sapo3     NVARCHAR(MAX) = N'Không ít phụ huynh lo ngại con em mình dùng trợ lý AI để làm hộ bài tập, dần mất khả năng tự suy nghĩ. Các chuyên gia khuyên nhà trường và gia đình cần phối hợp đặt ra giới hạn phù hợp với từng lứa tuổi.';
DECLARE @content3  NVARCHAR(MAX) = N'Chị Hoa, phụ huynh có con học lớp 5, kể rằng con chị từng nộp một bài văn tả cảnh rất hay nhưng không trả lời được khi cô giáo hỏi về chi tiết trong bài.' + @CRLF2
    + N'Theo khảo sát nhanh của phóng viên tại 3 trường tiểu học, đa số phụ huynh được hỏi mong nhà trường ban hành quy định cụ thể về việc học sinh dùng AI ở nhà.' + @CRLF2
    + N'Một chuyên gia tâm lý giáo dục cho rằng với học sinh tiểu học, AI chỉ nên dùng khi có người lớn bên cạnh và chủ yếu để tra cứu, không dùng để làm bài thay.' + @CRLF2
    + N'Một số trường đã gửi thư hướng dẫn cho phụ huynh, kèm gợi ý các câu hỏi để cha mẹ kiểm tra xem con có thật sự hiểu bài hay không.';

IF NOT EXISTS (SELECT 1 FROM dbo.article WHERE title = @title3)
BEGIN
    -- 1) Lead thêm 2 Source vào Topic: 1 ảnh + 1 tài liệu
    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Ảnh phụ huynh hướng dẫn con học bài buổi tối', 'IMAGE',
            N'https://res.cloudinary.com/demo/image/upload/sample.jpg',
            N'Ảnh: Vũ Thanh Mai', DATEADD(DAY, -4, @now));
    SET @image_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.source (added_by, title, source_type, file_url, description, uploaded_at)
    VALUES (@lead_id, N'Kết quả khảo sát nhanh phụ huynh tại 3 trường tiểu học', 'DOCUMENT', NULL,
            N'Phóng viên tự thực hiện, 120 phiếu', DATEADD(DAY, -4, @now));
    SET @other_source_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_source (topic_id, source_id, attached_at)
    VALUES (@topic_id, @image_source_id, DATEADD(DAY, -4, @now)),
           (@topic_id, @other_source_id, DATEADD(DAY, -4, @now));

    -- 2) Bài viết
    INSERT INTO dbo.article (topic_id, lead_id, title, subtitle, sapo, content, category, status,
                             created_at, updated_at, submitted_at, completed_at)
    VALUES (@topic_id, @lead_id, @title3, @subtitle3, @sapo3, @content3, N'Giáo dục', 'APPROVED',
            DATEADD(DAY, -4, @now), DATEADD(DAY, -3, @now), DATEADD(DAY, -3, @now), DATEADD(HOUR, -2, @now));
    SET @article_id = SCOPE_IDENTITY();

    -- 3) Source đưa vào bài; ảnh có chú thích
    INSERT INTO dbo.article_source (article_id, source_id, attached_at, caption, is_featured, display_order)
    VALUES (@article_id, @image_source_id, DATEADD(DAY, -3, @now),
            N'Phụ huynh cùng con kiểm tra bài tập về nhà.', 1, 1),
           (@article_id, @other_source_id, DATEADD(DAY, -3, @now), NULL, 0, 2);

    -- 4) Version 1.0
    INSERT INTO dbo.article_version (article_id, created_by, version_number, title, subtitle, sapo, content,
                                     change_summary, created_at)
    VALUES (@article_id, @lead_id, 1, @title3, @subtitle3, @sapo3, @content3, NULL, DATEADD(DAY, -3, @now));
    SET @version_id = SCOPE_IDENTITY();

    -- 5) Trưởng phòng duyệt, rồi Thư ký Final Approve trên cùng version
    INSERT INTO dbo.article_review (version_id, reviewer_id, review_stage, review_kind, decision, feedback, reviewed_at)
    VALUES (@version_id, @dept_head_id, 'DEPARTMENT', 'REVIEW', 'APPROVED',
            N'Có ý kiến phụ huynh và chuyên gia, đạt yêu cầu.', DATEADD(DAY, -1, @now)),
           (@version_id, 1, 'SECRETARY', 'REVIEW', 'APPROVED',
            N'Duyệt đăng. Nên hẹn giờ đăng sáng thứ Hai.', DATEADD(HOUR, -2, @now));

    PRINT N'Đã thêm bài 3 (APPROVED), article_id = ' + CAST(@article_id AS NVARCHAR(10));
END
ELSE
    PRINT N'Bài 3 đã tồn tại, bỏ qua.';

COMMIT TRANSACTION;

-- Kiểm tra: không được có dòng nào của các bài vừa thêm
SELECT * FROM dbo.v_rule_violations;

-- Các bài của Mai Vũ đang chờ xuất bản
SELECT a.article_id, a.title, a.status, a.completed_at
FROM dbo.article a
WHERE a.lead_id = 7 AND a.status = 'APPROVED'
ORDER BY a.completed_at;
