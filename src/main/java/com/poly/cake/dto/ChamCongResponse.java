package com.poly.cake.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ChamCongResponse {
    private Long id;
    private Long phanCaId;
    private String tenCa;
    private String ngayLamViec;
    private LocalDateTime gioVao;
    private LocalDateTime gioRa;
    private Integer phutDiTre;
    private String trangThai;
    // T102 – Hệ số lương của ca (1.0 = ngày thường, 2.0 = x2, 3.0 = x3...)
    private BigDecimal heSoLuong;
    private Boolean laNgayLe;

    // ── T062: Dữ liệu kết ca (chỉ có giá trị sau khi gọi kết ca) ────────────
    private LocalDateTime thoiDiemKetCa;
    private String loaiBaoCao;          // X_REPORT | Z_REPORT
    private String loaiBaoCaoLabel;     // "X-Report (Báo cáo giữa ca)" | "Z-Report (Kết ca chính thức)"
    private Integer tongSoDon;
    private BigDecimal doanhThuTienMat;
    private BigDecimal doanhThuSepay;
    private BigDecimal doanhThuKhac;
    private BigDecimal tongDoanhThu;
    private String ghiChuKetCa;
}