package com.techhub.specification;

import com.techhub.model.entity.User;
import com.techhub.model.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> filterUsers(String keyword, UserStatus status, Boolean emailVerified) {
        return Specification.where(hasStatus(status))
                .and(hasEmailVerified(emailVerified))
                .and(hasKeyword(keyword));
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> hasEmailVerified(Boolean emailVerified) {
        return (root, query, cb) -> emailVerified == null ? null : cb.equal(root.get("emailVerified"), emailVerified);
    }

    public static Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            );
        };
    }
}
