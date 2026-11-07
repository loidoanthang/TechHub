package com.techhub.repository;

import com.techhub.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    @Query("SELECT COUNT(n.id) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.deletedAt IS NULL")
    long countByUserIdAndIsReadFalseAndDeletedAtIsNull(@Param("userId") UUID userId);

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false AND n.deletedAt IS NULL")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO notifications (id, user_id, type, title, content, is_read, reference_type, reference_id, created_by, created_at) " +
            "SELECT gen_random_uuid(), u.id, :type, :title, :content, false, :referenceType, :referenceId, :adminId, NOW() " +
            "FROM users u WHERE u.status = 'ACTIVE' AND u.deleted_at IS NULL", nativeQuery = true)
    int broadcastToAllUsers(@Param("type") String type,
                            @Param("title") String title,
                            @Param("content") String content,
                            @Param("referenceType") String referenceType,
                            @Param("referenceId") UUID referenceId,
                            @Param("adminId") UUID adminId);

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO notifications (id, user_id, type, title, content, is_read, reference_type, reference_id, created_by, created_at) " +
            "SELECT gen_random_uuid(), u.id, :type, :title, :content, false, :referenceType, :referenceId, :adminId, NOW() " +
            "FROM users u WHERE u.status = 'ACTIVE' AND u.deleted_at IS NULL " +
            "AND EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'BUYER')", nativeQuery = true)
    int broadcastToAllBuyers(@Param("type") String type,
                             @Param("title") String title,
                             @Param("content") String content,
                             @Param("referenceType") String referenceType,
                             @Param("referenceId") UUID referenceId,
                             @Param("adminId") UUID adminId);

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO notifications (id, user_id, type, title, content, is_read, reference_type, reference_id, created_by, created_at) " +
            "SELECT gen_random_uuid(), u.id, :type, :title, :content, false, :referenceType, :referenceId, :adminId, NOW() " +
            "FROM users u WHERE u.status = 'ACTIVE' AND u.deleted_at IS NULL " +
            "AND EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'SELLER')", nativeQuery = true)
    int broadcastToAllSellers(@Param("type") String type,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("referenceType") String referenceType,
                              @Param("referenceId") UUID referenceId,
                              @Param("adminId") UUID adminId);
}
