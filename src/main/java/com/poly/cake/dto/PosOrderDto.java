package com.poly.cake.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class PosOrderDto {

    @Data
    public static class Request {
        private String emailKhachHang; // Nếu là khách vãng lai thì truyền null hoặc "khachvanglai@gmail.com"
        private String ghiChu;
        private List<ItemRequest> items;
    }

    @Data
    public static class ItemRequest {
        private Long sanPhamId;
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