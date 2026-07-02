-- Tạo Database mới
CREATE DATABASE [QL_TiemBanh];
GO
USE [QL_TiemBanh];
GO

-- =======================================================
-- TẠO CÁC BẢNG (TABLES)
-- =======================================================

CREATE TABLE [nguoi_dung] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ho_ten] nvarchar(150) NOT NULL,
  [email] nvarchar(150) UNIQUE NOT NULL,
  [mat_khau] nvarchar(255) NOT NULL,
  [so_dien_thoai] nvarchar(20),
  [anh_dai_dien] nvarchar(500),
  [quyen] nvarchar(255) NOT NULL CHECK ([quyen] IN ('ADMIN', 'NHAN_VIEN', 'KHACH_HANG')) DEFAULT 'KHACH_HANG',
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('HOAT_DONG', 'BI_KHOA', 'NGUNG_HOAT_DONG')) DEFAULT 'HOAT_DONG',
  [ma_otp] nvarchar(10),
  [otp_het_han] datetime,
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [lam_moi_token] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint NOT NULL,
  [token] nvarchar(512) UNIQUE NOT NULL,
  [ngay_het_han] datetime NOT NULL
)
GO

CREATE TABLE [thong_bao] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint NOT NULL,
  [tieu_de] nvarchar(200) NOT NULL,
  [noi_dung] nvarchar(max) NOT NULL,
  [loai_thong_bao] nvarchar(255) NOT NULL CHECK ([loai_thong_bao] IN ('DON_HANG', 'TON_KHO', 'HE_THONG')),
  [da_doc] BIT DEFAULT (0),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [danh_muc] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_danh_muc] nvarchar(150) NOT NULL,
  [mo_ta] nvarchar(255),
  [anh_dai_dien] nvarchar(500),
  [hoat_dong] BIT DEFAULT (1)
)
GO

CREATE TABLE [phu_kien_trang_tri] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_phu_kien] nvarchar(150) NOT NULL,
  [don_gia] decimal(12,2) NOT NULL,
  [so_luong_ton] int DEFAULT (0),
  [anh_phu_kien] nvarchar(500),
  [model_3d_url] nvarchar(500), -- đường dẫn file .glb (VD: /models/oreo.glb hoặc URL CDN ngoài) dùng để hiển thị đúng hình phụ kiện trên bánh 3D, NULL = dùng hình mẫu dựng sẵn (fallback)
  [hoat_dong] BIT DEFAULT (1),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [san_pham] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [danh_muc_id] bigint,
  [ten_san_pham] nvarchar(200) NOT NULL,
  [don_gia] decimal(12,2) NOT NULL,
  [so_luong_ton] int DEFAULT (0),
  [anh_san_pham] nvarchar(500),
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('DANG_BAN', 'TAM_AN')) DEFAULT 'DANG_BAN',
  [mo_ta] nvarchar(max),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [gio_hang] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [khach_hang_id] bigint UNIQUE NOT NULL,
  [ngay_tao] datetime NOT NULL,
  [ngay_cap_nhat] datetime NOT NULL
)
GO

CREATE TABLE [chi_tiet_gio_hang] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [gio_hang_id] bigint NOT NULL,
  [san_pham_id] bigint NOT NULL,
  [so_luong] int DEFAULT (0),
  [thiet_ke_banh_json] nvarchar(max),
  [ngay_tao] datetime NOT NULL,
  [ngay_cap_nhat] datetime NOT NULL
)
GO

CREATE TABLE [ma_giam_gia] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ma_code] nvarchar(50) UNIQUE NOT NULL,
  [loai_giam_gia] nvarchar(255) NOT NULL CHECK ([loai_giam_gia] IN ('PHAN_TRAM', 'SO_TIEN_CO_DINH')),
  [gia_tri_giam] decimal(12,2) NOT NULL,
  [don_hang_toi_thieu] decimal(12,2),
  [so_luot_toi_da] int,
  [so_luot_da_dung] int DEFAULT (0),
  [ngay_het_han] datetime NOT NULL,
  [hoat_dong] BIT DEFAULT (1)
)
GO

