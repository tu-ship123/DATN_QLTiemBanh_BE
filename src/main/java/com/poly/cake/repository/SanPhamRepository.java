package com.poly.cake.repository;

import com.poly.cake.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    // Tạm thời chưa cần viết thêm hàm gì, JpaRepository đã cung cấp sẵn hàm findById() cho OrderService dùng rồi.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SanPham s SET s.soLuongTon = s.soLuongTon - :qty WHERE s.id = :id AND s.soLuongTon >= :qty")
    int truSoLuongTon(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("qty") int qty);
}