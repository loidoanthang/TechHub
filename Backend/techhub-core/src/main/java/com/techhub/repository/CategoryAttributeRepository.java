package com.techhub.repository;

import com.techhub.model.entity.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {
    List<CategoryAttribute> findByCategoryId(UUID categoryId);
    List<CategoryAttribute> findByCategoryIdOrderByCreatedAtAsc(UUID categoryId);
    boolean existsByCategoryIdAndNameIgnoreCase(UUID categoryId, String name);
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(UUID categoryId, String name, UUID id);
    long countByCategoryId(UUID categoryId);

    @Query("SELECT ca.category.id, COUNT(ca.id) FROM CategoryAttribute ca WHERE ca.category.id IN :categoryIds GROUP BY ca.category.id")
    List<Object[]> countByCategoryIdIn(@Param("categoryIds") List<UUID> categoryIds);
}
