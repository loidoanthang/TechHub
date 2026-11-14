package com.techhub.service.voucher;

import com.techhub.modules.voucher.dto.response.CartShopVoucherResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ShopVoucherService {

    List<CartShopVoucherResponse> getShopVouchersForCart(String sellerName, BigDecimal orderAmount);
}
