package com.poly.cake.dto;

import lombok.Data;

import java.math.BigDecimal;

public class PhuKienTrangTriDto {

    /**
     * T050 - DTO trả về cho khách hàng xem danh sách phụ kiện trang trí còn hàng.
     */
    @Data
    public static class Response {

        private Long id;

        private String tenPhuKien;

        private BigDecimal donGia;

        private Integer soLuongTon;

        private String anhPhuKien;

        // Đường dẫn model 3D thật (.glb) - CakeBuilder3D.vue ưu tiên đọc field này để
        // hiển thị đúng hình phụ kiện lên bánh, thay vì đoán theo tên như trước.
        private String model3dUrl;
    }
}