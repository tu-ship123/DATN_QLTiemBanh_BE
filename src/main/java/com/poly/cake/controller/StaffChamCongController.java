package com.poly.cake.controller;

import com.poly.cake.dto.StaffCheckinRequest;
import com.poly.cake.dto.ChamCongResponse;
import com.poly.cake.service.ChamCongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff - Chấm công", description = "API check-in/check-out cho nhân viên")
public class StaffChamCongController {

    private final ChamCongService chamCongService;

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
    @Operation(summary = "Check-in ca làm việc",
            description = "Nhân viên chọn ca đã được phân, ghi nhận giờ vào và tính phút đi trễ")
    public ResponseEntity<?> checkIn(@Valid @RequestBody StaffCheckinRequest request) {
        ChamCongResponse response = chamCongService.checkIn(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{phanCaId}")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
    @Operation(summary = "Check-out ca làm việc",
            description = "Nhân viên chốt giờ ra, hệ thống tự động đánh dấu về sớm nếu ra trước giờ kết thúc ca")
    public ResponseEntity<?> checkOut(@PathVariable Long phanCaId) {
        ChamCongResponse response = chamCongService.checkOut(phanCaId);
        return ResponseEntity.ok(response);
    }
}