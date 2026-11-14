package com.techhub.service.payout;

import com.techhub.common.PageResponse;
import com.techhub.modules.seller.dto.request.ProcessPayoutRequest;
import com.techhub.modules.seller.dto.response.AdminPayoutResponse;
import com.techhub.model.enums.PayoutStatus;

import java.time.LocalDate;
import java.util.UUID;

public interface AdminPayoutService {

    PageResponse<AdminPayoutResponse> getPayoutRequests(
            PayoutStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String direction
    );

    AdminPayoutResponse processPayoutRequest(UUID id, UUID adminId, ProcessPayoutRequest request);
}
