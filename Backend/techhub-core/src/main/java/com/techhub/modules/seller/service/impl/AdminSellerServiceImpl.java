package com.techhub.modules.seller.service.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.seller.dto.request.SellerApprovalRequest;
import com.techhub.modules.seller.dto.response.AdminSellerApprovalResponse;
import com.techhub.modules.seller.dto.response.AdminSellerDetailResponse;
import com.techhub.modules.seller.dto.response.AdminSellerResponse;
import com.techhub.modules.user.entity.Address;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.shipping.entity.ShippingOption;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.user.enums.Role;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.user.repository.AddressRepository;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shipping.repository.ShippingOptionRepository;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.user.repository.UserRepository;
import com.techhub.modules.seller.service.AdminSellerService;
import com.techhub.modules.seller.specification.SellerSpecification;
import com.techhub.modules.notification.enums.NotificationType;
import com.techhub.modules.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.techhub.modules.seller.service.AdminSellerService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminSellerServiceImpl implements AdminSellerService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "sellerName", "verificationStatus"
    );

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final AddressRepository addressRepository;
    private final ShippingOptionRepository shippingOptionRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;


    @Override
    public PageResponse<AdminSellerResponse> getSellers(
            String keyword,
            SellerVerificationStatus status,
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

        Specification<Seller> spec = SellerSpecification.filterSellers(keyword, status);
        Page<Seller> sellerPage = sellerRepository.findAll(spec, pageable);

        if (sellerPage.isEmpty()) {
            return PageResponse.from(sellerPage, List.of());
        }

        List<UUID> sellerIds = sellerPage.getContent().stream()
                .map(Seller::getId)
                .toList();

        List<Shop> shops = shopRepository.findBySellerIdIn(sellerIds);
        Map<UUID, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(
                        shop -> shop.getSeller().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<AdminSellerResponse> content = sellerPage.getContent().stream()
                .map(seller -> {
                    User user = seller.getUser();
                    Shop shop = shopMap.get(seller.getId());

                    String ownerName = formatFullName(
                            user != null ? user.getFirstName() : null,
                            user != null ? user.getLastName() : null
                    );
                    UUID userId = user != null ? user.getId() : null;
                    String ownerEmail = user != null ? user.getEmail() : null;
                    String ownerPhone = user != null ? user.getPhone() : null;
                    UserStatus ownerStatus = user != null ? user.getStatus() : null;

                    UUID shopId = shop != null ? shop.getId() : null;
                    String shopName = shop != null ? shop.getName() : null;
                    ShopStatus shopStatus = shop != null ? shop.getStatus() : null;

                    return new AdminSellerResponse(
                            seller.getId(),
                            userId,
                            ownerName,
                            ownerEmail,
                            ownerPhone,
                            ownerStatus,
                            shopId,
                            shopName,
                            shopStatus,
                            seller.getSellerName(),
                            seller.getVerificationStatus(),
                            seller.getRejectionReason(),
                            seller.getBankName(),
                            seller.getBankAccountNumber(),
                            seller.getBankAccountHolder(),
                            seller.getCreatedAt(),
                            seller.getUpdatedAt()
                    );
                })
                .toList();

        return PageResponse.from(sellerPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSellerDetailResponse getSellerDetail(UUID id) {
        Seller seller = sellerRepository.findWithUserById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        log.info("Admin viewing seller details: sellerId={}, sellerName={}, verificationStatus={}",
                id, seller.getSellerName(), seller.getVerificationStatus());

        User user = seller.getUser();
        Shop shop = shopRepository.findBySellerId(seller.getId()).orElse(null);

        Address warehouseAddress = null;
        ShippingOption shippingOption = null;

        if (shop != null) {
            warehouseAddress = addressRepository.findByShopId(shop.getId()).orElse(null);
            shippingOption = shippingOptionRepository.findByShopId(shop.getId()).orElse(null);
        }

        AdminSellerDetailResponse.UserDetailResponse userDto = (user == null) ? null :
                new AdminSellerDetailResponse.UserDetailResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        formatFullName(user.getFirstName(), user.getLastName()),
                        user.getEmail(),
                        user.isEmailVerified(),
                        user.getPhone(),
                        user.getAvatarUrl(),
                        user.getStatus(),
                        user.getRoles() != null ? user.getRoles() : Set.of(),
                        user.getCreatedAt()
                );

        AdminSellerDetailResponse.ShopDetailResponse shopDto = (shop == null) ? null :
                new AdminSellerDetailResponse.ShopDetailResponse(
                        shop.getId(),
                        shop.getName(),
                        shop.getDescription(),
                        shop.getAvatarUrl(),
                        shop.getBannerUrl(),
                        shop.getStatus(),
                        shop.getCreatedAt(),
                        shop.getUpdatedAt()
                );

        AdminSellerDetailResponse.WarehouseAddressResponse addressDto = (warehouseAddress == null) ? null :
                new AdminSellerDetailResponse.WarehouseAddressResponse(
                        warehouseAddress.getId(),
                        warehouseAddress.getRecipientName(),
                        warehouseAddress.getPhone(),
                        warehouseAddress.getProvince(),
                        warehouseAddress.getDistrict(),
                        warehouseAddress.getWard(),
                        warehouseAddress.getStreetAddress(),
                        formatAddress(warehouseAddress),
                        warehouseAddress.getCreatedAt(),
                        warehouseAddress.getUpdatedAt()
                );

        AdminSellerDetailResponse.ShippingOptionResponse shippingDto = (shippingOption == null) ? null :
                new AdminSellerDetailResponse.ShippingOptionResponse(
                        shippingOption.getId(),
                        shippingOption.getName(),
                        shippingOption.getShippingFee(),
                        shippingOption.isActive(),
                        shippingOption.getCreatedAt(),
                        shippingOption.getUpdatedAt()
                );

        return new AdminSellerDetailResponse(
                seller.getId(),
                seller.getSellerName(),
                seller.getVerificationStatus(),
                seller.getRejectionReason(),
                seller.getBankName(),
                seller.getBankAccountNumber(),
                seller.getBankAccountHolder(),
                seller.getCreatedAt(),
                seller.getUpdatedAt(),
                userDto,
                shopDto,
                addressDto,
                shippingDto
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSellerApprovalResponse updateSellerApproval(UUID id, SellerApprovalRequest request) {
        if (request == null || request.status() == null || request.status() == SellerVerificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        Seller seller = sellerRepository.findWithUserForUpdateById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        User user = seller.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.status() == SellerVerificationStatus.APPROVED && user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Cannot approve seller {}: user {} is in status {}",
                    seller.getId(), user.getId(), user.getStatus());
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        SellerVerificationStatus currentStatus = seller.getVerificationStatus();

        if (currentStatus == SellerVerificationStatus.APPROVED && request.status() == SellerVerificationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        if (request.status() == SellerVerificationStatus.APPROVED) {
            seller.setVerificationStatus(SellerVerificationStatus.APPROVED);
            seller.setRejectionReason(null);

            user.getRoles().add(Role.SELLER);
            shop.setStatus(ShopStatus.ACTIVE);

            notificationDispatcher.dispatch(
                    user.getId(),
                    NotificationType.SHOP,
                    "Chúc mừng! Hồ sơ Người Bán đã được phê duyệt",
                    "Hồ sơ đăng ký Người Bán của bạn đã được phê duyệt thành công. Bạn có thể bắt đầu mở Shop và đăng bán sản phẩm.",
                    "SHOP",
                    shop.getId()
            );
        } else if (request.status() == SellerVerificationStatus.REJECTED) {
            if (request.rejectionReason() == null || request.rejectionReason().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.REJECTION_REASON_REQUIRED);
            }
            seller.setVerificationStatus(SellerVerificationStatus.REJECTED);
            seller.setRejectionReason(request.rejectionReason().trim());
            shop.setStatus(ShopStatus.SUSPENDED);

            notificationDispatcher.dispatch(
                    user.getId(),
                    NotificationType.SHOP,
                    "Hồ sơ Người Bán chưa được phê duyệt",
                    "Hồ sơ đăng ký Người Bán của bạn chưa được duyệt. Lý do: " + request.rejectionReason().trim(),
                    "SHOP",
                    seller.getId()
            );
        }

        sellerRepository.save(seller);
        userRepository.save(user);
        shopRepository.save(shop);

        log.info("Admin updated seller approval: sellerId={}, oldStatus={}, newStatus={}, shopStatus={}",
                seller.getId(), currentStatus, seller.getVerificationStatus(), shop.getStatus());

        return new AdminSellerApprovalResponse(
                seller.getId(),
                user.getId(),
                shop.getId(),
                seller.getSellerName(),
                shop.getName(),
                seller.getVerificationStatus(),
                shop.getStatus(),
                seller.getRejectionReason(),
                Set.copyOf(user.getRoles()),
                seller.getUpdatedAt() != null ? seller.getUpdatedAt() : LocalDateTime.now()
        );
    }

    private String formatAddress(Address addr) {
        if (addr == null) {
            return null;
        }
        return java.util.stream.Stream.of(
                addr.getStreetAddress(),
                addr.getWard(),
                addr.getDistrict(),
                addr.getProvince()
        )
        .filter(s -> s != null && !s.trim().isEmpty())
        .map(String::trim)
        .collect(java.util.stream.Collectors.joining(", "));
    }

    private String formatFullName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        } else if (!first.isEmpty()) {
            return first;
        } else {
            return last;
        }
    }
}
