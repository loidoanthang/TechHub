package com.techhub.controller.order;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.CheckoutPreviewRequest;
import com.techhub.model.dto.response.CheckoutPreviewResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.order.CheckoutService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CheckoutController (POST /api/checkout/preview)")
class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private CheckoutController checkoutController;

    private UUID userId;
    private Validator validator;

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

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp lớp của CheckoutController (@RestController, @RequestMapping)")
    void controller_classAnnotations_Correct() {
        assertTrue(CheckoutController.class.isAnnotationPresent(RestController.class),
                "CheckoutController phải có annotation @RestController");

        assertTrue(CheckoutController.class.isAnnotationPresent(RequestMapping.class),
                "CheckoutController phải có annotation @RequestMapping");

        RequestMapping requestMapping = CheckoutController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/api/checkout"}, requestMapping.value(),
                "RequestMapping base path phải là /api/checkout");
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp method của previewCheckout (@PostMapping, @PreAuthorize)")
    void controller_methodAnnotations_Correct() throws NoSuchMethodException {
        Method method = CheckoutController.class.getMethod("previewCheckout", CheckoutPreviewRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "previewCheckout phải có annotation @PostMapping");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/preview"}, postMapping.value(),
                "PostMapping path phải là /preview");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "previewCheckout phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền hạn yêu cầu phải là hasRole('BUYER')");
    }

    @Test
    @DisplayName("Gọi previewCheckout thành công - Ủy quyền đúng cho CheckoutService")
    void previewCheckout_Success_DelegatesToService() {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest(
                List.of(UUID.randomUUID()), null, null, null, null
        );

        CheckoutPreviewResponse mockResponse = new CheckoutPreviewResponse(
                null,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(530000),
                1,
                List.of(),
                List.of("COD", "VNPAY"),
                List.of()
        );

        when(checkoutService.previewCheckout(userId, request)).thenReturn(mockResponse);

        ApiResponse<CheckoutPreviewResponse> apiResponse = checkoutController.previewCheckout(request);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Checkout preview calculated successfully", apiResponse.getMessage());
        assertEquals(mockResponse, apiResponse.getData());
        verify(checkoutService, times(1)).previewCheckout(userId, request);
    }

    @Test
    @DisplayName("Validation CheckoutPreviewRequest: Báo lỗi khi selectedCartItemIds rỗng")
    void validation_Throws_WhenSelectedCartItemIdsEmpty() {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest(List.of(), null, null, null, null);
        Set<ConstraintViolation<CheckoutPreviewRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Selected cart item IDs cannot be empty")));
    }

    @Test
    @DisplayName("Validation CheckoutPreviewRequest: Báo lỗi khi selectedCartItemIds vượt quá 50 phần tử")
    void validation_Throws_WhenSelectedCartItemIdsExceed50() {
        List<UUID> over50Ids = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        CheckoutPreviewRequest request = new CheckoutPreviewRequest(over50Ids, null, null, null, null);
        Set<ConstraintViolation<CheckoutPreviewRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Selected cart items cannot exceed 50")));
    }

    @Test
    @DisplayName("Kiểm tra annotation cấp method của placeOrder (@PostMapping, @PreAuthorize, @ResponseStatus)")
    void controller_placeOrderMethodAnnotations_Correct() throws NoSuchMethodException {
        Method method = CheckoutController.class.getMethod("placeOrder", com.techhub.model.dto.request.PlaceOrderRequest.class);

        assertTrue(method.isAnnotationPresent(PostMapping.class),
                "placeOrder phải có annotation @PostMapping");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/place-order"}, postMapping.value(),
                "PostMapping path phải là /place-order");

        assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                "placeOrder phải có annotation @PreAuthorize");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('BUYER')", preAuthorize.value(),
                "Quyền hạn yêu cầu phải là hasRole('BUYER')");

        assertTrue(method.isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class),
                "placeOrder phải có annotation @ResponseStatus");

        org.springframework.web.bind.annotation.ResponseStatus responseStatus =
                method.getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class);
        assertEquals(org.springframework.http.HttpStatus.CREATED, responseStatus.value(),
                "ResponseStatus phải là CREATED (201)");
    }

    @Test
    @DisplayName("Gọi placeOrder thành công - Ủy quyền đúng cho CheckoutService")
    void placeOrder_Success_DelegatesToService() {
        UUID addressId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        com.techhub.model.dto.request.PlaceOrderRequest request = new com.techhub.model.dto.request.PlaceOrderRequest(
                addressId,
                com.techhub.model.enums.PaymentMethod.COD,
                List.of(UUID.randomUUID()),
                List.of(new com.techhub.model.dto.request.ShopOrderRequest(shopId, List.of(), "Giao nhanh giúp mình"))
        );

        com.techhub.model.dto.response.PlaceOrderResponse mockResponse = new com.techhub.model.dto.response.PlaceOrderResponse(
                UUID.randomUUID(),
                com.techhub.model.enums.PaymentMethod.COD,
                com.techhub.model.enums.PaymentStatus.PENDING,
                BigDecimal.valueOf(500000),
                1,
                List.of(),
                null,
                null,
                java.time.LocalDateTime.now()
        );

        when(checkoutService.placeOrder(userId, request)).thenReturn(mockResponse);

        ApiResponse<com.techhub.model.dto.response.PlaceOrderResponse> apiResponse = checkoutController.placeOrder(request);

        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Order placed successfully", apiResponse.getMessage());
        assertEquals(mockResponse, apiResponse.getData());
        verify(checkoutService, times(1)).placeOrder(userId, request);
    }

    @Test
    @DisplayName("Validation PlaceOrderRequest: Báo lỗi khi thiếu các trường bắt buộc")
    void validation_Throws_WhenPlaceOrderRequestMissingRequiredFields() {
        com.techhub.model.dto.request.PlaceOrderRequest request = new com.techhub.model.dto.request.PlaceOrderRequest(
                null, null, List.of(), List.of()
        );
        Set<ConstraintViolation<com.techhub.model.dto.request.PlaceOrderRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Address ID is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Payment method is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Selected cart item IDs cannot be empty")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Shop orders cannot be empty")));
    }
}
