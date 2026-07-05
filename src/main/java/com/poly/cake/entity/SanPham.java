package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id")
    private DanhMuc danhMuc;

    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String tenSanPham;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal donGia;

    private Integer soLuongTon = 0;

    // Ngưỡng cảnh báo tồn kho thấp - khi soLuongTon <= nguongCanhBao thì hệ thống
    // sẽ gửi thông báo TON_KHO cho Admin/NhanVien (xem InventoryService).
    // Ngưỡng cố định mặc định = 10 cho mọi sản phẩm theo yêu cầu nghiệp vụ.
    @Column(name = "nguong_canh_bao")
    @Builder.Default
    private Integer nguongCanhBao = 10;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String anhSanPham;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai = "DANG_BAN"; // DANG_BAN, TAM_AN

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    // Đánh dấu sản phẩm "nội bộ" (VD: sản phẩm đại diện dùng chung cho bánh 3D tùy
    // chỉnh) - KHÔNG bao giờ được hiện ra ở bất kỳ danh sách sản phẩm nào (trang khách,
    // trang admin...), dù trạng thái là DANG_BAN. Dùng cột riêng thay vì so khớp tên
    // sản phẩm, vì so tên rất dễ bị lệch (thừa khoảng trắng, tạo trùng bản ghi, sai
    // encoding dấu tiếng Việt...) khiến sản phẩm nội bộ lọt ra ngoài công khai.
    @Column(name = "la_noi_bo", nullable = false)
    @Builder.Default
    private Boolean laNoiBo = false;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}