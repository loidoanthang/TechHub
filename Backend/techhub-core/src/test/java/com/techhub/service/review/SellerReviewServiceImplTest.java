package com.techhub.service.review;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.review.SellerResponseRequest;
import com.techhub.model.dto.response.review.SellerReviewResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.ReviewHelpfulRepository;
import com.techhub.repository.ReviewRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.review.impl.SellerReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerReviewServiceImpl (GET /api/seller/reviews)")
class SellerReviewServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewHelpfulRepository reviewHelpfulRepository;

    @InjectMocks
    private SellerReviewServiceImpl sellerReviewService;

    private UUID sellerUserId;
    private User sellerUser;
    private Seller seller;
    private Shop shop;
    private UUID shopId;

    private User buyer;
    private Order order;
    private OrderItem orderItem;
    private Product product;
    private ProductVariant variant;
    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sellerUserId = UUID.randomUUID();
        sellerUser = new User();
        sellerUser.setId(sellerUserId);
        sellerUser.setEmail("seller@techhub.com");
        sellerUser.setStatus(UserStatus.ACTIVE);

        seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sellerUser)
                .sellerName("TechHub Official")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shopId = UUID.randomUUID();
        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("TechHub Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setFirstName("Van");
        buyer.setLastName("Nguyen");

        order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("TH-ORD-20261025-0012")
                .user(buyer)
                .shop(shop)
                .status(OrderStatus.COMPLETED)
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Laptop ASUS Zenbook 14")
                .build();

        variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .variantName("Xanh / 16GB / 1TB")
                .sku("ZENBOOK-14-BLU")
                .build();

        orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productVariant(variant)
                .productNameSnapshot("Laptop ASUS Zenbook 14")
                .variantNameSnapshot("Xanh / 16GB / 1TB")
                .imageUrlSnapshot("https://cdn.techhub.com/img.png")
                .originalPrice(BigDecimal.valueOf(25000000))
                .quantity(1)
                .build();

        sampleReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItem(orderItem)
                .user(buyer)
                .rating(5)
                .comment("Rất hài lòng")
                .createdAt(LocalDateTime.now())
                .mediaList(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("TC-46: getShopReviews_Success_DefaultPagination - Lấy danh sách đánh giá mặc định (page 0, size 10)")
    void getShopReviews_Success_DefaultPagination() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{sampleReview.getId(), 3L}));

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, null, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());

        SellerReviewResponse dto = response.content().get(0);
        assertEquals(sampleReview.getId(), dto.id());
        assertEquals("TH-ORD-20261025-0012", dto.orderNumber());
        assertEquals("Laptop ASUS Zenbook 14", dto.productName());
        assertEquals("ZENBOOK-14-BLU", dto.sku());
        assertEquals(5, dto.rating());
        assertEquals("V*** N", dto.reviewerName());
        assertEquals(3L, dto.helpfulCount());

        verify(sellerRepository, times(1)).findByUserId(sellerUserId);
        verify(shopRepository, times(1)).findBySellerId(seller.getId());
        verify(reviewRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("TC-47: getShopReviews_Success_FilterByRating - Lọc theo số sao (rating = 5)")
    void getShopReviews_Success_FilterByRating() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, 5, null, null, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(5, response.content().get(0).rating());
    }

    @Test
    @DisplayName("TC-48: getShopReviews_Success_FilterByHasRespondedFalse - Lọc các đánh giá chưa được phản hồi")
    void getShopReviews_Success_FilterByHasRespondedFalse() {
        Pageable pageable = PageRequest.of(0, 10);
        sampleReview.setSellerResponse(null);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, false, null, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertNull(response.content().get(0).sellerResponse());
    }

    @Test
    @DisplayName("TC-49: getShopReviews_Success_FilterByHasRespondedTrue - Lọc các đánh giá đã được phản hồi")
    void getShopReviews_Success_FilterByHasRespondedTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        sampleReview.setSellerResponse("Cảm ơn quý khách!");
        sampleReview.setSellerResponseAt(LocalDateTime.now());
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, true, null, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Cảm ơn quý khách!", response.content().get(0).sellerResponse());
    }

    @Test
    @DisplayName("TC-50: getShopReviews_Success_FilterByHasMedia - Lọc các đánh giá có ảnh/video đính kèm")
    void getShopReviews_Success_FilterByHasMedia() {
        Pageable pageable = PageRequest.of(0, 10);
        ReviewMedia media = ReviewMedia.builder()
                .id(UUID.randomUUID())
                .mediaUrl("https://cdn.techhub.com/img1.jpg")
                .mediaType(ReviewMediaType.IMAGE)
                .displayOrder(0)
                .build();
        sampleReview.getMediaList().add(media);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, true, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.content().get(0).mediaList().size());
    }

    @Test
    @DisplayName("TC-51: getShopReviews_Success_FilterByDateRange - Lọc đánh giá theo khoảng thời gian fromDate / toDate")
    void getShopReviews_Success_FilterByDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, null, null, null, fromDate, toDate, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-52: getShopReviews_Success_FilterByProductId - Lọc đánh giá theo productId cụ thể")
    void getShopReviews_Success_FilterByProductId() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID targetProductId = product.getId();
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, null, targetProductId, null, null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(targetProductId, response.content().get(0).productId());
    }

    @Test
    @DisplayName("TC-53: getShopReviews_Success_SearchKeyword - Tìm kiếm đánh giá theo từ khóa")
    void getShopReviews_Success_SearchKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> reviewPage = new PageImpl<>(List.of(sampleReview), pageable, 1);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(reviewPage);
        when(reviewHelpfulRepository.countByReviewIds(anyList())).thenReturn(Collections.emptyList());

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, null, null, "hài lòng", null, null, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-54: getShopReviews_Success_EmptyPage - Trả về mảng content rỗng khi Shop chưa có đánh giá nào")
    void getShopReviews_Success_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PageResponse<SellerReviewResponse> response = sellerReviewService.getShopReviews(
                sellerUserId, null, null, null, null, null, null, null, pageable
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verify(reviewHelpfulRepository, never()).countByReviewIds(any());
    }

    @Test
    @DisplayName("TC-55: getShopReviews_Failed_SellerNotFound - Ném SELLER_NOT_FOUND khi tài khoản không phải là Seller")
    void getShopReviews_Failed_SellerNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.getShopReviews(sellerUserId, null, null, null, null, null, null, null, pageable)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC-56: getShopReviews_Failed_SellerNotApproved - Ném NOT_A_SELLER khi Seller chưa được duyệt")
    void getShopReviews_Failed_SellerNotApproved() {
        Pageable pageable = PageRequest.of(0, 10);
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.getShopReviews(sellerUserId, null, null, null, null, null, null, null, pageable)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC-57: getShopReviews_Failed_ShopNotFound - Ném SHOP_NOT_FOUND khi Seller chưa tạo gian hàng")
    void getShopReviews_Failed_ShopNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.getShopReviews(sellerUserId, null, null, null, null, null, null, null, pageable)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("TC-58: getShopReviews_Failed_ShopBanned - Ném SHOP_NOT_ACTIVE khi Shop bị BANNED")
    void getShopReviews_Failed_ShopBanned() {
        Pageable pageable = PageRequest.of(0, 10);
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.getShopReviews(sellerUserId, null, null, null, null, null, null, null, pageable)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(reviewRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("TC-59: getShopReviews_Failed_InvalidDateRange - Ném INVALID_DATE_RANGE khi fromDate sau toDate")
    void getShopReviews_Failed_InvalidDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fromDate = LocalDate.now().plusDays(5);
        LocalDate toDate = LocalDate.now();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.getShopReviews(sellerUserId, null, null, null, null, null, fromDate, toDate, pageable)
        );

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    // =========================================================================
    // API 5: POST /api/seller/reviews/{id}/response (TC-62 đến TC-73)
    // =========================================================================

    @Test
    @DisplayName("TC-62: respondToReview_Success - Seller phản hồi thành công đánh giá hợp lệ")
    void respondToReview_Success() {
        UUID reviewId = sampleReview.getId();
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn đã mua sắm tại shop!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.updateSellerResponse(eq(reviewId), eq("Cảm ơn bạn đã mua sắm tại shop!"), any())).thenReturn(1);
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(5L);

        SellerReviewResponse response = sellerReviewService.respondToReview(sellerUserId, reviewId, request);

        assertNotNull(response);
        assertEquals("Cảm ơn bạn đã mua sắm tại shop!", response.sellerResponse());
        assertNotNull(response.sellerResponseAt());
        assertEquals(5L, response.helpfulCount());
        verify(reviewRepository, times(1)).updateSellerResponse(eq(reviewId), eq("Cảm ơn bạn đã mua sắm tại shop!"), any());
    }

    @Test
    @DisplayName("TC-63: respondToReview_Failed_ReviewNotFound - Review không tồn tại")
    void respondToReview_Failed_ReviewNotFound() {
        UUID reviewId = UUID.randomUUID();
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-64: respondToReview_Failed_ReviewSoftDeleted - Review đã bị xóa mềm")
    void respondToReview_Failed_ReviewSoftDeleted() {
        UUID reviewId = sampleReview.getId();
        sampleReview.setDeletedAt(LocalDateTime.now());
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(sampleReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-65: respondToReview_Failed_AlreadyResponded - Review đã có sellerResponse từ trước (BR-20.3)")
    void respondToReview_Failed_AlreadyResponded() {
        UUID reviewId = sampleReview.getId();
        sampleReview.setSellerResponse("Đã phản hồi trước đó");
        sampleReview.setSellerResponseAt(LocalDateTime.now().minusDays(1));
        SellerResponseRequest request = new SellerResponseRequest("Cố tình phản hồi lần 2");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(sampleReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_ALREADY_RESPONDED, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-66: respondToReview_Failed_IdorOtherShopReview - Review thuộc đơn hàng của Shop khác (IDOR)")
    void respondToReview_Failed_IdorOtherShopReview() {
        UUID reviewId = sampleReview.getId();
        Shop otherShop = Shop.builder().id(UUID.randomUUID()).build();
        sampleReview.getOrderItem().getOrder().setShop(otherShop);
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(sampleReview));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.REVIEW_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-67: respondToReview_Failed_SellerNotFound - Không tìm thấy hồ sơ Seller")
    void respondToReview_Failed_SellerNotFound() {
        UUID reviewId = UUID.randomUUID();
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-68: respondToReview_Failed_SellerNotApproved - Seller chưa được KYC duyệt")
    void respondToReview_Failed_SellerNotApproved() {
        UUID reviewId = UUID.randomUUID();
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-69: respondToReview_Failed_SellerUserBanned - Tài khoản Seller User bị BANNED")
    void respondToReview_Failed_SellerUserBanned() {
        UUID reviewId = UUID.randomUUID();
        sellerUser.setStatus(UserStatus.BANNED);
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-70: respondToReview_Failed_SellerUserSuspended - Tài khoản Seller User bị SUSPENDED")
    void respondToReview_Failed_SellerUserSuspended() {
        UUID reviewId = UUID.randomUUID();
        sellerUser.setStatus(UserStatus.SUSPENDED);
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-71: respondToReview_Failed_ShopNotFound - Seller chưa tạo Shop")
    void respondToReview_Failed_ShopNotFound() {
        UUID reviewId = UUID.randomUUID();
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-72: respondToReview_Failed_ShopBanned - Shop có trạng thái BANNED")
    void respondToReview_Failed_ShopBanned() {
        UUID reviewId = UUID.randomUUID();
        shop.setStatus(ShopStatus.BANNED);
        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn bạn!");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerReviewService.respondToReview(sellerUserId, reviewId, request)
        );

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(reviewRepository, never()).findWithDetailsById(any());
    }

    @Test
    @DisplayName("TC-73: respondToReview_Success_TrimWhitespaces - Cắt tỉa khoảng trắng đầu cuối của câu phản hồi")
    void respondToReview_Success_TrimWhitespaces() {
        UUID reviewId = sampleReview.getId();
        SellerResponseRequest request = new SellerResponseRequest("   Cảm ơn quý khách!   ");

        when(sellerRepository.findByUserId(sellerUserId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(reviewRepository.findWithDetailsById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.updateSellerResponse(eq(reviewId), eq("Cảm ơn quý khách!"), any())).thenReturn(1);
        when(reviewHelpfulRepository.countByReviewId(reviewId)).thenReturn(0L);

        SellerReviewResponse response = sellerReviewService.respondToReview(sellerUserId, reviewId, request);

        assertNotNull(response);
        assertEquals("Cảm ơn quý khách!", response.sellerResponse());
        verify(reviewRepository, times(1)).updateSellerResponse(eq(reviewId), eq("Cảm ơn quý khách!"), any());
    }
}
