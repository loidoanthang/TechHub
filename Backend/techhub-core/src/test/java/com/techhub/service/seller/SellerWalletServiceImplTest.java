package com.techhub.service.seller;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.payout.WithdrawalRequest;
import com.techhub.model.dto.response.payout.PayoutResponse;
import com.techhub.model.dto.response.wallet.SellerWalletTransactionResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.SellerPayoutRequest;
import com.techhub.model.entity.SellerWalletTransaction;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.PayoutStatus;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.SellerWalletTransactionType;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.SellerPayoutRequestRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.SellerWalletTransactionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.seller.impl.SellerWalletServiceImpl;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerWalletServiceImpl (POST /api/seller/wallet/withdraw)")
class SellerWalletServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerPayoutRequestRepository sellerPayoutRequestRepository;

    @Mock
    private SellerWalletTransactionRepository sellerWalletTransactionRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private SellerWalletServiceImpl sellerWalletService;

    private UUID userId;
    private UUID sellerId;
    private User testUser;
    private Seller testSeller;
    private Shop testShop;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("seller@techhub.vn");
        testUser.setStatus(UserStatus.ACTIVE);

        testSeller = Seller.builder()
                .id(sellerId)
                .user(testUser)
                .sellerName("TechHub Official Store")
                .verificationStatus(SellerVerificationStatus.APPROVED)
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountHolder("NGUYEN VAN A")
                .balance(new BigDecimal("5000000.00"))
                .build();

        testShop = Shop.builder()
                .id(UUID.randomUUID())
                .seller(testSeller)
                .name("TechHub Store")
                .status(ShopStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC-WDR-01: Happy Path - Yêu cầu rút tiền hợp lệ (1,000,000 VND)")
    void requestWithdrawal_Success_StandardAmount() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> {
            SellerPayoutRequest pr = invocation.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        PayoutResponse response = sellerWalletService.requestWithdrawal(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000000.00"), response.amount());
        assertEquals(PayoutStatus.PENDING, response.status());
        assertEquals("TechHub Store", response.shopName());
        assertEquals(new BigDecimal("4000000.00"), response.remainingBalance());
        assertEquals(new BigDecimal("4000000.00"), testSeller.getBalance());

        // Verify ledger creation
        ArgumentCaptor<SellerWalletTransaction> txCaptor = ArgumentCaptor.forClass(SellerWalletTransaction.class);
        verify(sellerWalletTransactionRepository).save(txCaptor.capture());
        SellerWalletTransaction tx = txCaptor.getValue();
        assertEquals(SellerWalletTransactionType.WITHDRAWAL, tx.getType());
        assertEquals(new BigDecimal("1000000.00"), tx.getAmount());
        assertEquals(new BigDecimal("5000000.00"), tx.getBalanceBefore());
        assertEquals(new BigDecimal("4000000.00"), tx.getBalanceAfter());

        // Verify snapshot bank info in payout request
        ArgumentCaptor<SellerPayoutRequest> payoutCaptor = ArgumentCaptor.forClass(SellerPayoutRequest.class);
        verify(sellerPayoutRequestRepository).save(payoutCaptor.capture());
        SellerPayoutRequest payout = payoutCaptor.getValue();
        assertEquals("Vietcombank", payout.getBankName());
        assertEquals("1234567890", payout.getBankAccountNumber());
        assertEquals("NGUYEN VAN A", payout.getBankAccountHolder());
    }

    @Test
    @DisplayName("TC-WDR-02: Boundary - Rút đúng mức tối thiểu 50,000 VND")
    void requestWithdrawal_Success_MinAmount() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("50000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> {
            SellerPayoutRequest pr = invocation.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        PayoutResponse response = sellerWalletService.requestWithdrawal(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000.00"), response.amount());
        assertEquals(new BigDecimal("4950000.00"), testSeller.getBalance());
        assertEquals(new BigDecimal("4950000.00"), response.remainingBalance());
    }

    @Test
    @DisplayName("TC-WDR-03: Boundary - Rút đúng mức tối đa 100,000,000 VND")
    void requestWithdrawal_Success_MaxAmount() {
        testSeller.setBalance(new BigDecimal("150000000.00"));
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("100000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> {
            SellerPayoutRequest pr = invocation.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        PayoutResponse response = sellerWalletService.requestWithdrawal(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("100000000.00"), response.amount());
        assertEquals(new BigDecimal("50000000.00"), testSeller.getBalance());
        assertEquals(new BigDecimal("50000000.00"), response.remainingBalance());
    }

    @Test
    @DisplayName("TC-WDR-08: Thất bại - Số dư ví không đủ (balance < amount)")
    void requestWithdrawal_ThrowsException_InsufficientBalance() {
        testSeller.setBalance(new BigDecimal("200000.00"));
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("500000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.INSUFFICIENT_WALLET_BALANCE, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).save(any());
        verify(sellerWalletTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-WDR-09: Thất bại - Vượt hạn mức rút tiền hàng ngày (100,000,000 VND/ngày)")
    void requestWithdrawal_ThrowsException_DailyLimitExceeded() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("20000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        // Đã rút 85,000,000 VND trong ngày, cộng thêm 20,000,000 VND sẽ vượt 100,000,000 VND
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(), any(), anyList()))
                .thenReturn(new BigDecimal("85000000.00"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(sellerPayoutRequestRepository, never()).save(any());
        verify(sellerWalletTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-WDR-10: Boundary - Rút đúng bằng hạn mức hàng ngày còn lại")
    void requestWithdrawal_Success_ExactDailyLimit() {
        testSeller.setBalance(new BigDecimal("50000000.00"));
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("15000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));
        // Đã rút 85,000,000 VND trong ngày, cộng thêm 15,000,000 VND đúng bằng 100,000,000 VND
        when(sellerPayoutRequestRepository.sumDailyWithdrawalAmount(eq(sellerId), any(), any(), anyList()))
                .thenReturn(new BigDecimal("85000000.00"));
        when(sellerPayoutRequestRepository.save(any(SellerPayoutRequest.class))).thenAnswer(invocation -> {
            SellerPayoutRequest pr = invocation.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        PayoutResponse response = sellerWalletService.requestWithdrawal(userId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("15000000.00"), response.amount());
        assertEquals(new BigDecimal("35000000.00"), testSeller.getBalance());
    }

    @Test
    @DisplayName("TC-WDR-11: Thất bại - Không tìm thấy Seller profile")
    void requestWithdrawal_ThrowsException_SellerNotFound() {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-WDR-12: Thất bại - Seller chưa được duyệt (PENDING/REJECTED)")
    void requestWithdrawal_ThrowsException_SellerNotApproved() {
        testSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.SELLER_NOT_VERIFIED, ex.getErrorCode());

        // Test with REJECTED
        testSeller.setVerificationStatus(SellerVerificationStatus.REJECTED);
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));
        assertEquals(ErrorCode.SELLER_NOT_VERIFIED, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-WDR-13: Thất bại - Tài khoản User không ở trạng thái ACTIVE")
    void requestWithdrawal_ThrowsException_UserNotActive() {
        testUser.setStatus(UserStatus.SUSPENDED);
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, ex.getErrorCode());

        testUser.setStatus(UserStatus.BANNED);
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));
        assertEquals(ErrorCode.USER_NOT_ACTIVE, ex2.getErrorCode());
    }

    @Test
    @DisplayName("TC-WDR-14: Thất bại - Shop của người bán bị BANNED")
    void requestWithdrawal_ThrowsException_ShopBanned() {
        testShop.setStatus(ShopStatus.BANNED);
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));

        assertEquals(ErrorCode.SHOP_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-WDR-15: Thất bại - Thông tin tài khoản ngân hàng chưa cấu hình hoặc rỗng")
    void requestWithdrawal_ThrowsException_BankNotConfigured() {
        testSeller.setBankName(null);
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000000.00"));

        when(sellerRepository.findWithUserForUpdateByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(shopRepository.findBySellerId(sellerId)).thenReturn(Optional.of(testShop));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));
        assertEquals(ErrorCode.BANK_ACCOUNT_NOT_CONFIGURED, ex.getErrorCode());

        // Test with blank account number
        testSeller.setBankName("Vietcombank");
        testSeller.setBankAccountNumber("   ");
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));
        assertEquals(ErrorCode.BANK_ACCOUNT_NOT_CONFIGURED, ex2.getErrorCode());

        // Test with empty account holder
        testSeller.setBankAccountNumber("1234567890");
        testSeller.setBankAccountHolder("");
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
                sellerWalletService.requestWithdrawal(userId, request));
        assertEquals(ErrorCode.BANK_ACCOUNT_NOT_CONFIGURED, ex3.getErrorCode());
    }

    // =========================================================================
    // API 2: GET /api/seller/wallet/transactions (TC-LEDGER-01 đến TC-LEDGER-18)
    // =========================================================================

    private SellerWalletTransaction createMockTransaction(UUID id, SellerWalletTransactionType type, BigDecimal amount, BigDecimal before, BigDecimal after) {
        return SellerWalletTransaction.builder()
                .id(id != null ? id : UUID.randomUUID())
                .seller(testSeller)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .referenceId(UUID.randomUUID())
                .description("Mock transaction " + type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-LEDGER-01: Phân trang mặc định - page=0, size=20, DESC (Happy Path)")
    void getWalletTransactions_Success_DefaultPagination() {
        List<SellerWalletTransaction> mockList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            mockList.add(createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.ORDER_SETTLEMENT,
                    new BigDecimal("100000.00"), new BigDecimal("1000000.00"), new BigDecimal("1100000.00")));
        }
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(mockList, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 25);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(20, response.content().size());
        assertEquals(0, response.pageNumber());
        assertEquals(20, response.pageSize());
        assertEquals(25, response.totalElements());
        assertEquals(2, response.totalPages());
        assertFalse(response.last());
    }

    @Test
    @DisplayName("TC-LEDGER-02: Phân trang trang kế tiếp - page=1, size=20")
    void getWalletTransactions_Success_NextPage() {
        List<SellerWalletTransaction> mockList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockList.add(createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.WITHDRAWAL,
                    new BigDecimal("50000.00"), new BigDecimal("500000.00"), new BigDecimal("450000.00")));
        }
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(mockList, PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 25);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 1, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(5, response.content().size());
        assertEquals(1, response.pageNumber());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("TC-LEDGER-03: Lọc theo type = ORDER_SETTLEMENT")
    void getWalletTransactions_Success_FilterOrderSettlement() {
        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.ORDER_SETTLEMENT,
                new BigDecimal("500000.00"), new BigDecimal("1000000.00"), new BigDecimal("1500000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, SellerWalletTransactionType.ORDER_SETTLEMENT, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(SellerWalletTransactionType.ORDER_SETTLEMENT, response.content().get(0).type());
    }

    @Test
    @DisplayName("TC-LEDGER-04: Lọc theo type = WITHDRAWAL")
    void getWalletTransactions_Success_FilterWithdrawal() {
        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.WITHDRAWAL,
                new BigDecimal("200000.00"), new BigDecimal("1000000.00"), new BigDecimal("800000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, SellerWalletTransactionType.WITHDRAWAL, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(SellerWalletTransactionType.WITHDRAWAL, response.content().get(0).type());
    }

    @Test
    @DisplayName("TC-LEDGER-05: Lọc theo type = WITHDRAWAL_REFUND")
    void getWalletTransactions_Success_FilterWithdrawalRefund() {
        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.WITHDRAWAL_REFUND,
                new BigDecimal("200000.00"), new BigDecimal("800000.00"), new BigDecimal("1000000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, SellerWalletTransactionType.WITHDRAWAL_REFUND, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(SellerWalletTransactionType.WITHDRAWAL_REFUND, response.content().get(0).type());
    }

    @Test
    @DisplayName("TC-LEDGER-06: Lọc theo khoảng ngày (startDate và endDate)")
    void getWalletTransactions_Success_FilterDateRange() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 4);

        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.ORDER_SETTLEMENT,
                new BigDecimal("300000.00"), new BigDecimal("700000.00"), new BigDecimal("1000000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, startDate, endDate, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-LEDGER-07: Lọc chỉ có startDate")
    void getWalletTransactions_Success_FilterOnlyStartDate() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);

        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.ORDER_SETTLEMENT,
                new BigDecimal("300000.00"), new BigDecimal("700000.00"), new BigDecimal("1000000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, startDate, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-LEDGER-08: Lọc chỉ có endDate")
    void getWalletTransactions_Success_FilterOnlyEndDate() {
        LocalDate endDate = LocalDate.of(2026, 11, 4);

        SellerWalletTransaction tx = createMockTransaction(UUID.randomUUID(), SellerWalletTransactionType.ORDER_SETTLEMENT,
                new BigDecimal("300000.00"), new BigDecimal("700000.00"), new BigDecimal("1000000.00"));
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, endDate, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
    }

    @Test
    @DisplayName("TC-LEDGER-09: Sắp xếp theo thứ tự tăng dần ASC")
    void getWalletTransactions_Success_SortAsc() {
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 0, 20, "ASC"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerWalletTransactionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("TC-LEDGER-10: Chuỗi direction không hợp lệ tự động fallback về DESC")
    void getWalletTransactions_Success_SortFallback() {
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 0, 20, "INVALID_DIR"
        );

        assertNotNull(response);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sellerWalletTransactionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("TC-LEDGER-11: Sổ cái rỗng cho người bán mới (0 bản ghi)")
    void getWalletTransactions_Success_EmptyLedger() {
        Page<SellerWalletTransaction> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("TC-LEDGER-12: Thất bại - Khoảng ngày không hợp lệ (startDate > endDate)")
    void getWalletTransactions_ThrowsException_InvalidDateRange() {
        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.getWalletTransactions(userId, null, startDate, endDate, 0, 20, "DESC"));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
        verify(sellerWalletTransactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-LEDGER-16: Thất bại - Không tìm thấy Seller profile")
    void getWalletTransactions_ThrowsException_SellerNotFound() {
        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                sellerWalletService.getWalletTransactions(userId, null, null, null, 0, 20, "DESC"));

        assertEquals(ErrorCode.SELLER_NOT_FOUND, ex.getErrorCode());
        verify(sellerWalletTransactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-LEDGER-18: Kiểm tra cô lập dữ liệu đa người thuê (Multi-Tenancy Isolation)")
    void getWalletTransactions_Success_MultiTenancyDataIsolation() {
        Page<SellerWalletTransaction> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(testSeller));
        when(sellerWalletTransactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SellerWalletTransactionResponse> response = sellerWalletService.getWalletTransactions(
                userId, null, null, null, 0, 20, "DESC"
        );

        assertNotNull(response);
        verify(sellerRepository).findByUserId(userId);
        verify(sellerWalletTransactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
