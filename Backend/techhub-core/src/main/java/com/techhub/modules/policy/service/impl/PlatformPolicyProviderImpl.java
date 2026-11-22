package com.techhub.modules.policy.service.impl;

import com.techhub.modules.policy.entity.TransactionPolicy;
import com.techhub.modules.policy.repository.TransactionPolicyRepository;
import com.techhub.modules.policy.service.PlatformPolicyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformPolicyProviderImpl implements PlatformPolicyProvider {

    public static final String KEY_COMMISSION_RATE = "PLATFORM_COMMISSION_RATE";
    public static final String KEY_VNPAY_TIMEOUT = "VNPAY_PAYMENT_TIMEOUT_MINUTES";
    public static final String KEY_ORDER_AUTO_COMPLETE = "ORDER_AUTO_COMPLETE_HOURS";
    public static final String KEY_RETURN_WINDOW = "RETURN_REQUEST_WINDOW_HOURS";
    public static final String KEY_AUTO_APPROVE_RETURN = "AUTO_APPROVE_RETURN_HOURS";

    public static final BigDecimal DEFAULT_COMMISSION_RATE = BigDecimal.valueOf(0.05);
    public static final int DEFAULT_VNPAY_TIMEOUT_MINUTES = 15;
    public static final int DEFAULT_ORDER_AUTO_COMPLETE_HOURS = 72;
    public static final int DEFAULT_RETURN_REQUEST_WINDOW_HOURS = 72;
    public static final int DEFAULT_AUTO_APPROVE_RETURN_HOURS = 72;

    private final TransactionPolicyRepository transactionPolicyRepository;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getPlatformCommissionRate() {
        Object cached = cache.get(KEY_COMMISSION_RATE);
        if (cached instanceof BigDecimal rate) {
            return rate;
        }

        try {
            BigDecimal rate = transactionPolicyRepository.findByPolicyKey(KEY_COMMISSION_RATE)
                    .map(TransactionPolicy::getPolicyValue)
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .orElse(DEFAULT_COMMISSION_RATE);

            cache.put(KEY_COMMISSION_RATE, rate);
            return rate;
        } catch (Exception e) {
            log.warn("Failed to load {} from database, falling back to default {}: {}",
                    KEY_COMMISSION_RATE, DEFAULT_COMMISSION_RATE, e.getMessage());
            return DEFAULT_COMMISSION_RATE;
        }
    }

    @Override
    public int getVNPayPaymentTimeoutMinutes() {
        Object cached = cache.get(KEY_VNPAY_TIMEOUT);
        if (cached instanceof Integer timeout) {
            return timeout;
        }

        try {
            int timeout = transactionPolicyRepository.findByPolicyKey(KEY_VNPAY_TIMEOUT)
                    .map(TransactionPolicy::getPolicyValue)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_VNPAY_TIMEOUT_MINUTES);

            cache.put(KEY_VNPAY_TIMEOUT, timeout);
            return timeout;
        } catch (Exception e) {
            log.warn("Failed to load {} from database, falling back to default {}: {}",
                    KEY_VNPAY_TIMEOUT, DEFAULT_VNPAY_TIMEOUT_MINUTES, e.getMessage());
            return DEFAULT_VNPAY_TIMEOUT_MINUTES;
        }
    }

    @Override
    public int getOrderAutoCompleteHours() {
        Object cached = cache.get(KEY_ORDER_AUTO_COMPLETE);
        if (cached instanceof Integer hours) {
            return hours;
        }

        try {
            int hours = transactionPolicyRepository.findByPolicyKey(KEY_ORDER_AUTO_COMPLETE)
                    .map(TransactionPolicy::getPolicyValue)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_ORDER_AUTO_COMPLETE_HOURS);

            cache.put(KEY_ORDER_AUTO_COMPLETE, hours);
            return hours;
        } catch (Exception e) {
            log.warn("Failed to load {} from database, falling back to default {}: {}",
                    KEY_ORDER_AUTO_COMPLETE, DEFAULT_ORDER_AUTO_COMPLETE_HOURS, e.getMessage());
            return DEFAULT_ORDER_AUTO_COMPLETE_HOURS;
        }
    }

    @Override
    public int getReturnRequestWindowHours() {
        Object cached = cache.get(KEY_RETURN_WINDOW);
        if (cached instanceof Integer hours) {
            return hours;
        }

        try {
            int hours = transactionPolicyRepository.findByPolicyKey(KEY_RETURN_WINDOW)
                    .map(TransactionPolicy::getPolicyValue)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_RETURN_REQUEST_WINDOW_HOURS);

            cache.put(KEY_RETURN_WINDOW, hours);
            return hours;
        } catch (Exception e) {
            log.warn("Failed to load {} from database, falling back to default {}: {}",
                    KEY_RETURN_WINDOW, DEFAULT_RETURN_REQUEST_WINDOW_HOURS, e.getMessage());
            return DEFAULT_RETURN_REQUEST_WINDOW_HOURS;
        }
    }

    @Override
    public int getAutoApproveReturnHours() {
        Object cached = cache.get(KEY_AUTO_APPROVE_RETURN);
        if (cached instanceof Integer hours) {
            return hours;
        }

        try {
            int hours = transactionPolicyRepository.findByPolicyKey(KEY_AUTO_APPROVE_RETURN)
                    .map(TransactionPolicy::getPolicyValue)
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_AUTO_APPROVE_RETURN_HOURS);

            cache.put(KEY_AUTO_APPROVE_RETURN, hours);
            return hours;
        } catch (Exception e) {
            log.warn("Failed to load {} from database, falling back to default {}: {}",
                    KEY_AUTO_APPROVE_RETURN, DEFAULT_AUTO_APPROVE_RETURN_HOURS, e.getMessage());
            return DEFAULT_AUTO_APPROVE_RETURN_HOURS;
        }
    }

    @Override
    public void evictCache(String policyKey) {
        if (policyKey == null) {
            return;
        }
        String normalizedKey = policyKey.trim().toUpperCase();
        cache.remove(normalizedKey);
        log.info("Evicted platform policy cache for key: [{}]", normalizedKey);
    }

    @Override
    public void evictAll() {
        cache.clear();
        log.info("Evicted all platform policy caches");
    }
}
