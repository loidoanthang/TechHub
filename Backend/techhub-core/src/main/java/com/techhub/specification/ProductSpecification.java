package com.techhub.specification;

import com.techhub.model.entity.Product;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.enums.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> filterPublicProducts(
            String keyword,
            UUID categoryId,
            String brandName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Gatekeeper 5-tier conditions:
            // Approval status = APPROVED
            predicates.add(cb.equal(root.get("approvalStatus"), ProductApprovalStatus.APPROVED));

            // Visibility status = PUBLISHED
            predicates.add(cb.equal(root.get("visibilityStatus"), ProductVisibilityStatus.PUBLISHED));

            // isSuspended = false AND isDeleted = false
            predicates.add(cb.isFalse(root.get("isSuspended")));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // Category status = ACTIVE (BR-11A)
            predicates.add(cb.equal(root.get("category").get("status"), CategoryStatus.ACTIVE));

            // Shop status IN (ACTIVE, PAUSED) (BR-07)
            predicates.add(root.get("shop").get("status").in(ShopStatus.ACTIVE, ShopStatus.PAUSED));

            // Shop owner user status NOT IN (BANNED, DELETED)
            predicates.add(cb.not(root.get("shop").get("seller").get("user").get("status").in(UserStatus.BANNED, UserStatus.DELETED)));

            // Must have at least 1 ACTIVE variant (BR-11B)
            Subquery<ProductVariant> activeVariantSub = query.subquery(ProductVariant.class);
            Root<ProductVariant> pvActive = activeVariantSub.from(ProductVariant.class);
            activeVariantSub.select(pvActive);
            activeVariantSub.where(
                    cb.equal(pvActive.get("product"), root),
                    cb.equal(pvActive.get("status"), ProductVariantStatus.ACTIVE)
            );
            predicates.add(cb.exists(activeVariantSub));

            // 2. User-specified filters:
            // Keyword filter on name or brandName
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("brandName")), pattern)
                ));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Brand name filter (case-insensitive)
            if (brandName != null && !brandName.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("brandName")), brandName.trim().toLowerCase()));
            }

            // Price range filter
            if (minPrice != null || maxPrice != null) {
                Subquery<ProductVariant> priceSub = query.subquery(ProductVariant.class);
                Root<ProductVariant> pvPrice = priceSub.from(ProductVariant.class);
                priceSub.select(pvPrice);

                List<Predicate> pricePreds = new ArrayList<>();
                pricePreds.add(cb.equal(pvPrice.get("product"), root));
                pricePreds.add(cb.equal(pvPrice.get("status"), ProductVariantStatus.ACTIVE));
                if (minPrice != null) {
                    pricePreds.add(cb.greaterThanOrEqualTo(pvPrice.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    pricePreds.add(cb.lessThanOrEqualTo(pvPrice.get("price"), maxPrice));
                }
                priceSub.where(pricePreds.toArray(new Predicate[0]));
                predicates.add(cb.exists(priceSub));
            }

            // In-stock only filter
            if (Boolean.TRUE.equals(inStockOnly)) {
                Subquery<ProductVariant> stockSub = query.subquery(ProductVariant.class);
                Root<ProductVariant> pvStock = stockSub.from(ProductVariant.class);
                stockSub.select(pvStock);
                stockSub.where(
                        cb.equal(pvStock.get("product"), root),
                        cb.equal(pvStock.get("status"), ProductVariantStatus.ACTIVE),
                        cb.greaterThan(pvStock.get("stockQuantity"), 0)
                );
                predicates.add(cb.exists(stockSub));
            }

            // 3. Sorting by active variant price if requested (only for selectQuery, NOT countQuery)
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                if ("price_asc".equalsIgnoreCase(sortBy)) {
                    Subquery<BigDecimal> minPriceSub = query.subquery(BigDecimal.class);
                    Root<ProductVariant> pvSort = minPriceSub.from(ProductVariant.class);
                    minPriceSub.select(cb.min(pvSort.get("price")));
                    minPriceSub.where(
                            cb.equal(pvSort.get("product"), root),
                            cb.equal(pvSort.get("status"), ProductVariantStatus.ACTIVE)
                    );
                    query.orderBy(cb.asc(minPriceSub), cb.desc(root.get("createdAt")));
                } else if ("price_desc".equalsIgnoreCase(sortBy)) {
                    Subquery<BigDecimal> minPriceSub = query.subquery(BigDecimal.class);
                    Root<ProductVariant> pvSort = minPriceSub.from(ProductVariant.class);
                    minPriceSub.select(cb.min(pvSort.get("price")));
                    minPriceSub.where(
                            cb.equal(pvSort.get("product"), root),
                            cb.equal(pvSort.get("status"), ProductVariantStatus.ACTIVE)
                    );
                    query.orderBy(cb.desc(minPriceSub), cb.desc(root.get("createdAt")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> filterSellerProducts(
            UUID shopId,
            String keyword,
            UUID categoryId,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted
    ) {
        return Specification.where(belongsToShop(shopId))
                .and(hasCategory(categoryId))
                .and(hasApprovalStatus(approvalStatus))
                .and(hasVisibilityStatus(visibilityStatus))
                .and(isSuspended(isSuspended))
                .and(isDeleted(isDeleted))
                .and(hasKeyword(keyword));
    }

    public static Specification<Product> filterAdminProducts(
            UUID shopId,
            String keyword,
            UUID categoryId,
            ProductApprovalStatus approvalStatus,
            ProductVisibilityStatus visibilityStatus,
            Boolean isSuspended,
            Boolean isDeleted
    ) {
        return Specification.where(hasShop(shopId))
                .and(hasCategory(categoryId))
                .and(hasApprovalStatus(approvalStatus))
                .and(hasVisibilityStatus(visibilityStatus))
                .and(isSuspended(isSuspended))
                .and(isDeleted(isDeleted))
                .and(hasKeyword(keyword));
    }

    public static Specification<Product> hasShop(UUID shopId) {
        return (root, query, cb) -> shopId == null ? null : cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<Product> belongsToShop(UUID shopId) {
        return (root, query, cb) -> cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasApprovalStatus(ProductApprovalStatus approvalStatus) {
        return (root, query, cb) -> approvalStatus == null ? null : cb.equal(root.get("approvalStatus"), approvalStatus);
    }

    public static Specification<Product> hasVisibilityStatus(ProductVisibilityStatus visibilityStatus) {
        return (root, query, cb) -> visibilityStatus == null ? null : cb.equal(root.get("visibilityStatus"), visibilityStatus);
    }

    public static Specification<Product> isSuspended(Boolean isSuspended) {
        return (root, query, cb) -> isSuspended == null ? null : cb.equal(root.get("isSuspended"), isSuspended);
    }

    public static Specification<Product> isDeleted(Boolean isDeleted) {
        return (root, query, cb) -> isDeleted == null ? null : cb.equal(root.get("isDeleted"), isDeleted);
    }

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("brandName")), pattern)
            );
        };
    }
}
