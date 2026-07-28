package com.poly.cake.repository;

import com.poly.cake.dto.DoanhThuKenhDto;
import com.poly.cake.dto.HieuSuatNhanVienDto;
import com.poly.cake.dto.TopSanPhamDto;
import com.poly.cake.dto.VoucherUsageDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Long> {

    // Tìm danh sách đơn hàng của một khách hàng, sắp xếp mới nhất lên đầu
    List<DonHang> findByKhachHangOrderByNgayTaoDesc(NguoiDung khachHang);

    // Tìm đích danh 1 đơn hàng theo ID và Khách hàng (Dùng để kiểm tra quyền hủy đơn)
    Optional<DonHang> findByIdAndKhachHang(Long id, NguoiDung khachHang);

    // [T105 - Fix N+1] Lấy đơn hàng của 1 khách kèm chi tiết sản phẩm trong 1 câu SQL
    // Dùng JOIN FETCH để tránh lazy-loading từng chiTietDonHang riêng lẻ
    // [T105 - Fix N+1] Lấy tất cả đơn hàng của khách kèm chi tiết
    // Dùng DISTINCT ở tầng Java (PASS_DISTINCT_THROUGH=false) thay vì SQL
    // để tránh lỗi "text data type cannot be selected as DISTINCT" trên SQL Server
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT DISTINCT d FROM DonHang d " +
            "LEFT JOIN FETCH d.chiTietDonHangs ct " +
            "LEFT JOIN FETCH ct.sanPham " +
            "WHERE d.khachHang = :khachHang " +
            "ORDER BY d.ngayTao DESC")
    List<DonHang> findByKhachHangWithDetails(@Param("khachHang") NguoiDung khachHang);

    // [T105 - Fix N+1] Lấy 1 đơn hàng theo ID kèm toàn bộ liên kết (khách, nhân viên, chi tiết, sản phẩm)
    // Dùng JOIN FETCH để tránh hàng loạt lazy-load khi map sang DTO
    @Query("SELECT d FROM DonHang d " +
            "LEFT JOIN FETCH d.chiTietDonHangs ct " +
            "LEFT JOIN FETCH ct.sanPham " +
            "LEFT JOIN FETCH d.khachHang " +
            "LEFT JOIN FETCH d.nhanVien " +
            "WHERE d.id = :id")
    Optional<DonHang> findByIdWithDetails(@Param("id") Long id);

    // Lọc đơn hàng nâng cao cho Admin
    @Query("SELECT d FROM DonHang d WHERE " +
            "(:trangThai IS NULL OR d.trangThai = :trangThai) AND " +
            "(:nguonDon IS NULL OR d.nguonDon = :nguonDon) AND " +
            "(:tuNgay IS NULL OR d.ngayTao >= :tuNgay) AND " +
            "(:denNgay IS NULL OR d.ngayTao <= :denNgay) " +
            "ORDER BY d.ngayTao DESC")
    List<DonHang> filterAdminOrders(@org.springframework.data.repository.query.Param("trangThai") String trangThai,
                                    @org.springframework.data.repository.query.Param("nguonDon") String nguonDon,
                                    @org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
                                    @org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay);
    // Thêm vào DonHangRepository
    @Query("SELECT COALESCE(SUM(d.tongTien), 0) FROM DonHang d WHERE d.trangThai <> 'DA_HUY' AND d.ngayTao >= :startDate AND d.ngayTao < :endDate")
    BigDecimal sumDoanhThuByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(d) FROM DonHang d WHERE d.trangThai <> 'DA_HUY' AND d.ngayTao >= :startDate AND d.ngayTao < :endDate")
    Long countDonHangByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Đếm số đơn đang trong quá trình sản xuất (dùng cho thẻ KPI "Đang sản xuất" ở Dashboard)
    // Khớp đúng danh sách trạng thái mà FE (Dashboard.vue) đang coi là "đang sản xuất"
    @Query("SELECT COUNT(d) FROM DonHang d WHERE d.trangThai IN ('DA_XAC_NHAN','DANG_CHUAN_BI','DANG_LAM','SAN_SANG')")
    Long countDonHangDangSanXuat();

    // Lấy báo cáo doanh thu nhóm theo ngày (Dùng Native Query cho SQL Server)
    @Query(value = "SELECT CAST(ngay_tao AS DATE) as date, SUM(tong_tien) as revenue " +
            "FROM don_hang WHERE trang_thai != 'DA_HUY' " +
            "GROUP BY CAST(ngay_tao AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getRevenueReportByDay();
    // 1. Thống kê doanh thu theo kênh
    @Query("SELECT new com.poly.cake.dto.DoanhThuKenhDto(d.nguonDon, SUM(d.tongTien)) " +
            "FROM DonHang d WHERE d.trangThai = 'HOAN_THANH' " +
            "GROUP BY d.nguonDon")
    List<DoanhThuKenhDto> getDoanhThuTheoKenh();

    // 2. Top sản phẩm bán chạy nhất
    @Query("SELECT new com.poly.cake.dto.TopSanPhamDto(sp.tenSanPham, SUM(ct.soLuong)) " +
            "FROM ChiTietDonHang ct JOIN ct.donHang d JOIN ct.sanPham sp " +
            "WHERE d.trangThai = 'HOAN_THANH' " +
            "GROUP BY sp.id, sp.tenSanPham " +
            "ORDER BY SUM(ct.soLuong) DESC")
    List<TopSanPhamDto> getTopSanPhamBanChay(Pageable pageable);

    @Query("SELECT new com.poly.cake.dto.HieuSuatNhanVienDto(nv.id, nv.hoTen, COUNT(d.id), SUM(d.tongTien)) " +
            "FROM DonHang d JOIN d.nhanVien nv " +
            "WHERE d.trangThai = 'HOAN_THANH' " +
            "GROUP BY nv.id, nv.hoTen " +
            "ORDER BY SUM(d.tongTien) DESC")
    List<HieuSuatNhanVienDto> getHieuSuatNhanVien();

    @Query("SELECT new com.poly.cake.dto.VoucherUsageDto(m.maCode, k.id, k.hoTen, d.id, d.ngayTao) " +
            "FROM DonHang d " +
            "JOIN d.maGiamGia m " +
            "JOIN d.khachHang k " +
            "WHERE (:maCode IS NULL OR m.maCode = :maCode) " +
            "ORDER BY m.maCode, d.ngayTao DESC")
    List<VoucherUsageDto> getVoucherUsage(@Param("maCode") String maCode);

    // Danh sách đơn hàng đã áp dụng 1 mã giảm giá cụ thể (dùng cho trang lịch sử dùng voucher)
    @Query("SELECT d FROM DonHang d JOIN FETCH d.khachHang WHERE d.maGiamGia.id = :maGiamGiaId ORDER BY d.ngayTao DESC")
    List<DonHang> findByMaGiamGiaId(@Param("maGiamGiaId") Long maGiamGiaId);

    // T-ADMIN: Hiệu suất nhân viên có thể lọc theo khoảng ngày, dùng cho trang Hiệu suất nhân viên (Admin)
    @Query("SELECT new com.poly.cake.dto.HieuSuatNhanVienDto(nv.id, nv.hoTen, COUNT(d.id), COALESCE(SUM(d.tongTien), 0)) " +
            "FROM DonHang d JOIN d.nhanVien nv " +
            "WHERE d.trangThai IN ('HOAN_THANH', 'DA_GIAO', 'DA_THANH_TOAN') " +
            "AND (:tuNgay IS NULL OR d.ngayTao >= :tuNgay) " +
            "AND (:denNgay IS NULL OR d.ngayTao <= :denNgay) " +
            "GROUP BY nv.id, nv.hoTen " +
            "ORDER BY SUM(d.tongTien) DESC")
    List<HieuSuatNhanVienDto> getHieuSuatNhanVienTheoKhoang(@Param("tuNgay") LocalDateTime tuNgay,
                                                             @Param("denNgay") LocalDateTime denNgay);
}