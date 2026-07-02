package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phu_kien_trang_tri")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuKienTrangTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(150)")
    private String tenPhuKien;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal donGia;

    private Integer soLuongTon = 0;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String anhPhuKien;

    // Đường dẫn/URL file model 3D (.glb) của phụ kiện này. NULL = FE chưa có model thật,
    // sẽ tự rơi về hình mẫu dựng sẵn (procedural) hoặc đoán theo tên (cách cũ) - xem
    // CakeBuilder3D.vue -> buildMarkerMesh().
    @Column(columnDefinition = "NVARCHAR(500)")
    private String model3dUrl;

    private Boolean hoatDong = true;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}