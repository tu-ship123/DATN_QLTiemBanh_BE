package com.poly.cake.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SanPhamDto {

    @Data
    public static class Request {

        @NotNull(message = "Danh mục không được để trống")
        private Long danhMucId;

        @NotBlank(message = "Tên sản phẩm không được để trống")
        private String tenSanPham;

        @NotNull(message = "Đơn giá không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá phải lớn hơn 0")
        private BigDecimal donGia;

        @NotNull(message = "Số lượng tồn không được để trống")
        @Min(value = 0, message = "Số lượng tồn không được âm")
        private Integer soLuongTon;

        private String anhSanPham;

        private String trangThai;

        private String moTa;

        // Ngưỡng cảnh báo tồn kho thấp - không bắt buộc, nếu không truyền thì
        // service sẽ tự gán mặc định = 10.
        @Min(value = 0, message = "Ngưỡng cảnh báo không được âm")
        private Integer nguongCanhBao;
    }

    // Dùng cho API cập nhật tồn kho (nhập thêm hàng / điều chỉnh thủ công)
    // PATCH /api/v1/admin/products/{id}/inventory
    @Data
    public static class InventoryUpdate {

        // Số lượng thay đổi: dương = nhập thêm hàng, âm = xuất/điều chỉnh giảm.
        // Không dùng số 0 vì không có tác dụng.
        @NotNull(message = "Số lượng thay đổi không được để trống")
        private Integer soLuongThayDoi;
    }

    @Data
    public static class Response {

        private Long id;

        private Long danhMucId;

        private String tenDanhMuc;

        private String tenSanPham;

        private BigDecimal donGia;

        private Integer soLuongTon;

        private String anhSanPham;

        private String trangThai;

        private String moTa;

        private LocalDateTime ngayTao;

        private Integer nguongCanhBao;
    }
}