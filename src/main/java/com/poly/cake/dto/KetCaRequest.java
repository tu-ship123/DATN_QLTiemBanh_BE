package com.poly.cake.dto;

import lombok.Data;

/**
 * T062 – Request body khi nhân viên gọi kết ca.
 */
@Data
public class KetCaRequest {

    /**
     * ID phân ca cần kết.
     * Nhân viên phải đã check-in ca này (PhanCa.trangThai = XAC_NHAN).
     */
    private Long phanCaId;

    /**
     * Loại báo cáo:
     *   "X_REPORT" – Xem doanh thu tạm giữa ca, ca vẫn tiếp tục.
     *   "Z_REPORT" – Kết ca chính thức, ghi doanh thu vào ChamCong,
     *                PhanCa → DA_KET_CA. Chỉ được thực hiện 1 lần.
     */
    private String loaiBaoCao;

    /** Ghi chú của nhân viên khi kết ca (không bắt buộc). */
    private String ghiChu;
}