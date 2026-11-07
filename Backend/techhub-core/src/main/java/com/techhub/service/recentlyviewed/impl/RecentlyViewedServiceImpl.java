package com.techhub.service.recentlyviewed.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.RecentlyViewedResponse;
import com.techhub.model.entity.Product;
import com.techhub.model.entity.RecentlyViewedProduct;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.repository.ProductImageRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.repository.ProductVariantRepository;
import com.techhub.repository.RecentlyViewedProductRepository;
import com.techhub.service.recentlyviewed.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    private final RecentlyViewedProductRepository recentlyViewedRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordProductView(UUID userId, UUID productId) {
        if (productId == null || new UUID(0L, 0L).equals(productId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        log.info("Recording product view: userId={}, productId={}", userId, productId);

        // 1. Gatekeeper kiểm tra hợp lệ của sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted() || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED) {
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

        // 2. Atomic Upsert (thêm mới hoặc cập nhật viewed_at = NOW())
        UUID recordId = UUID.randomUUID();
        recentlyViewedRepository.upsertProductView(recordId, userId, productId);

        // 3. Atomic FIFO Eviction (xóa các bản ghi vượt quá ngưỡng 10)
        recentlyViewedRepository.evictExcessRecentlyViewed(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewedResponse> getRecentlyViewedProducts(UUID userId) {
        log.info("Fetching recently viewed products for userId={}", userId);

        List<RecentlyViewedProduct> items = recentlyViewedRepository
                .findActiveRecentlyViewedByUserId(userId, PageRequest.of(0, 10));

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> productIds = items.stream()
                .map(r -> r.getProduct().getId())
                .toList();

        // Batch 1: Lấy metrics giá và tồn kho (O(1) query)
        Map<UUID, Object[]> metricsMap = productVariantRepository
                .aggregateActiveVariantMetricsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(m -> (UUID) m[0], m -> m, (a, b) -> a));

        // Batch 2: Lấy ảnh cover order 0 (O(1) query)
        Map<UUID, String> imageMap = productImageRepository
                .findCoverImagesByProductIds(productIds)
                .stream()
                .filter(img -> img[1] != null)
                .collect(Collectors.toMap(img -> (UUID) img[0], img -> (String) img[1], (a, b) -> a));

        return items.stream().map(item -> {
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

            return RecentlyViewedResponse.builder()
                    .id(item.getId())
                    .productId(pId)
                    .productName(product.getName())
                    .slug(product.getSlug())
                    .coverImageUrl(coverImageUrl)
                    .minPrice(minPrice)
                    .inStock(inStock)
                    .isShopPaused(isShopPaused)
                    .viewedAt(item.getViewedAt())
                    .build();
        }).toList();
    }
}
