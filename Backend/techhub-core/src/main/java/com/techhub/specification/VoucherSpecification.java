package com.techhub.specification;

import com.techhub.model.entity.Shop;
import com.techhub.model.entity.Voucher;
import com.techhub.model.enums.DiscountType;
import com.techhub.model.enums.VoucherAppliesTo;
import com.techhub.model.enums.VoucherStatus;
import com.techhub.model.enums.VoucherType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VoucherSpecification {

    private VoucherSpecification() {}

    public static Specification<Voucher> filterShopVouchers(
            UUID shopId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            LocalDateTime now
    ) {
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Multi-tenant isolation: shopId must match and type must be SHOP
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            predicates.add(cb.equal(root.get("type"), VoucherType.SHOP));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // 2. Keyword search: match code (case-insensitive) or name (case-insensitive), bounded length
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmed = keyword.trim();
                String sanitized = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                String pattern = "%" + sanitized.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            // 3. appliesTo filter
            if (appliesTo != null) {
                predicates.add(cb.equal(root.get("appliesTo"), appliesTo));
            }

            // 4. discountType filter
            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }

            // 5. Dynamic status filter based on effectiveNow
            if (status != null) {
                switch (status) {
                    case UPCOMING -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.greaterThan(root.get("startDate"), effectiveNow));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), effectiveNow));
                    }
                    case ONGOING -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), effectiveNow));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), effectiveNow));
                    }
                    case EXPIRED -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.lessThan(root.get("endDate"), effectiveNow));
                    }
                    case DISABLED -> {
                        predicates.add(cb.isFalse(root.get("isActive")));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Voucher> filterAdminVouchers(
            VoucherType type,
            UUID shopId,
            String keyword,
            VoucherAppliesTo appliesTo,
            DiscountType discountType,
            VoucherStatus status,
            LocalDateTime now
    ) {
        LocalDateTime effectiveNow = now != null ? now : LocalDateTime.now();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Soft-delete filter: only non-deleted vouchers
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // 2. Type filter (PLATFORM / SHOP)
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 3. Specific shopId filter
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            // 4. Keyword search: match code, name, OR shop.name (case-insensitive, max 100 chars, escaped wildcards)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmed = keyword.trim();
                String sanitized = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                String escaped = sanitized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped.toLowerCase() + "%";
                Join<Voucher, Shop> shopJoin = root.join("shop", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(shopJoin.get("name")), pattern, '\\')
                ));
            }

            // 5. appliesTo filter
            if (appliesTo != null) {
                predicates.add(cb.equal(root.get("appliesTo"), appliesTo));
            }

            // 6. discountType filter
            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }

            // 7. Dynamic status filter based on effectiveNow
            if (status != null) {
                switch (status) {
                    case UPCOMING -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.greaterThan(root.get("startDate"), effectiveNow));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), effectiveNow));
                    }
                    case ONGOING -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), effectiveNow));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), effectiveNow));
                    }
                    case EXPIRED -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.lessThan(root.get("endDate"), effectiveNow));
                    }
                    case DISABLED -> {
                        predicates.add(cb.isFalse(root.get("isActive")));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
