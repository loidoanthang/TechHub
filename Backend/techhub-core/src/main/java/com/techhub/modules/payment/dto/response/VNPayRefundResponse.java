package com.techhub.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record VNPayRefundResponse(
        @JsonProperty("vnp_ResponseId") String vnpResponseId,
        @JsonProperty("vnp_Command") String vnpCommand,
        @JsonProperty("vnp_ResponseCode") String vnpResponseCode,
        @JsonProperty("vnp_Message") String vnpMessage,
        @JsonProperty("vnp_TmnCode") String vnpTmnCode,
        @JsonProperty("vnp_TxnRef") String vnpTxnRef,
        @JsonProperty("vnp_Amount") String vnpAmount,
        @JsonProperty("vnp_OrderInfo") String vnpOrderInfo,
        @JsonProperty("vnp_BankCode") String vnpBankCode,
        @JsonProperty("vnp_PayDate") String vnpPayDate,
        @JsonProperty("vnp_TransactionNo") String vnpTransactionNo,
        @JsonProperty("vnp_TransactionType") String vnpTransactionType,
        @JsonProperty("vnp_TransactionStatus") String vnpTransactionStatus,
        @JsonProperty("vnp_SecureHash") String vnpSecureHash
) {
    /**
     * Checks if VNPAY successfully accepted/processed the refund request.
     * vnp_ResponseCode = "00": Refund request approved/accepted by VNPAY.
     */
    public boolean isSuccess() {
        return "00".equals(vnpResponseCode);
    }
}
