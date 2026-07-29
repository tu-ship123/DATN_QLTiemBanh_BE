package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "don_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NguoiDung nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_giam_gia_id")
    private MaGiamGia maGiamGia;

    // Voucher cá nhân (đổi bằng điểm) đã dùng cho đơn hàng này (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_khach_hang_id")
    private VoucherKhachHang voucherKhachHang;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiDonHang trangThai = TrangThaiDonHang.CHO_XAC_NHAN;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tongTien;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal soTienCoc = BigDecimal.ZERO;

    /**
     * T102 – Số tiền phụ thu tự động cộng thêm nếu ngày giao hàng (hoặc ngày
     * tạo đơn với đơn POS) rơi vào dịp đặc biệt đã cấu hình (xem PhuThuDonHang).
     * = 0 nếu không rơi vào dịp nào.
     */
    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal soTienPhuThu = BigDecimal.ZERO;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String thietKeBanhJson;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String diaChiGiao;

    private LocalDateTime ngayGiaoDuKien;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // T080 – Ghi chú nội bộ: chỉ Nhân viên/Bếp xem được qua trang quản trị,
    // KHÔNG bao giờ trả về cho khách hàng (khác với "ghiChu" ở trên vốn là
    // ghi chú công khai khách có thể xem lại đơn của mình).
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String ghiChuNoiBo;

    private LocalDateTime ngayTao;

    private LocalDateTime ngayCapNhat;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String lyDoHuy;

    // DF_ST06 – "Yêu cầu sửa đơn": khách hàng đề nghị thay đổi thông tin đơn
    // (địa chỉ/SĐT/ngày giao/ghi chú) khi đơn còn ở trạng thái Chờ xác nhận.
    // Lưu lại snapshot JSON của nội dung khách muốn sửa để nhân viên xem & duyệt.
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String yeuCauSuaDonJson;

    private LocalDateTime ngayYeuCauSuaDon;

    // CHO_XU_LY (khách vừa gửi, chưa ai xử lý) / DA_XU_LY (nhân viên đã xử lý xong) / null (chưa có yêu cầu nào)
    @Column(columnDefinition = "NVARCHAR(50)")
    private String trangThaiYeuCauSuaDon;

    // T080 – Thời điểm giao hàng THỰC TẾ, được ghi nhận tự động khi nhân viên
    // giao hàng quét mã vạch/mã đơn trên bill lúc giao (xem AdminOrderService#scanDelivery)
    private LocalDateTime thoiDiemGiao;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String nguonDon = "ONLINE"; // ONLINE, POS

    // Cờ đánh dấu đơn ĐÃ được trừ tồn kho sản phẩm hay chưa.
    // Nghiệp vụ: đơn online CHỈ trừ kho khi chuyển sang trạng thái SAN_SANG
    // (sẵn sàng giao) — không trừ ngay lúc đặt/xác nhận/thanh toán, vì thời điểm
    // đó cửa hàng còn có thể hủy/đổi số lượng trước khi thực sự chuẩn bị hàng.
    // Cờ này dùng để: (1) tránh trừ kho 2 lần nếu đơn được chuyển sang SAN_SANG
    // qua nhiều đường (process/status), (2) khi hủy/hoàn tiền biết chính xác có
    // cần cộng trả tồn kho hay không (chỉ cộng trả nếu trước đó ĐÃ thực sự trừ).
    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit not null default 0")
    private Boolean daTruTonKho = false;

    // Mapping 1-Nhiều với bảng chi tiết đơn hàng
    @Builder.Default
    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonHang> chiTietDonHangs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }

}