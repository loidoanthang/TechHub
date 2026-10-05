package com.techhub.repository;

import com.techhub.model.entity.ProductVariant;
import com.techhub.model.enums.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);
    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(UUID productId);
    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);
    boolean existsByProductIdAndSku(UUID productId, String sku);
    boolean existsByProductIdAndSkuIgnoreCase(UUID productId, String sku);
    boolean existsByProductIdAndSkuIgnoreCaseAndIdNot(UUID productId, String sku, UUID id);
    long countByProductId(UUID productId);
    long countByProductIdAndStatus(UUID productId, ProductVariantStatus status);
    List<ProductVariant> findByProductIdAndStatusOrderByPriceAscCreatedAtAsc(UUID productId, ProductVariantStatus status);

    @Query("""
        SELECT pv.product.id, COUNT(pv.id), MIN(pv.price), MAX(pv.price), COALESCE(SUM(pv.stockQuantity), 0)
        FROM ProductVariant pv
        WHERE pv.product.id IN :productIds
        GROUP BY pv.product.id
    """)
    List<Object[]> aggregateVariantMetricsByProductIds(@Param("productIds") List<UUID> productIds);

    @Query("""
        SELECT pv.product.id, COUNT(pv.id), MIN(pv.price), MAX(pv.price), COALESCE(SUM(pv.stockQuantity), 0)
        FROM ProductVariant pv
        WHERE pv.product.id IN :productIds AND pv.status = com.techhub.model.enums.ProductVariantStatus.ACTIVE
        GROUP BY pv.product.id
    """)
    List<Object[]> aggregateActiveVariantMetricsByProductIds(@Param("productIds") List<UUID> productIds);
}
