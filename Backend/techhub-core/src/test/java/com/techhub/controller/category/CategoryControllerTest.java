package com.techhub.controller.category;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategorySummaryResponse;
import com.techhub.model.enums.CategoryAttributeDataType;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.service.category.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho CategoryController (GET /api/categories)")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    @DisplayName("TC-11: Lấy danh mục công khai thành công - Không yêu cầu đăng nhập")
    void getCategories_PublicAccess_NoAuth_Success() {
        UUID catId = UUID.randomUUID();
        CategorySummaryResponse summary = new CategorySummaryResponse(
                catId,
                "Bàn phím cơ",
                "Mô tả bàn phím",
                CategoryStatus.ACTIVE,
                4L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(categoryService.getCategories(null, "name", "asc"))
                .thenReturn(List.of(summary));

        ApiResponse<List<CategorySummaryResponse>> response = categoryController.getCategories(null, "name", "asc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched categories successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("Bàn phím cơ", response.getData().get(0).name());
        verify(categoryService, times(1)).getCategories(null, "name", "asc");
    }

    @Test
    @DisplayName("TC-12: Lấy danh mục có tham số keyword, sortBy và sortDir thành công")
    void getCategories_WithKeywordAndSort_Success() {
        when(categoryService.getCategories("laptop", "createdAt", "desc"))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<CategorySummaryResponse>> response = categoryController.getCategories("laptop", "createdAt", "desc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched categories successfully", response.getMessage());
        verify(categoryService, times(1)).getCategories("laptop", "createdAt", "desc");
    }

    @Test
    @DisplayName("TC-13: Danh sách rỗng - Trả về HTTP 200 OK với mảng rỗng")
    void getCategories_EmptyList_Returns200WithEmptyArray() {
        when(categoryService.getCategories(null, "name", "asc"))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<CategorySummaryResponse>> response = categoryController.getCategories(null, "name", "asc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    @DisplayName("TC-14: Kiểm tra tính chất Public - Controller không gắn @PreAuthorize ở cả class và method")
    void getCategories_PublicAccess_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        // Kiểm tra class không bị khóa bởi @PreAuthorize
        assertFalse(CategoryController.class.isAnnotationPresent(PreAuthorize.class),
                "CategoryController không được gắn @PreAuthorize ở class level");

        // Kiểm tra method getCategories không bị khóa bởi @PreAuthorize
        Method method = CategoryController.class.getMethod("getCategories", String.class, String.class, String.class);
        assertFalse(method.isAnnotationPresent(PreAuthorize.class),
                "Method getCategories không được gắn @PreAuthorize để đảm bảo truy cập Public");

        // Kiểm tra Mapping
        assertTrue(CategoryController.class.isAnnotationPresent(RestController.class));
        RequestMapping reqMapping = CategoryController.class.getAnnotation(RequestMapping.class);
        assertNotNull(reqMapping);
        assertArrayEquals(new String[]{"/api/categories"}, reqMapping.value());
        assertTrue(method.isAnnotationPresent(GetMapping.class));
    }

    @Test
    @DisplayName("TC-15: Cấu trúc response đúng chuẩn hợp đồng DTO CategorySummaryResponse")
    void getCategories_ResponseStructureMatchesContract() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CategorySummaryResponse item = new CategorySummaryResponse(
                id,
                "Chuột máy tính",
                "Chuột văn phòng & gaming",
                CategoryStatus.ACTIVE,
                3L,
                now,
                now
        );

        when(categoryService.getCategories(null, "name", "asc"))
                .thenReturn(List.of(item));

        ApiResponse<List<CategorySummaryResponse>> response = categoryController.getCategories(null, "name", "asc");

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        CategorySummaryResponse dataItem = response.getData().get(0);
        assertEquals(id, dataItem.id());
        assertEquals("Chuột máy tính", dataItem.name());
        assertEquals("Chuột văn phòng & gaming", dataItem.description());
        assertEquals(CategoryStatus.ACTIVE, dataItem.status());
        assertEquals(3L, dataItem.attributesCount());
        assertEquals(now, dataItem.createdAt());
        assertEquals(now, dataItem.updatedAt());
    }

    // =========================================================================
    // Tests cho getCategoryAttributes (API 28: GET /api/categories/{id}/attributes)
    // =========================================================================

    @Test
    @DisplayName("TC-11: getCategoryAttributes_PublicAccess_NoAuth_Success")
    void getCategoryAttributes_PublicAccess_NoAuth_Success() {
        UUID categoryId = UUID.randomUUID();
        CategoryAttributeResponse attr1 = new CategoryAttributeResponse(
                UUID.randomUUID(),
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB"),
                "16GB",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CategoryAttributeResponse attr2 = new CategoryAttributeResponse(
                UUID.randomUUID(),
                "GPU",
                CategoryAttributeDataType.TEXT,
                Collections.emptyList(),
                "RTX 4060",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(categoryService.getCategoryAttributes(categoryId))
                .thenReturn(List.of(attr1, attr2));

        ApiResponse<List<CategoryAttributeResponse>> response = categoryController.getCategoryAttributes(categoryId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Fetched category attributes successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals("RAM", response.getData().get(0).name());
        assertEquals("GPU", response.getData().get(1).name());
        verify(categoryService, times(1)).getCategoryAttributes(categoryId);
    }

    @Test
    @DisplayName("TC-12: getCategoryAttributes_CategoryNotFound_Returns404")
    void getCategoryAttributes_CategoryNotFound_Returns404() {
        UUID categoryId = UUID.randomUUID();
        when(categoryService.getCategoryAttributes(categoryId))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                categoryController.getCategoryAttributes(categoryId)
        );

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(categoryService, times(1)).getCategoryAttributes(categoryId);
    }

    @Test
    @DisplayName("TC-13: getCategoryAttributes_CategoryInactive_Returns400")
    void getCategoryAttributes_CategoryInactive_Returns400() {
        UUID categoryId = UUID.randomUUID();
        when(categoryService.getCategoryAttributes(categoryId))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_INACTIVE));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                categoryController.getCategoryAttributes(categoryId)
        );

        assertEquals(ErrorCode.CATEGORY_INACTIVE, ex.getErrorCode());
        verify(categoryService, times(1)).getCategoryAttributes(categoryId);
    }

    @Test
    @DisplayName("TC-14: getCategoryAttributes_PublicAccess_NoPreAuthorizeAnnotation")
    void getCategoryAttributes_PublicAccess_NoPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = CategoryController.class.getMethod("getCategoryAttributes", UUID.class);

        // Đảm bảo không có @PreAuthorize trên method
        assertFalse(method.isAnnotationPresent(PreAuthorize.class),
                "Method getCategoryAttributes không được gắn @PreAuthorize để bảo đảm truy cập Public");

        // Đảm bảo có mapping @GetMapping("/{id}/attributes")
        assertTrue(method.isAnnotationPresent(GetMapping.class));
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{"/{id}/attributes"}, getMapping.value());
    }

    @Test
    @DisplayName("TC-15: getCategoryAttributes_ResponseStructureMatchesContract")
    void getCategoryAttributes_ResponseStructureMatchesContract() {
        UUID attrId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        CategoryAttributeResponse item = new CategoryAttributeResponse(
                attrId,
                "Dung lượng RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB", "32GB"),
                "16GB",
                now,
                now
        );

        when(categoryService.getCategoryAttributes(catId))
                .thenReturn(List.of(item));

        ApiResponse<List<CategoryAttributeResponse>> response = categoryController.getCategoryAttributes(catId);

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        CategoryAttributeResponse dataItem = response.getData().get(0);
        assertEquals(attrId, dataItem.id());
        assertEquals("Dung lượng RAM", dataItem.name());
        assertEquals(CategoryAttributeDataType.SELECT, dataItem.dataType());
        assertEquals(List.of("8GB", "16GB", "32GB"), dataItem.options());
        assertEquals("16GB", dataItem.sampleValue());
        assertEquals(now, dataItem.createdAt());
        assertEquals(now, dataItem.updatedAt());
    }
}

