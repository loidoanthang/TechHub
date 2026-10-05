package com.techhub.service.category;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.CategoryAttribute;
import com.techhub.model.enums.CategoryAttributeDataType;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.repository.CategoryAttributeRepository;
import com.techhub.repository.CategoryRepository;
import com.techhub.service.category.impl.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CategoryServiceImpl (GET /api/categories)")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryAttributeRepository categoryAttributeRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("TC-01: Lấy danh mục thành công - Chỉ trả về danh mục ACTIVE")
    void getCategories_Success_ReturnActiveOnly() {
        UUID cat1Id = UUID.randomUUID();
        UUID cat2Id = UUID.randomUUID();

        Category cat1 = Category.builder()
                .id(cat1Id)
                .name("Bàn phím cơ")
                .description("Bàn phím cơ cao cấp")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category cat2 = Category.builder()
                .id(cat2Id)
                .name("Laptop")
                .description("Laptop văn phòng & gaming")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(cat1, cat2));

        List<Object[]> counts = new ArrayList<>();
        counts.add(new Object[]{cat1Id, 4L});
        counts.add(new Object[]{cat2Id, 6L});
        when(categoryAttributeRepository.countByCategoryIdIn(anyList()))
                .thenReturn(counts);

        List<CategorySummaryResponse> result = categoryService.getCategories(null, "name", "asc");

        assertNotNull(result);
        assertEquals(2, result.size());

        CategorySummaryResponse r1 = result.get(0);
        assertEquals(cat1Id, r1.id());
        assertEquals("Bàn phím cơ", r1.name());
        assertEquals(CategoryStatus.ACTIVE, r1.status());
        assertEquals(4L, r1.attributesCount());

        CategorySummaryResponse r2 = result.get(1);
        assertEquals(cat2Id, r2.id());
        assertEquals("Laptop", r2.name());
        assertEquals(CategoryStatus.ACTIVE, r2.status());
        assertEquals(6L, r2.attributesCount());
    }

    @Test
    @DisplayName("TC-02: Không có danh mục nào phù hợp - Trả về List rỗng, không gọi query đếm")
    void getCategories_EmptyResult_ReturnEmptyList() {
        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<CategorySummaryResponse> result = categoryService.getCategories(null, "name", "asc");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryAttributeRepository, never()).countByCategoryIdIn(any());
    }

    @Test
    @DisplayName("TC-03: Tìm kiếm theo keyword - Trả về danh mục khớp từ khóa")
    void getCategories_WithKeyword_ReturnMatchingActive() {
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder()
                .id(catId)
                .name("Laptop Gaming")
                .description("Laptop chuyên đồ họa")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(cat));

        List<Object[]> counts = Collections.singletonList(new Object[]{catId, 5L});
        when(categoryAttributeRepository.countByCategoryIdIn(anyList()))
                .thenReturn(counts);

        List<CategorySummaryResponse> result = categoryService.getCategories("laptop", "name", "asc");

        assertEquals(1, result.size());
        assertEquals("Laptop Gaming", result.get(0).name());
        assertEquals(5L, result.get(0).attributesCount());
    }

    @Test
    @DisplayName("TC-04: Keyword không khớp danh mục nào - Trả về List rỗng")
    void getCategories_KeywordNoMatch_ReturnEmptyList() {
        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<CategorySummaryResponse> result = categoryService.getCategories("khong_ton_tai", "name", "asc");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-05: Ánh xạ chính xác attributesCount kể cả khi danh mục không có thuộc tính mẫu (0 attributes)")
    void getCategories_AttributesCount_MappedCorrectly() {
        UUID cat1Id = UUID.randomUUID();
        UUID cat2Id = UUID.randomUUID();

        Category cat1 = Category.builder()
                .id(cat1Id)
                .name("Danh mục có thuộc tính")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category cat2 = Category.builder()
                .id(cat2Id)
                .name("Danh mục chưa có thuộc tính")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(cat1, cat2));

        // cat2 không xuất hiện trong bảng đếm GROUP BY (count = 0)
        List<Object[]> counts = Collections.singletonList(new Object[]{cat1Id, 3L});
        when(categoryAttributeRepository.countByCategoryIdIn(anyList()))
                .thenReturn(counts);

        List<CategorySummaryResponse> result = categoryService.getCategories(null, "name", "asc");

        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).attributesCount());
        assertEquals(0L, result.get(1).attributesCount()); // Không NPE, mặc định 0L
    }

    @Test
    @DisplayName("TC-06: Sắp xếp theo createdAt DESC")
    void getCategories_SortByCreatedAt_Desc() {
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        when(categoryRepository.findAll(any(Specification.class), sortCaptor.capture()))
                .thenReturn(Collections.emptyList());

        categoryService.getCategories(null, "createdAt", "desc");

        Sort capturedSort = sortCaptor.getValue();
        Sort.Order order = capturedSort.getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("TC-07: Tham số sortBy không hợp lệ - Fallback an toàn về 'name'")
    void getCategories_InvalidSortBy_FallbackToName() {
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        when(categoryRepository.findAll(any(Specification.class), sortCaptor.capture()))
                .thenReturn(Collections.emptyList());

        categoryService.getCategories(null, "invalid_column", "asc");

        Sort capturedSort = sortCaptor.getValue();
        Sort.Order order = capturedSort.getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("TC-08: Tham số sortDir không hợp lệ - Fallback an toàn về 'asc'")
    void getCategories_InvalidSortDir_FallbackToAsc() {
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        when(categoryRepository.findAll(any(Specification.class), sortCaptor.capture()))
                .thenReturn(Collections.emptyList());

        categoryService.getCategories(null, "name", "random_dir");

        Sort capturedSort = sortCaptor.getValue();
        Sort.Order order = capturedSort.getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("TC-09: Keyword chỉ chứa khoảng trắng - Bỏ qua lọc keyword và trả về kết quả")
    void getCategories_WhitespaceKeyword_Ignored() {
        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<CategorySummaryResponse> result = categoryService.getCategories("    ", "name", "asc");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-10: Kiểm thử Zero N+1 Query - Batch count được gọi duy nhất 1 lần với toàn bộ IDs")
    void getCategories_ZeroNPlusOne_BatchQueryVerified() {
        List<Category> categories = new ArrayList<>();
        List<Object[]> counts = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            categories.add(Category.builder()
                    .id(id)
                    .name("Category " + i)
                    .status(CategoryStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            counts.add(new Object[]{id, (long) (i + 1)});
        }

        when(categoryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(categories);
        when(categoryAttributeRepository.countByCategoryIdIn(anyList()))
                .thenReturn(counts);

        List<CategorySummaryResponse> result = categoryService.getCategories(null, "name", "asc");

        assertEquals(5, result.size());
        // Batch query chỉ gọi đúng 1 lần duy nhất cho toàn bộ 5 category
        verify(categoryAttributeRepository, times(1)).countByCategoryIdIn(anyList());
    }

    // =========================================================================
    // Tests cho getCategoryAttributes (API 28: GET /api/categories/{id}/attributes)
    // =========================================================================

    @Test
    @DisplayName("TC-01: getCategoryAttributes_Success_ReturnAttributesList")
    void getCategoryAttributes_Success_ReturnAttributesList() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attr1 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Dung lượng RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB", "32GB"))
                .sampleValue("16GB")
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();

        CategoryAttribute attr2 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Card đồ họa (GPU)")
                .dataType(CategoryAttributeDataType.TEXT)
                .options(null)
                .sampleValue("RTX 4060")
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        CategoryAttribute attr3 = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Trọng lượng (kg)")
                .dataType(CategoryAttributeDataType.NUMBER)
                .options(Collections.emptyList())
                .sampleValue("1.4")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(List.of(attr1, attr2, attr3));

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertNotNull(result);
        assertEquals(3, result.size());

        CategoryAttributeResponse res1 = result.get(0);
        assertEquals("Dung lượng RAM", res1.name());
        assertEquals(CategoryAttributeDataType.SELECT, res1.dataType());
        assertEquals(List.of("8GB", "16GB", "32GB"), res1.options());
        assertEquals("16GB", res1.sampleValue());

        CategoryAttributeResponse res2 = result.get(1);
        assertEquals("Card đồ họa (GPU)", res2.name());
        assertEquals(CategoryAttributeDataType.TEXT, res2.dataType());
        assertNotNull(res2.options());
        assertTrue(res2.options().isEmpty(), "Options must default to empty list when null");

        CategoryAttributeResponse res3 = result.get(2);
        assertEquals("Trọng lượng (kg)", res3.name());
        assertEquals(CategoryAttributeDataType.NUMBER, res3.dataType());
        assertEquals("1.4", res3.sampleValue());
    }

    @Test
    @DisplayName("TC-02: getCategoryAttributes_CategoryNotFound_Throws404")
    void getCategoryAttributes_CategoryNotFound_Throws404() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                categoryService.getCategoryAttributes(categoryId)
        );

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).findByCategoryIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("TC-03: getCategoryAttributes_CategoryInactive_Throws400")
    void getCategoryAttributes_CategoryInactive_Throws400() {
        UUID categoryId = UUID.randomUUID();
        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("Điện thoại cũ")
                .status(CategoryStatus.INACTIVE)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(inactiveCategory));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                categoryService.getCategoryAttributes(categoryId)
        );

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).findByCategoryIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("TC-04: getCategoryAttributes_NoAttributes_ReturnEmptyList")
    void getCategoryAttributes_NoAttributes_ReturnEmptyList() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Danh mục mới")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(Collections.emptyList());

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC-05: getCategoryAttributes_NullOptions_SafelyReturnsEmptyList")
    void getCategoryAttributes_NullOptions_SafelyReturnsEmptyList() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("CPU")
                .dataType(CategoryAttributeDataType.TEXT)
                .options(null)
                .sampleValue("Core i7")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(List.of(attr));

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).options());
        assertTrue(result.get(0).options().isEmpty());
    }

    @Test
    @DisplayName("TC-06: getCategoryAttributes_SelectType_ReturnsOptionsCorrectly")
    void getCategoryAttributes_SelectType_ReturnsOptionsCorrectly() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Dung lượng RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB", "32GB"))
                .sampleValue("16GB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(List.of(attr));

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).options().size());
        assertTrue(result.get(0).options().contains("16GB"));
    }

    @Test
    @DisplayName("TC-07: getCategoryAttributes_VerifySortedByCreatedAtAsc")
    void getCategoryAttributes_VerifySortedByCreatedAtAsc() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(Collections.emptyList());

        categoryService.getCategoryAttributes(categoryId);

        verify(categoryAttributeRepository, times(1)).findByCategoryIdOrderByCreatedAtAsc(categoryId);
    }

    @Test
    @DisplayName("TC-08: getCategoryAttributes_SampleValueMappedCorrectly")
    void getCategoryAttributes_SampleValueMappedCorrectly() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute attr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Card đồ họa")
                .dataType(CategoryAttributeDataType.TEXT)
                .options(Collections.emptyList())
                .sampleValue("RTX 4060")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(List.of(attr));

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertEquals("RTX 4060", result.get(0).sampleValue());
    }

    @Test
    @DisplayName("TC-09: getCategoryAttributes_ZeroNPlusOne_QueryCountVerified")
    void getCategoryAttributes_ZeroNPlusOne_QueryCountVerified() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        List<CategoryAttribute> attributes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            attributes.add(CategoryAttribute.builder()
                    .id(UUID.randomUUID())
                    .category(category)
                    .name("Attr " + i)
                    .dataType(CategoryAttributeDataType.TEXT)
                    .options(Collections.emptyList())
                    .sampleValue("Sample " + i)
                    .createdAt(LocalDateTime.now().plusMinutes(i))
                    .updatedAt(LocalDateTime.now().plusMinutes(i))
                    .build());
        }

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(attributes);

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertEquals(5, result.size());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryAttributeRepository, times(1)).findByCategoryIdOrderByCreatedAtAsc(categoryId);
    }

    @Test
    @DisplayName("TC-10: getCategoryAttributes_AllDataTypesSupported")
    void getCategoryAttributes_AllDataTypesSupported() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .build();

        CategoryAttribute textAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID()).category(category).name("Brand")
                .dataType(CategoryAttributeDataType.TEXT).createdAt(LocalDateTime.now()).build();

        CategoryAttribute numAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID()).category(category).name("Weight")
                .dataType(CategoryAttributeDataType.NUMBER).createdAt(LocalDateTime.now()).build();

        CategoryAttribute boolAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID()).category(category).name("Touchscreen")
                .dataType(CategoryAttributeDataType.BOOLEAN).createdAt(LocalDateTime.now()).build();

        CategoryAttribute selectAttr = CategoryAttribute.builder()
                .id(UUID.randomUUID()).category(category).name("Color")
                .dataType(CategoryAttributeDataType.SELECT).options(List.of("Red", "Blue")).createdAt(LocalDateTime.now()).build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId))
                .thenReturn(List.of(textAttr, numAttr, boolAttr, selectAttr));

        List<CategoryAttributeResponse> result = categoryService.getCategoryAttributes(categoryId);

        assertEquals(4, result.size());
        assertEquals(CategoryAttributeDataType.TEXT, result.get(0).dataType());
        assertEquals(CategoryAttributeDataType.NUMBER, result.get(1).dataType());
        assertEquals(CategoryAttributeDataType.BOOLEAN, result.get(2).dataType());
        assertEquals(CategoryAttributeDataType.SELECT, result.get(3).dataType());
    }
}

