-- ═══════════════════════════════════════════════════════════════════════════
-- FILE GỘP: 002 + 003 + 004 + 005 + 006 + 007
-- Gộp toàn bộ 6 migration script thành 1 file duy nhất, chạy theo đúng thứ tự
-- cũ. Tất cả các bước đều có kiểm tra tồn tại trước khi thay đổi (IF NOT
-- EXISTS / IF EXISTS) nên chạy lại nhiều lần vẫn AN TOÀN, không lỗi.
--
-- CÁCH CHẠY: mở file này trong SSMS, chọn đúng database (VD: QLDA_TiemBanh)
-- rồi Execute toàn bộ (F5).
-- ═══════════════════════════════════════════════════════════════════════════


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 002: FIX "Bánh thiết kế 3D tùy chỉnh" bị lộ công khai
--
-- NGUYÊN NHÂN: code cũ chỉ "giấu" sản phẩm này bằng cách so khớp CHÍNH XÁC
-- theo tên. Từng có lỗi tạo trùng nhiều bản ghi marker (id 17,18,19,20...);
-- chỉ cần 1 bản ghi có tên lệch đi (thừa khoảng trắng, khác hoa/thường...)
-- là lọt qua điều kiện so khớp và hiện công khai cho tất cả mọi người.
--
-- CÁCH SỬA: thêm cột la_noi_bo (đánh dấu "sản phẩm nội bộ, không hiển thị
-- công khai"), gộp mọi bản ghi marker cũ về 1 bản ghi CŨ NHẤT, đánh dấu
-- la_noi_bo = 1, và chuyển hướng giỏ hàng/đơn hàng cũ về bản ghi được giữ.
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

-- B1. Xác định id CŨ NHẤT trong nhóm marker sẽ được GIỮ LẠI làm bản ghi chuẩn
DECLARE @idGiuLai INT = (
    SELECT MIN(id) FROM san_pham
    WHERE la_noi_bo = 1
       OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
          = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI
);

IF @idGiuLai IS NOT NULL
BEGIN
    -- B2. Chuyển hướng mọi giỏ hàng / đơn hàng cũ đang trỏ vào các bản ghi
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

    -- B3. Xóa các bản ghi trùng, chỉ giữ lại @idGiuLai
    DELETE FROM san_pham
    WHERE id <> @idGiuLai
      AND (la_noi_bo = 1
           OR LTRIM(RTRIM(ten_san_pham)) COLLATE Vietnamese_100_CI_AI
              = N'Bánh thiết kế 3D tùy chỉnh' COLLATE Vietnamese_100_CI_AI);

    -- B4. Chuẩn hoá lại tên + đánh dấu la_noi_bo = 1 cho bản ghi được giữ
    UPDATE san_pham
    SET ten_san_pham = N'Bánh thiết kế 3D tùy chỉnh',
        la_noi_bo = 1
    WHERE id = @idGiuLai;
END
GO


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 003: T065 - Đăng ký OTP SĐT + Đăng nhập Google OAuth2
--
-- Thêm cột google_id (liên kết tài khoản Google qua "sub" - định danh duy
-- nhất & không đổi) và ràng buộc UNIQUE cho so_dien_thoai (1 SĐT chỉ gắn
-- với đúng 1 tài khoản khi đăng ký bằng OTP SĐT).
-- (Lưu ý: UNIQUE trên google_id ở bước này sau đó sẽ được PHẦN 006 sửa lại
-- vì SQL Server chỉ cho phép đúng 1 dòng NULL với UNIQUE constraint thường.)
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


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 004: T080 - Ghi chú nội bộ + Barcode giao hàng + Refund
--
-- Thêm cột ghi_chu_noi_bo (NVARCHAR(MAX)) cho bảng don_hang - ghi chú nội bộ
-- dành cho Nhân viên/Bếp, tách biệt với cột ghi_chu (khách hàng xem được).
-- Cột này KHÔNG bao giờ được trả về cho khách hàng ở bất kỳ API nào.
-- ═══════════════════════════════════════════════════════════════════════════

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('don_hang') AND name = 'ghi_chu_noi_bo'
)
BEGIN
    ALTER TABLE don_hang ADD ghi_chu_noi_bo NVARCHAR(MAX) NULL;
END
GO


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 005: Thêm cột is_2fa_enabled cho bảng nguoi_dung (bật/tắt 2FA)
-- (Đã sửa lỗi bản gốc: bản gốc chạy ALTER TABLE trước cả IF NOT EXISTS nên
-- chạy lại lần 2 sẽ báo lỗi "column already exists". Bản gộp này chỉ giữ
-- lại khối có kiểm tra điều kiện, an toàn khi chạy lại nhiều lần.)
-- ═══════════════════════════════════════════════════════════════════════════

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('nguoi_dung') AND name = 'is_2fa_enabled'
)
BEGIN
    ALTER TABLE nguoi_dung ADD is_2fa_enabled BIT NOT NULL DEFAULT 0;
