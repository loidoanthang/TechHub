package com.techhub.modules.voucher.service.impl;

import com.techhub.modules.voucher.dto.response.CartPlatformVoucherResponse;
import com.techhub.modules.voucher.entity.Voucher;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;
import com.techhub.modules.voucher.repository.VoucherRepository;
import com.techhub.modules.voucher.service.BuyerVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.techhub.modules.voucher.service.BuyerVoucherService;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyerVoucherServiceImpl implements BuyerVoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartPlatformVoucherResponse> getPlatformVouchersForCart(
            BigDecimal orderAmount,
            BigDecimal shippingFee,
            VoucherAppliesTo appliesTo
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findAvailablePlatformVouchers(appliesTo, now);

        log.info("Found {} available platform vouchers with orderAmount: {}, shippingFee: {}, appliesTo: {}",
                vouchers.size(), orderAmount, shippingFee, appliesTo);

        return vouchers.stream()
                .map(voucher -> CartPlatformVoucherResponse.from(voucher, orderAmount, shippingFee))
                .toList();
    }
}
