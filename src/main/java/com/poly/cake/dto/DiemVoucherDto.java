package com.poly.cake.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs cho hệ thống Điểm Thưởng & Voucher Cá Nhân */
public class DiemVoucherDto {

    // ─── ĐIỂM THƯỞNG ─────────────────────────────────────────────────────────

    /** Thông tin tổng quan điểm của khách */
    @Data
    public static class DiemTongQuan {
        private Integer tongDiem;
        private List<GiaoDichDiem> lichSu;
    }

    @Data
    public static class GiaoDichDiem {
        private Long id;
        private Integer diemThayDoi;
        private String loaiGiaoDich;
        private String moTa;
        private Long donHangId;
        private LocalDateTime ngayTao;
    }

    // ─── MÃ GIẢM GIÁ (danh sách gói đổi điểm từ DB) ─────────────────────────

    /** Response trả về danh sách mã giảm giá có thể đổi bằng điểm */
    @Data
    public static class MaGiamGiaResponse {
        private Long id;
        private String maCode;
        private String loaiGiamGia;
        private BigDecimal giaTriGiam;
        private BigDecimal donHangToiThieu;
        private Integer diemCanDung;
        private LocalDateTime ngayHetHan;
        /** Khách có đủ điểm để đổi không (tính ở service) */
        private boolean duDiem;
    }

    // ─── VOUCHER KHÁCH HÀNG ───────────────────────────────────────────────────

    @Data
    public static class VoucherResponse {
        private Long id;
        private String tenVoucher;
        private String loaiGiam;
        private BigDecimal giaTriGiam;
        private BigDecimal donHangToiThieu;
        private Integer diemSuDung;
        private String trangThai;
        private LocalDateTime ngayHetHan;
        private LocalDateTime ngayTao;
        private boolean conHieuLuc;
    }

    /** Request đổi điểm lấy voucher */
    @Data
    public static class DoiDiemRequest {
        /** Mã gói voucher: GIAM_50K, GIAM_10_PHAN_TRAM, ... */
        private String maGoiVoucher;
    }

    // ─── POS: hỏi SĐT để cộng điểm offline ──────────────────────────────────

    @Data
    public static class CongDiemPosRequest {
        private String soDienThoai;
        private Long donHangId;
    }

    @Data
    public static class CongDiemPosResponse {
        private boolean timThayKhach;
        private String tenKhach;
        private Integer diemDuocCong;
        private Integer tongDiemMoi;
        private String thongBao;
    }
}