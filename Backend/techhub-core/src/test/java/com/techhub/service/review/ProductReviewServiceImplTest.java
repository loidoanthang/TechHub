package com.techhub.service.review;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.review.ProductRatingSummaryResponse;
import com.techhub.model.dto.response.review.ProductReviewResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.ReviewMediaType;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ReviewHelpfulRepository;
import com.techhub.repository.ReviewRepository;
import com.techhub.service.review.impl.ProductReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ProductReviewServiceImpl (GET /api/products/{id}/reviews)")
class ProductReviewServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewHelpfulRepository reviewHelpfulRepository;

    @InjectMocks
    private ProductReviewServiceImpl productReviewService;

    private UUID productId;
    private Product product;
    private Review sampleReview;
    private User buyer;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = Product.builder()
                .id(productId)
                .name("Bàn phím cơ TechHub Pro X")
                .build();

        buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setFirstName("Nguyen");
        buyer.setLastName("Van An");

        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .variantName("Space Gray / Brown Switch")
                .sku("TH-KB-PROX-GR")
                .build();

        orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .productVariant(variant)
                .productNameSnapshot("Bàn phím cơ TechHub Pro X")
                .variantNameSnapshot("Space Gray / Brown Switch")
                .imageUrlSnapshot("https://cdn.techhub.com/kb.jpg")
                .build();

        sampleReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItem(orderItem)
                .user(buyer)
                .rating(5)
                .comment("Bàn phím dùng rất thích, gõ đầm tay")
                .mediaList(new ArrayList<>())
                .sellerResponse("Cảm ơn quý khách!")
                .sellerResponseAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    @DisplayName("TC-76: getProductReviews_Success_DefaultPagination - Khách vãng lai xem đánh giá mặc định (page 0, size 10)")
    void getProductReviews_Success_DefaultPagination() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{sampleReview.getId(), 8L}));

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        ProductReviewResponse item = response.content().get(0);
        assertEquals(sampleReview.getId(), item.id());
        assertEquals(5, item.rating());
        assertEquals(8L, item.helpfulCount());
        assertFalse(item.hasVotedHelpful()); // anonymous user
        assertEquals("N*** V", item.reviewerName());
    }

    @Test
    @DisplayName("TC-77: getProductReviews_Success_FilterByRating - Lọc bài đánh giá 5 sao")
    void getProductReviews_Success_FilterByRating() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, 5, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(5, response.content().get(0).rating());
    }

    @Test
    @DisplayName("TC-78: getProductReviews_Success_FilterByHasMediaTrue - Lọc chỉ các đánh giá có ảnh/video")
    void getProductReviews_Success_FilterByHasMediaTrue() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        ReviewMedia media = ReviewMedia.builder()
                .id(UUID.randomUUID())
                .mediaUrl("https://cdn.techhub.com/pic1.jpg")
                .mediaType(ReviewMediaType.IMAGE)
                .displayOrder(0)
                .build();
        sampleReview.getMediaList().add(media);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, true, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.content().get(0).mediaList().size());
    }

    @Test
    @DisplayName("TC-79: getProductReviews_Success_SortByRatingDesc - Sắp xếp theo số sao giảm dần")
    void getProductReviews_Success_SortByRatingDesc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "rating", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-80: getProductReviews_Success_SortByRatingAsc - Sắp xếp theo số sao tăng dần")
    void getProductReviews_Success_SortByRatingAsc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "rating"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "rating", Sort.Direction.ASC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-81: getProductReviews_Success_SortByHelpfulDesc - Sắp xếp theo số lượt hữu ích nhiều nhất")
    void getProductReviews_Success_SortByHelpfulDesc() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{sampleReview.getId(), 25L}));

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "helpful", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(25L, response.content().get(0).helpfulCount());
    }

    @Test
    @DisplayName("TC-82: getProductReviews_Success_AuthenticatedUser_HasVotedTrue - Người dùng đã đăng nhập đã vote hữu ích")
    void getProductReviews_Success_AuthenticatedUser_HasVotedTrue() {
        UUID currentUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{sampleReview.getId(), 3L}));
        when(reviewHelpfulRepository.findVotedReviewIdsByUserIdAndReviewIds(eq(currentUserId), anyList()))
                .thenReturn(List.of(sampleReview.getId()));

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, currentUserId, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).hasVotedHelpful());
    }

    @Test
    @DisplayName("TC-83: getProductReviews_Success_AuthenticatedUser_HasVotedFalse - Người dùng đã đăng nhập chưa vote hữu ích")
    void getProductReviews_Success_AuthenticatedUser_HasVotedFalse() {
        UUID currentUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{sampleReview.getId(), 1L}));
        when(reviewHelpfulRepository.findVotedReviewIdsByUserIdAndReviewIds(eq(currentUserId), anyList()))
                .thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, currentUserId, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertFalse(response.content().get(0).hasVotedHelpful());
    }

    @Test
    @DisplayName("TC-84: getProductReviews_Success_AnonymousUser_HasVotedFalse - Khách vãng lai luôn có hasVotedHelpful = false")
    void getProductReviews_Success_AnonymousUser_HasVotedFalse() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertFalse(response.content().get(0).hasVotedHelpful());
        verify(reviewHelpfulRepository, never()).findVotedReviewIdsByUserIdAndReviewIds(any(), any());
    }

    @Test
    @DisplayName("TC-85: getProductReviews_Success_EmptyPage - Sản phẩm mới chưa có đánh giá nào")
    void getProductReviews_Success_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }

    @Test
    @DisplayName("TC-86: getProductReviews_Success_ExcludesSoftDeleted - Tự động loại trừ đánh giá đã xóa mềm")
    void getProductReviews_Success_ExcludesSoftDeleted() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        verify(reviewRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("TC-87: getProductReviews_Failed_ProductNotFound - Ném PRODUCT_NOT_FOUND khi sản phẩm không tồn tại")
    void getProductReviews_Failed_ProductNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productReviewService.getProductReviews(productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-87b: getProductReviews_Failed_ProductSoftDeleted - Ném PRODUCT_NOT_FOUND khi sản phẩm đã bị xóa mềm (is_deleted = true)")
    void getProductReviews_Failed_ProductSoftDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productReviewService.getProductReviews(productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-88: getProductReviews_MaskedReviewerName_Success - Che mờ họ tên khách hàng theo đúng chuẩn an toàn")
    void getProductReviews_MaskedReviewerName_Success() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        buyer.setFirstName("Tran");
        buyer.setLastName("Thi Bich");
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<ProductReviewResponse> response = productReviewService.getProductReviews(
                productId, null, null, null, "createdAt", Sort.Direction.DESC, pageable
        );

        assertNotNull(response);
        assertEquals("T*** T", response.content().get(0).reviewerName());
    }

    // =========================================================================
    // UNIT TESTS FOR GET /api/products/{id}/rating-summary (TC-91 to TC-103)
    // =========================================================================

    @Test
    @DisplayName("TC-91: getRatingSummary_Success_ProductWithMultipleReviews - Sản phẩm có nhiều đánh giá đầy đủ các mức sao và media")
    void getRatingSummary_Success_ProductWithMultipleReviews() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{153L, 4.82, 120L, 25L, 5L, 2L, 1L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(45L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(productId, summary.productId());
        assertEquals(4.8, summary.averageRating());
        assertEquals(153L, summary.totalReviews());
        assertEquals(45L, summary.mediaReviewCount());
        assertEquals(5, summary.ratingCounts().size());
        assertEquals(120L, summary.ratingCounts().get(5));
        assertEquals(25L, summary.ratingCounts().get(4));
        assertEquals(5L, summary.ratingCounts().get(3));
        assertEquals(2L, summary.ratingCounts().get(2));
        assertEquals(1L, summary.ratingCounts().get(1));
    }

    @Test
    @DisplayName("TC-92: getRatingSummary_Success_EmptyProduct_ZeroReviews - Sản phẩm mới chưa có đánh giá nào")
    void getRatingSummary_Success_EmptyProduct_ZeroReviews() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{0L, 0.0, 0L, 0L, 0L, 0L, 0L}));

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(productId, summary.productId());
        assertEquals(0.0, summary.averageRating());
        assertEquals(0L, summary.totalReviews());
        assertEquals(0L, summary.mediaReviewCount());
        assertEquals(5, summary.ratingCounts().size());
        assertEquals(0L, summary.ratingCounts().get(5));
        assertEquals(0L, summary.ratingCounts().get(1));
        verify(reviewRepository, never()).countMediaReviewsByProductId(any());
    }

    @Test
    @DisplayName("TC-93: getRatingSummary_Success_AllFiveStarReviews - Sản phẩm chỉ toàn đánh giá 5 sao")
    void getRatingSummary_Success_AllFiveStarReviews() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{20L, 5.0, 20L, 0L, 0L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(10L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(5.0, summary.averageRating());
        assertEquals(20L, summary.totalReviews());
        assertEquals(20L, summary.ratingCounts().get(5));
        assertEquals(0L, summary.ratingCounts().get(4));
        assertEquals(0L, summary.ratingCounts().get(1));
        assertEquals(10L, summary.mediaReviewCount());
    }

    @Test
    @DisplayName("TC-94: getRatingSummary_Success_RoundingHalfUp - Làm tròn điểm số trung bình chính xác 1 chữ số thập phân")
    void getRatingSummary_Success_RoundingHalfUp() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 4.85, 8L, 2L, 0L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(0L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(4.9, summary.averageRating());
    }

    @Test
    @DisplayName("TC-95: getRatingSummary_Success_ExcludesSoftDeletedReviews - Loại trừ các bài review đã xóa mềm")
    void getRatingSummary_Success_ExcludesSoftDeletedReviews() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{8L, 4.5, 5L, 2L, 1L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(2L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(8L, summary.totalReviews());
        assertEquals(4.5, summary.averageRating());
    }

    @Test
    @DisplayName("TC-96: getRatingSummary_Success_MultiVariantsAggregation - Tổng hợp đánh giá từ nhiều biến thể khác nhau")
    void getRatingSummary_Success_MultiVariantsAggregation() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{20L, 4.7, 15L, 4L, 1L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(5L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(20L, summary.totalReviews());
        assertEquals(4.7, summary.averageRating());
    }

    @Test
    @DisplayName("TC-97: getRatingSummary_Success_MediaReviewCount_Exact - Đếm chính xác số bài review có ảnh/video")
    void getRatingSummary_Success_MediaReviewCount_Exact() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 5.0, 5L, 0L, 0L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(2L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(2L, summary.mediaReviewCount());
    }

    @Test
    @DisplayName("TC-98: getRatingSummary_Success_MediaReviewCount_ZeroWhenNoMedia - Sản phẩm không có review nào chứa ảnh/video")
    void getRatingSummary_Success_MediaReviewCount_ZeroWhenNoMedia() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 4.0, 0L, 5L, 0L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(0L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(0L, summary.mediaReviewCount());
    }

    @Test
    @DisplayName("TC-99: getRatingSummary_Failed_ProductNotFound - Ném PRODUCT_NOT_FOUND khi sản phẩm không tồn tại")
    void getRatingSummary_Failed_ProductNotFound() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productReviewService.getProductRatingSummary(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).aggregateRatingSummaryByProductId(any());
        verify(reviewRepository, never()).countMediaReviewsByProductId(any());
    }

    @Test
    @DisplayName("TC-100: getRatingSummary_Failed_ProductSoftDeleted - Ném PRODUCT_NOT_FOUND khi sản phẩm đã bị xóa mềm")
    void getRatingSummary_Failed_ProductSoftDeleted() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                productReviewService.getProductRatingSummary(productId)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).aggregateRatingSummaryByProductId(any());
    }

    @Test
    @DisplayName("TC-101: getRatingSummary_Success_SingleReviewOneStar - Sản phẩm chỉ có đúng 1 đánh giá 1 sao")
    void getRatingSummary_Success_SingleReviewOneStar() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 1.0, 0L, 0L, 0L, 0L, 1L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(0L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(1.0, summary.averageRating());
        assertEquals(1L, summary.totalReviews());
        assertEquals(1L, summary.ratingCounts().get(1));
        assertEquals(0L, summary.ratingCounts().get(5));
    }

    @Test
    @DisplayName("TC-102: getRatingSummary_Success_GuaranteedAllStarKeys - Đảm bảo ratingCounts luôn chứa đầy đủ 5 key [1, 2, 3, 4, 5]")
    void getRatingSummary_Success_GuaranteedAllStarKeys() {
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(reviewRepository.aggregateRatingSummaryByProductId(productId))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 4.5, 5L, 5L, 0L, 0L, 0L}));
        when(reviewRepository.countMediaReviewsByProductId(productId)).thenReturn(1L);

        ProductRatingSummaryResponse summary = productReviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertTrue(summary.ratingCounts().containsKey(1));
        assertTrue(summary.ratingCounts().containsKey(2));
        assertTrue(summary.ratingCounts().containsKey(3));
        assertTrue(summary.ratingCounts().containsKey(4));
        assertTrue(summary.ratingCounts().containsKey(5));
    }

    @Test
    @DisplayName("TC-103: getRatingSummary_ReadOnlyTransaction_Integrity - Đảm bảo transaction chỉ đọc (readOnly = true)")
    void getRatingSummary_ReadOnlyTransaction_Integrity() throws NoSuchMethodException {
        java.lang.reflect.Method method = ProductReviewServiceImpl.class.getMethod("getProductRatingSummary", UUID.class);
        org.springframework.transaction.annotation.Transactional annotation =
                method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertNotNull(annotation, "Method getProductRatingSummary phải có @Transactional");
        assertTrue(annotation.readOnly(), "Transaction của getProductRatingSummary phải là readOnly = true");
    }
}
