package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "phan_ca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhanCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    private NguoiDung nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ca_lam_viec_id", nullable = false)
    private CaLamViec caLamViec;

    @Column(nullable = false)
    private LocalDate ngayLamViec;

    /**
     * Trạng thái phân ca:
     *   DA_LAP    – Đã lên lịch, chưa check-in.
     *   XAC_NHAN  – Nhân viên đã check-in.
     *   DA_KET_CA – Nhân viên đã kết ca chính thức (Z-Report). Không đổi tiếp.
     *   DA_HUY    – Ca bị hủy.
     */
    @Column(nullable = false)
    private String trangThai = "DA_LAP"; // DA_LAP | XAC_NHAN | DA_KET_CA | DA_HUY

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}