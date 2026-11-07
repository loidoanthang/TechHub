package com.techhub.service.review;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.entity.*;
import com.techhub.model.enums.ReviewMediaType;
import com.techhub.model.enums.Role;
import com.techhub.repository.ReviewRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.review.impl.AdminReviewServiceImpl;
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
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminReviewServiceImpl (DELETE /api/admin/reviews/{id})")
class AdminReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AdminReviewServiceImpl adminReviewService;

    private UUID adminId;
    private User adminUser;
    private UUID reviewId;
    private Review sampleReview;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setEmail("admin@techhub.com");
        adminUser.setRoles(Set.of(Role.ADMIN));

        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        reviewId = UUID.randomUUID();
        sampleReview = Review.builder()
                .id(reviewId)
                .rating(1)
                .comment("Nội dung đánh giá chứa từ ngữ xúc phạm")
                .mediaList(new ArrayList<>())
                .createdAt(LocalDateTime.now().minusDays(1))
                .deletedAt(null)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-106: deleteReview_Success_HappyPath - Admin xóa mềm bài đánh giá vi phạm thành công")
    void deleteReview_Success_HappyPath() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));

        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-107: deleteReview_Failed_ReviewNotFound - Ném REVIEW_NOT_FOUND khi ID không tồn tại")
    void deleteReview_Failed_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminReviewService.deleteReview(reviewId)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, never()).softDeleteReview(any(), any());
    }

    @Test
    @DisplayName("TC-108: deleteReview_Failed_ReviewAlreadyDeleted - Ném REVIEW_NOT_FOUND khi bài đánh giá đã bị xóa mềm trước đó")
    void deleteReview_Failed_ReviewAlreadyDeleted() {
        sampleReview.setDeletedAt(LocalDateTime.now().minusHours(2));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminReviewService.deleteReview(reviewId)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, never()).softDeleteReview(any(), any());
    }

    @Test
    @DisplayName("TC-109: deleteReview_Failed_ConcurrentDeleteConflict - Xung đột cập nhật đồng thời (Race Condition) ném REVIEW_NOT_FOUND")
    void deleteReview_Failed_ConcurrentDeleteConflict() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminReviewService.deleteReview(reviewId)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-110: deleteReview_Success_WithMediaList - Xóa mềm thành công bài đánh giá có đính kèm ảnh/video")
    void deleteReview_Success_WithMediaList() {
        ReviewMedia media = ReviewMedia.builder()
                .id(UUID.randomUUID())
                .review(sampleReview)
                .mediaUrl("https://cdn.techhub.com/spam.jpg")
                .mediaType(ReviewMediaType.IMAGE)
                .displayOrder(0)
                .build();
        sampleReview.getMediaList().add(media);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));
        assertEquals(1, sampleReview.getMediaList().size());
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-111: deleteReview_Success_WithSellerResponse - Xóa mềm thành công review đã có seller response")
    void deleteReview_Success_WithSellerResponse() {
        sampleReview.setSellerResponse("Cảm ơn quý khách đã phản hồi.");
        sampleReview.setSellerResponseAt(LocalDateTime.now().minusHours(5));

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));
        assertNotNull(sampleReview.getSellerResponse());
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-112: deleteReview_Success_WithHelpfulVotes - Xóa mềm review đã nhận nhiều vote hữu ích")
    void deleteReview_Success_WithHelpfulVotes() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-113: deleteReview_Success_AuditLogging - Kiểm tra ghi log AUDIT thành công")
    void deleteReview_Success_AuditLogging() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);

        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));
        verify(reviewRepository, times(1)).softDeleteReview(eq(reviewId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC-114: deleteReview_TransactionRollback_OnException - Kiểm tra cấu hình @Transactional(rollbackFor = Exception.class)")
    void deleteReview_TransactionRollback_OnException() throws NoSuchMethodException {
        Method method = AdminReviewServiceImpl.class.getMethod("deleteReview", UUID.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "deleteReview method phải có @Transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                "Phải cấu hình rollbackFor = Exception.class");
    }

    @Test
    @DisplayName("TC-115: deleteReview_Idempotency_SecondCallThrows404 - Gọi xóa lần thứ 2 ném 404 bảo toàn tính bất biến")
    void deleteReview_Idempotency_SecondCallThrows404() {
        // Lần 1: Thành công
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.softDeleteReview(eq(reviewId), any(LocalDateTime.class))).thenReturn(1);
        assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));

        // Giả lập trạng thái sau khi đã xóa: sampleReview có deletedAt != null
        sampleReview.setDeletedAt(LocalDateTime.now());

        // Lần 2: Ném REVIEW_NOT_FOUND (HTTP 404)
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminReviewService.deleteReview(reviewId)
        );
        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-119: deleteReview_Failed_Unauthenticated - Ném UNAUTHENTICATED khi không có phiên đăng nhập")
    void deleteReview_Failed_Unauthenticated() {
        SecurityContextHolder.clearContext();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminReviewService.deleteReview(reviewId)
        );

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(reviewRepository, never()).findById(any());
        verify(reviewRepository, never()).softDeleteReview(any(), any());
    }
}
