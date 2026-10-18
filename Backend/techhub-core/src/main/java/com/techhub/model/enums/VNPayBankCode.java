package com.techhub.model.enums;

import java.util.Arrays;

public enum VNPayBankCode {
    VNPAYQR("Quét mã VNPAY-QR qua ứng dụng Mobile Banking hoặc Ví VNPAY"),
    VNBANK("Thẻ ATM / Tài khoản ngân hàng nội địa"),
    INTCARD("Thẻ thanh toán quốc tế Visa, MasterCard, JCB"),
    VNPAY("Ví điện tử VNPAY");

    private final String description;

    VNPayBankCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return true; // null hoặc rỗng biểu thị Cổng VNPAY tổng hợp
        }
        return Arrays.stream(values())
                .anyMatch(v -> v.name().equalsIgnoreCase(code.trim()));
    }

    public static VNPayBankCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (VNPayBankCode v : values()) {
            if (v.name().equalsIgnoreCase(code.trim())) {
                return v;
            }
        }
        throw new IllegalArgumentException("Mã ngân hàng / kênh VNPAY không hợp lệ: " + code);
    }
}
