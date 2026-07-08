-- ═══════════════════════════════════════════════════════════════════════════
-- T080: API Ghi chú nội bộ + Barcode giao hàng + Refund
--
-- Script này chỉ thêm 1 cột mới cho bảng don_hang:
--   ghi_chu_noi_bo (NVARCHAR(MAX)) - Ghi chú nội bộ dành cho Nhân viên/Bếp,
--   hoàn toàn tách biệt với cột ghi_chu hiện có (vốn khách hàng có thể xem lại
--   khi tra cứu đơn của mình). Cột mới này KHÔNG bao giờ được trả về cho
--   khách hàng ở bất kỳ API nào (xem OrderDto.Response#ghiChuNoiBo và
--   OrderService/AdminOrderService).
--
-- Phần "Barcode giao hàng" (quét mã HD-{id} trên bill để tự động chuyển đơn
-- sang DA_GIAO) và phần "Refund" (hoàn tiền có hoàn kho + cập nhật bản ghi
-- thanh_toan) tái sử dụng các cột đã có sẵn (thoi_diem_giao, trang_thai,
-- thanh_toan.trang_thai) nên KHÔNG cần thêm migration riêng.
--
-- CÁCH CHẠY: mở file này trong SSMS, chọn đúng database rồi Execute.
-- Chạy lại nhiều lần vẫn an toàn (không tạo thêm lỗi).
-- ═══════════════════════════════════════════════════════════════════════════

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('don_hang') AND name = 'ghi_chu_noi_bo'
)
BEGIN
    ALTER TABLE don_hang ADD ghi_chu_noi_bo NVARCHAR(MAX) NULL;
END
GO
