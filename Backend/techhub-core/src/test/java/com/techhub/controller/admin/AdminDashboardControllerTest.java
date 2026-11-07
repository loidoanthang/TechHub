package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.dashboard.*;
import com.techhub.model.entity.User;
import com.techhub.model.enums.DashboardPeriod;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.dashboard.AdminDashboardService;
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
@DisplayName("Unit Tests cho AdminDashboardController (GET /api/admin/dashboard/metrics)")
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController controller;

    private UUID adminId;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        testAdminUser = new User();
        testAdminUser.setId(adminId);
        testAdminUser.setEmail("admin@techhub.com");
        testAdminUser.setStatus(UserStatus.ACTIVE);
        testAdminUser.setRoles(Set.of(Role.ADMIN));

        CustomUserDetails userDetails = new CustomUserDetails(testAdminUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-ADM-DASH-CTRL-01: getPlatformMetrics_Success_Returns200 - Trả về 200 OK kèm dữ liệu dashboard")
    void getPlatformMetrics_Success_Returns200() {
        AdminDashboardMetricsResponse mockResponse = new AdminDashboardMetricsResponse(
                DashboardPeriod.LAST_7_DAYS,
                LocalDate.now().minusDays(6),
                LocalDate.now(),
                new AdminDashboardSummaryDto(
                        new BigDecimal("10000000.00"),
                        new BigDecimal("8000000.00"),
                        new BigDecimal("400000.00"),
                        10L, 8L, 20L,
                        new BigDecimal("1000000.00"),
                        10.0, 5.0,
                        new BigDecimal("50000000.00"),
                        new BigDecimal("10000000.00"),
                        new BigDecimal("20000000.00")
                ),
                new AdminOrderStatusBreakdownDto(1, 1, 0, 0, 0, 8, 1, 1, 0),
                new PlatformGrowthDto(5, 2, 10, 1, 0, 0),
                Collections.emptyList()
        );

        when(adminDashboardService.getPlatformDashboardMetrics(eq(DashboardPeriod.LAST_7_DAYS), any(), any()))
                .thenReturn(mockResponse);

        ApiResponse<AdminDashboardMetricsResponse> response = controller.getPlatformDashboardMetrics(
                DashboardPeriod.LAST_7_DAYS, null, null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Lấy chỉ số vận hành toàn sàn thành công", response.getMessage());
        assertEquals(DashboardPeriod.LAST_7_DAYS, response.getData().period());
        assertEquals(new BigDecimal("10000000.00"), response.getData().summary().totalGmv());
    }

    @Test
    @DisplayName("TC-ADM-DASH-CTRL-02: getPlatformMetrics_InvalidRange_ThrowsException - Ngoại lệ từ Service được truyền ra ngoài")
    void getPlatformMetrics_InvalidRange_ThrowsException() {
        when(adminDashboardService.getPlatformDashboardMetrics(eq(DashboardPeriod.CUSTOM), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT_DATA));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getPlatformDashboardMetrics(
                        DashboardPeriod.CUSTOM,
                        LocalDate.now(),
                        LocalDate.now().minusDays(5)
                ));

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-DASH-CTRL-03: getPlatformMetrics_CustomRange_PassesParameters - Truyền đúng startDate và endDate vào Service")
    void getPlatformMetrics_CustomRange_PassesParameters() {
        LocalDate start = LocalDate.now().minusDays(20);
        LocalDate end = LocalDate.now().minusDays(5);

        AdminDashboardMetricsResponse mockResponse = new AdminDashboardMetricsResponse(
                DashboardPeriod.CUSTOM,
                start,
                end,
                null,
                null,
                null,
                Collections.emptyList()
        );

        when(adminDashboardService.getPlatformDashboardMetrics(eq(DashboardPeriod.CUSTOM), eq(start), eq(end)))
                .thenReturn(mockResponse);

        ApiResponse<AdminDashboardMetricsResponse> response = controller.getPlatformDashboardMetrics(
                DashboardPeriod.CUSTOM, start, end);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(DashboardPeriod.CUSTOM, response.getData().period());
        assertEquals(start, response.getData().startDate());
        assertEquals(end, response.getData().endDate());
    }

    @Test
    @DisplayName("TC-ADM-DASH-CTRL-04: getPlatformMetrics_DefaultPeriod_CallsServiceWithNulls")
    void getPlatformMetrics_DefaultPeriod_CallsServiceWithNulls() {
        AdminDashboardMetricsResponse mockResponse = new AdminDashboardMetricsResponse(
                DashboardPeriod.LAST_7_DAYS,
                LocalDate.now().minusDays(6),
                LocalDate.now(),
                null, null, null, Collections.emptyList()
        );

        when(adminDashboardService.getPlatformDashboardMetrics(isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        ApiResponse<AdminDashboardMetricsResponse> response = controller.getPlatformDashboardMetrics(
                null, null, null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(adminDashboardService).getPlatformDashboardMetrics(isNull(), isNull(), isNull());
    }
}
