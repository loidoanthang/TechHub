package com.techhub.service.admin.impl;

import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateCategoryAttributeRequest;
import com.techhub.model.dto.request.CreateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.model.dto.request.UpdateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryStatusRequest;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategoryDetailResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.CategoryAttribute;
import com.techhub.model.enums.CategoryAttributeDataType;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.repository.CategoryAttributeRepository;
import com.techhub.repository.CategoryRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.service.admin.AdminCategoryService;
import com.techhub.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "name", "updatedAt"
    );

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategorySummaryResponse> getCategories(
            String keyword,
            CategoryStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // 1. Whitelist & validate sorting
        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy))
                ? sortBy
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 2. Safe pagination bounds & deterministic tie-breaker sorting
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);
        Sort sort = Sort.by(direction, validSortBy).and(Sort.by(direction, "id"));
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        // 3. Dynamic criteria query
        Specification<Category> spec = CategorySpecification.filterCategories(keyword, status);
        Page<Category> categoryPage = categoryRepository.findAll(spec, pageable);

        if (categoryPage.isEmpty()) {
            return PageResponse.from(categoryPage, List.of());
        }

        // 4. Batch query attribute counts (Zero N+1 Query)
        List<UUID> categoryIds = categoryPage.getContent().stream()
                .map(Category::getId)
                .toList();

        List<Object[]> countResults = categoryAttributeRepository.countByCategoryIdIn(categoryIds);
        Map<UUID, Long> countMap = countResults.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // 5. Map to CategorySummaryResponse
        List<CategorySummaryResponse> summaryResponses = categoryPage.getContent().stream()
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

        return PageResponse.from(categoryPage, summaryResponses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryDetailResponse createCategory(CreateCategoryRequest request) {
        String trimmedName = request.name() != null ? request.name().trim() : "";
        String trimmedDesc = (request.description() != null && !request.description().trim().isEmpty())
                ? request.description().trim()
                : null;

        // 1. Kiểm tra tính duy nhất của tên danh mục (không phân biệt hoa/thường)
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }

        // 2. Validate số lượng thuộc tính kỹ thuật mẫu (BR-11A: 1 <= count <= 50)
        List<CreateCategoryAttributeRequest> attributeRequests = request.attributes();
        if (attributeRequests == null || attributeRequests.isEmpty() || attributeRequests.size() > 50) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED);
        }

        // 3. Kiểm tra tính hợp lệ, không trùng lặp và tiền xử lý thuộc tính (Clean once)
        Set<String> seenAttributeNames = new HashSet<>();
        List<CategoryAttribute> attributesToSave = new java.util.ArrayList<>(attributeRequests.size());

        for (CreateCategoryAttributeRequest attrReq : attributeRequests) {
            String attrName = attrReq.name() != null ? attrReq.name().trim() : "";
            if (!seenAttributeNames.add(attrName.toLowerCase())) {
                throw new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_NAME);
            }

            List<String> options = null;
            if (attrReq.dataType() == CategoryAttributeDataType.SELECT) {
                if (attrReq.options() == null || attrReq.options().isEmpty()) {
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
                }
                options = attrReq.options().stream()
                        .filter(opt -> opt != null && !opt.trim().isEmpty())
                        .map(String::trim)
                        .distinct()
                        .toList();
                if (options.isEmpty() || options.size() > 100) {
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
                }
                for (String opt : options) {
                    if (opt.length() > 100) {
                        throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
                    }
                }
            }

            String sampleVal = (attrReq.sampleValue() != null && !attrReq.sampleValue().trim().isEmpty())
                    ? attrReq.sampleValue().trim()
                    : null;

            attributesToSave.add(CategoryAttribute.builder()
                    .name(attrName)
                    .dataType(attrReq.dataType())
                    .options(options)
                    .sampleValue(sampleVal)
                    .build());
        }

        // 4. Lưu Category Entity
        Category category = Category.builder()
                .name(trimmedName)
                .description(trimmedDesc)
                .status(CategoryStatus.ACTIVE)
                .build();
        Category savedCategory = categoryRepository.save(category);

        // 5. Gán Category reference và lưu danh sách CategoryAttribute Entities
        for (CategoryAttribute attr : attributesToSave) {
            attr.setCategory(savedCategory);
        }
        List<CategoryAttribute> savedAttributes = categoryAttributeRepository.saveAll(attributesToSave);

        log.info("Admin created category: id={}, name='{}', attributesCount={}",
                savedCategory.getId(), savedCategory.getName(), savedAttributes.size());

        // 6. Map sang Response DTO
        List<CategoryAttributeResponse> attributeResponses = savedAttributes.stream()
                .map(attr -> new CategoryAttributeResponse(
                        attr.getId(),
                        attr.getName(),
                        attr.getDataType(),
                        attr.getOptions(),
                        attr.getSampleValue(),
                        attr.getCreatedAt(),
                        attr.getUpdatedAt()
                ))
                .toList();

        return new CategoryDetailResponse(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getDescription(),
                savedCategory.getStatus(),
                attributeResponses,
                savedCategory.getCreatedAt(),
                savedCategory.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDetailResponse getCategoryDetail(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        List<CategoryAttribute> attributes = categoryAttributeRepository
                .findByCategoryIdOrderByCreatedAtAsc(categoryId);

        List<CategoryAttributeResponse> attributeResponses = attributes.stream()
                .map(attr -> new CategoryAttributeResponse(
                        attr.getId(),
                        attr.getName(),
                        attr.getDataType(),
                        attr.getOptions(),
                        attr.getSampleValue(),
                        attr.getCreatedAt(),
                        attr.getUpdatedAt()
                ))
                .toList();

        return new CategoryDetailResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                attributeResponses,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryDetailResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        String trimmedName = request.name();
        String trimmedDesc = request.description();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }

        category.setName(trimmedName);
        category.setDescription(trimmedDesc);
        Category savedCategory = categoryRepository.save(category);

        List<CategoryAttribute> attributes = categoryAttributeRepository
                .findByCategoryIdOrderByCreatedAtAsc(categoryId);

        List<CategoryAttributeResponse> attributeResponses = attributes.stream()
                .map(attr -> new CategoryAttributeResponse(
                        attr.getId(),
                        attr.getName(),
                        attr.getDataType(),
                        attr.getOptions(),
                        attr.getSampleValue(),
                        attr.getCreatedAt(),
                        attr.getUpdatedAt()
                ))
                .toList();

        log.info("Admin updated category: id={}, name='{}', status={}",
                savedCategory.getId(), savedCategory.getName(), savedCategory.getStatus());

        return new CategoryDetailResponse(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getDescription(),
                savedCategory.getStatus(),
                attributeResponses,
                savedCategory.getCreatedAt(),
                savedCategory.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryDetailResponse updateCategoryStatus(UUID categoryId, UpdateCategoryStatusRequest request) {
        log.info("Processing updateCategoryStatus for categoryId={}, targetStatus={}", categoryId, request.status());

        // 1. Find Category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Idempotency Check (BR-11A)
        if (category.getStatus() == request.status()) {
            log.info("Category {} is already in status {}. Returning directly (idempotent).", categoryId, request.status());
            return buildCategoryDetailResponse(category);
        }

        // 3. Update Category Status
        category.setStatus(request.status());
        Category savedCategory = categoryRepository.save(category);

        // 4. Forced Hibernation Policy: When category is INACTIVE, hibernate all its products to DRAFT & UNPUBLISHED
        if (request.status() == CategoryStatus.INACTIVE) {
            int hibernatedCount = productRepository.hibernateProductsByCategoryId(
                    categoryId,
                    ProductApprovalStatus.DRAFT,
                    ProductVisibilityStatus.UNPUBLISHED
            );
            log.info("Hibernated {} products to DRAFT and UNPUBLISHED due to category {} inactivation",
                    hibernatedCount, categoryId);
        }

        log.info("Category {} status successfully updated to {}", categoryId, request.status());
        return buildCategoryDetailResponse(savedCategory);
    }

    private CategoryDetailResponse buildCategoryDetailResponse(Category category) {
        List<CategoryAttribute> attributes = categoryAttributeRepository
                .findByCategoryIdOrderByCreatedAtAsc(category.getId());

        List<CategoryAttributeResponse> attributeResponses = attributes.stream()
                .map(attr -> new CategoryAttributeResponse(
                        attr.getId(),
                        attr.getName(),
                        attr.getDataType(),
                        attr.getOptions(),
                        attr.getSampleValue(),
                        attr.getCreatedAt(),
                        attr.getUpdatedAt()
                ))
                .toList();

        return new CategoryDetailResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getStatus(),
                attributeResponses,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAttributeResponse createCategoryAttribute(UUID categoryId, CreateCategoryAttributeRequest request) {
        log.info("Admin creating attribute for category id={}: name='{}', dataType={}",
                categoryId, request.name(), request.dataType());

        // 1. Kiểm tra Category tồn tại
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Kiểm tra giới hạn tối đa 50 thuộc tính (BR-11A)
        long currentCount = categoryAttributeRepository.countByCategoryId(categoryId);
        if (currentCount >= 50) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED);
        }

        // 3. Kiểm tra trùng tên thuộc tính trong danh mục (BR-09)
        String trimmedName = request.name();
        if (categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, trimmedName)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_NAME);
        }

        // 4. Validate và chuẩn hóa options cho kiểu SELECT
        List<String> options = null;
        if (request.dataType() == CategoryAttributeDataType.SELECT) {
            if (request.options() == null || request.options().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
            }
            options = request.options().stream()
                    .filter(opt -> opt != null && !opt.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (options.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
            }
            for (String opt : options) {
                if (opt.length() > 100) {
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
                }
            }
        }

        // 5. Chuẩn hóa sampleValue
        String sampleValue = request.sampleValue();

        // 6. Xây dựng và lưu CategoryAttribute entity
        CategoryAttribute attribute = CategoryAttribute.builder()
                .category(category)
                .name(trimmedName)
                .dataType(request.dataType())
                .options(options)
                .sampleValue(sampleValue)
                .build();

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(attribute);

        log.info("Admin created attribute for category id={}: attrId={}, name='{}', dataType={}",
                categoryId, savedAttribute.getId(), savedAttribute.getName(), savedAttribute.getDataType());

        // 7. Map sang CategoryAttributeResponse
        return new CategoryAttributeResponse(
                savedAttribute.getId(),
                savedAttribute.getName(),
                savedAttribute.getDataType(),
                savedAttribute.getOptions(),
                savedAttribute.getSampleValue(),
                savedAttribute.getCreatedAt(),
                savedAttribute.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAttributeResponse updateCategoryAttribute(
            UUID categoryId, UUID attributeId, UpdateCategoryAttributeRequest request
    ) {
        // 1. Kiểm tra danh mục tồn tại
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. Kiểm tra thuộc tính tồn tại và thuộc về danh mục chỉ định
        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND));

        if (!attribute.getCategory().getId().equals(categoryId)) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }

        // 3. Kiểm tra trùng tên với thuộc tính khác trong danh mục (BR-09)
        String trimmedName = request.name();
        if (categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, trimmedName, attributeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_NAME);
        }

        // 4. Validate và chuẩn hóa options cho kiểu SELECT
        List<String> options = null;
        if (request.dataType() == CategoryAttributeDataType.SELECT) {
            if (request.options() == null || request.options().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
            }
            options = request.options().stream()
                    .filter(opt -> opt != null && !opt.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (options.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
            }
            for (String opt : options) {
                if (opt.length() > 100) {
                    throw new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS);
                }
            }
        }

        // 5. Cập nhật các trường thông tin
        attribute.setName(trimmedName);
        attribute.setDataType(request.dataType());
        attribute.setOptions(options);
        attribute.setSampleValue(request.sampleValue());

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(attribute);

        log.info("Admin updated attribute for category id={}: attrId={}, name='{}', dataType={}",
                categoryId, savedAttribute.getId(), savedAttribute.getName(), savedAttribute.getDataType());

        // 6. Map sang CategoryAttributeResponse
        return new CategoryAttributeResponse(
                savedAttribute.getId(),
                savedAttribute.getName(),
                savedAttribute.getDataType(),
                savedAttribute.getOptions(),
                savedAttribute.getSampleValue(),
                savedAttribute.getCreatedAt(),
                savedAttribute.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryAttribute(UUID categoryId, UUID attributeId) {
        // 1. Kiểm tra danh mục tồn tại và khóa Aggregate Root chống race condition (Pessimistic Write Lock)
        categoryRepository.findByIdForUpdate(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Kiểm tra thuộc tính tồn tại và thuộc về danh mục chỉ định
        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND));

        if (!attribute.getCategory().getId().equals(categoryId)) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }

        // 3. Kiểm tra bất biến thuộc tính cuối cùng của danh mục (BR-11A)
        long currentCount = categoryAttributeRepository.countByCategoryId(categoryId);
        if (currentCount <= 1) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_LAST_CATEGORY_ATTRIBUTE);
        }

        // 4. Xóa thuộc tính
        categoryAttributeRepository.delete(attribute);

        log.info("Admin deleted attribute id={} from category id={}", attributeId, categoryId);
    }
}
