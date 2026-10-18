package com.techhub.service.cart.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CartCountResponse;
import com.techhub.model.dto.response.CartItemResponse;
import com.techhub.model.dto.response.CartResponse;
import com.techhub.model.dto.response.ShopCartGroupResponse;
import com.techhub.model.entity.*;
import com.techhub.model.dto.request.AddToCartRequest;
import com.techhub.model.dto.request.CartPreviewRequest;
import com.techhub.model.dto.request.UpdateCartItemRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRepository voucherRepository;
    private final ShippingOptionRepository shippingOptionRepository;

    private static final UUID UNKNOWN_SHOP_ID = new UUID(0L, 0L);

    @Override
    @Transactional
    public CartResponse getCartDetails(UUID userId) {
        log.info("Fetching cart details for user: {}", userId);

        // 1. Get or create Cart (Lazy Initialization - BR-12)
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing new cart for user: {}", userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });

        // 2. Fetch all cart items with deep relations in 1 single query via @EntityGraph
        List<CartItem> items = cartItemRepository.findByCartIdWithDetails(cart.getId());
        if (items.isEmpty()) {
            log.info("Cart is empty for user: {}", userId);
            return new CartResponse(cart.getId(), 0, 0, Collections.emptyList());
        }

        // 3. Batch fetch fallback cover images for variants lacking own imageUrl
        Set<UUID> missingImageProductIds = items.stream()
                .filter(item -> item.getProduct() != null && item.getProductVariant() != null &&
                        (item.getProductVariant().getImageUrl() == null || item.getProductVariant().getImageUrl().isBlank()))
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        Map<UUID, String> fallbackCoverMap = new HashMap<>();
        if (!missingImageProductIds.isEmpty()) {
            List<ProductImage> coverImages = productImageRepository.findByProductIdInAndDisplayOrder(missingImageProductIds, 0);
            for (ProductImage pi : coverImages) {
                fallbackCoverMap.put(pi.getProduct().getId(), pi.getImageUrl());
            }
        }

        // 4. Map each CartItem and evaluate Live Price, Stock & 5-Tier Gatekeeper status
        Map<UUID, List<CartItemResponse>> shopItemsMap = new LinkedHashMap<>();
        Map<UUID, Shop> shopEntityMap = new LinkedHashMap<>();

        for (CartItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getProductVariant();
            Shop shop = (product != null) ? product.getShop() : null;
            Seller seller = (shop != null) ? shop.getSeller() : null;
            User sellerUser = (seller != null) ? seller.getUser() : null;
            Category category = (product != null) ? product.getCategory() : null;

            // Monetary calculations (Scale 2, HALF_UP)
            BigDecimal price = (variant != null && variant.getPrice() != null)
                    ? variant.getPrice().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            int quantity = item.getQuantity();
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

            int stock = (variant != null) ? variant.getStockQuantity() : 0;
            boolean isOutOfStock = (stock == 0);

            // Gatekeeper 5-Tier & Live Stock Evaluation
            boolean isAvailable = true;
            String unavailableReason = null;

            if (variant == null || variant.isDeleted() || variant.getStatus() == ProductVariantStatus.INACTIVE) {
                isAvailable = false;
                unavailableReason = "VARIANT_UNAVAILABLE";
            } else if (product == null || product.isDeleted() || product.isSuspended()
                    || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED
                    || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                isAvailable = false;
                unavailableReason = "PRODUCT_UNAVAILABLE";
            } else if (category == null || category.getStatus() == CategoryStatus.INACTIVE) {
                isAvailable = false;
                unavailableReason = "CATEGORY_INACTIVE";
            } else if (shop == null || shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED
                    || (sellerUser != null && (sellerUser.getStatus() == UserStatus.BANNED || sellerUser.getStatus() == UserStatus.DELETED))) {
                isAvailable = false;
                unavailableReason = "SHOP_NOT_ACTIVE";
            } else if (shop.getStatus() == ShopStatus.PAUSED) {
                isAvailable = false;
                unavailableReason = "SHOP_IS_PAUSED";
            } else if (price.compareTo(BigDecimal.ZERO) <= 0) {
                isAvailable = false;
                unavailableReason = "INVALID_PRICE";
            } else if (stock == 0) {
                isAvailable = false;
                unavailableReason = "OUT_OF_STOCK";
            } else if (quantity > stock) {
                isAvailable = false;
                unavailableReason = "EXCEEDS_AVAILABLE_STOCK";
            }

            // Image URL resolution: Prefer variant imageUrl, fallback to product cover image
            String imageUrl = (variant != null && variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                    ? variant.getImageUrl()
                    : ((product != null) ? fallbackCoverMap.get(product.getId()) : null);

            CartItemResponse itemResponse = new CartItemResponse(
                    item.getId(),
                    (product != null) ? product.getId() : null,
                    (product != null) ? product.getName() : "Unknown Product",
                    (product != null) ? product.getSlug() : null,
                    (variant != null) ? variant.getId() : null,
                    (variant != null) ? variant.getSku() : null,
                    (variant != null) ? variant.getVariantName() : null,
                    (variant != null) ? variant.getAttributes() : null,
                    price,
                    quantity,
                    itemSubtotal,
                    imageUrl,
                    stock,
                    isOutOfStock,
                    isAvailable,
                    unavailableReason,
                    item.getCreatedAt()
            );

            UUID shopId = (shop != null) ? shop.getId() : UNKNOWN_SHOP_ID;
            shopItemsMap.computeIfAbsent(shopId, k -> new ArrayList<>()).add(itemResponse);
            if (shop != null) {
                shopEntityMap.putIfAbsent(shopId, shop);
            }
        }

        // 5. In-Memory Grouping by Shop (BR-13)
        List<ShopCartGroupResponse> shopGroups = new ArrayList<>();
        for (Map.Entry<UUID, List<CartItemResponse>> entry : shopItemsMap.entrySet()) {
            UUID shopId = entry.getKey();
            Shop shop = shopEntityMap.get(shopId);
            boolean isShopPaused = (shop != null && shop.getStatus() == ShopStatus.PAUSED);
            String shopName = (shop != null) ? shop.getName() : "Unknown Shop";
            String sellerName = (shop != null && shop.getSeller() != null) ? shop.getSeller().getSellerName() : null;
            ShopStatus shopStatus = (shop != null) ? shop.getStatus() : ShopStatus.SUSPENDED;

            shopGroups.add(new ShopCartGroupResponse(
                    shopId,
                    shopName,
                    sellerName,
                    shopStatus,
                    isShopPaused,
                    entry.getValue()
            ));
        }

        // 6. Aggregate Overall Cart Metrics
        int totalItemTypes = items.size();
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        log.info("Cart details retrieved successfully for user: {} (totalItemTypes: {}, totalQuantity: {})",
                userId, totalItemTypes, totalQuantity);

        return new CartResponse(cart.getId(), totalItemTypes, totalQuantity, shopGroups);
    }

    @Override
    @Transactional(readOnly = true)
    public CartCountResponse getCartCount(UUID userId) {
        log.debug("Fetching cart count for user: {}", userId);

        if (userId == null) {
            return CartCountResponse.empty();
        }

        int totalItemTypes = cartItemRepository.countByUserId(userId);
        log.debug("Cart count retrieved successfully for user: {} (totalItemTypes: {})", userId, totalItemTypes);
        return new CartCountResponse(totalItemTypes);
    }

    @Override
    @Transactional
    public CartItemResponse addToCart(UUID userId, AddToCartRequest request) {
        log.info("Processing add to cart for user: {}, variant: {}, quantity: {}",
                userId, request.productVariantId(), request.quantity());

        // 1. Get or create Cart with Pessimistic Lock to serialize concurrent requests per user (BR-12)
        Cart cart = cartRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing new cart for user: {}", userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    try {
                        return cartRepository.save(newCart);
                    } catch (DataIntegrityViolationException ex) {
                        return cartRepository.findByUserIdWithLock(userId)
                                .orElseThrow(() -> ex);
                    }
                });

        // 2. Fetch Variant with ancestor details in 1 single query via @EntityGraph
        ProductVariant variant = productVariantRepository.findByIdWithDetails(request.productVariantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        // 3. Gatekeeper 5-Tier Checks & Anti-Self-Buying (BR-11A, BR-11B, BR-07)
        if (variant.getStatus() != ProductVariantStatus.ACTIVE) {
            log.warn("Cannot add inactive variant {} to cart", variant.getId());
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND);
        }

        Product product = variant.getProduct();
        if (product == null || product.isDeleted() || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED) {
            log.warn("Cannot add deleted or unlisted product to cart: {}", product != null ? product.getId() : null);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            log.warn("Cannot add unapproved product to cart: {}", product.getId());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
        }

        if (product.isSuspended()) {
            log.warn("Cannot add suspended product to cart: {}", product.getId());
            throw new BusinessException(ErrorCode.PRODUCT_SUSPENDED);
        }

        Shop shop = product.getShop();
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Seller seller = shop.getSeller();
        if (seller == null || seller.getUser() == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }

        // Anti-Self-Buying: Shop owner cannot add their own products (BR-11B)
        if (userId.equals(seller.getUser().getId())) {
            log.warn("User {} attempted to add product {} from their own shop {}", userId, product.getId(), shop.getId());
            throw new BusinessException(ErrorCode.CANNOT_ADD_OWN_PRODUCT);
        }

        // Shop status checks (BR-07)
        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Cannot add product from paused shop: {}", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }

        if (shop.getStatus() != ShopStatus.ACTIVE || seller.getUser().getStatus() != UserStatus.ACTIVE) {
            log.warn("Cannot add product from inactive shop {} or banned seller {}", shop.getId(), seller.getUser().getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // Category status check (BR-11A)
        Category category = product.getCategory();
        if (category == null || category.getStatus() != CategoryStatus.ACTIVE) {
            log.warn("Cannot add product from inactive category: {}", category != null ? category.getId() : null);
            throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        }

        // 4. Existing Item Check & Cart Capacity Check (BR-11B & BR-12)
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId());
        CartItem itemToSave;
        int targetQuantity;

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            targetQuantity = existingItem.getQuantity() + request.quantity();
            if (targetQuantity > 999) {
                log.warn("Accumulated quantity {} exceeds maximum limit 999", targetQuantity);
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
            if (targetQuantity > variant.getStockQuantity()) {
                log.warn("Accumulated quantity {} exceeds available stock {}", targetQuantity, variant.getStockQuantity());
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            existingItem.setQuantity(targetQuantity);
            itemToSave = existingItem;
        } else {
            // New SKU: Check Cart capacity (max 50 distinct items)
            int currentItemTypesCount = cartItemRepository.countByCartId(cart.getId());
            if (currentItemTypesCount >= 50) {
                log.warn("Cart {} capacity reached limit (50 items)", cart.getId());
                throw new BusinessException(ErrorCode.CART_LIMIT_EXCEEDED);
            }
            targetQuantity = request.quantity();
            if (targetQuantity > variant.getStockQuantity()) {
                log.warn("Requested quantity {} exceeds available stock {}", targetQuantity, variant.getStockQuantity());
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            itemToSave = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productVariant(variant)
                    .quantity(targetQuantity)
                    .build();
        }

        // 5. Persist with Double-Click Race Condition Handling
        try {
            itemToSave = cartItemRepository.saveAndFlush(itemToSave);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate insert detected for cart {} and variant {}. Merging quantities.",
                    cart.getId(), variant.getId());
            CartItem reloadedItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                    .orElseThrow(() -> ex);
            int mergedQty = reloadedItem.getQuantity() + request.quantity();
            if (mergedQty > 999) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
            if (mergedQty > variant.getStockQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            reloadedItem.setQuantity(mergedQty);
            itemToSave = cartItemRepository.saveAndFlush(reloadedItem);
        }

        // 6. Build and return CartItemResponse
        return buildCartItemResponse(itemToSave, product, variant);
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item quantity for user: {}, itemId: {}, newQuantity: {}",
                userId, itemId, request.quantity());

        // 1. Fetch CartItem with Pessimistic Lock & Full Graph (Zero IDOR + 1 Query)
        CartItem item = cartItemRepository.findByIdAndUserIdWithDetailsForUpdate(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        ProductVariant variant = item.getProductVariant();
        if (variant == null || variant.isDeleted() || variant.getStatus() != ProductVariantStatus.ACTIVE) {
            log.warn("Cannot update cart item with inactive/deleted variant: {}", variant != null ? variant.getId() : null);
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND);
        }

        Product product = item.getProduct();
        if (product == null || product.isDeleted() || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED) {
            log.warn("Cannot update cart item with unlisted/deleted product: {}", product != null ? product.getId() : null);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            log.warn("Cannot update cart item with unapproved product: {}", product.getId());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
        }

        if (product.isSuspended()) {
            log.warn("Cannot update cart item with suspended product: {}", product.getId());
            throw new BusinessException(ErrorCode.PRODUCT_SUSPENDED);
        }

        Shop shop = product.getShop();
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Seller seller = shop.getSeller();
        if (seller == null || seller.getUser() == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }

        if (shop.getStatus() == ShopStatus.PAUSED) {
            log.warn("Cannot update cart item from paused shop: {}", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
        }

        if (shop.getStatus() != ShopStatus.ACTIVE || seller.getUser().getStatus() != UserStatus.ACTIVE) {
            log.warn("Cannot update cart item from inactive shop: {}", shop.getId());
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        Category category = product.getCategory();
        if (category == null || category.getStatus() != CategoryStatus.ACTIVE) {
            log.warn("Cannot update cart item with inactive category: {}", category != null ? category.getId() : null);
            throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        }

        // 2. Validate Live Stock (BR-11C)
        if (request.quantity() > variant.getStockQuantity()) {
            log.warn("Requested quantity {} exceeds available stock {}", request.quantity(), variant.getStockQuantity());
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        // 3. Update quantity & persist
        item.setQuantity(request.quantity());
        CartItem savedItem = cartItemRepository.save(item);

        // 4. Build and return updated CartItemResponse (Synchronizing Live Price & Subtotal)
        return buildCartItemResponse(savedItem, product, variant);
    }

    @Override
    @Transactional
    public void deleteCartItem(UUID userId, UUID itemId) {
        log.info("Removing cart item {} for user {}", itemId, userId);

        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(item);

        log.info("Successfully removed cart item {} for user {}", itemId, userId);
    }

    private CartItemResponse buildCartItemResponse(CartItem item, Product product, ProductVariant variant) {
        String resolvedImageUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                ? variant.getImageUrl()
                : null;
        if (resolvedImageUrl == null && product != null) {
            List<ProductImage> coverImages = productImageRepository.findByProductIdInAndDisplayOrder(
                    Collections.singleton(product.getId()), 0);
            if (!coverImages.isEmpty()) {
                resolvedImageUrl = coverImages.get(0).getImageUrl();
            }
        }

        BigDecimal price = (variant.getPrice() != null)
                ? variant.getPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                variant.getId(),
                variant.getSku(),
                variant.getVariantName(),
                variant.getAttributes(),
                price,
                item.getQuantity(),
                itemSubtotal,
                resolvedImageUrl,
                variant.getStockQuantity(),
                variant.getStockQuantity() == 0,
                true,
                null,
                item.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CartPreviewResponse previewCart(UUID userId, CartPreviewRequest request) {
        log.info("Processing cart preview for user: {}", userId);

        if (request == null || request.selectedCartItemIds() == null || request.selectedCartItemIds().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        List<UUID> uniqueItemIds = request.selectedCartItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }
        if (uniqueItemIds.size() > 50) {
            throw new BusinessException(ErrorCode.CART_LIMIT_EXCEEDED);
        }

        // 1. Fetch CartItems with deep details via @EntityGraph (Zero N+1 Query)
        List<CartItem> items = cartItemRepository.findByUserIdAndIdInWithDetails(userId, uniqueItemIds);
        if (items.size() != uniqueItemIds.size()) {
            log.warn("Cart item mismatch for user {}: requested {}, found {}", userId, uniqueItemIds.size(), items.size());
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 2. Validate live status & stock for all selected items (BR-11C, BR-12B)
        for (CartItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getProductVariant();
            Shop shop = (product != null) ? product.getShop() : null;

            if (shop == null || shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
            }
            if (shop.getStatus() == ShopStatus.PAUSED) {
                throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
            }
            if (product == null || product.isDeleted() || product.isSuspended()
                    || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED
                    || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
            }
            if (variant == null || variant.isDeleted() || variant.getStatus() == ProductVariantStatus.INACTIVE) {
                throw new BusinessException(ErrorCode.NO_ACTIVE_VARIANT);
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 3. Fallback image resolution
        Set<UUID> missingImageProductIds = items.stream()
                .filter(item -> item.getProduct() != null && item.getProductVariant() != null &&
                        (item.getProductVariant().getImageUrl() == null || item.getProductVariant().getImageUrl().isBlank()))
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        Map<UUID, String> fallbackCoverMap = new HashMap<>();
        if (!missingImageProductIds.isEmpty()) {
            List<ProductImage> coverImages = productImageRepository.findByProductIdInAndDisplayOrder(missingImageProductIds, 0);
            for (ProductImage pi : coverImages) {
                fallbackCoverMap.put(pi.getProduct().getId(), pi.getImageUrl());
            }
        }

        List<String> warnings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 4. Group items by Shop (preserving order)
        Map<UUID, List<CartItem>> itemsByShopId = new LinkedHashMap<>();
        Map<UUID, Shop> shopMap = new LinkedHashMap<>();
        for (CartItem item : items) {
            UUID sId = item.getProduct().getShop().getId();
            itemsByShopId.computeIfAbsent(sId, k -> new ArrayList<>()).add(item);
            shopMap.putIfAbsent(sId, item.getProduct().getShop());
        }

        // 5. Fetch Shipping Options for all involved shops in batch (Zero N+1)
        Map<UUID, BigDecimal> shopShippingFeeMap = new HashMap<>();
        List<ShippingOption> shippingOptions = shippingOptionRepository.findByShopIdIn(shopMap.keySet());
        for (ShippingOption so : shippingOptions) {
            if (so.getShop() != null && so.isActive() && so.getShippingFee() != null) {
                shopShippingFeeMap.put(so.getShop().getId(), so.getShippingFee().setScale(2, RoundingMode.HALF_UP));
            }
        }

        // 6. Fetch & Categorize Shop Vouchers (PRODUCT vs SHIPPING_FEE)
        Map<UUID, Voucher> shopProductVoucherMap = new HashMap<>();
        Map<UUID, Voucher> shopShippingVoucherMap = new HashMap<>();
        if (request.shopVoucherIds() != null && !request.shopVoucherIds().isEmpty()) {
            List<UUID> uniqueShopVoucherIds = request.shopVoucherIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!uniqueShopVoucherIds.isEmpty()) {
                List<Voucher> shopVouchers = voucherRepository.findAllWithShopByIdIn(uniqueShopVoucherIds);
                for (Voucher sv : shopVouchers) {
                    if (sv.getType() == VoucherType.SHOP && sv.getShop() != null) {
                        if (sv.getAppliesTo() == VoucherAppliesTo.PRODUCT) {
                            if (shopProductVoucherMap.containsKey(sv.getShop().getId())) {
                                warnings.add("Shop " + sv.getShop().getName() + " chỉ áp dụng tối đa 1 voucher sản phẩm. Đã bỏ qua mã " + sv.getCode());
                            } else {
                                shopProductVoucherMap.put(sv.getShop().getId(), sv);
                            }
                        } else if (sv.getAppliesTo() == VoucherAppliesTo.SHIPPING_FEE) {
                            if (shopShippingVoucherMap.containsKey(sv.getShop().getId())) {
                                warnings.add("Shop " + sv.getShop().getName() + " chỉ áp dụng tối đa 1 voucher vận chuyển. Đã bỏ qua mã " + sv.getCode());
                            } else {
                                shopShippingVoucherMap.put(sv.getShop().getId(), sv);
                            }
                        }
                    }
                }
            }
        }

        // 7. First pass on Shops: Calculate shopSubtotal, shippingFee, and Shop Vouchers (Product & Shipping)
        BigDecimal totalProductAmount = BigDecimal.ZERO;
        BigDecimal totalShippingFee = BigDecimal.ZERO;
        BigDecimal totalShopProductDiscount = BigDecimal.ZERO;
        BigDecimal totalShopShippingDiscount = BigDecimal.ZERO;

        Map<UUID, BigDecimal> shopSubtotalMap = new LinkedHashMap<>();
        Map<UUID, BigDecimal> shopProductDiscountMap = new LinkedHashMap<>();
        Map<UUID, BigDecimal> shopBaseProductAmountMap = new LinkedHashMap<>();
        Map<UUID, BigDecimal> shopShippingDiscountMap = new LinkedHashMap<>();
        Map<UUID, BigDecimal> shopBaseShippingFeeMap = new LinkedHashMap<>();
        Map<UUID, List<AppliedVoucherPreviewResponse>> shopAppliedVouchersMap = new LinkedHashMap<>();
        Map<UUID, List<CartItemPreviewResponse>> rawItemPreviewsMap = new LinkedHashMap<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByShopId.entrySet()) {
            UUID shopId = entry.getKey();
            Shop shop = shopMap.get(shopId);
            List<CartItem> shopItems = entry.getValue();

            BigDecimal shopSubtotal = BigDecimal.ZERO;
            List<CartItemPreviewResponse> rawItems = new ArrayList<>();

            for (CartItem item : shopItems) {
                Product product = item.getProduct();
                ProductVariant variant = item.getProductVariant();

                BigDecimal livePrice = (variant.getPrice() != null)
                        ? variant.getPrice().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                int quantity = item.getQuantity();
                BigDecimal originalSubtotal = livePrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                shopSubtotal = shopSubtotal.add(originalSubtotal);

                String imageUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                        ? variant.getImageUrl()
                        : fallbackCoverMap.get(product.getId());

                rawItems.add(new CartItemPreviewResponse(
                        item.getId(),
                        variant.getId(),
                        product.getName(),
                        variant.getVariantName(),
                        imageUrl,
                        livePrice,
                        quantity,
                        originalSubtotal,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        livePrice,
                        originalSubtotal
                ));
            }

            shopSubtotalMap.put(shopId, shopSubtotal);
            rawItemPreviewsMap.put(shopId, rawItems);
            totalProductAmount = totalProductAmount.add(shopSubtotal);

            BigDecimal shopShippingFee = shopShippingFeeMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            totalShippingFee = totalShippingFee.add(shopShippingFee);

            List<AppliedVoucherPreviewResponse> appliedVouchers = new ArrayList<>();

            // 7a. Evaluate Shop Product Voucher
            BigDecimal shopProductDiscount = BigDecimal.ZERO;
            Voucher shopProductVoucher = shopProductVoucherMap.get(shopId);
            if (shopProductVoucher != null) {
                boolean isTimeValid = (shopProductVoucher.getStartDate() == null || !shopProductVoucher.getStartDate().isAfter(now))
                        && (shopProductVoucher.getEndDate() == null || !shopProductVoucher.getEndDate().isBefore(now));
                boolean isUsageValid = shopProductVoucher.getUsageLimit() == null
                        || ((shopProductVoucher.getUsedCount() != null ? shopProductVoucher.getUsedCount() : 0)
                        + (shopProductVoucher.getReservedCount() != null ? shopProductVoucher.getReservedCount() : 0)) < shopProductVoucher.getUsageLimit();
                boolean isActive = shopProductVoucher.isActive() && !shopProductVoucher.isDeleted();

                BigDecimal minOrder = shopProductVoucher.getMinOrderAmount() != null ? shopProductVoucher.getMinOrderAmount() : BigDecimal.ZERO;

                if (!isActive || !isTimeValid || !isUsageValid) {
                    warnings.add("Voucher của shop " + shop.getName() + " không còn hiệu lực");
                } else if (shopSubtotal.compareTo(minOrder) < 0) {
                    long roundedUpsell = minOrder.subtract(shopSubtotal).setScale(0, RoundingMode.HALF_UP).longValue();
                    String formattedUpsell = String.format(Locale.GERMAN, "%,d", roundedUpsell);
                    warnings.add("Voucher " + shopProductVoucher.getCode() + " yêu cầu đơn từ shop " + shop.getName() + " tối thiểu thêm " + formattedUpsell + "₫");
                } else {
                    if (shopProductVoucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        shopProductDiscount = shopProductVoucher.getDiscountValue().min(shopSubtotal);
                    } else { // PERCENTAGE
                        BigDecimal raw = shopSubtotal.multiply(shopProductVoucher.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (shopProductVoucher.getMaxDiscountAmount() != null) {
                            raw = raw.min(shopProductVoucher.getMaxDiscountAmount());
                        }
                        shopProductDiscount = raw.min(shopSubtotal);
                    }
                    shopProductDiscount = shopProductDiscount.setScale(2, RoundingMode.HALF_UP);
                    appliedVouchers.add(new AppliedVoucherPreviewResponse(
                            shopProductVoucher.getId(),
                            shopProductVoucher.getCode(),
                            shopProductVoucher.getName(),
                            shopProductDiscount
                    ));
                }
            }

            // 7b. Evaluate Shop Shipping Voucher (Trợ giá ship của Shop)
            BigDecimal shopShippingDiscount = BigDecimal.ZERO;
            Voucher shopShippingVoucher = shopShippingVoucherMap.get(shopId);
            if (shopShippingVoucher != null) {
                boolean isTimeValid = (shopShippingVoucher.getStartDate() == null || !shopShippingVoucher.getStartDate().isAfter(now))
                        && (shopShippingVoucher.getEndDate() == null || !shopShippingVoucher.getEndDate().isBefore(now));
                boolean isUsageValid = shopShippingVoucher.getUsageLimit() == null
                        || ((shopShippingVoucher.getUsedCount() != null ? shopShippingVoucher.getUsedCount() : 0)
                        + (shopShippingVoucher.getReservedCount() != null ? shopShippingVoucher.getReservedCount() : 0)) < shopShippingVoucher.getUsageLimit();
                boolean isActive = shopShippingVoucher.isActive() && !shopShippingVoucher.isDeleted();

                BigDecimal minOrder = shopShippingVoucher.getMinOrderAmount() != null ? shopShippingVoucher.getMinOrderAmount() : BigDecimal.ZERO;

                if (!isActive || !isTimeValid || !isUsageValid) {
                    warnings.add("Voucher vận chuyển của shop " + shop.getName() + " không còn hiệu lực");
                } else if (shopSubtotal.compareTo(minOrder) < 0) {
                    long roundedUpsell = minOrder.subtract(shopSubtotal).setScale(0, RoundingMode.HALF_UP).longValue();
                    String formattedUpsell = String.format(Locale.GERMAN, "%,d", roundedUpsell);
                    warnings.add("Voucher " + shopShippingVoucher.getCode() + " yêu cầu đơn từ shop " + shop.getName() + " tối thiểu thêm " + formattedUpsell + "₫");
                } else {
                    if (shopShippingVoucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        shopShippingDiscount = shopShippingVoucher.getDiscountValue().min(shopShippingFee);
                    } else { // PERCENTAGE
                        BigDecimal raw = shopShippingFee.multiply(shopShippingVoucher.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (shopShippingVoucher.getMaxDiscountAmount() != null) {
                            raw = raw.min(shopShippingVoucher.getMaxDiscountAmount());
                        }
                        shopShippingDiscount = raw.min(shopShippingFee);
                    }
                    shopShippingDiscount = shopShippingDiscount.setScale(2, RoundingMode.HALF_UP);
                    appliedVouchers.add(new AppliedVoucherPreviewResponse(
                            shopShippingVoucher.getId(),
                            shopShippingVoucher.getCode(),
                            shopShippingVoucher.getName(),
                            shopShippingDiscount
                    ));
                }
            }

            shopProductDiscountMap.put(shopId, shopProductDiscount);
            BigDecimal shopBaseProductAmount = shopSubtotal.subtract(shopProductDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            shopBaseProductAmountMap.put(shopId, shopBaseProductAmount);
            totalShopProductDiscount = totalShopProductDiscount.add(shopProductDiscount);

            shopShippingDiscountMap.put(shopId, shopShippingDiscount);
            BigDecimal shopBaseShippingFee = shopShippingFee.subtract(shopShippingDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            shopBaseShippingFeeMap.put(shopId, shopBaseShippingFee);
            totalShopShippingDiscount = totalShopShippingDiscount.add(shopShippingDiscount);

            shopAppliedVouchersMap.put(shopId, appliedVouchers);
        }

        // 8. Evaluate Platform Vouchers (Tier 1: Sàn -> Shop)
        // 8a. Platform Product Voucher
        BigDecimal totalPlatformProductDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseProductAmount = totalProductAmount.subtract(totalShopProductDiscount).max(BigDecimal.ZERO);

        if (request.platformVoucherId() != null) {
            Optional<Voucher> platVoucherOpt = voucherRepository.findWithShopById(request.platformVoucherId());
            if (platVoucherOpt.isEmpty()) {
                warnings.add("Voucher toàn sàn không tồn tại");
            } else {
                Voucher pv = platVoucherOpt.get();
                boolean isTypeValid = pv.getType() == VoucherType.PLATFORM;
                boolean isTimeValid = (pv.getStartDate() == null || !pv.getStartDate().isAfter(now))
                        && (pv.getEndDate() == null || !pv.getEndDate().isBefore(now));
                boolean isUsageValid = pv.getUsageLimit() == null
                        || ((pv.getUsedCount() != null ? pv.getUsedCount() : 0)
                        + (pv.getReservedCount() != null ? pv.getReservedCount() : 0)) < pv.getUsageLimit();
                boolean isActive = pv.isActive() && !pv.isDeleted();
                boolean isProductVoucher = pv.getAppliesTo() == VoucherAppliesTo.PRODUCT;

                BigDecimal minOrder = pv.getMinOrderAmount() != null ? pv.getMinOrderAmount() : BigDecimal.ZERO;

                if (!isTypeValid || !isActive || !isTimeValid || !isUsageValid || !isProductVoucher) {
                    warnings.add("Voucher toàn sàn " + pv.getCode() + " không còn hiệu lực");
                } else if (totalBaseProductAmount.compareTo(minOrder) < 0) {
                    long roundedUpsell = minOrder.subtract(totalBaseProductAmount).setScale(0, RoundingMode.HALF_UP).longValue();
                    String formattedUpsell = String.format(Locale.GERMAN, "%,d", roundedUpsell);
                    warnings.add("Voucher toàn sàn " + pv.getCode() + " yêu cầu giá trị đơn tối thiểu thêm " + formattedUpsell + "₫");
                } else {
                    if (pv.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        totalPlatformProductDiscount = pv.getDiscountValue().min(totalBaseProductAmount);
                    } else { // PERCENTAGE
                        BigDecimal raw = totalBaseProductAmount.multiply(pv.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (pv.getMaxDiscountAmount() != null) {
                            raw = raw.min(pv.getMaxDiscountAmount());
                        }
                        totalPlatformProductDiscount = raw.min(totalBaseProductAmount);
                    }
                    totalPlatformProductDiscount = totalPlatformProductDiscount.setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // 8b. Platform FreeShip Voucher
        BigDecimal totalPlatformShippingDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseShippingFee = totalShippingFee.subtract(totalShopShippingDiscount).max(BigDecimal.ZERO);

        if (request.platformShippingVoucherId() != null) {
            Optional<Voucher> platShipVoucherOpt = voucherRepository.findWithShopById(request.platformShippingVoucherId());
            if (platShipVoucherOpt.isEmpty()) {
                warnings.add("Voucher vận chuyển toàn sàn không tồn tại");
            } else {
                Voucher psv = platShipVoucherOpt.get();
                boolean isTypeValid = psv.getType() == VoucherType.PLATFORM;
                boolean isTimeValid = (psv.getStartDate() == null || !psv.getStartDate().isAfter(now))
                        && (psv.getEndDate() == null || !psv.getEndDate().isBefore(now));
                boolean isUsageValid = psv.getUsageLimit() == null
                        || ((psv.getUsedCount() != null ? psv.getUsedCount() : 0)
                        + (psv.getReservedCount() != null ? psv.getReservedCount() : 0)) < psv.getUsageLimit();
                boolean isActive = psv.isActive() && !psv.isDeleted();
                boolean isShippingVoucher = psv.getAppliesTo() == VoucherAppliesTo.SHIPPING_FEE;

                BigDecimal minOrder = psv.getMinOrderAmount() != null ? psv.getMinOrderAmount() : BigDecimal.ZERO;

                if (!isTypeValid || !isActive || !isTimeValid || !isUsageValid || !isShippingVoucher) {
                    warnings.add("Voucher vận chuyển toàn sàn " + psv.getCode() + " không còn hiệu lực");
                } else if (totalProductAmount.compareTo(minOrder) < 0) {
                    long roundedUpsell = minOrder.subtract(totalProductAmount).setScale(0, RoundingMode.HALF_UP).longValue();
                    String formattedUpsell = String.format(Locale.GERMAN, "%,d", roundedUpsell);
                    warnings.add("Voucher FreeShip " + psv.getCode() + " yêu cầu tổng tiền hàng tối thiểu thêm " + formattedUpsell + "₫");
                } else {
                    if (psv.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                        totalPlatformShippingDiscount = psv.getDiscountValue().min(totalBaseShippingFee);
                    } else { // PERCENTAGE
                        BigDecimal raw = totalBaseShippingFee.multiply(psv.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        if (psv.getMaxDiscountAmount() != null) {
                            raw = raw.min(psv.getMaxDiscountAmount());
                        }
                        totalPlatformShippingDiscount = raw.min(totalBaseShippingFee);
                    }
                    totalPlatformShippingDiscount = totalPlatformShippingDiscount.setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // 9. Allocate Platform Discounts Pro-Rata to Shops (Tier 1)
        Map<UUID, BigDecimal> allocatedPlatformProductDiscountMap = new HashMap<>();
        Map<UUID, BigDecimal> allocatedPlatformShippingDiscountMap = new HashMap<>();

        // 9a. Pro-Rata Platform Product Discount to Shops
        if (totalPlatformProductDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseProductAmount.compareTo(BigDecimal.ZERO) > 0) {
            UUID maxShopId = null;
            BigDecimal maxBase = BigDecimal.ZERO;
            BigDecimal allocatedSum = BigDecimal.ZERO;

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseProductAmountMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal base = entry.getValue();
                if (base.compareTo(maxBase) > 0) {
                    maxBase = base;
                    maxShopId = sId;
                }
                BigDecimal alloc = totalPlatformProductDiscount.multiply(base)
                        .divide(totalBaseProductAmount, 2, RoundingMode.HALF_UP)
                        .min(base);
                allocatedPlatformProductDiscountMap.put(sId, alloc);
                allocatedSum = allocatedSum.add(alloc);
            }

            BigDecimal diff = totalPlatformProductDiscount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShopId != null) {
                BigDecimal current = allocatedPlatformProductDiscountMap.get(maxShopId);
                allocatedPlatformProductDiscountMap.put(maxShopId, current.add(diff).max(BigDecimal.ZERO));
            }
        }

        // 9b. Pro-Rata Platform Shipping Discount to Shops
        if (totalPlatformShippingDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseShippingFee.compareTo(BigDecimal.ZERO) > 0) {
            UUID maxShopShipId = null;
            BigDecimal maxBaseShip = BigDecimal.ZERO;
            BigDecimal allocatedSumShip = BigDecimal.ZERO;

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseShippingFeeMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal baseShip = entry.getValue();
                if (baseShip.compareTo(maxBaseShip) > 0) {
                    maxBaseShip = baseShip;
                    maxShopShipId = sId;
                }
                BigDecimal alloc = totalPlatformShippingDiscount.multiply(baseShip)
                        .divide(totalBaseShippingFee, 2, RoundingMode.HALF_UP)
                        .min(baseShip);
                allocatedPlatformShippingDiscountMap.put(sId, alloc);
                allocatedSumShip = allocatedSumShip.add(alloc);
            }

            BigDecimal diffShip = totalPlatformShippingDiscount.subtract(allocatedSumShip);
            if (diffShip.compareTo(BigDecimal.ZERO) != 0 && maxShopShipId != null) {
                BigDecimal current = allocatedPlatformShippingDiscountMap.get(maxShopShipId);
                allocatedPlatformShippingDiscountMap.put(maxShopShipId, current.add(diffShip).max(BigDecimal.ZERO));
            }
        }

        // 10. Tier 2 Pro-Rata (Shop -> Items) & Build ShopCartPreviewGroupResponse
        List<ShopCartPreviewGroupResponse> shopGroups = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByShopId.entrySet()) {
            UUID shopId = entry.getKey();
            Shop shop = shopMap.get(shopId);
            BigDecimal shopSubtotal = shopSubtotalMap.get(shopId);
            BigDecimal shopShippingFee = shopShippingFeeMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            BigDecimal shopProdDisc = shopProductDiscountMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal shopShipDisc = shopShippingDiscountMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocPlatProdDisc = allocatedPlatformProductDiscountMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocPlatShipDisc = allocatedPlatformShippingDiscountMap.getOrDefault(shopId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            BigDecimal totalItemDiscountForShop = shopProdDisc.add(allocPlatProdDisc).min(shopSubtotal);
            List<CartItemPreviewResponse> rawItems = rawItemPreviewsMap.get(shopId);
            List<CartItemPreviewResponse> finalItems = new ArrayList<>();

            if (totalItemDiscountForShop.compareTo(BigDecimal.ZERO) > 0 && shopSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                int maxItemIndex = 0;
                BigDecimal maxItemSubtotal = BigDecimal.ZERO;
                for (int i = 0; i < rawItems.size(); i++) {
                    CartItemPreviewResponse ip = rawItems.get(i);
                    if (ip.originalSubtotal().compareTo(maxItemSubtotal) > 0) {
                        maxItemSubtotal = ip.originalSubtotal();
                        maxItemIndex = i;
                    }
                }

                List<BigDecimal> itemDiscounts = new ArrayList<>();
                BigDecimal itemAllocSum = BigDecimal.ZERO;

                for (CartItemPreviewResponse ip : rawItems) {
                    BigDecimal disc = totalItemDiscountForShop.multiply(ip.originalSubtotal())
                            .divide(shopSubtotal, 2, RoundingMode.HALF_UP)
                            .min(ip.originalSubtotal());
                    itemDiscounts.add(disc);
                    itemAllocSum = itemAllocSum.add(disc);
                }

                BigDecimal discDiff = totalItemDiscountForShop.subtract(itemAllocSum);
                if (discDiff.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal adj = itemDiscounts.get(maxItemIndex).add(discDiff)
                            .max(BigDecimal.ZERO).min(rawItems.get(maxItemIndex).originalSubtotal());
                    itemDiscounts.set(maxItemIndex, adj);
                }

                for (int i = 0; i < rawItems.size(); i++) {
                    CartItemPreviewResponse ip = rawItems.get(i);
                    BigDecimal disc = itemDiscounts.get(i);
                    BigDecimal finalSub = ip.originalSubtotal().subtract(disc).max(BigDecimal.ZERO);
                    int itemQty = Math.max(1, ip.quantity());
                    BigDecimal discountedUnit = finalSub.divide(BigDecimal.valueOf(itemQty), 2, RoundingMode.HALF_UP);

                    finalItems.add(new CartItemPreviewResponse(
                            ip.cartItemId(),
                            ip.productVariantId(),
                            ip.productName(),
                            ip.variantName(),
                            ip.imageUrl(),
                            ip.originalPrice(),
                            ip.quantity(),
                            ip.originalSubtotal(),
                            disc,
                            discountedUnit,
                            finalSub
                    ));
                }
            } else {
                finalItems = rawItems;
            }

            BigDecimal finalShippingFeeForShop = shopShippingFee.subtract(shopShipDisc).subtract(allocPlatShipDisc).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal shopProductFinal = shopSubtotal.subtract(totalItemDiscountForShop).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal shopFinalAmount = shopProductFinal.add(finalShippingFeeForShop).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalShopDiscountForShop = shopProdDisc.add(shopShipDisc).setScale(2, RoundingMode.HALF_UP);

            List<AppliedVoucherPreviewResponse> appliedVouchers = shopAppliedVouchersMap.getOrDefault(shopId, Collections.emptyList());
            AppliedVoucherPreviewResponse firstApplied = appliedVouchers.isEmpty() ? null : appliedVouchers.get(0);

            shopGroups.add(new ShopCartPreviewGroupResponse(
                    shopId,
                    shop.getName(),
                    shopSubtotal,
                    shopShippingFee,
                    shopProdDisc,
                    shopShipDisc,
                    allocPlatProdDisc,
                    allocPlatShipDisc,
                    finalShippingFeeForShop,
                    totalShopDiscountForShop,
                    shopFinalAmount,
                    firstApplied,
                    appliedVouchers,
                    finalItems
            ));
        }

        // 11. Aggregate Grand Totals
        BigDecimal totalShopDiscountAmount = totalShopProductDiscount.add(totalShopShippingDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPlatformDiscountAmount = totalPlatformProductDiscount.add(totalPlatformShippingDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSavedAmount = totalShopDiscountAmount.add(totalPlatformDiscountAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalProductAmount = totalProductAmount.subtract(totalShopProductDiscount).subtract(totalPlatformProductDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalShippingFee = totalShippingFee.subtract(totalShopShippingDiscount).subtract(totalPlatformShippingDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotalAmount = finalProductAmount.add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

        int totalSelectedItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        log.info("Cart preview completed for user {}: totalProduct={}, totalShip={}, totalShopDisc={}, totalPlatDisc={}, finalProduct={}, finalShip={}, finalTotal={}",
                userId, totalProductAmount, totalShippingFee, totalShopDiscountAmount, totalPlatformDiscountAmount, finalProductAmount, finalShippingFee, finalTotalAmount);

        return new CartPreviewResponse(
                totalProductAmount,
                totalShippingFee,
                totalShopDiscountAmount,
                totalPlatformDiscountAmount,
                totalSavedAmount,
                finalProductAmount,
                finalShippingFee,
                finalTotalAmount,
                totalSelectedItems,
                shopGroups,
                warnings
        );
    }
}

