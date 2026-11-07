package com.techhub.service.payout;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.payout.ProcessPayoutRequest;
import com.techhub.model.dto.response.payout.AdminPayoutResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PayoutProcessAction;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.SellerPayoutRequestRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.SellerWalletTransactionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.repository.UserRepository;
import com.techhub.service.payout.impl.AdminPayoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminPayoutServiceImpl (GET & PATCH /api/admin/payouts)")
class AdminPayoutServiceImplTest {

    @Mock
    private SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminPayoutServiceImpl adminPayoutService;

    private UUID sellerId;
    private Seller testSeller;
    private Shop testShop;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();

        testSeller = Seller.builder()
                .id(sellerId)
                .sellerName("TechHub Official Store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("NGUYEN VAN A")
                .balance(new BigDecimal("10000000.00"))
                .build();

        testShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(testSeller)
                .name("TechHub Store")
                .status(ShopStatus.ACTIVE)
                .build();

        testAdminUser = new User();
        testAdminUser.setId(UUID.randomUUID());
        testAdminUser.setFirstName("Admin");
        testAdminUser.setLastName("Super");
        testAdminUser.setEmail("admin@techhub.vn");
        testAdminUser.setStatus(UserStatus.ACTIVE);
    }

    private SellerPayoutRequest createPayoutRequest(UUID id, PayoutStatus status, BigDecimal amount, User processedBy) {
        return SellerPayoutRequest.builder()
                .id(id != null ? id : UUID.randomUUID())
                .seller(testSeller)
                .amount(amount)
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("NGUYEN VAN A")
                .status(status)
                .processedBy(processedBy)
                .processedAt(processedBy != null ? LocalDateTime.now() : null)
                .rejectionReason(status == PayoutStatus.REJECTED ? "Suspicious activity detected" : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-ADM-PAY-01: Phân trang mặc định - page=0, size=20, createdAt DESC (Happy Path)")
    void getPayoutRequests_Success_DefaultPagination() {
        List<SellerPayoutRequest> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add(createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null));
        }
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(list, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 35);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(20, response.content().size());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());
        assertEquals(35, response.totalElements());
        assertEquals(2, response.totalPages());
        assertFalse(response.last());
        assertEquals("TechHub Store", response.content().get(0).shopName());
        assertEquals("1234567890", response.content().get(0).bankAccountNumber()); // Admin sees full account number
    }

    @Test
    @DisplayName("TC-ADM-PAY-02: Phân trang trang kế tiếp - page=1, size=20")
    void getPayoutRequests_Success_NextPage() {
        List<SellerPayoutRequest> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            list.add(createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null));
        }
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(list, PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 35);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 1, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(15, response.content().size());
        assertEquals(1, response.pageNumber());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("TC-ADM-PAY-03: Lọc theo trạng thái PENDING")
    void getPayoutRequests_Success_FilterPending() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("2000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                PayoutStatus.PENDING, null, null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(PayoutStatus.PENDING, response.content().get(0).status());
        assertNull(response.content().get(0).processedById());
        assertNull(response.content().get(0).processedByName());
    }

    @Test
    @DisplayName("TC-ADM-PAY-04: Lọc theo trạng thái COMPLETED - Có thông tin người duyệt")
    void getPayoutRequests_Success_FilterCompleted() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.COMPLETED, new BigDecimal("5000000.00"), testAdminUser);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                PayoutStatus.COMPLETED, null, null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(PayoutStatus.COMPLETED, response.content().get(0).status());
        assertEquals(testAdminUser.getId(), response.content().get(0).processedById());
        assertEquals("Super Admin", response.content().get(0).processedByName());
        assertNotNull(response.content().get(0).processedAt());
    }

    @Test
    @DisplayName("TC-ADM-PAY-05: Lọc theo trạng thái REJECTED - Có lý do từ chối")
    void getPayoutRequests_Success_FilterRejected() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.REJECTED, new BigDecimal("3000000.00"), testAdminUser);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                PayoutStatus.REJECTED, null, null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(PayoutStatus.REJECTED, response.content().get(0).status());
        assertEquals("Suspicious activity detected", response.content().get(0).rejectionReason());
    }

    @Test
    @DisplayName("TC-ADM-PAY-06: Tìm kiếm theo từ khóa sellerName")
    void getPayoutRequests_Success_KeywordSellerName() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, "TechHub", null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("TechHub Official Store", response.content().get(0).sellerName());
    }

    @Test
    @DisplayName("TC-ADM-PAY-07: Tìm kiếm theo từ khóa bankAccountNumber")
    void getPayoutRequests_Success_KeywordBankAccountNumber() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, "1234567890", null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("1234567890", response.content().get(0).bankAccountNumber());
    }

    @Test
    @DisplayName("TC-ADM-PAY-08: Tìm kiếm theo từ khóa bankAccountHolder")
    void getPayoutRequests_Success_KeywordBankAccountHolder() {
        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, "NGUYEN VAN A", null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("NGUYEN VAN A", response.content().get(0).bankAccountHolder());
    }

    @Test
    @DisplayName("TC-ADM-PAY-09: Lọc theo khoảng ngày (startDate và endDate)")
    void getPayoutRequests_Success_FilterDateRange() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 4);

        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, startDate, endDate, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-ADM-PAY-10: Lọc chỉ có startDate")
    void getPayoutRequests_Success_FilterOnlyStartDate() {
        LocalDate startDate = LocalDate.of(2026, 11, 2);

        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, startDate, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-ADM-PAY-11: Lọc chỉ có endDate")
    void getPayoutRequests_Success_FilterOnlyEndDate() {
        LocalDate endDate = LocalDate.of(2026, 11, 3);

        SellerPayoutRequest payout = createPayoutRequest(UUID.randomUUID(), PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(List.of(payout), PageRequest.of(0, 20), 1);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(shopRepository.findBySellerIdIn(anySet())).thenReturn(List.of(testShop));

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, endDate, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-ADM-PAY-12: Sắp xếp theo amount ASC")
    void getPayoutRequests_Success_SortAmountAsc() {
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 0, 20, "amount", "ASC"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerPayoutRequestRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals("amount", pageableCaptor.getValue().getSort().getOrderFor("amount").getProperty());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("amount").getDirection());
    }

    @Test
    @DisplayName("TC-ADM-PAY-13: Sắp xếp theo amount DESC")
    void getPayoutRequests_Success_SortAmountDesc() {
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 0, 20, "amount", "DESC"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerPayoutRequestRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals("amount", pageableCaptor.getValue().getSort().getOrderFor("amount").getProperty());
        assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("amount").getDirection());
    }

    @Test
    @DisplayName("TC-ADM-PAY-14: Sắp xếp theo status ASC")
    void getPayoutRequests_Success_SortStatusAsc() {
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 0, 20, "status", "ASC"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerPayoutRequestRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals("status", pageableCaptor.getValue().getSort().getOrderFor("status").getProperty());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("status").getDirection());
    }

    @Test
    @DisplayName("TC-ADM-PAY-15: Fallback an toàn khi truyền sortBy không hợp lệ")
    void getPayoutRequests_Success_SortFallback() {
        Page<SellerPayoutRequest> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, null, null, null, 0, 20, "maliciousColumn", "DESC"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerPayoutRequestRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals("createdAt", pageableCaptor.getValue().getSort().getOrderFor("createdAt").getProperty());
    }

    @Test
    @DisplayName("TC-ADM-PAY-16: Kết quả rỗng khi không có bản ghi nào khớp tiêu chí")
    void getPayoutRequests_Success_EmptyResult() {
        Page<SellerPayoutRequest> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerPayoutRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<AdminPayoutResponse> response = adminPayoutService.getPayoutRequests(
                null, "NonExistentStore", null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("TC-ADM-PAY-17: Thất bại - Khoảng ngày không hợp lệ (startDate > endDate)")
    void getPayoutRequests_ThrowsException_InvalidDateRange() {
        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.getPayoutRequests(null, null, startDate, endDate, 0, 20, "createdAt", "DESC"));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // =========================================================================
    // UNIT TESTS CHO PROCESS PAYOUT REQUEST (PATCH /api/admin/payouts/{id}/process)
    // =========================================================================

    @Test
    @DisplayName("TC-ADM-PROC-01: Phê duyệt rút tiền thành công (Approve Happy Path)")
    void processPayoutRequest_Success_Approve() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdminUser));
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, null);

        AdminPayoutResponse response = adminPayoutService.processPayoutRequest(payoutId, adminId, request);

        assertNotNull(response);
        assertEquals(payoutId, response.id());
        assertEquals(PayoutStatus.COMPLETED, response.status());
        assertEquals(adminId, response.processedById());
        assertEquals("Super Admin", response.processedByName());
        assertNotNull(response.processedAt());
        assertNull(response.rejectionReason());
        assertEquals("TechHub Store", response.shopName());

        verify(sellerPayoutRequestRepository).save(payout);
        verify(sellerRepository, never()).incrementBalance(any(), any());
        verify(sellerWalletTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-02: Từ chối rút tiền thành công & Tự động hoàn tiền ví (Reject Happy Path)")
    void processPayoutRequest_Success_Reject() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.PENDING, new BigDecimal("2000000.00"), null);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdminUser));
        when(sellerWalletTransactionRepository.existsByReferenceIdAndType(payoutId, SellerWalletTransactionType.WITHDRAWAL_REFUND)).thenReturn(false);
        when(sellerRepository.findWithUserForUpdateById(sellerId)).thenReturn(Optional.of(testSeller));
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, "Sai tên chủ tài khoản thụ hưởng");

        AdminPayoutResponse response = adminPayoutService.processPayoutRequest(payoutId, adminId, request);

        assertNotNull(response);
        assertEquals(payoutId, response.id());
        assertEquals(PayoutStatus.REJECTED, response.status());
        assertEquals("Sai tên chủ tài khoản thụ hưởng", response.rejectionReason());
        assertEquals(adminId, response.processedById());
        assertNotNull(response.processedAt());

        verify(sellerRepository).incrementBalance(sellerId, new BigDecimal("2000000.00"));
        ArgumentCaptor<SellerWalletTransaction> txCaptor = ArgumentCaptor.forClass(SellerWalletTransaction.class);
        verify(sellerWalletTransactionRepository).save(txCaptor.capture());
        SellerWalletTransaction savedTx = txCaptor.getValue();
        assertEquals(SellerWalletTransactionType.WITHDRAWAL_REFUND, savedTx.getType());
        assertEquals(new BigDecimal("2000000.00"), savedTx.getAmount());
        assertEquals(new BigDecimal("10000000.00"), savedTx.getBalanceBefore());
        assertEquals(new BigDecimal("12000000.00"), savedTx.getBalanceAfter());
        assertEquals(payoutId, savedTx.getReferenceId());
        assertTrue(savedTx.getDescription().contains("Sai tên chủ tài khoản"));
    }

    @Test
    @DisplayName("TC-ADM-PROC-03: Thất bại khi Từ chối nhưng thiếu lý do (rejectionReason is null)")
    void processPayoutRequest_ThrowsException_RejectNullReason() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).findForUpdateById(any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-04: Thất bại khi Từ chối với lý do toàn khoảng trắng")
    void processPayoutRequest_ThrowsException_RejectBlankReason() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, "     ");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_REJECTION_REASON_REQUIRED, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).findForUpdateById(any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-05: Thất bại khi Payout Request không tồn tại")
    void processPayoutRequest_ThrowsException_NotFound() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.empty());

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_REQUEST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-ADM-PROC-06: Thất bại khi xử lý yêu cầu đã COMPLETED từ trước")
    void processPayoutRequest_ThrowsException_AlreadyCompleted() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.COMPLETED, new BigDecimal("1000000.00"), testAdminUser);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_ALREADY_PROCESSED, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-07: Thất bại khi xử lý yêu cầu đã REJECTED từ trước")
    void processPayoutRequest_ThrowsException_AlreadyRejected() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.REJECTED, new BigDecimal("1000000.00"), testAdminUser);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, "Lý do mới");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_ALREADY_PROCESSED, ex.getErrorCode());
        verify(sellerRepository, never()).incrementBalance(any(), any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-13: Kiểm tra tính Idempotent khi đã có giao dịch WITHDRAWAL_REFUND")
    void processPayoutRequest_ThrowsException_LedgerRefundAlreadyExists() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdminUser));
        when(sellerWalletTransactionRepository.existsByReferenceIdAndType(payoutId, SellerWalletTransactionType.WITHDRAWAL_REFUND)).thenReturn(true);

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.REJECT, "Từ chối hợp lệ");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.PAYOUT_ALREADY_PROCESSED, ex.getErrorCode());
        verify(sellerRepository, never()).incrementBalance(any(), any());
    }

    @Test
    @DisplayName("TC-ADM-PROC-14: Bỏ qua rejectionReason nếu action là APPROVE")
    void processPayoutRequest_Success_ApproveClearsReason() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = testAdminUser.getId();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(testAdminUser));
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, "Lý do vô nghĩa khi duyệt");

        AdminPayoutResponse response = adminPayoutService.processPayoutRequest(payoutId, adminId, request);

        assertNotNull(response);
        assertEquals(PayoutStatus.COMPLETED, response.status());
        assertNull(response.rejectionReason());
        verify(sellerPayoutRequestRepository).save(payout);
    }

    @Test
    @DisplayName("TC-ADM-PROC-17: Admin User không tồn tại ném USER_NOT_FOUND")
    void processPayoutRequest_ThrowsException_AdminUserNotFound() {
        UUID payoutId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        SellerPayoutRequest payout = createPayoutRequest(payoutId, PayoutStatus.PENDING, new BigDecimal("1000000.00"), null);

        when(sellerPayoutRequestRepository.findForUpdateById(payoutId)).thenReturn(Optional.of(payout));
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        ProcessPayoutRequest request = new ProcessPayoutRequest(PayoutProcessAction.APPROVE, null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminPayoutService.processPayoutRequest(payoutId, adminId, request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }
}
