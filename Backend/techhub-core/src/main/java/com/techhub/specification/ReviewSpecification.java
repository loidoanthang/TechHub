package com.techhub.specification;

import com.techhub.model.entity.Order;
import com.techhub.model.entity.OrderItem;
import com.techhub.model.entity.ProductVariant;
import com.techhub.model.entity.Review;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class ReviewSpecification {

    private ReviewSpecification() {}

    public static Specification<Review> belongsToShop(UUID shopId) {
        return (root, query, cb) -> {
            if (shopId == null) return null;
            Join<Review, OrderItem> orderItemJoin = root.join("orderItem", JoinType.INNER);
            Join<OrderItem, Order> orderJoin = orderItemJoin.join("order", JoinType.INNER);
            return cb.equal(orderJoin.get("shop").get("id"), shopId);
        };
    }

    public static Specification<Review> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Review> hasRating(Integer rating) {
        return (root, query, cb) -> rating == null ? null : cb.equal(root.get("rating"), rating);
    }

    public static Specification<Review> hasResponded(Boolean hasResponded) {
        return (root, query, cb) -> {
            if (hasResponded == null) return null;
            return hasResponded
                    ? cb.isNotNull(root.get("sellerResponse"))
                    : cb.isNull(root.get("sellerResponse"));
        };
    }

    public static Specification<Review> hasMedia(Boolean hasMedia) {
        return (root, query, cb) -> {
            if (hasMedia == null) return null;
            return hasMedia
                    ? cb.isNotEmpty(root.get("mediaList"))
                    : cb.isEmpty(root.get("mediaList"));
        };
    }

    public static Specification<Review> createdBetween(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            if (fromDate == null && toDate == null) return null;
            if (fromDate != null && toDate != null) {
                LocalDateTime start = fromDate.atStartOfDay();
                LocalDateTime end = toDate.atTime(LocalTime.MAX);
                return cb.between(root.get("createdAt"), start, end);
            }
            if (fromDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay());
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(LocalTime.MAX));
        };
    }

    public static Specification<Review> hasProductId(UUID productId) {
        return (root, query, cb) -> {
            if (productId == null) return null;
            Join<Review, OrderItem> orderItemJoin = root.join("orderItem", JoinType.INNER);
            Join<OrderItem, ProductVariant> variantJoin = orderItemJoin.join("productVariant", JoinType.INNER);
            return cb.equal(variantJoin.get("product").get("id"), productId);
        };
    }

    public static Specification<Review> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String escaped = keyword.trim().toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            Join<Review, OrderItem> orderItemJoin = root.join("orderItem", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("comment")), pattern, '\\'),
                    cb.like(cb.lower(orderItemJoin.get("productNameSnapshot")), pattern, '\\')
            );
        };
    }

    public static Specification<Review> orderByHelpfulCount(org.springframework.data.domain.Sort.Direction direction) {
        return (root, query, cb) -> {
            if (Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
                return null;
            }
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.techhub.model.entity.ReviewHelpful> helpfulRoot = subquery.from(com.techhub.model.entity.ReviewHelpful.class);
            subquery.select(cb.count(helpfulRoot));
            subquery.where(cb.equal(helpfulRoot.get("review"), root));

            if (direction == org.springframework.data.domain.Sort.Direction.ASC) {
                query.orderBy(cb.asc(subquery), cb.desc(root.get("createdAt")));
            } else {
                query.orderBy(cb.desc(subquery), cb.desc(root.get("createdAt")));
            }
            return null;
        };
    }
}
