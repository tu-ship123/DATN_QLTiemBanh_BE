package com.poly.cake.repository;

import com.poly.cake.entity.NguoiDung;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    List<NguoiDung> findByQuyen(String quyen);
    Optional<NguoiDung> findByEmail(String email);
    @Query("SELECT COUNT(u) FROM NguoiDung u WHERE u.quyen = 'KHACH_HANG' AND u.ngayTao >= :startDate AND u.ngayTao < :endDate")
    Long countKhachMoiByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
