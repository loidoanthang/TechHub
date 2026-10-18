package com.techhub.controller.cart;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.CartCountResponse;
import com.techhub.model.dto.response.CartItemResponse;
import com.techhub.model.dto.response.CartResponse;
import com.techhub.model.dto.response.ShopCartGroupResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.cart.CartService;
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
import com.techhub.model.dto.request.AddToCartRequest;
import com.techhub.model.dto.request.CartPreviewRequest;
import com.techhub.model.dto.request.UpdateCartItemRequest;
import com.techhub.model.dto.response.CartPreviewResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CartController (GET /api/cart)")
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
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
    @DisplayName("TC-23: controller_securityAnnotations_RequireBuyerRole - Kiểm tra annotation phân quyền BUYER và @GetMapping")
    void controller_securityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("getCart");

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getCart phải có annotation @GetMapping");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method getCart phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-24: controller_classAnnotations_Correct - Kiểm tra annotation của Controller class")
    void controller_classAnnotations_Correct() {
        assertTrue(CartController.class.isAnnotationPresent(RestController.class),
                "CartController phải có annotation @RestController");

        assertTrue(CartController.class.isAnnotationPresent(RequestMapping.class),
                "CartController phải có annotation @RequestMapping");

        RequestMapping requestMapping = CartController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/cart"}, requestMapping.value(),
                "RequestMapping base path phải là /api/cart");
    }

    @Test
    @DisplayName("TC-25: controller_getCart_Success - Lấy giỏ hàng thành công trả về HTTP 200 và ApiResponse chuẩn")
    void controller_getCart_Success() {
        UUID cartId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        CartItemResponse itemResponse = new CartItemResponse(
                cartItemId,
                productId,
                "Bàn phím cơ Keychron K2",
                "ban-phim-co-keychron-k2",
                variantId,
                "KEY-K2-RED",
                "Red Switch",
                Map.of("Switch", "Red"),
                new BigDecimal("1850000.00"),
                2,
                new BigDecimal("3700000.00"),
                "https://cdn.techhub.vn/products/k2.png",
                15,
                false,
                true,
                null,
                LocalDateTime.now()
        );

        ShopCartGroupResponse shopGroup = new ShopCartGroupResponse(
                shopId,
                "Keychron Official Store",
                "Keychron VN",
                ShopStatus.ACTIVE,
                false,
                List.of(itemResponse)
        );

        CartResponse mockResponse = new CartResponse(
                cartId,
                1,
                2,
                List.of(shopGroup)
        );

        when(cartService.getCartDetails(userId)).thenReturn(mockResponse);

        ApiResponse<CartResponse> response = cartController.getCart();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart details retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(cartId, response.getData().id());
        assertEquals(1, response.getData().totalItemTypes());
        assertEquals(2, response.getData().totalQuantity());
        assertEquals(1, response.getData().shops().size());

        verify(cartService, times(1)).getCartDetails(userId);
    }

    @Test
    @DisplayName("TC-11 & TC-12: controller_getCartCount_SecurityAnnotations_RequireBuyerRole - Kiểm tra annotation phân quyền BUYER và @GetMapping(\"/count\")")
    void controller_getCartCount_SecurityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("getCartCount");

        assertTrue(method.isAnnotationPresent(GetMapping.class),
                "Method getCartCount phải có annotation @GetMapping");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/count"}, getMapping.value(),
                "GetMapping path phải là /count");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method getCartCount phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-13: controller_getCartCount_Success - Lấy badge giỏ hàng thành công trả về HTTP 200 và totalItemTypes chuẩn")
    void controller_getCartCount_Success() {
        when(cartService.getCartCount(userId)).thenReturn(new CartCountResponse(3));

        ApiResponse<CartCountResponse> response = cartController.getCartCount();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart badge count retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(3, response.getData().totalItemTypes());

        verify(cartService, times(1)).getCartCount(userId);
    }

    // ==========================================
    // UNIT TESTS CHO POST /api/cart/items
    // ==========================================

    @Test
    @DisplayName("TC-14: controller_addToCart_SecurityAnnotations_RequireBuyerRole - Kiểm tra annotation phân quyền BUYER, @PostMapping(\"/items\"), @Valid, @RequestBody")
    void controller_addToCart_SecurityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("addToCart", AddToCartRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "Method addToCart phải có annotation @PostMapping");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/items"}, postMapping.value(),
                "PostMapping path phải là /items");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method addToCart phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter parameter = method.getParameters()[0];
        assertTrue(parameter.isAnnotationPresent(Valid.class),
                "Tham số request phải có annotation @Valid");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class),
                "Tham số request phải có annotation @RequestBody");
    }

    @Test
    @DisplayName("TC-15: controller_addToCart_Success - Thêm vào giỏ hàng thành công trả về HTTP 200 và CartItemResponse chuẩn")
    void controller_addToCart_Success() {
        UUID variantId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(variantId, 2);

        CartItemResponse mockItemResponse = new CartItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Bàn phím cơ Keychron K2",
                "ban-phim-co-keychron-k2",
                variantId,
                "KEY-K2-RED",
                "Red Switch",
                Map.of("Switch", "Red"),
                new BigDecimal("1850000.00"),
                2,
                new BigDecimal("3700000.00"),
                "https://cdn.techhub.vn/products/k2.png",
                15,
                false,
                true,
                null,
                LocalDateTime.now()
        );

        when(cartService.addToCart(userId, request)).thenReturn(mockItemResponse);

        ApiResponse<CartItemResponse> response = cartController.addToCart(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Item added to cart successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(variantId, response.getData().variantId());
        assertEquals(2, response.getData().quantity());
        assertEquals(new BigDecimal("3700000.00"), response.getData().itemSubtotal());

        verify(cartService, times(1)).addToCart(userId, request);
    }

    // ==========================================
    // UNIT TESTS CHO PUT /api/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("TC-20: controller_updateCartItem_SecurityAnnotations_RequireBuyerRole - Kiểm tra annotation phân quyền BUYER, @PutMapping(\"/items/{itemId}\"), @Valid, @RequestBody, @PathVariable")
    void controller_updateCartItem_SecurityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("updateCartItem", UUID.class, UpdateCartItemRequest.class);

        assertTrue(method.isAnnotationPresent(PutMapping.class),
                "Method updateCartItem phải có annotation @PutMapping");

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        assertArrayEquals(new String[]{"/items/{itemId}"}, putMapping.value(),
                "PutMapping path phải là /items/{itemId}");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method updateCartItem phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter pathVarParam = method.getParameters()[0];
        assertTrue(pathVarParam.isAnnotationPresent(PathVariable.class),
                "Tham số itemId phải có annotation @PathVariable");

        Parameter bodyParam = method.getParameters()[1];
        assertTrue(bodyParam.isAnnotationPresent(Valid.class),
                "Tham số request phải có annotation @Valid");
        assertTrue(bodyParam.isAnnotationPresent(RequestBody.class),
                "Tham số request phải có annotation @RequestBody");
    }

    @Test
    @DisplayName("TC-21: controller_updateCartItem_Success - Cập nhật số lượng thành công trả về HTTP 200 và CartItemResponse chuẩn")
    void controller_updateCartItem_Success() {
        UUID itemId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);

        CartItemResponse mockItemResponse = new CartItemResponse(
                itemId,
                UUID.randomUUID(),
                "Bàn phím cơ Keychron K2",
                "ban-phim-co-keychron-k2",
                variantId,
                "KEY-K2-RED",
                "Red Switch",
                Map.of("Switch", "Red"),
                new BigDecimal("1850000.00"),
                3,
                new BigDecimal("5550000.00"),
                "https://cdn.techhub.vn/products/k2.png",
                15,
                false,
                true,
                null,
                LocalDateTime.now()
        );

        when(cartService.updateCartItem(userId, itemId, request)).thenReturn(mockItemResponse);

        ApiResponse<CartItemResponse> response = cartController.updateCartItem(itemId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart item quantity updated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(itemId, response.getData().id());
        assertEquals(3, response.getData().quantity());
        assertEquals(new BigDecimal("5550000.00"), response.getData().itemSubtotal());

        verify(cartService, times(1)).updateCartItem(userId, itemId, request);
    }

    @Test
    @DisplayName("TC-22: controller_updateCartItem_Validation_QuantityZero_BadRequest - Quantity = 0 vi phạm @Min(1)")
    void controller_updateCartItem_Validation_QuantityZero_BadRequest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        UpdateCartItemRequest request = new UpdateCartItemRequest(0);
        Set<ConstraintViolation<UpdateCartItemRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Quantity = 0 phải vi phạm validation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")),
                "Lỗi vi phạm phải thuộc trường quantity");
    }

    @Test
    @DisplayName("TC-23: controller_updateCartItem_Validation_QuantityExceeds999_BadRequest - Quantity = 1000 vi phạm @Max(999)")
    void controller_updateCartItem_Validation_QuantityExceeds999_BadRequest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        UpdateCartItemRequest request = new UpdateCartItemRequest(1000);
        Set<ConstraintViolation<UpdateCartItemRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Quantity = 1000 phải vi phạm validation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")),
                "Lỗi vi phạm phải thuộc trường quantity");
    }

    // ==========================================
    // UNIT TESTS CHO DELETE /api/cart/items/{itemId}
    // ==========================================

    @Test
    @DisplayName("TC-15: controller_deleteCartItem_SecurityAnnotations_RequireBuyerRole - Kiểm tra annotation phân quyền BUYER, @DeleteMapping(\"/items/{itemId}\"), @PathVariable")
    void controller_deleteCartItem_SecurityAnnotations_RequireBuyerRole() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("deleteCartItem", UUID.class);

        assertTrue(method.isAnnotationPresent(DeleteMapping.class),
                "Method deleteCartItem phải có annotation @DeleteMapping");

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        assertArrayEquals(new String[]{"/items/{itemId}"}, deleteMapping.value(),
                "DeleteMapping path phải là /items/{itemId}");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "Method deleteCartItem phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền truy cập phải là hasRole('BUYER')");

        Parameter parameter = method.getParameters()[0];
        assertTrue(parameter.isAnnotationPresent(PathVariable.class),
                "Tham số itemId phải có annotation @PathVariable");
    }

    @Test
    @DisplayName("TC-16: controller_deleteCartItem_Success - Xóa mục giỏ hàng thành công trả về HTTP 200 và message chuẩn")
    void controller_deleteCartItem_Success() {
        UUID itemId = UUID.randomUUID();

        doNothing().when(cartService).deleteCartItem(userId, itemId);

        ApiResponse<Void> response = cartController.deleteCartItem(itemId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart item removed successfully", response.getMessage());
        assertNull(response.getData());

        verify(cartService, times(1)).deleteCartItem(userId, itemId);
    }

    @Test
    @DisplayName("TC-17: controller_previewCart_Success - Gọi previewCart thành công trả về HTTP 200 và data")
    void controller_previewCart_Success() {
        UUID itemId = UUID.randomUUID();
        CartPreviewRequest request = new CartPreviewRequest(List.of(itemId), null, null);
        CartPreviewResponse mockResponse = new CartPreviewResponse(
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                1,
                List.of(),
                List.of()
        );

        when(cartService.previewCart(userId, request)).thenReturn(mockResponse);

        ApiResponse<CartPreviewResponse> response = cartController.previewCart(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Cart preview calculated successfully", response.getMessage());
        assertEquals(new BigDecimal("500000.00"), response.getData().totalProductAmount());

        verify(cartService, times(1)).previewCart(userId, request);
    }

    @Test
    @DisplayName("TC-18: controller_previewCart_Metadata - Xác thực endpoint POST /api/cart/preview và quyền ROLE_BUYER")
    void controller_previewCart_Metadata() throws NoSuchMethodException {
        Method method = CartController.class.getMethod("previewCart", CartPreviewRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class));
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/preview"}, mapping.value());

        assertTrue(method.isAnnotationPresent(PreAuthorize.class));
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuth.value());

        Parameter param = method.getParameters()[0];
        assertTrue(param.isAnnotationPresent(Valid.class));
        assertTrue(param.isAnnotationPresent(RequestBody.class));
    }
}