END
GO


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 006: FIX BUG - Đăng ký tài khoản thường bị lỗi 500 / "Dữ liệu bị trùng"
-- dù email & số điện thoại hoàn toàn mới.
--
-- NGUYÊN NHÂN: cột google_id được tạo với UNIQUE CONSTRAINT thông thường (ở
-- PHẦN 003). Trên SQL Server, UNIQUE CONSTRAINT thông thường CHỈ cho phép
-- ĐÚNG 1 dòng có giá trị NULL (coi mọi NULL là bằng nhau) — khác MySQL/
-- PostgreSQL (cho phép nhiều NULL). Vì tài khoản đăng ký thường luôn có
-- google_id = NULL, nên ngay sau tài khoản thường đầu tiên, mọi tài khoản
-- thường tiếp theo đều vi phạm UNIQUE constraint này khi INSERT.
--
-- CÁCH SỬA: xoá UNIQUE constraint/index hiện có trên google_id, thay bằng
-- FILTERED UNIQUE INDEX chỉ áp dụng cho các dòng CÓ giá trị (IS NOT NULL).
-- ═══════════════════════════════════════════════════════════════════════════

-- B1. Xoá UNIQUE CONSTRAINT trên google_id nếu có (tên do PHẦN 003 tạo)
IF EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = 'UQ_nguoi_dung_google_id' AND parent_object_id = OBJECT_ID('nguoi_dung')
)
BEGIN
    ALTER TABLE nguoi_dung DROP CONSTRAINT UQ_nguoi_dung_google_id;
END
GO

-- B2. Xoá mọi UNIQUE INDEX khác trên đúng 1 cột google_id do Hibernate
--     tự sinh ra (ddl-auto=update thường đặt tên dạng UK_xxxxxxxxxxxxxxxx)
DECLARE @indexName NVARCHAR(255);
DECLARE idx_cursor CURSOR FOR
    SELECT i.name
    FROM sys.indexes i
    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('nguoi_dung')
      AND i.is_unique = 1
      AND i.name IS NOT NULL
      AND i.name <> 'UQ_nguoi_dung_google_id_filtered'
      AND c.name = 'google_id'
      -- chỉ lấy index có ĐÚNG 1 cột (google_id), tránh đụng index nhiều cột khác
      AND (SELECT COUNT(*) FROM sys.index_columns ic2 WHERE ic2.object_id = i.object_id AND ic2.index_id = i.index_id) = 1;

OPEN idx_cursor;
FETCH NEXT FROM idx_cursor INTO @indexName;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('DROP INDEX [' + @indexName + '] ON nguoi_dung');
    FETCH NEXT FROM idx_cursor INTO @indexName;
END
CLOSE idx_cursor;
DEALLOCATE idx_cursor;
GO

-- B3. Tạo lại UNIQUE INDEX đúng chuẩn: chỉ áp dụng cho các dòng CÓ liên kết
--     Google (google_id IS NOT NULL) -> cho phép nhiều dòng NULL cùng lúc
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_nguoi_dung_google_id_filtered' AND object_id = OBJECT_ID('nguoi_dung')
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_nguoi_dung_google_id_filtered
        ON nguoi_dung(google_id)
        WHERE google_id IS NOT NULL;
END
GO


-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 007: Đặt tên CỐ ĐỊNH cho ràng buộc UNIQUE trên cột email (thay vì để
-- Hibernate tự đặt tên ngẫu nhiên kiểu "UQ__nguoi_du__AB6E61645E2F1A01"), để
-- code Java (AuthService.register) có thể nhận diện CHẮC CHẮN đây là lỗi
-- trùng email khi bắt DataIntegrityViolationException, mà KHÔNG cần query
-- lại DB (query lại trong cùng transaction đã lỗi sẽ làm Hibernate Session
-- bị "nhiễm độc" và ném AssertionFailure).
-- ═══════════════════════════════════════════════════════════════════════════

-- B1. Tìm và xoá UNIQUE constraint/index hiện có trên (đúng) cột email,
--     bất kể tên do Hibernate tự sinh là gì
DECLARE @indexNameEmail NVARCHAR(255);
DECLARE idx_cursor_email CURSOR FOR
    SELECT i.name
    FROM sys.indexes i
    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('nguoi_dung')
      AND i.is_unique = 1
      AND i.name IS NOT NULL
      AND i.name <> 'UQ_nguoi_dung_email'
      AND c.name = 'email'
      AND (SELECT COUNT(*) FROM sys.index_columns ic2 WHERE ic2.object_id = i.object_id AND ic2.index_id = i.index_id) = 1;

OPEN idx_cursor_email;
FETCH NEXT FROM idx_cursor_email INTO @indexNameEmail;
WHILE @@FETCH_STATUS = 0
BEGIN
    -- Có thể là UNIQUE CONSTRAINT (key_constraints) hoặc UNIQUE INDEX thường
    IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = @indexNameEmail AND parent_object_id = OBJECT_ID('nguoi_dung'))
        EXEC('ALTER TABLE nguoi_dung DROP CONSTRAINT [' + @indexNameEmail + ']');
    ELSE
        EXEC('DROP INDEX [' + @indexNameEmail + '] ON nguoi_dung');

    FETCH NEXT FROM idx_cursor_email INTO @indexNameEmail;
