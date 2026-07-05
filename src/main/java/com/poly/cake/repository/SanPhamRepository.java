package com.poly.cake.repository;

import com.poly.cake.entity.SanPham;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    @Modifying
    @Query("UPDATE SanPham s SET s.soLuongTon = s.soLuongTon + :qty WHERE s.id = :id")
    int congLaiSoLuongTon(@Param("id") Long id, @Param("qty") int qty);

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

    // Tìm đúng 1 sản phẩm theo tên chính xác (dùng để lấy sản phẩm đại diện bánh 3D tùy chỉnh)
    java.util.Optional<SanPham> findByTenSanPham(String tenSanPham);

    // Lấy TẤT CẢ bản ghi trùng tên, cũ nhất trước - phòng trường hợp trước đây đã lỡ
    // tạo trùng nhiều "sản phẩm đại diện bánh 3D" (id 17,18,19,20...) do gọi API nhiều
    // lần; luôn ưu tiên dùng bản ghi cũ nhất làm chuẩn, không tạo thêm bản ghi mới.
    List<SanPham> findByTenSanPhamOrderByIdAsc(String tenSanPham);

    // Tim KHOAN DUNG hon findByTenSanPhamOrderByIdAsc: gop ca nhung ban ghi da duoc
    // danh dau laNoiBo=true (du ten hien tai la gi) LAN nhung ban ghi ten gan khop
    // (bo qua khoang trang thua 2 dau + khong phan biet hoa/thuong) - de tu phat hien
    // va "chua lanh" cac ban ghi cu bi lech ten do loi tao trung truoc day, thay vi chi
    // dua vao so khop ten tuyet doi (rat de bi bo sot).
    @Query("SELECT sp FROM SanPham sp WHERE sp.laNoiBo = true " +
           "OR LOWER(TRIM(sp.tenSanPham)) = LOWER(TRIM(:tenSanPham)) " +
           "ORDER BY sp.id ASC")
    List<SanPham> timCacBanGhiCoTheLaCustomCakeMarker(@Param("tenSanPham") String tenSanPham);

    @Query("SELECT sp FROM SanPham sp WHERE " +
           "(:keyword IS NULL OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:trangThai IS NULL OR sp.trangThai = :trangThai) AND " +
           "(:danhMucId IS NULL OR sp.danhMuc.id = :danhMucId) AND " +
           // Sản phẩm nội bộ (VD: sản phẩm đại diện bánh 3D tùy chỉnh, xem
           // AdminSanPhamService.getOrCreateCustomCakeMarker) KHÔNG BAO GIỜ được lọt
           // ra danh sách, dù ở trang khách hay trang quản lý admin. Lọc theo cột
           // laNoiBo (KHÔNG so khớp tên nữa) để không bị lệch bởi bản ghi trùng/lỗi
           // encoding/thừa khoảng trắng như trước đây.
           "sp.laNoiBo = false " +
           "ORDER BY sp.ngayTao DESC")
    List<SanPham> filterProducts(
            @Param("keyword") String keyword,
            @Param("trangThai") String trangThai,
            @Param("danhMucId") Long danhMucId
    );

    // Truy vấn danh sách sản phẩm thực tế có số lượng tồn thấp hơn hoặc bằng ngưỡng cảnh báo
    // (Loại bỏ các sản phẩm ảo hoặc marker nội bộ như bánh 3D tùy chỉnh)
    @Query("SELECT s FROM SanPham s WHERE s.laNoiBo = false AND s.soLuongTon <= s.nguongCanhBao")
    List<SanPham> findLowStockProducts();
}