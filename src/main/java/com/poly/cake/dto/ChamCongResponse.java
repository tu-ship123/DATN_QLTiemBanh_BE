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