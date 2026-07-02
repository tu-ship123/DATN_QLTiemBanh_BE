-- ═══════════════════════════════════════════════════════════════════════════
-- FIX: "Bánh thiết kế 3D tùy chỉnh" (sản phẩm nội bộ dùng làm marker cho mọi
-- bánh khách tự thiết kế ở CakeBuilder3D) đang bị hiện ra công khai ở trang
-- chủ / trang sản phẩm khách hàng / trang quản lý sản phẩm admin.
--
-- NGUYÊN NHÂN: code cũ chỉ "giấu" sản phẩm này bằng cách so khớp CHÍNH XÁC
-- theo tên (sp.ten_san_pham <> N'Bánh thiết kế 3D tùy chỉnh'). Trước đây từng
-- có lỗi khiến hệ thống tạo trùng nhiều bản ghi marker (id 17,18,19,20...);
-- chỉ cần 1 trong số đó có tên lệch đi (thừa khoảng trắng, khác hoa/thường...)
-- là sẽ lọt qua điều kiện so khớp và hiện ra công khai cho tất cả mọi người.
--
-- CÁCH SỬA TẬN GỐC: thêm hẳn 1 cột la_noi_bo (đánh dấu "sản phẩm nội bộ,
-- không bao giờ hiển thị công khai") và lọc theo cột này thay vì so tên.
-- Script này: (1) thêm cột, (2) gộp toàn bộ bản ghi marker cũ (dù tên có lệch
-- thế nào) về đúng 1 bản ghi CŨ NHẤT rồi đánh dấu la_noi_bo = 1 cho nó,
-- (3) chuyển hướng mọi giỏ hàng/đơn hàng cũ đang trỏ vào các bản ghi trùng
-- về lại bản ghi được giữ, để không mất dữ liệu đơn hàng thật của khách.
--
-- CÁCH CHẠY: mở file này trong SSMS, chọn đúng database (VD: QLDA_TiemBanh)
-- rồi Execute. Chạy lại nhiều lần vẫn an toàn (không tạo thêm lỗi).
-- ═══════════════════════════════════════════════════════════════════════════

-- B0. Thêm cột la_noi_bo nếu chưa có
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('san_pham') AND name = 'la_noi_bo'
)
BEGIN
    ALTER TABLE san_pham ADD la_noi_bo BIT NOT NULL DEFAULT 0;
END
GO

-- B1. Xem trước mọi bản ghi CÓ THỂ là marker: đã đánh dấu la_noi_bo = 1 sẵn,
--     HOẶC tên gần khớp "Bánh thiết kế 3D tùy chỉnh" (bỏ qua khoảng trắng
--     thừa 2 đầu + không phân biệt hoa/thường/dấu, dùng collation AI = Accent
--     Insensitive để bắt luôn cả trường hợp lệch cách encode dấu tiếng Việt)
SELECT id, ten_san_pham, don_gia, trang_thai, la_noi_bo, ngay_tao
FROM san_pham
WHERE la_noi_bo = 1
   OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
      = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI
ORDER BY id ASC;

-- B2. Xác định id CŨ NHẤT trong nhóm trên sẽ được GIỮ LẠI làm bản ghi chuẩn
DECLARE @idGiuLai INT = (
    SELECT MIN(id) FROM san_pham
    WHERE la_noi_bo = 1
       OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
          = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI
);

IF @idGiuLai IS NOT NULL
BEGIN
    -- B3. Chuyển hướng mọi giỏ hàng / đơn hàng cũ đang trỏ vào các bản ghi
    --     trùng (id khác @idGiuLai) về lại @idGiuLai, tránh mất dữ liệu thật
    UPDATE chi_tiet_gio_hang
    SET san_pham_id = @idGiuLai
    WHERE san_pham_id IN (
        SELECT id FROM san_pham
        WHERE id <> @idGiuLai
          AND (la_noi_bo = 1
               OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
                  = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI)
    );

    UPDATE chi_tiet_don_hang
    SET san_pham_id = @idGiuLai
    WHERE san_pham_id IN (
        SELECT id FROM san_pham
        WHERE id <> @idGiuLai
          AND (la_noi_bo = 1
               OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
                  = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI)
    );

    -- B4. Xóa các bản ghi trùng, chỉ giữ lại @idGiuLai
    DELETE FROM san_pham
    WHERE id <> @idGiuLai
      AND (la_noi_bo = 1
           OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
              = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI);

    -- B5. Chuẩn hoá lại tên + đánh dấu la_noi_bo = 1 cho bản ghi được giữ
    --     (đây là bước QUYẾT ĐỊNH khiến nó không bao giờ hiện ra công khai nữa)
    UPDATE san_pham
    SET ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh',
        la_noi_bo = 1
    WHERE id = @idGiuLai;
END
GO

-- B6. Kiểm tra lại - phải chỉ còn ĐÚNG 1 dòng, và la_noi_bo phải = 1
SELECT id, ten_san_pham, don_gia, trang_thai, la_noi_bo, ngay_tao
FROM san_pham
WHERE la_noi_bo = 1
   OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
      = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI;
GO
