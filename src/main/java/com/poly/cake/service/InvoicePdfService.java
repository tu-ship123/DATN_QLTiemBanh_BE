package com.poly.cake.service;

import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
 * T072 – Sinh file PDF hóa đơn bán hàng để đính kèm email xác nhận đặt hàng.
 * <p>
 * Dùng Apache PDFBox (org.apache.pdfbox:pdfbox) thay vì iText vì PDFBox là
 * Apache License 2.0 (miễn phí hoàn toàn cho mục đích thương mại), khác với
 * iText 5 là AGPL (bắt buộc phải mua license nếu dùng cho sản phẩm đóng nguồn).
 * <p>
 * QUAN TRỌNG - PDFBox không có font hỗ trợ tiếng Việt có dấu sẵn (font chuẩn
 * Helvetica/Times chỉ có bảng mã Latin cơ bản), nên bắt buộc phải nhúng
 * (embed) một font Unicode TTF. Ở đây dùng font DejaVu Sans (SIL Open Font
 * License - miễn phí) đặt sẵn tại: src/main/resources/fonts/DejaVuSans.ttf
 * và DejaVuSans-Bold.ttf.
 * <p>
 * ⚠️ CẦN THÊM DEPENDENCY VÀO pom.xml (project gửi lên không có sẵn pom.xml
 * nên phải tự thêm thủ công):
 * <pre>{@code
 * <dependency>
 *     <groupId>org.apache.pdfbox</groupId>
 *     <artifactId>pdfbox</artifactId>
 *     <version>2.0.31</version>
 * </dependency>
 * }</pre>
 */
