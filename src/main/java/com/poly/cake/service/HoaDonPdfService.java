package com.poly.cake.service;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * T072 - Sinh file PDF hoa don ban hang de dinh kem email xac nhan dat hang.
 * <p>
 * DA DOI THU VIEN: chuyen tu Apache PDFBox "tho" (tu ve text theo toa do
 * x/y, tu ngat trang bang tay) sang <b>openhtmltopdf</b> - build hoa don
 * duoi dang chuoi HTML/CSS (giong viet 1 trang web tinh) roi convert sang
 * PDF. Ly do doi:
 * <ul>
 *   <li>Khong can tu tinh toa do cot (COL_TEN, COL_SL...), khong can tu
 *       viet logic ngat trang (checkPageBreak) - HTML/CSS tu lo het.</li>
 *   <li>Layout de chinh sua, de them border/mau nen/logo hon nhieu.</li>
 *   <li>Van nhung (embed) font Unicode TTF de hien thi tieng Viet co dau
 *       y nhu cach cu, chi khac la khai bao qua PdfRendererBuilder.useFont()
 *       thay vi PDType0Font.load().</li>
 * </ul>
 * <p>
 * openhtmltopdf-pdfbox van dung PDFBox ben duoi de ghi ra PDF nen van can
 * dependency org.apache.pdfbox:pdfbox (da co san trong pom.xml), CHI THEM
 * dependency moi:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.openhtmltopdf</groupId>
 *     <artifactId>openhtmltopdf-pdfbox</artifactId>
 *     <version>1.0.10</version>
 * </dependency>
 * }</pre>
 * Font TTF van de tai: src/main/resources/fonts/DejaVuSans.ttf va
 * DejaVuSans-Bold.ttf (SIL Open Font License - mien phi).
 */
@Slf4j
@Service
public class HoaDonPdfService {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat TIEN_FORMAT = new DecimalFormat("#,##0");
    private static final String FONT_FAMILY = "DejaVu Sans";

