package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.review.ProductRatingSummaryResponse;
import com.techhub.model.dto.response.review.ProductReviewResponse;
import com.techhub.service.review.ProductReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ProductReviewController (GET /api/products/{id}/reviews)")
class ProductReviewControllerTest {

    @Mock
    private ProductReviewService productReviewService;

    @InjectMocks
    private ProductReviewController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-89: controller_getProductReviews_PermitAll_PublicAccess - Cho phép truy cập công khai không yêu cầu token")
    void controller_getProductReviews_PermitAll_PublicAccess() throws NoSuchMethodException {
        Method method = ProductReviewController.class.getMethod("getProductReviews",
                UUID.class, Integer.class, Boolean.class, String.class, String.class, int.class, int.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNull(preAuthorize, "Method getProductReviews phải cho phép truy cập công khai (không có @PreAuthorize)");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getProductReviews phải có annotation @GetMapping");
        assertArrayEquals(new String[]{"/{id}/reviews"}, getMapping.value());

        RequestMapping classMapping = ProductReviewController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/products"}, classMapping.value());
    }

    @Test
    @DisplayName("TC-90: controller_getProductReviews_Success_Returns200 - Trả về HTTP 200 OK kèm PageResponse")
    void controller_getProductReviews_Success_Returns200() {
        UUID productId = UUID.randomUUID();
        PageResponse<ProductReviewResponse> mockPageResponse = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0, 0, true
        );

        when(productReviewService.getProductReviews(eq(productId), any(), any(), any(), any(), any(Sort.Direction.class), any(Pageable.class)))
                .thenReturn(mockPageResponse);

        ApiResponse<PageResponse<ProductReviewResponse>> response = controller.getProductReviews(
                productId, 5, true, "createdAt", "DESC", 0, 10
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product reviews retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(0, response.getData().totalElements());

        verify(productReviewService, times(1)).getProductReviews(
                eq(productId), any(), eq(5), eq(true), eq("createdAt"), eq(Sort.Direction.DESC), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("TC-104: controller_getRatingSummary_PermitAll_PublicAccess - Endpoint cho phép truy cập công khai không yêu cầu token")
    void controller_getRatingSummary_PermitAll_PublicAccess() throws NoSuchMethodException {
        Method method = ProductReviewController.class.getMethod("getProductRatingSummary", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNull(preAuthorize, "Method getProductRatingSummary phải cho phép truy cập công khai (không có @PreAuthorize)");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getProductRatingSummary phải có annotation @GetMapping");
        assertArrayEquals(new String[]{"/{id}/rating-summary"}, getMapping.value());
    }

    @Test
    @DisplayName("TC-105: controller_getRatingSummary_Success_Returns200 - Trả về HTTP 200 OK kèm ProductRatingSummaryResponse")
    void controller_getRatingSummary_Success_Returns200() {
        UUID productId = UUID.randomUUID();
        ProductRatingSummaryResponse mockSummary = ProductRatingSummaryResponse.empty(productId);

        when(productReviewService.getProductRatingSummary(productId)).thenReturn(mockSummary);

        ApiResponse<ProductRatingSummaryResponse> response = controller.getProductRatingSummary(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Product rating summary retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(productId, response.getData().productId());

        verify(productReviewService, times(1)).getProductRatingSummary(productId);
    }
}
