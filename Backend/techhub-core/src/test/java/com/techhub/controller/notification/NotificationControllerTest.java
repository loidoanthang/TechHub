package com.techhub.controller.notification;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.notification.NotificationListResponse;
import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.NotificationType;
import com.techhub.model.enums.Role;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.notification.NotificationService;
import com.techhub.service.notification.NotificationStreamService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho NotificationController (GET /api/notifications)")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationStreamService notificationStreamService;

    @InjectMocks
    private NotificationController controller;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("buyer@techhub.com");
        testUser.setRoles(Set.of(Role.BUYER));

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
    @DisplayName("TC-01_Ctrl: controller_getNotifications_Success_Returns200 - Trả về HTTP 200 và dữ liệu danh sách thông báo")
    void controller_getNotifications_Success_Returns200() {
        NotificationResponse item = new NotificationResponse(
                UUID.randomUUID(),
                NotificationType.ORDER,
                "Đơn hàng mới",
                "Đơn hàng đã được tạo",
                false,
                "ORDER",
                UUID.randomUUID(),
                LocalDateTime.now()
        );
        PageResponse<NotificationResponse> pageResponse = new PageResponse<>(
                List.of(item), 0, 20, 1L, 1, true
        );
        NotificationListResponse mockResult = new NotificationListResponse(1L, pageResponse);

        when(notificationService.getNotifications(eq(userId), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(mockResult);

        ApiResponse<NotificationListResponse> apiResponse = controller.getNotifications(null, null, 0, 20);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Notifications retrieved successfully", apiResponse.getMessage());
        assertNotNull(apiResponse.getData());
        assertEquals(1L, apiResponse.getData().unreadCount());
        assertEquals(1, apiResponse.getData().notifications().content().size());

        verify(notificationService, times(1)).getNotifications(eq(userId), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-11: controller_getNotifications_Validation_PageMinAnnotation - Kiểm tra ràng buộc @Min(0) cho param page")
    void controller_getNotifications_Validation_PageMinAnnotation() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);
        Parameter pageParam = method.getParameters()[2];

        Min minAnnotation = pageParam.getAnnotation(Min.class);
        assertNotNull(minAnnotation, "Tham số page phải có annotation @Min");
        assertEquals(0L, minAnnotation.value(), "Tham số page phải có giá trị tối thiểu là 0");

        RequestParam requestParam = pageParam.getAnnotation(RequestParam.class);
        assertNotNull(requestParam);
        assertEquals("0", requestParam.defaultValue());
    }

    @Test
    @DisplayName("TC-12: controller_getNotifications_Validation_SizeMinAnnotation - Kiểm tra ràng buộc @Min(1) cho param size")
    void controller_getNotifications_Validation_SizeMinAnnotation() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);
        Parameter sizeParam = method.getParameters()[3];

        Min minAnnotation = sizeParam.getAnnotation(Min.class);
        assertNotNull(minAnnotation, "Tham số size phải có annotation @Min");
        assertEquals(1L, minAnnotation.value(), "Tham số size phải có giá trị tối thiểu là 1");
    }

    @Test
    @DisplayName("TC-13: controller_getNotifications_Validation_SizeMaxAnnotation - Kiểm tra ràng buộc @Max(100) cho param size")
    void controller_getNotifications_Validation_SizeMaxAnnotation() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);
        Parameter sizeParam = method.getParameters()[3];

        Max maxAnnotation = sizeParam.getAnnotation(Max.class);
        assertNotNull(maxAnnotation, "Tham số size phải có annotation @Max");
        assertEquals(100L, maxAnnotation.value(), "Tham số size không được vượt quá 100");

        RequestParam requestParam = sizeParam.getAnnotation(RequestParam.class);
        assertNotNull(requestParam);
        assertEquals("20", requestParam.defaultValue());
    }

    @Test
    @DisplayName("TC-14: controller_getNotifications_TypeParameterMapping - Kiểm tra kiểu NotificationType cho param type")
    void controller_getNotifications_TypeParameterMapping() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);
        Parameter typeParam = method.getParameters()[1];

        assertEquals(NotificationType.class, typeParam.getType(), "Tham số type phải có kiểu NotificationType");
        RequestParam requestParam = typeParam.getAnnotation(RequestParam.class);
        assertNotNull(requestParam);
        assertFalse(requestParam.required(), "Tham số type không được bắt buộc");
    }

    @Test
    @DisplayName("TC-15: controller_getNotifications_RequiresAuthentication - Bắt buộc @PreAuthorize('isAuthenticated()')")
    void controller_getNotifications_RequiresAuthentication() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method getNotifications phải có annotation @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value(), "Quyền hạn yêu cầu phải là isAuthenticated()");
    }

    @Test
    @DisplayName("TC-Mapping: controller_getNotifications_CorrectRequestMapping - Đúng cấu hình class và method mapping")
    void controller_getNotifications_CorrectRequestMapping() throws NoSuchMethodException {
        RestController restController = NotificationController.class.getAnnotation(RestController.class);
        assertNotNull(restController, "Controller phải có @RestController");

        RequestMapping requestMapping = NotificationController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "Controller phải có @RequestMapping");
        assertArrayEquals(new String[]{"/api/notifications"}, requestMapping.value());

        Validated validated = NotificationController.class.getAnnotation(Validated.class);
        assertNotNull(validated, "Controller phải có @Validated để kích hoạt validation param");

        Method method = NotificationController.class.getMethod("getNotifications", Boolean.class, NotificationType.class, int.class, int.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method phải có @GetMapping");
    }

    @Test
    @DisplayName("TC-25: controller_markAsRead_Success_Returns200 - Trả về HTTP 200 OK và message xác nhận")
    void controller_markAsRead_Success_Returns200() {
        UUID notifId = UUID.randomUUID();
        doNothing().when(notificationService).markAsRead(eq(userId), eq(notifId));

        ApiResponse<Void> response = controller.markAsRead(notifId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Notification marked as read successfully", response.getMessage());
        assertNull(response.getData());

        verify(notificationService, times(1)).markAsRead(eq(userId), eq(notifId));
    }

    @Test
    @DisplayName("TC-26: controller_markAsRead_RequiresAuthentication - Bắt buộc @PreAuthorize('isAuthenticated()')")
    void controller_markAsRead_RequiresAuthentication() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("markAsRead", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method markAsRead phải có @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value(), "Quyền hạn yêu cầu phải là isAuthenticated()");
    }

    @Test
    @DisplayName("TC-27: controller_markAsRead_CorrectMapping - Đúng cấu hình @PatchMapping('/{id}/read')")
    void controller_markAsRead_CorrectMapping() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("markAsRead", UUID.class);

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertNotNull(patchMapping, "Method markAsRead phải có annotation @PatchMapping");
        assertArrayEquals(new String[]{"/{id}/read"}, patchMapping.value());
    }

    @Test
    @DisplayName("TC-28: controller_markAsRead_Failed_Unauthenticated - Ném UNAUTHENTICATED khi không có phiên đăng nhập")
    void controller_markAsRead_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();
        UUID notifId = UUID.randomUUID();

        assertThrows(Exception.class, () -> controller.markAsRead(notifId));
        verify(notificationService, never()).markAsRead(any(), any());
    }

    // =========================================================================
    // API 3: PATCH /api/notifications/read-all (TC-36 -> TC-39)
    // =========================================================================

    @Test
    @DisplayName("TC-36: controller_markAllAsRead_Success_Returns200 - Trả về HTTP 200 OK và message xác nhận")
    void controller_markAllAsRead_Success_Returns200() {
        doNothing().when(notificationService).markAllAsRead(eq(userId));

        ApiResponse<Void> response = controller.markAllAsRead();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("All notifications marked as read successfully", response.getMessage());
        assertNull(response.getData());

        verify(notificationService, times(1)).markAllAsRead(eq(userId));
    }

    @Test
    @DisplayName("TC-37: controller_markAllAsRead_RequiresAuthentication - Bắt buộc @PreAuthorize('isAuthenticated()')")
    void controller_markAllAsRead_RequiresAuthentication() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("markAllAsRead");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method markAllAsRead phải có @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value(), "Quyền hạn yêu cầu phải là isAuthenticated()");
    }

    @Test
    @DisplayName("TC-38: controller_markAllAsRead_CorrectMapping - Đúng cấu hình @PatchMapping('/read-all')")
    void controller_markAllAsRead_CorrectMapping() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("markAllAsRead");

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        assertNotNull(patchMapping, "Method markAllAsRead phải có annotation @PatchMapping");
        assertArrayEquals(new String[]{"/read-all"}, patchMapping.value());
    }

    @Test
    @DisplayName("TC-39: controller_markAllAsRead_Failed_Unauthenticated - Ném UNAUTHENTICATED khi không có phiên đăng nhập")
    void controller_markAllAsRead_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(Exception.class, () -> controller.markAllAsRead());
        verify(notificationService, never()).markAllAsRead(any());
    }

    // =========================================================================
    // API 4: DELETE /api/notifications/{id} (TC-47 -> TC-50)
    // =========================================================================

    @Test
    @DisplayName("TC-47: controller_deleteNotification_Success_Returns200 - Trả về HTTP 200 OK và message xác nhận")
    void controller_deleteNotification_Success_Returns200() {
        UUID notifId = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(eq(userId), eq(notifId));

        ApiResponse<Void> response = controller.deleteNotification(notifId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Notification deleted successfully", response.getMessage());
        assertNull(response.getData());

        verify(notificationService, times(1)).deleteNotification(eq(userId), eq(notifId));
    }

    @Test
    @DisplayName("TC-48: controller_deleteNotification_RequiresAuthentication - Bắt buộc @PreAuthorize('isAuthenticated()')")
    void controller_deleteNotification_RequiresAuthentication() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("deleteNotification", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method deleteNotification phải có @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value(), "Quyền hạn yêu cầu phải là isAuthenticated()");
    }

    @Test
    @DisplayName("TC-49: controller_deleteNotification_CorrectMapping - Đúng cấu hình @DeleteMapping('/{id}')")
    void controller_deleteNotification_CorrectMapping() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("deleteNotification", UUID.class);

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        assertNotNull(deleteMapping, "Method deleteNotification phải có annotation @DeleteMapping");
        assertArrayEquals(new String[]{"/{id}"}, deleteMapping.value());
    }

    @Test
    @DisplayName("TC-50: controller_deleteNotification_Failed_Unauthenticated - Ném UNAUTHENTICATED khi không có phiên đăng nhập")
    void controller_deleteNotification_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();
        UUID notifId = UUID.randomUUID();

        assertThrows(Exception.class, () -> controller.deleteNotification(notifId));
        verify(notificationService, never()).deleteNotification(any(), any());
    }

    // =========================================================================
    // API 5: GET /api/notifications/stream (TC-60 -> TC-62)
    // =========================================================================

    @Test
    @DisplayName("TC-60: controller_stream_RequiresAuthentication - Bắt buộc @PreAuthorize('isAuthenticated()')")
    void controller_stream_RequiresAuthentication() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("stream");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method stream phải có @PreAuthorize");
        assertEquals("isAuthenticated()", preAuthorize.value(), "Quyền hạn yêu cầu phải là isAuthenticated()");
    }

    @Test
    @DisplayName("TC-61: controller_stream_CorrectMappingAndMediaType - Đúng cấu hình @GetMapping('/stream', produces = 'text/event-stream')")
    void controller_stream_CorrectMappingAndMediaType() throws NoSuchMethodException {
        Method method = NotificationController.class.getMethod("stream");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method stream phải có annotation @GetMapping");
        assertArrayEquals(new String[]{"/stream"}, getMapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, getMapping.produces());
    }

    @Test
    @DisplayName("TC-62: controller_stream_Failed_Unauthenticated - Ném UNAUTHENTICATED khi không có phiên đăng nhập")
    void controller_stream_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(Exception.class, () -> controller.stream());
        verify(notificationStreamService, never()).subscribe(any());
    }

    @Test
    @DisplayName("TC-63: controller_stream_Success_ReturnsSseEmitter - Đăng ký stream thành công trả về SseEmitter")
    void controller_stream_Success_ReturnsSseEmitter() {
        SseEmitter mockEmitter = new SseEmitter();
        when(notificationStreamService.subscribe(eq(userId))).thenReturn(mockEmitter);

        SseEmitter result = controller.stream();

        assertNotNull(result);
        assertEquals(mockEmitter, result);
        verify(notificationStreamService, times(1)).subscribe(eq(userId));
    }
}
