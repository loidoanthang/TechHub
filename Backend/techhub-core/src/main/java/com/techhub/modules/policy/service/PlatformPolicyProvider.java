package com.techhub.modules.policy.service;

import java.math.BigDecimal;

public interface PlatformPolicyProvider {

    /**
     * Tỷ lệ hoa hồng sàn chiết khấu từ doanh thu sản phẩm của Seller.
     * Mặc định fallback: 0.05 (5%)
     */
    BigDecimal getPlatformCommissionRate();

    /**
     * Thời gian chờ thanh toán đơn hàng trực tuyến (VNPay) trước khi hết hạn (phút).
     * Mặc định fallback: 15 phút
     */
    int getVNPayPaymentTimeoutMinutes();

    /**
     * Thời gian SLA tự động kết chuyển đơn hàng từ DELIVERED sang COMPLETED (giờ).
     * Mặc định fallback: 72 giờ
     */
    int getOrderAutoCompleteHours();

    /**
     * Thời hạn tối đa cho phép Người mua tạo yêu cầu Trả hàng / Hoàn tiền sau khi nhận hàng (giờ).
     * Mặc định fallback: 72 giờ
     */
    int getReturnRequestWindowHours();

    /**
     * Thời hạn tối đa Người bán phải phản hồi khiếu nại trước khi hệ thống tự động duyệt hoàn tiền (giờ).
     * Mặc định fallback: 72 giờ
     */
    int getAutoApproveReturnHours();

    /**
     * Xóa cache bộ nhớ đệm của một policy cụ thể khi Admin cập nhật giá trị mới.
     */
    void evictCache(String policyKey);

    /**
     * Xóa toàn bộ cache bộ nhớ đệm.
     */
    void evictAll();
}
