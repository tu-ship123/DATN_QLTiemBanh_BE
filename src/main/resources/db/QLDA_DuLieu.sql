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

-- =======================================================
-- Dữ liệu THẬT cho phụ kiện trang trí bánh 3D (T050/T051)
-- Trước đây bảng này chưa có dữ liệu mẫu -> API GET /api/v1/accessories luôn trả về
-- mảng rỗng -> FE (useDecorAccessories.js) luôn phải dùng FALLBACK_ACCESSORIES (id giả
-- dạng 'fallback-1'..'fallback-5'). 5 dòng dưới đây tạo dữ liệu thật tương ứng 1-1 với
-- FALLBACK_ACCESSORIES để khi seed vào DB, id thật sẽ lần lượt là 1, 2, 3, 4, 5 và khớp
-- với các phuKienId đã được cập nhật trong fe/src/data/cakeTemplates.js.
-- model_3d_url trỏ thẳng tới các file .glb đã có sẵn trong fe/public/models/ - CakeBuilder3D.vue
-- giờ ưu tiên đọc cột này (dữ liệu thật từ DB), chỉ khi nào NULL mới rơi về đoán theo tên (cũ).
INSERT INTO [phu_kien_trang_tri] ([ten_phu_kien], [don_gia], [so_luong_ton], [anh_phu_kien], [model_3d_url], [hoat_dong])
VALUES
  (N'Nến số sinh nhật', 15000, 50, NULL, '/models/birthday_candle.glb', 1),          -- id = 1
  (N'Hoa kem hồng pastel', 25000, 30, NULL, '/models/sweet_strawberry_macaron.glb', 1), -- id = 2 (dùng tạm model macaron)
  (N'Topper "Happy Birthday"', 20000, 40, NULL, NULL, 1),                              -- id = 3 (chưa có model thật -> hình mẫu dựng sẵn)
  (N'Trái cây tươi trang trí', 35000, 25, NULL, '/models/free_raspberry.glb', 1),     -- id = 4
  (N'Bánh quy Oreo', 18000, 35, NULL, '/models/oreo.glb', 1);                          -- id = 5
GO

-- Kiểm tra kết quả
SELECT * FROM [phu_kien_trang_tri];
GO

-- =======================================================
-- Dữ liệu mẫu cho DANH MỤC và SẢN PHẨM
-- =======================================================

INSERT INTO [danh_muc] ([ten_danh_muc], [mo_ta], [anh_dai_dien], [hoat_dong])
VALUES
  (N'Bánh kem sinh nhật', N'Các loại bánh kem trang trí cho tiệc sinh nhật, có thể tùy chỉnh theo yêu cầu', 'https://images.unsplash.com/photo-1559553156-2e97137af16f?w=800&q=80&auto=format&fit=crop', 1),
  (N'Bánh ngọt Âu', N'Bánh ngọt phong cách Âu: tiramisu, mousse, cheesecake...', 'https://images.unsplash.com/photo-1702925614886-50ad13c88d3f?w=800&q=80&auto=format&fit=crop', 1),
  (N'Bánh mì tươi', N'Bánh mì tươi mới ra lò mỗi ngày', 'https://images.unsplash.com/photo-1719161148345-c88b05af8186?w=800&q=80&auto=format&fit=crop', 1),
  (N'Bánh Trung Thu', N'Bánh Trung Thu truyền thống và hiện đại theo mùa', NULL, 1),
  (N'Cupcake & Muffin', N'Bánh cupcake, muffin nhỏ xinh nhiều hương vị', 'https://images.unsplash.com/photo-1486427944299-d1955d23e34d?w=800&q=80&auto=format&fit=crop', 1);
GO

-- Kiểm tra kết quả
SELECT * FROM [danh_muc];
GO

