package com.techhub.controller.user;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.CreatePaymentInfoRequest;
import com.techhub.model.dto.response.PaymentInfoResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PaymentType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.user.UserPaymentInfoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho UserPaymentInfoController (POST /api/users/payment-info)")
class UserPaymentInfoControllerTest {

    @Mock
    private UserPaymentInfoService userPaymentInfoService;

    @InjectMocks
    private UserPaymentInfoController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-13: controller_createPaymentInfo_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER")
    void controller_createPaymentInfo_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("createPaymentInfo", CreatePaymentInfoRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method createPaymentInfo phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-14: controller_createPaymentInfo_Success_Returns201 - Trả về 201 Created với PaymentInfoResponse")
    void controller_createPaymentInfo_Success_Returns201() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CreatePaymentInfoRequest request = new CreatePaymentInfoRequest(
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "9876543210123",
                "NGUYEN VAN AN",
                null,
                true
        );

        UUID paymentInfoId = UUID.randomUUID();
        PaymentInfoResponse responseDto = new PaymentInfoResponse(
                paymentInfoId,
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "*********0123",
                "NGUYEN VAN AN",
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userPaymentInfoService.createPaymentInfo(eq(buyerId), eq(request))).thenReturn(responseDto);

        ApiResponse<PaymentInfoResponse> response = controller.createPaymentInfo(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Payment information added successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(paymentInfoId, response.getData().id());
        assertEquals("Vietcombank", response.getData().provider());
        assertEquals("*********0123", response.getData().accountNumber());
        assertTrue(response.getData().isDefault());

        verify(userPaymentInfoService, times(1)).createPaymentInfo(eq(buyerId), eq(request));
    }

    @Test
    @DisplayName("TC-25: controller_getPaymentInfos_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER khi lấy danh sách")
    void controller_getPaymentInfos_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("getPaymentInfos");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getPaymentInfos phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-26: controller_getPaymentInfos_Success_Returns200WithList - Trả về 200 OK với danh sách PaymentInfoResponse")
    void controller_getPaymentInfos_Success_Returns200WithList() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        PaymentInfoResponse item1 = new PaymentInfoResponse(
                UUID.randomUUID(),
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "*********0123",
                "NGUYEN VAN AN",
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        PaymentInfoResponse item2 = new PaymentInfoResponse(
                UUID.randomUUID(),
                PaymentType.E_WALLET,
                "MoMo",
                "******9988",
                "NGUYEN VAN AN",
                null,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userPaymentInfoService.getPaymentInfos(eq(buyerId))).thenReturn(List.of(item1, item2));

        ApiResponse<List<PaymentInfoResponse>> response = controller.getPaymentInfos();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Payment information retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertTrue(response.getData().get(0).isDefault());
        assertEquals("Vietcombank", response.getData().get(0).provider());
        assertFalse(response.getData().get(1).isDefault());
        assertEquals("MoMo", response.getData().get(1).provider());

        verify(userPaymentInfoService, times(1)).getPaymentInfos(eq(buyerId));
    }

    @Test
    @DisplayName("TC-36: controller_setDefaultPaymentInfo_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER khi đặt mặc định")
    void controller_setDefaultPaymentInfo_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("setDefaultPaymentInfo", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method setDefaultPaymentInfo phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-37: controller_setDefaultPaymentInfo_Success_Returns200 - Trả về 200 OK với PaymentInfoResponse đã cập nhật")
    void controller_setDefaultPaymentInfo_Success_Returns200() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID paymentInfoId = UUID.randomUUID();
        PaymentInfoResponse responseDto = new PaymentInfoResponse(
                paymentInfoId,
                PaymentType.BANK_ACCOUNT,
                "Vietcombank",
                "*********0123",
                "NGUYEN VAN AN",
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userPaymentInfoService.setDefaultPaymentInfo(eq(buyerId), eq(paymentInfoId))).thenReturn(responseDto);

        ApiResponse<PaymentInfoResponse> response = controller.setDefaultPaymentInfo(paymentInfoId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Default payment information updated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(paymentInfoId, response.getData().id());
        assertTrue(response.getData().isDefault());
        assertEquals("Vietcombank", response.getData().provider());

        verify(userPaymentInfoService, times(1)).setDefaultPaymentInfo(eq(buyerId), eq(paymentInfoId));
    }

    @Test
    @DisplayName("TC-47: controller_deletePaymentInfo_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER khi xóa")
    void controller_deletePaymentInfo_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = UserPaymentInfoController.class.getMethod("deletePaymentInfo", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method deletePaymentInfo phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-48: controller_deletePaymentInfo_Success_Returns200 - Trả về 200 OK với ApiResponse void")
    void controller_deletePaymentInfo_Success_Returns200() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID paymentInfoId = UUID.randomUUID();
        doNothing().when(userPaymentInfoService).deletePaymentInfo(eq(buyerId), eq(paymentInfoId));

        ApiResponse<Void> response = controller.deletePaymentInfo(paymentInfoId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Payment information deleted successfully", response.getMessage());
        assertNull(response.getData());

        verify(userPaymentInfoService, times(1)).deletePaymentInfo(eq(buyerId), eq(paymentInfoId));
    }
}
