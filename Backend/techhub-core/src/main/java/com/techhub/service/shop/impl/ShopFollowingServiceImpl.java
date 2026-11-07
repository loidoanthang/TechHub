package com.techhub.service.shop.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.FollowShopResponse;
import com.techhub.model.dto.response.FollowedShopResponse;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.ShopFollowing;
import com.techhub.model.entity.User;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopFollowingRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.shop.ShopFollowingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopFollowingServiceImpl implements ShopFollowingService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final ShopFollowingRepository shopFollowingRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowShopResponse followShop(UUID userId, String sellerName) {
        log.info("Processing follow shop: userId={}, sellerName='{}'", userId, sellerName);

        // 1. Validate & Normalize Slug
        if (sellerName == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        String normalizedSlug = sellerName.trim().toLowerCase();
        if (normalizedSlug.length() < 3 || normalizedSlug.length() > 50 || !normalizedSlug.matches("^[a-z0-9-]+$")) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 2. Gatekeeper: Seller verification
        Seller seller = sellerRepository.findBySellerName(normalizedSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        User sellerUser = seller.getUser();
        if (sellerUser != null && (sellerUser.getStatus() == UserStatus.BANNED || sellerUser.getStatus() == UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 3. Gatekeeper: Shop status
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 4. Anti-Self-Follow Check (BR-23)
        if (sellerUser != null && sellerUser.getId().equals(userId)) {
            log.warn("Anti-Self-Follow triggered: User {} attempted to follow own shop '{}'", userId, normalizedSlug);
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_OWN_SHOP);
        }

        // 5. Idempotent check & Save
        Optional<ShopFollowing> existingFollowing = shopFollowingRepository.findByUserIdAndShopId(userId, shop.getId());
        LocalDateTime followedAt;

        if (existingFollowing.isPresent()) {
            log.info("User {} already followed shop {} (idempotent no-op)", userId, shop.getId());
            followedAt = existingFollowing.get().getCreatedAt();
        } else {
            ShopFollowing newFollowing = ShopFollowing.builder()
                    .userId(userId)
                    .shop(shop)
                    .build();
            try {
                ShopFollowing saved = shopFollowingRepository.saveAndFlush(newFollowing);
                followedAt = saved.getCreatedAt();
                log.info("User {} successfully followed shop {}", userId, shop.getId());
            } catch (DataIntegrityViolationException ex) {
                // Xử lý race condition đồng thời
                log.warn("Concurrent follow detected for user {} and shop {}, handling idempotently", userId, shop.getId());
                ShopFollowing concurrent = shopFollowingRepository.findByUserIdAndShopId(userId, shop.getId())
                        .orElseThrow(() -> ex);
                followedAt = concurrent.getCreatedAt();
            }
        }

        // 6. Tính tổng số follower hiện tại của shop
        long followerCount = shopFollowingRepository.countByShopId(shop.getId());

        return FollowShopResponse.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .sellerName(seller.getSellerName())
                .followedAt(followedAt != null ? followedAt : LocalDateTime.now())
                .followerCount(followerCount)
                .following(true)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowShop(UUID userId, String sellerName) {
        log.info("Processing unfollow shop: userId={}, sellerName='{}'", userId, sellerName);

        // 1. Validate & Normalize Slug
        if (sellerName == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        String normalizedSlug = sellerName.trim().toLowerCase();
        if (normalizedSlug.length() < 3 || normalizedSlug.length() > 50 || !normalizedSlug.matches("^[a-z0-9-]+$")) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 2. Gatekeeper: Seller verification
        Seller seller = sellerRepository.findBySellerName(normalizedSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        User sellerUser = seller.getUser();
        if (sellerUser != null && (sellerUser.getStatus() == UserStatus.BANNED || sellerUser.getStatus() == UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 3. Gatekeeper: Shop status
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        // 4. Hard Delete (Idempotent: nếu không có bản ghi thì 0 rows deleted, không văng lỗi)
        int deletedCount = shopFollowingRepository.deleteByUserIdAndShopId(userId, shop.getId());
        log.info("User {} unfollowed shop {} (rows deleted: {})", userId, shop.getId(), deletedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowedShopResponse> getFollowedShops(UUID userId, int page, int size) {
        log.info("Fetching followed shops for userId={}, page={}, size={}", userId, page, size);

        // 1. Sanitize pagination parameters
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = (size < 1) ? 10 : Math.min(50, size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);

        // 2. Query Page of ShopFollowing
        Page<ShopFollowing> pageResult = shopFollowingRepository.findByUserIdWithShopAndSeller(userId, pageable);

        if (pageResult.isEmpty()) {
            return PageResponse.from(pageResult, Collections.emptyList());
        }

        // 3. Extract shop IDs
        List<UUID> shopIds = pageResult.getContent().stream()
                .map(sf -> sf.getShop().getId())
                .distinct()
                .toList();

        // 4. Batch count followers
        Map<UUID, Long> followerCountMap = Collections.emptyMap();
        if (!shopIds.isEmpty()) {
            List<Object[]> followerCounts = shopFollowingRepository.countFollowersByShopIdIn(shopIds);
            followerCountMap = followerCounts.stream()
                    .collect(Collectors.toMap(
                            row -> (UUID) row[0],
                            row -> ((Number) row[1]).longValue()
                    ));
        }

        // 5. Batch count active products
        Map<UUID, Long> productCountMap = Collections.emptyMap();
        if (!shopIds.isEmpty()) {
            List<Object[]> productCounts = productRepository.countActiveProductsByShopIdIn(shopIds);
            productCountMap = productCounts.stream()
                    .collect(Collectors.toMap(
                            row -> (UUID) row[0],
                            row -> ((Number) row[1]).longValue()
                    ));
        }

        // 6. Map to DTOs
        Map<UUID, Long> finalFollowerMap = followerCountMap;
        Map<UUID, Long> finalProductMap = productCountMap;

        List<FollowedShopResponse> dtoList = pageResult.getContent().stream()
                .map(sf -> {
                    Shop shop = sf.getShop();
                    Seller seller = (shop != null) ? shop.getSeller() : null;
                    String sellerName = (seller != null) ? seller.getSellerName() : null;
                    ShopStatus status = (shop != null) ? shop.getStatus() : null;
                    boolean isPaused = (status == ShopStatus.PAUSED);
                    UUID shopId = (shop != null) ? shop.getId() : null;
                    long followers = (shopId != null) ? finalFollowerMap.getOrDefault(shopId, 0L) : 0L;
                    long products = (shopId != null) ? finalProductMap.getOrDefault(shopId, 0L) : 0L;

                    return FollowedShopResponse.builder()
                            .shopId(shopId)
                            .sellerName(sellerName)
                            .shopName(shop != null ? shop.getName() : null)
                            .avatarUrl(shop != null ? shop.getAvatarUrl() : null)
                            .bannerUrl(shop != null ? shop.getBannerUrl() : null)
                            .status(status)
                            .isPaused(isPaused)
                            .followerCount(followers)
                            .totalProducts(products)
                            .followedAt(sf.getCreatedAt())
                            .build();
                })
                .toList();

        return PageResponse.from(pageResult, dtoList);
    }
}