CREATE TABLE [don_hang] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [khach_hang_id] bigint NOT NULL,
  [nhan_vien_id] bigint,
  [ma_giam_gia_id] bigint,
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DANG_LAM', 'SAN_SANG', 'DANG_GIAO', 'HOAN_THANH', 'DA_HUY')) DEFAULT 'CHO_XAC_NHAN',
  [tong_tien] decimal(12,2) NOT NULL,
  [so_tien_coc] decimal(12,2) DEFAULT (0),
  [thiet_ke_banh_json] nvarchar(max),
  [dia_chi_giao] nvarchar(255),
  [ngay_giao_du_kien] datetime,
  [ghi_chu] nvarchar(max),
  [ngay_tao] datetime DEFAULT (GETDATE()),
  [ngay_cap_nhat] datetime,
  [ly_do_huy] nvarchar(max),
  [thoi_diem_giao] datetime,
  [nguon_don] nvarchar(255) NOT NULL CHECK ([nguon_don] IN ('ONLINE', 'POS')) DEFAULT 'ONLINE'
)
GO

CREATE TABLE [chi_tiet_don_hang] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [don_hang_id] bigint NOT NULL,
  [san_pham_id] bigint NOT NULL,
  [so_luong] int DEFAULT (1),
  [don_gia_tai_thoi_diem] decimal(12,2),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [thanh_toan] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [don_hang_id] bigint UNIQUE NOT NULL,
  [hinh_thuc] nvarchar(255) NOT NULL CHECK ([hinh_thuc] IN ('VNPAY', 'MOMO', 'TIEN_MAT', 'CHUYEN_KHOAN')),
  [so_tien] decimal(12,2) NOT NULL,
  [ma_giao_dich] nvarchar(255),
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('CHO_THANH_TOAN', 'THANH_CONG', 'THAT_BAI', 'DA_HOAN_TIEN')) DEFAULT 'CHO_THANH_TOAN',
  [thoi_diem_thanh_toan] datetime,
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [danh_gia] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [khach_hang_id] bigint NOT NULL,
  [san_pham_id] bigint NOT NULL,
  [don_hang_id] bigint NOT NULL,
  [so_sao] int NOT NULL,
  [phan_hoi_cua_tiem] nvarchar(max),
  [noi_dung] nvarchar(max),
  [bi_an] BIT DEFAULT (0),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [ca_lam_viec] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_ca] nvarchar(100) NOT NULL,
  [gio_bat_dau] time NOT NULL,
  [gio_ket_thuc] time NOT NULL,
  [hoat_dong] BIT DEFAULT (1)
)
GO

CREATE TABLE [phan_ca] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nhan_vien_id] bigint NOT NULL,
  [ca_lam_viec_id] bigint NOT NULL,
  [ngay_lam_viec] date NOT NULL,
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('DA_LAP', 'XAC_NHAN', 'DA_HUY')) DEFAULT 'DA_LAP',
  [ghi_chu] nvarchar(max),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [cham_cong] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [phan_ca_id] bigint UNIQUE NOT NULL,
  [gio_vao] datetime,
  [gio_ra] datetime,
  [phut_di_tre] int DEFAULT (0),
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('DUNG_GIO', 'DI_TRE', 'VANG_MAT', 'VE_SOM')) DEFAULT 'DUNG_GIO',
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [cau_hinh_he_thong] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [khoa_cau_hinh] nvarchar(100) UNIQUE,
  [gia_tri] nvarchar(500) NOT NULL,
  [mo_ta] nvarchar(max)
)
GO

CREATE TABLE [nhat_ky_he_thong] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint,
  [hanh_dong] nvarchar(100) NOT NULL,
  [ten_bang] nvarchar(100),
  [ban_ghi_id] bigint,
  [gia_tri_cu] nvarchar(max),
  [gia_tri_moi] nvarchar(max)
)
GO

-- =======================================================
-- TẠO KHÓA NGOẠI (FOREIGN KEYS)
-- =======================================================

