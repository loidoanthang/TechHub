package com.techhub.controller.wishlist;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.WishlistResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.wishlist.WishlistService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho WishlistController (POST /api/wishlist/{productId})")
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistController wishlistController;

    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("buyer@techhub.vn");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
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
    @DisplayName("TC-13: controller_classAnnotations_Correct - Kiểm tra annotation RestController và RequestMapping")
    void controller_classAnnotations_Correct() {
        assertTrue(WishlistController.class.isAnnotationPresent(RestController.class),
                "WishlistController phải có annotation @RestController");

        assertTrue(WishlistController.class.isAnnotationPresent(RequestMapping.class),
                "WishlistController phải có annotation @RequestMapping");

        RequestMapping requestMapping = WishlistController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/wishlist"}, requestMapping.value(),
                "RequestMapping base path phải là /api/wishlist");
    }

    @Test
    @DisplayName("TC-14: controller_securityAnnotations_RequireBuyerRole - Kiểm tra @PostMapping và phân quyền BUYER")
    void controller_securityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("addToWishlist", UUID.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "Method addToWishlist phải có annotation @PostMapping");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/{productId}"}, postMapping.value(),
                "PostMapping sub-path phải là /{productId}");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method addToWishlist phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter[] parameters = method.getParameters();
        assertEquals(1, parameters.length);
        assertTrue(parameters[0].isAnnotationPresent(PathVariable.class),
                "Tham số productId phải có annotation @PathVariable");
    }

    @Test
    @DisplayName("TC-15: controller_addToWishlist_Success - Gọi Controller trả về ApiResponse thành công")
    void controller_addToWishlist_Success() {
        WishlistResponse mockResponse = WishlistResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Bàn phím cơ Keychron K2 Pro")
                .slug("ban-phim-co-keychron-k2-pro-a1b2c3")
                .coverImageUrl("https://cdn.techhub.com/cover.jpg")
                .minPrice(new BigDecimal("1890000.00"))
                .inStock(true)
                .isShopPaused(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(wishlistService.addToWishlist(userId, productId)).thenReturn(mockResponse);

        ApiResponse<WishlistResponse> apiResponse = wishlistController.addToWishlist(productId);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Sản phẩm đã được thêm vào danh sách yêu thích", apiResponse.getMessage());
        assertNotNull(apiResponse.getData());
        assertEquals(productId, apiResponse.getData().getProductId());

        verify(wishlistService, times(1)).addToWishlist(userId, productId);
    }

    // =========================================================================
    // TESTS FOR DELETE /api/wishlist/{productId} (WishlistController)
    // =========================================================================

    @Test
    @DisplayName("TC-09: testController_RemoveFromWishlist_ClassAnnotations - Kiểm tra RequestMapping và RestController")
    void testController_RemoveFromWishlist_ClassAnnotations() {
        assertTrue(WishlistController.class.isAnnotationPresent(RestController.class));
        RequestMapping requestMapping = WishlistController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/api/wishlist"}, requestMapping.value());
    }

    @Test
    @DisplayName("TC-10: testController_RemoveFromWishlist_SecurityAnnotation - Kiểm tra @DeleteMapping và @PreAuthorize")
    void testController_RemoveFromWishlist_SecurityAnnotation() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("removeFromWishlist", UUID.class);

        assertTrue(method.isAnnotationPresent(DeleteMapping.class),
                "Method removeFromWishlist phải có annotation @DeleteMapping");

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        assertArrayEquals(new String[]{"/{productId}"}, deleteMapping.value(),
                "DeleteMapping path phải là /{productId}");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method removeFromWishlist phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter[] parameters = method.getParameters();
        assertEquals(1, parameters.length);
        assertTrue(parameters[0].isAnnotationPresent(PathVariable.class),
                "Tham số productId phải có annotation @PathVariable");
    }

    @Test
    @DisplayName("TC-11: testController_RemoveFromWishlist_Success - Xóa thành công trả về ApiResponse 200 OK")
    void testController_RemoveFromWishlist_Success() {
        doNothing().when(wishlistService).removeFromWishlist(userId, productId);

        ApiResponse<Void> response = wishlistController.removeFromWishlist(productId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Sản phẩm đã được xóa khỏi danh sách yêu thích", response.getMessage());
        assertNull(response.getData());

        verify(wishlistService, times(1)).removeFromWishlist(userId, productId);
    }

    @Test
    @DisplayName("TC-12: testController_RemoveFromWishlist_Forbidden_SellerRole - Kiểm tra chỉ BUYER mới có quyền xóa")
    void testController_RemoveFromWishlist_Forbidden_SellerRole() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("removeFromWishlist", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertFalse(preAuthorize.value().contains("SELLER"), "Role SELLER không được phép gọi endpoint này");
        assertTrue(preAuthorize.value().contains("BUYER"), "Bắt buộc phải có Role BUYER");
    }

    // =========================================================================
    // TESTS FOR GET /api/wishlist (WishlistController)
    // =========================================================================

    @Test
    @DisplayName("TC-13: testController_GetWishlist_ClassAndSecurityAnnotations - Kiểm tra @GetMapping và @PreAuthorize")
    void testController_GetWishlist_ClassAndSecurityAnnotations() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("getWishlist", int.class, int.class);

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getWishlist phải có annotation @GetMapping");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method getWishlist phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter[] parameters = method.getParameters();
        assertEquals(2, parameters.length);
        assertTrue(parameters[0].isAnnotationPresent(RequestParam.class));
        assertEquals("0", parameters[0].getAnnotation(RequestParam.class).defaultValue());
        assertTrue(parameters[1].isAnnotationPresent(RequestParam.class));
        assertEquals("10", parameters[1].getAnnotation(RequestParam.class).defaultValue());
    }

    @Test
    @DisplayName("TC-14: testController_GetWishlist_Success - Lấy danh sách thành công trả về ApiResponse 200 OK")
    void testController_GetWishlist_Success() {
        WishlistResponse item = WishlistResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Bàn phím cơ Keychron K2 Pro")
                .minPrice(new BigDecimal("1890000.00"))
                .inStock(true)
                .build();

        PageResponse<WishlistResponse> mockPageResponse = new PageResponse<>(
                List.of(item), 0, 10, 1, 1, true
        );

        when(wishlistService.getWishlist(userId, 0, 10)).thenReturn(mockPageResponse);

        ApiResponse<PageResponse<WishlistResponse>> response = wishlistController.getWishlist(0, 10);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Lấy danh sách yêu thích thành công", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().totalElements());
        assertEquals(1, response.getData().content().size());

        verify(wishlistService, times(1)).getWishlist(userId, 0, 10);
    }

    @Test
    @DisplayName("TC-15: testController_GetWishlist_NegativePage_SanitizedToZero - Tự động sanitize page âm về 0")
    void testController_GetWishlist_NegativePage_SanitizedToZero() {
        PageResponse<WishlistResponse> emptyResponse = new PageResponse<>(
                List.of(), 0, 10, 0, 0, true
        );

        when(wishlistService.getWishlist(userId, 0, 10)).thenReturn(emptyResponse);

        ApiResponse<PageResponse<WishlistResponse>> response = wishlistController.getWishlist(-5, 10);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        // Verify controller đã sanitize -5 thành 0
        verify(wishlistService, times(1)).getWishlist(userId, 0, 10);
    }

    @Test
    @DisplayName("TC-16: testController_GetWishlist_ExcessiveSize_SanitizedToFifty - Tự động sanitize size > 50 về 50")
    void testController_GetWishlist_ExcessiveSize_SanitizedToFifty() {
        PageResponse<WishlistResponse> emptyResponse = new PageResponse<>(
                List.of(), 0, 50, 0, 0, true
        );

        when(wishlistService.getWishlist(userId, 0, 50)).thenReturn(emptyResponse);

        ApiResponse<PageResponse<WishlistResponse>> response = wishlistController.getWishlist(0, 999999);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        // Verify controller đã sanitize 999999 thành 50
        verify(wishlistService, times(1)).getWishlist(userId, 0, 50);
    }

    @Test
    @DisplayName("TC-17: testController_GetWishlist_Forbidden_SellerRole - Kiểm tra chỉ BUYER mới có quyền lấy danh sách")
    void testController_GetWishlist_Forbidden_SellerRole() throws NoSuchMethodException {
        Method method = WishlistController.class.getMethod("getWishlist", int.class, int.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertFalse(preAuthorize.value().contains("SELLER"), "Role SELLER không được phép gọi endpoint này");
        assertTrue(preAuthorize.value().contains("BUYER"), "Bắt buộc phải có Role BUYER");
    }
}
