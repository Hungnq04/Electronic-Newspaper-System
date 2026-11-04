-- =====================================================================
-- FL-03 – Dữ liệu test màn phân công của Trưởng phòng.
-- Tạo 2 Topic ACTIVE thuộc Phòng Kinh tế, cùng do Trưởng phòng Đặng Quang Vinh (account 5) phụ trách:
--   (1) Topic Thư ký giao xuống (top-down): đã có DEPARTMENT_HEAD, CHƯA có Lead
--       -> Trưởng phòng phải chọn Lead (+ Supporting nếu cần), nhập nhiệm vụ + deadline.
--   (2) Topic Reporter đề xuất, đã qua 2 cấp duyệt: có Lead (Ngô Thùy Linh) nhưng
--       nhiệm vụ / deadline còn TRỐNG (OQ-06) -> Trưởng phòng chỉ nhập nhiệm vụ + deadline.
--   (3), (4) Thêm 2 Topic Thư ký giao xuống, chưa có Lead.
--   (5), (6) Thêm 2 Topic Reporter đề xuất (Khôi, Linh), có Lead nhưng chưa có nhiệm vụ / deadline.
-- Chỉ INSERT khi Topic cùng tiêu đề chưa tồn tại, nên chạy lại nhiều lần không bị trùng.
-- Chạy SAU cms_core_sqlserver_v2.sql:
--   sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl03_seed.sql
--
-- Đăng nhập để test (mật khẩu demo: Cms@2026):
--   vinh.dang@cmscore.example – Trưởng phòng Kinh tế (người phân công)
--   linh.ngo@cmscore.example  – Lead của Topic (2)
--   khoi.do@cmscore.example   – Phóng viên Kinh tế, có thể chọn làm Lead / Supporting
-- =====================================================================
SET XACT_ABORT ON;
SET NOCOUNT ON;

DECLARE @secretary_id  INT = 1;   -- Lê Minh Châu (Thư ký tòa soạn)
DECLARE @dept_head_id  INT = 5;   -- Đặng Quang Vinh (Trưởng phòng Kinh tế)
DECLARE @lead_id       INT = 12;  -- Ngô Thùy Linh (Phóng viên Kinh tế)
DECLARE @department_id INT = 4;   -- Phòng Kinh tế
DECLARE @now DATETIMEOFFSET(0) = SWITCHOFFSET(SYSDATETIMEOFFSET(), '+07:00');
DECLARE @topic_id INT;

BEGIN TRANSACTION;

-- (1) Topic Thư ký giao xuống: ACTIVE ngay, không qua duyệt (BR-05)
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Lãi suất vay mua nhà cuối năm')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id)
    VALUES (@secretary_id, N'[TEST] Lãi suất vay mua nhà cuối năm',
            N'Diễn biến lãi suất vay mua nhà của các ngân hàng trong quý IV và tác động tới người mua nhà lần đầu.',
            N'Kinh tế', N'Định hướng của Ban biên tập cho chuyên đề bất động sản cuối năm.',
            DATEADD(DAY, 14, CAST(@now AS DATE)), 'HIGH', 'ACTIVE',
            DATEADD(HOUR, -2, @now), DATEADD(HOUR, -2, @now), @department_id);
    SET @topic_id = SCOPE_IDENTITY();

    -- Thư ký giao Topic cho Trưởng phòng; chưa có Lead / Supporting
    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary_id, 'DEPARTMENT_HEAD',
            NULL, N'Phân công Lead và nhóm hỗ trợ cho chuyên đề lãi suất.', 'ASSIGNED', DATEADD(HOUR, -1, @now));
END

