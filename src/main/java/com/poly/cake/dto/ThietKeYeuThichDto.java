package com.poly.cake.dto;

import lombok.Data;

@Data
public class ThietKeYeuThichDto {
    private String tenThietKe;
    private String thietKeBanhJson;

    // Giá tham khảo do FE tính sẵn tại thời điểm lưu (CHỈ để hiển thị lại ở
    // WishlistPage.vue) — khi khách bấm "Đặt bánh này", BE vẫn luôn tự tính
    // lại giá thật từ thietKeBanhJson, không dùng số này để tạo đơn hàng.
    private Double gia;
}