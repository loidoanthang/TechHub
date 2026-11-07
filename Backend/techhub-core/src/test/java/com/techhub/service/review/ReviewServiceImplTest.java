package com.techhub.service.review;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.review.CreateReviewRequest;
import com.techhub.model.dto.request.review.ReviewMediaRequest;
import com.techhub.model.dto.request.review.UpdateReviewRequest;
import com.techhub.model.dto.response.review.HelpfulVoteResponse;
import com.techhub.model.dto.response.review.ReviewResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.OrderStatus;
import com.techhub.model.enums.ReviewMediaType;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.*;
import com.techhub.service.review.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ReviewServiceImpl (POST /api/buyer/reviews)")
class ReviewServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMediaRepository reviewMediaRepository;

    @Mock
    private ReviewHelpfulRepository reviewHelpfulRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID userId;
    private User activeUser;
    private UUID orderItemId;
    private Order completedOrder;
    private OrderItem orderItem;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User();
        activeUser.setId(userId);
        activeUser.setFirstName("Van");
        activeUser.setLastName("Nguyen");
        activeUser.setEmail("buyer@techhub.com");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setAvatarUrl("https://cdn.techhub.com/avatars/user.png");

        completedOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("TH-ORD-123456")
                .user(activeUser)
                .status(OrderStatus.COMPLETED)
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Laptop ASUS Zenbook 14")
                .build();

        variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .variantName("Space Gray / 16GB / 512GB")
                .sku("ZENBOOK-14-SG")
                .build();

        orderItemId = UUID.randomUUID();
        orderItem = OrderItem.builder()
                .id(orderItemId)
                .order(completedOrder)
                .productVariant(variant)
                .productNameSnapshot("Laptop ASUS Zenbook 14")
                .variantNameSnapshot("Space Gray / 16GB / 512GB")
                .imageUrlSnapshot("https://cdn.techhub.com/products/cover.png")
                .originalPrice(BigDecimal.valueOf(25000000))
                .quantity(1)
                .build();
    }

    @Test
    @DisplayName("TC-01: createReview_Success_RatingAndCommentOnly - Đánh giá thành công chỉ với số sao và bình luận")
    void createReview_Success_RatingAndCommentOnly() {
        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                5,
                "Sản phẩm rất tốt, đóng gói kỹ càng!",
                null
        );

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItem(orderItem)
                .user(activeUser)
                .rating(5)
                .comment("Sản phẩm rất tốt, đóng gói kỹ càng!")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(userId, request);

        assertNotNull(response);
        assertEquals(savedReview.getId(), response.id());
        assertEquals(orderItemId, response.orderItemId());
        assertEquals(5, response.rating());
        assertEquals("Sản phẩm rất tốt, đóng gói kỹ càng!", response.comment());
        assertEquals("Laptop ASUS Zenbook 14", response.productName());
        assertEquals("V*** N", response.reviewerName());
        assertEquals(0, response.helpfulCount());
        assertTrue(response.mediaList().isEmpty());

        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewMediaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-02: createReview_Success_WithMediaAttachments - Đánh giá thành công kèm tệp đa phương tiện ảnh/video")
    void createReview_Success_WithMediaAttachments() {
        List<ReviewMediaRequest> mediaRequests = List.of(
                new ReviewMediaRequest("https://cdn.techhub.com/reviews/img1.png", ReviewMediaType.IMAGE, 0),
                new ReviewMediaRequest("https://cdn.techhub.com/reviews/video1.mp4", ReviewMediaType.VIDEO, 1)
        );

        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                4,
                "Máy đẹp, màn hình 120Hz mượt",
                mediaRequests
        );

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItem(orderItem)
                .user(activeUser)
                .rating(4)
                .comment("Máy đẹp, màn hình 120Hz mượt")
                .createdAt(LocalDateTime.now())
                .build();

        List<ReviewMedia> savedMedia = List.of(
                ReviewMedia.builder()
                        .id(UUID.randomUUID())
                        .review(savedReview)
                        .mediaUrl("https://cdn.techhub.com/reviews/img1.png")
                        .mediaType(ReviewMediaType.IMAGE)
                        .displayOrder(0)
                        .build(),
                ReviewMedia.builder()
                        .id(UUID.randomUUID())
                        .review(savedReview)
                        .mediaUrl("https://cdn.techhub.com/reviews/video1.mp4")
                        .mediaType(ReviewMediaType.VIDEO)
                        .displayOrder(1)
                        .build()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewMediaRepository.saveAll(any())).thenReturn(savedMedia);

        ReviewResponse response = reviewService.createReview(userId, request);

        assertNotNull(response);
        assertEquals(2, response.mediaList().size());
        assertEquals("https://cdn.techhub.com/reviews/img1.png", response.mediaList().get(0).mediaUrl());
        assertEquals(ReviewMediaType.IMAGE, response.mediaList().get(0).mediaType());
        assertEquals("https://cdn.techhub.com/reviews/video1.mp4", response.mediaList().get(1).mediaUrl());
        assertEquals(ReviewMediaType.VIDEO, response.mediaList().get(1).mediaType());

        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewMediaRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("TC-03: createReview_Success_RatingOnly_NoComment - Đánh giá thành công khi chỉ chấm số sao và không ghi chữ")
    void createReview_Success_RatingOnly_NoComment() {
        CreateReviewRequest request = new CreateReviewRequest(
                orderItemId,
                5,
                null,
                null
        );

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItem(orderItem)
                .user(activeUser)
                .rating(5)
                .comment(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(userId, request);

        assertNotNull(response);
        assertNull(response.comment());
        assertEquals(5, response.rating());
    }

    @Test
    @DisplayName("TC-04: createReview_Failed_OrderNotCompleted_Shipping - Ném ORDER_NOT_COMPLETED khi đơn đang giao SHIPPING")
    void createReview_Failed_OrderNotCompleted_Shipping() {
        completedOrder.setStatus(OrderStatus.SHIPPING);

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_NOT_COMPLETED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-05: createReview_Failed_OrderNotCompleted_Delivered - Ném ORDER_NOT_COMPLETED khi đơn DELIVERED chưa chuyển COMPLETED")
    void createReview_Failed_OrderNotCompleted_Delivered() {
        completedOrder.setStatus(OrderStatus.DELIVERED);

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_NOT_COMPLETED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-06: createReview_Failed_OrderNotCompleted_ReturnRequested - Ném ORDER_NOT_COMPLETED khi đơn có khiếu nại RETURN_REQUESTED")
    void createReview_Failed_OrderNotCompleted_ReturnRequested() {
        completedOrder.setStatus(OrderStatus.RETURN_REQUESTED);

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 1, "Hỏng hàng", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_NOT_COMPLETED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-07: createReview_Failed_AlreadyReviewed - Ném ORDER_ITEM_ALREADY_REVIEWED khi OrderItem đã được đánh giá")
    void createReview_Failed_AlreadyReviewed() {
        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(true); // Đã có review

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_ITEM_ALREADY_REVIEWED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08: createReview_Failed_IDOR_BelongsToAnotherBuyer - Ném ORDER_ITEM_NOT_FOUND khi OrderItem thuộc về Buyer khác")
    void createReview_Failed_IDOR_BelongsToAnotherBuyer() {
        User otherBuyer = new User();
        otherBuyer.setId(UUID.randomUUID());
        completedOrder.setUser(otherBuyer); // Thuộc về buyer khác

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_ITEM_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09: createReview_Failed_OrderItemNotFound - Ném ORDER_ITEM_NOT_FOUND khi OrderItem không tồn tại")
    void createReview_Failed_OrderItemNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(nonExistentId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(nonExistentId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ORDER_ITEM_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-10: createReview_Failed_UserNotFound - Ném USER_NOT_FOUND khi không tìm thấy Buyer")
    void createReview_Failed_UserNotFound() {
        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(orderItemRepository, never()).findWithOrderAndProductById(any());
    }

    @Test
    @DisplayName("TC-11: createReview_Failed_UserBanned - Ném ACCESS_DENIED khi Buyer có status = BANNED")
    void createReview_Failed_UserBanned() {
        User bannedUser = new User();
        bannedUser.setId(userId);
        bannedUser.setStatus(UserStatus.BANNED);

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(bannedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(orderItemRepository, never()).findWithOrderAndProductById(any());
    }

    @Test
    @DisplayName("TC-12: createReview_Failed_UserSuspended - Ném ACCESS_DENIED khi Buyer có status = SUSPENDED")
    void createReview_Failed_UserSuspended() {
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(orderItemRepository, never()).findWithOrderAndProductById(any());
    }

    @Test
    @DisplayName("TC-13: createReview_Failed_MediaLimitExceeded - Ném REVIEW_MEDIA_LIMIT_EXCEEDED khi gửi hơn 5 tệp")
    void createReview_Failed_MediaLimitExceeded() {
        List<ReviewMediaRequest> sixMedia = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixMedia.add(new ReviewMediaRequest("https://cdn.techhub.com/reviews/file" + i + ".png", ReviewMediaType.IMAGE, i));
        }

        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Good", sixMedia);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(orderItemRepository.findWithOrderAndProductById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.createReview(userId, request)
        );

        assertEquals(ErrorCode.REVIEW_MEDIA_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // API 2: PUT /api/buyer/reviews/{id} Unit Tests (TC-17 -> TC-29)
    // =========================================================================

    @Test
    @DisplayName("TC-17: updateReview_Success_RatingAndCommentOnly - Cập nhật rating và comment thành công không kèm media")
    void updateReview_Success_RatingAndCommentOnly() {
        UUID reviewId = UUID.randomUUID();
        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(3)
                .comment("Bình thường")
                .createdAt(LocalDateTime.now().minusDays(5))
                .mediaList(new ArrayList<>())
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Dùng thêm vài hôm thấy khá tốt", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(2L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertEquals(reviewId, response.id());
        assertEquals(4, response.rating());
        assertEquals("Dùng thêm vài hôm thấy khá tốt", response.comment());
        assertEquals(2L, response.helpfulCount());
        assertTrue(response.mediaList().isEmpty());

        verify(reviewRepository, times(1)).save(existingReview);
        verify(reviewHelpfulRepository, times(1)).countByReviewId(reviewId);
    }

    @Test
    @DisplayName("TC-18: updateReview_Success_WithMediaAttachments - Cập nhật thành công kèm thay thế tệp media mới")
    void updateReview_Success_WithMediaAttachments() {
        UUID reviewId = UUID.randomUUID();
        List<ReviewMedia> oldMedia = new ArrayList<>();
        oldMedia.add(ReviewMedia.builder().id(UUID.randomUUID()).mediaUrl("https://cdn.techhub.com/old.png").mediaType(ReviewMediaType.IMAGE).displayOrder(0).build());

        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(4)
                .comment("Hài lòng")
                .createdAt(LocalDateTime.now().minusDays(2))
                .mediaList(oldMedia)
                .build();

        List<ReviewMediaRequest> newMedia = List.of(
                new ReviewMediaRequest("https://cdn.techhub.com/new1.png", ReviewMediaType.IMAGE, 0),
                new ReviewMediaRequest("https://cdn.techhub.com/new2.mp4", ReviewMediaType.VIDEO, 1)
        );
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Rất hài lòng sau cập nhật", newMedia);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(0L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertEquals(5, response.rating());
        assertEquals(2, response.mediaList().size());
        assertEquals("https://cdn.techhub.com/new1.png", response.mediaList().get(0).mediaUrl());
        assertEquals(ReviewMediaType.IMAGE, response.mediaList().get(0).mediaType());
        assertEquals("https://cdn.techhub.com/new2.mp4", response.mediaList().get(1).mediaUrl());
        assertEquals(ReviewMediaType.VIDEO, response.mediaList().get(1).mediaType());

        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    @DisplayName("TC-19: updateReview_Success_Boundary_AtDay30 - Thành công tại đúng biên ngày thứ 30")
    void updateReview_Success_Boundary_AtDay30() {
        UUID reviewId = UUID.randomUUID();
        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(2)
                .comment("Sản phẩm lỗi nhẹ")
                .createdAt(LocalDateTime.now().minusDays(30))
                .mediaList(new ArrayList<>())
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(3, "Shop đã hỗ trợ đổi trả", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(1L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertEquals(3, response.rating());
        assertEquals("Shop đã hỗ trợ đổi trả", response.comment());
        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    @DisplayName("TC-20: updateReview_Success_SellerResponsePreserved - Bảo toàn phản hồi của người bán (sellerResponse) khi sửa")
    void updateReview_Success_SellerResponsePreserved() {
        UUID reviewId = UUID.randomUUID();
        LocalDateTime sellerTime = LocalDateTime.now().minusDays(1);
        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(3)
                .comment("Cần cải thiện đóng gói")
                .sellerResponse("Cảm ơn bạn, Shop sẽ khắc phục ngay ạ!")
                .sellerResponseAt(sellerTime)
                .createdAt(LocalDateTime.now().minusDays(10))
                .mediaList(new ArrayList<>())
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(4, "Đã được shop tư vấn nhiệt tình", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(3L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertEquals("Cảm ơn bạn, Shop sẽ khắc phục ngay ạ!", response.sellerResponse());
        assertEquals(sellerTime, response.sellerResponseAt());
        assertEquals(4, response.rating());
    }

    @Test
    @DisplayName("TC-21: updateReview_Success_ClearMedia - Xóa toàn bộ ảnh/video cũ khi request gửi mediaList rỗng")
    void updateReview_Success_ClearMedia() {
        UUID reviewId = UUID.randomUUID();
        List<ReviewMedia> oldMedia = new ArrayList<>();
        oldMedia.add(ReviewMedia.builder().id(UUID.randomUUID()).mediaUrl("https://cdn.techhub.com/old.png").build());

        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(4)
                .comment("Bình thường")
                .createdAt(LocalDateTime.now().minusDays(1))
                .mediaList(oldMedia)
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Đánh giá lại không cần ảnh", Collections.emptyList());

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(0L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertTrue(response.mediaList().isEmpty());
        assertTrue(existingReview.getMediaList().isEmpty());
    }

    @Test
    @DisplayName("TC-22: updateReview_Failed_UserNotFound - Ném USER_NOT_FOUND khi user không tồn tại")
    void updateReview_Failed_UserNotFound() {
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Comment", null);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-23: updateReview_Failed_UserSuspended - Ném ACCESS_DENIED khi Buyer có status != ACTIVE")
    void updateReview_Failed_UserSuspended() {
        UUID reviewId = UUID.randomUUID();
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Comment", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-24: updateReview_Failed_ReviewNotFound - Ném REVIEW_NOT_FOUND khi reviewId không tồn tại")
    void updateReview_Failed_ReviewNotFound() {
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Comment", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-25: updateReview_Failed_ReviewSoftDeleted - Ném REVIEW_NOT_FOUND khi review đã bị soft-deleted")
    void updateReview_Failed_ReviewSoftDeleted() {
        UUID reviewId = UUID.randomUUID();
        Review softDeletedReview = Review.builder()
                .id(reviewId)
                .user(activeUser)
                .deletedAt(LocalDateTime.now().minusHours(2))
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Comment", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(softDeletedReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-26: updateReview_Failed_OwnershipMismatch_IDOR - Ném REVIEW_NOT_FOUND khi sửa review của user khác (chống IDOR)")
    void updateReview_Failed_OwnershipMismatch_IDOR() {
        UUID reviewId = UUID.randomUUID();
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());

        Review otherReview = Review.builder()
                .id(reviewId)
                .user(anotherUser)
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Comment", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(otherReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-27: updateReview_Failed_EditWindowExpired_Day31 - Ném REVIEW_EDIT_WINDOW_EXPIRED khi quá 30 ngày (ngày thứ 31)")
    void updateReview_Failed_EditWindowExpired_Day31() {
        UUID reviewId = UUID.randomUUID();
        Review expiredReview = Review.builder()
                .id(reviewId)
                .user(activeUser)
                .rating(5)
                .comment("Ban đầu rất tốt")
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(1, "Sau 1 tháng dùng bị hỏng", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(expiredReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_EDIT_WINDOW_EXPIRED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-28: updateReview_Failed_MediaLimitExceeded - Ném REVIEW_MEDIA_LIMIT_EXCEEDED khi gửi hơn 5 tệp")
    void updateReview_Failed_MediaLimitExceeded() {
        UUID reviewId = UUID.randomUUID();
        Review existingReview = Review.builder()
                .id(reviewId)
                .user(activeUser)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        List<ReviewMediaRequest> sixMedia = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixMedia.add(new ReviewMediaRequest("https://cdn.techhub.com/file" + i + ".png", ReviewMediaType.IMAGE, i));
        }

        UpdateReviewRequest request = new UpdateReviewRequest(5, "Quá nhiều ảnh", sixMedia);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.updateReview(userId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_MEDIA_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-29: updateReview_CommentTrimmed - Tự động loại bỏ khoảng trắng thừa đầu cuối của comment")
    void updateReview_CommentTrimmed() {
        UUID reviewId = UUID.randomUUID();
        Review existingReview = Review.builder()
                .id(reviewId)
                .orderItem(orderItem)
                .user(activeUser)
                .rating(3)
                .comment("Cũ")
                .createdAt(LocalDateTime.now().minusDays(3))
                .mediaList(new ArrayList<>())
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest(4, "   Nội dung có space thừa   ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(0L);

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);

        assertNotNull(response);
        assertEquals("Nội dung có space thừa", response.comment());
        assertEquals("Nội dung có space thừa", existingReview.getComment());
    }

    // =========================================================================
    // API 3: POST /api/reviews/{id}/helpful Unit Tests (TC-32 -> TC-40)
    // =========================================================================

    @Test
    @DisplayName("TC-32: toggleHelpful_Success_Vote - Bình chọn hữu ích thành công lần đầu (+1, isHelpful = true)")
    void toggleHelpful_Success_Vote() {
        UUID reviewId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());

        Review review = Review.builder()
                .id(reviewId)
                .user(author)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewHelpfulRepository.findByReviewIdAndUserId(reviewId, userId)).thenReturn(Optional.empty());
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(1L);

        HelpfulVoteResponse response = reviewService.toggleHelpfulVote(userId, reviewId);

        assertNotNull(response);
        assertEquals(reviewId, response.reviewId());
        assertTrue(response.isHelpful());
        assertEquals(1L, response.helpfulCount());

        verify(reviewHelpfulRepository, times(1)).save(any(ReviewHelpful.class));
        verify(reviewHelpfulRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-33: toggleHelpful_Success_Unvote - Hủy bình chọn hữu ích khi bấm lại lần hai (-1, isHelpful = false)")
    void toggleHelpful_Success_Unvote() {
        UUID reviewId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());

        Review review = Review.builder()
                .id(reviewId)
                .user(author)
                .build();

        ReviewHelpful existingVote = ReviewHelpful.builder()
                .id(UUID.randomUUID())
                .review(review)
                .user(activeUser)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewHelpfulRepository.findByReviewIdAndUserId(reviewId, userId)).thenReturn(Optional.of(existingVote));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(0L);

        HelpfulVoteResponse response = reviewService.toggleHelpfulVote(userId, reviewId);

        assertNotNull(response);
        assertEquals(reviewId, response.reviewId());
        assertFalse(response.isHelpful());
        assertEquals(0L, response.helpfulCount());

        verify(reviewHelpfulRepository, times(1)).delete(existingVote);
        verify(reviewHelpfulRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-34: toggleHelpful_Failed_CannotVoteOwnReview - Ném CANNOT_VOTE_OWN_REVIEW khi tác giả tự vote bài mình")
    void toggleHelpful_Failed_CannotVoteOwnReview() {
        UUID reviewId = UUID.randomUUID();
        Review ownReview = Review.builder()
                .id(reviewId)
                .user(activeUser)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(ownReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.CANNOT_VOTE_OWN_REVIEW, ex.getErrorCode());
        verify(reviewHelpfulRepository, never()).save(any());
        verify(reviewHelpfulRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-35: toggleHelpful_Failed_ReviewNotFound - Ném REVIEW_NOT_FOUND khi reviewId không tồn tại")
    void toggleHelpful_Failed_ReviewNotFound() {
        UUID reviewId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewHelpfulRepository, never()).findByReviewIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("TC-36: toggleHelpful_Failed_ReviewSoftDeleted - Ném REVIEW_NOT_FOUND khi bài đánh giá đã bị xóa mềm")
    void toggleHelpful_Failed_ReviewSoftDeleted() {
        UUID reviewId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());

        Review softDeletedReview = Review.builder()
                .id(reviewId)
                .user(author)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(softDeletedReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewHelpfulRepository, never()).findByReviewIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("TC-37: toggleHelpful_Failed_UserNotFound - Ném USER_NOT_FOUND khi userId không tồn tại")
    void toggleHelpful_Failed_UserNotFound() {
        UUID reviewId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC-38: toggleHelpful_Failed_UserSuspended - Ném ACCESS_DENIED khi tài khoản có status = SUSPENDED")
    void toggleHelpful_Failed_UserSuspended() {
        UUID reviewId = UUID.randomUUID();
        User suspendedUser = new User();
        suspendedUser.setId(userId);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC-39: toggleHelpful_Failed_UserBanned - Ném ACCESS_DENIED khi tài khoản có status = BANNED")
    void toggleHelpful_Failed_UserBanned() {
        UUID reviewId = UUID.randomUUID();
        User bannedUser = new User();
        bannedUser.setId(userId);
        bannedUser.setStatus(UserStatus.BANNED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(bannedUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                reviewService.toggleHelpfulVote(userId, reviewId)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC-40: toggleHelpful_Concurrency_DataIntegrityViolationHandled - Bắt DataIntegrityViolationException khi 2 luồng cùng vote")
    void toggleHelpful_Concurrency_DataIntegrityViolationHandled() {
        UUID reviewId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());

        Review review = Review.builder()
                .id(reviewId)
                .user(author)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewHelpfulRepository.findByReviewIdAndUserId(reviewId, userId)).thenReturn(Optional.empty());
        when(reviewHelpfulRepository.save(any(ReviewHelpful.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(1L);

        HelpfulVoteResponse response = reviewService.toggleHelpfulVote(userId, reviewId);

        assertNotNull(response);
        assertEquals(reviewId, response.reviewId());
        assertTrue(response.isHelpful());
        assertEquals(1L, response.helpfulCount());
    }
}
