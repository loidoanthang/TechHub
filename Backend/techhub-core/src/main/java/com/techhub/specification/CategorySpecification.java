package com.techhub.specification;

import com.techhub.model.entity.Category;
import com.techhub.model.enums.CategoryStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CategorySpecification {

    private CategorySpecification() {}

    public static Specification<Category> filterCategories(String keyword, CategoryStatus status) {
        return Specification.where(hasStatus(status))
                .and(hasKeyword(keyword));
    }

    public static Specification<Category> hasStatus(CategoryStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Category> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
