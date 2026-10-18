package com.techhub.service.order;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreateReturnRequest;
import com.techhub.model.dto.request.ProcessReturnRequest;
import com.techhub.model.dto.response.ReturnRequestResponse;
import com.techhub.model.enums.ReturnRequestStatus;

import java.util.UUID;

public interface ReturnRequestService {

    ReturnRequestResponse createReturnRequest(UUID userId, UUID orderId, CreateReturnRequest request);

    ReturnRequestResponse cancelReturnRequest(UUID userId, UUID returnRequestId);

    ReturnRequestResponse getBuyerReturnRequest(UUID userId, UUID returnRequestId);

    ReturnRequestResponse getBuyerReturnRequestByOrderId(UUID userId, UUID orderId);

    PageResponse<ReturnRequestResponse> getSellerReturnRequests(UUID userId, ReturnRequestStatus status, int page, int size);

    ReturnRequestResponse getSellerReturnRequestDetail(UUID userId, UUID returnRequestId);

    ReturnRequestResponse processReturnRequest(UUID userId, UUID returnRequestId, ProcessReturnRequest request);
}
