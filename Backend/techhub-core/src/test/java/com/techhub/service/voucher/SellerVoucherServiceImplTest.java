package com.techhub.service.voucher;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherRequest;
import com.techhub.model.dto.request.UpdateShopVoucherStatusRequest;
import com.techhub.model.dto.response.ShopVoucherDetailResponse;
import com.techhub.model.dto.response.ShopVoucherResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.*;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.impl.SellerVoucherServiceImpl;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerVoucherServiceImpl (POST, GET, GET by ID & PUT /api/seller/vouchers)")
class SellerVoucherServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private SellerVoucherServiceImpl sellerVoucherService;

    private UUID userId;
    private UUID sellerId;
    private UUID shopId;
    private Seller seller;
    private Shop shop;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        seller = Seller.builder()
                .id(sellerId)
                .sellerName("TechZone")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .build();

        shop = Shop.builder()
                .id(shopId)
                .seller(seller)
                .name("TechZone Official")
                .status(ShopStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC_SV_01: Tạo shop voucher tỷ lệ % thành công khi dữ liệu hợp lệ")
    void createShopVoucher_PercentageDiscount_Success() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "TECH10",
                "Giảm 10% đơn công nghệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("50000.00"),
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(7)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("TECH10")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            v.setCreatedAt(LocalDateTime.now());
            return v;
        });

        // Act
        ShopVoucherResponse response = sellerVoucherService.createShopVoucher(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(shopId, response.shopId());
        assertEquals("TECH10", response.code());
        assertEquals("Giảm 10% đơn công nghệ", response.name());
        assertEquals(VoucherType.SHOP, response.type());
        assertEquals(VoucherAppliesTo.PRODUCT, response.appliesTo());
        assertEquals(DiscountType.PERCENTAGE, response.discountType());
        assertEquals(new BigDecimal("10.00"), response.discountValue());
        assertEquals(new BigDecimal("100000.00"), response.minOrderAmount());
        assertEquals(new BigDecimal("50000.00"), response.maxDiscountAmount());
        assertEquals(100, response.usageLimit());
        assertEquals(1, response.usageLimitPerUser());
        assertEquals(0, response.reservedCount());
        assertEquals(0, response.usedCount());
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.UPCOMING, response.status());

        verify(voucherRepository).saveAndFlush(any(Voucher.class));
    }

    @Test
    @DisplayName("TC_SV_02: Tạo shop voucher số tiền cố định (FIXED_AMOUNT) thành công, maxDiscountAmount được gán null")
    void createShopVoucher_FixedAmountDiscount_Success() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "SHIP20K",
                "Miễn giảm 20k phí ship",
                VoucherAppliesTo.SHIPPING_FEE,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("20000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("50000.00"), // Seller nhập dư nhưng đối với fixed amount hệ thống ép null
                50,
                1,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusDays(3)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("SHIP20K")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        // Act
        ShopVoucherResponse response = sellerVoucherService.createShopVoucher(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(DiscountType.FIXED_AMOUNT, response.discountType());
        assertNull(response.maxDiscountAmount());
        assertEquals(VoucherAppliesTo.SHIPPING_FEE, response.appliesTo());

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getMaxDiscountAmount());
    }

    @Test
    @DisplayName("TC_SV_03: Tạo shop voucher với minOrderAmount null (mặc định 0) và usageLimit null (không giới hạn)")
    void createShopVoucher_OptionalFieldsNull_Success() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "NO_LIMIT",
                "Voucher không giới hạn",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("5.00"),
                null, // minOrderAmount null
                null,
                null, // usageLimit null
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("NO_LIMIT")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherResponse response = sellerVoucherService.createShopVoucher(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.minOrderAmount());
        assertNull(response.usageLimit());
    }

    @Test
    @DisplayName("TC_SV_04: Voucher code được tự động chuẩn hóa trim() và toUpperCase()")
    void createShopVoucher_TrimsAndUppercasesCode() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "  save50k  ",
                "  Giảm 50k  ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                null,
                10,
                1,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("SAVE50K")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherResponse response = sellerVoucherService.createShopVoucher(userId, request);

        // Assert
        assertEquals("SAVE50K", response.code());
        assertEquals("Giảm 50k", response.name());
        verify(voucherRepository).existsByCode("SAVE50K");
    }

    @Test
    @DisplayName("TC_SV_05: Ngày kết thúc trước hoặc bằng ngày bắt đầu ném INVALID_VOUCHER_DATES")
    void createShopVoucher_EndDateBeforeStartDate_ThrowsInvalidVoucherDates() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1); // end < start

        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "INVALID_DATE",
                "Test ngày sai",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                start,
                end
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_SV_06: Ngày bắt đầu ở quá khứ (> 5 phút trước) ném VOUCHER_START_DATE_IN_PAST")
    void createShopVoucher_StartDateInPast_ThrowsStartDateInPast() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusHours(1); // 1h in past
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "PAST_DATE",
                "Test ngày quá khứ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                start,
                end
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.VOUCHER_START_DATE_IN_PAST, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_SV_07: Giảm giá dạng PERCENTAGE lớn hơn 100% ném INVALID_DISCOUNT_VALUE")
    void createShopVoucher_PercentageExceeds100_ThrowsInvalidDiscountValue() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "OVER_100",
                "Giảm 120%",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("120.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.INVALID_DISCOUNT_VALUE, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_SV_08: Tổng lượt dùng nhỏ hơn lượt dùng mỗi user ném INVALID_USAGE_LIMIT")
    void createShopVoucher_UsageLimitLessThanPerUser_ThrowsInvalidUsageLimit() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "BAD_LIMIT",
                "Giới hạn không hợp lệ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                1, // usageLimit = 1
                2, // usageLimitPerUser = 2 (lớn hơn tổng)
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.INVALID_USAGE_LIMIT, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_SV_09: Mã voucher đã tồn tại trong DB ném VOUCHER_CODE_ALREADY_EXISTS")
    void createShopVoucher_DuplicateCodeExists_ThrowsVoucherCodeAlreadyExists() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "EXISTING_CODE",
                "Mã đã có",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("EXISTING_CODE")).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_SV_10: Race condition khi lưu vào DB gây DataIntegrityViolationException ném VOUCHER_CODE_ALREADY_EXISTS")
    void createShopVoucher_ConcurrentInsertDuplicateCode_ThrowsVoucherCodeAlreadyExists() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "RACE_CODE",
                "Race Condition Code",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("RACE_CODE")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation uk_vouchers_code"));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_11: Không tìm thấy Seller tương ứng userId ném SELLER_NOT_FOUND")
    void createShopVoucher_SellerNotFound_ThrowsSellerNotFound() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "CODE", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_12: Không tìm thấy Shop tương ứng seller ném SHOP_NOT_FOUND")
    void createShopVoucher_ShopNotFound_ThrowsShopNotFound() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "CODE", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_13: Shop đang ở trạng thái PAUSED ném SHOP_IS_PAUSED")
    void createShopVoucher_ShopPaused_ThrowsShopIsPaused() {
        // Arrange
        shop.setStatus(ShopStatus.PAUSED);

        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "CODE", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_14: Shop đang ở trạng thái SUSPENDED hoặc BANNED ném SHOP_NOT_ACTIVE")
    void createShopVoucher_ShopSuspended_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.SUSPENDED);

        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "CODE", "Name", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 10, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_15: Ngày kết thúc ở quá khứ (< hiện tại) ném INVALID_VOUCHER_DATES")
    void createShopVoucher_EndDateInPast_ThrowsInvalidVoucherDates() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusMinutes(4); // Trong ngưỡng grace period
        LocalDateTime end = LocalDateTime.now().minusMinutes(1);   // Nhưng kết thúc đã ở quá khứ

        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "PAST_END", "Hết hạn", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 100, 1,
                start, end
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SV_16: DataIntegrityViolationException không phải do code constraint được rethrow nguyên vẹn")
    void createShopVoucher_OtherDataIntegrityViolation_RethrowsException() {
        // Arrange
        CreateShopVoucherRequest request = new CreateShopVoucherRequest(
                "OTHER_ERR", "Lỗi khác", VoucherAppliesTo.PRODUCT, DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), BigDecimal.ZERO, null, 100, 1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(2)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.existsByCode("OTHER_ERR")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class)))
                .thenThrow(new DataIntegrityViolationException("foreign key violation fk_vouchers_shop_id"));

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> sellerVoucherService.createShopVoucher(userId, request));
    }

    // ==========================================
    // Tests cho GET /api/seller/vouchers (List Shop Vouchers)
    // ==========================================

    @Test
    @DisplayName("TC_SV_GET_01: Lấy danh sách voucher thành công với phân trang mặc định và sort mặc định")
    void getShopVouchers_Success_DefaultPaginationAndSort() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("SUMMER20")
                .name("Khuyến mãi mùa hè")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(2)
                .usedCount(5)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .createdAt(now.minusDays(2))
                .build();

        Page<Voucher> mockPage = new PageImpl<>(List.of(voucher), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Act
        PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());

        ShopVoucherResponse item = response.content().get(0);
        assertEquals(voucher.getId(), item.id());
        assertEquals(shopId, item.shopId());
        assertEquals("SUMMER20", item.code());
        assertEquals(VoucherType.SHOP, item.type());
        assertEquals(VoucherStatus.ONGOING, item.status());
        assertEquals(2, item.reservedCount());
        assertEquals(5, item.usedCount());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());
        assertNotNull(capturedPageable.getSort().getOrderFor("createdAt"));
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("TC_SV_GET_02: Lọc voucher theo keyword (code hoặc name)")
    void getShopVouchers_FilterByKeyword_Success() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                userId, "summer", null, null, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        verify(voucherRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SV_GET_03: Lọc voucher theo appliesTo (PRODUCT hoặc SHIPPING_FEE)")
    void getShopVouchers_FilterByAppliesTo_Success() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                userId, null, VoucherAppliesTo.SHIPPING_FEE, null, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        verify(voucherRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SV_GET_04: Lọc voucher theo discountType (PERCENTAGE hoặc FIXED_AMOUNT)")
    void getShopVouchers_FilterByDiscountType_Success() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                userId, null, null, DiscountType.FIXED_AMOUNT, null, 0, 10, "createdAt", "desc"
        );

        // Assert
        assertNotNull(response);
        verify(voucherRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SV_GET_05: Lọc voucher theo status UPCOMING, ONGOING, EXPIRED, DISABLED")
    void getShopVouchers_FilterByStatus_AllVariants() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        for (VoucherStatus st : VoucherStatus.values()) {
            PageResponse<ShopVoucherResponse> response = sellerVoucherService.getShopVouchers(
                    userId, null, null, null, st, 0, 10, "createdAt", "desc"
            );
            assertNotNull(response);
        }
        verify(voucherRepository, times(VoucherStatus.values().length)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SV_GET_06: Kiểm tra tính toán động VoucherStatus qua computeStatus đầy đủ 4 trạng thái")
    void computeStatus_AllFourStates() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Inactive -> DISABLED
        Voucher disabledVoucher = Voucher.builder()
                .isActive(false)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .build();
        assertEquals(VoucherStatus.DISABLED, SellerVoucherServiceImpl.computeStatus(disabledVoucher, now));

        // 2. Active + endDate < now -> EXPIRED
        Voucher expiredVoucher = Voucher.builder()
                .isActive(true)
                .startDate(now.minusDays(5))
                .endDate(now.minusDays(1))
                .build();
        assertEquals(VoucherStatus.EXPIRED, SellerVoucherServiceImpl.computeStatus(expiredVoucher, now));

        // 3. Active + startDate > now -> UPCOMING
        Voucher upcomingVoucher = Voucher.builder()
                .isActive(true)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(5))
                .build();
        assertEquals(VoucherStatus.UPCOMING, SellerVoucherServiceImpl.computeStatus(upcomingVoucher, now));

        // 4. Active + startDate <= now && endDate >= now -> ONGOING
        Voucher ongoingVoucher = Voucher.builder()
                .isActive(true)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .build();
        assertEquals(VoucherStatus.ONGOING, SellerVoucherServiceImpl.computeStatus(ongoingVoucher, now));

        // 5. Null-safety guards
        assertNull(SellerVoucherServiceImpl.computeStatus(null, now));
        assertNull(ShopVoucherResponse.from(null, now));
        assertNotNull(SellerVoucherServiceImpl.computeStatus(ongoingVoucher, null));
    }

    @Test
    @DisplayName("TC_SV_GET_07: Tự động chuẩn hóa phân trang khi page âm (< 0) hoặc size vượt ngưỡng ([1, 100])")
    void getShopVouchers_PaginationSanitization() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Case 1: page = -5, size = 500 -> sanitized to page = 0, size = 100
        sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, -5, 500, "createdAt", "desc"
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());

        // Case 2: size = 0 -> sanitized to size = 1
        sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 2, 0, "createdAt", "desc"
        );
        verify(voucherRepository, times(2)).findAll(any(Specification.class), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("TC_SV_GET_08: Sắp xếp an toàn theo Whitelist và hỗ trợ snake_case, fallback khi trường không hợp lệ")
    void getShopVouchers_SortWhitelistAndSnakeCaseMapping() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        // Case 1: snake_case 'start_date' và 'asc'
        sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 0, 10, "start_date", "asc"
        );
        verify(voucherRepository).findAll(any(Specification.class), captor.capture());
        assertNotNull(captor.getValue().getSort().getOrderFor("startDate"));
        assertEquals(Sort.Direction.ASC, captor.getValue().getSort().getOrderFor("startDate").getDirection());

        // Case 2: Invalid field 'hacker_column' -> fallback 'createdAt'
        sellerVoucherService.getShopVouchers(
                userId, null, null, null, null, 0, 10, "hacker_column", "desc"
        );
        verify(voucherRepository, times(2)).findAll(any(Specification.class), captor.capture());
        assertNotNull(captor.getValue().getSort().getOrderFor("createdAt"));
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("TC_SV_GET_09: Shop ở trạng thái ACTIVE, PAUSED hoặc SUSPENDED đều được cấp quyền xem (Read-Only Audit)")
    void getShopVouchers_ShopStatusAllowed_ActivePausedSuspended() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // ACTIVE
        shop.setStatus(ShopStatus.ACTIVE);
        assertDoesNotThrow(() -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        // PAUSED
        shop.setStatus(ShopStatus.PAUSED);
        assertDoesNotThrow(() -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));

        // SUSPENDED
        shop.setStatus(ShopStatus.SUSPENDED);
        assertDoesNotThrow(() -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));
    }

    @Test
    @DisplayName("TC_SV_GET_10: Shop ở trạng thái BANNED bị từ chối truy cập ném SHOP_NOT_ACTIVE")
    void getShopVouchers_ShopStatusBanned_ThrowsShopNotActive() {
        // Arrange
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SV_GET_11: Không tìm thấy Seller tương ứng userId ném SELLER_NOT_FOUND")
    void getShopVouchers_SellerNotFound_ThrowsSellerNotFound() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC_SV_GET_12: Không tìm thấy Shop tương ứng sellerId ném SHOP_NOT_FOUND")
    void getShopVouchers_ShopNotFound_ThrowsShopNotFound() {
        // Arrange
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVouchers(userId, null, null, null, null, 0, 10, "createdAt", "desc"));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // Tests cho GET /api/seller/vouchers/{id} (View Shop Voucher Details)
    // ==========================================

    @Test
    @DisplayName("TC_SVD_01: Lấy chi tiết voucher thành công cho voucher đang diễn ra (ONGOING), tính đúng remainingCount và redemptionRate")
    void getShopVoucherDetail_OngoingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("SUMMER20")
                .name("Summer Sale 20%")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(5)
                .usedCount(20)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(voucherId, response.id());
        assertEquals(shopId, response.shopId());
        assertEquals("SUMMER20", response.code());
        assertEquals("Summer Sale 20%", response.name());
        assertEquals(VoucherType.SHOP, response.type());
        assertEquals(VoucherAppliesTo.PRODUCT, response.appliesTo());
        assertEquals(DiscountType.PERCENTAGE, response.discountType());
        assertEquals(new BigDecimal("20.00"), response.discountValue());
        assertEquals(new BigDecimal("100000.00"), response.minOrderAmount());
        assertEquals(new BigDecimal("50000.00"), response.maxDiscountAmount());
        assertEquals(100, response.usageLimit());
        assertEquals(1, response.usageLimitPerUser());
        assertEquals(5, response.reservedCount());
        assertEquals(20, response.usedCount());
        assertEquals(80, response.remainingCount());
        assertEquals(20.00, response.redemptionRate());
        assertEquals(VoucherStatus.ONGOING, response.status());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("TC_SVD_02: Lấy chi tiết voucher thành công cho voucher sắp diễn ra (UPCOMING)")
    void getShopVoucherDetail_UpcomingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UPCOMING10")
                .name("Upcoming Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(50)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(7))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(VoucherStatus.UPCOMING, response.status());
        assertEquals(50, response.remainingCount());
        assertEquals(0.00, response.redemptionRate());
    }

    @Test
    @DisplayName("TC_SVD_03: Lấy chi tiết voucher thành công cho voucher đã hết hạn (EXPIRED)")
    void getShopVoucherDetail_ExpiredVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("EXPIRED10")
                .name("Expired Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("10000.00"))
                .usageLimit(50)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(30)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(2))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(VoucherStatus.EXPIRED, response.status());
        assertEquals(20, response.remainingCount());
        assertEquals(60.00, response.redemptionRate());
    }

    @Test
    @DisplayName("TC_SVD_04: Lấy chi tiết voucher thành công cho voucher bị tắt (DISABLED) do isActive = false")
    void getShopVoucherDetail_DisabledVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DISABLED10")
                .name("Disabled Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(10)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(false)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(VoucherStatus.DISABLED, response.status());
        assertFalse(response.isActive());
    }

    @Test
    @DisplayName("TC_SVD_05: Voucher không giới hạn số lượt dùng (usageLimit = null), remainingCount và redemptionRate trả về null")
    void getShopVoucherDetail_UnlimitedUsageLimit_MetricsAreNull() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UNLIMITED")
                .name("Unlimited Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("20000.00"))
                .usageLimit(null)
                .usageLimitPerUser(1)
                .reservedCount(3)
                .usedCount(50)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertNull(response.usageLimit());
        assertNull(response.remainingCount());
        assertNull(response.redemptionRate());
        assertEquals(50, response.usedCount());
    }

    @Test
    @DisplayName("TC_SVD_06: Voucher đã dùng hết hoặc vượt ngưỡng (usedCount >= usageLimit), remainingCount = 0 và redemptionRate >= 100.0")
    void getShopVoucherDetail_ExhaustedUsageLimit_RemainingCountZeroClamped() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("EXHAUSTED")
                .name("Exhausted Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("15.00"))
                .usageLimit(50)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(55)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.remainingCount());
        assertEquals(110.00, response.redemptionRate());
    }

    @Test
    @DisplayName("TC_SVD_07: Voucher chưa ai dùng (usedCount = 0), remainingCount = usageLimit và redemptionRate = 0.0")
    void getShopVoucherDetail_ZeroUsedCount_RemainingEqualsUsageLimit() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("NEWVOUCHER")
                .name("New Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.remainingCount());
        assertEquals(0.00, response.redemptionRate());
    }

    @Test
    @DisplayName("TC_SVD_08: Tỷ lệ quy đổi làm tròn chuẩn 2 chữ số thập phân (usedCount = 1, usageLimit = 3 -> 33.33%)")
    void getShopVoucherDetail_RoundingRedemptionRate_TwoDecimalPlaces() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("ROUNDING")
                .name("Rounding Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(3)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(1)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.remainingCount());
        assertEquals(33.33, response.redemptionRate());
    }

    @Test
    @DisplayName("TC_SVD_09: Chống rò rỉ IDOR: Voucher thuộc về Shop khác ném VOUCHER_NOT_FOUND (HTTP 404)")
    void getShopVoucherDetail_OtherShopVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Shop otherShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Other Competitor Shop")
                .status(ShopStatus.ACTIVE)
                .build();

        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(otherShop)
                .code("OTHER10")
                .name("Other Shop Voucher")
                .type(VoucherType.SHOP)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVD_10: Chống truy cập chéo loại: Voucher là PLATFORM voucher ném VOUCHER_NOT_FOUND (HTTP 404)")
    void getShopVoucherDetail_PlatformVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher platformVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .code("PLATFORM10")
                .name("Platform FreeShip")
                .type(VoucherType.PLATFORM)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(platformVoucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVD_11: Voucher ID không tồn tại trong DB ném VOUCHER_NOT_FOUND (HTTP 404)")
    void getShopVoucherDetail_VoucherNotFound_ThrowsVoucherNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, nonExistentId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVD_12: Shop ở trạng thái BANNED bị từ chối truy cập ném SHOP_NOT_ACTIVE (HTTP 400)")
    void getShopVoucherDetail_ShopStatusBanned_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.BANNED);
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, voucherId));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVD_13: Shop ở trạng thái PAUSED được phép đọc chi tiết voucher (Read-only access theo BR-07)")
    void getShopVoucherDetail_ShopStatusPaused_AllowedReadOnly() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.PAUSED);
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("PAUSED10")
                .name("Paused Shop Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(2))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        assertDoesNotThrow(() -> {
            ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);
            assertNotNull(response);
            assertEquals("PAUSED10", response.code());
        });
    }

    @Test
    @DisplayName("TC_SVD_14: Shop ở trạng thái SUSPENDED được phép đọc chi tiết voucher (Read-only access theo BR-07)")
    void getShopVoucherDetail_ShopStatusSuspended_AllowedReadOnly() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.SUSPENDED);
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("SUSPEND10")
                .name("Suspended Shop Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(2))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        assertDoesNotThrow(() -> {
            ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);
            assertNotNull(response);
            assertEquals("SUSPEND10", response.code());
        });
    }

    @Test
    @DisplayName("TC_SVD_15: Không tìm thấy Seller hoặc Shop tương ứng với userId ném SELLER_NOT_FOUND hoặc SHOP_NOT_FOUND")
    void getShopVoucherDetail_SellerOrShopNotFound_ThrowsException() {
        UUID voucherId = UUID.randomUUID();

        // Sub-case 1: Seller not found
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, voucherId));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex1.getErrorCode());

        // Sub-case 2: Shop not found
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());
        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> sellerVoucherService.getShopVoucherDetail(userId, voucherId));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVD_16: Dữ liệu biên: Voucher có usedCount và reservedCount null vẫn an toàn (fallback về 0, không ném NPE)")
    void getShopVoucherDetail_NullCounts_FallbackToZeroSafely() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("NULLCOUNTS")
                .name("Null Counts Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(null) // dirty/null data
                .usedCount(null)     // dirty/null data
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.getShopVoucherDetail(userId, voucherId);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.usedCount());
        assertEquals(0, response.reservedCount());
        assertEquals(100, response.remainingCount());
        assertEquals(0.00, response.redemptionRate());
    }

    // ==========================================
    // Tests cho PUT /api/seller/vouchers/{id} (Update Shop Voucher)
    // ==========================================

    @Test
    @DisplayName("TC_SVU_01: Cập nhật thành công toàn bộ thông tin voucher sắp diễn ra (UPCOMING)")
    void updateShopVoucher_UpcomingVoucher_FullSuccess() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UPCOMING10")
                .name("Old Name")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(new BigDecimal("50000.00"))
                .maxDiscountAmount(new BigDecimal("20000.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(10))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "New Upcoming Name",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("50000.00"),
                200,
                2,
                now.plusDays(3),
                now.plusDays(15)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertEquals("New Upcoming Name", response.name());
        assertEquals(new BigDecimal("20.00"), response.discountValue());
        assertEquals(new BigDecimal("100000.00"), response.minOrderAmount());
        assertEquals(new BigDecimal("50000.00"), response.maxDiscountAmount());
        assertEquals(200, response.usageLimit());
        assertEquals(2, response.usageLimitPerUser());
        assertEquals(request.startDate(), response.startDate());
        assertEquals(request.endDate(), response.endDate());
        assertEquals(VoucherStatus.UPCOMING, response.status());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVU_02: Can thiệp cắt lỗ thành công voucher đang diễn ra (ONGOING): hạ mức giảm, rút ngắn ngày kết thúc")
    void updateShopVoucher_OngoingVoucher_LossCuttingSuccess() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime originalStart = now.minusDays(2);
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("ONGOING50")
                .name("Catastrophic 50% discount")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .maxDiscountAmount(null)
                .usageLimit(1000)
                .usageLimitPerUser(5)
                .reservedCount(3)
                .usedCount(20)
                .startDate(originalStart)
                .endDate(now.plusDays(10))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Safe 10% discount capped",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), // Hạ từ 50% xuống 10%
                new BigDecimal("200000.00"), // Nâng min order lên 200k
                new BigDecimal("30000.00"), // Khóa trần 30k
                100, // Hạ usage limit xuống 100
                1,
                now.plusDays(1), // Client gửi startDate mới (sẽ bị khóa)
                now.plusDays(2)  // Rút ngắn ngày kết thúc để cắt lỗ
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Safe 10% discount capped", response.name());
        assertEquals(new BigDecimal("10.00"), response.discountValue());
        assertEquals(new BigDecimal("200000.00"), response.minOrderAmount());
        assertEquals(new BigDecimal("30000.00"), response.maxDiscountAmount());
        assertEquals(100, response.usageLimit());
        assertEquals(originalStart, response.startDate()); // Bất biến ngày bắt đầu
        assertEquals(request.endDate(), response.endDate());
        assertEquals(VoucherStatus.ONGOING, response.status());
    }

    @Test
    @DisplayName("TC_SVU_03: Voucher đang diễn ra (ONGOING) bỏ qua startDate gửi từ client và giữ nguyên startDate cũ")
    void updateShopVoucher_OngoingVoucher_IgnoresClientStartDate() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime originalStart = now.minusDays(3);
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("ONGOING10")
                .name("Ongoing Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(5)
                .startDate(originalStart)
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Updated Name",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                now.plusDays(1), // Cố tình đổi startDate
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, voucherId, request);

        // Assert
        assertEquals(originalStart, response.startDate());
    }

    @Test
    @DisplayName("TC_SVU_04: Chuyển discountType sang FIXED_AMOUNT tự động xóa maxDiscountAmount về null")
    void updateShopVoucher_SwitchToFixedAmount_ClearsMaxDiscountAmount() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("PERCENT10")
                .name("Percent Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .maxDiscountAmount(new BigDecimal("20000.00"))
                .usageLimit(50)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Now Fixed Amount",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("50000.00"), // Client vẫn gửi maxDiscountAmount thừa
                50,
                1,
                now.plusDays(1),
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertEquals(DiscountType.FIXED_AMOUNT, response.discountType());
        assertNull(response.maxDiscountAmount());
    }

    @Test
    @DisplayName("TC_SVU_05: Chuyển sang PERCENTAGE có discountValue > 100 ném INVALID_DISCOUNT_VALUE")
    void updateShopVoucher_PercentageExceeds100_ThrowsInvalidDiscountValue() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("CODE10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .usageLimit(50)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Invalid Percent",
                DiscountType.PERCENTAGE,
                new BigDecimal("150.00"), // Vượt quá 100%
                BigDecimal.ZERO,
                null,
                50,
                1,
                now.plusDays(1),
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.INVALID_DISCOUNT_VALUE, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVU_06: Cập nhật usageLimit >= usedCount thành công")
    void updateShopVoucher_UsageLimitGreaterOrEqualToUsedCount_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("USAGE100")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(20) // Đã dùng 20 lượt
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                50, // Hạ từ 100 xuống 50 (vẫn >= 20)
                1,
                null,
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucher(userId, voucherId, request);

        // Assert
        assertEquals(50, response.usageLimit());
        assertEquals(30, response.remainingCount());
    }

    @Test
    @DisplayName("TC_SVU_07: Hạ usageLimit < usedCount thất bại và ném USAGE_LIMIT_LESS_THAN_USED")
    void updateShopVoucher_UsageLimitLessThanUsedCount_ThrowsUsageLimitLessThanUsed() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("USAGE100")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(20) // Đã dùng 20 lượt
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                15, // Hạ xuống 15 (< 20 used)
                1,
                null,
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.USAGE_LIMIT_LESS_THAN_USED, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVU_08: Cấu hình usageLimit < usageLimitPerUser thất bại và ném INVALID_USAGE_LIMIT")
    void updateShopVoucher_UsageLimitLessThanPerUser_ThrowsInvalidUsageLimit() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("USAGE100")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                3, // usageLimit = 3
                5, // perUser = 5
                now.plusDays(1),
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.INVALID_USAGE_LIMIT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVU_09: Cố tình cập nhật voucher đã hết hạn (EXPIRED) ném VOUCHER_ALREADY_EXPIRED")
    void updateShopVoucher_ExpiredVoucher_ThrowsVoucherAlreadyExpired() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher expiredVoucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("EXPIRED10")
                .name("Expired Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(2)) // Đã hết hạn
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Try Update",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                now.plusDays(1),
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(expiredVoucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_ALREADY_EXPIRED, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVU_10: EndDate <= StartDate thất bại và ném INVALID_VOUCHER_DATES")
    void updateShopVoucher_EndDateBeforeStartDate_ThrowsInvalidVoucherDates() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DATES10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                now.plusDays(5),
                now.plusDays(3) // endDate trước startDate
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVU_11: EndDate ở quá khứ thất bại và ném INVALID_VOUCHER_DATES")
    void updateShopVoucher_EndDateInPast_ThrowsInvalidVoucherDates() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DATES10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.minusHours(1))
                .endDate(now.plusDays(2))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                null,
                now.minusMinutes(1) // endDate ở quá khứ
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVU_12: StartDate ở quá khứ đối với voucher UPCOMING ném VOUCHER_START_DATE_IN_PAST")
    void updateShopVoucher_UpcomingStartDateInPast_ThrowsStartDateInPast() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UPCOMING10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                now.minusHours(1), // startDate ở quá khứ (vượt grace period 5m)
                now.plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_START_DATE_IN_PAST, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVU_13: Chống rò rỉ IDOR: Cập nhật voucher thuộc Shop khác ném VOUCHER_NOT_FOUND (HTTP 404)")
    void updateShopVoucher_OtherShopVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Shop otherShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Other Competitor")
                .status(ShopStatus.ACTIVE)
                .build();

        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(otherShop) // Thuộc shop khác
                .code("OTHER10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Hacked Name",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                100,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVU_14: Voucher ID không tồn tại trong DB ném VOUCHER_NOT_FOUND (HTTP 404)")
    void updateShopVoucher_VoucherNotFound_ThrowsVoucherNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Name", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, 100, 1, null, LocalDateTime.now().plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, nonExistentId, request));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC_SVU_15: Shop ở trạng thái PAUSED bị từ chối cập nhật ném SHOP_IS_PAUSED (BR-07)")
    void updateShopVoucher_ShopStatusPaused_ThrowsShopIsPaused() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.PAUSED);
        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Name", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, 100, 1, null, LocalDateTime.now().plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVU_16: Shop ở trạng thái SUSPENDED hoặc BANNED bị từ chối cập nhật ném SHOP_NOT_ACTIVE (BR-07)")
    void updateShopVoucher_ShopStatusSuspendedOrBanned_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.SUSPENDED);
        UpdateShopVoucherRequest request = new UpdateShopVoucherRequest(
                "Name", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, 100, 1, null, LocalDateTime.now().plusDays(5)
        );

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucher(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    // ==========================================
    // Tests cho PATCH /api/seller/vouchers/{id}/status (Toggle Shop Voucher Status)
    // ==========================================

    @Test
    @DisplayName("TC_SVS_01: Tạm dừng thành công voucher đang diễn ra (ONGOING -> DISABLED)")
    void toggleShopVoucherStatus_DeactivateOngoingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("ONGOING10")
                .name("Ongoing Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(2)
                .usedCount(15)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(VoucherStatus.DISABLED, response.status());
        assertEquals(85, response.remainingCount());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVS_02: Kích hoạt lại thành công voucher đang trong khung thời gian (DISABLED -> ONGOING)")
    void toggleShopVoucherStatus_ReactivateDisabledOngoingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DISABLED_ONGOING")
                .name("Paused Ongoing Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(false)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.ONGOING, response.status());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVS_03: Kích hoạt lại thành công voucher sắp diễn ra (DISABLED -> UPCOMING)")
    void toggleShopVoucherStatus_ReactivateDisabledUpcomingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DISABLED_UPCOMING")
                .name("Paused Upcoming Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(7))
                .isActive(false)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.UPCOMING, response.status());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVS_04: Tạm dừng thành công voucher sắp diễn ra (UPCOMING -> DISABLED)")
    void toggleShopVoucherStatus_DeactivateUpcomingVoucher_Success() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UPCOMING10")
                .name("Upcoming Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(7))
                .isActive(true)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(VoucherStatus.DISABLED, response.status());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVS_05: Xử lý Idempotent an toàn khi gửi trạng thái trùng với trạng thái hiện tại (No-op)")
    void toggleShopVoucherStatus_AlreadySameStatus_IdempotentSuccess() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("SAME_STATUS")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(false) // Đang tắt sẵn
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false); // Tiếp tục gửi tắt

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShopVoucherDetailResponse response = sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request);

        // Assert
        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(VoucherStatus.DISABLED, response.status());
        verify(voucherRepository).save(voucher);
    }

    @Test
    @DisplayName("TC_SVS_06: Cố tình thay đổi trạng thái voucher đã hết hạn (EXPIRED) ném VOUCHER_ALREADY_EXPIRED")
    void toggleShopVoucherStatus_ExpiredVoucher_ThrowsVoucherAlreadyExpired() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher expiredVoucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("EXPIRED10")
                .name("Expired Voucher")
                .type(VoucherType.SHOP)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(2)) // Đã hết hạn
                .isActive(false)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(true);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(expiredVoucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_ALREADY_EXPIRED, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVS_07: Chống rò rỉ IDOR: Đổi trạng thái voucher thuộc Shop khác ném VOUCHER_NOT_FOUND")
    void toggleShopVoucherStatus_OtherShopVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Shop otherShop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Other Competitor")
                .status(ShopStatus.ACTIVE)
                .build();

        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(otherShop) // Thuộc shop khác
                .code("OTHER10")
                .name("Voucher")
                .type(VoucherType.SHOP)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVS_08: Chống thao tác chéo loại: Voucher là PLATFORM voucher ném VOUCHER_NOT_FOUND")
    void toggleShopVoucherStatus_PlatformVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher platformVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .code("PLATFORM10")
                .name("Platform Voucher")
                .type(VoucherType.PLATFORM)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(platformVoucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVS_09: Voucher ID không tồn tại trong DB ném VOUCHER_NOT_FOUND")
    void toggleShopVoucherStatus_VoucherNotFoundInDB_ThrowsVoucherNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, nonExistentId, request));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVS_10: Shop ở trạng thái PAUSED bị từ chối đổi trạng thái ném SHOP_IS_PAUSED (BR-07)")
    void toggleShopVoucherStatus_ShopPaused_ThrowsShopIsPaused() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.PAUSED);
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVS_11: Shop ở trạng thái SUSPENDED bị từ chối đổi trạng thái ném SHOP_NOT_ACTIVE (BR-07)")
    void toggleShopVoucherStatus_ShopSuspended_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.SUSPENDED);
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVS_12: Shop ở trạng thái BANNED bị từ chối đổi trạng thái ném SHOP_NOT_ACTIVE (BR-07)")
    void toggleShopVoucherStatus_ShopBanned_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.BANNED);
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVS_13: Seller hoặc Shop không tồn tại ném SELLER_NOT_FOUND hoặc SHOP_NOT_FOUND")
    void toggleShopVoucherStatus_SellerOrShopNotFound_ThrowsNotFound() {
        UUID voucherId = UUID.randomUUID();
        UpdateShopVoucherStatusRequest request = new UpdateShopVoucherStatusRequest(false);

        // Sub-case 1: Seller not found
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex1.getErrorCode());

        // Sub-case 2: Shop not found
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());
        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> sellerVoucherService.updateShopVoucherStatus(userId, voucherId, request));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex2.getErrorCode());
    }

    // ==========================================
    // Tests cho DELETE /api/seller/vouchers/{id} (Delete Shop Voucher)
    // ==========================================

    @Test
    @DisplayName("TC_SVD_01: Xóa thành công voucher chưa phát sinh lượt dùng (usedCount=0, reservedCount=0) bằng Hard Delete")
    void deleteShopVoucher_UnusedVoucher_HardDeleteSuccess() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("UNUSED10")
                .name("Unused Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usedCount(0)
                .reservedCount(0)
                .isDeleted(false)
                .isActive(true)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act
        sellerVoucherService.deleteShopVoucher(userId, voucherId);

        // Assert
        verify(voucherRepository).delete(voucher);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVD_02: Xóa thành công voucher đã phát sinh lượt dùng (usedCount>0, reservedCount=0) bằng Soft Delete (BR-16D)")
    void deleteShopVoucher_UsedVoucher_SoftDeleteSuccess() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("USED10")
                .name("Used Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usedCount(15)
                .reservedCount(0)
                .isDeleted(false)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sellerVoucherService.deleteShopVoucher(userId, voucherId);

        // Assert
        assertTrue(voucher.isDeleted());
        assertFalse(voucher.isActive());
        assertNotNull(voucher.getDeletedAt());
        verify(voucherRepository).save(voucher);
        verify(voucherRepository, never()).delete(any(Voucher.class));
    }

    @Test
    @DisplayName("TC_SVD_03: Từ chối xóa voucher đang có lượt giữ chỗ thanh toán ném VOUCHER_HAS_ACTIVE_RESERVATIONS (BR-16B)")
    void deleteShopVoucher_ActiveReservations_ThrowsException() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("RESERVED10")
                .name("Reserved Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usedCount(5)
                .reservedCount(2)
                .isDeleted(false)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_HAS_ACTIVE_RESERVATIONS, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVD_04: Từ chối xóa voucher đã bị soft delete trước đó ném VOUCHER_NOT_FOUND (404)")
    void deleteShopVoucher_AlreadySoftDeleted_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .code("DELETED10")
                .name("Deleted Voucher")
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usedCount(10)
                .reservedCount(0)
                .isDeleted(true)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .isActive(false)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SVD_05: IDOR Protection - Từ chối xóa voucher thuộc Shop khác ném VOUCHER_NOT_FOUND")
    void deleteShopVoucher_OtherShopVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Shop otherShop = Shop.builder().id(UUID.randomUUID()).build();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .shop(otherShop)
                .code("OTHER10")
                .name("Other Shop Voucher")
                .type(VoucherType.SHOP)
                .isDeleted(false)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
    }

    @Test
    @DisplayName("TC_SVD_06: IDOR Protection - Từ chối xóa voucher Platform toàn sàn ném VOUCHER_NOT_FOUND")
    void deleteShopVoucher_PlatformVoucher_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        Voucher platformVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .code("PLATFORM50")
                .name("Platform Voucher")
                .type(VoucherType.PLATFORM)
                .isDeleted(false)
                .build();

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(platformVoucher));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
    }

    @Test
    @DisplayName("TC_SVD_07: Từ chối khi voucher ID không tồn tại trong DB ném VOUCHER_NOT_FOUND")
    void deleteShopVoucher_NotFound_ThrowsVoucherNotFound() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));
        when(voucherRepository.findById(voucherId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
    }

    @Test
    @DisplayName("TC_SVD_08: Shop ở trạng thái PAUSED bị từ chối xóa ném SHOP_IS_PAUSED (BR-07)")
    void deleteShopVoucher_ShopPaused_ThrowsShopIsPaused() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.PAUSED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.SHOP_IS_PAUSED, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVD_09: Shop ở trạng thái SUSPENDED bị từ chối xóa ném SHOP_NOT_ACTIVE (BR-07)")
    void deleteShopVoucher_ShopSuspended_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.SUSPENDED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVD_10: Shop ở trạng thái BANNED bị từ chối xóa ném SHOP_NOT_ACTIVE (BR-07)")
    void deleteShopVoucher_ShopBanned_ThrowsShopNotActive() {
        // Arrange
        UUID voucherId = UUID.randomUUID();
        shop.setStatus(ShopStatus.BANNED);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(shop));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC_SVD_11: Seller không tồn tại ném SELLER_NOT_FOUND")
    void deleteShopVoucher_SellerNotFound_ThrowsNotFound() {
        UUID voucherId = UUID.randomUUID();
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(shopRepository, never()).findBySellerId(any());
    }

    @Test
    @DisplayName("TC_SVD_12: Shop không tồn tại ném SHOP_NOT_FOUND")
    void deleteShopVoucher_ShopNotFound_ThrowsNotFound() {
        UUID voucherId = UUID.randomUUID();
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerVoucherService.deleteShopVoucher(userId, voucherId));
        assertEquals(ErrorCode.SHOP_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }
}

