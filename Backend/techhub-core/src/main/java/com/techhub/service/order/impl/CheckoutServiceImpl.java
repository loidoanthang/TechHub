package com.techhub.service.order.impl;
 
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CheckoutPreviewRequest;
import com.techhub.model.dto.request.PlaceOrderRequest;
import com.techhub.model.dto.request.ShopOrderRequest;
import com.techhub.model.dto.response.*;
import com.techhub.model.entity.*;
import com.techhub.model.enums.*;
import com.techhub.repository.*;
import com.techhub.service.order.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techhub.common.VNPayUtils;
import com.techhub.service.payment.VNPayService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final BigDecimal DEFAULT_PLATFORM_SHIPPING_FEE = BigDecimal.valueOf(30000).setScale(2, RoundingMode.HALF_UP);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ShippingOptionRepository shippingOptionRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VNPayService vnPayService;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(UUID userId, CheckoutPreviewRequest request) {
        log.info("Calculating checkout preview for user: {}", userId);

        // 1. Validate Input
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

        // 2. Validate Buyer Security & Account Status (BR-11D)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 3. Resolve Delivery Address & IDOR Protection (BR-05, BR-11F)
        AddressResponse deliveryAddress = null;
        if (request.addressId() != null) {
            Address address = addressRepository.findByIdAndUserIdAndShopIdIsNull(request.addressId(), userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));
            deliveryAddress = AddressResponse.from(address);
        } else {
            Optional<Address> defaultAddressOpt = addressRepository.findDefaultAddressByUserId(userId);
            if (defaultAddressOpt.isPresent()) {
                deliveryAddress = AddressResponse.from(defaultAddressOpt.get());
            }
        }

        // 4. Batch Fetch CartItems with deep details via @EntityGraph (BR-11F, Zero N+1 Query)
        List<CartItem> items = cartItemRepository.findByUserIdAndIdInWithDetails(userId, uniqueItemIds);
        if (items.size() != uniqueItemIds.size()) {
            log.warn("Cart item mismatch for user {}: requested {}, found {}", userId, uniqueItemIds.size(), items.size());
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 5. Gatekeeper Live Checks (BR-11C, BR-11E, BR-11G, BR-12B)
        for (CartItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getProductVariant();
            Shop shop = (product != null) ? product.getShop() : null;

            // Anti-Wash Trading / Self-Buying Prevention (BR-11E)
            if (shop != null && shop.getSeller() != null && shop.getSeller().getUser() != null) {
                if (userId.equals(shop.getSeller().getUser().getId())) {
                    log.warn("Buyer {} attempted to purchase own product from shop {}", userId, shop.getId());
                    throw new BusinessException(ErrorCode.CANNOT_ADD_OWN_PRODUCT);
                }
            }

            // Shop Status Check
            if (shop == null || shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
            }
            if (shop.getStatus() == ShopStatus.PAUSED) {
                throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
            }

            // Product Status Check
            if (product == null || product.isDeleted() || product.isSuspended()
                    || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED
                    || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
            }

            // Variant Status & Live Price Freshness (BR-11G)
            if (variant == null || variant.isDeleted() || variant.getStatus() != ProductVariantStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.NO_ACTIVE_VARIANT);
            }
            if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
            }
            if (item.getQuantity() <= 0 || item.getQuantity() > 999) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 6. Fallback Image Resolution
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

        // 7. Group items by Shop (BR-13)
        Map<UUID, List<CartItem>> itemsByShopId = new LinkedHashMap<>();
        Map<UUID, Shop> shopMap = new LinkedHashMap<>();
        for (CartItem item : items) {
            UUID sId = item.getProduct().getShop().getId();
            itemsByShopId.computeIfAbsent(sId, k -> new ArrayList<>()).add(item);
            shopMap.putIfAbsent(sId, item.getProduct().getShop());
        }

        // 8. Fetch Shipping Options in batch & Fallback Guardrail (BR-17)
        Map<UUID, BigDecimal> shopShippingFeeMap = new HashMap<>();
        List<ShippingOption> shippingOptions = shippingOptionRepository.findByShopIdIn(shopMap.keySet());
        for (ShippingOption so : shippingOptions) {
            if (so.getShop() != null && so.isActive() && so.getShippingFee() != null) {
                shopShippingFeeMap.put(so.getShop().getId(), so.getShippingFee().setScale(2, RoundingMode.HALF_UP));
            }
        }
        for (UUID sId : shopMap.keySet()) {
            shopShippingFeeMap.putIfAbsent(sId, DEFAULT_PLATFORM_SHIPPING_FEE);
        }

        // 9. Collect Candidate Vouchers & Batch Fetch User Redemption Quota (BR-16E Chốt 4 - Zero N+1)
        Set<UUID> allCandidateVoucherIds = new HashSet<>();
        if (request.shopVoucherIds() != null) {
            request.shopVoucherIds().stream().filter(Objects::nonNull).forEach(allCandidateVoucherIds::add);
        }
        if (request.platformVoucherId() != null) {
            allCandidateVoucherIds.add(request.platformVoucherId());
        }
        if (request.platformShippingVoucherId() != null) {
            allCandidateVoucherIds.add(request.platformShippingVoucherId());
        }

        Map<UUID, Long> userVoucherUsageMap = new HashMap<>();
        if (!allCandidateVoucherIds.isEmpty()) {
            List<Object[]> redemptions = voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(
                    userId,
                    allCandidateVoucherIds,
                    List.of(VoucherRedemptionStatus.RESERVED, VoucherRedemptionStatus.REDEEMED)
            );
            for (Object[] row : redemptions) {
                UUID vId = (UUID) row[0];
                Long count = (row[1] instanceof Number n) ? n.longValue() : 0L;
                userVoucherUsageMap.put(vId, count);
            }
        }

        // 10. Fetch & Filter Shop Vouchers (BR-16E 7-Step Gatekeeper)
        Map<UUID, Voucher> shopProductVoucherMap = new HashMap<>();
        Map<UUID, Voucher> shopShippingVoucherMap = new HashMap<>();
        if (request.shopVoucherIds() != null && !request.shopVoucherIds().isEmpty()) {
            List<UUID> uniqueShopVoucherIds = request.shopVoucherIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!uniqueShopVoucherIds.isEmpty()) {
                List<Voucher> fetchedShopVouchers = voucherRepository.findAllWithShopByIdIn(uniqueShopVoucherIds);
                Map<UUID, Voucher> shopVoucherById = fetchedShopVouchers.stream()
                        .collect(Collectors.toMap(Voucher::getId, v -> v, (existing, replacement) -> existing));

                for (UUID vId : uniqueShopVoucherIds) {
                    Voucher sv = shopVoucherById.get(vId);
                    if (sv == null) {
                        warnings.add("Mã giảm giá cửa hàng không tồn tại");
                        continue;
                    }

                    // Chốt 1: Tồn tại & Active & Not Deleted
                    if (!sv.isActive() || sv.isDeleted()) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " không khả dụng hoặc đã bị vô hiệu hóa");
                        continue;
                    }
                    // Chốt 5: Thẩm quyền Shop & Phạm vi
                    if (sv.getType() != VoucherType.SHOP || sv.getShop() == null) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " không phải voucher dành cho cửa hàng");
                        continue;
                    }
                    if (!itemsByShopId.containsKey(sv.getShop().getId())) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " không thuộc gian hàng trong đơn");
                        continue;
                    }
                    // Chốt 2: Cửa sổ thời gian hiệu lực
                    if (sv.getStartDate() != null && sv.getStartDate().isAfter(now)) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " chưa đến thời gian áp dụng (Bắt đầu từ " + sv.getStartDate().format(DATE_FORMATTER) + ")");
                        continue;
                    }
                    if (sv.getEndDate() != null && sv.getEndDate().isBefore(now)) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " đã hết hạn sử dụng vào ngày " + sv.getEndDate().format(DATE_FORMATTER));
                        continue;
                    }
                    // Chốt 3: Hạn mức phát hành toàn hệ thống (Hết slot)
                    int usedCount = sv.getUsedCount() != null ? sv.getUsedCount() : 0;
                    int reservedCount = sv.getReservedCount() != null ? sv.getReservedCount() : 0;
                    if (sv.getUsageLimit() != null && (usedCount + reservedCount) >= sv.getUsageLimit()) {
                        warnings.add("Mã giảm giá " + sv.getCode() + " đã hết lượt sử dụng trên toàn hệ thống (Hết slot)");
                        continue;
                    }
                    // Chốt 4: Hạn mức cá nhân của Buyer (Hết lượt)
                    long buyerUsed = userVoucherUsageMap.getOrDefault(sv.getId(), 0L);
                    int perUserLimit = sv.getUsageLimitPerUser() != null ? sv.getUsageLimitPerUser() : 1;
                    if (buyerUsed >= perUserLimit) {
                        warnings.add("Bạn đã sử dụng hết số lượt cho phép (" + perUserLimit + " lượt) của mã giảm giá " + sv.getCode());
                        continue;
                    }

                    // Chốt 6: Mục tiêu & Giới hạn chồng mã
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

        // 11. Tính toán giá trị từng Shop & Áp dụng Voucher Shop
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

                BigDecimal livePrice = variant.getPrice().setScale(2, RoundingMode.HALF_UP);
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

            BigDecimal shopShippingFee = shopShippingFeeMap.getOrDefault(shopId, DEFAULT_PLATFORM_SHIPPING_FEE);
            totalShippingFee = totalShippingFee.add(shopShippingFee);

            List<AppliedVoucherPreviewResponse> appliedVouchers = new ArrayList<>();

            // 11a. Evaluate Shop Product Voucher (Chốt 7 - Min Order)
            BigDecimal shopProductDiscount = BigDecimal.ZERO;
            Voucher shopProductVoucher = shopProductVoucherMap.get(shopId);
            if (shopProductVoucher != null) {
                BigDecimal minOrder = shopProductVoucher.getMinOrderAmount() != null ? shopProductVoucher.getMinOrderAmount() : BigDecimal.ZERO;
                if (shopSubtotal.compareTo(minOrder) < 0) {
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

            // 11b. Evaluate Shop Shipping Voucher (Chốt 7 - Min Order)
            BigDecimal shopShippingDiscount = BigDecimal.ZERO;
            Voucher shopShippingVoucher = shopShippingVoucherMap.get(shopId);
            if (shopShippingVoucher != null) {
                BigDecimal minOrder = shopShippingVoucher.getMinOrderAmount() != null ? shopShippingVoucher.getMinOrderAmount() : BigDecimal.ZERO;
                if (shopSubtotal.compareTo(minOrder) < 0) {
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

        // 12. Evaluate Platform Vouchers (Tier 1 Pro-Rata Sàn -> Shop - BR-16C, BR-16E)
        // 12a. Platform Product Voucher
        BigDecimal totalPlatformProductDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseProductAmount = totalProductAmount.subtract(totalShopProductDiscount).max(BigDecimal.ZERO);

        if (request.platformVoucherId() != null) {
            Optional<Voucher> platVoucherOpt = voucherRepository.findWithShopById(request.platformVoucherId());
            if (platVoucherOpt.isEmpty()) {
                warnings.add("Mã giảm giá toàn sàn không tồn tại");
            } else {
                Voucher pv = platVoucherOpt.get();
                if (!pv.isActive() || pv.isDeleted()) {
                    warnings.add("Mã giảm giá toàn sàn " + pv.getCode() + " không khả dụng hoặc đã bị vô hiệu hóa");
                } else if (pv.getType() != VoucherType.PLATFORM || pv.getShop() != null || pv.getAppliesTo() != VoucherAppliesTo.PRODUCT) {
                    warnings.add("Mã giảm giá toàn sàn " + pv.getCode() + " không phải voucher sản phẩm toàn sàn");
                } else if (pv.getStartDate() != null && pv.getStartDate().isAfter(now)) {
                    warnings.add("Mã giảm giá " + pv.getCode() + " chưa đến thời gian áp dụng (Bắt đầu từ " + pv.getStartDate().format(DATE_FORMATTER) + ")");
                } else if (pv.getEndDate() != null && pv.getEndDate().isBefore(now)) {
                    warnings.add("Mã giảm giá " + pv.getCode() + " đã hết hạn sử dụng vào ngày " + pv.getEndDate().format(DATE_FORMATTER));
                } else if (pv.getUsageLimit() != null && ((pv.getUsedCount() != null ? pv.getUsedCount() : 0) + (pv.getReservedCount() != null ? pv.getReservedCount() : 0)) >= pv.getUsageLimit()) {
                    warnings.add("Mã giảm giá " + pv.getCode() + " đã hết lượt sử dụng trên toàn hệ thống (Hết slot)");
                } else if (userVoucherUsageMap.getOrDefault(pv.getId(), 0L) >= (pv.getUsageLimitPerUser() != null ? pv.getUsageLimitPerUser() : 1)) {
                    warnings.add("Bạn đã sử dụng hết số lượt cho phép (" + (pv.getUsageLimitPerUser() != null ? pv.getUsageLimitPerUser() : 1) + " lượt) của mã " + pv.getCode());
                } else {
                    BigDecimal minOrder = pv.getMinOrderAmount() != null ? pv.getMinOrderAmount() : BigDecimal.ZERO;
                    if (totalBaseProductAmount.compareTo(minOrder) < 0) {
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
        }

        // 12b. Platform FreeShip Voucher
        BigDecimal totalPlatformShippingDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseShippingFee = totalShippingFee.subtract(totalShopShippingDiscount).max(BigDecimal.ZERO);

        if (request.platformShippingVoucherId() != null) {
            Optional<Voucher> platShipVoucherOpt = voucherRepository.findWithShopById(request.platformShippingVoucherId());
            if (platShipVoucherOpt.isEmpty()) {
                warnings.add("Mã vận chuyển toàn sàn không tồn tại");
            } else {
                Voucher psv = platShipVoucherOpt.get();
                if (!psv.isActive() || psv.isDeleted()) {
                    warnings.add("Mã vận chuyển toàn sàn " + psv.getCode() + " không khả dụng hoặc đã bị vô hiệu hóa");
                } else if (psv.getType() != VoucherType.PLATFORM || psv.getShop() != null || psv.getAppliesTo() != VoucherAppliesTo.SHIPPING_FEE) {
                    warnings.add("Mã vận chuyển toàn sàn " + psv.getCode() + " không phải voucher vận chuyển toàn sàn");
                } else if (psv.getStartDate() != null && psv.getStartDate().isAfter(now)) {
                    warnings.add("Mã giảm giá " + psv.getCode() + " chưa đến thời gian áp dụng (Bắt đầu từ " + psv.getStartDate().format(DATE_FORMATTER) + ")");
                } else if (psv.getEndDate() != null && psv.getEndDate().isBefore(now)) {
                    warnings.add("Mã giảm giá " + psv.getCode() + " đã hết hạn sử dụng vào ngày " + psv.getEndDate().format(DATE_FORMATTER));
                } else if (psv.getUsageLimit() != null && ((psv.getUsedCount() != null ? psv.getUsedCount() : 0) + (psv.getReservedCount() != null ? psv.getReservedCount() : 0)) >= psv.getUsageLimit()) {
                    warnings.add("Mã giảm giá " + psv.getCode() + " đã hết lượt sử dụng trên toàn hệ thống (Hết slot)");
                } else if (userVoucherUsageMap.getOrDefault(psv.getId(), 0L) >= (psv.getUsageLimitPerUser() != null ? psv.getUsageLimitPerUser() : 1)) {
                    warnings.add("Bạn đã sử dụng hết số lượt cho phép (" + (psv.getUsageLimitPerUser() != null ? psv.getUsageLimitPerUser() : 1) + " lượt) của mã " + psv.getCode());
                } else {
                    BigDecimal minOrder = psv.getMinOrderAmount() != null ? psv.getMinOrderAmount() : BigDecimal.ZERO;
                    if (totalProductAmount.compareTo(minOrder) < 0) {
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
        }

        // 13. Phân bổ Pro-Rata Tầng 1 (Sàn -> Shop - BR-16C)
        Map<UUID, BigDecimal> allocatedPlatformProductDiscountMap = new HashMap<>();
        if (totalPlatformProductDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseProductAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            UUID maxShopId = null;
            BigDecimal maxShopBaseAmount = BigDecimal.valueOf(-1);

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseProductAmountMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal baseAmount = entry.getValue();

                if (baseAmount.compareTo(maxShopBaseAmount) > 0) {
                    maxShopBaseAmount = baseAmount;
                    maxShopId = sId;
                }

                BigDecimal allocated = totalPlatformProductDiscount.multiply(baseAmount)
                        .divide(totalBaseProductAmount, 2, RoundingMode.HALF_UP);
                allocatedPlatformProductDiscountMap.put(sId, allocated);
                allocatedSum = allocatedSum.add(allocated);
            }

            BigDecimal diff = totalPlatformProductDiscount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShopId != null) {
                allocatedPlatformProductDiscountMap.put(maxShopId, allocatedPlatformProductDiscountMap.get(maxShopId).add(diff));
            }
        }

        Map<UUID, BigDecimal> allocatedPlatformShippingDiscountMap = new HashMap<>();
        if (totalPlatformShippingDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseShippingFee.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            UUID maxShopId = null;
            BigDecimal maxShopBaseShipping = BigDecimal.valueOf(-1);

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseShippingFeeMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal baseShipping = entry.getValue();

                if (baseShipping.compareTo(maxShopBaseShipping) > 0) {
                    maxShopBaseShipping = baseShipping;
                    maxShopId = sId;
                }

                BigDecimal allocated = totalPlatformShippingDiscount.multiply(baseShipping)
                        .divide(totalBaseShippingFee, 2, RoundingMode.HALF_UP);
                allocatedPlatformShippingDiscountMap.put(sId, allocated);
                allocatedSum = allocatedSum.add(allocated);
            }

            BigDecimal diff = totalPlatformShippingDiscount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShopId != null) {
                allocatedPlatformShippingDiscountMap.put(maxShopId, allocatedPlatformShippingDiscountMap.get(maxShopId).add(diff));
            }
        }

        // 14. Phân bổ Pro-Rata Tầng 2 (Shop -> Từng CartItem - Chuẩn Shopee)
        List<ShopCartPreviewGroupResponse> shopPreviewGroups = new ArrayList<>();

        for (UUID sId : shopMap.keySet()) {
            Shop shop = shopMap.get(sId);
            BigDecimal shopSubtotal = shopSubtotalMap.get(sId);
            BigDecimal shippingFee = shopShippingFeeMap.getOrDefault(sId, DEFAULT_PLATFORM_SHIPPING_FEE);
            BigDecimal shopProductDiscount = shopProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal shopShippingDiscount = shopShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocatedPlatformProduct = allocatedPlatformProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocatedPlatformShipping = allocatedPlatformShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            BigDecimal finalShippingFee = shippingFee.subtract(shopShippingDiscount).subtract(allocatedPlatformShipping)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalProductDiscountForShop = shopProductDiscount.add(allocatedPlatformProduct).min(shopSubtotal);
            BigDecimal shopDiscountAmount = shopProductDiscount.add(shopShippingDiscount)
                    .add(allocatedPlatformProduct).add(allocatedPlatformShipping).setScale(2, RoundingMode.HALF_UP);

            BigDecimal shopFinalAmount = shopSubtotal.subtract(shopProductDiscount).subtract(allocatedPlatformProduct)
                    .max(BigDecimal.ZERO).add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

            List<CartItemPreviewResponse> rawItems = rawItemPreviewsMap.get(sId);
            List<CartItemPreviewResponse> finalItems = new ArrayList<>();

            if (totalProductDiscountForShop.compareTo(BigDecimal.ZERO) > 0 && shopSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal allocatedSum = BigDecimal.ZERO;
                int maxItemIndex = -1;
                BigDecimal maxItemSubtotal = BigDecimal.valueOf(-1);

                for (int i = 0; i < rawItems.size(); i++) {
                    CartItemPreviewResponse item = rawItems.get(i);
                    if (item.originalSubtotal().compareTo(maxItemSubtotal) > 0) {
                        maxItemSubtotal = item.originalSubtotal();
                        maxItemIndex = i;
                    }

                    BigDecimal itemDiscount = totalProductDiscountForShop.multiply(item.originalSubtotal())
                            .divide(shopSubtotal, 2, RoundingMode.HALF_UP);
                    allocatedSum = allocatedSum.add(itemDiscount);

                    BigDecimal finalItemSubtotal = item.originalSubtotal().subtract(itemDiscount).max(BigDecimal.ZERO);
                    BigDecimal discountedUnitPrice = finalItemSubtotal.divide(BigDecimal.valueOf(item.quantity()), 2, RoundingMode.HALF_UP);

                    finalItems.add(new CartItemPreviewResponse(
                            item.cartItemId(),
                            item.productVariantId(),
                            item.productName(),
                            item.variantName(),
                            item.imageUrl(),
                            item.originalPrice(),
                            item.quantity(),
                            item.originalSubtotal(),
                            itemDiscount,
                            discountedUnitPrice,
                            finalItemSubtotal
                    ));
                }

                BigDecimal diff = totalProductDiscountForShop.subtract(allocatedSum);
                if (diff.compareTo(BigDecimal.ZERO) != 0 && maxItemIndex >= 0) {
                    CartItemPreviewResponse maxItem = finalItems.get(maxItemIndex);
                    BigDecimal adjustedDiscount = maxItem.itemDiscountAmount().add(diff).max(BigDecimal.ZERO);
                    BigDecimal adjustedFinalSubtotal = maxItem.originalSubtotal().subtract(adjustedDiscount).max(BigDecimal.ZERO);
                    BigDecimal adjustedUnitPrice = adjustedFinalSubtotal.divide(BigDecimal.valueOf(maxItem.quantity()), 2, RoundingMode.HALF_UP);

                    finalItems.set(maxItemIndex, new CartItemPreviewResponse(
                            maxItem.cartItemId(),
                            maxItem.productVariantId(),
                            maxItem.productName(),
                            maxItem.variantName(),
                            maxItem.imageUrl(),
                            maxItem.originalPrice(),
                            maxItem.quantity(),
                            maxItem.originalSubtotal(),
                            adjustedDiscount,
                            adjustedUnitPrice,
                            adjustedFinalSubtotal
                    ));
                }
            } else {
                finalItems.addAll(rawItems);
            }

            List<AppliedVoucherPreviewResponse> appliedVouchers = shopAppliedVouchersMap.getOrDefault(sId, List.of());
            AppliedVoucherPreviewResponse primaryVoucher = appliedVouchers.isEmpty() ? null : appliedVouchers.get(0);

            shopPreviewGroups.add(new ShopCartPreviewGroupResponse(
                    sId,
                    shop.getName(),
                    shopSubtotal,
                    shippingFee,
                    shopProductDiscount,
                    shopShippingDiscount,
                    allocatedPlatformProduct,
                    allocatedPlatformShipping,
                    finalShippingFee,
                    shopDiscountAmount,
                    shopFinalAmount,
                    primaryVoucher,
                    appliedVouchers,
                    finalItems
            ));
        }

        // 15. Tổng Hợp Toàn Bộ Đơn Hàng & Chặn Số Âm (BR-14 Zero-Negative Constraint)
        BigDecimal totalShopDiscountAmount = totalShopProductDiscount.add(totalShopShippingDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPlatformDiscountAmount = totalPlatformProductDiscount.add(totalPlatformShippingDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSavedAmount = totalShopDiscountAmount.add(totalPlatformDiscountAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalProductAmount = totalProductAmount.subtract(totalShopProductDiscount).subtract(totalPlatformProductDiscount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalShippingFee = totalShippingFee.subtract(totalShopShippingDiscount).subtract(totalPlatformShippingDiscount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotalAmount = finalProductAmount.add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

        int totalSelectedItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        List<String> availablePaymentMethods = List.of("COD", "VNPAY");

        return new CheckoutPreviewResponse(
                deliveryAddress,
                totalProductAmount.setScale(2, RoundingMode.HALF_UP),
                totalShippingFee.setScale(2, RoundingMode.HALF_UP),
                totalShopDiscountAmount,
                totalPlatformDiscountAmount,
                totalSavedAmount,
                finalProductAmount,
                finalShippingFee,
                finalTotalAmount,
                totalSelectedItems,
                shopPreviewGroups,
                availablePaymentMethods,
                warnings
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaceOrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        log.info("Processing placeOrder for user: {}", userId);

        // 0. Idempotency Check (Defense against double-click / network retry)
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Payment> existingPaymentOpt = paymentRepository.findByTransactionId(request.idempotencyKey());
            if (existingPaymentOpt.isPresent()) {
                Payment existingPayment = existingPaymentOpt.get();
                List<Order> existingOrders = orderRepository.findByPaymentId(existingPayment.getId());
                if (!existingOrders.isEmpty()) {
                    log.info("Idempotent request detected for key {}. Returning existing order summary.", request.idempotencyKey());
                    List<OrderSummaryResponse> orderSummaries = existingOrders.stream()
                            .map(o -> new OrderSummaryResponse(
                                    o.getId(),
                                    o.getOrderNumber(),
                                    o.getShop().getId(),
                                    o.getShop().getName(),
                                    o.getStatus(),
                                    o.getTotalAmount(),
                                    o.getShippingFee(),
                                    o.getItems() != null ? o.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0
                            )).toList();
                    String paymentUrl = (existingPayment.getPaymentMethod() == PaymentMethod.VNPAY && existingPayment.getStatus() == PaymentStatus.PENDING)
                            ? vnPayService.createPaymentUrl(existingPayment, existingPayment.getBankCode(), resolveClientIp())
                            : null;
                    return new PlaceOrderResponse(
                            existingPayment.getId(),
                            existingPayment.getPaymentMethod(),
                            existingPayment.getStatus(),
                            existingPayment.getAmount(),
                            existingOrders.size(),
                            orderSummaries,
                            paymentUrl,
                            existingPayment.getBankCode(),
                            existingPayment.getCreatedAt() != null ? existingPayment.getCreatedAt() : LocalDateTime.now()
                    );
                }
            }
        }

        // 1. Validate Input (Gate 1)
        if (request == null || request.selectedCartItemIds() == null || request.selectedCartItemIds().isEmpty()
                || request.addressId() == null || request.paymentMethod() == null
                || request.shopOrders() == null || request.shopOrders().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
        }

        if (request.bankCode() != null && !request.bankCode().isBlank() && !VNPayBankCode.isValid(request.bankCode())) {
            throw new BusinessException(ErrorCode.INVALID_BANK_CODE);
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

        // 2. Validate Buyer Security & Account Status (Gate 2 - BR-11D)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 3. Resolve Delivery Address & IDOR Protection (Gate 3 - BR-05, BR-11F)
        Address address = addressRepository.findByIdAndUserIdAndShopIdIsNull(request.addressId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        String addressSnapshotJson = buildAddressSnapshotJson(address);

        // 4. Batch Fetch CartItems with deep details via @EntityGraph (Gate 4 - BR-11F, Zero N+1 Query)
        List<CartItem> items = cartItemRepository.findByUserIdAndIdInWithDetails(userId, uniqueItemIds);
        if (items.size() != uniqueItemIds.size()) {
            log.warn("Cart item mismatch for user {}: requested {}, found {}", userId, uniqueItemIds.size(), items.size());
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        for (CartItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getProductVariant();
            Shop shop = (product != null) ? product.getShop() : null;

            // Anti-Wash Trading / Self-Buying Prevention (BR-11E)
            if (shop != null && shop.getSeller() != null && shop.getSeller().getUser() != null) {
                if (userId.equals(shop.getSeller().getUser().getId())) {
                    log.warn("Buyer {} attempted to purchase own product from shop {}", userId, shop.getId());
                    throw new BusinessException(ErrorCode.CANNOT_ADD_OWN_PRODUCT);
                }
            }

            // Shop Status Check
            if (shop == null || shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
            }
            if (shop.getStatus() == ShopStatus.PAUSED) {
                throw new BusinessException(ErrorCode.SHOP_IS_PAUSED);
            }

            // Product Status Check
            if (product == null || product.isDeleted() || product.isSuspended()
                    || product.getVisibilityStatus() != ProductVisibilityStatus.PUBLISHED
                    || product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
            }

            // Variant Status Check
            if (variant == null || variant.isDeleted() || variant.getStatus() != ProductVariantStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.NO_ACTIVE_VARIANT);
            }
            if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
            }
            if (item.getQuantity() <= 0 || item.getQuantity() > 999) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
        }

        // 5. Pessimistic Locking & Deadlock Prevention (Gate 5 - SELECT ... FOR UPDATE ORDER BY id ASC)
        List<UUID> sortedVariantIds = items.stream()
                .map(ci -> ci.getProductVariant().getId())
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findAllByIdInForUpdate(sortedVariantIds);
        Map<UUID, ProductVariant> lockedVariantsMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Map<UUID, Integer> requiredQtyMap = new HashMap<>();
        for (CartItem item : items) {
            UUID vId = item.getProductVariant().getId();
            requiredQtyMap.put(vId, requiredQtyMap.getOrDefault(vId, 0) + item.getQuantity());
        }

        for (Map.Entry<UUID, Integer> entry : requiredQtyMap.entrySet()) {
            ProductVariant lockedVar = lockedVariantsMap.get(entry.getKey());
            if (lockedVar == null || lockedVar.getStockQuantity() < entry.getValue()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // Deduct inventory
        for (Map.Entry<UUID, Integer> entry : requiredQtyMap.entrySet()) {
            ProductVariant lockedVar = lockedVariantsMap.get(entry.getKey());
            lockedVar.setStockQuantity(lockedVar.getStockQuantity() - entry.getValue());
        }
        productVariantRepository.saveAll(lockedVariants);

        // 6. Fallback Image Resolution
        Set<UUID> missingImageProductIds = items.stream()
                .filter(item -> item.getProduct() != null && item.getProductVariant() != null &&
                        (item.getProductVariant().getImageUrl() == null || item.getProductVariant().getImageUrl().isBlank()))
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        Map<UUID, String> productCoverImageMap = new HashMap<>();
        if (!missingImageProductIds.isEmpty()) {
            List<ProductImage> coverImages = productImageRepository.findByProductIdInAndDisplayOrder(new ArrayList<>(missingImageProductIds), 0);
            for (ProductImage img : coverImages) {
                productCoverImageMap.put(img.getProduct().getId(), img.getImageUrl());
            }
        }

        // 7. Group items by Shop (BR-13)
        Map<UUID, List<CartItem>> itemsByShopId = items.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getShop().getId()));

        Map<UUID, ShopOrderRequest> shopOrderReqMap = request.shopOrders().stream()
                .filter(so -> so != null && so.shopId() != null)
                .collect(Collectors.toMap(ShopOrderRequest::shopId, so -> so, (existing, replacement) -> existing));

        // 8. Shipping Options per Shop (BR-17)
        List<UUID> shopIds = new ArrayList<>(itemsByShopId.keySet());
        List<ShippingOption> activeShippingOptions = shippingOptionRepository.findByShopIdIn(shopIds);
        Map<UUID, BigDecimal> shopShippingFeeMap = activeShippingOptions.stream()
                .filter(ShippingOption::isActive)
                .collect(Collectors.toMap(
                        so -> so.getShop().getId(),
                        ShippingOption::getShippingFee,
                        (existing, replacement) -> existing
                ));

        // 9. Batch Fetch & Strict Validation for Vouchers (Gate 6 - BR-16F)
        List<UUID> allRequestedVoucherIds = new ArrayList<>();
        for (ShopOrderRequest sor : request.shopOrders()) {
            if (sor != null && sor.shopVoucherIds() != null) {
                sor.shopVoucherIds().stream().filter(Objects::nonNull).forEach(allRequestedVoucherIds::add);
            }
        }
        if (request.platformVoucherId() != null) {
            allRequestedVoucherIds.add(request.platformVoucherId());
        }
        if (request.platformShippingVoucherId() != null) {
            allRequestedVoucherIds.add(request.platformShippingVoucherId());
        }
        List<UUID> uniqueVoucherIds = allRequestedVoucherIds.stream().distinct().sorted().toList();

        Map<UUID, Voucher> voucherMap = new HashMap<>();
        if (!uniqueVoucherIds.isEmpty()) {
            List<Voucher> fetchedVouchers = voucherRepository.findAllWithShopByIdInForUpdate(uniqueVoucherIds);
            for (Voucher v : fetchedVouchers) {
                voucherMap.put(v.getId(), v);
            }
            if (voucherMap.size() != uniqueVoucherIds.size()) {
                throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
            }
        }

        Map<UUID, Long> userVoucherUsageMap = new HashMap<>();
        if (!uniqueVoucherIds.isEmpty()) {
            List<Object[]> usageRows = voucherRedemptionRepository.countByUserIdAndVoucherIdInAndStatusIn(
                    userId,
                    uniqueVoucherIds,
                    List.of(VoucherRedemptionStatus.RESERVED, VoucherRedemptionStatus.REDEEMED)
            );
            for (Object[] row : usageRows) {
                UUID vId = (UUID) row[0];
                long count = (row[1] instanceof Number n) ? n.longValue() : 0L;
                userVoucherUsageMap.put(vId, count);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 10. Strict Validation & Calculation of Shop Vouchers
        Map<UUID, Voucher> appliedShopProductVouchers = new HashMap<>();
        Map<UUID, Voucher> appliedShopShippingVouchers = new HashMap<>();
        Map<UUID, BigDecimal> shopSubtotalMap = new HashMap<>();
        Map<UUID, BigDecimal> shopProductDiscountMap = new HashMap<>();
        Map<UUID, BigDecimal> shopShippingDiscountMap = new HashMap<>();
        Map<UUID, BigDecimal> shopBaseProductAmountMap = new HashMap<>();
        Map<UUID, BigDecimal> shopBaseShippingFeeMap = new HashMap<>();

        BigDecimal totalProductAmount = BigDecimal.ZERO;
        BigDecimal totalShippingFee = BigDecimal.ZERO;

        for (UUID sId : itemsByShopId.keySet()) {
            List<CartItem> shopItems = itemsByShopId.get(sId);
            BigDecimal shopSubtotal = shopItems.stream()
                    .map(ci -> ci.getProductVariant().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            shopSubtotalMap.put(sId, shopSubtotal);
            totalProductAmount = totalProductAmount.add(shopSubtotal);

            BigDecimal shippingFee = shopShippingFeeMap.getOrDefault(sId, DEFAULT_PLATFORM_SHIPPING_FEE);
            totalShippingFee = totalShippingFee.add(shippingFee);

            ShopOrderRequest sor = shopOrderReqMap.get(sId);
            BigDecimal shopProductDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal shopShippingDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if (sor != null && sor.shopVoucherIds() != null) {
                for (UUID vId : sor.shopVoucherIds()) {
                    if (vId == null) continue;
                    Voucher v = voucherMap.get(vId);
                    if (v == null || !v.isActive() || v.isDeleted()) {
                        throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
                    }
                    if (v.getType() != VoucherType.SHOP || v.getShop() == null || !v.getShop().getId().equals(sId)) {
                        throw new BusinessException(ErrorCode.VOUCHER_SHOP_MISMATCH);
                    }
                    if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
                        throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
                    }
                    if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
                        throw new BusinessException(ErrorCode.VOUCHER_EXPIRED);
                    }
                    int totalCount = (v.getUsedCount() != null ? v.getUsedCount() : 0) + (v.getReservedCount() != null ? v.getReservedCount() : 0);
                    if (v.getUsageLimit() != null && totalCount >= v.getUsageLimit()) {
                        throw new BusinessException(ErrorCode.VOUCHER_OUT_OF_STOCK);
                    }
                    long userUsed = userVoucherUsageMap.getOrDefault(vId, 0L);
                    int userLimit = v.getUsageLimitPerUser() != null ? v.getUsageLimitPerUser() : 1;
                    if (userUsed >= userLimit) {
                        throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED);
                    }
                    if (v.getMinOrderAmount() != null && shopSubtotal.compareTo(v.getMinOrderAmount()) < 0) {
                        throw new BusinessException(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
                    }

                    if (v.getAppliesTo() == VoucherAppliesTo.PRODUCT) {
                        if (appliedShopProductVouchers.containsKey(sId)) {
                            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                        }
                        appliedShopProductVouchers.put(sId, v);
                        shopProductDiscount = calculateVoucherDiscount(v, shopSubtotal);
                    } else if (v.getAppliesTo() == VoucherAppliesTo.SHIPPING_FEE) {
                        if (appliedShopShippingVouchers.containsKey(sId)) {
                            throw new BusinessException(ErrorCode.INVALID_INPUT_DATA);
                        }
                        appliedShopShippingVouchers.put(sId, v);
                        shopShippingDiscount = calculateVoucherDiscount(v, shippingFee);
                    }
                }
            }

            shopProductDiscountMap.put(sId, shopProductDiscount);
            shopShippingDiscountMap.put(sId, shopShippingDiscount);

            BigDecimal baseProductAmount = shopSubtotal.subtract(shopProductDiscount).max(BigDecimal.ZERO);
            shopBaseProductAmountMap.put(sId, baseProductAmount);

            BigDecimal baseShippingFee = shippingFee.subtract(shopShippingDiscount).max(BigDecimal.ZERO);
            shopBaseShippingFeeMap.put(sId, baseShippingFee);
        }

        // 11. Strict Validation & Calculation of Platform Vouchers
        Voucher appliedPlatformProductVoucher = null;
        BigDecimal totalPlatformProductDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseProductAmount = shopBaseProductAmountMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.platformVoucherId() != null) {
            Voucher pv = voucherMap.get(request.platformVoucherId());
            if (pv == null || !pv.isActive() || pv.isDeleted()) {
                throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
            }
            if (pv.getType() != VoucherType.PLATFORM || pv.getShop() != null || pv.getAppliesTo() != VoucherAppliesTo.PRODUCT) {
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_TYPE);
            }
            if (pv.getStartDate() != null && now.isBefore(pv.getStartDate())) {
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
            }
            if (pv.getEndDate() != null && now.isAfter(pv.getEndDate())) {
                throw new BusinessException(ErrorCode.VOUCHER_EXPIRED);
            }
            int totalCount = (pv.getUsedCount() != null ? pv.getUsedCount() : 0) + (pv.getReservedCount() != null ? pv.getReservedCount() : 0);
            if (pv.getUsageLimit() != null && totalCount >= pv.getUsageLimit()) {
                throw new BusinessException(ErrorCode.VOUCHER_OUT_OF_STOCK);
            }
            long userUsed = userVoucherUsageMap.getOrDefault(pv.getId(), 0L);
            int userLimit = pv.getUsageLimitPerUser() != null ? pv.getUsageLimitPerUser() : 1;
            if (userUsed >= userLimit) {
                throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED);
            }
            if (pv.getMinOrderAmount() != null && totalProductAmount.compareTo(pv.getMinOrderAmount()) < 0) {
                throw new BusinessException(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
            }
            appliedPlatformProductVoucher = pv;
            totalPlatformProductDiscount = calculateVoucherDiscount(pv, totalBaseProductAmount);
        }

        Voucher appliedPlatformShippingVoucher = null;
        BigDecimal totalPlatformShippingDiscount = BigDecimal.ZERO;
        BigDecimal totalBaseShippingFee = shopBaseShippingFeeMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.platformShippingVoucherId() != null) {
            Voucher psv = voucherMap.get(request.platformShippingVoucherId());
            if (psv == null || !psv.isActive() || psv.isDeleted()) {
                throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
            }
            if (psv.getType() != VoucherType.PLATFORM || psv.getShop() != null || psv.getAppliesTo() != VoucherAppliesTo.SHIPPING_FEE) {
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_TYPE);
            }
            if (psv.getStartDate() != null && now.isBefore(psv.getStartDate())) {
                throw new BusinessException(ErrorCode.INVALID_VOUCHER_DATES);
            }
            if (psv.getEndDate() != null && now.isAfter(psv.getEndDate())) {
                throw new BusinessException(ErrorCode.VOUCHER_EXPIRED);
            }
            int totalCount = (psv.getUsedCount() != null ? psv.getUsedCount() : 0) + (psv.getReservedCount() != null ? psv.getReservedCount() : 0);
            if (psv.getUsageLimit() != null && totalCount >= psv.getUsageLimit()) {
                throw new BusinessException(ErrorCode.VOUCHER_OUT_OF_STOCK);
            }
            long userUsed = userVoucherUsageMap.getOrDefault(psv.getId(), 0L);
            int userLimit = psv.getUsageLimitPerUser() != null ? psv.getUsageLimitPerUser() : 1;
            if (userUsed >= userLimit) {
                throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED);
            }
            if (psv.getMinOrderAmount() != null && totalProductAmount.compareTo(psv.getMinOrderAmount()) < 0) {
                throw new BusinessException(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
            }
            appliedPlatformShippingVoucher = psv;
            totalPlatformShippingDiscount = calculateVoucherDiscount(psv, totalBaseShippingFee);
        }

        // 12. Phân bổ Pro-Rata Tầng 1 (Sàn -> Shop - BR-16C)
        Map<UUID, BigDecimal> allocatedPlatformProductDiscountMap = new HashMap<>();
        if (totalPlatformProductDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseProductAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            UUID maxShopId = null;
            BigDecimal maxShopBaseAmount = BigDecimal.valueOf(-1);

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseProductAmountMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal baseAmount = entry.getValue();

                if (baseAmount.compareTo(maxShopBaseAmount) > 0) {
                    maxShopBaseAmount = baseAmount;
                    maxShopId = sId;
                }

                BigDecimal allocated = totalPlatformProductDiscount.multiply(baseAmount)
                        .divide(totalBaseProductAmount, 2, RoundingMode.HALF_UP);
                allocatedPlatformProductDiscountMap.put(sId, allocated);
                allocatedSum = allocatedSum.add(allocated);
            }

            BigDecimal diff = totalPlatformProductDiscount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShopId != null) {
                allocatedPlatformProductDiscountMap.put(maxShopId, allocatedPlatformProductDiscountMap.get(maxShopId).add(diff));
            }
        }

        Map<UUID, BigDecimal> allocatedPlatformShippingDiscountMap = new HashMap<>();
        if (totalPlatformShippingDiscount.compareTo(BigDecimal.ZERO) > 0 && totalBaseShippingFee.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            UUID maxShopId = null;
            BigDecimal maxShopBaseShipping = BigDecimal.valueOf(-1);

            for (Map.Entry<UUID, BigDecimal> entry : shopBaseShippingFeeMap.entrySet()) {
                UUID sId = entry.getKey();
                BigDecimal baseShipping = entry.getValue();

                if (baseShipping.compareTo(maxShopBaseShipping) > 0) {
                    maxShopBaseShipping = baseShipping;
                    maxShopId = sId;
                }

                BigDecimal allocated = totalPlatformShippingDiscount.multiply(baseShipping)
                        .divide(totalBaseShippingFee, 2, RoundingMode.HALF_UP);
                allocatedPlatformShippingDiscountMap.put(sId, allocated);
                allocatedSum = allocatedSum.add(allocated);
            }

            BigDecimal diff = totalPlatformShippingDiscount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShopId != null) {
                allocatedPlatformShippingDiscountMap.put(maxShopId, allocatedPlatformShippingDiscountMap.get(maxShopId).add(diff));
            }
        }

        // 13. Phân bổ Pro-Rata Tầng 2 (Shop -> Items) & Tính Tài Chính
        Map<UUID, BigDecimal> shopFinalAmountMap = new HashMap<>();
        Map<UUID, BigDecimal> shopTotalCommissionMap = new HashMap<>();
        Map<UUID, BigDecimal> shopTotalSellerEarningsMap = new HashMap<>();
        Map<UUID, List<OrderItem>> shopOrderItemsMap = new HashMap<>();

        BigDecimal finalTotalAmount = BigDecimal.ZERO;

        for (UUID sId : itemsByShopId.keySet()) {
            List<CartItem> shopItems = itemsByShopId.get(sId);
            BigDecimal shopSubtotal = shopSubtotalMap.get(sId);
            BigDecimal shippingFee = shopShippingFeeMap.getOrDefault(sId, DEFAULT_PLATFORM_SHIPPING_FEE);
            BigDecimal shopProductDiscount = shopProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal shopShippingDiscount = shopShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocatedPlatformProduct = allocatedPlatformProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal allocatedPlatformShipping = allocatedPlatformShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            BigDecimal finalShippingFee = shippingFee.subtract(shopShippingDiscount).subtract(allocatedPlatformShipping)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalProductDiscountForShop = shopProductDiscount.add(allocatedPlatformProduct).min(shopSubtotal);

            BigDecimal shopFinalAmount = shopSubtotal.subtract(shopProductDiscount).subtract(allocatedPlatformProduct)
                    .max(BigDecimal.ZERO).add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

            shopFinalAmountMap.put(sId, shopFinalAmount);
            finalTotalAmount = finalTotalAmount.add(shopFinalAmount);

            // Item-level Pro-Rata
            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal orderTotalCommission = BigDecimal.ZERO;

            if (totalProductDiscountForShop.compareTo(BigDecimal.ZERO) > 0 && shopSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal allocatedSum = BigDecimal.ZERO;
                int maxItemIndex = -1;
                BigDecimal maxItemSubtotal = BigDecimal.valueOf(-1);

                List<BigDecimal> itemDiscounts = new ArrayList<>();
                for (int i = 0; i < shopItems.size(); i++) {
                    CartItem ci = shopItems.get(i);
                    BigDecimal itemOriginalSubtotal = ci.getProductVariant().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
                    if (itemOriginalSubtotal.compareTo(maxItemSubtotal) > 0) {
                        maxItemSubtotal = itemOriginalSubtotal;
                        maxItemIndex = i;
                    }
                    BigDecimal itemDiscount = totalProductDiscountForShop.multiply(itemOriginalSubtotal)
                            .divide(shopSubtotal, 2, RoundingMode.HALF_UP);
                    itemDiscounts.add(itemDiscount);
                    allocatedSum = allocatedSum.add(itemDiscount);
                }

                BigDecimal diff = totalProductDiscountForShop.subtract(allocatedSum);
                if (diff.compareTo(BigDecimal.ZERO) != 0 && maxItemIndex >= 0) {
                    itemDiscounts.set(maxItemIndex, itemDiscounts.get(maxItemIndex).add(diff));
                }

                for (int i = 0; i < shopItems.size(); i++) {
                    CartItem ci = shopItems.get(i);
                    BigDecimal itemDiscount = itemDiscounts.get(i);
                    BigDecimal origPrice = ci.getProductVariant().getPrice().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal origSubtotal = origPrice.multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal finalSubtotal = origSubtotal.subtract(itemDiscount).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = finalSubtotal.divide(BigDecimal.valueOf(ci.getQuantity()), 2, RoundingMode.HALF_UP);

                    BigDecimal commissionAmount = finalSubtotal.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal sellerEarnings = finalSubtotal.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);
                    orderTotalCommission = orderTotalCommission.add(commissionAmount);

                    String imgUrl = ci.getProductVariant().getImageUrl();
                    if (imgUrl == null || imgUrl.isBlank()) {
                        imgUrl = productCoverImageMap.get(ci.getProduct().getId());
                    }

                    OrderItem orderItem = OrderItem.builder()
                            .productVariant(ci.getProductVariant())
                            .productNameSnapshot(ci.getProduct().getName())
                            .variantNameSnapshot(ci.getProductVariant().getVariantName() != null ? ci.getProductVariant().getVariantName() : ci.getProductVariant().getSku())
                            .skuSnapshot(ci.getProductVariant().getSku())
                            .imageUrlSnapshot(imgUrl)
                            .originalPrice(origPrice)
                            .quantity(ci.getQuantity())
                            .itemDiscountAmount(itemDiscount)
                            .discountedUnitPrice(unitPrice)
                            .finalSubtotal(finalSubtotal)
                            .commissionRateSnapshot(BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP))
                            .commissionAmount(commissionAmount)
                            .sellerEarnings(sellerEarnings)
                            .build();

                    orderItems.add(orderItem);
                }
            } else {
                for (CartItem ci : shopItems) {
                    BigDecimal origPrice = ci.getProductVariant().getPrice().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal origSubtotal = origPrice.multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal commissionAmount = origSubtotal.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal sellerEarnings = origSubtotal.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);
                    orderTotalCommission = orderTotalCommission.add(commissionAmount);

                    String imgUrl = ci.getProductVariant().getImageUrl();
                    if (imgUrl == null || imgUrl.isBlank()) {
                        imgUrl = productCoverImageMap.get(ci.getProduct().getId());
                    }

                    OrderItem orderItem = OrderItem.builder()
                            .productVariant(ci.getProductVariant())
                            .productNameSnapshot(ci.getProduct().getName())
                            .variantNameSnapshot(ci.getProductVariant().getVariantName() != null ? ci.getProductVariant().getVariantName() : ci.getProductVariant().getSku())
                            .skuSnapshot(ci.getProductVariant().getSku())
                            .imageUrlSnapshot(imgUrl)
                            .originalPrice(origPrice)
                            .quantity(ci.getQuantity())
                            .itemDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .discountedUnitPrice(origPrice)
                            .finalSubtotal(origSubtotal)
                            .commissionRateSnapshot(BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP))
                            .commissionAmount(commissionAmount)
                            .sellerEarnings(sellerEarnings)
                            .build();

                    orderItems.add(orderItem);
                }
            }

            shopTotalCommissionMap.put(sId, orderTotalCommission);
            BigDecimal sellerEarnings = shopSubtotal.subtract(shopProductDiscount).subtract(shopShippingDiscount)
                    .subtract(orderTotalCommission).setScale(2, RoundingMode.HALF_UP);
            shopTotalSellerEarningsMap.put(sId, sellerEarnings);
            shopOrderItemsMap.put(sId, orderItems);
        }

        finalTotalAmount = finalTotalAmount.setScale(2, RoundingMode.HALF_UP);

        // 14. Create Unified Payment Session
        PaymentStatus initialPaymentStatus = (finalTotalAmount.compareTo(BigDecimal.ZERO) == 0)
                ? PaymentStatus.PAID
                : PaymentStatus.PENDING;

        Payment payment = Payment.builder()
                .user(user)
                .paymentMethod(request.paymentMethod())
                .bankCode(request.bankCode())
                .transactionId(request.idempotencyKey() != null && !request.idempotencyKey().isBlank() ? request.idempotencyKey() : null)
                .status(initialPaymentStatus)
                .amount(finalTotalAmount)
                .refundedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
        payment = paymentRepository.save(payment);

        // 15. Create N Orders (BR-13)
        OrderStatus orderStatus = (request.paymentMethod() == PaymentMethod.COD || finalTotalAmount.compareTo(BigDecimal.ZERO) == 0)
                ? OrderStatus.PENDING_CONFIRMATION
                : OrderStatus.PENDING_PAYMENT;

        List<Order> createdOrders = new ArrayList<>();
        List<OrderSummaryResponse> orderSummaries = new ArrayList<>();

        for (UUID sId : itemsByShopId.keySet()) {
            List<CartItem> shopItems = itemsByShopId.get(sId);
            Shop shop = shopItems.get(0).getProduct().getShop();
            ShopOrderRequest sor = shopOrderReqMap.get(sId);

            BigDecimal shopSubtotal = shopSubtotalMap.get(sId);
            BigDecimal shippingFee = shopShippingFeeMap.getOrDefault(sId, DEFAULT_PLATFORM_SHIPPING_FEE);
            BigDecimal shopProductDiscount = shopProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO);
            BigDecimal shopShippingDiscount = shopShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO);
            BigDecimal allocatedPlatformProduct = allocatedPlatformProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO);
            BigDecimal allocatedPlatformShipping = allocatedPlatformShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO);

            BigDecimal shopDiscountAmount = shopProductDiscount.add(shopShippingDiscount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal platformDiscountAmount = allocatedPlatformProduct.add(allocatedPlatformShipping).setScale(2, RoundingMode.HALF_UP);
            BigDecimal shopFinalAmount = shopFinalAmountMap.get(sId);
            BigDecimal orderCommission = shopTotalCommissionMap.get(sId);
            BigDecimal orderSellerEarnings = shopTotalSellerEarningsMap.get(sId);

            String orderNumber = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .user(user)
                    .shop(shop)
                    .payment(payment)
                    .status(orderStatus)
                    .deliveryAddressSnapshot(addressSnapshotJson)
                    .totalProductAmount(shopSubtotal)
                    .shippingFee(shippingFee)
                    .shopDiscountAmount(shopDiscountAmount)
                    .platformDiscountAmount(platformDiscountAmount)
                    .totalAmount(shopFinalAmount)
                    .totalCommission(orderCommission)
                    .totalSellerEarnings(orderSellerEarnings)
                    .note(sor != null ? sor.note() : null)
                    .build();

            order = orderRepository.save(order);

            List<OrderItem> itemsToSave = shopOrderItemsMap.get(sId);
            for (OrderItem oi : itemsToSave) {
                oi.setOrder(order);
            }
            orderItemRepository.saveAll(itemsToSave);

            createdOrders.add(order);
            orderSummaries.add(new OrderSummaryResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    shop.getId(),
                    shop.getName(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getShippingFee(),
                    itemsToSave.stream().mapToInt(OrderItem::getQuantity).sum()
            ));
        }

        // 16. Record Voucher Redemptions & Update Usage Counts (BR-16B)
        VoucherRedemptionStatus redemptionStatus = (request.paymentMethod() == PaymentMethod.COD || finalTotalAmount.compareTo(BigDecimal.ZERO) == 0)
                ? VoucherRedemptionStatus.REDEEMED
                : VoucherRedemptionStatus.RESERVED;

        List<VoucherRedemption> redemptions = new ArrayList<>();
        Set<Voucher> vouchersToUpdate = new HashSet<>();

        // Shop Vouchers Redemptions
        for (Order order : createdOrders) {
            UUID sId = order.getShop().getId();
            if (appliedShopProductVouchers.containsKey(sId)) {
                Voucher v = appliedShopProductVouchers.get(sId);
                redemptions.add(VoucherRedemption.builder()
                        .voucher(v)
                        .user(user)
                        .paymentId(payment.getId())
                        .orderId(order.getId())
                        .voucherCode(v.getCode())
                        .discountAmount(shopProductDiscountMap.getOrDefault(sId, BigDecimal.ZERO))
                        .status(redemptionStatus)
                        .redeemedAt(redemptionStatus == VoucherRedemptionStatus.REDEEMED ? now : null)
                        .reservedAt(redemptionStatus == VoucherRedemptionStatus.RESERVED ? now : null)
                        .build());
                vouchersToUpdate.add(v);
            }
            if (appliedShopShippingVouchers.containsKey(sId)) {
                Voucher v = appliedShopShippingVouchers.get(sId);
                redemptions.add(VoucherRedemption.builder()
                        .voucher(v)
                        .user(user)
                        .paymentId(payment.getId())
                        .orderId(order.getId())
                        .voucherCode(v.getCode())
                        .discountAmount(shopShippingDiscountMap.getOrDefault(sId, BigDecimal.ZERO))
                        .status(redemptionStatus)
                        .redeemedAt(redemptionStatus == VoucherRedemptionStatus.REDEEMED ? now : null)
                        .reservedAt(redemptionStatus == VoucherRedemptionStatus.RESERVED ? now : null)
                        .build());
                vouchersToUpdate.add(v);
            }
        }

        // Platform Vouchers Redemptions
        if (appliedPlatformProductVoucher != null) {
            redemptions.add(VoucherRedemption.builder()
                    .voucher(appliedPlatformProductVoucher)
                    .user(user)
                    .paymentId(payment.getId())
                    .orderId(null)
                    .voucherCode(appliedPlatformProductVoucher.getCode())
                    .discountAmount(totalPlatformProductDiscount)
                    .status(redemptionStatus)
                    .redeemedAt(redemptionStatus == VoucherRedemptionStatus.REDEEMED ? now : null)
                    .reservedAt(redemptionStatus == VoucherRedemptionStatus.RESERVED ? now : null)
                    .build());
            vouchersToUpdate.add(appliedPlatformProductVoucher);
        }

        if (appliedPlatformShippingVoucher != null) {
            redemptions.add(VoucherRedemption.builder()
                    .voucher(appliedPlatformShippingVoucher)
                    .user(user)
                    .paymentId(payment.getId())
                    .orderId(null)
                    .voucherCode(appliedPlatformShippingVoucher.getCode())
                    .discountAmount(totalPlatformShippingDiscount)
                    .status(redemptionStatus)
                    .redeemedAt(redemptionStatus == VoucherRedemptionStatus.REDEEMED ? now : null)
                    .reservedAt(redemptionStatus == VoucherRedemptionStatus.RESERVED ? now : null)
                    .build());
            vouchersToUpdate.add(appliedPlatformShippingVoucher);
        }

        if (!redemptions.isEmpty()) {
            voucherRedemptionRepository.saveAll(redemptions);
        }

        for (Voucher v : vouchersToUpdate) {
            if (redemptionStatus == VoucherRedemptionStatus.REDEEMED) {
                v.setUsedCount((v.getUsedCount() != null ? v.getUsedCount() : 0) + 1);
            } else {
                v.setReservedCount((v.getReservedCount() != null ? v.getReservedCount() : 0) + 1);
            }
        }
        if (!vouchersToUpdate.isEmpty()) {
            voucherRepository.saveAll(vouchersToUpdate);
        }

        // 17. Cart Cleanup (BR-11F)
        if (items.get(0).getCart() != null) {
            UUID cartId = items.get(0).getCart().getId();
            cartItemRepository.deleteByCartIdAndIdIn(cartId, uniqueItemIds);
        }

        // 18. Build PlaceOrderResponse
        String paymentUrl = null;
        if (request.paymentMethod() == PaymentMethod.VNPAY && finalTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentUrl = vnPayService.createPaymentUrl(payment, request.bankCode(), resolveClientIp());
        }

        return new PlaceOrderResponse(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                finalTotalAmount,
                createdOrders.size(),
                orderSummaries,
                paymentUrl,
                payment.getBankCode(),
                payment.getCreatedAt() != null ? payment.getCreatedAt() : now
        );
    }

    private BigDecimal calculateVoucherDiscount(Voucher v, BigDecimal baseAmount) {
        if (v == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discount;
        if (v.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = v.getDiscountValue().min(baseAmount);
        } else { // PERCENTAGE
            BigDecimal raw = baseAmount.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (v.getMaxDiscountAmount() != null) {
                raw = raw.min(v.getMaxDiscountAmount());
            }
            discount = raw.min(baseAmount);
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildAddressSnapshotJson(Address a) {
        return String.format(
                "{\"id\":\"%s\",\"recipientName\":\"%s\",\"phoneNumber\":\"%s\",\"streetAddress\":\"%s\",\"ward\":\"%s\",\"district\":\"%s\",\"provinceCity\":\"%s\",\"isDefault\":%b}",
                a.getId(),
                escapeJson(a.getRecipientName()),
                escapeJson(a.getPhone()),
                escapeJson(a.getStreetAddress()),
                escapeJson(a.getWard()),
                escapeJson(a.getDistrict()),
                escapeJson(a.getProvince()),
                a.isDefault()
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveClientIp() {
        try {
            RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs instanceof ServletRequestAttributes servletRequestAttributes) {
                return VNPayUtils.getClientIp(servletRequestAttributes.getRequest());
            }
        } catch (Exception e) {
            log.debug("Unable to resolve client IP from RequestContextHolder, falling back to 127.0.0.1: {}", e.getMessage());
        }
        return "127.0.0.1";
    }
}
