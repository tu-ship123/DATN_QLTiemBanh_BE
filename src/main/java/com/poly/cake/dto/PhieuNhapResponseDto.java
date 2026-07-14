package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về cho danh sách/chi tiết phiếu nhập kho (Admin) — khác PhieuNhapDto (dùng để TẠO phiếu).
 * Bổ sung tên sản phẩm (FE cần hiển thị, entity ChiTietPhieuNhap chỉ lưu sanPhamId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhieuNhapResponseDto {
    private Long id;
    private Long nguoiTaoId;
    private Long nguoiDuyetId;
    private String trangThai;
    private Double tongTien;
    private String ghiChu;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayDuyet;
    private List<ChiTiet> chiTietList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChiTiet {
        private Long sanPhamId;
        private String tenSanPham;
        private Integer soLuong;
        private Double giaNhap;
    }
}
