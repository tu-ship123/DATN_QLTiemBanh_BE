package com.poly.cake.repository;

import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {

    Optional<ThanhToan> findByDonHang(DonHang donHang);

    // ── T062: Tổng hợp doanh thu trong ca theo hình thức thanh toán ──────────

    /**
     * Tổng doanh thu nhóm theo hình thức thanh toán trong khoảng [tuThoiDiem, denThoiDiem).
     * Chỉ tính giao dịch THANH_CONG, đơn không bị hủy.
     *
     * Trả về List<Object[]> gồm: [hinhThuc (String), tongTien (BigDecimal)].
     */
    @Query("SELECT t.hinhThuc, COALESCE(SUM(t.soTien), 0) " +
       "FROM ThanhToan t " +
       "JOIN t.donHang d " +
       "WHERE t.trangThai = 'THANH_CONG' " +
       "  AND d.trangThai <> 'DA_HUY' " +
       "  AND t.thoiDiemThanhToan >= :tuThoiDiem " +
       "  AND t.thoiDiemThanhToan < :denThoiDiem " +
       "GROUP BY t.hinhThuc")
    List<Object[]> sumDoanhThuTheoHinhThuc(
            @Param("tuThoiDiem") LocalDateTime tuThoiDiem,
            @Param("denThoiDiem") LocalDateTime denThoiDiem
    );

    @Query("SELECT COUNT(DISTINCT t.donHang.id) " +
        "FROM ThanhToan t " +
        "JOIN t.donHang d " +
        "WHERE t.trangThai = 'THANH_CONG' " +
        "  AND d.trangThai <> 'DA_HUY' " +
        "  AND t.thoiDiemThanhToan >= :tuThoiDiem " +
        "  AND t.thoiDiemThanhToan < :denThoiDiem")
    Long countDonThanhCong(
            @Param("tuThoiDiem") LocalDateTime tuThoiDiem,
            @Param("denThoiDiem") LocalDateTime denThoiDiem
    );
}