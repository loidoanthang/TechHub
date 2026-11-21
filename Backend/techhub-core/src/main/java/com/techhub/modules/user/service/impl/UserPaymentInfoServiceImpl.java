package com.techhub.modules.user.service.impl;

import com.techhub.common.VNPayUtils;
import com.techhub.config.properties.VNPayProperties;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.payment.dto.response.PaymentInfoResponse;
import com.techhub.modules.payment.dto.response.VNPayIpnResponse;
import com.techhub.modules.payment.dto.response.VNPayTokenLinkResponse;
import com.techhub.modules.payment.enums.PaymentInfoStatus;
import com.techhub.modules.payment.enums.PaymentType;
import com.techhub.modules.payment.service.VNPayService;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.entity.UserPaymentInfo;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.user.repository.UserPaymentInfoRepository;
import com.techhub.modules.user.repository.UserRepository;
import com.techhub.modules.user.service.UserPaymentInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPaymentInfoServiceImpl implements UserPaymentInfoService {

    private static final int MAX_PAYMENT_INFO_PER_USER = 5;

    private final UserRepository userRepository;
    private final UserPaymentInfoRepository userPaymentInfoRepository;
    private final VNPayService vnPayService;
    private final VNPayProperties vnPayProperties;

    @Override
    @Transactional(readOnly = true)
    public VNPayTokenLinkResponse initiateTokenLink(UUID userId, String clientIp) {
        log.info("Initiating VNPAY card linking for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to link card for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        long currentCount = userPaymentInfoRepository.countByUserIdAndStatus(userId, PaymentInfoStatus.ACTIVE);
        if (currentCount >= MAX_PAYMENT_INFO_PER_USER) {
            log.warn("Payment info capacity reached for userId={}: currentCount={}", userId, currentCount);
            throw new BusinessException(ErrorCode.PAYMENT_INFO_LIMIT_EXCEEDED);
        }

        String txnRef = "LINK_" + userId.toString().substring(0, 8) + "_" + System.currentTimeMillis();
        String redirectUrl = vnPayService.createTokenLinkUrl(userId, txnRef, clientIp);

        return new VNPayTokenLinkResponse(redirectUrl, txnRef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VNPayIpnResponse processTokenCreateIpn(Map<String, String> queryParams) {
        log.info("Processing VNPAY Token Create IPN: {}", queryParams);

        if (!VNPayUtils.verifySignature(queryParams, vnPayProperties.getHashSecret())) {
            log.warn("VNPAY Token Create IPN: Invalid checksum");
            return VNPayIpnResponse.invalidChecksum();
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        if (!"00".equals(responseCode)) {
            log.warn("VNPAY Token Create IPN failed with responseCode: {}", responseCode);
            return VNPayIpnResponse.success();
        }

        String appUserId = queryParams.get("vnp_AppUserId");
        String token = queryParams.get("vnp_Token");
        String bankCode = queryParams.get("vnp_BankCode");
        String cardType = queryParams.get("vnp_CardType");
        String cardMask = queryParams.get("vnp_CardNumber");

        if (appUserId == null || token == null) {
            log.warn("VNPAY Token Create IPN: missing appUserId or token");
            return VNPayIpnResponse.orderNotFound();
        }

        UUID userId;
        try {
            userId = UUID.fromString(appUserId);
        } catch (Exception e) {
            log.warn("VNPAY Token Create IPN: malformed userId: {}", appUserId);
            return VNPayIpnResponse.orderNotFound();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<UserPaymentInfo> existingOpt = userPaymentInfoRepository.findByVnpToken(token);
        if (existingOpt.isPresent()) {
            UserPaymentInfo existing = existingOpt.get();
            existing.setStatus(PaymentInfoStatus.ACTIVE);
            userPaymentInfoRepository.save(existing);
            return VNPayIpnResponse.success();
        }

        long activeCount = userPaymentInfoRepository.countByUserIdAndStatus(userId, PaymentInfoStatus.ACTIVE);
        boolean isDefault = (activeCount == 0);

        UserPaymentInfo paymentInfo = UserPaymentInfo.builder()
                .user(user)
                .paymentType(PaymentType.BANK_ACCOUNT)
                .vnpToken(token)
                .bankCode(bankCode != null ? bankCode : "VNPAY")
                .cardMask(cardMask != null ? cardMask : "****")
                .cardType(cardType != null ? cardType : "ATM")
                .status(PaymentInfoStatus.ACTIVE)
                .isDefault(isDefault)
                .build();

        userPaymentInfoRepository.save(paymentInfo);
        log.info("Saved new VNPAY token for user {}: token={}", userId, token);
        return VNPayIpnResponse.success();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentInfoResponse> getPaymentInfos(UUID userId) {
        log.info("Fetching active payment infos for userId={}", userId);
        return userPaymentInfoRepository
                .findByUserIdAndStatusOrderByIsDefaultDescCreatedAtDesc(userId, PaymentInfoStatus.ACTIVE)
                .stream()
                .map(PaymentInfoResponse::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentInfoResponse setDefaultPaymentInfo(UUID userId, UUID paymentInfoId) {
        log.info("Setting default payment info: userId={}, paymentInfoId={}", userId, paymentInfoId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to set default payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserPaymentInfo paymentInfo = userPaymentInfoRepository.findByIdAndUserIdAndStatus(paymentInfoId, userId, PaymentInfoStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));

        if (paymentInfo.isDefault()) {
            return PaymentInfoResponse.from(paymentInfo);
        }

        userPaymentInfoRepository.resetDefaultByUserId(userId);
        paymentInfo.setDefault(true);
        UserPaymentInfo saved = userPaymentInfoRepository.save(paymentInfo);

        return PaymentInfoResponse.from(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaymentInfo(UUID userId, UUID paymentInfoId) {
        log.info("Removing payment info: userId={}, paymentInfoId={}", userId, paymentInfoId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to delete payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserPaymentInfo paymentInfo = userPaymentInfoRepository.findByIdAndUserIdAndStatus(paymentInfoId, userId, PaymentInfoStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));

        boolean wasDefault = paymentInfo.isDefault();

        // 1. Mark as REMOVED locally
        paymentInfo.setStatus(PaymentInfoStatus.REMOVED);
        paymentInfo.setDefault(false);
        userPaymentInfoRepository.save(paymentInfo);

        // 2. Call VNPAY gateway to remove token
        if (paymentInfo.getVnpToken() != null) {
            try {
                vnPayService.removeToken(userId, paymentInfo.getVnpToken(), "127.0.0.1");
            } catch (Exception e) {
                log.warn("Failed to notify VNPAY token removal: {}", e.getMessage());
            }
        }

        // 3. Promote another active card as default if wasDefault
        if (wasDefault) {
            userPaymentInfoRepository.findFirstByUserIdAndIdNotAndStatusOrderByCreatedAtDesc(userId, paymentInfoId, PaymentInfoStatus.ACTIVE)
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefault(true);
                        userPaymentInfoRepository.save(nextDefault);
                        log.info("Promoted payment info {} to new default for userId={}", nextDefault.getId(), userId);
                    });
        }
    }
}
