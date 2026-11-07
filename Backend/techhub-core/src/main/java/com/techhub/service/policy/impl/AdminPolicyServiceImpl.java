package com.techhub.service.policy.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.policy.UpdatePlatformPolicyRequest;
import com.techhub.model.dto.response.policy.PlatformPolicyResponse;
import com.techhub.model.entity.TransactionPolicy;
import com.techhub.model.entity.User;
import com.techhub.repository.TransactionPolicyRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.policy.AdminPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPolicyServiceImpl implements AdminPolicyService {

    private final TransactionPolicyRepository transactionPolicyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<PlatformPolicyResponse> getAllPolicies() {
        log.info("Admin querying platform policies");

        List<TransactionPolicy> policies = transactionPolicyRepository.findAllWithUpdatedByOrderByPolicyKeyAsc();

        if (policies.isEmpty()) {
            log.info("Platform policies table is empty. Initializing default system policies...");
            List<TransactionPolicy> defaultPolicies = List.of(
                    TransactionPolicy.builder()
                            .policyKey("AUTO_APPROVE_RETURN_HOURS")
                            .policyValue("72")
                            .description("Thời gian tự động duyệt khiếu nại nếu Seller không phản hồi (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("ORDER_AUTO_COMPLETE_HOURS")
                            .policyValue("72")
                            .description("Thời gian SLA tự động kết chuyển đơn DELIVERED sang COMPLETED (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("PLATFORM_COMMISSION_RATE")
                            .policyValue("0.05")
                            .description("Tỷ lệ hoa hồng chiết khấu sàn áp dụng cho Seller (5%)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("RETURN_REQUEST_WINDOW_HOURS")
                            .policyValue("72")
                            .description("Thời hạn cho phép Buyer gửi khiếu nại Trả hàng/Hoàn tiền sau khi nhận hàng (giờ)")
                            .build(),
                    TransactionPolicy.builder()
                            .policyKey("VNPAY_PAYMENT_TIMEOUT_MINUTES")
                            .policyValue("15")
                            .description("Thời gian chờ thanh toán đơn hàng VNPAY trước khi tự hủy (phút)")
                            .build()
            );
            policies = transactionPolicyRepository.saveAll(defaultPolicies);
        }

        return policies.stream()
                .map(PlatformPolicyResponse::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformPolicyResponse updatePolicy(UUID adminId, UpdatePlatformPolicyRequest request) {
        String trimmedKey = request.policyKey().trim().toUpperCase();
        String trimmedValue = request.policyValue().trim();

        log.info("Admin {} updating policy {}: newValue={}, desc={}",
                adminId, trimmedKey, trimmedValue, request.description());

        validatePolicyValue(trimmedKey, trimmedValue);

        TransactionPolicy policy = transactionPolicyRepository.findForUpdateByPolicyKey(trimmedKey)
                .orElseThrow(() -> {
                    log.warn("Policy key {} not found in database", trimmedKey);
                    return new BusinessException(ErrorCode.POLICY_NOT_FOUND);
                });

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        policy.setPolicyValue(trimmedValue);
        if (request.description() != null && !request.description().trim().isEmpty()) {
            policy.setDescription(request.description().trim());
        }
        policy.setUpdatedBy(adminUser);

        TransactionPolicy saved = transactionPolicyRepository.save(policy);
        log.info("Policy {} successfully updated by admin {}", trimmedKey, adminId);

        return PlatformPolicyResponse.from(saved);
    }

    private void validatePolicyValue(String policyKey, String policyValue) {
        switch (policyKey) {
            case "PLATFORM_COMMISSION_RATE" -> {
                try {
                    BigDecimal rate = new BigDecimal(policyValue);
                    if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                        log.warn("Invalid PLATFORM_COMMISSION_RATE value: {}. Must be between 0.00 and 1.00", policyValue);
                        throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse PLATFORM_COMMISSION_RATE to BigDecimal: {}", policyValue);
                    throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                }
            }
            case "VNPAY_PAYMENT_TIMEOUT_MINUTES" -> {
                try {
                    int minutes = Integer.parseInt(policyValue);
                    if (minutes < 5 || minutes > 1440) {
                        log.warn("Invalid VNPAY_PAYMENT_TIMEOUT_MINUTES: {}. Must be between 5 and 1440 minutes", policyValue);
                        throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse VNPAY_PAYMENT_TIMEOUT_MINUTES to int: {}", policyValue);
                    throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                }
            }
            case "ORDER_AUTO_COMPLETE_HOURS" -> {
                try {
                    int hours = Integer.parseInt(policyValue);
                    if (hours < 12 || hours > 720) {
                        log.warn("Invalid ORDER_AUTO_COMPLETE_HOURS: {}. Must be between 12 and 720 hours", policyValue);
                        throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse ORDER_AUTO_COMPLETE_HOURS to int: {}", policyValue);
                    throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                }
            }
            case "RETURN_REQUEST_WINDOW_HOURS" -> {
                try {
                    int hours = Integer.parseInt(policyValue);
                    if (hours < 24 || hours > 720) {
                        log.warn("Invalid RETURN_REQUEST_WINDOW_HOURS: {}. Must be between 24 and 720 hours", policyValue);
                        throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse RETURN_REQUEST_WINDOW_HOURS to int: {}", policyValue);
                    throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                }
            }
            case "AUTO_APPROVE_RETURN_HOURS" -> {
                try {
                    int hours = Integer.parseInt(policyValue);
                    if (hours < 24 || hours > 720) {
                        log.warn("Invalid AUTO_APPROVE_RETURN_HOURS: {}. Must be between 24 and 720 hours", policyValue);
                        throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse AUTO_APPROVE_RETURN_HOURS to int: {}", policyValue);
                    throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
                }
            }
            default -> log.info("Validating generic policy key: {}", policyKey);
        }
    }
}
