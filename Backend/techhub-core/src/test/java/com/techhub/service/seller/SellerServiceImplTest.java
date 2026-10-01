package com.techhub.service.seller;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.RegisterSellerRequest;
import com.techhub.model.dto.request.UpdateSellerProfileRequest;
import com.techhub.model.dto.request.UpdateSellerShopRequest;
import com.techhub.model.dto.request.UpdateShippingOptionRequest;
import com.techhub.model.dto.request.UpdateShopStatusRequest;
import com.techhub.model.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.model.dto.response.SellerProfileResponse;
import com.techhub.model.dto.response.SellerRegistrationResponse;
import com.techhub.model.dto.response.SellerShippingOptionResponse;
import com.techhub.model.dto.response.SellerShopResponse;
import com.techhub.model.dto.response.SellerVerificationStatusResponse;
import com.techhub.model.dto.response.SellerWarehouseAddressResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.*;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.seller.impl.SellerServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerService - Đăng ký Người bán & Re-submit (POST /api/seller/register)")
class SellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ShippingOptionRepository shippingOptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerServiceImpl sellerService;

    private User sampleUser;
    private RegisterSellerRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setFirstName("Thang");
        sampleUser.setLastName("Loi");
        sampleUser.setEmail("buyer@techhub.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);
        sampleUser.setLockoutEndTime(null);

        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleRequest = new RegisterSellerRequest(
                "apple-store-vn",
                "Apple Store VN",
                "Chuyên bán đồ Apple",
                "https://techhub.com/avatar.png",
                "https://techhub.com/banner.png",
                "Vietcombank",
                "0123456789",
                "DOAN THANG LOI",
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn",
                new BigDecimal("30000.00")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Đăng ký mới thành công: tạo đủ 4 thực thể Seller, Shop, Address, ShippingOption, status=PENDING, shopStatus=SUSPENDED, isResubmitted=false")
    void registerSeller_Success_NewRegistration() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());
        when(sellerRepository.existsBySellerName("apple-store-vn")).thenReturn(false);

        Seller savedSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("DOAN THANG LOI")
                .createdAt(LocalDateTime.now())
                .build();
        when(sellerRepository.save(any(Seller.class))).thenReturn(savedSeller);

        Shop savedShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(savedSeller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();
        when(shopRepository.save(any(Shop.class))).thenReturn(savedShop);

        Address savedAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(savedShop.getId())
                .recipientName("Nguyen Van Kho")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Duẩn")
                .isDefault(true)
                .build();
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        ShippingOption savedShipping = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(savedShop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("30000.00"))
                .isActive(true)
                .build();
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenReturn(savedShipping);

        SellerRegistrationResponse response = sellerService.registerSeller(sampleRequest);

        assertNotNull(response);
        assertEquals(savedSeller.getId(), response.sellerId());
        assertEquals(savedShop.getId(), response.shopId());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals("Apple Store VN", response.shopName());
        assertEquals(SellerVerificationStatus.PENDING, response.verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());
        assertEquals("Vietcombank", response.bankName());
        assertFalse(response.isResubmitted());
        assertTrue(response.warehouseAddress().contains("123 Lê Duẩn"));
        assertEquals(new BigDecimal("30000.00"), response.shippingFee());

        verify(sellerRepository, times(1)).save(any(Seller.class));
        verify(shopRepository, times(1)).save(any(Shop.class));
        verify(addressRepository, times(1)).save(any(Address.class));
        verify(shippingOptionRepository, times(1)).save(any(ShippingOption.class));
    }

    @Test
    @DisplayName("Nộp lại hồ sơ thành công khi bị REJECTED: cập nhật thông tin, reset PENDING, xóa lý do, isResubmitted=true")
    void registerSeller_Success_Resubmit_RejectedApplication() {
        Seller existingSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason("Số tài khoản ngân hàng không hợp lệ")
                .bankName("Techcombank")
                .bankAccountNumber("999999999")
                .bankAccountHolder("OLD HOLDER")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Shop existingShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(existingSeller)
                .name("Old Shop Name")
                .status(ShopStatus.SUSPENDED)
                .build();

        Address existingAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(existingShop.getId())
                .recipientName("Old Contact")
                .phone("0912345678")
                .province("Ha Noi")
                .district("Ba Dinh")
                .ward("Kim Ma")
                .streetAddress("99 Old St")
                .isDefault(true)
                .build();

        ShippingOption existingShipping = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(existingShop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("40000.00"))
                .isActive(true)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shopRepository.findBySellerId(existingSeller.getId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressRepository.findByShopId(existingShop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shippingOptionRepository.findByShopId(existingShop.getId())).thenReturn(Optional.of(existingShipping));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerRegistrationResponse response = sellerService.registerSeller(sampleRequest);

        assertNotNull(response);
        assertEquals(SellerVerificationStatus.PENDING, response.verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());
        assertEquals("Vietcombank", response.bankName());
        assertEquals("Apple Store VN", response.shopName());
        assertTrue(response.isResubmitted());
        assertNull(existingSeller.getRejectionReason());

        verify(sellerRepository, times(1)).save(existingSeller);
        verify(shopRepository, times(1)).save(existingShop);
        verify(addressRepository, times(1)).save(existingAddress);
        verify(shippingOptionRepository, times(1)).save(existingShipping);
    }

    @Test
    @DisplayName("Nộp lại hồ sơ và đổi sellerName mới thành công khi tên mới chưa ai dùng")
    void registerSeller_Success_Resubmit_ChangeSellerName_Success() {
        Seller existingSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("old-seller-name")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason("Tên vi phạm thương hiệu")
                .build();

        Shop existingShop = Shop.builder().id(UUID.randomUUID()).seller(existingSeller).build();
        Address existingAddress = Address.builder().id(UUID.randomUUID()).shopId(existingShop.getId()).build();
        ShippingOption existingShipping = ShippingOption.builder().id(UUID.randomUUID()).shop(existingShop).build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.existsBySellerName("apple-store-vn")).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shopRepository.findBySellerId(existingSeller.getId())).thenReturn(Optional.of(existingShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressRepository.findByShopId(existingShop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shippingOptionRepository.findByShopId(existingShop.getId())).thenReturn(Optional.of(existingShipping));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerRegistrationResponse response = sellerService.registerSeller(sampleRequest);

        assertNotNull(response);
        assertEquals("apple-store-vn", response.sellerName());
        assertTrue(response.isResubmitted());
    }

    @Test
    @DisplayName("Nộp lại hồ sơ nhưng đổi sang sellerName đã bị người khác đăng ký -> ném SELLER_NAME_ALREADY_EXISTS")
    void registerSeller_Resubmit_ChangeSellerName_AlreadyExists_ThrowsException() {
        Seller existingSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("old-seller-name")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason("Tên vi phạm")
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(existingSeller));
        when(sellerRepository.existsBySellerName("apple-store-vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.SELLER_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký mới nhưng sellerName đã tồn tại -> ném SELLER_NAME_ALREADY_EXISTS (HTTP 409)")
    void registerSeller_NewRegistration_SellerNameAlreadyExists_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());
        when(sellerRepository.existsBySellerName("apple-store-vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.SELLER_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký khi hồ sơ đã tồn tại và đang PENDING -> ném SELLER_ALREADY_PENDING (HTTP 400)")
    void registerSeller_AlreadyPending_ThrowsException() {
        Seller existingSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(existingSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.SELLER_ALREADY_PENDING, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký khi tài khoản đã là Seller APPROVED -> ném ALREADY_A_SELLER (HTTP 400)")
    void registerSeller_AlreadyApproved_ThrowsException() {
        Seller existingSeller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(existingSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ALREADY_A_SELLER, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("User không tồn tại trong DB -> ném USER_NOT_FOUND (HTTP 404)")
    void registerSeller_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void registerSeller_UserNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void registerSeller_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void registerSeller_UserSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void registerSeller_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
    }

    @Test
    @DisplayName("User đang bị tạm khóa đăng nhập -> ném ACCOUNT_LOCKED (HTTP 403)")
    void registerSeller_UserLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.registerSeller(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    // =========================================================================
    // TESTS FOR getVerificationStatus() (GET /api/seller/verification-status)
    // =========================================================================

    @Test
    @DisplayName("User chưa từng đăng ký bán hàng -> trả về hasApplied = false, các trường khác null")
    void getVerificationStatus_UserNotApplied_ReturnsHasAppliedFalse() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        SellerVerificationStatusResponse response = sellerService.getVerificationStatus();

        assertNotNull(response);
        assertFalse(response.hasApplied());
        assertNull(response.sellerId());
        assertNull(response.shopId());
        assertNull(response.sellerName());
        assertNull(response.shopName());
        assertNull(response.verificationStatus());
        assertNull(response.shopStatus());
        assertNull(response.rejectionReason());
        assertNull(response.submittedAt());
        assertNull(response.updatedAt());
    }

    @Test
    @DisplayName("Hồ sơ đang PENDING -> trả về hasApplied = true, status = PENDING, rejectionReason = null")
    void getVerificationStatus_StatusPending_ReturnsPendingInfo() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .rejectionReason(null)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("DOAN THANG LOI")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerVerificationStatusResponse response = sellerService.getVerificationStatus();

        assertNotNull(response);
        assertTrue(response.hasApplied());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals("Apple Store VN", response.shopName());
        assertEquals(SellerVerificationStatus.PENDING, response.verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());
        assertNull(response.rejectionReason());
        assertEquals(seller.getCreatedAt(), response.submittedAt());
    }

    @Test
    @DisplayName("Hồ sơ bị REJECTED -> trả về hasApplied = true, status = REJECTED kèm lý do rejectionReason")
    void getVerificationStatus_StatusRejected_ReturnsRejectedInfoWithReason() {
        String reason = "Số tài khoản ngân hàng không trùng khớp với tên chủ tài khoản.";
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason(reason)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("DOAN THANG LOI")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusHours(3))
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerVerificationStatusResponse response = sellerService.getVerificationStatus();

        assertNotNull(response);
        assertTrue(response.hasApplied());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals("Apple Store VN", response.shopName());
        assertEquals(SellerVerificationStatus.REJECTED, response.verificationStatus());
        assertEquals(ShopStatus.SUSPENDED, response.shopStatus());
        assertEquals(reason, response.rejectionReason());
    }

    @Test
    @DisplayName("Hồ sơ đã APPROVED -> trả về hasApplied = true, status = APPROVED, shopStatus = ACTIVE")
    void getVerificationStatus_StatusApproved_ReturnsApprovedInfo() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .rejectionReason(null)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("DOAN THANG LOI")
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerVerificationStatusResponse response = sellerService.getVerificationStatus();

        assertNotNull(response);
        assertTrue(response.hasApplied());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals("Apple Store VN", response.shopName());
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals(ShopStatus.ACTIVE, response.shopStatus());
        assertNull(response.rejectionReason());
    }

    @Test
    @DisplayName("getVerificationStatus: User ID không tồn tại -> ném USER_NOT_FOUND (HTTP 404)")
    void getVerificationStatus_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: Email chưa xác thực -> ném ACCOUNT_NOT_VERIFIED")
    void getVerificationStatus_UserNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void getVerificationStatus_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void getVerificationStatus_UserSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void getVerificationStatus_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: User bị khóa đăng nhập tạm thời -> ném ACCOUNT_LOCKED (HTTP 403)")
    void getVerificationStatus_UserLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getVerificationStatus: Seller có nhưng không tìm thấy Shop -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getVerificationStatus_SellerExists_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getVerificationStatus());

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    // =========================================================================
    // GET /api/seller/profile UNIT TESTS
    // =========================================================================

    @Test
    @DisplayName("getSellerProfile: Thành công khi User hợp lệ và Seller APPROVED -> trả về SellerProfileResponse")
    void getSellerProfile_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-flagship-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        SellerProfileResponse response = sellerService.getSellerProfile();

        assertNotNull(response);
        assertEquals(seller.getId(), response.sellerId());
        assertEquals("apple-flagship-store", response.sellerName());
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals("Vietcombank", response.bankName());
        assertEquals("0123456789", response.bankAccountNumber());
        assertEquals("NGUYEN VAN A", response.bankAccountHolder());
        assertEquals(seller.getCreatedAt(), response.createdAt());
        assertEquals(seller.getUpdatedAt(), response.updatedAt());

        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(sellerRepository, times(1)).findByUserId(sampleUser.getId());
    }

    @Test
    @DisplayName("getSellerProfile: User không tồn tại -> ném USER_NOT_FOUND (HTTP 404)")
    void getSellerProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void getSellerProfile_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void getSellerProfile_UserSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void getSellerProfile_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: User chưa verify email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void getSellerProfile_UserNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: User bị khóa tạm thời -> ném ACCOUNT_LOCKED (HTTP 403)")
    void getSellerProfile_UserLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getSellerProfile: Không tìm thấy hồ sơ Seller trong DB -> ném SELLER_NOT_FOUND (HTTP 404)")
    void getSellerProfile_SellerNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getSellerProfile: Seller đang ở trạng thái PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void getSellerProfile_SellerPending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    @Test
    @DisplayName("getSellerProfile: Seller đang ở trạng thái REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void getSellerProfile_SellerRejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .rejectionReason("Thông tin ngân hàng không hợp lệ")
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getSellerProfile());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    // =========================================================================
    // PUT /api/seller/profile UNIT TESTS
    // =========================================================================

    @Test
    @DisplayName("updateSellerProfile: Thành công với slug mới chưa ai dùng -> cập nhật slug và ngân hàng")
    void updateSellerProfile_Success_NewSlug() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-old-slug")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store",
                "Techcombank",
                "9876543210",
                "nguyen van a"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(sellerRepository.existsBySellerName("apple-official-store")).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerProfileResponse response = sellerService.updateSellerProfile(request);

        assertNotNull(response);
        assertEquals(seller.getId(), response.sellerId());
        assertEquals("apple-official-store", response.sellerName());
        assertEquals(SellerVerificationStatus.APPROVED, response.verificationStatus());
        assertEquals("Techcombank", response.bankName());
        assertEquals("9876543210", response.bankAccountNumber());
        assertEquals("NGUYEN VAN A", response.bankAccountHolder()); // uppercase check

        verify(sellerRepository, times(1)).existsBySellerName("apple-official-store");
        verify(sellerRepository, times(1)).save(seller);
    }

    @Test
    @DisplayName("updateSellerProfile: Thành công khi giữ nguyên slug cũ -> bỏ qua check tồn tại slug")
    void updateSellerProfile_Success_SameSlug() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store",
                "Techcombank",
                "9876543210",
                "nguyen van a"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerProfileResponse response = sellerService.updateSellerProfile(request);

        assertNotNull(response);
        assertEquals("apple-official-store", response.sellerName());
        assertEquals("Techcombank", response.bankName());

        verify(sellerRepository, never()).existsBySellerName(anyString());
        verify(sellerRepository, times(1)).save(seller);
    }

    @Test
    @DisplayName("updateSellerProfile: Slug mới đã có Seller khác sử dụng -> ném SELLER_NAME_ALREADY_EXISTS (HTTP 409)")
    void updateSellerProfile_SlugAlreadyExists_ThrowsConflict() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-old-slug")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .build();

        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "samsung-store",
                "Techcombank",
                "9876543210",
                "nguyen van a"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(sellerRepository.existsBySellerName("samsung-store")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.SELLER_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User không tồn tại -> ném USER_NOT_FOUND (HTTP 404)")
    void updateSellerProfile_UserNotFound_ThrowsException() {
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateSellerProfile_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void updateSellerProfile_UserSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void updateSellerProfile_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User chưa verify email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateSellerProfile_UserNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: User bị khóa tạm thời -> ném ACCOUNT_LOCKED (HTTP 403)")
    void updateSellerProfile_UserLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateSellerProfile: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void updateSellerProfile_SellerNotFound_ThrowsException() {
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateSellerProfile: Seller đang ở trạng thái PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void updateSellerProfile_SellerPending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateSellerProfile: Seller đang ở trạng thái REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void updateSellerProfile_SellerRejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest(
                "apple-official-store", "Vietcombank", "0123456789", "NGUYEN VAN A"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateSellerProfile(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
    }

    // ==========================================
    // GET /api/seller/shop: Lấy thông tin Shop cá nhân
    // ==========================================

    @Test
    @DisplayName("getShopInfo: Lấy thông tin Shop thành công khi Shop đang ACTIVE")
    void getShopInfo_Success_ActiveShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-flagship-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("0123456789")
                .bankAccountHolder("NGUYEN VAN A")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .description("Cửa hàng Apple chính hãng")
                .avatarUrl("https://techhub.vn/avatar.png")
                .bannerUrl("https://techhub.vn/banner.png")
                .status(ShopStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerShopResponse response = sellerService.getShopInfo();

        assertNotNull(response);
        assertEquals(shop.getId(), response.shopId());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals("apple-flagship-store", response.sellerName());
        assertEquals("Apple Flagship Store VN", response.name());
        assertEquals("Cửa hàng Apple chính hãng", response.description());
        assertEquals("https://techhub.vn/avatar.png", response.avatarUrl());
        assertEquals("https://techhub.vn/banner.png", response.bannerUrl());
        assertEquals(ShopStatus.ACTIVE, response.status());
        assertEquals(shop.getCreatedAt(), response.createdAt());
        assertEquals(shop.getUpdatedAt(), response.updatedAt());

        verify(shopRepository, times(1)).findBySellerId(seller.getId());
    }

    @Test
    @DisplayName("getShopInfo: Lấy thông tin Shop thành công khi Shop đang PAUSED (tạm nghỉ bán)")
    void getShopInfo_Success_PausedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.PAUSED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerShopResponse response = sellerService.getShopInfo();

        assertNotNull(response);
        assertEquals(ShopStatus.PAUSED, response.status());
    }

    @Test
    @DisplayName("getShopInfo: Lấy thông tin Shop thành công khi Shop đang SUSPENDED")
    void getShopInfo_Success_SuspendedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerShopResponse response = sellerService.getShopInfo();

        assertNotNull(response);
        assertEquals(ShopStatus.SUSPENDED, response.status());
    }

    @Test
    @DisplayName("getShopInfo: Không tìm thấy User trong DB -> ném USER_NOT_FOUND (HTTP 404)")
    void getShopInfo_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShopInfo: User bị cấm (BANNED) -> ném ACCOUNT_BANNED (HTTP 403)")
    void getShopInfo_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShopInfo: User bị tạm đình chỉ (SUSPENDED) -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void getShopInfo_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShopInfo: User đã bị xóa (DELETED) -> ném ACCOUNT_DELETED (HTTP 403)")
    void getShopInfo_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShopInfo: User chưa verify email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void getShopInfo_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShopInfo: User bị khóa tạm thời do brute-force -> ném ACCOUNT_LOCKED (HTTP 403)")
    void getShopInfo_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShopInfo: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void getShopInfo_SellerNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShopInfo: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void getShopInfo_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShopInfo: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void getShopInfo_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShopInfo: Không tìm thấy Shop tương ứng với Seller -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getShopInfo_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShopInfo());

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // PUT /api/seller/shop: Cập nhật thông tin Shop cá nhân
    // ==========================================

    @Test
    @DisplayName("updateShopInfo: Cập nhật đầy đủ các trường thông tin thành công khi Shop đang ACTIVE")
    void updateShopInfo_Success_AllFields() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-flagship-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Old Name")
                .description("Old description")
                .avatarUrl("https://techhub.vn/old_avatar.png")
                .bannerUrl("https://techhub.vn/old_banner.png")
                .status(ShopStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Apple Flagship Store VN",
                "Mô tả mới về Apple Store",
                "https://techhub.vn/new_avatar.png",
                "https://techhub.vn/new_banner.png"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopInfo(request);

        assertNotNull(response);
        assertEquals(shop.getId(), response.shopId());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals("apple-flagship-store", response.sellerName());
        assertEquals("Apple Flagship Store VN", response.name());
        assertEquals("Mô tả mới về Apple Store", response.description());
        assertEquals("https://techhub.vn/new_avatar.png", response.avatarUrl());
        assertEquals("https://techhub.vn/new_banner.png", response.bannerUrl());
        assertEquals(ShopStatus.ACTIVE, response.status());

        verify(shopRepository, times(1)).save(shop);
    }

    @Test
    @DisplayName("updateShopInfo: Cập nhật chỉ trường bắt buộc, các trường tùy chọn set về null")
    void updateShopInfo_Success_OnlyRequiredField() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store")
                .description("Old description")
                .avatarUrl("https://techhub.vn/old_avatar.png")
                .bannerUrl("https://techhub.vn/old_banner.png")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Apple Authorized Store",
                null,
                null,
                null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopInfo(request);

        assertNotNull(response);
        assertEquals("Apple Authorized Store", response.name());
        assertNull(response.description());
        assertNull(response.avatarUrl());
        assertNull(response.bannerUrl());
    }

    @Test
    @DisplayName("updateShopInfo: Tự động trim khoảng trắng và chuyển chuỗi rỗng thành null")
    void updateShopInfo_Success_TrimStrings() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Old Name")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "   Apple Store Trimmed   ",
                "   Mô tả có dấu cách   ",
                "   ",
                ""
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopInfo(request);

        assertNotNull(response);
        assertEquals("Apple Store Trimmed", response.name());
        assertEquals("Mô tả có dấu cách", response.description());
        assertNull(response.avatarUrl());
        assertNull(response.bannerUrl());
    }

    @Test
    @DisplayName("updateShopInfo: Cập nhật thành công khi Shop đang PAUSED (giữ nguyên status PAUSED)")
    void updateShopInfo_Success_PausedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Old Name")
                .status(ShopStatus.PAUSED)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "New Shop Name While Paused",
                "Description",
                null,
                null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopInfo(request);

        assertNotNull(response);
        assertEquals("New Shop Name While Paused", response.name());
        assertEquals(ShopStatus.PAUSED, response.status());
    }

    @Test
    @DisplayName("updateShopInfo: Cập nhật thành công khi Shop đang SUSPENDED (giữ nguyên status SUSPENDED)")
    void updateShopInfo_Success_SuspendedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Old Name")
                .status(ShopStatus.SUSPENDED)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "New Shop Name While Suspended",
                "Description",
                null,
                null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopInfo(request);

        assertNotNull(response);
        assertEquals("New Shop Name While Suspended", response.name());
        assertEquals(ShopStatus.SUSPENDED, response.status());
    }

    @Test
    @DisplayName("updateShopInfo: Shop đã bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateShopInfo_ShopBanned_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-store-vn")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Banned Shop")
                .status(ShopStatus.BANNED)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Try Update Banned Shop",
                "Description",
                null,
                null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopInfo: User không tồn tại -> ném USER_NOT_FOUND (HTTP 404)")
    void updateShopInfo_UserNotFound_ThrowsException() {
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopInfo: User bị cấm (BANNED) -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateShopInfo_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopInfo: User bị tạm đình chỉ (SUSPENDED) -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void updateShopInfo_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopInfo: User đã bị xóa (DELETED) -> ném ACCOUNT_DELETED (HTTP 403)")
    void updateShopInfo_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopInfo: User chưa verify email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateShopInfo_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopInfo: User bị khóa tạm thời do brute-force -> ném ACCOUNT_LOCKED (HTTP 403)")
    void updateShopInfo_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopInfo: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void updateShopInfo_SellerNotFound_ThrowsException() {
        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopInfo: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void updateShopInfo_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopInfo: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void updateShopInfo_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopInfo: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void updateShopInfo_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        UpdateSellerShopRequest request = new UpdateSellerShopRequest(
                "Shop Name", null, null, null
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopInfo(request));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    // =========================================================================
    // PATCH /api/seller/shop/status: updateShopStatus Unit Tests
    // =========================================================================

    @Test
    @DisplayName("updateShopStatus: Chuyển trạng thái từ ACTIVE -> PAUSED thành công")
    void updateShopStatus_Success_ActiveToPaused() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .description("Mô tả")
                .avatarUrl("https://techhub.com/avatar.png")
                .bannerUrl("https://techhub.com/banner.png")
                .status(ShopStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopStatus(request);

        assertNotNull(response);
        assertEquals(shop.getId(), response.shopId());
        assertEquals(seller.getId(), response.sellerId());
        assertEquals("apple-store-vn", response.sellerName());
        assertEquals(ShopStatus.PAUSED, response.status());
        verify(shopRepository, times(1)).save(shop);
        assertEquals(ShopStatus.PAUSED, shop.getStatus());
    }

    @Test
    @DisplayName("updateShopStatus: Chuyển trạng thái từ PAUSED -> ACTIVE thành công")
    void updateShopStatus_Success_PausedToActive() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.PAUSED)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.ACTIVE);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShopResponse response = sellerService.updateShopStatus(request);

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.status());
        verify(shopRepository, times(1)).save(shop);
        assertEquals(ShopStatus.ACTIVE, shop.getStatus());
    }

    @Test
    @DisplayName("updateShopStatus: Idempotent - Shop đang ACTIVE yêu cầu ACTIVE -> không gọi DB save")
    void updateShopStatus_Success_Idempotent_ActiveToActive() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.ACTIVE);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerShopResponse response = sellerService.updateShopStatus(request);

        assertNotNull(response);
        assertEquals(ShopStatus.ACTIVE, response.status());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Idempotent - Shop đang PAUSED yêu cầu PAUSED -> không gọi DB save")
    void updateShopStatus_Success_Idempotent_PausedToPaused() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.PAUSED)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        SellerShopResponse response = sellerService.updateShopStatus(request);

        assertNotNull(response);
        assertEquals(ShopStatus.PAUSED, response.status());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Yêu cầu chuyển sang SUSPENDED -> ném INVALID_SHOP_STATUS_TRANSITION (HTTP 400)")
    void updateShopStatus_TargetStatusSuspended_ThrowsBadRequest() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.SUSPENDED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Yêu cầu chuyển sang BANNED -> ném INVALID_SHOP_STATUS_TRANSITION (HTTP 400)")
    void updateShopStatus_TargetStatusBanned_ThrowsBadRequest() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.BANNED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Shop hiện tại đang SUSPENDED -> ném INVALID_SHOP_STATUS_TRANSITION (HTTP 400)")
    void updateShopStatus_CurrentStatusSuspended_ThrowsBadRequest() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.ACTIVE);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Shop hiện tại đang BANNED -> ném INVALID_SHOP_STATUS_TRANSITION (HTTP 400)")
    void updateShopStatus_CurrentStatusBanned_ThrowsBadRequest() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.BANNED)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.ACTIVE);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.INVALID_SHOP_STATUS_TRANSITION, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateShopStatus: Không tìm thấy User từ token -> ném USER_NOT_FOUND (HTTP 404)")
    void updateShopStatus_UserNotFound_ThrowsException() {
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Tài khoản User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateShopStatus_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Tài khoản User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void updateShopStatus_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Tài khoản User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void updateShopStatus_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Tài khoản User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateShopStatus_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Tài khoản User đang bị khóa -> ném ACCOUNT_LOCKED (HTTP 403)")
    void updateShopStatus_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void updateShopStatus_SellerNotFound_ThrowsException() {
        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void updateShopStatus_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void updateShopStatus_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShopStatus: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void updateShopStatus_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        UpdateShopStatusRequest request = new UpdateShopStatusRequest(ShopStatus.PAUSED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShopStatus(request));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).save(any());
    }

    // =========================================================================
    // GET /api/seller/shop/address: getWarehouseAddress Unit Tests
    // =========================================================================

    @Test
    @DisplayName("getWarehouseAddress: Lấy địa chỉ kho thành công trả về đầy đủ thông tin")
    void getWarehouseAddress_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        Address address = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Nguyen Van Kho")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Duẩn")
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(address));

        SellerWarehouseAddressResponse response = sellerService.getWarehouseAddress();

        assertNotNull(response);
        assertEquals(address.getId(), response.id());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("Nguyen Van Kho", response.contactName());
        assertEquals("0987654321", response.phone());
        assertEquals("TP. Hồ Chí Minh", response.province());
        assertEquals("Quận 1", response.district());
        assertEquals("Phường Bến Nghé", response.ward());
        assertEquals("123 Lê Duẩn", response.streetAddress());
        assertEquals("123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", response.fullAddress());
        assertEquals(address.getCreatedAt(), response.createdAt());
        assertEquals(address.getUpdatedAt(), response.updatedAt());
    }

    @Test
    @DisplayName("getWarehouseAddress: Địa chỉ có trường null -> format fullAddress null-safe không crash")
    void getWarehouseAddress_Success_NullSafeFullAddress() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .sellerName("apple-store-vn")
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        Address address = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Nguyen Van Kho")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward(null)
                .streetAddress(null)
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(address));

        SellerWarehouseAddressResponse response = sellerService.getWarehouseAddress();

        assertNotNull(response);
        assertNotNull(response.fullAddress());
        assertEquals(", , Quận 1, TP. Hồ Chí Minh", response.fullAddress());
    }

    @Test
    @DisplayName("getWarehouseAddress: Không tìm thấy User từ token -> ném USER_NOT_FOUND (HTTP 404)")
    void getWarehouseAddress_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Tài khoản User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void getWarehouseAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Tài khoản User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void getWarehouseAddress_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Tài khoản User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void getWarehouseAddress_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Tài khoản User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void getWarehouseAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Tài khoản User đang bị khóa -> ném ACCOUNT_LOCKED (HTTP 403)")
    void getWarehouseAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void getWarehouseAddress_SellerNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void getWarehouseAddress_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void getWarehouseAddress_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getWarehouseAddress_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("getWarehouseAddress: Không tìm thấy Address tương ứng của Shop -> ném ADDRESS_NOT_FOUND (HTTP 404)")
    void getWarehouseAddress_AddressNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getWarehouseAddress());

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // PUT /api/seller/shop/address: Cập nhật địa chỉ kho lấy hàng
    // ==========================================

    @Test
    @DisplayName("updateWarehouseAddress: Cập nhật địa chỉ kho thành công khi Shop đang ACTIVE")
    void updateWarehouseAddress_Success_AllFieldsUpdated() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        Address existingAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Old Contact")
                .phone("0900000000")
                .province("Hà Nội")
                .district("Hoàn Kiếm")
                .ward("Hàng Bạc")
                .streetAddress("10 Phố Huế")
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerWarehouseAddressResponse response = sellerService.updateWarehouseAddress(request);

        assertNotNull(response);
        assertEquals(existingAddress.getId(), response.id());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("Nguyen Van Kho", response.contactName());
        assertEquals("0987654321", response.phone());
        assertEquals("TP. Hồ Chí Minh", response.province());
        assertEquals("Quận 1", response.district());
        assertEquals("Phường Bến Nghé", response.ward());
        assertEquals("123 Lê Duẩn", response.streetAddress());
        assertEquals("123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", response.fullAddress());

        verify(addressRepository, times(1)).save(existingAddress);
    }

    @Test
    @DisplayName("updateWarehouseAddress: Trimming chuỗi - Tự động loại bỏ khoảng trắng thừa ở các trường")
    void updateWarehouseAddress_Success_WithLeadingAndTrailingSpaces_TrimsProperly() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        Address existingAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Old Contact")
                .phone("0900000000")
                .province("Hà Nội")
                .district("Hoàn Kiếm")
                .ward("Hàng Bạc")
                .streetAddress("10 Phố Huế")
                .isDefault(true)
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "  Nguyen Van Kho  ",
                "  0987654321  ",
                "  TP. Hồ Chí Minh  ",
                "  Quận 1  ",
                "  Phường Bến Nghé  ",
                "  123 Lê Duẩn  "
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerWarehouseAddressResponse response = sellerService.updateWarehouseAddress(request);

        assertNotNull(response);
        assertEquals("Nguyen Van Kho", response.contactName());
        assertEquals("0987654321", response.phone());
        assertEquals("TP. Hồ Chí Minh", response.province());
        assertEquals("Quận 1", response.district());
        assertEquals("Phường Bến Nghé", response.ward());
        assertEquals("123 Lê Duẩn", response.streetAddress());
        assertEquals("123 Lê Duẩn, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", response.fullAddress());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Cho phép cập nhật kho khi Shop đang ở trạng thái PAUSED (Tạm nghỉ bán)")
    void updateWarehouseAddress_Success_WhenShopPaused() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.PAUSED)
                .build();

        Address existingAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Old Contact")
                .phone("0900000000")
                .province("Hà Nội")
                .district("Hoàn Kiếm")
                .ward("Hàng Bạc")
                .streetAddress("10 Phố Huế")
                .isDefault(true)
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerWarehouseAddressResponse response = sellerService.updateWarehouseAddress(request);

        assertNotNull(response);
        assertEquals("Nguyen Van Kho", response.contactName());
        verify(addressRepository, times(1)).save(existingAddress);
    }

    @Test
    @DisplayName("updateWarehouseAddress: Cho phép cập nhật kho khi Shop đang ở trạng thái SUSPENDED (Tạm khóa / chờ duyệt lại)")
    void updateWarehouseAddress_Success_WhenShopSuspended() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        Address existingAddress = Address.builder()
                .id(UUID.randomUUID())
                .shopId(shop.getId())
                .recipientName("Old Contact")
                .phone("0900000000")
                .province("Hà Nội")
                .district("Hoàn Kiếm")
                .ward("Hàng Bạc")
                .streetAddress("10 Phố Huế")
                .isDefault(true)
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerWarehouseAddressResponse response = sellerService.updateWarehouseAddress(request);

        assertNotNull(response);
        assertEquals("Nguyen Van Kho", response.contactName());
        verify(addressRepository, times(1)).save(existingAddress);
    }

    @Test
    @DisplayName("updateWarehouseAddress: Shop đang ở trạng thái BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateWarehouseAddress_ShopBanned_ThrowsAccountBanned() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.BANNED)
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Không tìm thấy User trong DB -> ném USER_NOT_FOUND (HTTP 404)")
    void updateWarehouseAddress_UserNotFound_ThrowsException() {
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Tài khoản User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateWarehouseAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Tài khoản User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void updateWarehouseAddress_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Tài khoản User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void updateWarehouseAddress_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Tài khoản User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateWarehouseAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Tài khoản User đang bị khóa -> ném ACCOUNT_LOCKED (HTTP 403)")
    void updateWarehouseAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void updateWarehouseAddress_SellerNotFound_ThrowsException() {
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void updateWarehouseAddress_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void updateWarehouseAddress_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void updateWarehouseAddress_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();
        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("updateWarehouseAddress: Không tìm thấy Address tương ứng của Shop -> ném ADDRESS_NOT_FOUND (HTTP 404)")
    void updateWarehouseAddress_AddressNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateWarehouseAddressRequest request = new UpdateWarehouseAddressRequest(
                "Nguyen Van Kho", "0987654321", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "123 Lê Duẩn"
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(addressRepository.findByShopId(shop.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateWarehouseAddress(request));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // GET /api/seller/shop/shipping-option: Lấy cấu hình phí vận chuyển
    // ==========================================

    @Test
    @DisplayName("getShippingOption: Lấy cấu hình phí ship thành công khi Shop đang ACTIVE")
    void getShippingOption_Success_ActiveShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));

        SellerShippingOptionResponse response = sellerService.getShippingOption();

        assertNotNull(response);
        assertEquals(shippingOption.getId(), response.id());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("Standard Delivery", response.name());
        assertEquals(new BigDecimal("25000.00"), response.shippingFee());
        assertTrue(response.isActive());
        assertEquals(shippingOption.getCreatedAt(), response.createdAt());
        assertEquals(shippingOption.getUpdatedAt(), response.updatedAt());

        verify(shippingOptionRepository, times(1)).findByShopId(shop.getId());
    }

    @Test
    @DisplayName("getShippingOption: Lấy cấu hình thành công khi Shop đang PAUSED (Tạm nghỉ bán)")
    void getShippingOption_Success_PausedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.PAUSED)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("30000.00"))
                .isActive(true)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));

        SellerShippingOptionResponse response = sellerService.getShippingOption();

        assertNotNull(response);
        assertEquals(new BigDecimal("30000.00"), response.shippingFee());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("getShippingOption: Lấy cấu hình thành công khi Shop đang SUSPENDED (Tạm khóa / chờ duyệt)")
    void getShippingOption_Success_SuspendedShop() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("15000.00"))
                .isActive(true)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));

        SellerShippingOptionResponse response = sellerService.getShippingOption();

        assertNotNull(response);
        assertEquals(new BigDecimal("15000.00"), response.shippingFee());
    }

    @Test
    @DisplayName("getShippingOption: Lấy cấu hình khi isActive = false -> trả về đúng trạng thái tắt")
    void getShippingOption_Success_InactiveShippingOption() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .sellerName("apple-official-store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("20000.00"))
                .isActive(false)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));

        SellerShippingOptionResponse response = sellerService.getShippingOption();

        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(new BigDecimal("20000.00"), response.shippingFee());
    }

    @Test
    @DisplayName("getShippingOption: Không tìm thấy User trong DB -> ném USER_NOT_FOUND (HTTP 404)")
    void getShippingOption_UserNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Tài khoản User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void getShippingOption_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Tài khoản User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void getShippingOption_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Tài khoản User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void getShippingOption_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Tài khoản User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void getShippingOption_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Tài khoản User đang bị khóa -> ném ACCOUNT_LOCKED (HTTP 403)")
    void getShippingOption_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getShippingOption: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void getShippingOption_SellerNotFound_ThrowsException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShippingOption: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void getShippingOption_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShippingOption: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void getShippingOption_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("getShippingOption: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void getShippingOption_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shippingOptionRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("getShippingOption: Không tìm thấy ShippingOption tương ứng của Shop -> ném SHIPPING_OPTION_NOT_FOUND (HTTP 404)")
    void getShippingOption_ShippingOptionNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.getShippingOption());

        assertEquals(ErrorCode.SHIPPING_OPTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateShippingOption: Cập nhật cước phí vận chuyển thành công")
    void updateShippingOption_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShippingOptionResponse response = sellerService.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(shippingOption.getId(), response.id());
        assertEquals(shop.getId(), response.shopId());
        assertEquals("Standard Delivery", response.name());
        assertEquals(new BigDecimal("22000.00"), response.shippingFee());
        assertTrue(response.isActive());
        verify(shippingOptionRepository, times(1)).save(shippingOption);
    }

    @Test
    @DisplayName("updateShippingOption: Cập nhật cước phí bằng 0.00 (Freeship Storewide) thành công")
    void updateShippingOption_ZeroFee_Freeship_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("0.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShippingOptionResponse response = sellerService.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("0.00"), response.shippingFee());
        verify(shippingOptionRepository, times(1)).save(shippingOption);
    }

    @Test
    @DisplayName("updateShippingOption: Cập nhật cước phí mức cận trên tối đa 10,000,000.00 VNĐ thành công")
    void updateShippingOption_MaxFee_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("10000000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShippingOptionResponse response = sellerService.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("10000000.00"), response.shippingFee());
        verify(shippingOptionRepository, times(1)).save(shippingOption);
    }

    @Test
    @DisplayName("updateShippingOption: Shop đang ở trạng thái PAUSED -> Cho phép cập nhật cước phí bình thường")
    void updateShippingOption_ShopPaused_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.PAUSED)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("30000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShippingOptionResponse response = sellerService.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("30000.00"), response.shippingFee());
        verify(shippingOptionRepository, times(1)).save(shippingOption);
    }

    @Test
    @DisplayName("updateShippingOption: Shop đang ở trạng thái SUSPENDED -> Cho phép cập nhật cước phí bình thường")
    void updateShippingOption_ShopSuspended_Success() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.SUSPENDED)
                .build();

        ShippingOption shippingOption = ShippingOption.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .name("Standard Delivery")
                .shippingFee(new BigDecimal("25000.00"))
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("35000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.of(shippingOption));
        when(shippingOptionRepository.save(any(ShippingOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerShippingOptionResponse response = sellerService.updateShippingOption(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("35000.00"), response.shippingFee());
        verify(shippingOptionRepository, times(1)).save(shippingOption);
    }

    @Test
    @DisplayName("updateShippingOption: Không tìm thấy User từ token -> ném USER_NOT_FOUND (HTTP 404)")
    void updateShippingOption_UserNotFound_ThrowsException() {
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Tài khoản User bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateShippingOption_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Tài khoản User bị SUSPENDED -> ném ACCOUNT_SUSPENDED (HTTP 403)")
    void updateShippingOption_UserSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Tài khoản User bị DELETED -> ném ACCOUNT_DELETED (HTTP 403)")
    void updateShippingOption_UserDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Tài khoản User chưa xác thực email -> ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateShippingOption_UserNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Tài khoản User đang bị khóa lockout -> ném ACCOUNT_LOCKED (HTTP 403)")
    void updateShippingOption_UserLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusHours(1));
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(sellerRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Không tìm thấy hồ sơ Seller -> ném SELLER_NOT_FOUND (HTTP 404)")
    void updateShippingOption_SellerNotFound_ThrowsException() {
        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Hồ sơ Seller đang PENDING -> ném NOT_A_SELLER (HTTP 403)")
    void updateShippingOption_SellerNotApproved_Pending_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Hồ sơ Seller bị REJECTED -> ném NOT_A_SELLER (HTTP 403)")
    void updateShippingOption_SellerNotApproved_Rejected_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.REJECTED)
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.NOT_A_SELLER, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Không tìm thấy Shop tương ứng -> ném SHOP_NOT_FOUND (HTTP 404)")
    void updateShippingOption_ShopNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(shippingOptionRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Shop bị BANNED -> ném ACCOUNT_BANNED (HTTP 403)")
    void updateShippingOption_ShopBanned_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.BANNED)
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(shippingOptionRepository, never()).findByShopId(any());
    }

    @Test
    @DisplayName("updateShippingOption: Không tìm thấy ShippingOption tương ứng của Shop -> ném SHIPPING_OPTION_NOT_FOUND (HTTP 404)")
    void updateShippingOption_ShippingOptionNotFound_ThrowsException() {
        Seller seller = Seller.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        Shop shop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .name("Apple Store VN")
                .status(ShopStatus.ACTIVE)
                .build();

        UpdateShippingOptionRequest request = new UpdateShippingOptionRequest(new BigDecimal("22000.00"));

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(sellerRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(seller.getId())).thenReturn(Optional.of(shop));
        when(shippingOptionRepository.findByShopId(shop.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerService.updateShippingOption(request));

        assertEquals(ErrorCode.SHIPPING_OPTION_NOT_FOUND, ex.getErrorCode());
    }
}


