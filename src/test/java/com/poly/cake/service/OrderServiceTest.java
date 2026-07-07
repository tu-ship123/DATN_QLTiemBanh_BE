package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private DonHangRepository donHangRepository;

    @Mock
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private NguoiDung currentUser;
    private DonHang donHang;

    @BeforeEach
    void setUp() {
        currentUser = new NguoiDung();
        currentUser.setId(1L);
        currentUser.setHoTen("Test User");

        donHang = new DonHang();
        donHang.setId(1L);
        donHang.setKhachHang(currentUser);
        donHang.setTrangThai("CHO_XAC_NHAN");
    }

    @Test
    void testCancelMyOrder_Success() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(donHangRepository.findByIdAndKhachHangId(1L, 1L)).thenReturn(Optional.of(donHang));
        when(donHangRepository.save(any(DonHang.class))).thenReturn(donHang);

        OrderDto.Response result = orderService.cancelMyOrder(1L);

        assertEquals("DA_HUY", donHang.getTrangThai());
        assertNotNull(result);
        verify(donHangRepository, times(1)).save(donHang);
        verify(notificationService, times(1)).notifyNewOrderToAdmins(anyString());
    }

    @Test
    void testCancelMyOrder_NotFound() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(donHangRepository.findByIdAndKhachHangId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.cancelMyOrder(1L);
        });
    }

    @Test
    void testCancelMyOrder_InvalidStatus() {
        donHang.setTrangThai("DANG_GIAO");
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(donHangRepository.findByIdAndKhachHangId(1L, 1L)).thenReturn(Optional.of(donHang));

        assertThrows(BusinessException.class, () -> {
            orderService.cancelMyOrder(1L);
        });
    }

    @Test
    void testProcessOrder_Confirm_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(donHangRepository.findById(1L)).thenReturn(Optional.of(donHang));
        when(donHangRepository.save(any(DonHang.class))).thenReturn(donHang);

        OrderDto.Response result = orderService.processOrder(1L, "DA_XAC_NHAN", null);

        assertEquals("DA_XAC_NHAN", donHang.getTrangThai());
        assertNotNull(result);
        verify(donHangRepository, times(1)).save(donHang);
        verify(notificationService, times(1)).notifyOrderStatus(eq(1L), anyString());
    }

    @Test
    void testProcessOrder_InvalidTransition() {
        donHang.setTrangThai("DA_XAC_NHAN");
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(donHangRepository.findById(1L)).thenReturn(Optional.of(donHang));

        assertThrows(BusinessException.class, () -> {
            orderService.processOrder(1L, "HOAN_THANH", null); // DA_XAC_NHAN -> HOAN_THANH is not allowed directly
        });
    }
}
