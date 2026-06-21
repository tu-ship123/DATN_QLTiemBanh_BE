package com.poly.cake.repository;

import com.poly.cake.entity.SanPham;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    // Tạm thời chưa cần viết thêm hàm gì, JpaRepository đã cung cấp sẵn hàm findById() cho OrderService dùng rồi.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SanPham s SET s.soLuongTon = s.soLuongTon - :qty WHERE s.id = :id AND s.soLuongTon >= :qty")
    int truSoLuongTon(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("qty") int qty);

    // Tìm theo tên sản phẩm
    List<SanPham> findByTenSanPhamContainingIgnoreCase(String keyword);

    // Tìm theo trạng thái
    List<SanPham> findByTrangThai(String trangThai);

    // Tìm theo danh mục
    List<SanPham> findByDanhMucId(Long danhMucId);

    // Kiểm tra trùng tên
    boolean existsByTenSanPham(String tenSanPham);

    @Query("SELECT sp FROM SanPham sp WHERE " +
           "(:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:trangThai IS NULL OR sp.trangThai = :trangThai) AND " +
           "(:danhMucId IS NULL OR sp.danhMuc.id = :danhMucId) " +
           "ORDER BY sp.ngayTao DESC")
    List<SanPham> filterProducts(
            @Param("keyword") String keyword,
            @Param("trangThai") String trangThai,
            @Param("danhMucId") Long danhMucId
    );
}