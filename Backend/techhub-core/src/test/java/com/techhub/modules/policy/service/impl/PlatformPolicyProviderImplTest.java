package com.techhub.modules.policy.service.impl;

import com.techhub.modules.policy.entity.TransactionPolicy;
import com.techhub.modules.policy.repository.TransactionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho PlatformPolicyProviderImpl (Dynamic Policy Provider with Caching & Fallback)")
class PlatformPolicyProviderImplTest {

    @Mock
    private TransactionPolicyRepository transactionPolicyRepository;

    @InjectMocks
    private PlatformPolicyProviderImpl policyProvider;

    @BeforeEach
    void setUp() {
        policyProvider.evictAll();
    }

    @Test
    @DisplayName("getPlatformCommissionRate - Đọc từ CSDL và lưu đệm thành công")
    void getPlatformCommissionRate_FromDb_Success() {
        TransactionPolicy policy = TransactionPolicy.builder()
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.08")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(policy));

        BigDecimal rate1 = policyProvider.getPlatformCommissionRate();
        BigDecimal rate2 = policyProvider.getPlatformCommissionRate();

        assertEquals(new BigDecimal("0.08"), rate1);
        assertEquals(new BigDecimal("0.08"), rate2);

        // Chỉ query DB 1 lần duy nhất nhờ cơ chế Cache
        verify(transactionPolicyRepository, times(1)).findByPolicyKey("PLATFORM_COMMISSION_RATE");
    }

    @Test
    @DisplayName("getPlatformCommissionRate - Fallback 0.05 khi CSDL không có dữ liệu")
    void getPlatformCommissionRate_Fallback_WhenNotFound() {
        when(transactionPolicyRepository.findByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.empty());

        BigDecimal rate = policyProvider.getPlatformCommissionRate();

        assertEquals(PlatformPolicyProviderImpl.DEFAULT_COMMISSION_RATE, rate);
    }

    @Test
    @DisplayName("getPlatformCommissionRate - Fallback 0.05 khi CSDL chứa giá trị sai định dạng")
    void getPlatformCommissionRate_Fallback_WhenInvalidFormat() {
        TransactionPolicy invalidPolicy = TransactionPolicy.builder()
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("invalid_number")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(invalidPolicy));

        BigDecimal rate = policyProvider.getPlatformCommissionRate();

        assertEquals(PlatformPolicyProviderImpl.DEFAULT_COMMISSION_RATE, rate);
    }

    @Test
    @DisplayName("getVNPayPaymentTimeoutMinutes - Đọc từ CSDL và fallback chuẩn 15")
    void getVNPayPaymentTimeoutMinutes_SuccessAndFallback() {
        TransactionPolicy policy = TransactionPolicy.builder()
                .policyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES")
                .policyValue("30")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES"))
                .thenReturn(Optional.of(policy));

        int timeout = policyProvider.getVNPayPaymentTimeoutMinutes();
        assertEquals(30, timeout);

        policyProvider.evictCache("VNPAY_PAYMENT_TIMEOUT_MINUTES");

        when(transactionPolicyRepository.findByPolicyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES"))
                .thenReturn(Optional.empty());

        int fallbackTimeout = policyProvider.getVNPayPaymentTimeoutMinutes();
        assertEquals(PlatformPolicyProviderImpl.DEFAULT_VNPAY_TIMEOUT_MINUTES, fallbackTimeout);
    }

    @Test
    @DisplayName("getOrderAutoCompleteHours - Đọc từ CSDL và fallback chuẩn 72")
    void getOrderAutoCompleteHours_SuccessAndFallback() {
        TransactionPolicy policy = TransactionPolicy.builder()
                .policyKey("ORDER_AUTO_COMPLETE_HOURS")
                .policyValue("48")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("ORDER_AUTO_COMPLETE_HOURS"))
                .thenReturn(Optional.of(policy));

        int hours = policyProvider.getOrderAutoCompleteHours();
        assertEquals(48, hours);

        policyProvider.evictCache("ORDER_AUTO_COMPLETE_HOURS");

        when(transactionPolicyRepository.findByPolicyKey("ORDER_AUTO_COMPLETE_HOURS"))
                .thenReturn(Optional.empty());

        int fallbackHours = policyProvider.getOrderAutoCompleteHours();
        assertEquals(PlatformPolicyProviderImpl.DEFAULT_ORDER_AUTO_COMPLETE_HOURS, fallbackHours);
    }

    @Test
    @DisplayName("getReturnRequestWindowHours - Đọc từ CSDL và fallback chuẩn 72")
    void getReturnRequestWindowHours_SuccessAndFallback() {
        TransactionPolicy policy = TransactionPolicy.builder()
                .policyKey("RETURN_REQUEST_WINDOW_HOURS")
                .policyValue("120")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("RETURN_REQUEST_WINDOW_HOURS"))
                .thenReturn(Optional.of(policy));

        int hours = policyProvider.getReturnRequestWindowHours();
        assertEquals(120, hours);

        policyProvider.evictCache("RETURN_REQUEST_WINDOW_HOURS");

        when(transactionPolicyRepository.findByPolicyKey("RETURN_REQUEST_WINDOW_HOURS"))
                .thenReturn(Optional.empty());

        int fallbackHours = policyProvider.getReturnRequestWindowHours();
        assertEquals(PlatformPolicyProviderImpl.DEFAULT_RETURN_REQUEST_WINDOW_HOURS, fallbackHours);
    }

    @Test
    @DisplayName("getAutoApproveReturnHours - Đọc từ CSDL và fallback chuẩn 72")
    void getAutoApproveReturnHours_SuccessAndFallback() {
        TransactionPolicy policy = TransactionPolicy.builder()
                .policyKey("AUTO_APPROVE_RETURN_HOURS")
                .policyValue("96")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("AUTO_APPROVE_RETURN_HOURS"))
                .thenReturn(Optional.of(policy));

        int hours = policyProvider.getAutoApproveReturnHours();
        assertEquals(96, hours);

        policyProvider.evictCache("AUTO_APPROVE_RETURN_HOURS");

        when(transactionPolicyRepository.findByPolicyKey("AUTO_APPROVE_RETURN_HOURS"))
                .thenReturn(Optional.empty());

        int fallbackHours = policyProvider.getAutoApproveReturnHours();
        assertEquals(PlatformPolicyProviderImpl.DEFAULT_AUTO_APPROVE_RETURN_HOURS, fallbackHours);
    }

    @Test
    @DisplayName("evictCache - Xóa cache làm mới giá trị đọc ở lần tiếp theo")
    void evictCache_RefreshesOnNextCall() {
        TransactionPolicy initialPolicy = TransactionPolicy.builder()
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.05")
                .build();

        TransactionPolicy updatedPolicy = TransactionPolicy.builder()
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.10")
                .build();

        when(transactionPolicyRepository.findByPolicyKey("PLATFORM_COMMISSION_RATE"))
                .thenReturn(Optional.of(initialPolicy))
                .thenReturn(Optional.of(updatedPolicy));

        BigDecimal rate1 = policyProvider.getPlatformCommissionRate();
        assertEquals(new BigDecimal("0.05"), rate1);

        // Gọi evict cache
        policyProvider.evictCache("PLATFORM_COMMISSION_RATE");

        // Lần gọi tiếp theo đọc giá trị mới 0.10
        BigDecimal rate2 = policyProvider.getPlatformCommissionRate();
        assertEquals(new BigDecimal("0.10"), rate2);

        verify(transactionPolicyRepository, times(2)).findByPolicyKey("PLATFORM_COMMISSION_RATE");
    }
}
