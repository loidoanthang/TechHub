package com.techhub.controller.shop;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.FollowedShopResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.shop.ShopFollowingService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerShopFollowingController (GET /api/buyer/followed-shops)")
class BuyerShopFollowingControllerTest {

    @Mock
    private ShopFollowingService shopFollowingService;

    @InjectMocks
    private BuyerShopFollowingController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-13: controller_getFollowedShops_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER")
    void controller_getFollowedShops_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = BuyerShopFollowingController.class.getMethod("getFollowedShops", int.class, int.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getFollowedShops phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-12: controller_getFollowedShops_Success_Returns200 - Trả về 200 OK với PageResponse")
    void controller_getFollowedShops_Success_Returns200() {
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

        UUID shopId = UUID.randomUUID();
        FollowedShopResponse item = FollowedShopResponse.builder()
                .shopId(shopId)
                .sellerName("tech-zone-store")
                .shopName("TechZone Official Store")
                .avatarUrl("https://cdn.techhub.com/avatars/techzone.png")
                .bannerUrl("https://cdn.techhub.com/banners/techzone.png")
                .status(ShopStatus.ACTIVE)
                .isPaused(false)
                .followerCount(1250L)
                .totalProducts(48L)
                .followedAt(LocalDateTime.now())
                .build();

        PageResponse<FollowedShopResponse> pageResponse = new PageResponse<>(
                List.of(item), 0, 10, 1, 1, true
        );

        when(shopFollowingService.getFollowedShops(buyerId, 0, 10)).thenReturn(pageResponse);

        ApiResponse<PageResponse<FollowedShopResponse>> response = controller.getFollowedShops(0, 10);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Followed shops retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().totalElements());
        assertEquals(1, response.getData().content().size());

        FollowedShopResponse first = response.getData().content().get(0);
        assertEquals(shopId, first.getShopId());
        assertEquals("TechZone Official Store", first.getShopName());
        assertEquals("tech-zone-store", first.getSellerName());
        assertEquals(1250L, first.getFollowerCount());
        assertEquals(48L, first.getTotalProducts());
        assertFalse(first.getIsPaused());

        verify(shopFollowingService, times(1)).getFollowedShops(buyerId, 0, 10);
    }
}
