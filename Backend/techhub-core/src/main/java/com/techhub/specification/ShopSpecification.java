package com.techhub.specification;

import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ShopStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ShopSpecification {

    private ShopSpecification() {}

    public static Specification<Shop> filterShops(String keyword, ShopStatus status) {
        return Specification.where(hasStatus(status))
                .and(hasKeyword(keyword));
    }

    public static Specification<Shop> hasStatus(ShopStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Shop> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String sanitized = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + sanitized.toLowerCase() + "%";

            Join<Shop, Seller> sellerJoin = root.join("seller", JoinType.LEFT);
            Join<Seller, User> userJoin = sellerJoin.join("user", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(sellerJoin.get("sellerName")), pattern, '\\'),
                    cb.like(cb.lower(userJoin.get("email")), pattern, '\\'),
                    cb.like(cb.lower(userJoin.get("phone")), pattern, '\\'),
                    cb.like(cb.lower(userJoin.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(userJoin.get("lastName")), pattern, '\\')
            );
        };
    }
}
