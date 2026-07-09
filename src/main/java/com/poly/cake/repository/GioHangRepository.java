package com.poly.cake.repository;

import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Long> {
    Optional<GioHang> findByKhachHang(NguoiDung khachHang);
    Optional<GioHang> findByKhachHangEmail(String email);

    // [T105 - Fix N+1] Lấy giỏ hàng kèm chi tiết sản phẩm và danh mục trong 1 câu SQL
    // Tránh N+1 kép: lazy-load chiTietGioHang + lazy-load sanPham.danhMuc
    @Query("SELECT g FROM GioHang g " +
            "LEFT JOIN FETCH g.chiTietGioHangs cg " +
            "LEFT JOIN FETCH cg.sanPham s " +
            "LEFT JOIN FETCH s.danhMuc " +
            "WHERE g.khachHang = :khachHang")
    Optional<GioHang> findByKhachHangWithDetails(@Param("khachHang") NguoiDung khachHang);
}
