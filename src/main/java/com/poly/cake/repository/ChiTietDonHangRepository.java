package com.poly.cake.repository;

import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Long> {

    // Lấy toàn bộ chi tiết sản phẩm của 1 đơn hàng - dùng để trừ tồn kho khi
    // thanh toán thành công (xem TonKhoService.truTonKhoTheoDonHang)
    List<ChiTietDonHang> findByDonHang(DonHang donHang);
}