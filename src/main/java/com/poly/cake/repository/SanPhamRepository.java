package com.poly.cake.repository;

import com.poly.cake.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {

    // 1. Tìm theo tên sản phẩm
    List<SanPham> findByTenSanPhamContainingIgnoreCase(String keyword);

    // 2. Tìm theo trạng thái
    List<SanPham> findByTrangThai(String trangThai);

    // 3. Tìm theo danh mục
    List<SanPham> findByDanhMucId(Long danhMucId);

    // 4. Kiểm tra trùng tên
    boolean existsByTenSanPham(String tenSanPham);

    // 5. Lọc sản phẩm nâng cao
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

    // 6. CỘNG LẠI TỒN KHO (Dùng khi Hủy đơn hàng)
    @Modifying
    @Query("UPDATE SanPham s SET s.soLuongTon = s.soLuongTon + :qty WHERE s.id = :id")
    int congLaiSoLuongTon(@Param("id") Long id, @Param("qty") int qty);

    // 7. TRỪ TỒN KHO (Dùng khi Đặt hàng)
    @Modifying
    @Query("UPDATE SanPham s SET s.soLuongTon = s.soLuongTon - :qty WHERE s.id = :id AND s.soLuongTon >= :qty")
    int truSoLuongTon(@Param("id") Long id, @Param("qty") int qty);

}