-- Mật khẩu cho TẤT CẢ tài khoản dưới đây đều là: 123456
-- Chuỗi mã hóa BCrypt tương ứng: $2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu

INSERT INTO [nguoi_dung] ([ho_ten], [email], [mat_khau], [so_dien_thoai], [quyen], [trang_thai], [ngay_tao])
VALUES 
-- 1 Tài khoản Admin cao nhất
(N'Nguyễn Ngọc Tú', 'admin.tu@gmail.com', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu', '0901234567', 'ADMIN', 'HOAT_DONG', GETDATE()),

-- 2 Tài khoản Nhân viên (Dùng để test tính năng POS và cập nhật tiến độ làm bánh)
(N'Trần Văn Thu Ngân', 'thungan.pos@gmail.com', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu', '0912345678', 'NHAN_VIEN', 'HOAT_DONG', GETDATE()),
(N'Lê Thị Thợ Bếp', 'bep.banh@gmail.com', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu', '0923456789', 'NHAN_VIEN', 'HOAT_DONG', GETDATE()),

-- 2 Tài khoản Khách hàng (Dùng để test chức năng thêm Giỏ hàng, Đặt bánh online)
(N'Nguyễn Văn Khách', 'khachhang.demo@gmail.com', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu', '0934567890', 'KHACH_HANG', 'HOAT_DONG', GETDATE()),
(N'Lee Sang Hyeok', 'fan.t1.hcm@gmail.com', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E97k2FZbFvGFFaSsiTUIl.TCrFu', '0945678901', 'KHACH_HANG', 'HOAT_DONG', GETDATE());
GO

-- Xoá dữ liệu cũ nếu có (để chạy lại an toàn)
-- TRUNCATE TABLE [ca_lam_viec];  -- Bỏ comment nếu muốn reset

-- Thêm 3 ca chuẩn
INSERT INTO [ca_lam_viec] ([ten_ca], [gio_bat_dau], [gio_ket_thuc], [hoat_dong])
VALUES
  (N'Ca sáng',  '07:00:00', '13:00:00', 1),
  (N'Ca chiều', '13:00:00', '19:00:00', 1),
  (N'Ca tối',   '19:00:00', '23:00:00', 1);
GO

-- Kiểm tra kết quả
SELECT * FROM [ca_lam_viec];
GO