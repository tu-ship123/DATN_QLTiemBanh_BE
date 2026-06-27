package com.poly.cake.repository;

import com.poly.cake.entity.DanhGia;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {

    /** Lấy tất cả đánh giá của 1 đơn hàng */
    List<DanhGia> findByDonHang(DonHang donHang);

    /** Kiểm tra khách đã đánh giá sản phẩm trong đơn này chưa */
    boolean existsByKhachHangAndDonHangAndSanPham(NguoiDung khachHang, DonHang donHang, SanPham sanPham);

    /** Kiểm tra đơn hàng đã có ít nhất 1 đánh giá chưa */
    boolean existsByKhachHangAndDonHang(NguoiDung khachHang, DonHang donHang);

    /** Lấy tất cả đánh giá của 1 sản phẩm (dùng cho trang sản phẩm) */
    List<DanhGia> findBySanPhamAndBiAnFalseOrderByNgayTaoDesc(SanPham sanPham);

    /** Lấy đánh giá của khách trong đơn cụ thể */
    Optional<DanhGia> findByKhachHangAndDonHangAndSanPham(NguoiDung khachHang, DonHang donHang, SanPham sanPham);
}