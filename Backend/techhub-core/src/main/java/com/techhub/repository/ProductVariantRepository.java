package com.techhub.repository;

import com.techhub.model.entity.ProductVariant;
import com.techhub.model.enums.ProductVariantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    @EntityGraph(attributePaths = {
            "product",
            "product.shop",
            "product.shop.seller",
            "product.shop.seller.user",
            "product.category"
    })
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.isDeleted = false")
    Optional<ProductVariant> findByIdWithDetails(@Param("id") UUID id);

    List<ProductVariant> findByProductId(UUID productId);
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.isDeleted = false ORDER BY pv.createdAt ASC")
    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(@Param("productId") UUID productId);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);
    boolean existsByProductIdAndSku(UUID productId, String sku);

    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN TRUE ELSE FALSE END FROM ProductVariant pv WHERE pv.product.id = :productId AND LOWER(pv.sku) = LOWER(:sku) AND pv.isDeleted = false")
    boolean existsByProductIdAndSkuIgnoreCase(@Param("productId") UUID productId, @Param("sku") String sku);

    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN TRUE ELSE FALSE END FROM ProductVariant pv WHERE pv.product.id = :productId AND LOWER(pv.sku) = LOWER(:sku) AND pv.id != :id AND pv.isDeleted = false")
    boolean existsByProductIdAndSkuIgnoreCaseAndIdNot(@Param("productId") UUID productId, @Param("sku") String sku, @Param("id") UUID id);
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.isDeleted = false")
    long countByProductId(@Param("productId") UUID productId);

    long countByProductIdAndIsDeletedFalse(UUID productId);

    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.status = :status AND pv.isDeleted = false")
    long countByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ProductVariantStatus status);

    long countByProductIdAndStatusAndIsDeletedFalse(UUID productId, ProductVariantStatus status);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.status = :status AND pv.isDeleted = false ORDER BY pv.price ASC, pv.createdAt ASC")
    List<ProductVariant> findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(@Param("productId") UUID productId, @Param("status") ProductVariantStatus status);

    @Query("""
        SELECT pv.product.id, COUNT(pv.id), MIN(pv.price), MAX(pv.price), COALESCE(SUM(pv.stockQuantity), 0)
        FROM ProductVariant pv
        WHERE pv.product.id IN :productIds AND pv.isDeleted = false
        GROUP BY pv.product.id
    """)
    List<Object[]> aggregateVariantMetricsByProductIds(@Param("productIds") List<UUID> productIds);

    @Query("""
        SELECT pv.product.id, COUNT(pv.id), MIN(pv.price), MAX(pv.price), COALESCE(SUM(pv.stockQuantity), 0)
        FROM ProductVariant pv
        WHERE pv.product.id IN :productIds AND pv.status = com.techhub.model.enums.ProductVariantStatus.ACTIVE AND pv.isDeleted = false
        GROUP BY pv.product.id
    """)
    List<Object[]> aggregateActiveVariantMetricsByProductIds(@Param("productIds") List<UUID> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id IN :ids ORDER BY pv.id ASC")
    List<ProductVariant> findAllByIdInForUpdate(@Param("ids") List<UUID> ids);
}
