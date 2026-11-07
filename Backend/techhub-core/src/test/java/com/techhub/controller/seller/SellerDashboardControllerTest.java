package com.techhub.controller.seller;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.SellerDashboardMetricsResponse;
import com.techhub.model.dto.response.dashboard.SellerDashboardSummaryDto;
import com.techhub.model.dto.response.dashboard.SellerOrderStatusBreakdownDto;
import com.techhub.model.entity.User;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.dashboard.SellerDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerDashboardController (GET /api/seller/dashboard/metrics)")
class SellerDashboardControllerTest {

    @Mock
    private SellerDashboardService sellerDashboardService;

    @InjectMocks
    private SellerDashboardController controller;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("seller@techhub.com");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRoles(Set.of(Role.SELLER));

        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getDashboardMetrics_Success_Returns200 - Trả về 200 OK kèm dữ liệu dashboard")
    void getDashboardMetrics_Success_Returns200() {
        SellerDashboardMetricsResponse mockResponse = new SellerDashboardMetricsResponse(
                DashboardPeriod.LAST_7_DAYS,
                LocalDate.now().minusDays(6),
                LocalDate.now(),
                new SellerDashboardSummaryDto(new BigDecimal("10000000.00"), 5L, 5L, 10L, new BigDecimal("15000000.00"), BigDecimal.ZERO, new BigDecimal("2000000.00")),
                new SellerOrderStatusBreakdownDto(0, 0, 0, 0, 0, 5, 0, 0, 0),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(sellerDashboardService.getDashboardMetrics(eq(userId), eq(DashboardPeriod.LAST_7_DAYS), isNull(), isNull()))
                .thenReturn(mockResponse);

        ApiResponse<SellerDashboardMetricsResponse> response = controller.getDashboardMetrics(DashboardPeriod.LAST_7_DAYS, null, null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Lấy chỉ số vận hành gian hàng thành công", response.getMessage());
        assertEquals(DashboardPeriod.LAST_7_DAYS, response.getData().period());
        assertEquals(new BigDecimal("10000000.00"), response.getData().summary().netRevenue());
    }

    @Test
    @DisplayName("getDashboardMetrics_Unauthenticated_ThrowsException - Chưa xác thực ném UNAUTHENTICATED")
    void getDashboardMetrics_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        assertThrows(BusinessException.class, () ->
                controller.getDashboardMetrics(DashboardPeriod.LAST_7_DAYS, null, null));
    }

    @Test
    @DisplayName("getDashboardMetrics_BannedShop_Returns400 - Service ném SHOP_NOT_ACTIVE khi Shop bị cấm")
    void getDashboardMetrics_BannedShop_Returns400() {
        when(sellerDashboardService.getDashboardMetrics(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.SHOP_NOT_ACTIVE));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getDashboardMetrics(DashboardPeriod.LAST_7_DAYS, null, null));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("getDashboardMetrics_InvalidCustomRange_Returns400 - Service ném INVALID_INPUT_DATA khi khoảng ngày sai")
    void getDashboardMetrics_InvalidCustomRange_Returns400() {
        when(sellerDashboardService.getDashboardMetrics(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT_DATA));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getDashboardMetrics(DashboardPeriod.CUSTOM, LocalDate.now(), LocalDate.now().minusDays(5)));

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }
}