-- Lưu ý: dùng subquery lấy [danh_muc_id] theo tên để không phụ thuộc thứ tự IDENTITY
-- Ảnh lấy thật từ Unsplash (giấy phép Unsplash License - miễn phí dùng thương mại, không cần credit).
-- Một số sản phẩm cùng danh mục dùng chung 1 ảnh minh họa do chưa tìm được ảnh khớp 1-1 cho từng món;
-- bạn có thể thay bằng ảnh chụp thật của tiệm sau này.
INSERT INTO [san_pham] ([danh_muc_id], [ten_san_pham], [don_gia], [so_luong_ton], [anh_san_pham], [trang_thai], [mo_ta])
VALUES
  -- Bánh kem sinh nhật
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh kem sinh nhật'), N'Bánh kem dâu tây', 250000, 20, 'https://images.unsplash.com/photo-1559553156-2e97137af16f?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh kem phủ kem tươi và dâu tây tươi, phù hợp cho tiệc sinh nhật'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh kem sinh nhật'), N'Bánh kem socola', 270000, 15, 'https://images.unsplash.com/photo-1564844536308-75c540dbf14e?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh kem socola đậm vị, nhân socola tan chảy'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh kem sinh nhật'), N'Bánh kem vani hoa hồng', 280000, 10, 'https://images.unsplash.com/photo-1486427944299-d1955d23e34d?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh kem vani trang trí hoa kem hồng pastel'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh kem sinh nhật'), N'Bánh kem trái cây tổng hợp', 300000, 12, 'https://images.unsplash.com/photo-1559553156-2e97137af16f?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh kem phủ trái cây tươi theo mùa'),

  -- Bánh ngọt Âu
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh ngọt Âu'), N'Tiramisu', 65000, 25, 'https://images.unsplash.com/photo-1564844536308-75c540dbf14e?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh tiramisu vị cà phê truyền thống Ý'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh ngọt Âu'), N'Cheesecake phô mai', 70000, 20, 'https://images.unsplash.com/photo-1702925614886-50ad13c88d3f?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh phô mai béo mịn, đế bánh quy giòn'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh ngọt Âu'), N'Mousse xoài', 60000, 18, 'https://images.unsplash.com/photo-1702925614886-50ad13c88d3f?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh mousse vị xoài tươi mát'),

  -- Bánh mì tươi
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh mì tươi'), N'Bánh mì bơ sữa', 15000, 50, 'https://images.unsplash.com/photo-1719161148345-c88b05af8186?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh mì mềm thơm bơ sữa, mới ra lò'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh mì tươi'), N'Bánh mì nho khô óc chó', 25000, 30, 'https://images.unsplash.com/photo-1719161148345-c88b05af8186?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh mì nguyên cám kết hợp nho khô và óc chó'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh mì tươi'), N'Bánh mì hoa cúc', 18000, 40, 'https://images.unsplash.com/photo-1719161148345-c88b05af8186?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Bánh mì mềm dạng hoa, thơm bơ'),

  -- Bánh Trung Thu
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh Trung Thu'), N'Bánh Trung Thu thập cẩm', 85000, 40, NULL, 'DANG_BAN', N'Bánh nướng nhân thập cẩm truyền thống'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh Trung Thu'), N'Bánh Trung Thu đậu xanh trứng muối', 90000, 35, NULL, 'DANG_BAN', N'Bánh nướng nhân đậu xanh và trứng muối béo bùi'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Bánh Trung Thu'), N'Bánh Trung Thu dẻo trà xanh', 80000, 25, NULL, 'DANG_BAN', N'Bánh dẻo nhân trà xanh thanh mát'),

  -- Cupcake & Muffin
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Cupcake & Muffin'), N'Cupcake vani kem bơ', 30000, 45, 'https://images.unsplash.com/photo-1486427944299-d1955d23e34d?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Cupcake vani phủ kem bơ mềm mịn'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Cupcake & Muffin'), N'Muffin việt quất', 32000, 30, 'https://images.unsplash.com/photo-1702925614886-50ad13c88d3f?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Muffin mềm xốp nhân việt quất tươi'),
  ((SELECT id FROM [danh_muc] WHERE [ten_danh_muc] = N'Cupcake & Muffin'), N'Cupcake red velvet', 35000, 20, 'https://images.unsplash.com/photo-1564844536308-75c540dbf14e?w=800&q=80&auto=format&fit=crop', 'DANG_BAN', N'Cupcake red velvet phủ kem phô mai');
GO

-- Kiểm tra kết quả
SELECT sp.[id], sp.[ten_san_pham], dm.[ten_danh_muc], sp.[don_gia], sp.[so_luong_ton]
FROM [san_pham] sp
LEFT JOIN [danh_muc] dm ON sp.[danh_muc_id] = dm.[id];
GO

-- ═══════════════════════════════════════════════════════════════════════════
-- Dọn các bản ghi "Bánh thiết kế 3D tùy chỉnh" bị tạo trùng trong bảng san_pham
-- (lỗi cũ: mỗi lần khách bấm "Đặt bánh này" mà tìm không ra bản ghi cũ, hệ thống
-- lại tạo thêm 1 bản ghi mới -> ra các id 17,18,19,20... như trong ảnh bạn gửi).
--
-- Cách chạy: mở file này trong SSMS, chọn đúng database QLDA_TiemBanh rồi Execute.
-- Script CHỈ xóa các đơn hàng/giỏ hàng KHÔNG tham chiếu tới, để tránh mất dữ liệu
-- thật của khách đã đặt bánh 3D trước đó.
-- ═══════════════════════════════════════════════════════════════════════════

-- B1. Xem trước các bản ghi trùng (kiểm tra lại trước khi xóa)
SELECT id, ten_san_pham, don_gia, danh_muc_id, trang_thai, ngay_tao
FROM san_pham
WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh'
ORDER BY id ASC;

-- B2. Xác định id CŨ NHẤT sẽ được GIỮ LẠI làm bản ghi chuẩn
DECLARE @idGiuLai INT = (
    SELECT MIN(id) FROM san_pham WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh'
);

-- B3. Chuyển hướng mọi chi_tiet_gio_hang / chi_tiet_don_hang đang trỏ vào các bản ghi
--     trùng (id khác @idGiuLai) về lại @idGiuLai, để không làm hỏng đơn/giỏ hàng cũ
UPDATE chi_tiet_gio_hang
SET san_pham_id = @idGiuLai
WHERE san_pham_id IN (
    SELECT id FROM san_pham WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh' AND id <> @idGiuLai
);

UPDATE chi_tiet_don_hang
SET san_pham_id = @idGiuLai
WHERE san_pham_id IN (
    SELECT id FROM san_pham WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh' AND id <> @idGiuLai
);

-- B4. Xóa các bản ghi trùng, chỉ giữ lại @idGiuLai
DELETE FROM san_pham
WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh' AND id <> @idGiuLai;

-- B5. Kiểm tra lại - phải chỉ còn đúng 1 dòng
SELECT id, ten_san_pham, don_gia, danh_muc_id, trang_thai, ngay_tao
FROM san_pham
WHERE ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh';