package com.poly.cake.repository;

import com.poly.cake.entity.DiaChiGiaoHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiGiaoHangRepository extends JpaRepository<DiaChiGiaoHang, Long> {
    List<DiaChiGiaoHang> findByNguoiDungId(Long nguoiDungId);
    long countByNguoiDungId(Long nguoiDungId);
    Optional<DiaChiGiaoHang> findByNguoiDungIdAndLaMacDinhTrue(Long nguoiDungId);
}