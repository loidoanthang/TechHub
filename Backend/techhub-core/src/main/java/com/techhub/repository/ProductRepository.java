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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "shop", "shop.seller", "shop.seller.user"})
    Optional<Product> findBySlug(String slug);
    boolean existsByCategoryId(UUID categoryId);
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
}

