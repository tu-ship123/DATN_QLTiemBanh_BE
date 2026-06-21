package com.poly.cake.dto;

import lombok.Data;

@Data
public class SePayWebhookDto {
    private Long id;
    private String gateway;           // Tên ngân hàng (MBBank, VCB...)
    private String transactionDate;   // Ngày giờ giao dịch
    private String accountNumber;     // Số tài khoản nhận
    private String code;              // Mã giao dịch ngân hàng
    private String content;           // Nội dung chuyển khoản (Ví dụ: "PC1234 thanh toan")
    private String transferType;      // Loại giao dịch ("in" là nhận tiền, "out" là rút tiền)
    private Double transferAmount;    // Số tiền khách chuyển
    private Double accumulated;       // Số dư
    private String subAccountCode;
    private String referenceCode;
    private String description;
}