-- =====================================================================
-- FL-01 Iteration 1 – Dữ liệu test: 2 phóng viên ở 2 phòng khác nhau.
-- Chỉ INSERT khi email chưa tồn tại; không đụng account / topic có sẵn của nhóm.
-- Chạy SAU fl01_iter1_migration.sql:
--   sqlcmd -S localhost,1433 -d cms_core -U sa -P <mật khẩu> -C -f 65001 -i database/fl01_seed.sql
--
-- Tài khoản (mật khẩu gốc chung: Fl01@Test123 – password_hash là BCrypt của chuỗi này):
--   reporter.kinhte@fl01.test   / Fl01@Test123   – Phóng viên, Phòng Kinh tế
--   reporter.giaoduc@fl01.test  / Fl01@Test123   – Phóng viên, Phòng Giáo dục
-- =====================================================================
SET XACT_ABORT ON;
SET NOCOUNT ON;

DECLARE @reporterRole INT = (SELECT role_id FROM dbo.role WHERE role_name = 'REPORTER');
DECLARE @hash VARCHAR(100) = '$2a$10$UVmtRygxtKmvd04EvPcdh.VVBUrTrUApgOrHrb0lm6IoaRWULGB4S';

IF NOT EXISTS (SELECT 1 FROM dbo.account WHERE email = 'reporter.kinhte@fl01.test')
    INSERT INTO dbo.account (role_id, department_id, username, full_name, password_hash, email, status)
    VALUES (@reporterRole,
            (SELECT department_id FROM dbo.department WHERE department_name = N'Phòng Kinh tế'),
            'fl01.reporter.kinhte', N'Phóng viên Test Kinh tế', @hash, 'reporter.kinhte@fl01.test', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM dbo.account WHERE email = 'reporter.giaoduc@fl01.test')
    INSERT INTO dbo.account (role_id, department_id, username, full_name, password_hash, email, status)
    VALUES (@reporterRole,
            (SELECT department_id FROM dbo.department WHERE department_name = N'Phòng Giáo dục'),
            'fl01.reporter.giaoduc', N'Phóng viên Test Giáo dục', @hash, 'reporter.giaoduc@fl01.test', 'ACTIVE');

SELECT a.email, a.full_name, r.role_name, d.department_name, a.status
  FROM dbo.account a
  JOIN dbo.role r ON r.role_id = a.role_id
  LEFT JOIN dbo.department d ON d.department_id = a.department_id
 WHERE a.email LIKE '%@fl01.test';