ALTER TABLE [lam_moi_token] ADD FOREIGN KEY ([nguoi_dung_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [thong_bao] ADD FOREIGN KEY ([nguoi_dung_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [san_pham] ADD FOREIGN KEY ([danh_muc_id]) REFERENCES [danh_muc] ([id])
GO

ALTER TABLE [gio_hang] ADD FOREIGN KEY ([khach_hang_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [chi_tiet_gio_hang] ADD FOREIGN KEY ([gio_hang_id]) REFERENCES [gio_hang] ([id])
GO

ALTER TABLE [chi_tiet_gio_hang] ADD FOREIGN KEY ([san_pham_id]) REFERENCES [san_pham] ([id])
GO

ALTER TABLE [don_hang] ADD FOREIGN KEY ([khach_hang_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [don_hang] ADD FOREIGN KEY ([nhan_vien_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [don_hang] ADD FOREIGN KEY ([ma_giam_gia_id]) REFERENCES [ma_giam_gia] ([id])
GO

ALTER TABLE [chi_tiet_don_hang] ADD FOREIGN KEY ([don_hang_id]) REFERENCES [don_hang] ([id])
GO

ALTER TABLE [chi_tiet_don_hang] ADD FOREIGN KEY ([san_pham_id]) REFERENCES [san_pham] ([id])
GO

ALTER TABLE [thanh_toan] ADD FOREIGN KEY ([don_hang_id]) REFERENCES [don_hang] ([id])
GO

ALTER TABLE [danh_gia] ADD FOREIGN KEY ([khach_hang_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [danh_gia] ADD FOREIGN KEY ([san_pham_id]) REFERENCES [san_pham] ([id])
GO

ALTER TABLE [danh_gia] ADD FOREIGN KEY ([don_hang_id]) REFERENCES [don_hang] ([id])
GO

ALTER TABLE [phan_ca] ADD FOREIGN KEY ([nhan_vien_id]) REFERENCES [nguoi_dung] ([id])
GO

ALTER TABLE [phan_ca] ADD FOREIGN KEY ([ca_lam_viec_id]) REFERENCES [ca_lam_viec] ([id])
GO

ALTER TABLE [cham_cong] ADD FOREIGN KEY ([phan_ca_id]) REFERENCES [phan_ca] ([id])
GO

ALTER TABLE [nhat_ky_he_thong] ADD FOREIGN KEY ([nguoi_dung_id]) REFERENCES [nguoi_dung] ([id])
GO
-- ═══════════════════════════════════════════════════════════════════════════
-- MIGRATION: Bổ sung cột phục vụ tính năng "Thiết kế bánh 3D" (CakeBuilder3D)
-- Bánh khách tự thiết kế (size + số tầng + phụ kiện) có giá khác với giá gốc
-- của sản phẩm đại diện, và JSON thiết kế cần được giữ lại từ giỏ hàng sang
-- đơn hàng để nhân viên bếp xem lại đúng thiết kế khách đã chọn.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE [chi_tiet_gio_hang] ADD [don_gia_tuy_chinh] decimal(12,2) NULL
GO

ALTER TABLE [chi_tiet_don_hang] ADD [thiet_ke_banh_json] nvarchar(max) NULL
GO

-- ═══════════════════════════════════════════════════════════════════════════
-- MIGRATION: Liên kết voucher cá nhân (đổi bằng điểm) ngược về mã giảm giá gốc
-- Trước đây khi khách dùng voucher cá nhân (voucher_khach_hang) lúc checkout,
-- hệ thống không cộng dồn lượt sử dụng về mã giảm giá gốc (ma_giam_gia) đã sinh
-- ra voucher đó -> trang quản lý voucher (đọc bảng ma_giam_gia) không thống kê
-- được các lượt dùng này. Thêm cột liên kết ngược để BE cộng dồn đúng.
-- Lưu ý: bảng [voucher_khach_hang] hiện do Hibernate (ddl-auto) tự tạo ở dev,
-- nếu bảng đã tồn tại ở prod thì chạy đoạn ALTER TABLE bên dưới; nếu bảng chưa
-- tồn tại, Hibernate/JPA sẽ tự tạo đầy đủ cột khi entity được cập nhật.
-- ═══════════════════════════════════════════════════════════════════════════

IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'voucher_khach_hang')
   AND NOT EXISTS (
       SELECT 1 FROM sys.columns
       WHERE object_id = OBJECT_ID('voucher_khach_hang') AND name = 'ma_giam_gia_goc_id'
   )
BEGIN
    ALTER TABLE [voucher_khach_hang] ADD [ma_giam_gia_goc_id] bigint NULL
    ALTER TABLE [voucher_khach_hang] ADD FOREIGN KEY ([ma_giam_gia_goc_id]) REFERENCES [ma_giam_gia] ([id])
END
GO
