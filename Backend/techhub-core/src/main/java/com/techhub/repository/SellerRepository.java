package com.techhub.repository;

import com.techhub.model.entity.Seller;
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

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID>, JpaSpecificationExecutor<Seller> {

    Optional<Seller> findByUserId(UUID userId);

    Optional<Seller> findBySellerName(String sellerName);

    boolean existsBySellerName(String sellerName);

    boolean existsByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Seller> findAll(Specification<Seller> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<Seller> findWithUserById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT s FROM Seller s WHERE s.id = :id")
    Optional<Seller> findWithUserForUpdateById(@Param("id") UUID id);
}

