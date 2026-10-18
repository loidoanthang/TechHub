package com.techhub.specification;

import com.techhub.model.entity.Order;
import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.User;
import com.techhub.model.enums.OrderStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> filterBuyerOrders(UUID userId, OrderStatus status, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory IDOR Protection (BR-11F)
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Optional Order Status filter (UC-02)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Optional Keyword search (UC-03)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmed = keyword.trim();
                String sanitized = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                String escaped = sanitized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped.toLowerCase() + "%";

                // Sub-predicate 1: orderNumber matches
                Predicate orderNumberPred = cb.like(cb.lower(root.get("orderNumber")), pattern, '\\');

                // Sub-predicate 2: shop.name matches
                Predicate shopNamePred = cb.like(cb.lower(root.get("shop").get("name")), pattern, '\\');

                // Sub-predicate 3: order_items.product_name_snapshot matches (via subquery on OrderItem)
                Subquery<UUID> itemSubquery = query.subquery(UUID.class);
                Root<OrderItem> itemRoot = itemSubquery.from(OrderItem.class);
                itemSubquery.select(itemRoot.get("order").get("id"))
                        .where(
                                cb.equal(itemRoot.get("order"), root),
                                cb.like(cb.lower(itemRoot.get("productNameSnapshot")), pattern, '\\')
                        );
                Predicate itemPred = cb.exists(itemSubquery);

                predicates.add(cb.or(orderNumberPred, shopNamePred, itemPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Order> filterSellerOrders(
            UUID shopId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Multi-tenant Isolation (BR-11F)
            predicates.add(cb.equal(root.get("shop").get("id"), shopId));

            // 2. Optional Order Status filter (UC-02)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Optional Date Range filter (UC-04)
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }

            // 4. Optional Keyword search (UC-03)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmed = keyword.trim();
                String sanitized = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                String escaped = sanitized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped.toLowerCase() + "%";

                // Sub-predicate 1: orderNumber matches
                Predicate orderNumberPred = cb.like(cb.lower(root.get("orderNumber")), pattern, '\\');

                // Sub-predicate 2: buyer user profile matches (firstName, lastName, email, phone) without JSON key leakage
                Path<User> userPath = root.get("user");
                Predicate buyerPred = cb.or(
                        cb.like(cb.lower(userPath.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(userPath.get("lastName")), pattern, '\\'),
                        cb.like(cb.lower(userPath.get("email")), pattern, '\\'),
                        cb.like(cb.lower(userPath.get("phone")), pattern, '\\')
                );

                // Sub-predicate 3: order_items.product_name_snapshot matches (via subquery on OrderItem)
                Subquery<UUID> itemSubquery = query.subquery(UUID.class);
                Root<OrderItem> itemRoot = itemSubquery.from(OrderItem.class);
                itemSubquery.select(itemRoot.get("order").get("id"))
                        .where(
                                cb.equal(itemRoot.get("order"), root),
                                cb.like(cb.lower(itemRoot.get("productNameSnapshot")), pattern, '\\')
                        );
                Predicate itemPred = cb.exists(itemSubquery);

                predicates.add(cb.or(orderNumberPred, buyerPred, itemPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Order> filterAdminOrders(
            UUID shopId,
            UUID userId,
            OrderStatus status,
            String keyword,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Optional Shop Filter
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            // 2. Optional Buyer User Filter
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // 3. Optional Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 4. Optional Date Range Filter
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }

            // 5. Optional Global Keyword Search
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmed = keyword.trim();
                String sanitized = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                String escaped = sanitized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped.toLowerCase() + "%";

                // Sub-predicate 1: orderNumber
                Predicate orderNumberPred = cb.like(cb.lower(root.get("orderNumber")), pattern, '\\');

                // Sub-predicate 2: shop.name
                Predicate shopNamePred = cb.like(cb.lower(root.get("shop").get("name")), pattern, '\\');

                // Sub-predicate 3: buyer user profile (firstName, lastName, full name, email, phone)
                Path<User> userPath = root.get("user");
                Expression<String> firstNameExpr = cb.coalesce(userPath.get("firstName"), "");
                Expression<String> lastNameExpr = cb.coalesce(userPath.get("lastName"), "");
                Expression<String> fullNameWestern = cb.concat(cb.concat(cb.lower(firstNameExpr), " "), cb.lower(lastNameExpr));
                Expression<String> fullNameVietnamese = cb.concat(cb.concat(cb.lower(lastNameExpr), " "), cb.lower(firstNameExpr));

                Predicate buyerPred = cb.or(
                        cb.like(cb.lower(userPath.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(userPath.get("lastName")), pattern, '\\'),
                        cb.like(fullNameWestern, pattern, '\\'),
                        cb.like(fullNameVietnamese, pattern, '\\'),
                        cb.like(cb.lower(userPath.get("email")), pattern, '\\'),
                        cb.like(cb.lower(userPath.get("phone")), pattern, '\\')
                );

                // Sub-predicate 4: order_items.product_name_snapshot (via subquery)
                Subquery<UUID> itemSubquery = query.subquery(UUID.class);
                Root<OrderItem> itemRoot = itemSubquery.from(OrderItem.class);
                itemSubquery.select(itemRoot.get("order").get("id"))
                        .where(
                                cb.equal(itemRoot.get("order"), root),
                                cb.like(cb.lower(itemRoot.get("productNameSnapshot")), pattern, '\\')
                        );
                Predicate itemPred = cb.exists(itemSubquery);

                predicates.add(cb.or(orderNumberPred, shopNamePred, buyerPred, itemPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
