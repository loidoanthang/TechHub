package com.techhub.repository;

import com.techhub.model.entity.RecentlyViewedProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, UUID> {

    Optional<RecentlyViewedProduct> findByUserIdAndProductId(UUID userId, UUID productId);

    long countByUserId(UUID userId);

    @Modifying
    @Query(value = """
        INSERT INTO recently_viewed_products (id, user_id, product_id, viewed_at)
        VALUES (:id, :userId, :productId, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, product_id)
        DO UPDATE SET viewed_at = CURRENT_TIMESTAMP
    """, nativeQuery = true)
    int upsertProductView(@Param("id") UUID id, @Param("userId") UUID userId, @Param("productId") UUID productId);

    @Modifying
    @Query(value = """
        DELETE FROM recently_viewed_products
        WHERE id IN (
            SELECT id FROM recently_viewed_products
            WHERE user_id = :userId
            ORDER BY viewed_at DESC, id DESC
            OFFSET 10
        )
    """, nativeQuery = true)
    int evictExcessRecentlyViewed(@Param("userId") UUID userId);

    @Query("SELECT r FROM RecentlyViewedProduct r JOIN FETCH r.product p LEFT JOIN FETCH p.shop WHERE r.userId = :userId ORDER BY r.viewedAt DESC")
    List<RecentlyViewedProduct> findTop10ByUserIdOrderByViewedAtDesc(@Param("userId") UUID userId);

    @Query("""
        SELECT r FROM RecentlyViewedProduct r 
        JOIN FETCH r.product p 
        LEFT JOIN FETCH p.shop s 
        WHERE r.userId = :userId 
          AND p.isDeleted = false 
          AND p.visibilityStatus = com.techhub.model.enums.ProductVisibilityStatus.PUBLISHED 
          AND p.approvalStatus = com.techhub.model.enums.ProductApprovalStatus.APPROVED 
          AND p.isSuspended = false 
          AND (s IS NULL OR s.status != com.techhub.model.enums.ShopStatus.BANNED) 
        ORDER BY r.viewedAt DESC, r.id DESC
    """)
    List<RecentlyViewedProduct> findActiveRecentlyViewedByUserId(@Param("userId") UUID userId, Pageable pageable);
}
