package com.poly.cake.repository;

import com.poly.cake.entity.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Long> {

    boolean existsByMaCode(String maCode);

    Optional<MaGiamGia> findByMaCode(String maCode);

    /** Lấy các mã có thể đổi bằng điểm, đang hoạt động, sắp xếp theo điểm tăng dần */
    List<MaGiamGia> findByDiemCanDungIsNotNullAndHoatDongTrueOrderByDiemCanDungAsc();

    /** Tìm mã theo maCode, còn hoạt động và có diemCanDung (dùng khi đổi điểm) */
    Optional<MaGiamGia> findByMaCodeAndHoatDongTrueAndDiemCanDungIsNotNull(String maCode);
}