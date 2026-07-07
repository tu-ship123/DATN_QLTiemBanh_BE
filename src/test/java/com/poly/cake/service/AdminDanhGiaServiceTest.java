package com.poly.cake.service;

import com.poly.cake.dto.DanhGiaDto;
import com.poly.cake.entity.DanhGia;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.DanhGiaRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminDanhGiaServiceTest {

    @Mock
    private DanhGiaRepository danhGiaRepository;

    @Mock
    private SanPhamRepository sanPhamRepository;

    @InjectMocks
    private AdminDanhGiaService adminDanhGiaService;

    private DanhGia danhGia1;
    private DanhGia danhGia2;

    @BeforeEach
    void setUp() {
        NguoiDung khachHang = new NguoiDung();
        khachHang.setId(1L);
        khachHang.setHoTen("Nguyen Van A");

        SanPham sanPham = new SanPham();
        sanPham.setId(1L);
        sanPham.setTenSanPham("Banh kem socola");
        sanPham.setAnhSanPham("anh.jpg");

        DonHang donHang = new DonHang();
        donHang.setId(1L);

        danhGia1 = new DanhGia();
        danhGia1.setId(1L);
        danhGia1.setKhachHang(khachHang);
        danhGia1.setSanPham(sanPham);
        danhGia1.setDonHang(donHang);
        danhGia1.setSoSao(5);
        danhGia1.setNoiDung("Banh ngon");
        danhGia1.setBiAn(false);
        danhGia1.setNgayTao(LocalDateTime.now());

        danhGia2 = new DanhGia();
        danhGia2.setId(2L);
        danhGia2.setKhachHang(khachHang);
        danhGia2.setSanPham(sanPham);
        danhGia2.setDonHang(donHang);
        danhGia2.setSoSao(3);
        danhGia2.setNoiDung("Tam duoc");
        danhGia2.setPhanHoiCuaTiem("Cam on ban");
        danhGia2.setBiAn(true);
        danhGia2.setNgayTao(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testGetAll_NoFilter() {
        when(danhGiaRepository.findAll()).thenReturn(Arrays.asList(danhGia1, danhGia2));

        List<DanhGiaDto.Response> result = adminDanhGiaService.getAll(null, null, null);

        assertEquals(2, result.size());
        assertEquals(5, result.get(0).getSoSao()); // Sorted by date desc
    }

    @Test
    void testGetAll_WithFilter() {
        when(danhGiaRepository.findAll()).thenReturn(Arrays.asList(danhGia1, danhGia2));

        List<DanhGiaDto.Response> result = adminDanhGiaService.getAll(5, null, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testGetStats() {
        when(danhGiaRepository.findAll()).thenReturn(Arrays.asList(danhGia1, danhGia2));

        DanhGiaDto.StatsResponse stats = adminDanhGiaService.getStats();

        assertEquals(2, stats.getTong());
        assertEquals(1, stats.getChuaTraLoi());
        assertEquals(1, stats.getBiAn());
        assertEquals(4.0, stats.getTrungBinhSao());
        assertEquals(1, stats.getSao5());
        assertEquals(0, stats.getSao4());
        assertEquals(1, stats.getSao3());
    }

    @Test
    void testReply_Success() {
        when(danhGiaRepository.findById(1L)).thenReturn(Optional.of(danhGia1));
        when(danhGiaRepository.save(any(DanhGia.class))).thenReturn(danhGia1);

        DanhGiaDto.Response result = adminDanhGiaService.reply(1L, "Phan hoi moi");

        assertNotNull(result);
        assertEquals("Phan hoi moi", result.getPhanHoiCuaTiem());
        verify(danhGiaRepository, times(1)).save(danhGia1);
    }

    @Test
    void testReply_NotFound() {
        when(danhGiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            adminDanhGiaService.reply(99L, "Phan hoi moi");
        });
    }

    @Test
    void testToggleBiAn() {
        when(danhGiaRepository.findById(1L)).thenReturn(Optional.of(danhGia1));
        when(danhGiaRepository.save(any(DanhGia.class))).thenReturn(danhGia1);

        DanhGiaDto.Response result = adminDanhGiaService.toggleBiAn(1L);

        assertTrue(result.getBiAn()); // Initially false, toggled to true
        verify(danhGiaRepository, times(1)).save(danhGia1);
    }

    @Test
    void testDelete() {
        when(danhGiaRepository.findById(1L)).thenReturn(Optional.of(danhGia1));

        adminDanhGiaService.delete(1L);

        verify(danhGiaRepository, times(1)).delete(danhGia1);
    }
}
