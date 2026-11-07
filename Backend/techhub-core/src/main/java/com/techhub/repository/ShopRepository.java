package com.techhub.repository;

import com.techhub.model.entity.Shop;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID>, JpaSpecificationExecutor<Shop> {

    Optional<Shop> findBySellerId(UUID sellerId);

    Optional<Shop> findBySeller_SellerName(String sellerName);

    @EntityGraph(attributePaths = {"seller"})
    List<Shop> findBySellerIdIn(Collection<UUID> sellerIds);

    @Override
    @EntityGraph(attributePaths = {"seller", "seller.user"})
    Page<Shop> findAll(Specification<Shop> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "seller.user"})
    Optional<Shop> findWithDetailsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"seller", "seller.user"})
    @Query("SELECT s FROM Shop s WHERE s.id = :id")
    Optional<Shop> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT COUNT(s.id) FROM Shop s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate")
    long countNewShopsByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT s.status AS status, COUNT(s.id) AS count FROM Shop s GROUP BY s.status")
    java.util.List<com.techhub.model.projection.ShopStatusSummaryProjection> countShopsByStatusGrouped();
}
