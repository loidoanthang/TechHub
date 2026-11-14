package com.techhub.service.seller;

import com.techhub.common.PageResponse;
import com.techhub.modules.seller.dto.request.WithdrawalRequest;
import com.techhub.modules.seller.dto.response.PayoutResponse;
import com.techhub.modules.seller.dto.response.SellerWalletTransactionResponse;
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