@Slf4j
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat TIEN_FORMAT = new DecimalFormat("#,##0");

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    // Vị trí các cột trong bảng sản phẩm
    private static final float COL_TEN = MARGIN;
    private static final float COL_SL = MARGIN + 270;
    private static final float COL_DON_GIA = MARGIN + 330;
    private static final float COL_THANH_TIEN = MARGIN + 430;

    /**
     * Sinh PDF hóa đơn cho 1 đơn hàng, trả về mảng byte để đính kèm email
     * hoặc lưu file / trả về FE tải xuống.
     */
    public byte[] generateInvoicePdf(DonHang donHang) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontRegular = loadFont(document, "fonts/DejaVuSans.ttf");
            PDFont fontBold = loadFont(document, "fonts/DejaVuSans-Bold.ttf");

            Trang trang = new Trang(document);

            // ── Tiêu đề ──────────────────────────────────────────────────
            trang.writeCentered(fontBold, 18, "CHOCOPINE BAKERY");
            trang.y -= 22;
            trang.writeCentered(fontBold, 13, "HÓA ĐƠN BÁN HÀNG / XÁC NHẬN ĐẶT HÀNG");
            trang.y -= 28;

            String maDonHang = "HD-" + donHang.getId();
            trang.writeLeft(fontRegular, 11, "Mã đơn hàng: " + maDonHang);
            trang.writeLeft(fontRegular, 11, "Ngày đặt: "
                    + (donHang.getNgayTao() != null ? donHang.getNgayTao().format(NGAY_GIO) : "—"));
            trang.writeLeft(fontRegular, 11, "Trạng thái: " + donHang.getTrangThai().name());
            trang.writeLeft(fontRegular, 11, "Nguồn đơn: "
                    + ("ONLINE".equals(donHang.getNguonDon()) ? "Đặt hàng online" : donHang.getNguonDon()));
            trang.y -= 14;

            // ── Thông tin khách hàng ─────────────────────────────────────
            trang.writeLeft(fontBold, 12, "Thông tin khách hàng");
            NguoiDung kh = donHang.getKhachHang();
            if (kh != null) {
                trang.writeLeft(fontRegular, 11, "Họ tên: " + nvl(kh.getHoTen()));
                trang.writeLeft(fontRegular, 11, "Email: " + nvl(kh.getEmail()));
                trang.writeLeft(fontRegular, 11, "Số điện thoại: " + nvl(kh.getSoDienThoai()));
            }
            trang.writeLeft(fontRegular, 11, "Địa chỉ giao hàng: " + nvl(donHang.getDiaChiGiao()));
            if (donHang.getNgayGiaoDuKien() != null) {
                trang.writeLeft(fontRegular, 11,
                        "Ngày giao dự kiến: " + donHang.getNgayGiaoDuKien().toLocalDate());
            }
            trang.y -= 18;

            // ── Bảng sản phẩm ────────────────────────────────────────────
            trang.checkPageBreak(60);
            trang.drawTableHeader(fontBold);

            List<ChiTietDonHang> items = donHang.getChiTietDonHangs();
            BigDecimal tongTienHang = BigDecimal.ZERO;

            if (items != null) {
                for (ChiTietDonHang ct : items) {
                    trang.checkPageBreak(25);

                    String ten = ct.getSanPham() != null ? ct.getSanPham().getTenSanPham() : "Sản phẩm";
                    int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 0;
                    BigDecimal donGia = ct.getDonGiaTaiThoiDiem() != null ? ct.getDonGiaTaiThoiDiem() : BigDecimal.ZERO;
                    BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));
                    tongTienHang = tongTienHang.add(thanhTien);

                    trang.drawTableRow(fontRegular, truncate(ten, 40), String.valueOf(soLuong),
                            formatTien(donGia), formatTien(thanhTien));
                }
            }

            trang.y -= 6;
            trang.drawLine();
            trang.y -= 20;

            // ── Tổng kết ─────────────────────────────────────────────────
            trang.checkPageBreak(100);

            BigDecimal tongThanhToan = donHang.getTongTien() != null ? donHang.getTongTien() : tongTienHang;

            trang.writeRight(fontRegular, 11, "Tổng tiền hàng: " + formatTien(tongTienHang) + " đ");

            if (donHang.getMaGiamGia() != null) {
                trang.writeRight(fontRegular, 11,
                        "Mã giảm giá áp dụng: " + donHang.getMaGiamGia().getMaCode());
            } else if (donHang.getVoucherKhachHang() != null) {
                trang.writeRight(fontRegular, 11,
                        "Voucher áp dụng: " + donHang.getVoucherKhachHang().getTenVoucher());
            }

            if (donHang.getSoTienCoc() != null && donHang.getSoTienCoc().compareTo(BigDecimal.ZERO) > 0) {
                trang.writeRight(fontRegular, 11, "Đã đặt cọc: " + formatTien(donHang.getSoTienCoc()) + " đ");
                trang.writeRight(fontRegular, 11,
                        "Còn lại phải thanh toán: " + formatTien(tongThanhToan.subtract(donHang.getSoTienCoc())) + " đ");
            }

            trang.writeRight(fontBold, 13, "TỔNG THANH TOÁN: " + formatTien(tongThanhToan) + " đ");
            trang.y -= 20;

            if (donHang.getGhiChu() != null && !donHang.getGhiChu().isBlank()) {
                trang.checkPageBreak(40);
                trang.writeLeft(fontBold, 11, "Ghi chú đơn hàng:");
                trang.writeLeft(fontRegular, 10, truncate(donHang.getGhiChu(), 110));
            }

            trang.y -= 20;
            trang.checkPageBreak(30);
            trang.writeCentered(fontRegular, 10,
                    "Cảm ơn quý khách đã tin tưởng và ủng hộ Chocopine Bakery! 🍰");

            trang.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi sinh PDF hóa đơn cho đơn hàng HD-{}: {}", donHang.getId(), e.getMessage(), e);
            throw new RuntimeException("Không thể sinh PDF hóa đơn: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER: Nạp font TTF Unicode từ resources/fonts để nhúng vào PDF
    // ─────────────────────────────────────────────────────────────────────
    private PDFont loadFont(PDDocument document, String classpathLocation) throws IOException {
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return PDType0Font.load(document, is);
        }
    }

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

    /**
     * Lớp nội bộ quản lý "con trỏ" vẽ trên trang PDF hiện tại: vị trí Y, tự
     * động ngắt trang mới khi hết chỗ (checkPageBreak), tự mở/đóng
     * PDPageContentStream tương ứng khi sang trang.
     */
    private static class Trang {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        Trang(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        void checkPageBreak(float neededSpace) throws IOException {
            if (y - neededSpace < MARGIN) {
                stream.close();
                newPage();
            }
        }

        void writeLeft(PDFont font, float size, String text) throws IOException {
            checkPageBreak(18);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtPosition(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= (size + 6);
        }

        void writeRight(PDFont font, float size, String text) throws IOException {
            checkPageBreak(18);
            float textWidth = font.getStringWidth(text) / 1000 * size;
            float x = PAGE_WIDTH - MARGIN - textWidth;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtPosition(x, y);
            stream.showText(text);
            stream.endText();
            y -= (size + 6);
        }

        void writeCentered(PDFont font, float size, String text) throws IOException {
            checkPageBreak(18);
            float textWidth = font.getStringWidth(text) / 1000 * size;
            float x = (PAGE_WIDTH - textWidth) / 2;
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtPosition(x, y);
            stream.showText(text);
            stream.endText();
            y -= (size + 6);
        }

        void drawLine() throws IOException {
            stream.moveTo(MARGIN, y);
            stream.lineTo(PAGE_WIDTH - MARGIN, y);
            stream.stroke();
        }

        void drawTableHeader(PDFont fontBold) throws IOException {
            stream.beginText();
            stream.setFont(fontBold, 11);
            stream.newLineAtPosition(COL_TEN, y);
            stream.showText("Sản phẩm");
            stream.endText();

            stream.beginText();
            stream.setFont(fontBold, 11);
            stream.newLineAtPosition(COL_SL, y);
            stream.showText("SL");
            stream.endText();

            stream.beginText();
            stream.setFont(fontBold, 11);
            stream.newLineAtPosition(COL_DON_GIA, y);
            stream.showText("Đơn giá");
            stream.endText();

            stream.beginText();
            stream.setFont(fontBold, 11);
            stream.newLineAtPosition(COL_THANH_TIEN, y);
            stream.showText("Thành tiền");
            stream.endText();

            y -= 8;
            drawLine();
            y -= 18;
        }

        void drawTableRow(PDFont font, String ten, String soLuong, String donGia, String thanhTien) throws IOException {
            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtPosition(COL_TEN, y);
            stream.showText(ten);
            stream.endText();

            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtPosition(COL_SL, y);
            stream.showText(soLuong);
            stream.endText();

            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtPosition(COL_DON_GIA, y);
            stream.showText(donGia);
            stream.endText();

            stream.beginText();
            stream.setFont(font, 10);
            stream.newLineAtPosition(COL_THANH_TIEN, y);
            stream.showText(thanhTien);
            stream.endText();

            y -= 20;
        }

        void close() throws IOException {
            stream.close();
        }
    }
}
