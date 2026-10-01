package com.techhub.service.admin;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.SellerApprovalRequest;
import com.techhub.model.dto.response.AdminSellerApprovalResponse;
import com.techhub.model.dto.response.AdminSellerDetailResponse;
import com.techhub.model.dto.response.AdminSellerResponse;
import com.techhub.model.enums.SellerVerificationStatus;

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

