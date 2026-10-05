package com.techhub.service.admin;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateCategoryAttributeRequest;
import com.techhub.model.dto.request.CreateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.model.dto.request.UpdateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryStatusRequest;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategoryDetailResponse;
import com.techhub.model.entity.Category;
import com.techhub.model.entity.CategoryAttribute;
import com.techhub.model.enums.CategoryAttributeDataType;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.model.enums.ProductApprovalStatus;
import com.techhub.model.enums.ProductVisibilityStatus;
import com.techhub.repository.CategoryAttributeRepository;
import com.techhub.repository.CategoryRepository;
import com.techhub.repository.ProductRepository;
import com.techhub.service.admin.impl.AdminCategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminCategoryServiceImpl (POST /api/admin/categories)")
class AdminCategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryAttributeRepository categoryAttributeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminCategoryServiceImpl adminCategoryService;

    @Test
    @DisplayName("TC-01: createCategory_Success_WithMixedAttributes - Tạo danh mục thành công với 4 loại thuộc tính")
    void createCategory_Success_WithMixedAttributes() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Điện thoại & Phụ kiện",
                "Ngành hàng điện thoại thông minh và phụ kiện",
                List.of(
                        new CreateCategoryAttributeRequest("Chipset", CategoryAttributeDataType.TEXT, null, "Apple A16 Bionic"),
                        new CreateCategoryAttributeRequest("Kích thước màn hình", CategoryAttributeDataType.NUMBER, null, "6.1"),
                        new CreateCategoryAttributeRequest("Hỗ trợ 5G", CategoryAttributeDataType.BOOLEAN, null, "true"),
                        new CreateCategoryAttributeRequest("Cổng kết nối", CategoryAttributeDataType.SELECT, List.of("Type-C", "Lightning"), "Type-C")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Điện thoại & Phụ kiện")).thenReturn(false);

        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryId);
            cat.setCreatedAt(LocalDateTime.now());
            cat.setUpdatedAt(LocalDateTime.now());
            return cat;
        });

        when(categoryAttributeRepository.saveAll(any())).thenAnswer(invocation -> {
            List<CategoryAttribute> list = invocation.getArgument(0);
            for (CategoryAttribute attr : list) {
                attr.setId(UUID.randomUUID());
                attr.setCreatedAt(LocalDateTime.now());
                attr.setUpdatedAt(LocalDateTime.now());
            }
            return list;
        });

        // Act
        CategoryDetailResponse response = adminCategoryService.createCategory(request);

        // Assert
        assertNotNull(response);
        assertEquals(categoryId, response.id());
        assertEquals("Điện thoại & Phụ kiện", response.name());
        assertEquals("Ngành hàng điện thoại thông minh và phụ kiện", response.description());
        assertEquals(CategoryStatus.ACTIVE, response.status());
        assertEquals(4, response.attributes().size());

        verify(categoryRepository).existsByNameIgnoreCase("Điện thoại & Phụ kiện");
        verify(categoryRepository).save(any(Category.class));
        verify(categoryAttributeRepository).saveAll(any());
    }

    @Test
    @DisplayName("TC-02: createCategory_Success_SelectWithMultipleOptions - Thuộc tính SELECT lưu mảng options sạch")
    void createCategory_Success_SelectWithMultipleOptions() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Laptop",
                "Máy tính xách tay",
                List.of(
                        new CreateCategoryAttributeRequest("Màu sắc", CategoryAttributeDataType.SELECT,
                                List.of("  Bạc  ", "Xám", "Bạc", ""), "Bạc")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        when(categoryAttributeRepository.saveAll(any())).thenAnswer(inv -> {
            List<CategoryAttribute> list = inv.getArgument(0);
            list.forEach(a -> a.setId(UUID.randomUUID()));
            return list;
        });

        // Act
        CategoryDetailResponse response = adminCategoryService.createCategory(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.attributes().size());
        List<String> options = response.attributes().get(0).options();
        assertEquals(2, options.size()); // "Bạc", "Xám" (lọc bỏ rỗng và distinct)
        assertTrue(options.contains("Bạc"));
        assertTrue(options.contains("Xám"));
    }

    @Test
    @DisplayName("TC-03: createCategory_Fail_DuplicateCategoryName - Tên danh mục đã tồn tại ném CATEGORY_NAME_ALREADY_EXISTS")
    void createCategory_Fail_DuplicateCategoryName() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Laptop",
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("RAM", CategoryAttributeDataType.TEXT, null, "16GB"))
        );

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: createCategory_Fail_DuplicateAttributeNamesInPayload - Trùng tên thuộc tính ném DUPLICATE_ATTRIBUTE_NAME")
    void createCategory_Fail_DuplicateAttributeNamesInPayload() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Laptop",
                "Mô tả",
                List.of(
                        new CreateCategoryAttributeRequest("RAM", CategoryAttributeDataType.TEXT, null, "16GB"),
                        new CreateCategoryAttributeRequest("  ram  ", CategoryAttributeDataType.TEXT, null, "32GB")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-05: createCategory_Fail_SelectWithoutOptions - Thuộc tính SELECT có options rỗng ném INVALID_ATTRIBUTE_OPTIONS")
    void createCategory_Fail_SelectWithoutOptions() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Laptop",
                "Mô tả",
                List.of(
                        new CreateCategoryAttributeRequest("Màu sắc", CategoryAttributeDataType.SELECT, List.of(), "Bạc")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-06: createCategory_Fail_SelectWithBlankOption - Thuộc tính SELECT chỉ chứa khoảng trắng ném INVALID_ATTRIBUTE_OPTIONS")
    void createCategory_Fail_SelectWithBlankOption() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Laptop",
                "Mô tả",
                List.of(
                        new CreateCategoryAttributeRequest("Màu sắc", CategoryAttributeDataType.SELECT, List.of("  ", ""), "Bạc")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-07: createCategory_Fail_EmptyAttributesList - Danh sách thuộc tính rỗng ném ATTRIBUTE_LIMIT_EXCEEDED")
    void createCategory_Fail_EmptyAttributesList() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest("Laptop", "Mô tả", List.of());

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-08: createCategory_Fail_Exceed50Attributes - Vượt quá 50 thuộc tính ném ATTRIBUTE_LIMIT_EXCEEDED")
    void createCategory_Fail_Exceed50Attributes() {
        // Arrange
        List<CreateCategoryAttributeRequest> list = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            list.add(new CreateCategoryAttributeRequest("Attr" + i, CategoryAttributeDataType.TEXT, null, null));
        }
        CreateCategoryRequest request = new CreateCategoryRequest("Laptop", "Mô tả", list);

        when(categoryRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-09: createCategory_Success_TrimWhitespace - Tự động trim khoảng trắng thừa ở tất cả các trường")
    void createCategory_Success_TrimWhitespace() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "  Thiết bị âm thanh  ",
                "  Mô tả âm thanh  ",
                List.of(
                        new CreateCategoryAttributeRequest("  Công suất  ", CategoryAttributeDataType.NUMBER, null, "  100W  ")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Thiết bị âm thanh")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        when(categoryAttributeRepository.saveAll(any())).thenAnswer(inv -> {
            List<CategoryAttribute> list = inv.getArgument(0);
            list.forEach(a -> a.setId(UUID.randomUUID()));
            return list;
        });

        // Act
        CategoryDetailResponse response = adminCategoryService.createCategory(request);

        // Assert
        assertEquals("Thiết bị âm thanh", response.name());
        assertEquals("Mô tả âm thanh", response.description());
        assertEquals("Công suất", response.attributes().get(0).name());
        assertEquals("100W", response.attributes().get(0).sampleValue());
    }

    @Test
    @DisplayName("TC-10: createCategory_Success_NonSelectOptionsCleared - Kiểu TEXT gửi kèm options bị xóa về null")
    void createCategory_Success_NonSelectOptionsCleared() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Màn hình",
                "Mô tả",
                List.of(
                        new CreateCategoryAttributeRequest("Độ phân giải", CategoryAttributeDataType.TEXT, List.of("4K", "2K"), "4K")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Màn hình")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ArgumentCaptor<List<CategoryAttribute>> captor = ArgumentCaptor.forClass(List.class);
        when(categoryAttributeRepository.saveAll(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        adminCategoryService.createCategory(request);

        // Assert
        List<CategoryAttribute> savedList = captor.getValue();
        assertNull(savedList.get(0).getOptions());
    }

    @Test
    @DisplayName("TC-11: createCategory_Fail_SelectOptionTooLong - Option dài hơn 100 ký tự bị từ chối")
    void createCategory_Fail_SelectOptionTooLong() {
        // Arrange
        String longOption = "A".repeat(101);
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Màn hình",
                "Mô tả",
                List.of(
                        new CreateCategoryAttributeRequest("Cổng kết nối", CategoryAttributeDataType.SELECT, List.of(longOption), null)
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Màn hình")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategory(request)
        );
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-12: createCategory_Success_BlankSampleValueStoredAsNull - sampleValue chứa toàn khoảng trắng được lưu là null")
    void createCategory_Success_BlankSampleValueStoredAsNull() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Màn hình",
                "   ",
                List.of(
                        new CreateCategoryAttributeRequest("Độ phân giải", CategoryAttributeDataType.TEXT, null, "   ")
                )
        );

        when(categoryRepository.existsByNameIgnoreCase("Màn hình")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ArgumentCaptor<List<CategoryAttribute>> captor = ArgumentCaptor.forClass(List.class);
        when(categoryAttributeRepository.saveAll(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryDetailResponse response = adminCategoryService.createCategory(request);

        // Assert
        assertNull(response.description());
        List<CategoryAttribute> savedList = captor.getValue();
        assertNull(savedList.get(0).getSampleValue());
    }

    // =========================================================================
    // UNIT TESTS CHO GET /api/admin/categories (Xem danh sách danh mục phân trang)
    // =========================================================================

    @Test
    @DisplayName("TC-13: getCategories_Success_DefaultPagination - Phân trang mặc định trả về danh mục kèm số thuộc tính")
    void getCategories_Success_DefaultPagination() {
        // Arrange
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Category cat1 = Category.builder().id(catId1).name("Điện thoại").description("Mô tả 1").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Category cat2 = Category.builder().id(catId2).name("Laptop").description("Mô tả 2").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        Page<Category> categoryPage = new PageImpl<>(List.of(cat1, cat2), PageRequest.of(0, 20), 2);
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(categoryPage);

        List<Object[]> countList = new ArrayList<>();
        countList.add(new Object[]{catId1, 4L});
        countList.add(new Object[]{catId2, 6L});
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId1, catId2))).thenReturn(countList);

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories(null, null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalElements());
        assertEquals(4L, result.content().get(0).attributesCount());
        assertEquals(6L, result.content().get(1).attributesCount());
    }

    @Test
    @DisplayName("TC-14: getCategories_Success_FilterActiveOnly - Lọc chỉ lấy danh mục đang hoạt động (ACTIVE)")
    void getCategories_Success_FilterActiveOnly() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Điện thoại").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Page<Category> categoryPage = new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1);
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(categoryPage);

        List<Object[]> countList = new ArrayList<>();
        countList.add(new Object[]{catId, 2L});
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(countList);

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories(null, CategoryStatus.ACTIVE, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(CategoryStatus.ACTIVE, result.content().get(0).status());
    }

    @Test
    @DisplayName("TC-15: getCategories_Success_FilterInactiveOnly - Lọc chỉ lấy danh mục ngừng hoạt động (INACTIVE)")
    void getCategories_Success_FilterInactiveOnly() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Máy cũ").status(CategoryStatus.INACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Page<Category> categoryPage = new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1);
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(categoryPage);

        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(List.of());

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories(null, CategoryStatus.INACTIVE, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(CategoryStatus.INACTIVE, result.content().get(0).status());
        assertEquals(0L, result.content().get(0).attributesCount());
    }

    @Test
    @DisplayName("TC-16: getCategories_Success_SearchByKeyword_Name - Tìm kiếm theo tên danh mục")
    void getCategories_Success_SearchByKeyword_Name() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Điện thoại").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1));
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(List.of());

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories("điện", null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Điện thoại", result.content().get(0).name());
    }

    @Test
    @DisplayName("TC-17: getCategories_Success_SearchByKeyword_Description - Tìm kiếm theo mô tả")
    void getCategories_Success_SearchByKeyword_Description() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Thiết bị mạng").description("Router và Switch").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1));
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(List.of());

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories("router", null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Thiết bị mạng", result.content().get(0).name());
    }

    @Test
    @DisplayName("TC-18: getCategories_Success_SearchByKeyword_CaseInsensitive - Tìm kiếm không phân biệt hoa thường")
    void getCategories_Success_SearchByKeyword_CaseInsensitive() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Laptop Gaming").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1));
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(List.of());

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories("LAPTOP", null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    @DisplayName("TC-19: getCategories_Success_PaddedKeyword_Trimmed - Từ khóa có khoảng trắng đầu cuối được xử lý sạch sẽ")
    void getCategories_Success_PaddedKeyword_Trimmed() {
        // Arrange
        UUID catId = UUID.randomUUID();
        Category cat = Category.builder().id(catId).name("Màn hình").status(CategoryStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat), PageRequest.of(0, 20), 1));
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId))).thenReturn(List.of());

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories("   màn hình   ", null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    @DisplayName("TC-20: getCategories_Success_SortByNameAsc - Sắp xếp theo tên tăng dần A-Z")
    void getCategories_Success_SortByNameAsc() {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(categoryRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        adminCategoryService.getCategories(null, null, 0, 20, "name", "asc");

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("name");
        assertNotNull(order);
        assertTrue(order.isAscending());
        Sort.Order tieBreakerOrder = capturedPageable.getSort().getOrderFor("id");
        assertNotNull(tieBreakerOrder);
        assertTrue(tieBreakerOrder.isAscending());
    }

    @Test
    @DisplayName("TC-21: getCategories_Success_InvalidSortByField_FallbackToCreatedAt - Sort field không hợp lệ tự động fallback về createdAt desc")
    void getCategories_Success_InvalidSortByField_FallbackToCreatedAt() {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(categoryRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        adminCategoryService.getCategories(null, null, 0, 20, "invalidField", "desc");

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertTrue(order.isDescending());
    }

    @Test
    @DisplayName("TC-22: getCategories_Success_PageOutOfBounds_ReturnsEmptyList - Truy cập trang ngoài phạm vi trả về mảng rỗng an toàn")
    void getCategories_Success_PageOutOfBounds_ReturnsEmptyList() {
        // Arrange
        Pageable pageable = PageRequest.of(100, 20);
        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 5));

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories(null, null, 100, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(5, result.totalElements());
    }

    @Test
    @DisplayName("TC-23: getCategories_Success_SizeCapAt100 - Client gửi size=500 bị giới hạn về 100")
    void getCategories_Success_SizeCapAt100() {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(categoryRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        // Act
        adminCategoryService.getCategories(null, null, 0, 500, "createdAt", "desc");

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(100, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("TC-24: getCategories_Success_NegativePageAndSize_HandledSafely - Page âm và size âm được xử lý an toàn không văng 500")
    void getCategories_Success_NegativePageAndSize_HandledSafely() {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(categoryRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        // Act
        adminCategoryService.getCategories(null, null, -5, -10, "createdAt", "desc");

        // Assert
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(1, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("TC-25: getCategories_Success_CorrectAttributesCount_WithZeroAttributes - Danh mục không có attribute có attributesCount = 0")
    void getCategories_Success_CorrectAttributesCount_WithZeroAttributes() {
        // Arrange
        UUID catId1 = UUID.randomUUID();
        UUID catId2 = UUID.randomUUID();
        Category cat1 = Category.builder().id(catId1).name("Cat 1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Category cat2 = Category.builder().id(catId2).name("Cat 2").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cat1, cat2), PageRequest.of(0, 20), 2));

        List<Object[]> countList = new ArrayList<>();
        countList.add(new Object[]{catId1, 5L}); // catId2 không có trong countList
        when(categoryAttributeRepository.countByCategoryIdIn(List.of(catId1, catId2))).thenReturn(countList);

        // Act
        PageResponse<CategorySummaryResponse> result = adminCategoryService.getCategories(null, null, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.content().get(0).attributesCount());
        assertEquals(0L, result.content().get(1).attributesCount());
    }

    // =========================================================================
    // API 3: GET /api/admin/categories/{id} (View Category Details)
    // =========================================================================

    @Test
    @DisplayName("TC-26: getCategoryDetail_Success_ActiveCategory - Lấy chi tiết danh mục đang active kèm 3 thuộc tính")
    void getCategoryDetail_Success_ActiveCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Điện thoại & Phụ kiện")
                .description("Mô tả ngành hàng")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UUID attrId1 = UUID.randomUUID();
        UUID attrId2 = UUID.randomUUID();
        UUID attrId3 = UUID.randomUUID();

        List<CategoryAttribute> attributes = List.of(
                CategoryAttribute.builder().id(attrId1).category(category).name("RAM").dataType(CategoryAttributeDataType.SELECT).options(List.of("4GB", "8GB")).sampleValue("8GB").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                CategoryAttribute.builder().id(attrId2).category(category).name("Kích thước màn hình").dataType(CategoryAttributeDataType.NUMBER).options(null).sampleValue("6.7").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                CategoryAttribute.builder().id(attrId3).category(category).name("Hỗ trợ 5G").dataType(CategoryAttributeDataType.BOOLEAN).options(null).sampleValue("true").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(attributes);

        // Act
        CategoryDetailResponse result = adminCategoryService.getCategoryDetail(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals(categoryId, result.id());
        assertEquals("Điện thoại & Phụ kiện", result.name());
        assertEquals("Mô tả ngành hàng", result.description());
        assertEquals(CategoryStatus.ACTIVE, result.status());
        assertEquals(3, result.attributes().size());

        CategoryAttributeResponse attrRes1 = result.attributes().get(0);
        assertEquals(attrId1, attrRes1.id());
        assertEquals("RAM", attrRes1.name());
        assertEquals(CategoryAttributeDataType.SELECT, attrRes1.dataType());
        assertEquals(List.of("4GB", "8GB"), attrRes1.options());
        assertEquals("8GB", attrRes1.sampleValue());

        CategoryAttributeResponse attrRes2 = result.attributes().get(1);
        assertEquals("Kích thước màn hình", attrRes2.name());
        assertEquals(CategoryAttributeDataType.NUMBER, attrRes2.dataType());
        assertNull(attrRes2.options());
        assertEquals("6.7", attrRes2.sampleValue());
    }

    @Test
    @DisplayName("TC-27: getCategoryDetail_Success_InactiveCategory - Admin xem được danh mục ngừng hoạt động (status = INACTIVE)")
    void getCategoryDetail_Success_InactiveCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Thiết bị cũ")
                .status(CategoryStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CategoryAttribute attr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("Thuộc tính cũ")
                .dataType(CategoryAttributeDataType.TEXT)
                .sampleValue("Ví dụ")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of(attr));

        // Act
        CategoryDetailResponse result = adminCategoryService.getCategoryDetail(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals(CategoryStatus.INACTIVE, result.status());
        assertEquals(1, result.attributes().size());
    }

    @Test
    @DisplayName("TC-28: getCategoryDetail_Success_EmptyAttributes - Danh mục legacy không có thuộc tính trả về mảng rỗng")
    void getCategoryDetail_Success_EmptyAttributes() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Danh mục rỗng")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse result = adminCategoryService.getCategoryDetail(categoryId);

        // Assert
        assertNotNull(result);
        assertTrue(result.attributes().isEmpty());
    }

    @Test
    @DisplayName("TC-29: getCategoryDetail_Throws_CategoryNotFound - ID không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void getCategoryDetail_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.getCategoryDetail(categoryId));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).findByCategoryIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("TC-30: getCategoryDetail_AttributesSortedByCreatedAtAsc - Thứ tự thuộc tính bảo toàn sắp xếp createdAt asc")
    void getCategoryDetail_AttributesSortedByCreatedAtAsc() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Test Cat").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 1, 11, 0);

        CategoryAttribute attr1 = CategoryAttribute.builder().id(UUID.randomUUID()).name("Attr 1").dataType(CategoryAttributeDataType.TEXT).createdAt(t1).updatedAt(t1).build();
        CategoryAttribute attr2 = CategoryAttribute.builder().id(UUID.randomUUID()).name("Attr 2").dataType(CategoryAttributeDataType.TEXT).createdAt(t2).updatedAt(t2).build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of(attr1, attr2));

        // Act
        CategoryDetailResponse result = adminCategoryService.getCategoryDetail(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals("Attr 1", result.attributes().get(0).name());
        assertEquals("Attr 2", result.attributes().get(1).name());
    }

    // =========================================================================
    // API 4: PUT /api/admin/categories/{id} (Update Category)
    // =========================================================================

    @Test
    @DisplayName("TC-31: updateCategory_Success_BothNameAndDescription - Cập nhật thành công cả tên và mô tả danh mục")
    void updateCategory_Success_BothNameAndDescription() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop Cũ")
                .description("Mô tả cũ")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop & PC", "Mô tả mới");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Laptop & PC", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID attrId = UUID.randomUUID();
        CategoryAttribute attr = CategoryAttribute.builder()
                .id(attrId)
                .name("RAM")
                .dataType(CategoryAttributeDataType.TEXT)
                .sampleValue("16GB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of(attr));

        // Act
        CategoryDetailResponse result = adminCategoryService.updateCategory(categoryId, request);

        // Assert
        assertNotNull(result);
        assertEquals(categoryId, result.id());
        assertEquals("Laptop & PC", result.name());
        assertEquals("Mô tả mới", result.description());
        assertEquals(CategoryStatus.ACTIVE, result.status());
        assertEquals(1, result.attributes().size());
        assertEquals("RAM", result.attributes().get(0).name());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("TC-32: updateCategory_Success_KeepSameName_UpdateDescriptionOnly - Giữ nguyên tên cũ chỉ cập nhật mô tả không bị báo trùng tên")
    void updateCategory_Success_KeepSameName_UpdateDescriptionOnly() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .description("Mô tả ban đầu")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop", "Chỉ sửa mô tả");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Laptop", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse result = adminCategoryService.updateCategory(categoryId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Laptop", result.name());
        assertEquals("Chỉ sửa mô tả", result.description());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("TC-33: updateCategory_Success_CaseChangeOnly - Chỉ đổi định dạng chữ hoa thường của chính nó thành công")
    void updateCategory_Success_CaseChangeOnly() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("laptop")
                .description("Mô tả")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("LAPTOP", "Mô tả");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("LAPTOP", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse result = adminCategoryService.updateCategory(categoryId, request);

        // Assert
        assertNotNull(result);
        assertEquals("LAPTOP", result.name());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("TC-34: updateCategory_Success_SetDescriptionToNull - Xóa mô tả bằng cách truyền null hoặc khoảng trắng")
    void updateCategory_Success_SetDescriptionToNull() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .description("Mô tả cũ cần xóa")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop", "   ");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Laptop", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse result = adminCategoryService.updateCategory(categoryId, request);

        // Assert
        assertNotNull(result);
        assertNull(result.description());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("TC-35: updateCategory_Success_InactiveCategory - Admin cập nhật tên và mô tả cho danh mục ngừng hoạt động (status = INACTIVE)")
    void updateCategory_Success_InactiveCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop Cũ")
                .description("Mô tả")
                .status(CategoryStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop Thanh Lý", "Mô tả mới");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Laptop Thanh Lý", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse result = adminCategoryService.updateCategory(categoryId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Laptop Thanh Lý", result.name());
        assertEquals(CategoryStatus.INACTIVE, result.status());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("TC-36: updateCategory_Throws_CategoryNotFound - ID không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void updateCategory_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop Mới", "Mô tả");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategory(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
        verify(categoryAttributeRepository, never()).findByCategoryIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("TC-37: updateCategory_Throws_CategoryNameAlreadyExists - Tên mới trùng danh mục khác ném CATEGORY_NAME_ALREADY_EXISTS (HTTP 409)")
    void updateCategory_Throws_CategoryNameAlreadyExists() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryRequest request = new UpdateCategoryRequest("Điện thoại", "Mô tả");

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Điện thoại", categoryId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategory(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
        verify(categoryAttributeRepository, never()).findByCategoryIdOrderByCreatedAtAsc(any());
    }

    // =========================================================================
    // UNIT TESTS CHO POST /api/admin/categories/{id}/attributes (Thêm thuộc tính kỹ thuật mẫu)
    // =========================================================================

    @Test
    @DisplayName("TC-38: createCategoryAttribute_Success_SelectType - Tạo thuộc tính kiểu SELECT thành công kèm options và sampleValue")
    void createCategoryAttribute_Success_SelectType() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Điện thoại")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Chuẩn Bluetooth",
                CategoryAttributeDataType.SELECT,
                List.of("Bluetooth 5.0", "Bluetooth 5.2"),
                "Bluetooth 5.2"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(10L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Chuẩn Bluetooth")).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> {
            CategoryAttribute saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CategoryAttributeResponse response = adminCategoryService.createCategoryAttribute(categoryId, request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Chuẩn Bluetooth", response.name());
        assertEquals(CategoryAttributeDataType.SELECT, response.dataType());
        assertEquals(List.of("Bluetooth 5.0", "Bluetooth 5.2"), response.options());
        assertEquals("Bluetooth 5.2", response.sampleValue());

        ArgumentCaptor<CategoryAttribute> captor = ArgumentCaptor.forClass(CategoryAttribute.class);
        verify(categoryAttributeRepository).save(captor.capture());
        CategoryAttribute captured = captor.getValue();
        assertEquals(category, captured.getCategory());
        assertEquals("Chuẩn Bluetooth", captured.getName());
        assertEquals(CategoryAttributeDataType.SELECT, captured.getDataType());
        assertEquals(List.of("Bluetooth 5.0", "Bluetooth 5.2"), captured.getOptions());
        assertEquals("Bluetooth 5.2", captured.getSampleValue());
    }

    @Test
    @DisplayName("TC-39: createCategoryAttribute_Success_NonSelectType_OptionsNormalizedToNull - Kiểu dữ liệu khác SELECT tự chuẩn hóa options = null")
    void createCategoryAttribute_Success_NonSelectType_OptionsNormalizedToNull() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Điện thoại")
                .status(CategoryStatus.ACTIVE)
                .build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Công suất sạc (W)",
                CategoryAttributeDataType.NUMBER,
                List.of("10", "20"),
                "67"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(5L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Công suất sạc (W)")).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> {
            CategoryAttribute saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CategoryAttributeResponse response = adminCategoryService.createCategoryAttribute(categoryId, request);

        // Assert
        assertNotNull(response);
        assertNull(response.options());
        assertEquals("67", response.sampleValue());

        ArgumentCaptor<CategoryAttribute> captor = ArgumentCaptor.forClass(CategoryAttribute.class);
        verify(categoryAttributeRepository).save(captor.capture());
        assertNull(captor.getValue().getOptions());
    }

    @Test
    @DisplayName("TC-40: createCategoryAttribute_Success_WhitespaceSampleValue_NormalizedToNull - sampleValue chứa toàn khoảng trắng được chuẩn hóa thành null")
    void createCategoryAttribute_Success_WhitespaceSampleValue_NormalizedToNull() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Điện thoại")
                .status(CategoryStatus.ACTIVE)
                .build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Chất liệu khung",
                CategoryAttributeDataType.TEXT,
                null,
                "   "
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(2L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Chất liệu khung")).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> {
            CategoryAttribute saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CategoryAttributeResponse response = adminCategoryService.createCategoryAttribute(categoryId, request);

        // Assert
        assertNotNull(response);
        assertNull(response.sampleValue());

        ArgumentCaptor<CategoryAttribute> captor = ArgumentCaptor.forClass(CategoryAttribute.class);
        verify(categoryAttributeRepository).save(captor.capture());
        assertNull(captor.getValue().getSampleValue());
    }

    @Test
    @DisplayName("TC-41: createCategoryAttribute_Success_InactiveCategory - Thêm thuộc tính vào danh mục đang ngừng hoạt động thành công")
    void createCategoryAttribute_Success_InactiveCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Danh mục cũ")
                .status(CategoryStatus.INACTIVE)
                .build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Thời gian bảo hành",
                CategoryAttributeDataType.TEXT,
                null,
                "12 tháng"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(0L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Thời gian bảo hành")).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> {
            CategoryAttribute saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CategoryAttributeResponse response = adminCategoryService.createCategoryAttribute(categoryId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Thời gian bảo hành", response.name());
        verify(categoryAttributeRepository).save(any());
    }

    @Test
    @DisplayName("TC-42: createCategoryAttribute_Throws_CategoryNotFound - ID danh mục không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void createCategoryAttribute_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).countByCategoryId(any());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-43: createCategoryAttribute_Throws_AttributeLimitExceeded - Danh mục đã có đủ 50 thuộc tính ném ATTRIBUTE_LIMIT_EXCEEDED (HTTP 400)")
    void createCategoryAttribute_Throws_AttributeLimitExceeded() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Thuộc tính thứ 51",
                CategoryAttributeDataType.TEXT,
                null,
                "Test"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(50L);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).existsByCategoryIdAndNameIgnoreCase(any(), any());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-44: createCategoryAttribute_Throws_DuplicateAttributeName - Trùng tên thuộc tính trong danh mục ném DUPLICATE_ATTRIBUTE_NAME (HTTP 400)")
    void createCategoryAttribute_Throws_DuplicateAttributeName() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Điện thoại").status(CategoryStatus.ACTIVE).build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(5L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "RAM")).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-45: createCategoryAttribute_Throws_DuplicateAttributeName_CaseInsensitive - Trùng tên không phân biệt hoa thường ném DUPLICATE_ATTRIBUTE_NAME")
    void createCategoryAttribute_Throws_DuplicateAttributeName_CaseInsensitive() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Điện thoại").status(CategoryStatus.ACTIVE).build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "ram",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(5L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "ram")).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-46: createCategoryAttribute_Throws_SelectMissingOptions - Kiểu SELECT nhưng options null hoặc rỗng ném INVALID_ATTRIBUTE_OPTIONS")
    void createCategoryAttribute_Throws_SelectMissingOptions() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Điện thoại").status(CategoryStatus.ACTIVE).build();

        CreateCategoryAttributeRequest requestNullOptions = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                null,
                "8GB"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(5L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "RAM")).thenReturn(false);

        // Act & Assert
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, requestNullOptions));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex1.getErrorCode());

        CreateCategoryAttributeRequest requestEmptyOptions = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("   ", ""),
                "8GB"
        );

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, requestEmptyOptions));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex2.getErrorCode());

        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-47: createCategoryAttribute_Throws_SelectOptionTooLong - Kiểu SELECT có option dài hơn 100 ký tự ném INVALID_ATTRIBUTE_OPTIONS")
    void createCategoryAttribute_Throws_SelectOptionTooLong() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Điện thoại").status(CategoryStatus.ACTIVE).build();

        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("A".repeat(101)),
                "8GB"
        );

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(5L);
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "RAM")).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    // =========================================================================
    // UNIT TESTS CHO PUT /api/admin/categories/{categoryId}/attributes/{attributeId} (Cập nhật thuộc tính)
    // =========================================================================

    @Test
    @DisplayName("TC-48: updateCategoryAttribute_Success_SameName_UpdateOptions - Cập nhật options cho kiểu SELECT giữ nguyên tên")
    void updateCategoryAttribute_Success_SameName_UpdateOptions() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB"))
                .sampleValue("16GB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB", "32GB", "64GB"),
                "16GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "RAM", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertEquals(attributeId, response.id());
        assertEquals("RAM", response.name());
        assertEquals(CategoryAttributeDataType.SELECT, response.dataType());
        assertEquals(List.of("8GB", "16GB", "32GB", "64GB"), response.options());
        assertEquals("16GB", response.sampleValue());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-49: updateCategoryAttribute_Success_RenameAttribute - Đổi tên thuộc tính không trùng lặp")
    void updateCategoryAttribute_Success_RenameAttribute() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB"))
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "Bộ nhớ RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB"),
                "16GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "Bộ nhớ RAM", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Bộ nhớ RAM", response.name());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-50: updateCategoryAttribute_Success_ChangeCaseSameName - Đổi hoa thường trên cùng một thuộc tính")
    void updateCategoryAttribute_Success_ChangeCaseSameName() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("ram")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "16GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "RAM", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertEquals("RAM", response.name());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-51: updateCategoryAttribute_Success_ChangeDataType_SelectToText_OptionsCleared - Đổi từ SELECT sang TEXT làm sạch options thành null")
    void updateCategoryAttribute_Success_ChangeDataType_SelectToText_OptionsCleared() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("Kích thước màn hình")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("14 inch", "15.6 inch"))
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "Kích thước màn hình",
                CategoryAttributeDataType.TEXT,
                List.of("14 inch"), // Gửi options nhưng dataType là TEXT
                "15.6 inch"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "Kích thước màn hình", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertEquals(CategoryAttributeDataType.TEXT, response.dataType());
        assertNull(response.options());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-52: updateCategoryAttribute_Success_ChangeDataType_TextToSelect_WithOptions - Đổi từ TEXT sang SELECT kèm mảng options hợp lệ")
    void updateCategoryAttribute_Success_ChangeDataType_TextToSelect_WithOptions() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("Độ phân giải")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "Độ phân giải",
                CategoryAttributeDataType.SELECT,
                List.of("Full HD", "2K", "4K"),
                "Full HD"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "Độ phân giải", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertEquals(CategoryAttributeDataType.SELECT, response.dataType());
        assertEquals(List.of("Full HD", "2K", "4K"), response.options());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-53: updateCategoryAttribute_Success_WhitespaceSampleValue_NormalizedToNull - sampleValue chứa khoảng trắng được chuẩn hóa thành null")
    void updateCategoryAttribute_Success_WhitespaceSampleValue_NormalizedToNull() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("Thời gian bảo hành")
                .dataType(CategoryAttributeDataType.TEXT)
                .sampleValue("12 tháng")
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "Thời gian bảo hành",
                CategoryAttributeDataType.TEXT,
                null,
                "   "
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "Thời gian bảo hành", attributeId)).thenReturn(false);
        when(categoryAttributeRepository.save(any(CategoryAttribute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryAttributeResponse response = adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertNull(response.sampleValue());
        verify(categoryAttributeRepository).save(attribute);
    }

    @Test
    @DisplayName("TC-54: updateCategoryAttribute_Throws_CategoryNotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND")
    void updateCategoryAttribute_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-55: updateCategoryAttribute_Throws_AttributeNotFound - Thuộc tính không tồn tại ném ATTRIBUTE_NOT_FOUND")
    void updateCategoryAttribute_Throws_AttributeNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-56: updateCategoryAttribute_Throws_AttributeBelongsToDifferentCategory - Thuộc tính thuộc danh mục khác ném ATTRIBUTE_NOT_FOUND")
    void updateCategoryAttribute_Throws_AttributeBelongsToDifferentCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID otherCategoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category otherCategory = Category.builder().id(otherCategoryId).name("Điện thoại").build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(otherCategory)
                .name("RAM")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-57: updateCategoryAttribute_Throws_DuplicateAttributeName - Trùng tên với thuộc tính khác trong danh mục ném DUPLICATE_ATTRIBUTE_NAME")
    void updateCategoryAttribute_Throws_DuplicateAttributeName() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "CPU",
                CategoryAttributeDataType.TEXT,
                null,
                "Core i7"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "CPU", attributeId)).thenReturn(true);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-58: updateCategoryAttribute_Throws_SelectMissingOptions - Kiểu SELECT nhưng options rỗng ném INVALID_ATTRIBUTE_OPTIONS")
    void updateCategoryAttribute_Throws_SelectMissingOptions() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .build();

        UpdateCategoryAttributeRequest requestNullOptions = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                null,
                "8GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "RAM", attributeId)).thenReturn(false);

        // Act & Assert
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, requestNullOptions));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex1.getErrorCode());

        UpdateCategoryAttributeRequest requestEmptyOptions = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("   ", ""),
                "8GB"
        );

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, requestEmptyOptions));
        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex2.getErrorCode());

        verify(categoryAttributeRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-59: updateCategoryAttribute_Throws_SelectOptionTooLong - Kiểu SELECT có option dài hơn 100 ký tự ném INVALID_ATTRIBUTE_OPTIONS")
    void updateCategoryAttribute_Throws_SelectOptionTooLong() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .build();

        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("A".repeat(101)),
                "8GB"
        );

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, "RAM", attributeId)).thenReturn(false);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).save(any());
    }

    // =========================================================================
    // UNIT TESTS CHO DELETE /api/admin/categories/{categoryId}/attributes/{attributeId} (Xóa thuộc tính)
    // =========================================================================

    @Test
    @DisplayName("TC-60: deleteCategoryAttribute_Success - Xóa thuộc tính thành công khi danh mục có nhiều hơn 1 thuộc tính")
    void deleteCategoryAttribute_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("Cổng VGA")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(3L);

        // Act
        assertDoesNotThrow(() -> adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        // Assert
        verify(categoryAttributeRepository).delete(attribute);
    }

    @Test
    @DisplayName("TC-61: deleteCategoryAttribute_Success_InactiveCategory - Xóa thuộc tính thành công trên danh mục đang INACTIVE")
    void deleteCategoryAttribute_Success_InactiveCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.INACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("Cổng VGA")
                .dataType(CategoryAttributeDataType.TEXT)
                .build();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(2L);

        // Act
        assertDoesNotThrow(() -> adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        // Assert
        verify(categoryAttributeRepository).delete(attribute);
    }

    @Test
    @DisplayName("TC-62: deleteCategoryAttribute_Throws_CategoryNotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND")
    void deleteCategoryAttribute_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-63: deleteCategoryAttribute_Throws_AttributeNotFound - Thuộc tính không tồn tại ném ATTRIBUTE_NOT_FOUND")
    void deleteCategoryAttribute_Throws_AttributeNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-64: deleteCategoryAttribute_Throws_AttributeBelongsToDifferentCategory - Thuộc tính thuộc danh mục khác ném ATTRIBUTE_NOT_FOUND")
    void deleteCategoryAttribute_Throws_AttributeBelongsToDifferentCategory() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID otherCategoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").build();
        Category otherCategory = Category.builder().id(otherCategoryId).name("Điện thoại").build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(otherCategory)
                .name("Camera trước")
                .build();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-65: deleteCategoryAttribute_Throws_CannotDeleteLastAttribute - Chặn xóa thuộc tính cuối cùng của danh mục (BR-11A)")
    void deleteCategoryAttribute_Throws_CannotDeleteLastAttribute() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Laptop").status(CategoryStatus.ACTIVE).build();
        CategoryAttribute attribute = CategoryAttribute.builder()
                .id(attributeId)
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .build();

        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findById(attributeId)).thenReturn(java.util.Optional.of(attribute));
        when(categoryAttributeRepository.countByCategoryId(categoryId)).thenReturn(1L);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.CANNOT_DELETE_LAST_CATEGORY_ATTRIBUTE, ex.getErrorCode());
        verify(categoryAttributeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-66: updateCategoryStatus_Success_ToInactive_WithProductsHibernation - Cập nhật danh mục sang INACTIVE và cưỡng chế ngủ đông sản phẩm")
    void updateCategoryStatus_Success_ToInactive_WithProductsHibernation() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop & Máy tính")
                .description("Mô tả laptop")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CategoryAttribute attr = CategoryAttribute.builder()
                .id(UUID.randomUUID())
                .category(category)
                .name("RAM")
                .dataType(CategoryAttributeDataType.SELECT)
                .options(List.of("8GB", "16GB"))
                .sampleValue("8GB")
                .build();

        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.INACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.hibernateProductsByCategoryId(
                eq(categoryId),
                eq(ProductApprovalStatus.DRAFT),
                eq(ProductVisibilityStatus.UNPUBLISHED)
        )).thenReturn(5);
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of(attr));

        // Act
        CategoryDetailResponse response = adminCategoryService.updateCategoryStatus(categoryId, request);

        // Assert
        assertNotNull(response);
        assertEquals(categoryId, response.id());
        assertEquals(CategoryStatus.INACTIVE, response.status());
        assertEquals("Laptop & Máy tính", response.name());
        assertEquals(1, response.attributes().size());

        verify(categoryRepository).save(category);
        verify(productRepository).hibernateProductsByCategoryId(
                categoryId,
                ProductApprovalStatus.DRAFT,
                ProductVisibilityStatus.UNPUBLISHED
        );
    }

    @Test
    @DisplayName("TC-67: updateCategoryStatus_Success_ToActive - Tái kích hoạt danh mục sang ACTIVE không gọi hibernateProducts")
    void updateCategoryStatus_Success_ToActive() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop & Máy tính")
                .description("Mô tả laptop")
                .status(CategoryStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse response = adminCategoryService.updateCategoryStatus(categoryId, request);

        // Assert
        assertNotNull(response);
        assertEquals(categoryId, response.id());
        assertEquals(CategoryStatus.ACTIVE, response.status());

        verify(categoryRepository).save(category);
        verify(productRepository, never()).hibernateProductsByCategoryId(any(), any(), any());
    }

    @Test
    @DisplayName("TC-68: updateCategoryStatus_Success_IdempotentSameStatus - Thao tác trùng trạng thái trả về kết quả ngay (Idempotent)")
    void updateCategoryStatus_Success_IdempotentSameStatus() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Laptop & Máy tính")
                .description("Mô tả laptop")
                .status(CategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryAttributeRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId)).thenReturn(List.of());

        // Act
        CategoryDetailResponse response = adminCategoryService.updateCategoryStatus(categoryId, request);

        // Assert
        assertNotNull(response);
        assertEquals(categoryId, response.id());
        assertEquals(CategoryStatus.ACTIVE, response.status());

        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).hibernateProductsByCategoryId(any(), any(), any());
    }

    @Test
    @DisplayName("TC-69: updateCategoryStatus_Throws_CategoryNotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND")
    void updateCategoryStatus_Throws_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.INACTIVE);

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryService.updateCategoryStatus(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).hibernateProductsByCategoryId(any(), any(), any());
    }
}
