package com.poly.cake.repository;

import com.poly.cake.entity.CaLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {
    // Spring Boot sẽ tự động "đúc" ra toàn bộ các hàm thêm/sửa/xóa cơ bản
}