package com.techhub.repository;

import com.techhub.model.entity.Product;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);
    boolean existsByIdAndIsDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"category", "shop", "shop.seller", "shop.seller.user"})
    Optional<Product> findBySlug(String slug);
    boolean existsByCategoryId(UUID categoryId);
    boolean existsByCategoryIdAndIsDeletedFalse(UUID categoryId);
    List<Product> findByShopId(UUID shopId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findByIdAndShopId(UUID id, UUID shopId);

    @Override
    @EntityGraph(attributePaths = {"category", "shop", "shop.seller"})
    Optional<Product> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"category", "shop", "shop.seller"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.approvalStatus = :draftStatus, p.visibilityStatus = :unpublishedStatus, p.updatedAt = CURRENT_TIMESTAMP WHERE p.category.id = :categoryId AND p.isDeleted = false")
    int hibernateProductsByCategoryId(
            @Param("categoryId") UUID categoryId,
            @Param("draftStatus") ProductApprovalStatus draftStatus,
            @Param("unpublishedStatus") ProductVisibilityStatus unpublishedStatus
    );

    @Query("""
        SELECT p FROM Product p 
        JOIN FETCH p.category c 
        LEFT JOIN FETCH p.shop s 
        WHERE c.id = :categoryId 
          AND p.id != :targetProductId 
          AND p.isDeleted = false 
          AND p.visibilityStatus = com.techhub.model.enums.ProductVisibilityStatus.PUBLISHED 
          AND p.approvalStatus = com.techhub.model.enums.ProductApprovalStatus.APPROVED 
          AND p.isSuspended = false 
          AND (s IS NULL OR s.status != com.techhub.model.enums.ShopStatus.BANNED) 
        ORDER BY 
          CASE WHEN LOWER(p.brandName) = LOWER(:brandName) THEN 0 ELSE 1 END, 
          p.createdAt DESC
    """)
    List<Product> findSimilarProducts(
            @Param("categoryId") UUID categoryId,
            @Param("targetProductId") UUID targetProductId,
            @Param("brandName") String brandName,
            Pageable pageable
    );

    @Query("""
        SELECT p.shop.id, COUNT(p.id)
        FROM Product p
        WHERE p.shop.id IN :shopIds
          AND p.isDeleted = false
          AND p.visibilityStatus = com.techhub.model.enums.ProductVisibilityStatus.PUBLISHED
          AND p.approvalStatus = com.techhub.model.enums.ProductApprovalStatus.APPROVED
          AND p.isSuspended = false
        GROUP BY p.shop.id
    """)
    List<Object[]> countActiveProductsByShopIdIn(@Param("shopIds") Collection<UUID> shopIds);

    @Query("""
        SELECT p.shop.id, COUNT(p.id)
        FROM Product p
        WHERE p.shop.id IN :shopIds
          AND p.isDeleted = false
        GROUP BY p.shop.id
    """)
    List<Object[]> countTotalProductsByShopIdIn(@Param("shopIds") Collection<UUID> shopIds);

    long countByShopIdAndIsDeletedFalse(UUID shopId);

    @Query("""
        SELECT COUNT(p.id) FROM Product p 
        WHERE p.shop.id = :shopId 
          AND p.isDeleted = false 
          AND p.visibilityStatus = com.techhub.model.enums.ProductVisibilityStatus.PUBLISHED 
          AND p.approvalStatus = com.techhub.model.enums.ProductApprovalStatus.APPROVED 
          AND p.isSuspended = false
    """)
    long countActiveProductsByShopId(@Param("shopId") UUID shopId);
}

