package com.techhub.service.wishlist.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.WishlistResponse;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.WishlistItem;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.repository.WishlistRepository;
import com.techhub.service.wishlist.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private static final long MAX_WISHLIST_ITEMS = 20;

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WishlistResponse addToWishlist(UUID userId, UUID productId) {
        log.info("Processing add to wishlist: userId={}, productId={}", userId, productId);

        // 1. Kiểm tra tồn tại sản phẩm và 4-Tier Gatekeeper
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
        }

        if (product.isSuspended()) {
            throw new BusinessException(ErrorCode.PRODUCT_SUSPENDED);
        }

        if (product.getShop() != null && product.getShop().getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // 2. Kiểm tra tính lũy thừa (Idempotency Check)
        Optional<WishlistItem> existingItem = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existingItem.isPresent()) {
            log.info("Product already in wishlist, returning existing item: userId={}, productId={}", userId, productId);
            return mapToWishlistResponse(existingItem.get(), product);
        }

        // 3. Kiểm tra giới hạn 20 món (BR-05B)
        long currentCount = wishlistRepository.countByUserId(userId);
        if (currentCount >= MAX_WISHLIST_ITEMS) {
            log.warn("Wishlist limit exceeded: userId={}, currentCount={}", userId, currentCount);
            throw new BusinessException(ErrorCode.WISHLIST_LIMIT_EXCEEDED);
        }

        // 4. Lưu bản ghi WishlistItem vào DB
        WishlistItem wishlistItem = WishlistItem.builder()
                .userId(userId)
                .product(product)
                .build();

        try {
            wishlistItem = wishlistRepository.save(wishlistItem);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition detected when adding to wishlist for userId={}, productId={}", userId, productId);
            wishlistItem = wishlistRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> ex);
        }

        return mapToWishlistResponse(wishlistItem, product);
    }

    private WishlistResponse mapToWishlistResponse(WishlistItem item, Product product) {
        UUID productId = product.getId();

        // Nạp minPrice và inStock qua aggregation query tối ưu (tránh N+1)
        BigDecimal minPrice = BigDecimal.ZERO;
        boolean inStock = false;

        List<Object[]> variantMetrics = productVariantRepository.aggregateActiveVariantMetricsByProductIds(List.of(productId));
        if (!variantMetrics.isEmpty()) {
            Object[] metric = variantMetrics.get(0);
            if (metric[2] != null) {
                minPrice = (BigDecimal) metric[2];
            }
            if (metric[4] != null) {
                Number sumStock = (Number) metric[4];
                inStock = sumStock.longValue() > 0;
            }
        }

        // Nạp ảnh cover (displayOrder = 0)
        String coverImageUrl = null;
        List<Object[]> coverImages = productImageRepository.findCoverImagesByProductIds(List.of(productId));
        if (!coverImages.isEmpty() && coverImages.get(0)[1] != null) {
            coverImageUrl = (String) coverImages.get(0)[1];
        }

        boolean isShopPaused = product.getShop() != null && product.getShop().getStatus() == ShopStatus.PAUSED;

        return WishlistResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .slug(product.getSlug())
                .coverImageUrl(coverImageUrl)
                .minPrice(minPrice)
                .inStock(inStock)
                .isShopPaused(isShopPaused)
                .createdAt(item.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromWishlist(UUID userId, UUID productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }
        log.info("Removing product from wishlist: userId={}, productId={}", userId, productId);
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> getWishlist(UUID userId, int page, int size) {
        log.info("Fetching wishlist for userId={}, page={}, size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<WishlistItem> itemPage = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        if (itemPage.isEmpty()) {
            return PageResponse.from(itemPage, Collections.emptyList());
        }

        List<UUID> productIds = itemPage.getContent().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        // Batch 1: Lấy metrics giá và tồn kho (O(1) query)
        Map<UUID, Object[]> metricsMap = productVariantRepository
                .aggregateActiveVariantMetricsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(m -> (UUID) m[0], m -> m, (a, b) -> a));

        // Batch 2: Lấy ảnh đại diện order 0 (O(1) query)
        Map<UUID, String> imageMap = productImageRepository
                .findCoverImagesByProductIds(productIds)
                .stream()
                .filter(img -> img[1] != null)
                .collect(Collectors.toMap(img -> (UUID) img[0], img -> (String) img[1], (a, b) -> a));

        List<WishlistResponse> content = itemPage.getContent().stream().map(item -> {
            Product product = item.getProduct();
            UUID pId = product.getId();

            BigDecimal minPrice = BigDecimal.ZERO;
            boolean inStock = false;
            if (metricsMap.containsKey(pId)) {
                Object[] metric = metricsMap.get(pId);
                if (metric[2] != null) {
                    minPrice = (BigDecimal) metric[2];
                }
                if (metric[4] != null) {
                    Number sumStock = (Number) metric[4];
                    inStock = sumStock.longValue() > 0;
                }
            }

            String coverImageUrl = imageMap.get(pId);
            boolean isShopPaused = product.getShop() != null && product.getShop().getStatus() == ShopStatus.PAUSED;

            return WishlistResponse.builder()
                    .id(item.getId())
                    .productId(pId)
                    .productName(product.getName())
                    .slug(product.getSlug())
                    .coverImageUrl(coverImageUrl)
                    .minPrice(minPrice)
                    .inStock(inStock)
                    .isShopPaused(isShopPaused)
                    .createdAt(item.getCreatedAt())
                    .build();
        }).toList();

        return PageResponse.from(itemPage, content);
    }
}
