package com.techhub.service.voucher.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CartShopVoucherResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.ShopVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopVoucherServiceImpl implements ShopVoucherService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartShopVoucherResponse> getShopVouchersForCart(String sellerName, BigDecimal orderAmount) {
        if (sellerName == null || sellerName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Shop shop = resolveShop(sellerName);

        // BR-07: Shop Status Gatekeeper
        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            log.warn("Shop '{}' (id: {}) is SUSPENDED or BANNED. Denying voucher discovery", sellerName, shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.info("Shop '{}' (id: {}) is PAUSED. Returning empty voucher list", sellerName, shop.getId());
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findAvailableShopVouchers(shop.getId(), now);

        log.info("Found {} available vouchers for shop '{}' (id: {}) with orderAmount: {}",
                vouchers.size(), sellerName, shop.getId(), orderAmount);

        return vouchers.stream()
                .map(voucher -> CartShopVoucherResponse.from(voucher, orderAmount))
                .toList();
    }

    private Shop resolveShop(String identifier) {
        String trimmed = identifier.trim();

        // 1. Check if identifier is a valid UUID
        Optional<UUID> uuidOpt = tryParseUuid(trimmed);
        if (uuidOpt.isPresent()) {
            Shop shop = shopRepository.findById(uuidOpt.get())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));
            validateSellerAndUser(shop.getSeller());
            return shop;
        }

        // 2. Otherwise treat identifier as seller slug
        String normalizedSlug = trimmed.toLowerCase();
        if (normalizedSlug.length() < 3 || normalizedSlug.length() > 50 || !normalizedSlug.matches("^[a-z0-9-]+$")) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Seller seller = sellerRepository.findBySellerName(normalizedSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));
        validateSellerAndUser(seller);

        return shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));
    }

    private void validateSellerAndUser(Seller seller) {
        if (seller == null || seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        User user = seller.getUser();
        if (user != null && (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
    }

    private Optional<UUID> tryParseUuid(String value) {
        if (value == null || value.length() != 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
