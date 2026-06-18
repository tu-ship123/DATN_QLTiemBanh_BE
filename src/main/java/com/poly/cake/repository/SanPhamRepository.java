package com.poly.cake.repository;

import com.poly.cake.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    // Tạm thời chưa cần viết thêm hàm gì, JpaRepository đã cung cấp sẵn hàm findById() cho OrderService dùng rồi.
}