package com.techhub.specification;

import com.techhub.model.entity.Notification;
import com.techhub.model.enums.NotificationType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class NotificationSpecification {

    private NotificationSpecification() {}

    public static Specification<Notification> belongsToUser(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) return null;
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Notification> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Notification> hasReadStatus(Boolean isRead) {
        return (root, query, cb) -> isRead == null ? null : cb.equal(root.get("isRead"), isRead);
    }

    public static Specification<Notification> hasType(NotificationType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }
}
