package com.techhub.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record VNPayQueryDrResponse(
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
        @JsonProperty("vnp_PromotionCode") String vnpPromotionCode,
        @JsonProperty("vnp_PromotionAmount") String vnpPromotionAmount,
        @JsonProperty("vnp_SecureHash") String vnpSecureHash
) {
    /**
     * Checks if the transaction was completely successful on VNPAY.
     * vnp_ResponseCode = "00": WebAPI call processed successfully
     * vnp_TransactionStatus = "00": Customer transaction completed successfully
     */
    public boolean isSuccess() {
        return "00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus);
    }
}
