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