-- (2) Topic Reporter đề xuất: đã được Trưởng phòng và Thư ký duyệt
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Chợ truyền thống trước sức ép siêu thị mini')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id, reviewed_by_head_id)
    VALUES (@lead_id, N'[TEST] Chợ truyền thống trước sức ép siêu thị mini',
            N'Tiểu thương ở chợ truyền thống thay đổi cách bán hàng thế nào khi siêu thị mini mở dày đặc.',
            N'Kinh tế', N'Nhiều sạp ở chợ dân sinh đóng cửa trong năm nay, bạn đọc gửi phản ánh.',
            DATEADD(DAY, 10, CAST(@now AS DATE)), 'NORMAL', 'ACTIVE',
            DATEADD(DAY, -2, @now), DATEADD(HOUR, -3, @now), @department_id, @dept_head_id);
    SET @topic_id = SCOPE_IDENTITY();

    -- 2 cấp duyệt Topic (rule R10)
    INSERT INTO dbo.topic_review (topic_id, reviewer_id, review_stage, decision, feedback, reviewed_at)
    VALUES (@topic_id, @dept_head_id, 'DEPARTMENT', 'APPROVED', N'Đề tài sát đời sống, chuyển Thư ký.', DATEADD(DAY, -1, @now)),
           (@topic_id, @secretary_id, 'SECRETARY',  'APPROVED', NULL, DATEADD(HOUR, -3, @now));

    -- Hệ thống tự tạo 2 Assignment khi Thư ký duyệt (mục 4.1 A).
    -- Lead: nhiệm vụ và deadline để TRỐNG, chờ Trưởng phòng bổ sung (OQ-06)
    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary_id, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -3, @now)),
           (@topic_id, @lead_id,      @secretary_id, 'LEAD',            NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -3, @now));
END

-- ===== Thêm dữ liệu: 2 Topic "Chưa có Lead" + 2 Topic "Chờ giao nhiệm vụ" =====
DECLARE @secretary2_id INT = 2;   -- Phạm Quốc Bảo (Thư ký tòa soạn)
DECLARE @lead2_id      INT = 13;  -- Đỗ Minh Khôi (Phóng viên Kinh tế)

-- (3) Thư ký giao xuống, CHƯA có Lead
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Giá vàng biến động mạnh dịp cuối năm')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id)
    VALUES (@secretary_id, N'[TEST] Giá vàng biến động mạnh dịp cuối năm',
            N'Chênh lệch giá vàng trong nước và thế giới, tâm lý người dân đầu tư vàng.',
            N'Kinh tế', N'Ban biên tập cần loạt bài phân tích thị trường vàng.',
            DATEADD(DAY, 7, CAST(@now AS DATE)), 'URGENT', 'ACTIVE',
            DATEADD(HOUR, -5, @now), DATEADD(HOUR, -5, @now), @department_id);
    SET @topic_id = SCOPE_IDENTITY();

    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary_id, 'DEPARTMENT_HEAD',
            NULL, N'Chọn Lead theo dõi thị trường vàng.', 'ASSIGNED', DATEADD(HOUR, -4, @now));
END

-- (4) Thư ký (người khác) giao xuống, CHƯA có Lead
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Doanh nghiệp nhỏ khó tiếp cận vốn vay')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id)
    VALUES (@secretary2_id, N'[TEST] Doanh nghiệp nhỏ khó tiếp cận vốn vay',
            N'Rào cản thủ tục và tài sản đảm bảo khiến doanh nghiệp nhỏ khó vay vốn ngân hàng.',
            N'Kinh tế', N'Phản hồi từ hiệp hội doanh nghiệp gửi tòa soạn.',
            DATEADD(DAY, 21, CAST(@now AS DATE)), 'NORMAL', 'ACTIVE',
            DATEADD(DAY, -1, @now), DATEADD(DAY, -1, @now), @department_id);
    SET @topic_id = SCOPE_IDENTITY();

    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary2_id, 'DEPARTMENT_HEAD',
            NULL, N'Phân công nhóm viết về vốn cho doanh nghiệp nhỏ.', 'ASSIGNED', DATEADD(HOUR, -20, @now));
END

