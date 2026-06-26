package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * T055 – Validate chuỗi JSON thiết kế bánh 3D.
 *
 * Luồng tại bước "chọn khung":
 *   FE bắt khách nhập kich_thuoc (chiều cao + đường kính).
 *   Backend kiểm tra lại để đảm bảo dữ liệu luôn hợp lệ trước khi lưu DB.
 */
@Component
public class CakeDesignValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse + validate chuỗi JSON thiết kế.
     *
     * @param cakeDesignJson chuỗi JSON từ FE (có thể null → không làm bánh 3D)
     * @return Map đã parse, hoặc null nếu không có thiết kế
     * @throws BusinessException nếu JSON không hợp lệ hoặc thiếu kích thước
     */
    public Map<String, Object> validateAndParse(String cakeDesignJson) {
        if (cakeDesignJson == null || cakeDesignJson.trim().isEmpty()) {
            return null; // Đơn bình thường, không có thiết kế 3D
        }

        Map<String, Object> designMap;
        try {
            designMap = objectMapper.readValue(cakeDesignJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException(
                    "Dữ liệu thiết kế bánh 3D không đúng định dạng JSON! Chi tiết: " + e.getMessage());
        }

        // ── Kiểm tra "khung" bắt buộc ────────────────────────────────────────
        Object khungObj = designMap.get("khung");
        if (khungObj == null) {
            throw new BusinessException(
                    "Thiết kế bánh 3D thiếu thông tin 'khung'! " +
                    "Vui lòng quay lại bước chọn khung và hoàn tất.");
        }

        if (!(khungObj instanceof Map)) {
            throw new BusinessException("Trường 'khung' trong thiết kế 3D không đúng cấu trúc!");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> khung = (Map<String, Object>) khungObj;

        // ── Kiểm tra "kich_thuoc" bắt buộc (khách PHẢI tự nhập) ─────────────
        Object kichThuocObj = khung.get("kich_thuoc");
        if (kichThuocObj == null) {
            throw new BusinessException(
                    "Bạn chưa nhập kích thước bánh! " +
                    "Tại bước chọn khung, vui lòng điền Chiều cao (cm) và Đường kính (cm) của bánh.");
        }

        if (!(kichThuocObj instanceof Map)) {
            throw new BusinessException("Trường 'kich_thuoc' trong thiết kế 3D không đúng cấu trúc!");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> kichThuoc = (Map<String, Object>) kichThuocObj;

        // Kiểm tra chiều cao
        Object chieuCaoObj = kichThuoc.get("chieu_cao_cm");
        if (chieuCaoObj == null) {
            throw new BusinessException(
                    "Bạn chưa nhập chiều cao bánh! Vui lòng điền chiều cao (cm) tại bước chọn khung.");
        }
        double chieuCao = toDouble(chieuCaoObj, "chieu_cao_cm");
        if (chieuCao < 5 || chieuCao > 80) {
            throw new BusinessException(
                    "Chiều cao bánh phải từ 5 cm đến 80 cm! (Bạn nhập: " + chieuCao + " cm)");
        }

        // Kiểm tra đường kính
        Object duongKinhObj = kichThuoc.get("duong_kinh_cm");
        if (duongKinhObj == null) {
            throw new BusinessException(
                    "Bạn chưa nhập đường kính bánh! Vui lòng điền đường kính (cm) tại bước chọn khung.");
        }
        double duongKinh = toDouble(duongKinhObj, "duong_kinh_cm");
        if (duongKinh < 5 || duongKinh > 80) {
            throw new BusinessException(
                    "Đường kính bánh phải từ 5 cm đến 80 cm! (Bạn nhập: " + duongKinh + " cm)");
        }

        return designMap;
    }

    /**
     * Tạo chuỗi ghi chú kích thước tự động để lưu vào cột ghi_chu của đơn hàng.
     * Thợ làm bánh sẽ thấy ngay thông tin này khi mở đơn.
     *
     * @param designMap Map đã parse từ validateAndParse()
     * @param ghiChuKhach ghi chú gốc của khách (có thể null)
     * @return chuỗi ghi chú đã ghép, ví dụ:
     *         "[BÁNH 3D] Kích thước: Đường kính 20cm × Chiều cao 12cm\n---\nGhi chú khách: ..."
     */
    public String buildGhiChuKichThuoc(Map<String, Object> designMap, String ghiChuKhach) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> khung = (Map<String, Object>) designMap.get("khung");
            @SuppressWarnings("unchecked")
            Map<String, Object> kichThuoc = (Map<String, Object>) khung.get("kich_thuoc");

            double chieuCao  = toDouble(kichThuoc.get("chieu_cao_cm"),  "chieu_cao_cm");
            double duongKinh = toDouble(kichThuoc.get("duong_kinh_cm"), "duong_kinh_cm");

            // Lấy thêm hình dạng nếu có (TRON / VUONG / TIM...)
            String hinhDang = (String) khung.getOrDefault("hinh_dang", "");
            String hinhDangText = (hinhDang != null && !hinhDang.isBlank())
                    ? " | Hình dạng: " + hinhDang : "";

            String ghiChuKichThuoc = String.format(
                    "[BÁNH 3D] Kích thước: Đường kính %.0f cm × Chiều cao %.0f cm%s",
                    duongKinh, chieuCao, hinhDangText
            );

            // Ghép với ghi chú của khách
            if (ghiChuKhach != null && !ghiChuKhach.trim().isEmpty()) {
                return ghiChuKichThuoc + "\n---\nGhi chú thêm: " + ghiChuKhach.trim();
            }
            return ghiChuKichThuoc;

        } catch (Exception e) {
            // Không vỡ đơn hàng vì lỗi ghi chú phụ
            return (ghiChuKhach != null ? ghiChuKhach : "") + " [BÁNH 3D]";
        }
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────
    private double toDouble(Object value, String fieldName) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            throw new BusinessException(
                    "Trường '" + fieldName + "' trong kích thước bánh phải là số! (Nhận được: " + value + ")");
        }
    }
}