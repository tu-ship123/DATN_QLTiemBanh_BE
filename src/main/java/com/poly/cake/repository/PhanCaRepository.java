package com.poly.cake.repository;

import com.poly.cake.entity.PhanCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhanCaRepository extends JpaRepository<PhanCa, Long> {

    // Query cũ — giữ nguyên
    List<PhanCa> findByNhanVienIdAndNgayLamViec(Long nhanVienId, LocalDate ngayLamViec);
    Optional<PhanCa> findByIdAndNhanVienId(Long id, Long nhanVienId);

    // ── THÊM MỚI ──────────────────────────────────────────────────────────────

    /**
     * Nhân viên xem lịch ca của mình trong 1 ngày cụ thể.
     * Dùng cho: GET /api/v1/staff/my-schedules?date=2025-06-25
     */
    @Query("SELECT p FROM PhanCa p JOIN FETCH p.caLamViec WHERE p.nhanVien.id = :nhanVienId AND p.ngayLamViec = :ngay ORDER BY p.caLamViec.gioBatDau")
    List<PhanCa> findByNhanVienIdAndNgayWithCa(@Param("nhanVienId") Long nhanVienId, @Param("ngay") LocalDate ngay);

    /**
     * Nhân viên xem lịch ca của mình trong khoảng thời gian (dùng cho trang lịch tuần).
     * Dùng cho: GET /api/v1/staff/my-schedules?tuNgay=...&denNgay=...
     */
    @Query("SELECT p FROM PhanCa p JOIN FETCH p.caLamViec WHERE p.nhanVien.id = :nhanVienId AND p.ngayLamViec BETWEEN :tuNgay AND :denNgay ORDER BY p.ngayLamViec, p.caLamViec.gioBatDau")
    List<PhanCa> findByNhanVienIdAndDateRange(
            @Param("nhanVienId") Long nhanVienId,
            @Param("tuNgay") LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay
    );

    /**
     * Admin xem tất cả phân ca trong 1 ngày (dùng cho bảng quản lý lịch).
     */
    @Query("SELECT p FROM PhanCa p JOIN FETCH p.caLamViec JOIN FETCH p.nhanVien WHERE p.ngayLamViec = :ngay ORDER BY p.caLamViec.gioBatDau, p.nhanVien.hoTen")
    List<PhanCa> findAllByNgay(@Param("ngay") LocalDate ngay);

    /**
     * Admin xem phân ca của 1 nhân viên cụ thể trong khoảng ngày.
     */
    @Query("SELECT p FROM PhanCa p JOIN FETCH p.caLamViec JOIN FETCH p.nhanVien WHERE p.nhanVien.id = :nhanVienId AND p.ngayLamViec BETWEEN :tuNgay AND :denNgay ORDER BY p.ngayLamViec")
    List<PhanCa> findByNhanVienAndDateRange(
            @Param("nhanVienId") Long nhanVienId,
            @Param("tuNgay") LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay
    );

    /**
     * Kiểm tra nhân viên đã có ca trong ngày đó chưa (tránh phân ca trùng).
     */
    boolean existsByNhanVienIdAndCaLamViecIdAndNgayLamViec(
            Long nhanVienId, Long caLamViecId, LocalDate ngayLamViec
    );
}
