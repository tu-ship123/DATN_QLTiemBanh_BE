package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.entity.PhuKienTrangTri;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.repository.PhuKienTrangTriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tính lại giá CHUẨN của 1 chiếc bánh 3D tùy chỉnh (CakeBuilder3D) ngay tại Backend,
 * thay vì tin tưởng số tiền FE gửi lên (donGiaTuyChinh trong GioHangDto.ThemVaoGioRequest).
 *
 * Lý do: khách hoàn toàn có thể sửa giá gửi lên qua DevTools/Postman nếu BE chỉ lưu
 * nguyên số FE tính, nên phần "giá kem" (size + số tầng) và "giá phụ kiện" phải được
 * BE tính lại độc lập từ chính JSON thiết kế (thietKeBanhJson) rồi mới lưu vào DB.
 *
 * QUAN TRỌNG: Công thức + bảng giá bên dưới phải LUÔN khớp với CakeBuilder3D.vue
 * (biến `sizes`, `tiers` và computed `totalPrice`). Nếu bên FE đổi giá, phải sửa lại
 * y hệt ở đây, nếu không giá hiển thị cho khách và giá lưu vào đơn sẽ bị lệch nhau.
 */
@Service
@RequiredArgsConstructor
public class CakeDesignPricingService {

    private final PhuKienTrangTriRepository phuKienTrangTriRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Khớp với `sizes` trong CakeBuilder3D.vue
    private static final Map<String, BigDecimal> GIA_THEO_SIZE = Map.of(
            "s", BigDecimal.valueOf(320000),
            "m", BigDecimal.valueOf(420000),
            "l", BigDecimal.valueOf(560000)
    );

    // Khớp với `tiers` (phụ phí theo số tầng) trong CakeBuilder3D.vue - đây chính là
    // "giá kem/công phủ kem" tính theo tầng mà không cần quản lý tồn kho nguyên liệu.
    private static final Map<Integer, BigDecimal> PHU_PHI_THEO_TANG = Map.of(
            1, BigDecimal.ZERO,
            2, BigDecimal.valueOf(150000),
            3, BigDecimal.valueOf(320000)
    );

    /**
     * @param thietKeBanhJson snapshot thô của CakeBuilder3D (shape/size/tierCount/accessories...)
     * @return giá chuẩn đã tính lại, hoặc null nếu không phải bánh tùy chỉnh (JSON rỗng)
     */
    public BigDecimal tinhGiaChuan(String thietKeBanhJson) {
        if (thietKeBanhJson == null || thietKeBanhJson.isBlank()) {
            return null;
        }

        Map<String, Object> design;
        try {
            design = objectMapper.readValue(thietKeBanhJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("Dữ liệu thiết kế bánh 3D không đúng định dạng JSON!");
        }

        // ── Giá theo size (S/M/L) ──────────────────────────────────────────
        Object sizeObj = design.get("size");
        String size = sizeObj != null ? sizeObj.toString() : "m";
        BigDecimal giaSize = GIA_THEO_SIZE.getOrDefault(size, GIA_THEO_SIZE.get("m"));

        // ── Phụ phí theo số tầng (= "giá kem" tính theo tầng) ──────────────
        int soTang = toInt(design.get("tierCount"), 1);
        BigDecimal phuPhiTang = PHU_PHI_THEO_TANG.getOrDefault(soTang, BigDecimal.ZERO);

        // ── Tổng tiền phụ kiện trang trí đã gắn lên bánh ───────────────────
        BigDecimal giaPhuKien = tinhGiaPhuKien(design.get("accessories"));

        return giaSize.add(phuPhiTang).add(giaPhuKien);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal tinhGiaPhuKien(Object accessoriesObj) {
        if (!(accessoriesObj instanceof List)) {
            return BigDecimal.ZERO;
        }

        BigDecimal tong = BigDecimal.ZERO;
        for (Object o : (List<Object>) accessoriesObj) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> acc = (Map<String, Object>) o;

            int soLuong = toInt(acc.get("so_luong"), 0);
            if (soLuong <= 0) continue;

            tong = tong.add(donGiaPhuKien(acc).multiply(BigDecimal.valueOf(soLuong)));
        }
        return tong;
    }

    /**
     * Ưu tiên lấy giá THẬT trong DB theo phu_kien_id (chống sửa giá từ client).
     * Nếu id không phải số hợp lệ (VD: "fallback-1" - dữ liệu tĩnh dự phòng của FE khi
     * chưa gọi được API phụ kiện) hoặc không còn tồn tại trong DB, đành tạm tin giá FE
     * gửi kèm để không chặn luồng đặt hàng khi hệ thống phụ kiện đang lỗi.
     */
    private BigDecimal donGiaPhuKien(Map<String, Object> acc) {
        Object idObj = acc.get("phu_kien_id");
        if (idObj != null) {
            try {
                Long id = Long.parseLong(idObj.toString());
                return phuKienTrangTriRepository.findById(id)
                        .map(PhuKienTrangTri::getDonGia)
                        .orElseGet(() -> donGiaTuFE(acc));
            } catch (NumberFormatException ignored) {
                // id kiểu "fallback-1" -> không phải ID thật trong DB, dùng giá FE gửi
            }
        }
        return donGiaTuFE(acc);
    }

    private BigDecimal donGiaTuFE(Map<String, Object> acc) {
        Object donGiaObj = acc.get("don_gia");
        if (donGiaObj instanceof Number) {
            return BigDecimal.valueOf(((Number) donGiaObj).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private int toInt(Object o, int defaultValue) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return defaultValue;
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
