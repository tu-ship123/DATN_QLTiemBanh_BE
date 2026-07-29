package com.poly.cake.controller;

import com.poly.cake.dto.ThietKeBanhDto;
import com.poly.cake.dto.DatHangDto;
import com.poly.cake.dto.DatHangXuLyDto;
import com.poly.cake.service.DatHangService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class DatHangController {

    @Autowired
    private DatHangService orderService;


    // 1. API ĐẶT HÀNG (CHECKOUT) - Chỉ dành cho Khách hàng
    @PostMapping
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> checkout(@Valid @RequestBody DatHangDto.Request request, Authentication authentication) {
        String email = authentication.getName();
        DatHangDto.Response response = orderService.createOrder(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // T055 – VALIDATE THIẾT KẾ 3D TRƯỚC KHI ĐẶT HÀNG
    // FE gọi ngay tại bước chọn khung để kiểm tra kích thước trước khi next step
    @PostMapping("/validate-cake-design")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Operation(summary = "Validate thiết kế bánh 3D",
            description = "FE gọi tại bước chọn khung để kiểm tra JSON hợp lệ chưa (kích thước chiều cao + đường kính)")
    public ResponseEntity<?> validateCakeDesign(@Valid @RequestBody ThietKeBanhDto.Request request) {
        ThietKeBanhDto.KichThuoc kt = request.getKhung().getKich_thuoc();
        return ResponseEntity.ok(Map.of(
                "hopLe",   true,
                "tomTat",  String.format("Đường kính %.0f cm × Chiều cao %.0f cm",
                        kt.getDuong_kinh_cm(), kt.getChieu_cao_cm()),
                "message", "Kích thước hợp lệ! Bạn có thể tiếp tục đặt hàng."
        ));
    }

    // 2. API LẤY LỊCH SỬ ĐƠN HÀNG CỦA KHÁCH ĐANG LOG IN - Chỉ dành cho Khách hàng
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<DatHangDto.Response>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(email));
    }

    // 3. API XEM CHI TIẾT ĐƠN HÀNG THEO ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KHACH_HANG', 'ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id, Authentication authentication) {
        // Lấy email người dùng từ token
        String email = authentication.getName();

        // Lấy quyền (role) đầu tiên của người dùng
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(orderService.getOrderById(id, email, role));
    }

    // 4. API LẤY TOÀN BỘ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN mới được xem
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<List<DatHangDto.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // 5. API XỬ LÝ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN có quyền thao tác
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> processOrder(@PathVariable Long id, @Valid @RequestBody DatHangXuLyDto request, Authentication authentication) {
        String emailNhanVien = authentication.getName();
        DatHangDto.Response updatedOrder = orderService.processOrder(id, request, emailNhanVien);
        return ResponseEntity.ok(updatedOrder);
    }

    // 6. API USER TỰ HỦY ĐƠN HÀNG - Chỉ Khách hàng mới được tự hủy đơn của mình
    // T072: Service tự kiểm tra điều kiện hủy + tự hoàn tiền cọc/thanh toán nếu có
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        DatHangDto.Response response = orderService.cancelOrder(id, email);
        return ResponseEntity.ok(response);
    }

    // 7. API LẤY DỮ LIỆU THIẾT KẾ 3D
    @GetMapping("/{id}/design")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN', 'KHACH_HANG')")
    @Operation(summary = "Lấy dữ liệu thiết kế 3D của đơn hàng",
            description = "Trả về cấu trúc JSON đầy đủ để Frontend render Three.js popup")
    public ResponseEntity<?> getOrder3DDesign(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get3DCakeDesign(id));
    }

    // 8. API TẢI LẠI PDF HÓA ĐƠN CỦA ĐƠN HÀNG (T072)
    // Dùng cho trang "Lịch sử đơn hàng" của khách - khách có thể tải lại hóa đơn
    // bất kỳ lúc nào mà không cần lục lại email cũ.
    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAnyRole('KHACH_HANG', 'ADMIN', 'NHAN_VIEN')")
    @Operation(summary = "Tải PDF hóa đơn của đơn hàng",
            description = "Sinh lại file PDF hóa đơn ngay tại thời điểm gọi API (không lưu file cố định)")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        byte[] pdf = orderService.getInvoicePdf(id, email, role);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"HoaDon-HD-" + id + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // 9. API "ĐẶT LẠI ĐƠN CŨ" (RE-ORDER) - Chỉ dành cho Khách hàng (DF_ST05)
    // Copy toàn bộ sản phẩm của đơn cũ vào giỏ hàng hiện tại của khách.
    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Operation(summary = "Đặt lại đơn hàng cũ",
            description = "Thêm toàn bộ sản phẩm của 1 đơn hàng cũ vào giỏ hàng hiện tại của khách")
    public ResponseEntity<?> reorder(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        DatHangDto.ReorderResponse response = orderService.datLaiDonHang(id, email);
        return ResponseEntity.ok(response);
    }

    // 10. API GỬI "YÊU CẦU SỬA ĐƠN" - Chỉ dành cho Khách hàng (DF_ST06)
    // Chỉ áp dụng khi đơn đang ở trạng thái Chờ xác nhận, đồng bộ ngay tới nhân viên qua WebSocket.
    @PostMapping("/{id}/edit-request")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Operation(summary = "Gửi yêu cầu sửa đơn hàng",
            description = "Khách hàng đề nghị thay đổi thông tin đơn (địa chỉ/SĐT/ngày giao/ghi chú), " +
                    "hệ thống lưu lại và thông báo realtime cho nhân viên/admin")
    public ResponseEntity<?> guiYeuCauSuaDon(@PathVariable Long id,
                                              @Valid @RequestBody DatHangDto.UpdateRequest request,
                                              Authentication authentication) {
        String email = authentication.getName();
        DatHangDto.Response response = orderService.guiYeuCauSuaDon(id, request, email);
        return ResponseEntity.ok(response);
    }
}