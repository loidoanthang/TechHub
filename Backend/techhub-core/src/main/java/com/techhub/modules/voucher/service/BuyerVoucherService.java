package com.techhub.modules.voucher.service;

import com.techhub.modules.voucher.dto.response.CartPlatformVoucherResponse;
import com.techhub.modules.voucher.enums.VoucherAppliesTo;

import java.math.BigDecimal;
import java.util.List;

public interface BuyerVoucherService {

    List<CartPlatformVoucherResponse> getPlatformVouchersForCart(
            BigDecimal orderAmount,
            BigDecimal shippingFee,
            VoucherAppliesTo appliesTo
    );
}
