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
  [ho_ten] varchar(150) NOT NULL,
  [email] varchar(150) UNIQUE NOT NULL,
  [mat_khau] varchar(255) NOT NULL,
  [so_dien_thoai] varchar(20),
  [anh_dai_dien] varchar(500),
  [quyen] nvarchar(255) NOT NULL CHECK ([quyen] IN ('ADMIN', 'NHAN_VIEN', 'KHACH_HANG')) DEFAULT 'KHACH_HANG',
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('HOAT_DONG', 'BI_KHOA', 'NGUNG_HOAT_DONG')) DEFAULT 'HOAT_DONG',
  [ma_otp] varchar(10),
  [otp_het_han] datetime,
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [lam_moi_token] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint NOT NULL,
  [token] varchar(512) UNIQUE NOT NULL,
  [ngay_het_han] datetime NOT NULL
)
GO

CREATE TABLE [thong_bao] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint NOT NULL,
  [tieu_de] varchar(200) NOT NULL,
  [noi_dung] text NOT NULL,
  [loai_thong_bao] nvarchar(255) NOT NULL CHECK ([loai_thong_bao] IN ('DON_HANG', 'TON_KHO', 'HE_THONG')),
  [da_doc] BIT DEFAULT (0),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [danh_muc] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_danh_muc] varchar(150) NOT NULL,
  [mo_ta] varchar(255),
  [anh_dai_dien] varchar(500),
  [hoat_dong] BIT DEFAULT (1)
)
GO

CREATE TABLE [phu_kien_trang_tri] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_phu_kien] varchar(150) NOT NULL,
  [don_gia] decimal(12,2) NOT NULL,
  [so_luong_ton] int DEFAULT (0),
  [anh_phu_kien] varchar(500),
  [hoat_dong] BIT DEFAULT (1),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [san_pham] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [danh_muc_id] bigint,
  [ten_san_pham] varchar(200) NOT NULL,
  [don_gia] decimal(12,2) NOT NULL,
  [so_luong_ton] int DEFAULT (0),
  [anh_san_pham] varchar(500),
  [trang_thai] nvarchar(255) NOT NULL CHECK ([trang_thai] IN ('DANG_BAN', 'TAM_AN')) DEFAULT 'DANG_BAN',
  [mo_ta] text,
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
  [thiet_ke_banh_json] text,
  [ngay_tao] datetime NOT NULL,
  [ngay_cap_nhat] datetime NOT NULL
)
GO

CREATE TABLE [ma_giam_gia] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ma_code] varchar(50) UNIQUE NOT NULL,
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
  [thiet_ke_banh_json] text,
  [dia_chi_giao] varchar(255),
  [ngay_giao_du_kien] datetime,
  [ghi_chu] text,
  [ngay_tao] datetime DEFAULT (GETDATE()),
  [ngay_cap_nhat] datetime,
  [ly_do_huy] text,
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
  [ma_giao_dich] varchar(255),
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
  [phan_hoi_cua_tiem] text,
  [noi_dung] text,
  [bi_an] BIT DEFAULT (0),
  [ngay_tao] datetime DEFAULT (GETDATE())
)
GO

CREATE TABLE [ca_lam_viec] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [ten_ca] varchar(100) NOT NULL,
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
  [ghi_chu] text,
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
  [khoa_cau_hinh] varchar(100) UNIQUE,
  [gia_tri] varchar(500) NOT NULL,
  [mo_ta] text
)
GO

CREATE TABLE [nhat_ky_he_thong] (
  [id] bigint PRIMARY KEY IDENTITY(1, 1),
  [nguoi_dung_id] bigint,
  [hanh_dong] varchar(100) NOT NULL,
  [ten_bang] varchar(100),
  [ban_ghi_id] bigint,
  [gia_tri_cu] text,
  [gia_tri_moi] text
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