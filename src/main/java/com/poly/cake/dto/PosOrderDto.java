package com.poly.cake.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class PosOrderDto {

    @Data
    public static class Request {
        @Email(message = "Email không đúng định dạng")
        private String emailKhachHang; // Nếu là khách vãng lai thì truyền null hoặc "khachvanglai@gmail.com"

        private String ghiChu;

        @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
        @Valid
        private List<ItemRequest> items;

        private String phuongThucThanhToan; // "TIEN_MAT" (mặc định) hoặc "VIET_QR"
    }

    @Data
    public static class ItemRequest {
        @NotNull(message = "Sản phẩm không được để trống")
        private Long sanPhamId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer soLuong;
    }

    @Data
    public static class Response {
        private Long donHangId;
        private BigDecimal tongTien;
        private String trangThai;
        private String nguonDon;
        private String vietQrUrl;     // Đường dẫn ảnh QR để FE hiển thị quét mã
        private String receiptText;    // Chuỗi văn bản thô định dạng sẵn để FE bấm in ra máy in nhiệt
    }
}