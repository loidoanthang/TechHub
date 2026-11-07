package com.techhub.repository;

import com.techhub.model.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @EntityGraph(attributePaths = {"product", "product.shop"})
    Optional<WishlistItem> findByUserIdAndProductId(UUID userId, UUID productId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.userId = :userId AND w.product.id = :productId")
    int deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);

    @EntityGraph(attributePaths = {"product", "product.shop"})
    Page<WishlistItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
