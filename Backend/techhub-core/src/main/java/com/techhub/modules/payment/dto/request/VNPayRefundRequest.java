package com.techhub.modules.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record VNPayRefundRequest(
        @JsonProperty("vnp_RequestId") String vnpRequestId,
        @JsonProperty("vnp_Version") String vnpVersion,
        @JsonProperty("vnp_Command") String vnpCommand,
        @JsonProperty("vnp_TmnCode") String vnpTmnCode,
        @JsonProperty("vnp_TransactionType") String vnpTransactionType,
        @JsonProperty("vnp_TxnRef") String vnpTxnRef,
        @JsonProperty("vnp_Amount") long vnpAmount,
        @JsonProperty("vnp_OrderInfo") String vnpOrderInfo,
        @JsonProperty("vnp_BankCode") String vnpBankCode,
        @JsonProperty("vnp_TransactionNo") String vnpTransactionNo,
        @JsonProperty("vnp_TransactionDate") String vnpTransactionDate,
        @JsonProperty("vnp_CreateBy") String vnpCreateBy,
        @JsonProperty("vnp_CreateDate") String vnpCreateDate,
        @JsonProperty("vnp_IpAddr") String vnpIpAddr,
        @JsonProperty("vnp_SecureHash") String vnpSecureHash
) {}
