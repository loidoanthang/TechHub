package com.techhub.specification;

import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.SellerVerificationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class SellerSpecification {

    private SellerSpecification() {}

    public static Specification<Seller> filterSellers(String keyword, SellerVerificationStatus status) {
        return Specification.where(hasStatus(status))
                .and(hasKeyword(keyword));
    }

    public static Specification<Seller> hasStatus(SellerVerificationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("verificationStatus"), status);
    }

    public static Specification<Seller> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            // Join user for searching user fields
            Join<Seller, User> userJoin = root.join("user", JoinType.LEFT);

            // Subquery for searching shop name
            Subquery<UUID> shopSubquery = query.subquery(UUID.class);
            Root<Shop> shopRoot = shopSubquery.from(Shop.class);
            shopSubquery.select(shopRoot.get("seller").get("id"))
                    .where(cb.like(cb.lower(shopRoot.get("name")), pattern));

            return cb.or(
                    cb.like(cb.lower(root.get("sellerName")), pattern),
                    cb.like(cb.lower(userJoin.get("email")), pattern),
                    cb.like(cb.lower(userJoin.get("phone")), pattern),
                    cb.like(cb.lower(userJoin.get("firstName")), pattern),
                    cb.like(cb.lower(userJoin.get("lastName")), pattern),
                    cb.in(root.get("id")).value(shopSubquery)
            );
        };
    }
}
