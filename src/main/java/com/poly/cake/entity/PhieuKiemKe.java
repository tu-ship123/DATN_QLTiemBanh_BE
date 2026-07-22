package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_kiem_ke")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuKiemKe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(nullable = false)
    private Integer tonHeThong; // Tồn kho thực tế TRƯỚC KHI kiểm kê

    @Column(nullable = false)
    private Integer tonThucTe; // Số lượng sau khi kiểm kê thực tế

    @Column(nullable = false)
    private Integer chenhLech; // (tonThucTe - tonHeThong) -> Dương là thừa, Âm là thiếu

    @Column(columnDefinition = "NVARCHAR(500)")
    private String lyDo; // Ví dụ: Hỏng, hết hạn, thất thoát, kiểm kê định kỳ...

    private String nguoiThucHien; // Tên/Email admin hoặc nhân viên thực hiện kiểm kê

    private LocalDateTime ngayKiemKe;

    @PrePersist
    protected void onCreate() {
        ngayKiemKe = LocalDateTime.now();
    }
}