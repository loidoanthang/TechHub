package com.techhub.repository;

import com.techhub.model.entity.ShopFollowing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface ShopFollowingRepository extends JpaRepository<ShopFollowing, UUID>, JpaSpecificationExecutor<ShopFollowing> {

    Optional<ShopFollowing> findByUserIdAndShopId(UUID userId, UUID shopId);

    boolean existsByUserIdAndShopId(UUID userId, UUID shopId);

    long countByShopId(UUID shopId);

    @Modifying
    @Query("DELETE FROM ShopFollowing sf WHERE sf.userId = :userId AND sf.shop.id = :shopId")
    int deleteByUserIdAndShopId(@Param("userId") UUID userId, @Param("shopId") UUID shopId);

    @Query(value = """
        SELECT sf FROM ShopFollowing sf 
        JOIN FETCH sf.shop s 
        JOIN FETCH s.seller sel 
        WHERE sf.userId = :userId 
          AND s.status NOT IN (com.techhub.model.enums.ShopStatus.SUSPENDED, com.techhub.model.enums.ShopStatus.BANNED) 
        ORDER BY sf.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(sf) FROM ShopFollowing sf 
        JOIN sf.shop s 
        WHERE sf.userId = :userId 
          AND s.status NOT IN (com.techhub.model.enums.ShopStatus.SUSPENDED, com.techhub.model.enums.ShopStatus.BANNED)
        """)
    Page<ShopFollowing> findByUserIdWithShopAndSeller(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT sf.shop.id, COUNT(sf.id)
        FROM ShopFollowing sf
        WHERE sf.shop.id IN :shopIds
        GROUP BY sf.shop.id
    """)
    List<Object[]> countFollowersByShopIdIn(@Param("shopIds") Collection<UUID> shopIds);
}
