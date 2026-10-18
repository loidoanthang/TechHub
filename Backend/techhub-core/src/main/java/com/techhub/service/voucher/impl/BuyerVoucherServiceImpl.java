package com.techhub.service.voucher.impl;

import com.techhub.model.dto.response.CartPlatformVoucherResponse;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.BuyerVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
