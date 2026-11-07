package com.techhub.controller.notification;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.notification.BroadcastNotificationRequest;
import com.techhub.model.dto.response.notification.BroadcastNotificationResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.TargetAudience;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.notification.AdminNotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminNotificationController (POST /api/admin/notifications/broadcast)")
class AdminNotificationControllerTest {

    @Mock
    private AdminNotificationService adminNotificationService;

    @InjectMocks
    private AdminNotificationController controller;

    private Validator validator;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        adminId = UUID.randomUUID();
        User adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setEmail("admin@techhub.com");
        adminUser.setRoles(Set.of(Role.ADMIN));

        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
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
    @DisplayName("TC-87: controller_ClassAnnotations_CorrectMappingAndAdminPreAuthorize - Xác thực annotation lớp Controller")
    void controller_ClassAnnotations_CorrectMappingAndAdminPreAuthorize() {
        Class<?> clazz = AdminNotificationController.class;

        assertTrue(clazz.isAnnotationPresent(RestController.class));
        assertTrue(clazz.isAnnotationPresent(Validated.class));

        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/api/admin/notifications"}, requestMapping.value());

        PreAuthorize preAuthorize = clazz.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    @DisplayName("TC-88: controller_broadcast_MethodMappingAndValidAnnotation - Xác thực annotation phương thức broadcast")
    void controller_broadcast_MethodMappingAndValidAnnotation() throws NoSuchMethodException {
        Method method = AdminNotificationController.class.getMethod("broadcast", BroadcastNotificationRequest.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/broadcast"}, postMapping.value());

        Parameter requestParam = method.getParameters()[0];
        assertTrue(requestParam.isAnnotationPresent(Valid.class));
    }

    @Test
    @DisplayName("TC-89: controller_broadcast_Success_HappyPath - Admin broadcast thành công trả về 200 OK")
    void controller_broadcast_Success_HappyPath() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Tiêu đề hệ thống",
                "Nội dung thông báo hệ thống",
                NotificationType.SYSTEM,
                null,
                null
        );

        BroadcastNotificationResponse serviceResponse = new BroadcastNotificationResponse(
                50, 50, TargetAudience.ALL_USERS, NotificationType.SYSTEM, "Tiêu đề hệ thống", LocalDateTime.now()
        );

        when(adminNotificationService.broadcast(eq(request), eq(adminId))).thenReturn(serviceResponse);

        ApiResponse<BroadcastNotificationResponse> apiResponse = controller.broadcast(request);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Broadcast notification sent successfully", apiResponse.getMessage());
        assertEquals(50, apiResponse.getData().totalSent());
        verify(adminNotificationService, times(1)).broadcast(eq(request), eq(adminId));
    }

    @Test
    @DisplayName("TC-90: controller_broadcast_Failed_Unauthenticated - Ném lỗi khi SecurityContext rỗng")
    void controller_broadcast_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();

        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS,
                "Tiêu đề",
                "Nội dung",
                NotificationType.SYSTEM,
                null,
                null
        );

        assertThrows(Exception.class, () -> controller.broadcast(request));
        verify(adminNotificationService, never()).broadcast(any(), any());
    }

    @Test
    @DisplayName("TC-79: validation_NullTargetAudience_ViolationDetected - Báo lỗi khi thiếu targetAudience")
    void validation_NullTargetAudience_ViolationDetected() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                null, "Tiêu đề", "Nội dung", NotificationType.SYSTEM, null, null
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("targetAudience")));
    }

    @Test
    @DisplayName("TC-80: validation_BlankTitle_ViolationDetected - Báo lỗi khi tiêu đề để trống")
    void validation_BlankTitle_ViolationDetected() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS, "   ", "Nội dung", NotificationType.SYSTEM, null, null
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("TC-81: validation_TitleExceeds300Chars_ViolationDetected - Báo lỗi khi tiêu đề quá 300 ký tự")
    void validation_TitleExceeds300Chars_ViolationDetected() {
        String longTitle = "A".repeat(301);
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS, longTitle, "Nội dung", NotificationType.SYSTEM, null, null
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("TC-82: validation_BlankContent_ViolationDetected - Báo lỗi khi nội dung để trống")
    void validation_BlankContent_ViolationDetected() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS, "Tiêu đề hợp lệ", "", NotificationType.SYSTEM, null, null
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("content")));
    }

    @Test
    @DisplayName("TC-91: validation_ReferenceTypeExceeds30Chars_ViolationDetected - Báo lỗi khi referenceType quá 30 ký tự")
    void validation_ReferenceTypeExceeds30Chars_ViolationDetected() {
        String longRefType = "A".repeat(31);
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_USERS, "Tiêu đề", "Nội dung", NotificationType.SYSTEM, longRefType, UUID.randomUUID()
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("referenceType")));
    }

    @Test
    @DisplayName("TC-92: validation_ValidRequest_NoViolations - Request hợp lệ không có lỗi validation")
    void validation_ValidRequest_NoViolations() {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest(
                TargetAudience.ALL_BUYERS, "Tiêu đề hợp lệ", "Nội dung hợp lệ", NotificationType.PROMOTION, "VOUCHER", UUID.randomUUID()
        );

        Set<ConstraintViolation<BroadcastNotificationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}