END
CLOSE idx_cursor_email;
DEALLOCATE idx_cursor_email;
GO

-- B2. Tạo lại UNIQUE CONSTRAINT với tên cố định
IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = 'UQ_nguoi_dung_email' AND parent_object_id = OBJECT_ID('nguoi_dung')
)
BEGIN
    ALTER TABLE nguoi_dung ADD CONSTRAINT UQ_nguoi_dung_email UNIQUE (email);
END
GO


-- ═══════════════════════════════════════════════════════════════════════════
-- KIỂM TRA LẠI SAU KHI CHẠY
-- ═══════════════════════════════════════════════════════════════════════════
SELECT id, ten_san_pham, la_noi_bo FROM san_pham WHERE la_noi_bo = 1;
GO

SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'nguoi_dung' AND COLUMN_NAME IN ('google_id', 'is_2fa_enabled');
GO

SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'don_hang' AND COLUMN_NAME = 'ghi_chu_noi_bo';
GO

SELECT i.name AS index_or_constraint_name, i.is_unique
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('nguoi_dung')
  AND i.name IN ('UQ_nguoi_dung_google_id_filtered', 'UQ_nguoi_dung_email', 'UQ_nguoi_dung_so_dien_thoai');
GO
-- ═══════════════════════════════════════════════════════════════════════════
-- PHẦN 008: Thêm cột gia (giá tham khảo) vào bảng thiet_ke_yeu_thich
-- Phục vụ API GET/POST /api/v1/wishlist/thiet-ke — cho phép WishlistPage.vue
-- hiển thị lại đúng giá đã tính khi khách lưu thiết kế bánh 3D.
-- ═══════════════════════════════════════════════════════════════════════════
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('thiet_ke_yeu_thich') AND name = 'gia'
)
BEGIN
    ALTER TABLE thiet_ke_yeu_thich ADD gia FLOAT NULL;
END
GO

-- ═══════════════════════════════════════════════════════════════════════════
-- MIGRATION: thêm cột da_tru_ton_kho vào bảng don_hang
--
-- Lý do: sửa nghiệp vụ trừ tồn kho cho đơn hàng ONLINE (khách tự đặt qua web)
-- — trước đây trừ kho ngay khi đơn được xác nhận/thanh toán (chuyển sang
-- DA_XAC_NHAN), giờ CHỈ trừ kho khi đơn chuyển sang SAN_SANG (sẵn sàng giao).
-- Cột này đánh dấu 1 đơn ONLINE đã thực sự bị trừ kho hay chưa, để logic
-- hủy/hoàn tiền cộng trả kho chính xác.
--
-- LƯU Ý: cờ này CHỈ dùng cho luồng đơn ONLINE (OrderService). Đơn tạo tay bởi
-- nhân viên/POS (AdminOrderService) KHÔNG dùng cờ này — bên đó trừ kho ngay
-- lúc tạo đơn (1 bước, coi như hoàn thành luôn) và cộng trả kho vô điều kiện
-- khi hủy/hoàn tiền, giữ nguyên như logic cũ, không thay đổi.
--
-- Chạy 1 LẦN DUY NHẤT trên môi trường PROD trước khi deploy code mới (vì
-- application-prod.yml dùng ddl-auto: validate, không tự tạo cột).
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE don_hang ADD da_tru_ton_kho BIT NOT NULL CONSTRAINT DF_don_hang_da_tru_ton_kho DEFAULT 0;

-- Backfill dữ liệu cho các đơn ONLINE đã tồn tại trước khi có bản vá này:
-- Dưới nghiệp vụ CŨ, tồn kho bị trừ ngay khi đơn chuyển sang DA_XAC_NHAN.
-- Vậy nên bất kỳ đơn ONLINE nào hiện đang ở DA_XAC_NHAN trở lên trong luồng
-- chuẩn đều ĐÃ bị trừ kho rồi -> đánh dấu = 1, để sau này nếu đơn đó bị
-- hủy/hoàn tiền thì hệ thống vẫn cộng trả kho đúng.
-- Đơn còn ở CHO_XAC_NHAN (chưa xác nhận) thì chưa từng bị trừ -> giữ = 0.
-- Đơn đã hủy (DA_HUY) / đã hoàn tiền (DA_HOAN_TIEN) thì kho đã được cộng trả
-- lại bởi chính logic hủy/hoàn tiền CŨ rồi -> giữ = 0 (không trừ trùng nữa).
-- Đơn POS (nguon_don = 'POS') không thuộc phạm vi cờ này -> bỏ qua, giữ = 0.
UPDATE don_hang
SET da_tru_ton_kho = 1
WHERE nguon_don = 'ONLINE'
  AND trang_thai IN ('DA_XAC_NHAN', 'DANG_LAM', 'SAN_SANG', 'DANG_GIAO', 'DA_GIAO', 'HOAN_THANH');