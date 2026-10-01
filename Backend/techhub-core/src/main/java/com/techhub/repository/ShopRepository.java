package com.techhub.repository;

import com.techhub.model.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID>, JpaSpecificationExecutor<Shop> {

    Optional<Shop> findBySellerId(UUID sellerId);

    Optional<Shop> findBySeller_SellerName(String sellerName);

    List<Shop> findBySellerIdIn(Collection<UUID> sellerIds);
}
