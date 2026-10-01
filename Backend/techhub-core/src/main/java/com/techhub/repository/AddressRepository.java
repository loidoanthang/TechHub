package com.techhub.repository;

import com.techhub.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    long countByUserIdAndShopIdIsNull(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.shopId IS NULL")
    void resetDefaultAddressesByUserId(@Param("userId") UUID userId);

    Optional<Address> findFirstByUserIdAndShopIdIsNullOrderByUpdatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUserIdAndShopIdIsNull(UUID id, UUID userId);

    @Query("SELECT a FROM Address a WHERE a.user.id IN :userIds AND a.isDefault = true AND a.shopId IS NULL")
    List<Address> findDefaultAddressesByUserIds(@Param("userIds") Collection<UUID> userIds);

    Optional<Address> findByShopId(UUID shopId);
}
