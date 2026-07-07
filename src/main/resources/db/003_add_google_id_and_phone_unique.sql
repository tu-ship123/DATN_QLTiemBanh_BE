-- ═══════════════════════════════════════════════════════════════════════════
-- T065: API Đăng ký OTP SĐT + Đăng nhập Google OAuth2
--
-- Script này: (1) thêm cột google_id vào bảng nguoi_dung để liên kết tài
-- khoản Google (lưu "sub" - định danh duy nhất & không đổi của Google, KHÔNG
-- dùng email để liên kết vì email có thể đổi/không đáng tin 100%), và (2)
-- thêm ràng buộc UNIQUE cho so_dien_thoai để đảm bảo 1 số điện thoại chỉ
-- gắn với đúng 1 tài khoản khi đăng ký bằng OTP SĐT.
--
-- Lưu ý: SQL Server cho phép NHIỀU giá trị NULL trong 1 cột UNIQUE, nên các
-- tài khoản cũ chưa có SĐT / chưa liên kết Google vẫn không bị ảnh hưởng.
--
-- CÁCH CHẠY: mở file này trong SSMS, chọn đúng database rồi Execute.
-- Chạy lại nhiều lần vẫn an toàn (không tạo thêm lỗi).
-- ═══════════════════════════════════════════════════════════════════════════

-- B1. Thêm cột google_id nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('nguoi_dung') AND name = 'google_id'
)
BEGIN
    ALTER TABLE nguoi_dung ADD google_id NVARCHAR(255) NULL;
END
GO

-- B2. Thêm ràng buộc UNIQUE cho google_id nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_nguoi_dung_google_id' AND object_id = OBJECT_ID('nguoi_dung')
)
BEGIN
    ALTER TABLE nguoi_dung ADD CONSTRAINT UQ_nguoi_dung_google_id UNIQUE (google_id);
END
GO

-- B3. Trước khi thêm UNIQUE cho so_dien_thoai, dọn các giá trị rỗng ('') về
--     NULL (rỗng khác NULL và có thể gây đụng UNIQUE nếu tồn tại > 1 dòng '')
UPDATE nguoi_dung SET so_dien_thoai = NULL WHERE so_dien_thoai = N'';
GO

-- B4. Thêm ràng buộc UNIQUE cho so_dien_thoai nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_nguoi_dung_so_dien_thoai' AND object_id = OBJECT_ID('nguoi_dung')
)
BEGIN
    ALTER TABLE nguoi_dung ADD CONSTRAINT UQ_nguoi_dung_so_dien_thoai UNIQUE (so_dien_thoai);
END
GO
