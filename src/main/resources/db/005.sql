use QL_TiemBanh
go
-- ═══════════════════════════════════════════════════════════════════════════
ALTER TABLE nguoi_dung ADD is_2fa_enabled bit NOT NULL DEFAULT 0;
 IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('nguoi_dung') AND name = 'is_2fa_enabled'
)
BEGIN
    ALTER TABLE nguoi_dung ADD is_2fa_enabled bit NOT NULL DEFAULT 0;
END
GO