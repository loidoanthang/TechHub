package com.techhub.controller.shipping;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.ShippingSimulationRequest;
import com.techhub.model.dto.response.ShippingSimulationResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.shipping.ShippingSimulationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminShippingController - POST /api/admin/shipping/simulate-advance")
class AdminShippingControllerTest {

    @Mock
    private ShippingSimulationService shippingSimulationService;

    @InjectMocks
    private AdminShippingController adminShippingController;

    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        User mockAdmin = new User();
        mockAdmin.setId(adminUserId);
        mockAdmin.setEmail("admin@techhub.vn");
        mockAdmin.setStatus(UserStatus.ACTIVE);
        mockAdmin.setRoles(Set.of(Role.ADMIN));

        CustomUserDetails userDetails = new CustomUserDetails(mockAdmin);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp lớp (@RestController, @RequestMapping(\"/api/admin/shipping\"))")
    void controller_classAnnotations_Correct() {
        assertTrue(AdminShippingController.class.isAnnotationPresent(RestController.class),
                "AdminShippingController phải có annotation @RestController");

        RequestMapping requestMapping = AdminShippingController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "AdminShippingController phải có annotation @RequestMapping");
        assertArrayEquals(new String[]{"/api/admin/shipping"}, requestMapping.value(),
                "Endpoint phải là /api/admin/shipping");
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp phương thức (@PostMapping(\"/simulate-advance\"), @PreAuthorize)")
    void simulateAdvance_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = AdminShippingController.class.getMethod("simulateAdvance", ShippingSimulationRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "Method simulateAdvance phải có annotation @PostMapping");
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/simulate-advance"}, postMapping.value(),
                "PostMapping path phải là /simulate-advance");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method simulateAdvance phải có annotation @PreAuthorize");
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", preAuth.value(),
                "PreAuthorize phải yêu cầu hasRole('ADMIN')");
    }

    @Test
    @DisplayName("simulateAdvance_Success_Returns200WithResponse")
    void simulateAdvance_Success_Returns200WithResponse() {
        ShippingSimulationRequest request = new ShippingSimulationRequest(true, 100);
        ShippingSimulationResponse mockResponse = new ShippingSimulationResponse(
                5, 8, 3, 150L, "Advanced 5 orders to SHIPPING, 8 to DELIVERED"
        );

        when(shippingSimulationService.advanceShippingLifecycle(eq(true), eq(100)))
                .thenReturn(mockResponse);

        ApiResponse<ShippingSimulationResponse> response = adminShippingController.simulateAdvance(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shipping simulation completed successfully", response.getMessage());
        assertEquals(5, response.getData().processedWaitingOrders());
        assertEquals(8, response.getData().processedShippingOrders());
        assertEquals(3, response.getData().updatedCodPayments());

        verify(shippingSimulationService, times(1)).advanceShippingLifecycle(eq(true), eq(100));
    }

    @Test
    @DisplayName("simulateAdvance_Success_WithNullRequest_AppliesDefaults")
    void simulateAdvance_Success_WithNullRequest_AppliesDefaults() {
        ShippingSimulationResponse mockResponse = new ShippingSimulationResponse(
                0, 0, 0, 10L, "Advanced 0 orders"
        );

        when(shippingSimulationService.advanceShippingLifecycle(eq(true), eq(50)))
                .thenReturn(mockResponse);

        ApiResponse<ShippingSimulationResponse> response = adminShippingController.simulateAdvance(null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shipping simulation completed successfully", response.getMessage());

        verify(shippingSimulationService, times(1)).advanceShippingLifecycle(eq(true), eq(50));
    }
}
