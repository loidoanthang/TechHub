package com.techhub.service.voucher;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.CreatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherRequest;
import com.techhub.model.dto.request.UpdatePlatformVoucherStatusRequest;
import com.techhub.model.dto.response.AdminVoucherDetailResponse;
import com.techhub.model.dto.response.AdminVoucherResponse;
import com.techhub.model.dto.response.PlatformVoucherResponse;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import com.techhub.repository.VoucherRepository;
import com.techhub.service.voucher.impl.AdminVoucherServiceImpl;
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
@DisplayName("Unit Tests cho AdminVoucherServiceImpl (POST /api/admin/vouchers)")
class AdminVoucherServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private AdminVoucherServiceImpl adminVoucherService;

    @Test
    @DisplayName("TC01: Tạo Platform Product Voucher thành công với PERCENTAGE")
    void testCreatePlatformVoucher_Percentage_Success() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(7);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "TECHHUB10",
                "Giảm 10% đơn từ 200k",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(50000),
                500,
                1,
                start,
                end
        );

        when(voucherRepository.existsByCode("TECHHUB10")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("TECHHUB10", response.code());
        assertEquals(VoucherType.PLATFORM, response.type());
        assertEquals(VoucherAppliesTo.PRODUCT, response.appliesTo());
        assertEquals(DiscountType.PERCENTAGE, response.discountType());
        assertEquals(BigDecimal.valueOf(10), response.discountValue());
        assertEquals(BigDecimal.valueOf(50000), response.maxDiscountAmount());
        assertEquals(500, response.usageLimit());
        assertEquals(1, response.usageLimitPerUser());
        assertEquals(0, response.reservedCount());
        assertEquals(0, response.usedCount());
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.UPCOMING, response.status());

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).saveAndFlush(captor.capture());
        Voucher saved = captor.getValue();
        assertNull(saved.getShop());
        assertEquals(VoucherType.PLATFORM, saved.getType());
        assertFalse(saved.isDeleted());
    }

    @Test
    @DisplayName("TC02: Tạo Platform FreeShip Voucher thành công với FIXED_AMOUNT (maxDiscountAmount tự động set về null)")
    void testCreatePlatformVoucher_FixedAmount_ClearsCap_Success() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(2);
        LocalDateTime end = LocalDateTime.now().plusDays(3);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "FREESHIP15K",
                "Miễn phí ship 15k",
                VoucherAppliesTo.SHIPPING_FEE,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(30000), // truyền thừa
                null, // không giới hạn tổng lượt dùng
                2,
                start,
                end
        );

        when(voucherRepository.existsByCode("FREESHIP15K")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);

        assertNotNull(response);
        assertEquals("FREESHIP15K", response.code());
        assertNull(response.maxDiscountAmount());
        assertNull(response.usageLimit());
        assertEquals(2, response.usageLimitPerUser());
        assertEquals(VoucherStatus.ONGOING, response.status());
    }

    @Test
    @DisplayName("TC03: Chuẩn hóa mã voucher in thường và khoảng trắng sang in hoa")
    void testCreatePlatformVoucher_NormalizesCode() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                " summer_2026 ",
                "  Summer Sale  ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20),
                null,
                null,
                100,
                1,
                start,
                end
        );

        when(voucherRepository.existsByCode("SUMMER_2026")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);

        assertEquals("SUMMER_2026", response.code());
        assertEquals("Summer Sale", response.name());
        verify(voucherRepository).existsByCode("SUMMER_2026");
    }

    @Test
    @DisplayName("TC04: minOrderAmount null tự động fallback về BigDecimal.ZERO")
    void testCreatePlatformVoucher_NullMinOrderAmount_DefaultsToZero() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "TECHHUBZERO",
                "Không đơn tối thiểu",
                VoucherAppliesTo.PRODUCT,
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(10000),
                null,
                null,
                50,
                1,
                start,
                end
        );

        when(voucherRepository.existsByCode("TECHHUBZERO")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);

        assertEquals(BigDecimal.ZERO, response.minOrderAmount());
    }

    @Test
    @DisplayName("TC05: Mã voucher đã tồn tại ném VOUCHER_CODE_ALREADY_EXISTS")
    void testCreatePlatformVoucher_DuplicateCode_ThrowsException() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "DUPLICATE100",
                "Mã trùng",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        when(voucherRepository.existsByCode("DUPLICATE100")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC06: endDate trước hoặc bằng startDate ném INVALID_VOUCHER_DATES")
    void testCreatePlatformVoucher_EndDateBeforeStartDate_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "ERRORDATE",
                "Lỗi ngày",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                start,
                end
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC07: endDate ở quá khứ ném INVALID_VOUCHER_DATES")
    void testCreatePlatformVoucher_EndDateInPast_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(5);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "ERRORDATEPAST",
                "Lỗi ngày quá khứ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                start,
                end
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC08: startDate ở quá khứ sâu hơn 5 phút ném VOUCHER_START_DATE_IN_PAST")
    void testCreatePlatformVoucher_StartDateDeepInPast_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "ERRORSTARTPAST",
                "Lỗi bắt đầu quá khứ",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                start,
                end
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.VOUCHER_START_DATE_IN_PAST, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC09: startDate trong phạm vi Grace Period (<= 5 phút trước) được chấp nhận thành công")
    void testCreatePlatformVoucher_StartDateWithinGracePeriod_Success() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(3);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "GRACEPERIODOK",
                "Chấp nhận trong 5 phút",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                start,
                end
        );

        when(voucherRepository.existsByCode("GRACEPERIODOK")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformVoucherResponse response = adminVoucherService.createPlatformVoucher(request);

        assertNotNull(response);
        assertEquals("GRACEPERIODOK", response.code());
    }

    @Test
    @DisplayName("TC10: discountValue vượt quá 100% khi PERCENTAGE ném INVALID_DISCOUNT_VALUE")
    void testCreatePlatformVoucher_PercentageExceeds100_ThrowsException() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "OVER100PCT",
                "Vượt 100%",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(105),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.INVALID_DISCOUNT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC11: usageLimit nhỏ hơn usageLimitPerUser ném INVALID_USAGE_LIMIT")
    void testCreatePlatformVoucher_UsageLimitLessThanPerUser_ThrowsException() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "INVALIDLIMIT",
                "Lỗi giới hạn dùng",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                2,
                5, // per user lớn hơn tổng limit
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.INVALID_USAGE_LIMIT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC14: DataIntegrityViolationException do trùng mã code khi race condition được dịch sang VOUCHER_CODE_ALREADY_EXISTS")
    void testCreatePlatformVoucher_RaceConditionConstraintViolation_ThrowsConflict() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "RACECOND",
                "Race Condition",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        when(voucherRepository.existsByCode("RACECOND")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_vouchers_code"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );

        assertEquals(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC15: DataIntegrityViolationException không liên quan code được rethrow nguyên bản")
    void testCreatePlatformVoucher_OtherIntegrityViolation_Rethrows() {
        CreatePlatformVoucherRequest request = new CreatePlatformVoucherRequest(
                "OTHERERR",
                "Lỗi DB khác",
                VoucherAppliesTo.PRODUCT,
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                null,
                null,
                null,
                1,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(2)
        );

        when(voucherRepository.existsByCode("OTHERERR")).thenReturn(false);
        when(voucherRepository.saveAndFlush(any(Voucher.class)))
                .thenThrow(new DataIntegrityViolationException("foreign key violation foreign_table_fkey"));

        assertThrows(DataIntegrityViolationException.class, () ->
                adminVoucherService.createPlatformVoucher(request)
        );
    }

    // ==========================================
    // Tests for GET /api/admin/vouchers (getVouchers)
    // ==========================================

    @Test
    @DisplayName("TC16 (GET): Lấy danh sách voucher mặc định (phân trang, sắp xếp createdAt desc, tie-breaker id desc)")
    void testGetVouchers_DefaultPaginationAndSort_Success() {
        LocalDateTime now = LocalDateTime.now();
        Voucher platformVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(null)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("PLATFORM10")
                .name("Giảm 10% toàn sàn")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(100000))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(0)
                .usedCount(5)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .createdAt(now.minusDays(2))
                .build();

        Page<Voucher> mockPage = new PageImpl<>(List.of(platformVoucher), PageRequest.of(0, 20), 1);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminVoucherResponse> response = adminVoucherService.getVouchers(
                null, null, null, null, null, null, 0, 20, null, null
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());

        AdminVoucherResponse item = response.content().get(0);
        assertEquals("PLATFORM10", item.code());
        assertEquals(VoucherType.PLATFORM, item.type());
        assertNull(item.shopId());
        assertNull(item.shopName());
        assertNull(item.shopStatus());
        assertEquals(VoucherStatus.ONGOING, item.status());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        Sort.Order firstOrder = pageable.getSort().getOrderFor("createdAt");
        assertNotNull(firstOrder);
        assertEquals(Sort.Direction.DESC, firstOrder.getDirection());
        Sort.Order secondOrder = pageable.getSort().getOrderFor("id");
        assertNotNull(secondOrder);
        assertEquals(Sort.Direction.DESC, secondOrder.getDirection());
    }

    @Test
    @DisplayName("TC17 (GET): Lấy voucher của Shop bao gồm đầy đủ shopId, shopName, shopStatus (Active)")
    void testGetVouchers_ShopVoucher_MapsShopFieldsCorrectly() {
        UUID shopId = UUID.randomUUID();
        Shop shop = Shop.builder()
                .id(shopId)
                .name("Tech Official Store")
                .status(ShopStatus.ACTIVE)
                .build();

        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("SHOP20K")
                .name("Giảm 20K đơn 150K")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000))
                .minOrderAmount(BigDecimal.valueOf(150000))
                .maxDiscountAmount(null)
                .usageLimit(200)
                .usageLimitPerUser(2)
                .reservedCount(10)
                .usedCount(50)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(3))
                .isActive(true)
                .isDeleted(false)
                .createdAt(now.minusDays(1))
                .build();

        Page<Voucher> mockPage = new PageImpl<>(List.of(shopVoucher));
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminVoucherResponse> response = adminVoucherService.getVouchers(
                VoucherType.SHOP, shopId, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        AdminVoucherResponse item = response.content().get(0);
        assertEquals("SHOP20K", item.code());
        assertEquals(VoucherType.SHOP, item.type());
        assertEquals(shopId, item.shopId());
        assertEquals("Tech Official Store", item.shopName());
        assertEquals(ShopStatus.ACTIVE, item.shopStatus());
    }

    @Test
    @DisplayName("TC18 (GET): Nhận diện shop vi phạm (ShopStatus = SUSPENDED) trong danh sách voucher")
    void testGetVouchers_SuspendedShopVoucher_MapsShopStatusCorrectly() {
        UUID shopId = UUID.randomUUID();
        Shop suspendedShop = Shop.builder()
                .id(shopId)
                .name("Shop Vi Phạm Gian Lận")
                .status(ShopStatus.SUSPENDED)
                .build();

        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(UUID.randomUUID())
                .shop(suspendedShop)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("SCAMVOUCHER")
                .name("Voucher giảm ảo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(99))
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(3))
                .isActive(true)
                .isDeleted(false)
                .build();

        Page<Voucher> mockPage = new PageImpl<>(List.of(shopVoucher));
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminVoucherResponse> response = adminVoucherService.getVouchers(
                null, null, "Gian Lận", null, null, null, 0, 10, null, null
        );

        assertEquals(1, response.content().size());
        AdminVoucherResponse item = response.content().get(0);
        assertEquals("Shop Vi Phạm Gian Lận", item.shopName());
        assertEquals(ShopStatus.SUSPENDED, item.shopStatus());
    }

    @Test
    @DisplayName("TC19 (GET): Whitelist sorting an toàn - fallback về createdAt desc khi client truyền field không hợp lệ")
    void testGetVouchers_InvalidSortField_FallsBackToCreatedAtDesc() {
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList());
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        adminVoucherService.getVouchers(
                null, null, null, null, null, null, 0, 10, "sql_injection_column", "invalid_dir"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        Sort.Order firstOrder = pageable.getSort().getOrderFor("createdAt");
        assertNotNull(firstOrder);
        assertEquals(Sort.Direction.DESC, firstOrder.getDirection());
    }

    @Test
    @DisplayName("TC20 (GET): Sắp xếp hợp lệ theo discountValue asc kèm tie-breaker id asc")
    void testGetVouchers_ValidCustomSortAsc() {
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList());
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        adminVoucherService.getVouchers(
                null, null, null, null, null, null, 0, 10, "discountValue", "asc"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        Sort.Order sortOrder = pageable.getSort().getOrderFor("discountValue");
        assertNotNull(sortOrder);
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());
        Sort.Order tieBreaker = pageable.getSort().getOrderFor("id");
        assertNotNull(tieBreaker);
        assertEquals(Sort.Direction.ASC, tieBreaker.getDirection());
    }

    @Test
    @DisplayName("TC21 (GET): Sanitize page âm về 0, size âm về 1, size vượt ngưỡng 100 về 100")
    void testGetVouchers_SanitizesPageAndSize() {
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList());
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Negative page & negative size
        adminVoucherService.getVouchers(
                null, null, null, null, null, null, -10, -5, null, null
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voucherRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable1 = pageableCaptor.getValue();
        assertEquals(0, pageable1.getPageNumber());
        assertEquals(1, pageable1.getPageSize());

        // Oversized page size > 100
        adminVoucherService.getVouchers(
                null, null, null, null, null, null, 2, 500, null, null
        );

        verify(voucherRepository, times(2)).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable2 = pageableCaptor.getValue();
        assertEquals(2, pageable2.getPageNumber());
        assertEquals(100, pageable2.getPageSize());
    }

    @Test
    @DisplayName("TC22 (GET): Trả về danh sách rỗng khi không có voucher nào thỏa mãn điều kiện")
    void testGetVouchers_EmptyResult_ReturnsEmptyPageResponse() {
        Page<Voucher> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(voucherRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminVoucherResponse> response = adminVoucherService.getVouchers(
                VoucherType.SHOP, UUID.randomUUID(), "NONEXISTENT", VoucherAppliesTo.SHIPPING_FEE,
                DiscountType.FIXED_AMOUNT, VoucherStatus.EXPIRED, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }

    // ==========================================
    // Tests for GET /api/admin/vouchers/{id} (getVoucherDetail)
    // ==========================================

    @Test
    @DisplayName("TC23 (GET {id}): Lấy chi tiết Platform Voucher thành công (shop=null, tính đúng tỷ lệ giải ngân và ngân sách)")
    void testGetVoucherDetail_PlatformVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher platformVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("TECHHUB10")
                .name("Giảm 10% toàn sàn đơn từ 200k")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .usageLimit(500)
                .usageLimitPerUser(1)
                .reservedCount(5)
                .usedCount(42)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(7))
                .isActive(true)
                .isDeleted(false)
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(2))
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(platformVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertEquals(voucherId, detail.id());
        assertEquals("TECHHUB10", detail.code());
        assertEquals(VoucherType.PLATFORM, detail.type());
        assertNull(detail.shopId());
        assertNull(detail.shopName());
        assertNull(detail.shopStatus());
        assertNull(detail.shopAvatarUrl());
        assertEquals("TECHHUB_PLATFORM", detail.sponsor());
        assertTrue(detail.subsidyNote().contains("BR-16D"));
        assertEquals(500, detail.usageLimit());
        assertEquals(42, detail.usedCount());
        assertEquals(5, detail.reservedCount());
        assertEquals(458, detail.remainingCount());
        assertEquals(8.40, detail.redemptionRate());
        assertEquals(VoucherStatus.ONGOING, detail.status());
    }

    @Test
    @DisplayName("TC24 (GET {id}): Lấy chi tiết Shop Voucher thành công (shop có đầy đủ thông tin, trạng thái ACTIVE)")
    void testGetVoucherDetail_ShopVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Shop shop = Shop.builder()
                .id(shopId)
                .name("Apple Flagship Store VN")
                .status(ShopStatus.ACTIVE)
                .avatarUrl("https://cdn.techhub.vn/apple.png")
                .build();

        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(voucherId)
                .shop(shop)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("APPLE50K")
                .name("Giảm 50k phụ kiện chính hãng")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(300000))
                .maxDiscountAmount(null)
                .usageLimit(100)
                .usageLimitPerUser(1)
                .reservedCount(3)
                .usedCount(85)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .createdAt(now.minusDays(3))
                .updatedAt(now.minusDays(1))
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(shopVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertEquals(voucherId, detail.id());
        assertEquals("APPLE50K", detail.code());
        assertEquals(VoucherType.SHOP, detail.type());
        assertEquals(shopId, detail.shopId());
        assertEquals("Apple Flagship Store VN", detail.shopName());
        assertEquals(ShopStatus.ACTIVE, detail.shopStatus());
        assertEquals("https://cdn.techhub.vn/apple.png", detail.shopAvatarUrl());
        assertEquals("SHOP_SELLER", detail.sponsor());
        assertTrue(detail.subsidyNote().contains("Gian hàng"));
        assertEquals(100, detail.usageLimit());
        assertEquals(85, detail.usedCount());
        assertEquals(3, detail.reservedCount());
        assertEquals(15, detail.remainingCount());
        assertEquals(85.00, detail.redemptionRate());
    }

    @Test
    @DisplayName("TC25 (GET {id}): Nhận diện Shop bị đình chỉ (ShopStatus = SUSPENDED) trong chi tiết voucher")
    void testGetVoucherDetail_SuspendedShop_Success() {
        UUID voucherId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        Shop suspendedShop = Shop.builder()
                .id(shopId)
                .name("Gian Hàng Vi Phạm")
                .status(ShopStatus.SUSPENDED)
                .build();

        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(voucherId)
                .shop(suspendedShop)
                .type(VoucherType.SHOP)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("VIOLATION10")
                .name("Voucher lừa đảo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(50))
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(shopVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertEquals(shopId, detail.shopId());
        assertEquals("Gian Hàng Vi Phạm", detail.shopName());
        assertEquals(ShopStatus.SUSPENDED, detail.shopStatus());
    }

    @Test
    @DisplayName("TC26 (GET {id}): Voucher không giới hạn lượt dùng (usageLimit = null) -> remainingCount = null, redemptionRate = null")
    void testGetVoucherDetail_UnlimitedUsageLimit_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher unlimitedVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.SHIPPING_FEE)
                .code("FREESHIP")
                .name("Freeship vô tận")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(15000))
                .usageLimit(null)
                .usageLimitPerUser(2)
                .reservedCount(0)
                .usedCount(120)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(unlimitedVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertNull(detail.usageLimit());
        assertNull(detail.remainingCount());
        assertNull(detail.redemptionRate());
        assertEquals(120, detail.usedCount());
    }

    @Test
    @DisplayName("TC27 (GET {id}): Voucher có usageLimit = 0 (phòng chống lỗi chia cho 0) -> redemptionRate = null")
    void testGetVoucherDetail_ZeroUsageLimit_DoesNotDivideByZero() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher zeroLimitVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("ZEROLIMIT")
                .name("Chưa mở phát hành")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000))
                .usageLimit(0)
                .reservedCount(0)
                .usedCount(0)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(zeroLimitVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertEquals(0, detail.usageLimit());
        assertEquals(0, detail.remainingCount());
        assertNull(detail.redemptionRate());
    }

    @Test
    @DisplayName("TC28 (GET {id}): Voucher đã giải ngân 100% (usedCount >= usageLimit)")
    void testGetVoucherDetail_ExhaustedUsageLimit() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher exhaustedVoucher = Voucher.builder()
                .id(voucherId)
                .shop(null)
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .code("SOLDOUT")
                .name("Voucher đã cháy hàng")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .usageLimit(100)
                .usedCount(100)
                .reservedCount(0)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(exhaustedVoucher));

        AdminVoucherDetailResponse detail = adminVoucherService.getVoucherDetail(voucherId);

        assertNotNull(detail);
        assertEquals(0, detail.remainingCount());
        assertEquals(100.00, detail.redemptionRate());
    }

    @Test
    @DisplayName("TC29 (GET {id}): Không tìm thấy voucher trong DB -> Ném VOUCHER_NOT_FOUND (404)")
    void testGetVoucherDetail_NotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(voucherRepository.findWithShopById(randomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.getVoucherDetail(randomId)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC30 (GET {id}): Voucher đã bị xóa mềm (isDeleted = true) -> Ném VOUCHER_NOT_FOUND (404)")
    void testGetVoucherDetail_SoftDeleted_ThrowsException() {
        UUID voucherId = UUID.randomUUID();
        Voucher deletedVoucher = Voucher.builder()
                .id(voucherId)
                .code("DELETED_CODE")
                .name("Voucher đã xóa")
                .isDeleted(true)
                .deletedAt(LocalDateTime.now())
                .build();

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(deletedVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.getVoucherDetail(voucherId)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC31 (GET {id}): id bị null -> Fail-fast ném VOUCHER_NOT_FOUND (404) không gọi DB")
    void testGetVoucherDetail_NullId_ThrowsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.getVoucherDetail(null)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC32 (PUT {id}): Cập nhật voucher UPCOMING thành công (sửa toàn bộ kể cả discount và dates)")
    void testUpdatePlatformVoucher_Upcoming_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHUPCOMING")
                .name("Tên cũ")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000))
                .minOrderAmount(BigDecimal.valueOf(100000))
                .maxDiscountAmount(null)
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .reservedCount(0)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Tên mới siêu sale",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(150000),
                BigDecimal.valueOf(50000),
                200,
                2,
                now.plusDays(3),
                now.plusDays(15)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals("Tên mới siêu sale", response.name());
        assertEquals(DiscountType.PERCENTAGE, response.discountType());
        assertEquals(BigDecimal.valueOf(25), response.discountValue());
        assertEquals(BigDecimal.valueOf(150000), response.minOrderAmount());
        assertEquals(BigDecimal.valueOf(50000), response.maxDiscountAmount());
        assertEquals(200, response.usageLimit());
        assertEquals(2, response.usageLimitPerUser());
        assertEquals(request.startDate(), response.startDate());
        assertEquals(request.endDate(), response.endDate());
        verify(voucherRepository).saveAndFlush(existingVoucher);
    }

    @Test
    @DisplayName("TC33 (PUT {id}): Mở rộng ngân sách voucher ONGOING thành công (giữ nguyên startDate)")
    void testUpdatePlatformVoucher_Ongoing_ExpandUsageLimit_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime originalStart = now.minusDays(2);
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHONGOING")
                .name("Chương trình đang chạy")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(300000))
                .maxDiscountAmount(null)
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(50)
                .reservedCount(10)
                .startDate(originalStart)
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Chương trình đang chạy - Mở rộng ngân sách",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(300000),
                null,
                500,
                1,
                now.plusDays(1), // startDate bị bỏ qua vì ONGOING
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals(500, response.usageLimit());
        assertEquals(450, response.remainingCount());
        assertEquals(10.0, response.redemptionRate());
        assertEquals(originalStart, response.startDate()); // Bất biến
        assertEquals(request.endDate(), response.endDate());
    }

    @Test
    @DisplayName("TC34 (PUT {id}): Rút ngắn ngày kết thúc voucher ONGOING để cắt lỗ sớm")
    void testUpdatePlatformVoucher_Ongoing_ShortenEndDate_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHSHORTEN")
                .name("Cắt lỗ")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(10)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        LocalDateTime shortenedEnd = now.plusHours(2);
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Cắt lỗ sớm",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(200000),
                null,
                100,
                1,
                null,
                shortenedEnd
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals(shortenedEnd, response.endDate());
    }

    @Test
    @DisplayName("TC35 (PUT {id}): Từ chối sửa giá trị chiết khấu khi voucher ONGOING đã có lượt dùng (usedCount > 0)")
    void testUpdatePlatformVoucher_Ongoing_UsedCountGreaterThanZero_ModifyingDiscountValue_ThrowsCoreDiscountError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHTAMPER")
                .name("Đang chạy")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(20)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Cố tình sửa giảm giá",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(30000), // Sửa từ 50k xuống 30k
                BigDecimal.valueOf(200000),
                null,
                100,
                1,
                null,
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.CANNOT_MODIFY_CORE_DISCOUNT, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC36 (PUT {id}): Form submit lại cùng giá trị chiết khấu cũ khi usedCount > 0 -> Thành công")
    void testUpdatePlatformVoucher_Ongoing_UsedCountGreaterThanZero_KeepingSameDiscount_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHSAME")
                .name("Tên cũ")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.valueOf(200000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(20)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Tên mới cập nhật",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000), // Giữ nguyên
                BigDecimal.valueOf(200000), // Giữ nguyên
                null,
                150, // Nâng lượt dùng
                1,
                null,
                now.plusDays(7)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals("Tên mới cập nhật", response.name());
        assertEquals(150, response.usageLimit());
    }

    @Test
    @DisplayName("TC37 (PUT {id}): Từ chối hạ usageLimit thấp hơn số lượt đã dùng (usageLimit < usedCount)")
    void testUpdatePlatformVoucher_UsageLimitLessThanUsedCount_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHLIMIT")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(80)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                50, // Nhỏ hơn usedCount 80
                1,
                null,
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.USAGE_LIMIT_LESS_THAN_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC38 (PUT {id}): Từ chối cập nhật voucher đã hết hạn (EXPIRED)")
    void testUpdatePlatformVoucher_Expired_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher expiredVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHEXPIRED")
                .name("Đã hết hạn")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(1))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Mở lại",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(expiredVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_ALREADY_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC39 (PUT {id}): Từ chối ngày kết thúc mới ở quá khứ (endDate < now)")
    void testUpdatePlatformVoucher_EndDateInPast_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHENDPAST")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                now.minusHours(1) // endDate ở quá khứ
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC40 (PUT {id}): Từ chối ngày kết thúc trước ngày bắt đầu (endDate <= startDate)")
    void testUpdatePlatformVoucher_EndDateBeforeStartDate_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher upcomingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHDATEORDER")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(5))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                now.plusDays(8), // startDate mới
                now.plusDays(6)  // endDate trước startDate
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(upcomingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_DATES, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC41 (PUT {id}): Từ chối startDate mới ở quá khứ (> 5 phút trước) đối với voucher UPCOMING")
    void testUpdatePlatformVoucher_Upcoming_StartDateTooFarInPast_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher upcomingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHSTARTPAST")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                now.minusMinutes(15), // Quá khứ 15 phút (> 5m threshold)
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(upcomingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_START_DATE_IN_PAST, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC42 (PUT {id}): Chấp nhận startDate mới trong vùng đệm 5 phút (Grace Period)")
    void testUpdatePlatformVoucher_Upcoming_StartDateWithinGracePeriod_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher upcomingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHGRACE")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        LocalDateTime graceStart = now.minusMinutes(2); // Vùng đệm 2 phút
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher kích hoạt ngay",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                graceStart,
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(upcomingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals(graceStart, response.startDate());
    }

    @Test
    @DisplayName("TC43 (PUT {id}): Từ chối tỷ lệ chiết khấu phần trăm vượt quá 100%")
    void testUpdatePlatformVoucher_PercentageOver100_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHOVER100")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(101), // Vượt quá 100%
                null,
                null,
                100,
                1,
                null,
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_DISCOUNT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC44 (PUT {id}): Tự động đưa maxDiscountAmount về null khi chọn FIXED_AMOUNT")
    void testUpdatePlatformVoucher_FixedAmount_ResetsMaxDiscountToNull() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHFIXED")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Đổi sang tiền cố định",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                BigDecimal.valueOf(100000), // Gửi maxDiscountAmount nhưng FIXED_AMOUNT phải reset về null
                100,
                1,
                null,
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertNull(response.maxDiscountAmount());
    }

    @Test
    @DisplayName("TC45 (PUT {id}): Từ chối usageLimit < usageLimitPerUser")
    void testUpdatePlatformVoucher_UsageLimitLessThanUsageLimitPerUser_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHHIERARCHY")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                2, // usageLimit
                5, // usageLimitPerUser > usageLimit
                null,
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_USAGE_LIMIT, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC46 (PUT {id}): Cập nhật voucher không giới hạn lượt dùng (usageLimit = null)")
    void testUpdatePlatformVoucher_UnlimitedUsageLimit_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHUNLIMITED")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher vô tận",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                null, // Không giới hạn
                2,
                null,
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertNull(response.usageLimit());
        assertNull(response.remainingCount());
    }

    @Test
    @DisplayName("TC47 (PUT {id}): Từ chối cập nhật khi truyền ID của Shop Voucher")
    void testUpdatePlatformVoucher_ShopVoucherType_ThrowsInvalidVoucherType() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(voucherId)
                .code("SHOPVOUCHER")
                .name("Voucher của Shop")
                .type(VoucherType.SHOP) // Loại SHOP
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Thử sửa voucher shop",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                now.plusDays(10)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(shopVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC48 (PUT {id}): Từ chối cập nhật voucher không tồn tại (UUID ngẫu nhiên)")
    void testUpdatePlatformVoucher_NotFound_ThrowsError() {
        UUID randomId = UUID.randomUUID();
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );

        when(voucherRepository.findWithShopById(randomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(randomId, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC49 (PUT {id}): Từ chối cập nhật voucher đã bị xóa mềm (isDeleted = true)")
    void testUpdatePlatformVoucher_SoftDeleted_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        Voucher deletedVoucher = Voucher.builder()
                .id(voucherId)
                .code("DELETED")
                .name("Đã xóa")
                .isDeleted(true)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(deletedVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC50 (PUT {id}): Fail-fast khi tham số ID bị null")
    void testUpdatePlatformVoucher_NullId_ThrowsError() {
        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                100,
                1,
                null,
                LocalDateTime.now().plusDays(5)
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(null, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC51 (PUT {id}): Fail-fast khi tham số request bị null")
    void testUpdatePlatformVoucher_NullRequest_ThrowsError() {
        UUID voucherId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, null)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC52 (PUT {id}): Từ chối usageLimit nhỏ hơn tổng (usedCount + reservedCount)")
    void testUpdatePlatformVoucher_UsageLimitLessThanUsedPlusReserved_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHRESERVE")
                .name("Voucher")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(30)
                .reservedCount(25) // Tổng đã cam kết = 55
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Voucher",
                DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                null,
                40, // 40 > usedCount(30) nhưng < committed(55)
                1,
                null,
                now.plusDays(5)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucher(voucherId, request)
        );

        assertEquals(ErrorCode.USAGE_LIMIT_LESS_THAN_USED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC53 (PUT {id}): Cho phép cập nhật khi maxDiscountAmount cùng giá trị toán học nhưng khác scale BigDecimal")
    void testUpdatePlatformVoucher_Percentage_SameMaxDiscountDifferentScale_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher existingVoucher = Voucher.builder()
                .id(voucherId)
                .code("TECHSCALE")
                .name("Tên cũ")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(100000))
                .maxDiscountAmount(new BigDecimal("50000.00")) // Scale 2 từ DB
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(15) // usedCount > 0
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherRequest request = new UpdatePlatformVoucherRequest(
                "Tên mới cập nhật",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100000),
                new BigDecimal("50000"), // Scale 0 từ JSON
                150,
                1,
                null,
                now.plusDays(7)
        );

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(existingVoucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucher(voucherId, request);

        assertNotNull(response);
        assertEquals("Tên mới cập nhật", response.name());
        assertEquals(150, response.usageLimit());
    }

    // ==========================================
    // Tests for PATCH /api/admin/vouchers/{id}/status (updatePlatformVoucherStatus)
    // ==========================================

    @Test
    @DisplayName("TC54 (PATCH status): Ngắt kích hoạt voucher toàn sàn đang chạy thành công (Circuit Breaker ONGOING -> DISABLED)")
    void testToggleStatus_Deactivate_PlatformVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("SUMMER50K")
                .name("Siêu Sale Hè 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(1000)
                .usageLimitPerUser(2)
                .usedCount(142)
                .reservedCount(15)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(VoucherStatus.DISABLED, response.status());
        assertEquals(142, response.usedCount());
        assertEquals(15, response.reservedCount());
        verify(voucherRepository, times(1)).saveAndFlush(voucher);
    }

    @Test
    @DisplayName("TC55 (PATCH status): Tái kích hoạt voucher toàn sàn đang tạm dừng thành công (DISABLED -> ONGOING)")
    void testToggleStatus_Activate_PlatformVoucher_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("SUMMER50K")
                .name("Siêu Sale Hè 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(1000)
                .usageLimitPerUser(2)
                .usedCount(142)
                .reservedCount(15)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(10))
                .isActive(false) // Đang tắt
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(true);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.ONGOING, response.status());
        verify(voucherRepository, times(1)).saveAndFlush(voucher);
    }

    @Test
    @DisplayName("TC56 (PATCH status): Tạm dừng voucher UPCOMING thành công (UPCOMING -> DISABLED)")
    void testToggleStatus_Upcoming_Deactivate_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("AUTUMN2026")
                .name("Mùa Thu 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(30000))
                .usageLimit(500)
                .usageLimitPerUser(1)
                .usedCount(0)
                .reservedCount(0)
                .startDate(now.plusDays(10)) // UPCOMING
                .endDate(now.plusDays(20))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(VoucherStatus.DISABLED, response.status());
    }

    @Test
    @DisplayName("TC57 (PATCH status): Mở lại voucher UPCOMING thành công (DISABLED -> UPCOMING)")
    void testToggleStatus_Upcoming_Reactivate_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("AUTUMN2026")
                .name("Mùa Thu 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(30000))
                .usageLimit(500)
                .usageLimitPerUser(1)
                .usedCount(0)
                .reservedCount(0)
                .startDate(now.plusDays(10)) // UPCOMING
                .endDate(now.plusDays(20))
                .isActive(false) // Đang tắt
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(true);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertTrue(response.isActive());
        assertEquals(VoucherStatus.UPCOMING, response.status());
    }

    @Test
    @DisplayName("TC58 (PATCH status): Từ chối cập nhật khi truyền ID của Shop Voucher (INVALID_VOUCHER_TYPE)")
    void testToggleStatus_ShopVoucher_ThrowsInvalidVoucherType() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher shopVoucher = Voucher.builder()
                .id(voucherId)
                .code("SHOPVOUCHER")
                .name("Voucher của Shop")
                .type(VoucherType.SHOP) // Loại SHOP
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(10))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(shopVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_TYPE, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC59 (PATCH status): Thao tác Idempotent khi trạng thái không đổi (không gọi saveAndFlush)")
    void testToggleStatus_Idempotent_SameState_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("IDEMPOTENT")
                .name("Voucher Idempotent")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(10)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(false) // Đang tắt
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false); // Gửi tắt tiếp

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertFalse(response.isActive());
        verify(voucherRepository, never()).saveAndFlush(any()); // Không ghi DB thừa
    }

    @Test
    @DisplayName("TC60 (PATCH status): Từ chối kích hoạt lại voucher đã hết hạn (VOUCHER_ALREADY_EXPIRED)")
    void testToggleStatus_ExpiredVoucher_ThrowsVoucherAlreadyExpired() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher expiredVoucher = Voucher.builder()
                .id(voucherId)
                .code("EXPIRED")
                .name("Đã hết hạn")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(5)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(1)) // Hết hạn hôm qua
                .isActive(false)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(true);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(expiredVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_ALREADY_EXPIRED, ex.getErrorCode());
        verify(voucherRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC61 (PATCH status): Từ chối ngắt kích hoạt voucher đã hết hạn (VOUCHER_ALREADY_EXPIRED)")
    void testToggleStatus_ExpiredVoucher_Deactivate_ThrowsVoucherAlreadyExpired() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher expiredVoucher = Voucher.builder()
                .id(voucherId)
                .code("EXPIRED")
                .name("Đã hết hạn")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(5)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(1))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(expiredVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_ALREADY_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC62 (PATCH status): Từ chối cập nhật voucher đã bị xóa mềm (VOUCHER_NOT_FOUND)")
    void testToggleStatus_SoftDeletedVoucher_ThrowsVoucherNotFound() {
        UUID voucherId = UUID.randomUUID();
        Voucher deletedVoucher = Voucher.builder()
                .id(voucherId)
                .code("DELETED")
                .name("Đã xóa")
                .type(VoucherType.PLATFORM)
                .isDeleted(true)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(deletedVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC63 (PATCH status): Từ chối cập nhật voucher không tồn tại (VOUCHER_NOT_FOUND)")
    void testToggleStatus_VoucherNotFound_ThrowsVoucherNotFound() {
        UUID randomId = UUID.randomUUID();
        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(randomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(randomId, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC64 (PATCH status): Fail-fast khi tham số id bị null (VOUCHER_NOT_FOUND)")
    void testToggleStatus_NullId_ThrowsVoucherNotFound() {
        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(null, request)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC65 (PATCH status): Fail-fast khi tham số request bị null (INVALID_INPUT_DATA)")
    void testToggleStatus_NullRequest_ThrowsInvalidInputData() {
        UUID voucherId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, null)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC66 (PATCH status): Fail-fast khi trường isActive trong request bị null (INVALID_INPUT_DATA)")
    void testToggleStatus_NullIsActive_ThrowsInvalidInputData() {
        UUID voucherId = UUID.randomUUID();
        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.updatePlatformVoucherStatus(voucherId, request)
        );

        assertEquals(ErrorCode.INVALID_INPUT_DATA, ex.getErrorCode());
        verify(voucherRepository, never()).findWithShopById(any());
    }

    @Test
    @DisplayName("TC67 (PATCH status): Bảo toàn nguyên vẹn số lượng usedCount và reservedCount khi ngắt kích hoạt")
    void testToggleStatus_PreservesReservedAndUsedCounts() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("TECHPROTECT")
                .name("Bảo Vệ Đơn Hàng")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(50)
                .reservedCount(10)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminVoucherDetailResponse response = adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        assertNotNull(response);
        assertFalse(response.isActive());
        assertEquals(50, response.usedCount());
        assertEquals(10, response.reservedCount());
        assertEquals(50, response.remainingCount());
    }

    @Test
    @DisplayName("TC68 (PATCH status): Truy vấn tối ưu Zero N+1 query với findWithShopById")
    void testToggleStatus_ZeroNPlusOneQuery_WithEntityGraph() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("TECHGRAPH")
                .name("Entity Graph Test")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .isActive(true)
                .isDeleted(false)
                .build();

        UpdatePlatformVoucherStatusRequest request = new UpdatePlatformVoucherStatusRequest(false);

        when(voucherRepository.findWithShopById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.saveAndFlush(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminVoucherService.updatePlatformVoucherStatus(voucherId, request);

        verify(voucherRepository, times(1)).findWithShopById(voucherId);
    }

    // ==========================================
    // Tests for DELETE /api/admin/vouchers/{id} (deletePlatformVoucher)
    // ==========================================

    @Test
    @DisplayName("TC69 (DELETE): Xóa cứng voucher toàn sàn chưa dùng (usedCount=0, reservedCount=0) thành công")
    void testDeletePlatformVoucher_Unused_HardDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("NEWYEAR2026")
                .name("Năm Mới 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .usedCount(0)
                .reservedCount(0)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, times(1)).delete(voucher);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC70 (DELETE): Xóa mềm voucher toàn sàn đã dùng (usedCount>0, reservedCount=0) thành công")
    void testDeletePlatformVoucher_Used_SoftDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("SUMMER50K")
                .name("Siêu Sale Hè 2026")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .usedCount(142)
                .reservedCount(0)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, never()).delete(any(Voucher.class));
        assertTrue(voucher.isDeleted());
        assertFalse(voucher.isActive());
        assertNotNull(voucher.getDeletedAt());
        verify(voucherRepository, times(1)).save(voucher);
    }

    @Test
    @DisplayName("TC71 (DELETE): Từ chối xóa voucher đang có giữ chỗ checkout (reservedCount>0) - VOUCHER_HAS_ACTIVE_RESERVATIONS")
    void testDeletePlatformVoucher_ActiveReservations_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("FLASH100K")
                .name("Flash Sale")
                .type(VoucherType.PLATFORM)
                .appliesTo(VoucherAppliesTo.PRODUCT)
                .usedCount(5)
                .reservedCount(2)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(voucherId)
        );

        assertEquals(ErrorCode.VOUCHER_HAS_ACTIVE_RESERVATIONS, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC72 (DELETE): Từ chối xóa voucher chưa dùng nhưng có giữ chỗ (usedCount=0, reservedCount=1) - BR-16B")
    void testDeletePlatformVoucher_ActiveReservationsZeroUsed_ThrowsError() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("ZERO_USED_ONE_RESERVED")
                .name("Voucher giữ chỗ")
                .type(VoucherType.PLATFORM)
                .usedCount(0)
                .reservedCount(1)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(voucherId)
        );

        assertEquals(ErrorCode.VOUCHER_HAS_ACTIVE_RESERVATIONS, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC73 (DELETE): Từ chối xóa voucher của Shop trên endpoint Platform (INVALID_VOUCHER_TYPE)")
    void testDeletePlatformVoucher_ShopVoucherType_ThrowsInvalidVoucherType() {
        UUID voucherId = UUID.randomUUID();
        Voucher shopVoucher = Voucher.builder()
                .id(voucherId)
                .code("SHOPVOUCHER")
                .name("Voucher của Shop")
                .type(VoucherType.SHOP)
                .usedCount(0)
                .reservedCount(0)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(shopVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(voucherId)
        );

        assertEquals(ErrorCode.INVALID_VOUCHER_TYPE, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC74 (DELETE): Từ chối xóa voucher đã bị xóa mềm trước đó (VOUCHER_NOT_FOUND)")
    void testDeletePlatformVoucher_AlreadySoftDeleted_ThrowsVoucherNotFound() {
        UUID voucherId = UUID.randomUUID();
        Voucher deletedVoucher = Voucher.builder()
                .id(voucherId)
                .code("ALREADY_DELETED")
                .name("Đã xóa trước")
                .type(VoucherType.PLATFORM)
                .isDeleted(true)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(deletedVoucher));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(voucherId)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC75 (DELETE): Từ chối xóa voucher không tồn tại trong DB (VOUCHER_NOT_FOUND)")
    void testDeletePlatformVoucher_NotFound_ThrowsVoucherNotFound() {
        UUID randomId = UUID.randomUUID();
        when(voucherRepository.findById(randomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(randomId)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).delete(any(Voucher.class));
        verify(voucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC76 (DELETE): Fail-fast khi tham số id bị null (VOUCHER_NOT_FOUND)")
    void testDeletePlatformVoucher_NullId_ThrowsVoucherNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminVoucherService.deletePlatformVoucher(null)
        );

        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
        verify(voucherRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TC77 (DELETE): Xóa thành công voucher UPCOMING chưa dùng bằng Hard delete")
    void testDeletePlatformVoucher_Upcoming_Unused_HardDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("UPCOMING_UNTOUCHED")
                .name("Sắp diễn ra")
                .type(VoucherType.PLATFORM)
                .startDate(now.plusDays(5))
                .endDate(now.plusDays(10))
                .usedCount(0)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, times(1)).delete(voucher);
    }

    @Test
    @DisplayName("TC78 (DELETE): Xóa thành công voucher ONGOING đã dùng bằng Soft delete")
    void testDeletePlatformVoucher_Ongoing_Used_SoftDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("ONGOING_USED")
                .name("Đang chạy")
                .type(VoucherType.PLATFORM)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(5))
                .usedCount(50)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, never()).delete(any(Voucher.class));
        assertTrue(voucher.isDeleted());
        verify(voucherRepository, times(1)).save(voucher);
    }

    @Test
    @DisplayName("TC79 (DELETE): Xóa thành công voucher EXPIRED đã dùng bằng Soft delete (Bảo toàn BR-16D)")
    void testDeletePlatformVoucher_Expired_Used_SoftDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("EXPIRED_USED")
                .name("Đã hết hạn")
                .type(VoucherType.PLATFORM)
                .startDate(now.minusDays(15))
                .endDate(now.minusDays(1))
                .usedCount(100)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, never()).delete(any(Voucher.class));
        assertTrue(voucher.isDeleted());
        assertFalse(voucher.isActive());
        verify(voucherRepository, times(1)).save(voucher);
    }

    @Test
    @DisplayName("TC80 (DELETE): Xóa thành công voucher EXPIRED chưa dùng bằng Hard delete")
    void testDeletePlatformVoucher_Expired_Unused_HardDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("EXPIRED_UNUSED")
                .name("Hết hạn không ai dùng")
                .type(VoucherType.PLATFORM)
                .startDate(now.minusDays(15))
                .endDate(now.minusDays(1))
                .usedCount(0)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, times(1)).delete(voucher);
    }

    @Test
    @DisplayName("TC81 (DELETE): Xóa thành công voucher DISABLED đã dùng bằng Soft delete")
    void testDeletePlatformVoucher_Disabled_Used_SoftDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("DISABLED_USED")
                .name("Đang tạm tắt")
                .type(VoucherType.PLATFORM)
                .isActive(false)
                .usedCount(20)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, never()).delete(any(Voucher.class));
        assertTrue(voucher.isDeleted());
        verify(voucherRepository, times(1)).save(voucher);
    }

    @Test
    @DisplayName("TC82 (DELETE): Xóa thành công voucher DISABLED chưa dùng bằng Hard delete")
    void testDeletePlatformVoucher_Disabled_Unused_HardDelete_Success() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("DISABLED_UNUSED")
                .name("Đang tắt chưa dùng")
                .type(VoucherType.PLATFORM)
                .isActive(false)
                .usedCount(0)
                .reservedCount(0)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, times(1)).delete(voucher);
    }

    @Test
    @DisplayName("TC83 (DELETE): Xử lý an toàn khi usedCount và reservedCount bị null (fallback về 0)")
    void testDeletePlatformVoucher_NullCounters_SafeDefaults_Success() {
        UUID voucherId = UUID.randomUUID();
        Voucher voucher = Voucher.builder()
                .id(voucherId)
                .code("NULL_COUNTERS")
                .name("Null counter entity")
                .type(VoucherType.PLATFORM)
                .usedCount(null)
                .reservedCount(null)
                .isDeleted(false)
                .build();

        when(voucherRepository.findById(voucherId)).thenReturn(Optional.of(voucher));

        adminVoucherService.deletePlatformVoucher(voucherId);

        verify(voucherRepository, times(1)).delete(voucher);
    }
}
