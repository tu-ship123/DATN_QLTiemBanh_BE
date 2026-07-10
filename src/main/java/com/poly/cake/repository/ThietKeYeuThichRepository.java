package com.poly.cake.repository;

import com.poly.cake.entity.ThietKeYeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThietKeYeuThichRepository extends JpaRepository<ThietKeYeuThich, Long> {
    List<ThietKeYeuThich> findByKhachHangId(Long khachHangId);
    boolean existsByKhachHangIdAndThietKeBanhJson(Long khachHangId, String thietKeBanhJson);
}