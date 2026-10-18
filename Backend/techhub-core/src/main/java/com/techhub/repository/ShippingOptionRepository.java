package com.techhub.repository;

import com.techhub.model.entity.ShippingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingOptionRepository extends JpaRepository<ShippingOption, UUID> {

    Optional<ShippingOption> findByShopId(UUID shopId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"shop"})
    List<ShippingOption> findByShopIdIn(Collection<UUID> shopIds);
}
