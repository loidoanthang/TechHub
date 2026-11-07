package com.techhub.service.shop;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.FollowShopResponse;
import com.techhub.model.dto.response.FollowedShopResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.ShopFollowing;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopFollowingRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.shop.impl.ShopFollowingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ShopFollowingServiceImpl (Follow / Unfollow / GetFollowedShops)")
class ShopFollowingServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private ShopFollowingRepository shopFollowingRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ShopFollowingServiceImpl shopFollowingService;

    private UUID buyerId;
    private UUID sellerUserId;
    private UUID sellerId;
    private UUID shopId;

    private User sellerUser;
    private Seller seller;
    private Shop shop;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerUserId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        sellerUser = new User();
        sellerUser.setId(sellerUserId);
        sellerUser.setEmail("seller@techhub.com");
        sellerUser.setStatus(UserStatus.ACTIVE);
        sellerUser.setRoles(Set.of(Role.SELLER));

        seller = Seller.builder()
                .id(sellerId)
                .user(sellerUser)
                .sellerName("anker-official")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("Anker Official Store")
                .status(ShopStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC-01: followShop_Success_FirstTime - Buyer theo dõi shop lần đầu thành công")
    void followShop_Success_FirstTime() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.findByUserIdAndShopId(buyerId, shopId)).thenReturn(Optional.empty());

        ShopFollowing savedFollowing = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();
        when(shopFollowingRepository.saveAndFlush(any(ShopFollowing.class))).thenReturn(savedFollowing);
        when(shopFollowingRepository.countByShopId(shopId)).thenReturn(1L);

        FollowShopResponse response = shopFollowingService.followShop(buyerId, "anker-official");

        assertNotNull(response);
        assertEquals(shopId, response.getShopId());
        assertEquals("Anker Official Store", response.getShopName());
        assertEquals("anker-official", response.getSellerName());
        assertEquals(1L, response.getFollowerCount());
        assertTrue(response.getFollowing());

        verify(shopFollowingRepository, times(1)).saveAndFlush(any(ShopFollowing.class));
        verify(shopFollowingRepository, times(1)).countByShopId(shopId);
    }

    @Test
    @DisplayName("TC-02: followShop_Success_Idempotent_AlreadyFollowed - Đã theo dõi từ trước, trả về kết quả an toàn không chèn trùng")
    void followShop_Success_Idempotent_AlreadyFollowed() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        ShopFollowing existing = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        when(shopFollowingRepository.findByUserIdAndShopId(buyerId, shopId)).thenReturn(Optional.of(existing));
        when(shopFollowingRepository.countByShopId(shopId)).thenReturn(150L);

        FollowShopResponse response = shopFollowingService.followShop(buyerId, "anker-official");

        assertNotNull(response);
        assertEquals(shopId, response.getShopId());
        assertEquals(150L, response.getFollowerCount());
        assertTrue(response.getFollowing());
        assertEquals(existing.getCreatedAt(), response.getFollowedAt());

        verify(shopFollowingRepository, never()).saveAndFlush(any(ShopFollowing.class));
        verify(shopFollowingRepository, times(1)).countByShopId(shopId);
    }

    @Test
    @DisplayName("TC-03: followShop_Failed_AntiSelfFollow - Chủ shop tự theo dõi gian hàng mình bị chặn với CANNOT_FOLLOW_OWN_SHOP")
    void followShop_Failed_AntiSelfFollow() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // currentUserId trùng với seller.user.id
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(sellerUserId, "anker-official")
        );

        assertEquals(ErrorCode.CANNOT_FOLLOW_OWN_SHOP, ex.getErrorCode());
        verify(shopFollowingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC-04: followShop_Failed_ShopNotFound_NonExistentSlug - Slug không tồn tại ném SHOP_NOT_FOUND")
    void followShop_Failed_ShopNotFound_NonExistentSlug() {
        when(sellerRepository.findBySellerName("unknown-shop")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "unknown-shop")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-05: followShop_Failed_InvalidSlug_TooShort - Slug < 3 ký tự ném SHOP_NOT_FOUND")
    void followShop_Failed_InvalidSlug_TooShort() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "ab")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(sellerRepository);
    }

    @Test
    @DisplayName("TC-06: followShop_Failed_InvalidSlug_SpecialCharacters - Slug chứa ký tự đặc biệt ném SHOP_NOT_FOUND")
    void followShop_Failed_InvalidSlug_SpecialCharacters() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "shop@vip!")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(sellerRepository);
    }

    @Test
    @DisplayName("TC-07: followShop_Failed_InvalidSlug_NullOrBlank - Slug null hoặc khoảng trắng ném SHOP_NOT_FOUND")
    void followShop_Failed_InvalidSlug_NullOrBlank() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, null)
        );
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex1.getErrorCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "   ")
        );
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: followShop_Failed_SellerNotApproved_Pending - Seller chưa được duyệt ném SHOP_NOT_FOUND")
    void followShop_Failed_SellerNotApproved_Pending() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-09: followShop_Failed_SellerUserBanned - Tài khoản Seller bị khóa ném SHOP_NOT_FOUND")
    void followShop_Failed_SellerUserBanned() {
        sellerUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-10: followShop_Failed_ShopSuspended - Shop bị đình chỉ ném SHOP_NOT_FOUND")
    void followShop_Failed_ShopSuspended() {
        shop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11: followShop_Failed_ShopBanned - Shop bị cấm ném SHOP_NOT_FOUND")
    void followShop_Failed_ShopBanned() {
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.followShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: followShop_Success_ShopPaused - Shop đang tạm nghỉ bán vẫn cho phép follow")
    void followShop_Success_ShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.findByUserIdAndShopId(buyerId, shopId)).thenReturn(Optional.empty());

        ShopFollowing saved = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();
        when(shopFollowingRepository.saveAndFlush(any(ShopFollowing.class))).thenReturn(saved);
        when(shopFollowingRepository.countByShopId(shopId)).thenReturn(10L);

        FollowShopResponse response = shopFollowingService.followShop(buyerId, "anker-official");

        assertNotNull(response);
        assertEquals(shopId, response.getShopId());
        assertEquals(10L, response.getFollowerCount());
        assertTrue(response.getFollowing());
    }

    @Test
    @DisplayName("TC-13: followShop_Success_RaceCondition_Handled - Xử lý DataIntegrityViolationException khi 2 request cùng chèn")
    void followShop_Success_RaceCondition_Handled() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Lần đầu find trả về empty
        // Khi saveAndFlush thì ném DataIntegrityViolationException
        when(shopFollowingRepository.findByUserIdAndShopId(buyerId, shopId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ShopFollowing.builder()
                        .id(UUID.randomUUID())
                        .userId(buyerId)
                        .shop(shop)
                        .createdAt(LocalDateTime.now())
                        .build()));

        when(shopFollowingRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate key uk_shop_followings_user_shop"));
        when(shopFollowingRepository.countByShopId(shopId)).thenReturn(5L);

        FollowShopResponse response = shopFollowingService.followShop(buyerId, "anker-official");

        assertNotNull(response);
        assertEquals(shopId, response.getShopId());
        assertEquals(5L, response.getFollowerCount());
        assertTrue(response.getFollowing());
    }

    @Test
    @DisplayName("TC-14: followShop_FollowerCount_CalculatedAccurately - Tính toán followerCount chính xác sau khi follow")
    void followShop_FollowerCount_CalculatedAccurately() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.findByUserIdAndShopId(buyerId, shopId)).thenReturn(Optional.empty());

        ShopFollowing saved = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();
        when(shopFollowingRepository.saveAndFlush(any(ShopFollowing.class))).thenReturn(saved);
        when(shopFollowingRepository.countByShopId(shopId)).thenReturn(6L);

        FollowShopResponse response = shopFollowingService.followShop(buyerId, "anker-official");

        assertNotNull(response);
        assertEquals(6L, response.getFollowerCount());
    }

    // =========================================================================
    // TESTS FOR DELETE /api/shops/{sellerName}/follow (unfollowShop)
    // =========================================================================

    @Test
    @DisplayName("TC-01-UNFOLLOW: unfollowShop_Success_WhenCurrentlyFollowing - Hủy theo dõi thành công khi đang follow")
    void unfollowShop_Success_WhenCurrentlyFollowing() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.deleteByUserIdAndShopId(buyerId, shopId)).thenReturn(1);

        assertDoesNotThrow(() -> shopFollowingService.unfollowShop(buyerId, "anker-official"));

        verify(shopFollowingRepository, times(1)).deleteByUserIdAndShopId(buyerId, shopId);
    }

    @Test
    @DisplayName("TC-02-UNFOLLOW: unfollowShop_Success_Idempotent_WhenNotFollowing - Xử lý lũy thừa khi chưa từng follow (0 rows deleted)")
    void unfollowShop_Success_Idempotent_WhenNotFollowing() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.deleteByUserIdAndShopId(buyerId, shopId)).thenReturn(0);

        assertDoesNotThrow(() -> shopFollowingService.unfollowShop(buyerId, "anker-official"));

        verify(shopFollowingRepository, times(1)).deleteByUserIdAndShopId(buyerId, shopId);
    }

    @Test
    @DisplayName("TC-03-UNFOLLOW: unfollowShop_Success_Idempotent_DoubleCall - Gọi unfollow 2 lần liên tiếp đều thành công")
    void unfollowShop_Success_Idempotent_DoubleCall() {
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.deleteByUserIdAndShopId(buyerId, shopId)).thenReturn(1, 0);

        // Lần 1
        assertDoesNotThrow(() -> shopFollowingService.unfollowShop(buyerId, "anker-official"));
        // Lần 2
        assertDoesNotThrow(() -> shopFollowingService.unfollowShop(buyerId, "anker-official"));

        verify(shopFollowingRepository, times(2)).deleteByUserIdAndShopId(buyerId, shopId);
    }

    @Test
    @DisplayName("TC-04-UNFOLLOW: unfollowShop_Failed_ShopNotFound_NonExistentSlug - Slug không tồn tại ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_ShopNotFound_NonExistentSlug() {
        when(sellerRepository.findBySellerName("unknown-shop")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "unknown-shop")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(shopFollowingRepository);
    }

    @Test
    @DisplayName("TC-05-UNFOLLOW: unfollowShop_Failed_InvalidSlug_TooShort - Slug < 3 ký tự ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_InvalidSlug_TooShort() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "ab")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(sellerRepository);
    }

    @Test
    @DisplayName("TC-06-UNFOLLOW: unfollowShop_Failed_InvalidSlug_SpecialCharacters - Slug chứa ký tự đặc biệt ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_InvalidSlug_SpecialCharacters() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "shop@vip!")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(sellerRepository);
    }

    @Test
    @DisplayName("TC-07-UNFOLLOW: unfollowShop_Failed_InvalidSlug_NullOrBlank - Slug null hoặc khoảng trắng ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_InvalidSlug_NullOrBlank() {
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, null)
        );
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex1.getErrorCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "   ")
        );
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-08-UNFOLLOW: unfollowShop_Failed_SellerNotApproved_Pending - Seller chưa được duyệt ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_SellerNotApproved_Pending() {
        seller.setVerificationStatus(SellerVerificationStatus.PENDING);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-09-UNFOLLOW: unfollowShop_Failed_SellerUserBanned - Tài khoản Seller bị khóa ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_SellerUserBanned() {
        sellerUser.setStatus(UserStatus.BANNED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(shopRepository);
    }

    @Test
    @DisplayName("TC-10-UNFOLLOW: unfollowShop_Failed_ShopSuspended - Shop bị đình chỉ ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_ShopSuspended() {
        shop.setStatus(ShopStatus.SUSPENDED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-11-UNFOLLOW: unfollowShop_Failed_ShopBanned - Shop bị cấm ném SHOP_NOT_FOUND")
    void unfollowShop_Failed_ShopBanned() {
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shopFollowingService.unfollowShop(buyerId, "anker-official")
        );

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12-UNFOLLOW: unfollowShop_Success_ShopPaused - Shop đang tạm nghỉ bán vẫn cho phép unfollow")
    void unfollowShop_Success_ShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);
        when(sellerRepository.findBySellerName("anker-official")).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(shopFollowingRepository.deleteByUserIdAndShopId(buyerId, shopId)).thenReturn(1);

        assertDoesNotThrow(() -> shopFollowingService.unfollowShop(buyerId, "anker-official"));

        verify(shopFollowingRepository, times(1)).deleteByUserIdAndShopId(buyerId, shopId);
    }

    // =========================================================================
    // GET /api/buyer/followed-shops TESTS
    // =========================================================================

    @Test
    @DisplayName("TC-01: getFollowedShops_Success_WithData - Lấy danh sách shop đang theo dõi thành công")
    void getFollowedShops_Success_WithData() {
        // Arrange
        Shop shop2 = Shop.builder()
                .id(UUID.randomUUID())
                .name("Gear Haven Gaming")
                .avatarUrl("https://cdn.techhub.com/avatars/gearhaven.png")
                .bannerUrl("https://cdn.techhub.com/banners/gearhaven.png")
                .status(ShopStatus.PAUSED)
                .seller(Seller.builder().sellerName("gear-haven").build())
                .build();

        ShopFollowing sf1 = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        ShopFollowing sf2 = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop2)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf1, sf2), pageable, 2);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);

        List<Object[]> followerCounts = List.of(
                new Object[]{shop.getId(), 1250L},
                new Object[]{shop2.getId(), 340L}
        );
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(followerCounts);

        List<Object[]> productCounts = List.of(
                new Object[]{shop.getId(), 48L},
                new Object[]{shop2.getId(), 0L}
        );
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(productCounts);

        // Act
        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(2, response.content().size());

        FollowedShopResponse first = response.content().get(0);
        assertEquals(shop.getId(), first.getShopId());
        assertEquals("anker-official", first.getSellerName());
        assertEquals("Anker Official Store", first.getShopName());
        assertEquals(ShopStatus.ACTIVE, first.getStatus());
        assertFalse(first.getIsPaused());
        assertEquals(1250L, first.getFollowerCount());
        assertEquals(48L, first.getTotalProducts());
        assertEquals(sf1.getCreatedAt(), first.getFollowedAt());

        FollowedShopResponse second = response.content().get(1);
        assertEquals(shop2.getId(), second.getShopId());
        assertEquals("gear-haven", second.getSellerName());
        assertEquals(ShopStatus.PAUSED, second.getStatus());
        assertTrue(second.getIsPaused());
        assertEquals(340L, second.getFollowerCount());
        assertEquals(0L, second.getTotalProducts());
    }

    @Test
    @DisplayName("TC-02: getFollowedShops_Success_Pagination - Lấy trang thứ 2 thành công")
    void getFollowedShops_Success_Pagination() {
        ShopFollowing sf = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(1, 10);
        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf), pageable, 11);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(Collections.emptyList());
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 1, 10);

        assertNotNull(response);
        assertEquals(1, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertEquals(11, response.totalElements());
        assertEquals(2, response.totalPages());
        assertTrue(response.last());
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-03: getFollowedShops_Success_EmptyList - Buyer chưa theo dõi shop nào, trả về rỗng và không gọi count queries")
    void getFollowedShops_Success_EmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ShopFollowing> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        assertNotNull(response);
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.content().isEmpty());

        verify(shopFollowingRepository, never()).countFollowersByShopIdIn(anyList());
        verify(productRepository, never()).countActiveProductsByShopIdIn(anyList());
    }

    @Test
    @DisplayName("TC-04: getFollowedShops_Sanitize_NegativePage - page < 0 được chuẩn hóa về 0")
    void getFollowedShops_Sanitize_NegativePage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ShopFollowing> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(emptyPage);

        shopFollowingService.getFollowedShops(buyerId, -5, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shopFollowingRepository).findByUserIdWithShopAndSeller(eq(buyerId), captor.capture());

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("TC-05: getFollowedShops_Sanitize_ZeroOrNegativeSize - size <= 0 được chuẩn hóa về 10")
    void getFollowedShops_Sanitize_ZeroOrNegativeSize() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ShopFollowing> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(emptyPage);

        shopFollowingService.getFollowedShops(buyerId, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shopFollowingRepository).findByUserIdWithShopAndSeller(eq(buyerId), captor.capture());

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("TC-06: getFollowedShops_Sanitize_ExceedMaxSize - size > 50 được giới hạn trần về 50")
    void getFollowedShops_Sanitize_ExceedMaxSize() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<ShopFollowing> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(emptyPage);

        shopFollowingService.getFollowedShops(buyerId, 0, 100);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(shopFollowingRepository).findByUserIdWithShopAndSeller(eq(buyerId), captor.capture());

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("TC-07: getFollowedShops_Success_PageOutOfRange - Trang vượt quá totalPages trả về content rỗng an toàn")
    void getFollowedShops_Success_PageOutOfRange() {
        Pageable pageable = PageRequest.of(99, 10);
        Page<ShopFollowing> outOfRangePage = new PageImpl<>(Collections.emptyList(), pageable, 2);

        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(outOfRangePage);

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 99, 10);

        assertNotNull(response);
        assertEquals(2, response.totalElements());
        assertEquals(99, response.pageNumber());
        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("TC-08: getFollowedShops_CheckShopPaused - Shop có trạng thái PAUSED trả về isPaused = true")
    void getFollowedShops_CheckShopPaused() {
        shop.setStatus(ShopStatus.PAUSED);
        ShopFollowing sf = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();

        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf), PageRequest.of(0, 10), 1);
        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(Collections.emptyList());
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertTrue(response.content().get(0).getIsPaused());
        assertEquals(ShopStatus.PAUSED, response.content().get(0).getStatus());
    }

    @Test
    @DisplayName("TC-09: getFollowedShops_CountActiveProducts_Accurate - Đếm chính xác số lượng sản phẩm từ Map")
    void getFollowedShops_CountActiveProducts_Accurate() {
        ShopFollowing sf = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();

        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf), PageRequest.of(0, 10), 1);
        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(Collections.emptyList());
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(Collections.singletonList(new Object[]{shopId, 25L}));

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        assertEquals(25L, response.content().get(0).getTotalProducts());
    }

    @Test
    @DisplayName("TC-10: getFollowedShops_CountFollowers_Accurate - Đếm chính xác số lượng follower từ Map")
    void getFollowedShops_CountFollowers_Accurate() {
        ShopFollowing sf = ShopFollowing.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .shop(shop)
                .createdAt(LocalDateTime.now())
                .build();

        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf), PageRequest.of(0, 10), 1);
        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(Collections.singletonList(new Object[]{shopId, 500L}));
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(Collections.emptyList());

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        assertEquals(500L, response.content().get(0).getFollowerCount());
    }

    @Test
    @DisplayName("TC-11: getFollowedShops_FilterOutBannedShops - Kiểm tra repository gọi đúng query lọc shop")
    void getFollowedShops_FilterOutBannedShops() {
        Page<ShopFollowing> pageResult = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);

        PageResponse<FollowedShopResponse> response = shopFollowingService.getFollowedShops(buyerId, 0, 10);

        assertNotNull(response);
        verify(shopFollowingRepository).findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-14: getFollowedShops_VerifyNoNPlusOneQueries - Xác nhận truy vấn theo batch, không bị N+1")
    void getFollowedShops_VerifyNoNPlusOneQueries() {
        Shop shop2 = Shop.builder().id(UUID.randomUUID()).name("Shop 2").seller(seller).build();
        Shop shop3 = Shop.builder().id(UUID.randomUUID()).name("Shop 3").seller(seller).build();

        ShopFollowing sf1 = ShopFollowing.builder().id(UUID.randomUUID()).userId(buyerId).shop(shop).build();
        ShopFollowing sf2 = ShopFollowing.builder().id(UUID.randomUUID()).userId(buyerId).shop(shop2).build();
        ShopFollowing sf3 = ShopFollowing.builder().id(UUID.randomUUID()).userId(buyerId).shop(shop3).build();

        Page<ShopFollowing> pageResult = new PageImpl<>(List.of(sf1, sf2, sf3), PageRequest.of(0, 10), 3);
        when(shopFollowingRepository.findByUserIdWithShopAndSeller(eq(buyerId), any(Pageable.class)))
                .thenReturn(pageResult);
        when(shopFollowingRepository.countFollowersByShopIdIn(anyList())).thenReturn(Collections.emptyList());
        when(productRepository.countActiveProductsByShopIdIn(anyList())).thenReturn(Collections.emptyList());

        shopFollowingService.getFollowedShops(buyerId, 0, 10);

        // Verify batch queries are called EXACTLY ONCE with all shop IDs, NOT 3 times
        verify(shopFollowingRepository, times(1)).countFollowersByShopIdIn(anyList());
        verify(productRepository, times(1)).countActiveProductsByShopIdIn(anyList());
    }
}
