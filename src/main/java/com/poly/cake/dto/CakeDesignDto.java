package com.poly.cake.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * T055 – DTO nhận & validate dữ liệu thiết kế bánh 3D từ FE.
 *
 * Cấu trúc JSON mà FE gửi lên:
 * {
 *   "khung": { "kich_thuoc": { "chieu_cao_cm": 12, "duong_kinh_cm": 20 }, ... },
 *   "tang": [...],
 *   "trang_tri": [...]
 * }
 *
 * Quy tắc bắt buộc:
 *  - khung       : bắt buộc (bước "chọn khung" FE bắt người dùng chọn)
 *  - kich_thuoc  : bắt buộc trong khung (chiều cao + đường kính do khách tự nhập)
 *  - chieuCaoCm  : 5 – 80 cm
 *  - duongKinhCm : 5 – 80 cm
 */
public class CakeDesignDto {

    // ── Root request ──────────────────────────────────────────────────────────
    @Data
    public static class Request {
        @NotNull(message = "Thiết kế phải có thông tin khung bánh!")
        @Valid
        private KhungBanh khung;

        // Các lớp tầng bánh (tuỳ chọn – có thể null hoặc rỗng)
        @Valid
        private List<TangBanh> tang;

        // Phụ kiện trang trí (tuỳ chọn)
        private List<Object> trangTri;
    }

    // ── Khung bánh ───────────────────────────────────────────────────────────
    @Data
    public static class KhungBanh {

        /**
         * KÍCH THƯỚC BẮT BUỘC – khách phải tự nhập tại bước chọn khung.
         * Backend sẽ tự động ghi chú vào ghi_chu của đơn hàng để thợ làm bánh biết.
         */
        @NotNull(message = "Bạn chưa nhập kích thước bánh! Vui lòng điền chiều cao và đường kính.")
        @Valid
        private KichThuoc kich_thuoc;

        // Hình dạng: TRON / VUONG / TIM ... (không bắt buộc, FE có thể null)
        private String hinh_dang;

        // Màu kem nền (hex / tên màu)
        private String mau_nen;
    }

    // ── Kích thước – ĐÂY LÀ PHẦN KHÁCH BẮT BUỘC PHẢI TỰ NHẬP ───────────────
    @Data
    public static class KichThuoc {
        @NotNull(message = "Vui lòng nhập chiều cao bánh (cm)!")
        @DecimalMin(value = "5.0",  message = "Chiều cao tối thiểu là 5 cm!")
        @DecimalMax(value = "80.0", message = "Chiều cao tối đa là 80 cm!")
        private Double chieu_cao_cm;

        @NotNull(message = "Vui lòng nhập đường kính bánh (cm)!")
        @DecimalMin(value = "5.0",  message = "Đường kính tối thiểu là 5 cm!")
        @DecimalMax(value = "80.0", message = "Đường kính tối đa là 80 cm!")
        private Double duong_kinh_cm;
    }

    // ── Tầng bánh ────────────────────────────────────────────────────────────
    @Data
    public static class TangBanh {
        @NotBlank(message = "Tên tầng bánh không được để trống!")
        private String ten_tang;

        // Hương vị kem (không bắt buộc)
        private String huong_vi;

        // Màu tầng
        private String mau;
    }
}