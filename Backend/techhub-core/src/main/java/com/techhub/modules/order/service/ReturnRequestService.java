package com.techhub.modules.order.service;

import com.techhub.common.PageResponse;
import com.techhub.modules.order.dto.request.CreateReturnRequest;
import com.techhub.modules.order.dto.request.ProcessReturnRequest;
import com.techhub.modules.order.dto.response.ReturnRequestResponse;
import com.techhub.modules.order.enums.ReturnRequestStatus;

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
