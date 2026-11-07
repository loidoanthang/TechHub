package com.techhub.repository;

import com.techhub.model.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @EntityGraph(attributePaths = {
            "productVariant",
            "product",
            "product.shop",
            "product.shop.seller",
            "product.shop.seller.user",
            "product.category"
    })
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId ORDER BY ci.createdAt DESC")
    List<CartItem> findByCartIdWithDetails(@Param("cartId") UUID cartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "cart",
            "productVariant",
            "product",
            "product.shop",
            "product.shop.seller",
            "product.shop.seller.user",
            "product.category"
    })
    @Query("SELECT ci FROM CartItem ci WHERE ci.id = :id AND ci.cart.user.id = :userId")
    Optional<CartItem> findByIdAndUserIdWithDetailsForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.id = :id AND ci.cart.user.id = :userId")
    Optional<CartItem> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);

    @EntityGraph(attributePaths = {"productVariant"})
    List<CartItem> findByCartId(UUID cartId);

    int countByCartId(UUID cartId);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId")
    int countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    int sumQuantityByCartId(@Param("cartId") UUID cartId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.id IN :itemIds")
    void deleteByCartIdAndIdIn(@Param("cartId") UUID cartId, @Param("itemIds") List<UUID> itemIds);

    @EntityGraph(attributePaths = {
            "cart",
            "productVariant",
            "product",
            "product.shop",
            "product.shop.seller",
            "product.shop.seller.user",
            "product.category"
    })
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.id IN :itemIds")
    List<CartItem> findByUserIdAndIdInWithDetails(@Param("userId") UUID userId, @Param("itemIds") List<UUID> itemIds);
}
