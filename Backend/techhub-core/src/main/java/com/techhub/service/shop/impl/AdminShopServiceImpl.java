package com.techhub.service.shop.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.AdminUpdateShopStatusRequest;
import com.techhub.model.dto.response.AdminShopDetailResponse;
import com.techhub.model.dto.response.AdminShopResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.ShippingOption;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.OrderRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ShippingOptionRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.security.SecurityUtils;
import com.techhub.service.shop.AdminShopService;
import com.techhub.specification.ShopSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminShopServiceImpl implements AdminShopService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "name", "status", "updatedAt"
    );

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final ShippingOptionRepository shippingOptionRepository;
    private final OrderRepository orderRepository;

    @Override
    public PageResponse<AdminShopResponse> getShops(
            String keyword,
            ShopStatus status,
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

        Specification<Shop> spec = ShopSpecification.filterShops(keyword, status);
        Page<Shop> shopPage = shopRepository.findAll(spec, pageable);

        if (shopPage.isEmpty()) {
            return PageResponse.from(shopPage, List.of());
        }

        List<UUID> shopIds = shopPage.getContent().stream()
                .map(Shop::getId)
                .toList();

        Map<UUID, Long> productCountMap = getProductCountMap(shopIds);

        List<AdminShopResponse> content = shopPage.getContent().stream()
                .map(shop -> mapToResponse(shop, productCountMap.getOrDefault(shop.getId(), 0L)))
                .toList();

        log.debug("Retrieved {} shops for admin query (page: {}, size: {}, totalElements: {})",
                content.size(), sanitizedPage, sanitizedSize, shopPage.getTotalElements());

        return PageResponse.from(shopPage, content);
    }

    @Override
    public AdminShopDetailResponse getShopDetail(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Shop shop = shopRepository.findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        log.info("Admin viewing shop details: shopId={}, shopName={}, status={}",
                shop.getId(), shop.getName(), shop.getStatus());

        Seller seller = shop.getSeller();
        User user = seller != null ? seller.getUser() : null;

        Address warehouseAddress = addressRepository.findByShopId(shop.getId()).orElse(null);
        ShippingOption shippingOption = shippingOptionRepository.findByShopId(shop.getId()).orElse(null);

        long totalProducts = productRepository.countByShopIdAndIsDeletedFalse(shop.getId());
        long activeProducts = productRepository.countActiveProductsByShopId(shop.getId());
        long completedOrders = orderRepository.countCompletedOrdersByShopId(shop.getId());
        BigDecimal cumulativeRevenue = orderRepository.calculateCumulativeRevenueByShopId(shop.getId());

        AdminShopDetailResponse.ShopCommercialResponse shopDto = new AdminShopDetailResponse.ShopCommercialResponse(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getAvatarUrl(),
                shop.getBannerUrl(),
                shop.getStatus(),
                shop.getCreatedAt(),
                shop.getUpdatedAt()
        );

        AdminShopDetailResponse.SellerProfileResponse sellerDto = (seller == null) ? null :
                new AdminShopDetailResponse.SellerProfileResponse(
                        seller.getId(),
                        seller.getSellerName(),
                        seller.getVerificationStatus(),
                        seller.getRejectionReason(),
                        seller.getBankName(),
                        seller.getBankAccountNumber(),
                        seller.getBankAccountHolder(),
                        seller.getBalance() != null ? seller.getBalance() : BigDecimal.ZERO,
                        seller.getCreatedAt(),
                        seller.getUpdatedAt()
                );

        AdminShopDetailResponse.OwnerAccountResponse ownerDto = (user == null) ? null :
                new AdminShopDetailResponse.OwnerAccountResponse(
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

        AdminShopDetailResponse.WarehouseAddressResponse addressDto = (warehouseAddress == null) ? null :
                new AdminShopDetailResponse.WarehouseAddressResponse(
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

        AdminShopDetailResponse.ShippingOptionResponse shippingDto = (shippingOption == null) ? null :
                new AdminShopDetailResponse.ShippingOptionResponse(
                        shippingOption.getId(),
                        shippingOption.getName(),
                        shippingOption.getShippingFee(),
                        shippingOption.isActive(),
                        shippingOption.getCreatedAt(),
                        shippingOption.getUpdatedAt()
                );

        AdminShopDetailResponse.ShopMetricsResponse metricsDto = new AdminShopDetailResponse.ShopMetricsResponse(
                totalProducts,
                activeProducts,
                completedOrders,
                cumulativeRevenue != null ? cumulativeRevenue : BigDecimal.ZERO
        );

        return new AdminShopDetailResponse(
                shopDto,
                sellerDto,
                ownerDto,
                addressDto,
                shippingDto,
                metricsDto
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminShopResponse updateShopStatus(UUID id, AdminUpdateShopStatusRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        if (request == null || request.status() == null) {
            throw new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION);
        }

        Shop shop = shopRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        ShopStatus oldStatus = shop.getStatus();
        ShopStatus targetStatus = request.status();

        // 1. Idempotency: If target status matches current status, return without saving
        if (oldStatus == targetStatus) {
            log.info("Admin requested identical status for shopId={}: current status is already {}",
                    shop.getId(), oldStatus);
            long productCount = productRepository.countByShopIdAndIsDeletedFalse(shop.getId());
            return mapToResponse(shop, productCount);
        }

        // 2. BR-07: BANNED is a permanent terminal state
        if (oldStatus == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION);
        }

        // 3. BR-07.B: To activate a shop, seller profile must be APPROVED
        Seller seller = shop.getSeller();
        if (targetStatus == ShopStatus.ACTIVE) {
            if (seller == null || seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
                throw new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION);
            }
        }

        shop.setStatus(targetStatus);
        Shop savedShop = shopRepository.save(shop);

        long productCount = productRepository.countByShopIdAndIsDeletedFalse(savedShop.getId());

        UUID adminId = SecurityUtils.getCurrentUserIdOptional().orElse(null);
        log.info("Admin {} updated shop status: shopId={}, oldStatus={}, newStatus={}, reason={}",
                adminId, savedShop.getId(), oldStatus, targetStatus, request.reason());

        return mapToResponse(savedShop, productCount);
    }

    private Map<UUID, Long> getProductCountMap(List<UUID> shopIds) {
        if (shopIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> results = productRepository.countTotalProductsByShopIdIn(shopIds);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (UUID) r[0],
                        r -> r[1] != null ? ((Number) r[1]).longValue() : 0L,
                        (existing, replacement) -> existing
                ));
    }

    private AdminShopResponse mapToResponse(Shop shop, Long productCount) {
        Seller seller = shop.getSeller();
        User user = seller != null ? seller.getUser() : null;

        String ownerName = formatFullName(
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null
        );

        return new AdminShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getAvatarUrl(),
                shop.getBannerUrl(),
                shop.getStatus(),
                seller != null ? seller.getId() : null,
                seller != null ? seller.getSellerName() : null,
                seller != null ? seller.getVerificationStatus() : null,
                user != null ? user.getId() : null,
                ownerName,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getStatus() : null,
                seller != null && seller.getBalance() != null ? seller.getBalance() : BigDecimal.ZERO,
                productCount != null ? productCount : 0L,
                shop.getCreatedAt(),
                shop.getUpdatedAt()
        );
    }

    private String formatFullName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        } else if (!first.isEmpty()) {
            return first;
        } else {
            return last.isEmpty() ? null : last;
        }
    }

    private String formatAddress(Address address) {
        if (address == null) return null;
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(address.getStreetAddress())) parts.add(address.getStreetAddress().trim());
        if (StringUtils.hasText(address.getWard())) parts.add(address.getWard().trim());
        if (StringUtils.hasText(address.getDistrict())) parts.add(address.getDistrict().trim());
        if (StringUtils.hasText(address.getProvince())) parts.add(address.getProvince().trim());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