-- (5) Khôi đề xuất, đã duyệt 2 cấp: có Lead, nhiệm vụ / deadline TRỐNG
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Giá xăng dầu tác động chi phí vận tải')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id, reviewed_by_head_id)
    VALUES (@lead2_id, N'[TEST] Giá xăng dầu tác động chi phí vận tải',
            N'Các doanh nghiệp vận tải điều chỉnh giá cước sau các đợt tăng giá xăng dầu.',
            N'Kinh tế', N'Giá cước taxi, xe khách tăng, nhiều bạn đọc quan tâm.',
            DATEADD(DAY, 12, CAST(@now AS DATE)), 'HIGH', 'ACTIVE',
            DATEADD(DAY, -3, @now), DATEADD(HOUR, -6, @now), @department_id, @dept_head_id);
    SET @topic_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_review (topic_id, reviewer_id, review_stage, decision, feedback, reviewed_at)
    VALUES (@topic_id, @dept_head_id,  'DEPARTMENT', 'APPROVED', N'Đề tài có tính thời sự.', DATEADD(DAY, -2, @now)),
           (@topic_id, @secretary2_id, 'SECRETARY',  'APPROVED', NULL, DATEADD(HOUR, -6, @now));

    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary2_id, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -6, @now)),
           (@topic_id, @lead2_id,     @secretary2_id, 'LEAD',            NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -6, @now));
END

-- (6) Linh đề xuất thêm một Topic (một Lead làm nhiều Topic được), nhiệm vụ / deadline TRỐNG
IF NOT EXISTS (SELECT 1 FROM dbo.topic WHERE title = N'[TEST] Mua sắm trực tuyến mùa sale 11.11')
BEGIN
    INSERT INTO dbo.topic (created_by, title, description, category, rationale, expected_date, priority, status,
                           created_at, updated_at, department_id, reviewed_by_head_id)
    VALUES (@lead_id, N'[TEST] Mua sắm trực tuyến mùa sale 11.11',
            N'Người tiêu dùng săn sale trên sàn thương mại điện tử và các chiêu giảm giá ảo.',
            N'Kinh tế', N'Mùa sale lớn sắp tới, cần cảnh báo người tiêu dùng.',
            DATEADD(DAY, 9, CAST(@now AS DATE)), 'NORMAL', 'ACTIVE',
            DATEADD(DAY, -4, @now), DATEADD(HOUR, -1, @now), @department_id, @dept_head_id);
    SET @topic_id = SCOPE_IDENTITY();

    INSERT INTO dbo.topic_review (topic_id, reviewer_id, review_stage, decision, feedback, reviewed_at)
    VALUES (@topic_id, @dept_head_id, 'DEPARTMENT', 'APPROVED', N'Đồng ý, chuyển Thư ký.', DATEADD(DAY, -3, @now)),
           (@topic_id, @secretary_id, 'SECRETARY',  'APPROVED', NULL, DATEADD(HOUR, -1, @now));

    INSERT INTO dbo.assignment (topic_id, account_id, assigned_by, assignment_role, deadline, requirements, status, assigned_at)
    VALUES (@topic_id, @dept_head_id, @secretary_id, 'DEPARTMENT_HEAD', NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -1, @now)),
           (@topic_id, @lead_id,      @secretary_id, 'LEAD',            NULL, NULL, 'ASSIGNED', DATEADD(HOUR, -1, @now));
END

COMMIT TRANSACTION;

-- Kiểm tra kết quả
SELECT t.topic_id, t.title, cr.full_name AS created_by, s.assignment_role, a.full_name AS assignee,
       s.requirements, s.deadline, s.status
  FROM dbo.topic t
  JOIN dbo.account cr ON cr.account_id = t.created_by
  LEFT JOIN dbo.assignment s ON s.topic_id = t.topic_id AND s.status <> 'REMOVED'
  LEFT JOIN dbo.account a ON a.account_id = s.account_id
 WHERE t.title LIKE N'[[]TEST]%'
 ORDER BY t.topic_id, s.assignment_role;

-- Phải trả về 0 dòng
SELECT * FROM dbo.v_rule_violations;
