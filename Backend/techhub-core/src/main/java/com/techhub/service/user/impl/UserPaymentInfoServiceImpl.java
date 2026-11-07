package com.techhub.service.user.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreatePaymentInfoRequest;
import com.techhub.model.dto.response.PaymentInfoResponse;
import com.techhub.model.entity.User;
import com.techhub.model.entity.UserPaymentInfo;
import com.techhub.model.enums.PaymentType;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserPaymentInfoRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.user.UserPaymentInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPaymentInfoServiceImpl implements UserPaymentInfoService {

    private static final int MAX_PAYMENT_INFO_PER_USER = 5;
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private final UserRepository userRepository;
    private final UserPaymentInfoRepository userPaymentInfoRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentInfoResponse createPaymentInfo(UUID userId, CreatePaymentInfoRequest request) {
        log.info("Creating payment info for userId={}, paymentType={}, provider='{}'",
                userId, request.paymentType(), request.provider());

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to add payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Sanitize Inputs
        String cleanAccountNumber = request.accountNumber().replaceAll("[^0-9a-zA-Z]", "").trim();
        if (cleanAccountNumber.length() < 6 || cleanAccountNumber.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        String normalizedProvider = request.provider().trim();
        String normalizedHolderName = (request.accountHolderName() != null)
                ? request.accountHolderName().trim().toUpperCase()
                : null;

        // 3. Check Capacity Limit (Max 5)
        long currentCount = userPaymentInfoRepository.countByUserId(userId);
        if (currentCount >= MAX_PAYMENT_INFO_PER_USER) {
            log.warn("Payment info capacity reached for userId={}: currentCount={}", userId, currentCount);
            throw new BusinessException(ErrorCode.PAYMENT_INFO_LIMIT_EXCEEDED);
        }

        // 4. Check Duplicate Account for same user and provider
        if (userPaymentInfoRepository.existsByUserIdAndProviderIgnoreCaseAndAccountNumber(
                userId, normalizedProvider, cleanAccountNumber)) {
            log.warn("Duplicate payment info detected: userId={}, provider='{}'", userId, normalizedProvider);
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_INFO);
        }

        // 5. Validate Card Expiry according to PaymentType
        String validatedCardExpiry = null;
        if (request.paymentType() == PaymentType.CREDIT_CARD) {
            if (request.cardExpiry() == null || request.cardExpiry().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
            }
            String expiry = request.cardExpiry().trim();
            if (!expiry.matches("^(0[1-9]|1[0-2])\\/\\d{4}$")) {
                throw new BusinessException(ErrorCode.INVALID_CARD_EXPIRY);
            }
            try {
                YearMonth expiryYearMonth = YearMonth.parse(expiry, EXPIRY_FORMATTER);
                if (expiryYearMonth.isBefore(YearMonth.now())) {
                    log.warn("Expired credit card rejected: userId={}, expiry='{}'", userId, expiry);
                    throw new BusinessException(ErrorCode.INVALID_CARD_EXPIRY);
                }
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ErrorCode.INVALID_CARD_EXPIRY);
            }
            validatedCardExpiry = expiry;
        }

        // 6. Resolve Default Status (Always reset atomically when willBeDefault is true)
        boolean willBeDefault = (currentCount == 0) || Boolean.TRUE.equals(request.isDefault());
        if (willBeDefault) {
            userPaymentInfoRepository.resetDefaultByUserId(userId);
        }

        // 7. Save Entity with Concurrency Duplicate Protection
        UserPaymentInfo paymentInfo = UserPaymentInfo.builder()
                .user(user)
                .paymentType(request.paymentType())
                .provider(normalizedProvider)
                .accountNumber(cleanAccountNumber)
                .accountHolderName(normalizedHolderName)
                .cardExpiry(validatedCardExpiry)
                .isDefault(willBeDefault)
                .build();

        UserPaymentInfo saved;
        try {
            saved = userPaymentInfoRepository.saveAndFlush(paymentInfo);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate payment info detected: userId={}, provider='{}'", userId, normalizedProvider);
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_INFO);
        }

        log.info("Payment info created successfully: id={}, userId={}, isDefault={}",
                saved.getId(), userId, saved.isDefault());

        return PaymentInfoResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentInfoResponse> getPaymentInfos(UUID userId) {
        log.info("Fetching payment infos for userId={}", userId);

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to fetch payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Query saved payment accounts (Default first, then newest first)
        List<UserPaymentInfo> paymentInfos = userPaymentInfoRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);

        // 3. Map to DTO with data masking
        return paymentInfos.stream()
                .map(PaymentInfoResponse::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentInfoResponse setDefaultPaymentInfo(UUID userId, UUID paymentInfoId) {
        log.info("Setting default payment info: userId={}, paymentInfoId={}", userId, paymentInfoId);

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to set default payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Validate Payment Info exists and belongs to current user (IDOR prevention)
        UserPaymentInfo paymentInfo = userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));

        // 3. If already default, return immediately (Idempotency)
        if (paymentInfo.isDefault()) {
            log.info("Payment info is already default: id={}, userId={}", paymentInfoId, userId);
            return PaymentInfoResponse.from(paymentInfo);
        }

        // 4. Atomically reset all existing defaults for this user
        userPaymentInfoRepository.resetDefaultByUserId(userId);

        // 5. Set this payment info as default and save
        paymentInfo.setDefault(true);
        UserPaymentInfo saved = userPaymentInfoRepository.save(paymentInfo);
        log.info("Payment info successfully set to default: id={}, userId={}", saved.getId(), userId);

        return PaymentInfoResponse.from(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaymentInfo(UUID userId, UUID paymentInfoId) {
        log.info("Deleting payment info: userId={}, paymentInfoId={}", userId, paymentInfoId);

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Attempt to delete payment info for non-active user: userId={}, status={}", userId, user.getStatus());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 2. Validate Payment Info exists and belongs to current user (IDOR prevention)
        UserPaymentInfo paymentInfo = userPaymentInfoRepository.findByIdAndUserId(paymentInfoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));

        // 3. Auto-promote remaining account if deleting default
        boolean wasDefault = paymentInfo.isDefault();
        if (wasDefault) {
            userPaymentInfoRepository.findFirstByUserIdAndIdNotOrderByCreatedAtDesc(userId, paymentInfoId)
                    .ifPresent(nextDefault -> {
                        log.info("Auto-promoting next payment info to default: id={}, userId={}", nextDefault.getId(), userId);
                        nextDefault.setDefault(true);
                        userPaymentInfoRepository.save(nextDefault);
                    });
        }

        // 4. Hard delete payment info
        userPaymentInfoRepository.delete(paymentInfo);
        log.info("Payment info deleted successfully: id={}, userId={}, wasDefault={}", paymentInfoId, userId, wasDefault);
    }
}
