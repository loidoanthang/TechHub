package com.techhub.service.seller;

import com.techhub.common.PageResponse;
import com.techhub.modules.seller.dto.request.SellerApprovalRequest;
import com.techhub.modules.seller.dto.response.AdminSellerApprovalResponse;
import com.techhub.modules.seller.dto.response.AdminSellerDetailResponse;
import com.techhub.modules.seller.dto.response.AdminSellerResponse;
import com.techhub.modules.seller.enums.SellerVerificationStatus;

import java.util.UUID;

public interface AdminSellerService {

    PageResponse<AdminSellerResponse> getSellers(
            String keyword,
            SellerVerificationStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminSellerDetailResponse getSellerDetail(UUID id);

    AdminSellerApprovalResponse updateSellerApproval(UUID id, SellerApprovalRequest request);
}

