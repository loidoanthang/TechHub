package com.techhub.service.category.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.CategoryAttribute;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.repository.CategoryAttributeRepository;
import com.techhub.repository.CategoryRepository;
import com.techhub.service.category.CategoryService;
import com.techhub.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategories(String keyword, String sortBy, String sortDir) {
        log.info("Public fetching categories: keyword='{}', sortBy='{}', sortDir='{}'", keyword, sortBy, sortDir);

        // 1. Sanitize sort parameters
        String sanitizedSortBy = ("createdAt".equalsIgnoreCase(sortBy)) ? "createdAt" : "name";
        Sort.Direction direction = ("desc".equalsIgnoreCase(sortDir)) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sanitizedSortBy);

        // 2. Build Specification: Always enforce status = ACTIVE (BR-11A Active Category Filter Guard)
        Specification<Category> spec = CategorySpecification.filterCategories(keyword, CategoryStatus.ACTIVE);

        // 3. Query categories
        List<Category> categories = categoryRepository.findAll(spec, sort);

        // 4. Handle empty result
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. Batch count attributes in 1 query (O(1) SQL - zero N+1 query)
        List<UUID> categoryIds = categories.stream().map(Category::getId).toList();
        List<Object[]> countResults = categoryAttributeRepository.countByCategoryIdIn(categoryIds);
        Map<UUID, Long> countMap = countResults.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // 6. Map to CategorySummaryResponse
        return categories.stream()
                .map(cat -> new CategorySummaryResponse(
                        cat.getId(),
                        cat.getName(),
                        cat.getDescription(),
                        cat.getStatus(),
                        countMap.getOrDefault(cat.getId(), 0L),
                        cat.getCreatedAt(),
                        cat.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getCategoryAttributes(UUID categoryId) {
        log.info("Public fetching attributes for categoryId='{}'", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getStatus() == CategoryStatus.INACTIVE) {
            log.warn("Category {} is INACTIVE. Public access blocked (BR-11A).", categoryId);
            throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        }

        List<CategoryAttribute> attributes = categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId);

        return attributes.stream()
                .map(attr -> new CategoryAttributeResponse(
                        attr.getId(),
                        attr.getName(),
                        attr.getDataType(),
                        attr.getOptions() != null ? attr.getOptions() : Collections.emptyList(),
                        attr.getSampleValue(),
                        attr.getCreatedAt(),
                        attr.getUpdatedAt()
                ))
                .toList();
    }
}