    /**
     * Sinh PDF hoa don cho 1 don hang, tra ve mang byte de dinh kem email
     * hoac luu file / tra ve FE tai xuong.
     */
    public byte[] generateInvoicePdf(DonHang donHang) {
        try {
            String html = buildInvoiceHtml(donHang);
            return renderHtmlToPdf(html);
        } catch (IOException e) {
            log.error("Loi sinh PDF hoa don cho don hang HD-{}: {}", donHang.getId(), e.getMessage(), e);
            throw new RuntimeException("Khong the sinh PDF hoa don: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML → PDF: nạp font, build renderer, xuất byte[]
    // ─────────────────────────────────────────────────────────────────────
    private byte[] renderHtmlToPdf(String html) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Nhúng font Unicode để hiển thị tiếng Việt có dấu.
            // Mỗi lần renderer cần đọc font, supplier sẽ mở 1 InputStream mới
            // từ classpath (không dùng chung 1 stream đã đọc hết).
            builder.useFont(
                    (FSSupplier<InputStream>) () -> openFontStream("fonts/DejaVuSans.ttf"),
                    FONT_FAMILY, 400, FontStyle.NORMAL, true);
            builder.useFont(
                    (FSSupplier<InputStream>) () -> openFontStream("fonts/DejaVuSans-Bold.ttf"),
                    FONT_FAMILY, 700, FontStyle.NORMAL, true);

            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();
        }
    }

    private InputStream openFontStream(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Không tìm thấy font " + classpathLocation, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Build nội dung HTML của hóa đơn
    // ─────────────────────────────────────────────────────────────────────
    private String buildInvoiceHtml(DonHang donHang) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><head><meta charset=\"UTF-8\"/><style>").append(css()).append("</style></head><body>");

        sb.append("<h1>CHOCOPINE BAKERY</h1>");
        sb.append("<h2>HÓA ĐƠN BÁN HÀNG / XÁC NHẬN ĐẶT HÀNG</h2>");

        String maDonHang = "HD-" + donHang.getId();
        sb.append("<div class=\"info\">");
        sb.append("<p><b>Mã đơn hàng:</b> ").append(esc(maDonHang)).append("</p>");
        sb.append("<p><b>Ngày đặt:</b> ").append(esc(donHang.getNgayTao() != null
                ? donHang.getNgayTao().format(NGAY_GIO) : "—")).append("</p>");
        sb.append("<p><b>Trạng thái:</b> ").append(esc(donHang.getTrangThai().name())).append("</p>");
        sb.append("<p><b>Nguồn đơn:</b> ").append(esc("ONLINE".equals(donHang.getNguonDon())
                ? "Đặt hàng online" : donHang.getNguonDon())).append("</p>");
        sb.append("</div>");

        sb.append("<h3>Thông tin khách hàng</h3>");
        sb.append("<div class=\"info\">");
        NguoiDung kh = donHang.getKhachHang();
        if (kh != null) {
            sb.append("<p><b>Họ tên:</b> ").append(esc(nvl(kh.getHoTen()))).append("</p>");
            sb.append("<p><b>Email:</b> ").append(esc(nvl(kh.getEmail()))).append("</p>");
            sb.append("<p><b>Số điện thoại:</b> ").append(esc(nvl(kh.getSoDienThoai()))).append("</p>");
        }
        sb.append("<p><b>Địa chỉ giao hàng:</b> ").append(esc(nvl(donHang.getDiaChiGiao()))).append("</p>");
        if (donHang.getNgayGiaoDuKien() != null) {
            sb.append("<p><b>Ngày giao dự kiến:</b> ")
                    .append(esc(donHang.getNgayGiaoDuKien().toLocalDate().toString())).append("</p>");
        }
        sb.append("</div>");

        // ── Bảng sản phẩm ────────────────────────────────────────────────
        sb.append("<table class=\"items\">");
        sb.append("<thead><tr><th>Sản phẩm</th><th class=\"num\">SL</th><th class=\"num\">Đơn giá</th><th class=\"num\">Thành tiền</th></tr></thead>");
        sb.append("<tbody>");

        List<ChiTietDonHang> items = donHang.getChiTietDonHangs();
        BigDecimal tongTienHang = BigDecimal.ZERO;

        if (items != null) {
            for (ChiTietDonHang ct : items) {
                String ten = ct.getSanPham() != null ? ct.getSanPham().getTenSanPham() : "Sản phẩm";
                int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                BigDecimal donGia = ct.getDonGiaTaiThoiDiem() != null ? ct.getDonGiaTaiThoiDiem() : BigDecimal.ZERO;
                BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));
                tongTienHang = tongTienHang.add(thanhTien);

                sb.append("<tr>");
                sb.append("<td>").append(esc(truncate(ten, 60))).append("</td>");
                sb.append("<td class=\"num\">").append(soLuong).append("</td>");
                sb.append("<td class=\"num\">").append(formatTien(donGia)).append("</td>");
                sb.append("<td class=\"num\">").append(formatTien(thanhTien)).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // ── Tổng kết ─────────────────────────────────────────────────────
        BigDecimal tongThanhToan = donHang.getTongTien() != null ? donHang.getTongTien() : tongTienHang;

        sb.append("<div class=\"tong-ket\">");
        sb.append("<p>Tổng tiền hàng: ").append(formatTien(tongTienHang)).append(" đ</p>");

        if (donHang.getMaGiamGia() != null) {
            sb.append("<p>Mã giảm giá áp dụng: ").append(esc(donHang.getMaGiamGia().getMaCode())).append("</p>");
        } else if (donHang.getVoucherKhachHang() != null) {
            sb.append("<p>Voucher áp dụng: ")
                    .append(esc(donHang.getVoucherKhachHang().getTenVoucher())).append("</p>");
        }

        if (donHang.getSoTienCoc() != null && donHang.getSoTienCoc().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<p>Đã đặt cọc: ").append(formatTien(donHang.getSoTienCoc())).append(" đ</p>");
            sb.append("<p>Còn lại phải thanh toán: ")
                    .append(formatTien(tongThanhToan.subtract(donHang.getSoTienCoc()))).append(" đ</p>");
        }

        sb.append("<p class=\"tong-thanh-toan\">TỔNG THANH TOÁN: ").append(formatTien(tongThanhToan)).append(" đ</p>");
        sb.append("</div>");

        if (donHang.getGhiChu() != null && !donHang.getGhiChu().isBlank()) {
            sb.append("<h3>Ghi chú đơn hàng</h3>");
            sb.append("<p>").append(esc(truncate(donHang.getGhiChu(), 300))).append("</p>");
        }

        sb.append("<p class=\"footer\">Cảm ơn quý khách đã tin tưởng và ủng hộ Chocopine Bakery!</p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String css() {
        return "@page { size: A4; margin: 50px; } "
                + "body { font-family: '" + FONT_FAMILY + "'; font-size: 11px; color: #222; } "
                + "h1 { text-align: center; font-size: 18px; margin-bottom: 4px; } "
                + "h2 { text-align: center; font-size: 13px; margin-top: 0; margin-bottom: 20px; } "
                + "h3 { font-size: 12px; margin-bottom: 6px; border-bottom: 1px solid #ccc; padding-bottom: 4px; } "
                + "p { margin: 3px 0; } "
                + "table.items { width: 100%; border-collapse: collapse; margin: 12px 0; } "
                + "table.items th, table.items td { border: 1px solid #999; padding: 5px 8px; } "
                + "table.items th { background: #f2f2f2; text-align: left; } "
                + "table.items th.num, table.items td.num { text-align: right; } "
                + "div.tong-ket { text-align: right; margin-top: 10px; } "
                + "p.tong-thanh-toan { font-weight: bold; font-size: 13px; margin-top: 8px; } "
                + "p.footer { text-align: center; margin-top: 24px; font-size: 10px; color: #555; }";
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────
    private static String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String formatTien(BigDecimal tien) {
        return TIEN_FORMAT.format(tien == null ? BigDecimal.ZERO : tien);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    /** Escape các ký tự đặc biệt HTML để tránh vỡ layout khi dữ liệu chứa &lt; &gt; & "... */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}