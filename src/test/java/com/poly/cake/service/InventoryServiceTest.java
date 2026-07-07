package com.poly.cake.service;

import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThongBaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private SanPhamRepository sanPhamRepository;

    @Mock
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private ThongBaoRepository thongBaoRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InventoryService inventoryService;

    private SanPham sanPham;

    @BeforeEach
    void setUp() {
        sanPham = new SanPham();
        sanPham.setId(1L);
        sanPham.setTenSanPham("Bánh kem");
        sanPham.setSoLuongTon(20);
        sanPham.setNguongCanhBao(10);
    }

    @Test
    void testDieuChinhTonKhoThuCong_Add() {
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(sanPham));
        
        SanPham updatedSanPham = new SanPham();
        updatedSanPham.setId(1L);
        updatedSanPham.setTenSanPham("Bánh kem");
        updatedSanPham.setSoLuongTon(25); // 20 + 5
        updatedSanPham.setNguongCanhBao(10);
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(updatedSanPham)); // for the second findById

        SanPham result = inventoryService.dieuChinhTonKhoThuCong(1L, 5);

        assertEquals(25, result.getSoLuongTon());
        verify(sanPhamRepository, times(1)).congLaiSoLuongTon(1L, 5);
    }

    @Test
    void testDieuChinhTonKhoThuCong_Subtract_Success() {
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(sanPham));
        when(sanPhamRepository.truSoLuongTon(1L, 5)).thenReturn(1);
        
        SanPham updatedSanPham = new SanPham();
        updatedSanPham.setId(1L);
        updatedSanPham.setTenSanPham("Bánh kem");
        updatedSanPham.setSoLuongTon(15); // 20 - 5
        updatedSanPham.setNguongCanhBao(10);
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(updatedSanPham));

        SanPham result = inventoryService.dieuChinhTonKhoThuCong(1L, -5);

        assertEquals(15, result.getSoLuongTon());
        verify(sanPhamRepository, times(1)).truSoLuongTon(1L, 5);
    }

    @Test
    void testDieuChinhTonKhoThuCong_Subtract_FailNotEnoughStock() {
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(sanPham));
        when(sanPhamRepository.truSoLuongTon(1L, 25)).thenReturn(0);

        assertThrows(BusinessException.class, () -> {
            inventoryService.dieuChinhTonKhoThuCong(1L, -25);
        });
    }

    @Test
    void testTruTonKhoTheoDonHang_Success() {
        DonHang donHang = new DonHang();
        donHang.setId(1L);
        
        ChiTietDonHang ct = new ChiTietDonHang();
        ct.setSanPham(sanPham);
        ct.setSoLuong(5);
        
        when(chiTietDonHangRepository.findByDonHang(donHang)).thenReturn(Collections.singletonList(ct));
        when(sanPhamRepository.truSoLuongTon(1L, 5)).thenReturn(1);
        
        SanPham updatedSanPham = new SanPham();
        updatedSanPham.setId(1L);
        updatedSanPham.setSoLuongTon(15);
        updatedSanPham.setNguongCanhBao(10);
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(updatedSanPham));

        inventoryService.truTonKhoTheoDonHang(donHang);

        verify(sanPhamRepository, times(1)).truSoLuongTon(1L, 5);
        // Ngưỡng 10, Tồn 15 -> Không cảnh báo
        verify(notificationService, never()).notifyLowStockToAdmins(anyString());
    }

    @Test
    void testTruTonKhoTheoDonHang_LowStockWarning() {
        DonHang donHang = new DonHang();
        donHang.setId(1L);
        
        ChiTietDonHang ct = new ChiTietDonHang();
        ct.setSanPham(sanPham);
        ct.setSoLuong(12);
        
        when(chiTietDonHangRepository.findByDonHang(donHang)).thenReturn(Collections.singletonList(ct));
        when(sanPhamRepository.truSoLuongTon(1L, 12)).thenReturn(1);
        
        SanPham updatedSanPham = new SanPham();
        updatedSanPham.setId(1L);
        updatedSanPham.setTenSanPham("Bánh kem");
        updatedSanPham.setSoLuongTon(8); // 20 - 12
        updatedSanPham.setNguongCanhBao(10);
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(updatedSanPham));
        
        // Mocking user for warning
        NguoiDung admin = new NguoiDung();
        admin.setId(1L);
        when(nguoiDungRepository.findByQuyenInAndTrangThai(anyList(), eq("HOAT_DONG"))).thenReturn(Collections.singletonList(admin));
        when(thongBaoRepository.existsCanhBaoTrungGanDay(any(), anyString(), any())).thenReturn(false);

        inventoryService.truTonKhoTheoDonHang(donHang);

        verify(sanPhamRepository, times(1)).truSoLuongTon(1L, 12);
        verify(notificationService, times(1)).notifyLowStockToAdmins(anyString());
    }
}
