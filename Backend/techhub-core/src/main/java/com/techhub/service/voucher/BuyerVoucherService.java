package com.techhub.service.voucher;

import com.techhub.model.dto.response.CartPlatformVoucherResponse;
import com.techhub.model.enums.VoucherAppliesTo;

import java.math.BigDecimal;
import java.util.List;

public interface BuyerVoucherService {

    List<CartPlatformVoucherResponse> getPlatformVouchersForCart(
            BigDecimal orderAmount,
            BigDecimal shippingFee,
            VoucherAppliesTo appliesTo
    );
}
