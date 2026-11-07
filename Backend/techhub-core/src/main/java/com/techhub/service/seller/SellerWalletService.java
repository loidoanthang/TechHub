package com.techhub.service.seller;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.payout.WithdrawalRequest;
import com.techhub.model.dto.response.payout.PayoutResponse;
import com.techhub.model.dto.response.wallet.SellerWalletTransactionResponse;
import com.techhub.model.enums.SellerWalletTransactionType;

import java.time.LocalDate;
import java.util.UUID;

public interface SellerWalletService {

    PayoutResponse requestWithdrawal(UUID currentUserId, WithdrawalRequest request);

    PageResponse<SellerWalletTransactionResponse> getWalletTransactions(
            UUID currentUserId,
            SellerWalletTransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String direction
    );
}
