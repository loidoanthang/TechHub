package com.techhub.modules.payment.dto.response;

public record VNPayTokenLinkResponse(
        String redirectUrl,
        String txnRef
) {
}
