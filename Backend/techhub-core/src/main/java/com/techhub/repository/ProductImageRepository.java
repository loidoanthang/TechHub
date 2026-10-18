package com.techhub.repository;

import com.techhub.model.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);

    @Query("SELECT pi.product.id, pi.imageUrl FROM ProductImage pi WHERE pi.product.id IN :productIds AND pi.displayOrder = 0")
    List<Object[]> findCoverImagesByProductIds(@Param("productIds") List<UUID> productIds);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds AND pi.displayOrder = :displayOrder")
    List<ProductImage> findByProductIdInAndDisplayOrder(
            @Param("productIds") java.util.Collection<UUID> productIds,
            @Param("displayOrder") int displayOrder
    );
}
