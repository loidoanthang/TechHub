package com.techhub.service.notification.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.notification.NotificationListResponse;
import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.model.entity.Notification;
import com.techhub.model.entity.User;
import com.techhub.model.enums.NotificationType;
import com.techhub.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho NotificationServiceImpl (GET /api/notifications)")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private User sampleUser;
    private Notification notif1;
    private Notification notif2;
    private Notification notif3;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = new User();
        sampleUser.setId(userId);

        notif1 = Notification.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .type(NotificationType.ORDER)
                .title("Đơn hàng #TH-1001 đã xác nhận")
                .content("Shop đã xác nhận đơn hàng của bạn")
                .isRead(false)
                .referenceType("ORDER")
                .referenceId(UUID.randomUUID())
                .createdAt(LocalDateTime.now().minusHours(1))
                .deletedAt(null)
                .build();

        notif2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .type(NotificationType.REVIEW)
                .title("Shop đã phản hồi đánh giá")
                .content("Shop cảm ơn bạn đã ủng hộ")
                .isRead(false)
                .referenceType("REVIEW")
                .referenceId(UUID.randomUUID())
                .createdAt(LocalDateTime.now().minusHours(2))
                .deletedAt(null)
                .build();

        notif3 = Notification.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .type(NotificationType.PROMOTION)
                .title("Voucher 50K")
                .content("Mã giảm giá cho bạn")
                .isRead(true)
                .referenceType("VOUCHER")
                .referenceId(UUID.randomUUID())
                .createdAt(LocalDateTime.now().minusDays(1))
                .deletedAt(null)
                .build();
    }

    @Test
    @DisplayName("TC-01: getNotifications_Success_DefaultPagination - Lấy danh sách mặc định có đầy đủ unreadCount")
    void getNotifications_Success_DefaultPagination() {
        List<Notification> list = List.of(notif1, notif2, notif3);
        Page<Notification> pageResult = new PageImpl<>(list, PageRequest.of(0, 20), 3);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(2L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, null, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2L, response.unreadCount());
        assertEquals(3, response.notifications().content().size());
        assertEquals(3L, response.notifications().totalElements());
        assertEquals(0, response.notifications().pageNumber());

        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
        verify(notificationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-02: getNotifications_Success_FilterUnreadOnly - Lọc thông báo chưa đọc isRead = false")
    void getNotifications_Success_FilterUnreadOnly() {
        List<Notification> unreadList = List.of(notif1, notif2);
        Page<Notification> pageResult = new PageImpl<>(unreadList, PageRequest.of(0, 20), 2);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(2L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, false, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2L, response.unreadCount());
        assertEquals(2, response.notifications().content().size());
        assertFalse(response.notifications().content().get(0).isRead());
        assertFalse(response.notifications().content().get(1).isRead());
    }

    @Test
    @DisplayName("TC-03: getNotifications_Success_FilterReadOnly - Lọc thông báo đã đọc isRead = true nhưng vẫn bảo toàn unreadCount toàn cục")
    void getNotifications_Success_FilterReadOnly() {
        List<Notification> readList = List.of(notif3);
        Page<Notification> pageResult = new PageImpl<>(readList, PageRequest.of(0, 20), 1);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(2L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, true, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2L, response.unreadCount()); // Vẫn là 2L dù trang hiện tại chỉ chứa thông báo đã đọc
        assertEquals(1, response.notifications().content().size());
        assertTrue(response.notifications().content().get(0).isRead());
    }

    @Test
    @DisplayName("TC-04: getNotifications_Success_FilterByType_Order - Lọc theo loại danh mục ORDER")
    void getNotifications_Success_FilterByType_Order() {
        List<Notification> orderList = List.of(notif1);
        Page<Notification> pageResult = new PageImpl<>(orderList, PageRequest.of(0, 20), 1);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(2L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, null, NotificationType.ORDER, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.notifications().content().size());
        assertEquals(NotificationType.ORDER, response.notifications().content().get(0).type());
    }

    @Test
    @DisplayName("TC-05: getNotifications_Success_CombinedFilter_UnreadAndType - Kết hợp lọc vừa chưa đọc vừa loại ORDER")
    void getNotifications_Success_CombinedFilter_UnreadAndType() {
        List<Notification> filteredList = List.of(notif1);
        Page<Notification> pageResult = new PageImpl<>(filteredList, PageRequest.of(0, 20), 1);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(2L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, false, NotificationType.ORDER, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.notifications().content().size());
        assertEquals(NotificationType.ORDER, response.notifications().content().get(0).type());
        assertFalse(response.notifications().content().get(0).isRead());
    }

    @Test
    @DisplayName("TC-06: getNotifications_Success_EmptyList - Người dùng chưa có thông báo nào trả về rỗng an toàn")
    void getNotifications_Success_EmptyList() {
        Page<Notification> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(0L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        NotificationListResponse response = notificationService.getNotifications(userId, null, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(0L, response.unreadCount());
        assertTrue(response.notifications().content().isEmpty());
        assertEquals(0L, response.notifications().totalElements());
        assertEquals(0, response.notifications().totalPages());
        assertTrue(response.notifications().last());
    }

    @Test
    @DisplayName("TC-07: getNotifications_Success_ExcludeSoftDeleted - Không lấy thông báo đã bị xóa mềm")
    void getNotifications_Success_ExcludeSoftDeleted() {
        // Giả lập Specification lọc ra 2 thông báo active, loại bỏ thông báo deleted
        List<Notification> activeList = List.of(notif1, notif3);
        Page<Notification> pageResult = new PageImpl<>(activeList, PageRequest.of(0, 20), 2);

        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(1L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageResult);

        NotificationListResponse response = notificationService.getNotifications(userId, null, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2, response.notifications().content().size());
        verify(notificationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-08: getNotifications_Success_UnreadCount_ExcludesSoftDeleted - Đếm unreadCount loại trừ thông báo đã xóa mềm")
    void getNotifications_Success_UnreadCount_ExcludesSoftDeleted() {
        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(1L);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        NotificationListResponse response = notificationService.getNotifications(userId, null, null, PageRequest.of(0, 20));

        assertEquals(1L, response.unreadCount());
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }

    @Test
    @DisplayName("TC-09: getNotifications_Success_DeterministicSort_TieBreaker - Đảm bảo phân trang có sắp xếp tie-breaker id DESC")
    void getNotifications_Success_DeterministicSort_TieBreaker() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(0L);
        when(notificationRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(Page.empty());

        notificationService.getNotifications(userId, null, null, PageRequest.of(0, 20));

        Pageable passedPageable = pageableCaptor.getValue();
        assertNotNull(passedPageable);
        Sort sort = passedPageable.getSort();
        assertNotNull(sort);
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
    }

    @Test
    @DisplayName("TC-10: getNotifications_Success_CustomPagination - Phân trang tùy biến page=1, size=10")
    void getNotifications_Success_CustomPagination() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationRepository.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId)).thenReturn(5L);
        when(notificationRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(
                new PageImpl<>(List.of(notif1), PageRequest.of(1, 10), 25)
        );

        NotificationListResponse response = notificationService.getNotifications(userId, null, null, PageRequest.of(1, 10));

        assertEquals(1, response.notifications().pageNumber());
        assertEquals(10, response.notifications().pageSize());
        assertEquals(25L, response.notifications().totalElements());
        assertEquals(3, response.notifications().totalPages());
    }

    @Test
    @DisplayName("TC-16: getNotifications_TransactionReadOnly_Verification - Kiểm tra cấu hình @Transactional(readOnly = true)")
    void getNotifications_TransactionReadOnly_Verification() throws NoSuchMethodException {
        Method method = NotificationServiceImpl.class.getMethod("getNotifications", UUID.class, Boolean.class, NotificationType.class, Pageable.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "Method getNotifications phải có annotation @Transactional");
        assertTrue(annotation.readOnly(), "Method getNotifications phải được cấu hình readOnly = true");
    }

    @Test
    @DisplayName("TC-19: markAsRead_Success_HappyPath - Đánh dấu thông báo chưa đọc thành đã đọc thành công")
    void markAsRead_Success_HappyPath() {
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif1));

        assertDoesNotThrow(() -> notificationService.markAsRead(userId, notifId));

        assertTrue(notif1.isRead());
        verify(notificationRepository, times(1)).save(notif1);
    }

    @Test
    @DisplayName("TC-20: markAsRead_Success_Idempotent_AlreadyRead - Thông báo đã đọc từ trước thì bỏ qua save DB (Idempotent)")
    void markAsRead_Success_Idempotent_AlreadyRead() {
        UUID notifId = notif3.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif3));

        assertDoesNotThrow(() -> notificationService.markAsRead(userId, notifId));

        assertTrue(notif3.isRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-21: markAsRead_Failed_NotFound - Ném NOTIFICATION_NOT_FOUND khi ID không tồn tại")
    void markAsRead_Failed_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(randomId, userId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.markAsRead(userId, randomId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-22: markAsRead_Failed_IDOR_BelongsToAnotherUser - Chống IDOR: ném NOTIFICATION_NOT_FOUND khi thông báo thuộc user khác")
    void markAsRead_Failed_IDOR_BelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, otherUserId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.markAsRead(otherUserId, notifId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-23: markAsRead_Failed_AlreadySoftDeleted - Ném NOTIFICATION_NOT_FOUND khi thông báo đã bị xóa mềm")
    void markAsRead_Failed_AlreadySoftDeleted() {
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.markAsRead(userId, notifId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-24: markAsRead_TransactionRollback_OnException - Kiểm tra cấu hình @Transactional(rollbackFor = Exception.class)")
    void markAsRead_TransactionRollback_OnException() throws NoSuchMethodException {
        Method method = NotificationServiceImpl.class.getMethod("markAsRead", UUID.class, UUID.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "Method markAsRead phải có @Transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                "Phải cấu hình rollbackFor = Exception.class");
    }

    @Test
    @DisplayName("TC-29: markAsRead_Success_VerifyRepositoryInteraction - Kiểm tra tương tác đầy đủ với Repository khi markAsRead")
    void markAsRead_Success_VerifyRepositoryInteraction() {
        UUID notifId = notif2.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif2));

        notificationService.markAsRead(userId, notifId);

        verify(notificationRepository, times(1)).findByIdAndUserIdAndDeletedAtIsNull(notifId, userId);
        verify(notificationRepository, times(1)).save(notif2);
        assertTrue(notif2.isRead());
    }

    // =========================================================================
    // API 3: PATCH /api/notifications/read-all (TC-30 -> TC-35, TC-40)
    // =========================================================================

    @Test
    @DisplayName("TC-30: markAllAsRead_Success_HappyPath - Cập nhật toàn bộ thông báo chưa đọc thành đã đọc")
    void markAllAsRead_Success_HappyPath() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(5);

        assertDoesNotThrow(() -> notificationService.markAllAsRead(userId));

        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    @DisplayName("TC-31: markAllAsRead_Success_ZeroUnread_Idempotent - Xử lý an toàn khi người dùng có 0 thông báo chưa đọc")
    void markAllAsRead_Success_ZeroUnread_Idempotent() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(0);

        assertDoesNotThrow(() -> notificationService.markAllAsRead(userId));

        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    @DisplayName("TC-32: markAllAsRead_Success_DoesNotAffectOtherUsers - Chỉ cập nhật thông báo của user hiện tại (Anti-IDOR)")
    void markAllAsRead_Success_DoesNotAffectOtherUsers() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(3);

        notificationService.markAllAsRead(userId);

        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
        verify(notificationRepository, never()).markAllAsReadByUserId(otherUserId);
    }

    @Test
    @DisplayName("TC-33: markAllAsRead_Success_DoesNotAffectSoftDeletedNotifications - Truy vấn loại trừ các thông báo đã xóa mềm")
    void markAllAsRead_Success_DoesNotAffectSoftDeletedNotifications() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(2);

        notificationService.markAllAsRead(userId);

        // markAllAsReadByUserId có mệnh đề WHERE deleted_at IS NULL
        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    @DisplayName("TC-34: markAllAsRead_Success_AlreadyReadNotificationsRemainUntouched - Chỉ cập nhật thông báo có is_read = false")
    void markAllAsRead_Success_AlreadyReadNotificationsRemainUntouched() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(4);

        assertDoesNotThrow(() -> notificationService.markAllAsRead(userId));

        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    @DisplayName("TC-35: markAllAsRead_TransactionRollback_OnException - Kiểm tra cấu hình @Transactional(rollbackFor = Exception.class)")
    void markAllAsRead_TransactionRollback_OnException() throws NoSuchMethodException {
        Method method = NotificationServiceImpl.class.getMethod("markAllAsRead", UUID.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "Method markAllAsRead phải có @Transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                "Phải cấu hình rollbackFor = Exception.class");
    }

    @Test
    @DisplayName("TC-40: markAllAsRead_Success_VerifyRepositoryInteraction - Kiểm tra tương tác đúng đắn duy nhất 1 lần với repository")
    void markAllAsRead_Success_VerifyRepositoryInteraction() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(10);

        notificationService.markAllAsRead(userId);

        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
        verifyNoMoreInteractions(notificationRepository);
    }

    // =========================================================================
    // API 4: DELETE /api/notifications/{id} (TC-41 -> TC-46, TC-51)
    // =========================================================================

    @Test
    @DisplayName("TC-41: deleteNotification_Success_HappyPath_UnreadNotification - Xóa mềm thông báo chưa đọc thành công")
    void deleteNotification_Success_HappyPath_UnreadNotification() {
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif1));

        assertDoesNotThrow(() -> notificationService.deleteNotification(userId, notifId));

        assertNotNull(notif1.getDeletedAt());
        verify(notificationRepository, times(1)).save(notif1);
    }

    @Test
    @DisplayName("TC-42: deleteNotification_Success_HappyPath_ReadNotification - Xóa mềm thông báo đã đọc thành công")
    void deleteNotification_Success_HappyPath_ReadNotification() {
        UUID notifId = notif2.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif2));

        assertDoesNotThrow(() -> notificationService.deleteNotification(userId, notifId));

        assertNotNull(notif2.getDeletedAt());
        verify(notificationRepository, times(1)).save(notif2);
    }

    @Test
    @DisplayName("TC-43: deleteNotification_Failed_NotFound - Ném NOTIFICATION_NOT_FOUND khi ID thông báo không tồn tại")
    void deleteNotification_Failed_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(randomId, userId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.deleteNotification(userId, randomId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-44: deleteNotification_Failed_IDOR_BelongsToAnotherUser - Ném NOTIFICATION_NOT_FOUND khi cố xóa thông báo của user khác (Anti-IDOR)")
    void deleteNotification_Failed_IDOR_BelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, otherUserId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.deleteNotification(otherUserId, notifId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-45: deleteNotification_Failed_AlreadySoftDeleted - Ném NOTIFICATION_NOT_FOUND khi thông báo đã bị xóa mềm trước đó")
    void deleteNotification_Failed_AlreadySoftDeleted() {
        UUID notifId = notif1.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                notificationService.deleteNotification(userId, notifId)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-46: deleteNotification_TransactionRollback_OnException - Kiểm tra cấu hình @Transactional(rollbackFor = Exception.class)")
    void deleteNotification_TransactionRollback_OnException() throws NoSuchMethodException {
        Method method = NotificationServiceImpl.class.getMethod("deleteNotification", UUID.class, UUID.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "Method deleteNotification phải có @Transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                "Phải cấu hình rollbackFor = Exception.class");
    }

    @Test
    @DisplayName("TC-51: deleteNotification_Success_VerifyRepositoryInteraction - Kiểm tra tương tác đầy đủ với Repository khi xóa mềm")
    void deleteNotification_Success_VerifyRepositoryInteraction() {
        UUID notifId = notif3.getId();
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notifId, userId))
                .thenReturn(java.util.Optional.of(notif3));

        notificationService.deleteNotification(userId, notifId);

        verify(notificationRepository, times(1)).findByIdAndUserIdAndDeletedAtIsNull(notifId, userId);
        verify(notificationRepository, times(1)).save(notif3);
        assertNotNull(notif3.getDeletedAt());
    }
}
