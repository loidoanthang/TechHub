package com.techhub.repository;

import com.techhub.model.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    boolean existsByOrderItemId(UUID orderItemId);

    Optional<Review> findByOrderItemId(UUID orderItemId);

    @EntityGraph(attributePaths = {"orderItem", "orderItem.order", "orderItem.productVariant", "orderItem.productVariant.product", "user", "mediaList"})
    Optional<Review> findWithDetailsById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order", "orderItem.productVariant", "orderItem.productVariant.product", "user"})
    org.springframework.data.domain.Page<Review> findAll(
            org.springframework.data.jpa.domain.Specification<Review> spec,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Review r SET r.sellerResponse = :response, r.sellerResponseAt = :responseAt, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id AND r.sellerResponse IS NULL")
    int updateSellerResponse(
            @org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("response") String response,
            @org.springframework.data.repository.query.Param("responseAt") java.time.LocalDateTime responseAt
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT 
            COUNT(r.id),
            COALESCE(AVG(r.rating), 0.0),
            COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1L ELSE 0L END), 0L)
        FROM Review r
        JOIN r.orderItem oi
        JOIN oi.productVariant pv
        WHERE pv.product.id = :productId
          AND r.deletedAt IS NULL
    """)
    java.util.List<Object[]> aggregateRatingSummaryByProductId(@org.springframework.data.repository.query.Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(DISTINCT r.id)
        FROM Review r
        JOIN r.orderItem oi
        JOIN oi.productVariant pv
        JOIN r.mediaList rm
        WHERE pv.product.id = :productId
          AND r.deletedAt IS NULL
    """)
    long countMediaReviewsByProductId(@org.springframework.data.repository.query.Param("productId") UUID productId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
        UPDATE Review r 
        SET r.deletedAt = :deletedAt, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id AND r.deletedAt IS NULL
    """)
    int softDeleteReview(
            @org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("deletedAt") java.time.LocalDateTime deletedAt
    );
}
