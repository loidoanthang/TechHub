package com.techhub.service.admin.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateUserStatusRequest;
import com.techhub.model.dto.response.AdminUserResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.service.RefreshTokenService;
import com.techhub.specification.UserSpecification;
import com.techhub.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "email", "firstName", "lastName", "status"
    );

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public PageResponse<AdminUserResponse> getUsers(
            String keyword,
            UserStatus status,
            Boolean emailVerified,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);

        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, validSortBy));

        Specification<User> spec = UserSpecification.filterUsers(keyword, status, emailVerified);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        if (userPage.isEmpty()) {
            return PageResponse.from(userPage, List.of());
        }

        List<UUID> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        List<Address> defaultAddresses = addressRepository.findDefaultAddressesByUserIds(userIds);
        Map<UUID, Address> defaultAddressMap = defaultAddresses.stream()
                .collect(Collectors.toMap(
                        addr -> addr.getUser().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<AdminUserResponse> content = userPage.getContent().stream()
                .map(user -> {
                    Address defaultAddr = defaultAddressMap.get(user.getId());
                    String formattedAddress = formatAddress(defaultAddr);
                    return new AdminUserResponse(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getPhone(),
                            user.isEmailVerified(),
                            user.getStatus(),
                            formattedAddress,
                            user.getAvatarUrl(),
                            user.getCreatedAt()
                    );
                })
                .toList();

        return PageResponse.from(userPage, content);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        CustomUserDetails currentAdmin = SecurityUtils.getCurrentUser();
        UUID currentAdminId = currentAdmin.user().getId();

        // 1. Chống tự khóa chính mình
        if (currentAdminId.equals(userId) && (request.status() == UserStatus.BANNED || request.status() == UserStatus.SUSPENDED)) {
            throw new BusinessException(ErrorCode.CANNOT_BAN_SELF);
        }

        // 2. Chống chuyển trạng thái sang DELETED qua API này
        if (request.status() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 3. Tìm target user
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. Chống thao tác trên user đã bị DELETED
        if (targetUser.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // 5. Chống ban Admin khác
        if (targetUser.getRoles() != null && targetUser.getRoles().contains(Role.ADMIN)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_ADMIN_USER);
        }

        // 6. Cập nhật trạng thái

        UserStatus oldStatus = targetUser.getStatus();
        targetUser.setStatus(request.status());
        userRepository.save(targetUser);

        // 7. Nếu bị BANNED hoặc SUSPENDED -> Lập tức thu hồi toàn bộ Refresh Token của user
        if (request.status() == UserStatus.BANNED || request.status() == UserStatus.SUSPENDED) {
            refreshTokenService.revokeAll(targetUser);
        }

        log.info("Admin {} changed user {} status from {} to {}. Reason: {}",
                currentAdminId, userId, oldStatus, request.status(), request.reason());

        // 8. Lấy địa chỉ mặc định nếu có
        List<Address> addresses = addressRepository.findDefaultAddressesByUserIds(List.of(userId));
        Address defaultAddr = addresses.isEmpty() ? null : addresses.get(0);
        String formattedAddress = formatAddress(defaultAddr);

        return new AdminUserResponse(
                targetUser.getId(),
                targetUser.getFirstName(),
                targetUser.getLastName(),
                targetUser.getEmail(),
                targetUser.getPhone(),
                targetUser.isEmailVerified(),
                targetUser.getStatus(),
                formattedAddress,
                targetUser.getAvatarUrl(),
                targetUser.getCreatedAt()
        );
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return null;
        }
        return String.format("%s, %s, %s, %s",
                address.getStreetAddress(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince());
    }
}
