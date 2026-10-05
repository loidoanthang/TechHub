package com.techhub.repository;

import com.techhub.model.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    Optional<Category> findByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") UUID id);
}

