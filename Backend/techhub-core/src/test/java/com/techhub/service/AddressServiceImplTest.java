package com.techhub.service;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateAddressRequest;
import com.techhub.model.dto.request.UpdateAddressRequest;
import com.techhub.model.dto.response.AddressResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.User;
import com.techhub.model.enums.AddressLabel;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AddressService - API Thêm Mới Địa Chỉ (POST /api/addresses)")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User sampleUser;
    private CreateAddressRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("buyer@techhub.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setFirstName("Thang");
        sampleUser.setLastName("Loi");
        sampleUser.setPhone("0987654321");
        sampleUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setLockoutEndTime(null);

        sampleRequest = new CreateAddressRequest(
                AddressLabel.HOME,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi",
                false
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContextUser(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== SINGLE DEFAULT & FIRST ADDRESS TESTS ====================

    @Test
    @DisplayName("Tạo địa chỉ đầu tiên (count = 0) với isDefault = false -> Hệ thống tự động ép isDefault = true")
    void createAddress_FirstAddress_ForcedDefaultTrue() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(0L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        AddressResponse response = addressService.createAddress(sampleRequest);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Địa chỉ đầu tiên luôn phải được ép là mặc định (isDefault = true)");
        assertEquals("Loi Doan Thang", response.recipientName());
        assertEquals("0987654321", response.phone());
        assertEquals("TP. Hồ Chí Minh", response.province());
        assertEquals("123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", response.fullAddress());

        // Kiểm tra không gọi reset vì chưa có địa chỉ cũ nào
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Tạo địa chỉ đầu tiên với isDefault = null -> Hệ thống tự động ép isDefault = true")
    void createAddress_FirstAddress_NullDefault_ForcedDefaultTrue() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(0L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateAddressRequest nullDefaultRequest = new CreateAddressRequest(
                AddressLabel.HOME,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi",
                null
        );

        AddressResponse response = addressService.createAddress(nullDefaultRequest);

        assertNotNull(response);
        assertTrue(response.isDefault(), "isDefault null ở địa chỉ đầu tiên vẫn phải được ép thành true");
    }

    @Test
    @DisplayName("Tạo địa chỉ thứ 2 với isDefault = false -> Lưu isDefault = false, không reset default cũ")
    void createAddress_SecondAddress_NotDefault() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(1L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AddressResponse response = addressService.createAddress(sampleRequest);

        assertNotNull(response);
        assertFalse(response.isDefault(), "Địa chỉ thứ 2 tạo với isDefault = false thì không là mặc định");
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Tạo địa chỉ thứ 2 với isDefault = true -> Reset default các địa chỉ cũ và lưu mới là default")
    void createAddress_SecondAddress_SetDefaultTrue() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(1L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateAddressRequest defaultRequest = new CreateAddressRequest(
                AddressLabel.OFFICE,
                "Loi Doan Thang",
                "0987654321",
                "Hà Nội",
                "Quận Ba Đình",
                "Phường Kim Mã",
                "456 Kim Mã",
                true
        );

        AddressResponse response = addressService.createAddress(defaultRequest);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Địa chỉ mới phải là default");
        assertEquals(AddressLabel.OFFICE, response.label());

        // Phải gọi reset default địa chỉ cũ
        verify(addressRepository, times(1)).resetDefaultAddressesByUserId(sampleUser.getId());
        verify(addressRepository).save(any(Address.class));
    }

    // ==================== MAXIMUM 10 ADDRESSES LIMIT TESTS ====================

    @Test
    @DisplayName("Đã có đủ 10 địa chỉ -> Ném ADDRESS_LIMIT_EXCEEDED (HTTP 400)")
    void createAddress_LimitExceeded_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(10L);

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ADDRESS_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đã có hơn 10 địa chỉ (ví dụ 11) -> Vẫn ném ADDRESS_LIMIT_EXCEEDED")
    void createAddress_MoreThan10Addresses_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(11L);

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ADDRESS_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    // ==================== NORMALIZATION & VALIDATION TESTS ====================

    @Test
    @DisplayName("Request gửi label = null -> Hệ thống tự gán mặc định là AddressLabel.HOME")
    void createAddress_DefaultLabelHome_WhenLabelNull() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(0L);

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        when(addressRepository.save(addressCaptor.capture())).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateAddressRequest requestWithNullLabel = new CreateAddressRequest(
                null,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi",
                false
        );

        AddressResponse response = addressService.createAddress(requestWithNullLabel);

        assertNotNull(response);
        assertEquals(AddressLabel.HOME, response.label(), "Label null phải mặc định là HOME");
        assertEquals(AddressLabel.HOME, addressCaptor.getValue().getLabel());
        assertNull(addressCaptor.getValue().getShopId(), "shopId của Buyer address luôn là null");
        assertEquals(sampleUser.getId(), addressCaptor.getValue().getUser().getId());
    }

    @Test
    @DisplayName("Input có khoảng trắng ở hai đầu -> Tự động trim sạch sẽ")
    void createAddress_NormalizesAndTrimsStrings() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.countByUserIdAndShopIdIsNull(sampleUser.getId())).thenReturn(0L);

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        when(addressRepository.save(addressCaptor.capture())).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateAddressRequest untrimmedRequest = new CreateAddressRequest(
                AddressLabel.OTHER,
                "   Loi Doan Thang   ",
                "   0987654321   ",
                "   TP. Hồ Chí Minh   ",
                "   Quận 1   ",
                "   Phường Bến Nghé   ",
                "   123 Lê Lợi   ",
                false
        );

        addressService.createAddress(untrimmedRequest);

        Address saved = addressCaptor.getValue();
        assertEquals("Loi Doan Thang", saved.getRecipientName());
        assertEquals("0987654321", saved.getPhone());
        assertEquals("TP. Hồ Chí Minh", saved.getProvince());
        assertEquals("Quận 1", saved.getDistrict());
        assertEquals("Phường Bến Nghé", saved.getWard());
        assertEquals("123 Lê Lợi", saved.getStreetAddress());
        assertEquals(AddressLabel.OTHER, saved.getLabel());
    }

    // ==================== SECURITY & USER ACCOUNT STATUS TESTS ====================

    @Test
    @DisplayName("Không tìm thấy User trong DB -> Ném USER_NOT_FOUND (HTTP 404)")
    void createAddress_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chưa xác thực (SecurityContext rỗng) -> Ném UNAUTHENTICATED (HTTP 401)")
    void createAddress_Unauthenticated_ThrowsException() {
        // Không set SecurityContext
        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản đang bị tạm khóa (lockoutEndTime còn hiệu lực) -> Ném ACCOUNT_LOCKED (HTTP 403)")
    void createAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản chưa xác thực email (emailVerified = false) -> Ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void createAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản bị BANNED -> Ném ACCOUNT_BANNED (HTTP 403)")
    void createAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản bị SUSPENDED -> Ném ACCOUNT_SUSPENDED (HTTP 403)")
    void createAddress_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản đã bị xóa (DELETED) -> Ném ACCOUNT_DELETED (HTTP 403)")
    void createAddress_AccountDeleted_ThrowsException() {
        sampleUser.setStatus(UserStatus.DELETED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.createAddress(sampleRequest));

        assertEquals(ErrorCode.ACCOUNT_DELETED, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    // ==================== GET MY ADDRESSES TESTS ====================

    @Test
    @DisplayName("Lấy danh sách địa chỉ thành công - Sắp xếp địa chỉ mặc định lên đầu")
    void getMyAddresses_Success_ReturnsSortedList() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        Address defaultAddress = Address.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Loi Doan Thang")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .isDefault(true)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Address secondaryAddress = Address.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .label(AddressLabel.OFFICE)
                .recipientName("Loi Doan Thang")
                .phone("0987654321")
                .province("Hà Nội")
                .district("Quận Ba Đình")
                .ward("Phường Kim Mã")
                .streetAddress("456 Kim Mã")
                .isDefault(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        when(addressRepository.findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(sampleUser.getId()))
                .thenReturn(List.of(defaultAddress, secondaryAddress));

        List<AddressResponse> responses = addressService.getMyAddresses();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).isDefault(), "Phần tử đầu tiên phải là địa chỉ mặc định");
        assertEquals(AddressLabel.HOME, responses.get(0).label());
        assertFalse(responses.get(1).isDefault(), "Phần tử thứ hai là địa chỉ phụ");
        assertEquals(AddressLabel.OFFICE, responses.get(1).label());
        assertEquals("123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", responses.get(0).fullAddress());
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ khi chưa có địa chỉ nào -> Trả về mảng rỗng")
    void getMyAddresses_EmptyList_ReturnsEmpty() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(addressRepository.findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(sampleUser.getId()))
                .thenReturn(List.of());

        List<AddressResponse> responses = addressService.getMyAddresses();

        assertNotNull(responses);
        assertTrue(responses.isEmpty(), "Danh sách trả về phải rỗng khi user chưa tạo địa chỉ nào");
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ: Không tìm thấy User trong DB -> Ném USER_NOT_FOUND (HTTP 404)")
    void getMyAddresses_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.getMyAddresses());

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ: Chưa đăng nhập -> Ném UNAUTHENTICATED (HTTP 401)")
    void getMyAddresses_Unauthenticated_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.getMyAddresses());

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ: Tài khoản đang bị khóa 15p -> Ném ACCOUNT_LOCKED (HTTP 403)")
    void getMyAddresses_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.getMyAddresses());

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(addressRepository, never()).findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ: Tài khoản chưa kích hoạt email -> Ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void getMyAddresses_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.getMyAddresses());

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(addressRepository, never()).findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Lấy danh sách địa chỉ: Tài khoản bị BANNED -> Ném ACCOUNT_BANNED (HTTP 403)")
    void getMyAddresses_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.getMyAddresses());

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(any());
    }

    // ==================== UPDATE ADDRESS TESTS ====================

    @Test
    @DisplayName("Cập nhật địa chỉ phụ thành công (giữ nguyên isDefault = false)")
    void updateAddress_Success_SecondaryAddress_KeepSecondary() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address existingAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Old Name")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Old Address")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.OFFICE,
                "New Name",
                "0912345678",
                "Hà Nội",
                "Quận Cầu Giấy",
                "Phường Dịch Vọng",
                "99 Xuân Thủy",
                false
        );

        AddressResponse response = addressService.updateAddress(addressId, updateRequest);

        assertNotNull(response);
        assertEquals("New Name", response.recipientName());
        assertEquals("0912345678", response.phone());
        assertEquals("Hà Nội", response.province());
        assertEquals("Quận Cầu Giấy", response.district());
        assertEquals("Phường Dịch Vọng", response.ward());
        assertEquals("99 Xuân Thủy", response.streetAddress());
        assertEquals(AddressLabel.OFFICE, response.label());
        assertFalse(response.isDefault(), "Địa chỉ phụ tiếp tục giữ isDefault = false");

        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository).save(existingAddress);
    }

    @Test
    @DisplayName("Cập nhật địa chỉ phụ và đôn lên làm mặc định (isDefault = true) -> Reset default cũ và lưu mới")
    void updateAddress_Success_PromoteToDefault() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address existingAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Old Name")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Old Address")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi",
                true
        );

        AddressResponse response = addressService.updateAddress(addressId, updateRequest);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Địa chỉ này phải được chuyển thành isDefault = true");
        verify(addressRepository, times(1)).resetDefaultAddressesByUserId(sampleUser.getId());
        verify(addressRepository).save(existingAddress);
    }

    @Test
    @DisplayName("Cập nhật địa chỉ đang là mặc định với isDefault = true hoặc null -> Giữ nguyên mặc định")
    void updateAddress_Success_DefaultAddress_KeepDefault() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address defaultAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Old Name")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("Old Address")
                .isDefault(true)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(defaultAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi Mới",
                null
        );

        AddressResponse response = addressService.updateAddress(addressId, updateRequest);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Địa chỉ mặc định vẫn phải là true");
        assertEquals("123 Lê Lợi Mới", response.streetAddress());
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository).save(defaultAddress);
    }

    @Test
    @DisplayName("Cập nhật địa chỉ đang là mặc định nhưng cố tình gửi isDefault = false -> Ném CANNOT_UNSET_DEFAULT_ADDRESS (HTTP 400)")
    void updateAddress_DefaultAddress_UnsetDefault_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address defaultAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Loi Doan Thang")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .isDefault(true)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(defaultAddress));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME,
                "Loi Doan Thang",
                "0987654321",
                "TP. Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "123 Lê Lợi",
                false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.CANNOT_UNSET_DEFAULT_ADDRESS, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Input có khoảng trắng ở hai đầu -> Tự động trim() toàn bộ các trường")
    void updateAddress_NormalizesAndTrimsStrings() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address address = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Old")
                .phone("0987654321")
                .province("Old")
                .district("Old")
                .ward("Old")
                .streetAddress("Old")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.OTHER,
                "   Trimmed Name   ",
                "   0912345678   ",
                "   Đà Nẵng   ",
                "   Quận Hải Châu   ",
                "   Phường Thạch Thang   ",
                "   10 Quang Trung   ",
                false
        );

        AddressResponse response = addressService.updateAddress(addressId, updateRequest);

        assertNotNull(response);
        assertEquals("Trimmed Name", response.recipientName());
        assertEquals("0912345678", response.phone());
        assertEquals("Đà Nẵng", response.province());
        assertEquals("Quận Hải Châu", response.district());
        assertEquals("Phường Thạch Thang", response.ward());
        assertEquals("10 Quang Trung", response.streetAddress());
        assertEquals("10 Quang Trung, Phường Thạch Thang, Quận Hải Châu, Đà Nẵng", response.fullAddress());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Request gửi label = null -> Hệ thống gán mặc định là AddressLabel.HOME")
    void updateAddress_DefaultLabelHome_WhenLabelNull() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address address = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.OFFICE)
                .recipientName("Name")
                .phone("0987654321")
                .province("Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng")
                .streetAddress("123 Cầu Giấy")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                null,
                "Name",
                "0987654321",
                "Hà Nội",
                "Quận Cầu Giấy",
                "Phường Dịch Vọng",
                "123 Cầu Giấy",
                false
        );

        AddressResponse response = addressService.updateAddress(addressId, updateRequest);

        assertNotNull(response);
        assertEquals(AddressLabel.HOME, response.label(), "Label null phải mặc định là HOME");
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Không tìm thấy Address ID -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void updateAddress_AddressNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID nonExistentId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(nonExistentId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(nonExistentId, updateRequest));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Cố tình sửa địa chỉ của người khác (IDOR) -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void updateAddress_OtherUserAddress_IDOR_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID victimAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(victimAddressId, sampleUser.getId()))
                .thenReturn(Optional.empty()); // DB query đã lọc theo sampleUser.getId()

        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(victimAddressId, updateRequest));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Không tìm thấy User trong DB -> Ném USER_NOT_FOUND (HTTP 404)")
    void updateAddress_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        UUID addressId = UUID.randomUUID();
        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Chưa đăng nhập -> Ném UNAUTHENTICATED (HTTP 401)")
    void updateAddress_Unauthenticated_ThrowsException() {
        UUID addressId = UUID.randomUUID();
        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Tài khoản đang bị khóa 15p -> Ném ACCOUNT_LOCKED (HTTP 403)")
    void updateAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Tài khoản chưa kích hoạt email -> Ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void updateAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
    }

    @Test
    @DisplayName("Cập nhật địa chỉ: Tài khoản bị BANNED -> Ném ACCOUNT_BANNED (HTTP 403)")
    void updateAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        UpdateAddressRequest updateRequest = new UpdateAddressRequest(
                AddressLabel.HOME, "Name", "0987654321", "Province", "District", "Ward", "Street", false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.updateAddress(addressId, updateRequest));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
    }

    // =========================================================================
    // UNIT TESTS: API XÓA ĐỊA CHỈ (DELETE /api/addresses/{id})
    // =========================================================================

    @Test
    @DisplayName("Xóa địa chỉ phụ thành công (isDefault = false) -> Gọi delete() bình thường")
    void deleteAddress_Success_SecondaryAddress_DeletesSuccessfully() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address secondaryAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.OFFICE)
                .recipientName("Secondary Address")
                .phone("0987654321")
                .province("Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng")
                .streetAddress("123 Duy Tân")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(secondaryAddress));

        assertDoesNotThrow(() -> addressService.deleteAddress(addressId));

        verify(addressRepository).delete(secondaryAddress);
    }

    @Test
    @DisplayName("Cố tình xóa địa chỉ đang là mặc định (isDefault = true) -> Ném CANNOT_DELETE_DEFAULT_ADDRESS (HTTP 400)")
    void deleteAddress_DefaultAddress_ThrowsCannotDeleteDefaultAddress() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address defaultAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Default Address")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .isDefault(true)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(defaultAddress));

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS, ex.getErrorCode());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Không tìm thấy ID địa chỉ trong DB -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void deleteAddress_AddressNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID nonExistentId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(nonExistentId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(nonExistentId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Chống IDOR (Địa chỉ thuộc về User khác) -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void deleteAddress_BelongsToAnotherUser_ThrowsAddressNotFound() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID otherUserAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(otherUserAddressId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(otherUserAddressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Địa chỉ kho của Shop (shop_id IS NOT NULL) -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void deleteAddress_ShopAddress_ThrowsAddressNotFound() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID shopAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(shopAddressId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(shopAddressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Không tìm thấy User trong DB -> Ném USER_NOT_FOUND (HTTP 404)")
    void deleteAddress_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Chưa xác thực (SecurityContext rỗng) -> Ném UNAUTHENTICATED (HTTP 401)")
    void deleteAddress_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Tài khoản đang bị tạm khóa 15p -> Ném ACCOUNT_LOCKED (HTTP 403)")
    void deleteAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Tài khoản chưa kích hoạt email -> Ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void deleteAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Tài khoản bị BANNED -> Ném ACCOUNT_BANNED (HTTP 403)")
    void deleteAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa địa chỉ: Tài khoản bị SUSPENDED -> Ném ACCOUNT_SUSPENDED (HTTP 403)")
    void deleteAddress_AccountSuspended_ThrowsException() {
        sampleUser.setStatus(UserStatus.SUSPENDED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.deleteAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).delete(any());
    }

    // =========================================================================
    // UNIT TESTS: API 1-CLICK THIẾT LẬP ĐỊA CHỈ MẶC ĐỊNH (PATCH /api/addresses/{id}/default)
    // =========================================================================

    @Test
    @DisplayName("Đặt địa chỉ phụ làm mặc định thành công -> Reset default cũ và gán isDefault = true")
    void setDefaultAddress_Success_PromotesSecondaryToDefault() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address secondaryAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.OFFICE)
                .recipientName("Secondary Address")
                .phone("0987654321")
                .province("Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng")
                .streetAddress("123 Duy Tân")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(secondaryAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(addressId);

        assertNotNull(response);
        assertTrue(response.isDefault(), "Địa chỉ phải được chuyển sang isDefault = true");
        verify(addressRepository).resetDefaultAddressesByUserId(sampleUser.getId());
        verify(addressRepository).save(secondaryAddress);
    }

    @Test
    @DisplayName("Địa chỉ ĐÃ LÀ MẶC ĐỊNH sẵn -> Idempotent, không gọi reset và không gọi save DB")
    void setDefaultAddress_Success_AlreadyDefault_Idempotent() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();
        Address defaultAddress = Address.builder()
                .id(addressId)
                .user(sampleUser)
                .label(AddressLabel.HOME)
                .recipientName("Default Address")
                .phone("0987654321")
                .province("TP. Hồ Chí Minh")
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .isDefault(true)
                .build();

        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, sampleUser.getId()))
                .thenReturn(Optional.of(defaultAddress));

        AddressResponse response = addressService.setDefaultAddress(addressId);

        assertNotNull(response);
        assertTrue(response.isDefault());
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Không tìm thấy ID địa chỉ trong DB -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void setDefaultAddress_AddressNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID nonExistentId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(nonExistentId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(nonExistentId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Chống IDOR (Địa chỉ thuộc về User khác) -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void setDefaultAddress_BelongsToAnotherUser_ThrowsAddressNotFound() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID otherUserAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(otherUserAddressId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(otherUserAddressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Địa chỉ kho của Shop (shop_id IS NOT NULL) -> Ném ADDRESS_NOT_FOUND (HTTP 404)")
    void setDefaultAddress_ShopAddress_ThrowsAddressNotFound() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID shopAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserIdAndShopIdIsNull(shopAddressId, sampleUser.getId()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(shopAddressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).resetDefaultAddressesByUserId(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Không tìm thấy User trong DB -> Ném USER_NOT_FOUND (HTTP 404)")
    void setDefaultAddress_UserNotFound_ThrowsException() {
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(addressId));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Chưa xác thực (SecurityContext rỗng) -> Ném UNAUTHENTICATED (HTTP 401)")
    void setDefaultAddress_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(addressId));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Tài khoản đang bị tạm khóa 15p -> Ném ACCOUNT_LOCKED (HTTP 403)")
    void setDefaultAddress_AccountLocked_ThrowsException() {
        sampleUser.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Tài khoản chưa kích hoạt email -> Ném ACCOUNT_NOT_VERIFIED (HTTP 403)")
    void setDefaultAddress_AccountNotVerified_ThrowsException() {
        sampleUser.setEmailVerified(false);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_NOT_VERIFIED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt mặc định: Tài khoản bị BANNED -> Ném ACCOUNT_BANNED (HTTP 403)")
    void setDefaultAddress_AccountBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);
        setSecurityContextUser(sampleUser);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UUID addressId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class, () -> addressService.setDefaultAddress(addressId));

        assertEquals(ErrorCode.ACCOUNT_BANNED, ex.getErrorCode());
        verify(addressRepository, never()).findByIdAndUserIdAndShopIdIsNull(any(), any());
        verify(addressRepository, never()).save(any());
    }
}
