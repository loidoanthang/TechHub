package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateCategoryAttributeRequest;
import com.techhub.model.dto.request.CreateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryAttributeRequest;
import com.techhub.model.dto.request.UpdateCategoryRequest;
import com.techhub.model.dto.request.UpdateCategoryStatusRequest;
import com.techhub.model.dto.response.CategoryAttributeResponse;
import com.techhub.model.dto.response.CategoryDetailResponse;
import com.techhub.model.enums.CategoryAttributeDataType;
import com.techhub.model.enums.CategoryStatus;
import com.techhub.service.admin.AdminCategoryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminCategoryController (POST /api/admin/categories)")
class AdminCategoryControllerTest {

    @Mock
    private AdminCategoryService adminCategoryService;

    @InjectMocks
    private AdminCategoryController adminCategoryController;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TC-11: controller_createCategory_Success - Gọi service và trả về HTTP 201 Created")
    void controller_createCategory_Success() {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Điện thoại",
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("Chipset", CategoryAttributeDataType.TEXT, null, "A16"))
        );

        UUID categoryId = UUID.randomUUID();
        CategoryDetailResponse detailResponse = new CategoryDetailResponse(
                categoryId,
                "Điện thoại",
                "Mô tả",
                CategoryStatus.ACTIVE,
                List.of(new CategoryAttributeResponse(UUID.randomUUID(), "Chipset", CategoryAttributeDataType.TEXT, null, "A16", LocalDateTime.now(), LocalDateTime.now())),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.createCategory(request)).thenReturn(detailResponse);

        // Act
        ApiResponse<CategoryDetailResponse> response = adminCategoryController.createCategory(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(categoryId, response.getData().id());
        assertEquals("Category created successfully", response.getMessage());
        verify(adminCategoryService).createCategory(request);
    }

    @Test
    @DisplayName("TC-12: controller_createCategory_PreAuthorizeCheck - Class và Method được bảo vệ bằng ROLE_ADMIN")
    void controller_createCategory_PreAuthorizeCheck() {
        PreAuthorize classAnnotation = AdminCategoryController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classAnnotation, "AdminCategoryController phải có annotation @PreAuthorize");
        assertEquals("hasRole('ADMIN')", classAnnotation.value(), "Annotation phải yêu cầu hasRole('ADMIN')");
    }

    @Test
    @DisplayName("TC-13: validation_createCategory_Fail_BlankCategoryName - Tên danh mục trống vi phạm Bean Validation")
    void validation_createCategory_Fail_BlankCategoryName() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "",
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("Chipset", CategoryAttributeDataType.TEXT, null, "A16"))
        );

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TC-14: validation_createCategory_Fail_CategoryNameTooLong - Tên danh mục vượt quá 100 ký tự")
    void validation_createCategory_Fail_CategoryNameTooLong() {
        String longName = "a".repeat(101);
        CreateCategoryRequest request = new CreateCategoryRequest(
                longName,
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("Chipset", CategoryAttributeDataType.TEXT, null, "A16"))
        );

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TC-15: validation_createCategory_Fail_BlankAttributeName - Tên thuộc tính trống")
    void validation_createCategory_Fail_BlankAttributeName() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Điện thoại",
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("", CategoryAttributeDataType.TEXT, null, "A16"))
        );

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Attribute name is required")));
    }

    @Test
    @DisplayName("TC-16: validation_createCategory_Fail_EmptyAttributesList - Danh sách thuộc tính rỗng")
    void validation_createCategory_Fail_EmptyAttributesList() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Điện thoại",
                "Mô tả",
                List.of()
        );

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("attributes")));
    }

    @Test
    @DisplayName("TC-17: validation_createCategory_Fail_Exceed50Attributes - Danh sách thuộc tính vượt 50 phần tử")
    void validation_createCategory_Fail_Exceed50Attributes() {
        List<CreateCategoryAttributeRequest> list = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            list.add(new CreateCategoryAttributeRequest("Attr" + i, CategoryAttributeDataType.TEXT, null, null));
        }
        CreateCategoryRequest request = new CreateCategoryRequest("Điện thoại", "Mô tả", list);

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("attributes")));
    }

    @Test
    @DisplayName("TC-18: validation_createCategory_Fail_NullDataType - Kiểu dữ liệu thuộc tính bị null")
    void validation_createCategory_Fail_NullDataType() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Điện thoại",
                "Mô tả",
                List.of(new CreateCategoryAttributeRequest("Chipset", null, null, "A16"))
        );

        Set<ConstraintViolation<CreateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Data type is required")));
    }

    @Test
    @DisplayName("TC-19: controller_getCategories_Success - Trả về HTTP 200 OK kèm danh sách phân trang")
    void controller_getCategories_Success() {
        // Arrange
        com.techhub.model.dto.response.CategorySummaryResponse summary = new com.techhub.model.dto.response.CategorySummaryResponse(
                UUID.randomUUID(),
                "Điện thoại",
                "Mô tả",
                CategoryStatus.ACTIVE,
                4L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        com.techhub.common.PageResponse<com.techhub.model.dto.response.CategorySummaryResponse> pageResponse =
                new com.techhub.common.PageResponse<>(List.of(summary), 0, 20, 1, 1, true);

        when(adminCategoryService.getCategories("điện", CategoryStatus.ACTIVE, 0, 20, "createdAt", "desc"))
                .thenReturn(pageResponse);

        // Act
        ApiResponse<com.techhub.common.PageResponse<com.techhub.model.dto.response.CategorySummaryResponse>> response =
                adminCategoryController.getCategories("điện", CategoryStatus.ACTIVE, 0, 20, "createdAt", "desc");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().content().size());
        assertEquals("Category list retrieved successfully", response.getMessage());
        verify(adminCategoryService).getCategories("điện", CategoryStatus.ACTIVE, 0, 20, "createdAt", "desc");
    }

    // =========================================================================
    // API 3: GET /api/admin/categories/{id} (View Category Details)
    // =========================================================================

    @Test
    @DisplayName("TC-20: controller_getCategoryDetail_Success - Lấy chi tiết danh mục thành công HTTP 200")
    void controller_getCategoryDetail_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        CategoryDetailResponse detail = new CategoryDetailResponse(
                categoryId,
                "Điện thoại & Phụ kiện",
                "Mô tả chi tiết",
                CategoryStatus.ACTIVE,
                List.of(
                        new CategoryAttributeResponse(
                                UUID.randomUUID(),
                                "RAM",
                                CategoryAttributeDataType.SELECT,
                                List.of("4GB", "8GB"),
                                "8GB",
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        )
                ),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.getCategoryDetail(categoryId)).thenReturn(detail);

        // Act
        ApiResponse<CategoryDetailResponse> response =
                adminCategoryController.getCategoryDetail(categoryId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category details retrieved successfully", response.getMessage());
        assertEquals(categoryId, response.getData().id());
        assertEquals("Điện thoại & Phụ kiện", response.getData().name());
        assertEquals(1, response.getData().attributes().size());
        verify(adminCategoryService).getCategoryDetail(categoryId);
    }

    @Test
    @DisplayName("TC-21: controller_getCategoryDetail_NotFound - Category không tồn tại ném CATEGORY_NOT_FOUND")
    void controller_getCategoryDetail_NotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        when(adminCategoryService.getCategoryDetail(categoryId))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.getCategoryDetail(categoryId));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(adminCategoryService).getCategoryDetail(categoryId);
    }

    // =========================================================================
    // API 4: PUT /api/admin/categories/{id} (Update Category)
    // =========================================================================

    @Test
    @DisplayName("TC-22: controller_updateCategory_Success - Cập nhật danh mục thành công HTTP 200")
    void controller_updateCategory_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop Mới", "Mô tả mới");
        CategoryDetailResponse detail = new CategoryDetailResponse(
                categoryId,
                "Laptop Mới",
                "Mô tả mới",
                CategoryStatus.ACTIVE,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.updateCategory(categoryId, request)).thenReturn(detail);

        // Act
        ApiResponse<CategoryDetailResponse> response =
                adminCategoryController.updateCategory(categoryId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category updated successfully", response.getMessage());
        assertEquals("Laptop Mới", response.getData().name());
        assertEquals("Mô tả mới", response.getData().description());
        verify(adminCategoryService).updateCategory(categoryId, request);
    }

    @Test
    @DisplayName("TC-23: controller_updateCategory_NotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void controller_updateCategory_NotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop Mới", "Mô tả mới");

        when(adminCategoryService.updateCategory(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategory(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        verify(adminCategoryService).updateCategory(categoryId, request);
    }

    @Test
    @DisplayName("TC-24: controller_updateCategory_Conflict - Tên danh mục trùng lặp ném CATEGORY_NAME_ALREADY_EXISTS (HTTP 409)")
    void controller_updateCategory_Conflict() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Điện thoại", "Mô tả");

        when(adminCategoryService.updateCategory(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategory(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(adminCategoryService).updateCategory(categoryId, request);
    }

    @Test
    @DisplayName("TC-25: validation_updateCategory_Fail_BlankName - Tên danh mục rỗng hoặc toàn khoảng trắng bị chặn")
    void validation_updateCategory_Fail_BlankName() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("   ", "Mô tả");

        Set<ConstraintViolation<UpdateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name is required")));
    }

    @Test
    @DisplayName("TC-26: validation_updateCategory_Fail_NameTooShort - Tên danh mục dưới 2 ký tự sau khi trim bị chặn")
    void validation_updateCategory_Fail_NameTooShort() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(" a ", "Mô tả");

        Set<ConstraintViolation<UpdateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name must be between 2 and 100 characters")));
    }

    @Test
    @DisplayName("TC-27: validation_updateCategory_Fail_NameTooLong - Tên danh mục vượt quá 100 ký tự bị chặn")
    void validation_updateCategory_Fail_NameTooLong() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("a".repeat(101), "Mô tả");

        Set<ConstraintViolation<UpdateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category name must be between 2 and 100 characters")));
    }

    @Test
    @DisplayName("TC-28: validation_updateCategory_Fail_DescriptionTooLong - Mô tả danh mục vượt quá 500 ký tự bị chặn")
    void validation_updateCategory_Fail_DescriptionTooLong() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Laptop", "a".repeat(501));

        Set<ConstraintViolation<UpdateCategoryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Description cannot exceed 500 characters")));
    }

    // =========================================================================
    // UNIT TESTS CHO POST /api/admin/categories/{id}/attributes (Thêm thuộc tính kỹ thuật mẫu)
    // =========================================================================

    @Test
    @DisplayName("TC-29: controller_createCategoryAttribute_Success - Controller gọi service và trả về HTTP 201 Created")
    void controller_createCategoryAttribute_Success() {
        UUID categoryId = UUID.randomUUID();
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Chuẩn Bluetooth",
                CategoryAttributeDataType.SELECT,
                List.of("Bluetooth 5.0", "Bluetooth 5.2"),
                "Bluetooth 5.2"
        );

        UUID attrId = UUID.randomUUID();
        CategoryAttributeResponse attrResponse = new CategoryAttributeResponse(
                attrId,
                "Chuẩn Bluetooth",
                CategoryAttributeDataType.SELECT,
                List.of("Bluetooth 5.0", "Bluetooth 5.2"),
                "Bluetooth 5.2",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.createCategoryAttribute(categoryId, request)).thenReturn(attrResponse);

        // Act
        ApiResponse<CategoryAttributeResponse> response =
                adminCategoryController.createCategoryAttribute(categoryId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category attribute created successfully", response.getMessage());
        assertEquals(attrId, response.getData().id());
        assertEquals("Chuẩn Bluetooth", response.getData().name());
    }

    @Test
    @DisplayName("TC-30: controller_createCategoryAttribute_NotFound - Service ném CATEGORY_NOT_FOUND")
    void controller_createCategoryAttribute_NotFound() {
        UUID categoryId = UUID.randomUUID();
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(adminCategoryService.createCategoryAttribute(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-31: controller_createCategoryAttribute_LimitExceeded - Service ném ATTRIBUTE_LIMIT_EXCEEDED")
    void controller_createCategoryAttribute_LimitExceeded() {
        UUID categoryId = UUID.randomUUID();
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "Thuộc tính thứ 51",
                CategoryAttributeDataType.TEXT,
                null,
                "Test"
        );

        when(adminCategoryService.createCategoryAttribute(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.ATTRIBUTE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-32: controller_createCategoryAttribute_DuplicateName - Service ném DUPLICATE_ATTRIBUTE_NAME")
    void controller_createCategoryAttribute_DuplicateName() {
        UUID categoryId = UUID.randomUUID();
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(adminCategoryService.createCategoryAttribute(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_NAME));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.createCategoryAttribute(categoryId, request));

        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-33: validation_createCategoryAttribute_Fail_BlankName - Tên thuộc tính để trống bị chặn bởi Bean Validation")
    void validation_createCategoryAttribute_Fail_BlankName() {
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "   ",
                CategoryAttributeDataType.TEXT,
                null,
                "Test"
        );

        Set<ConstraintViolation<CreateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Attribute name is required")));
    }

    @Test
    @DisplayName("TC-34: validation_createCategoryAttribute_Fail_NullDataType - Kiểu dữ liệu null bị chặn bởi Bean Validation")
    void validation_createCategoryAttribute_Fail_NullDataType() {
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                null,
                null,
                "Test"
        );

        Set<ConstraintViolation<CreateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Data type is required")));
    }

    @Test
    @DisplayName("TC-35: validation_createCategoryAttribute_Fail_NameTooLong - Tên thuộc tính dài hơn 100 ký tự bị chặn")
    void validation_createCategoryAttribute_Fail_NameTooLong() {
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "A".repeat(101),
                CategoryAttributeDataType.TEXT,
                null,
                "Test"
        );

        Set<ConstraintViolation<CreateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Attribute name must be between 1 and 100 characters")));
    }

    @Test
    @DisplayName("TC-36: validation_createCategoryAttribute_Fail_SampleValueTooLong - sampleValue dài hơn 255 ký tự bị chặn")
    void validation_createCategoryAttribute_Fail_SampleValueTooLong() {
        CreateCategoryAttributeRequest request = new CreateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "A".repeat(256)
        );

        Set<ConstraintViolation<CreateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Sample value cannot exceed 255 characters")));
    }

    // =========================================================================
    // UNIT TESTS CHO PUT /api/admin/categories/{categoryId}/attributes/{attributeId} (Cập nhật thuộc tính)
    // =========================================================================

    @Test
    @DisplayName("TC-37: controller_updateCategoryAttribute_Success - Cập nhật thuộc tính danh mục thành công HTTP 200")
    void controller_updateCategoryAttribute_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB", "32GB"),
                "16GB"
        );

        CategoryAttributeResponse mockResponse = new CategoryAttributeResponse(
                attributeId,
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of("8GB", "16GB", "32GB"),
                "16GB",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request)).thenReturn(mockResponse);

        // Act
        ApiResponse<CategoryAttributeResponse> response =
                adminCategoryController.updateCategoryAttribute(categoryId, attributeId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category attribute updated successfully", response.getMessage());
        assertEquals("RAM", response.getData().name());
        verify(adminCategoryService).updateCategoryAttribute(categoryId, attributeId, request);
    }

    @Test
    @DisplayName("TC-38: controller_updateCategoryAttribute_CategoryNotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void controller_updateCategoryAttribute_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-39: controller_updateCategoryAttribute_AttributeNotFound - Thuộc tính không tồn tại ném ATTRIBUTE_NOT_FOUND (HTTP 404)")
    void controller_updateCategoryAttribute_AttributeNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        when(adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request))
                .thenThrow(new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-40: controller_updateCategoryAttribute_DuplicateName - Trùng tên thuộc tính ném DUPLICATE_ATTRIBUTE_NAME (HTTP 400)")
    void controller_updateCategoryAttribute_DuplicateName() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "CPU",
                CategoryAttributeDataType.TEXT,
                null,
                "Core i7"
        );

        when(adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTE_NAME));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.DUPLICATE_ATTRIBUTE_NAME, ex.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-41: controller_updateCategoryAttribute_InvalidOptions - Sai options cho kiểu SELECT ném INVALID_ATTRIBUTE_OPTIONS (HTTP 400)")
    void controller_updateCategoryAttribute_InvalidOptions() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.SELECT,
                List.of(),
                "8GB"
        );

        when(adminCategoryService.updateCategoryAttribute(categoryId, attributeId, request))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ATTRIBUTE_OPTIONS));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategoryAttribute(categoryId, attributeId, request));

        assertEquals(ErrorCode.INVALID_ATTRIBUTE_OPTIONS, ex.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-42: validation_updateCategoryAttribute_Fail_BlankName - Tên thuộc tính rỗng hoặc khoảng trắng ném lỗi validation")
    void validation_updateCategoryAttribute_Fail_BlankName() {
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "   ",
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        Set<ConstraintViolation<UpdateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Attribute name is required")));
    }

    @Test
    @DisplayName("TC-43: validation_updateCategoryAttribute_Fail_NullDataType - Kiểu dữ liệu null ném lỗi validation")
    void validation_updateCategoryAttribute_Fail_NullDataType() {
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                null,
                null,
                "8GB"
        );

        Set<ConstraintViolation<UpdateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Data type is required")));
    }

    @Test
    @DisplayName("TC-44: validation_updateCategoryAttribute_Fail_NameTooLong - Tên thuộc tính dài hơn 100 ký tự ném lỗi validation")
    void validation_updateCategoryAttribute_Fail_NameTooLong() {
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "A".repeat(101),
                CategoryAttributeDataType.TEXT,
                null,
                "8GB"
        );

        Set<ConstraintViolation<UpdateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Attribute name must be between 1 and 100 characters")));
    }

    @Test
    @DisplayName("TC-45: validation_updateCategoryAttribute_Fail_SampleValueTooLong - Sample value dài hơn 255 ký tự ném lỗi validation")
    void validation_updateCategoryAttribute_Fail_SampleValueTooLong() {
        UpdateCategoryAttributeRequest request = new UpdateCategoryAttributeRequest(
                "RAM",
                CategoryAttributeDataType.TEXT,
                null,
                "A".repeat(256)
        );

        Set<ConstraintViolation<UpdateCategoryAttributeRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Sample value cannot exceed 255 characters")));
    }

    // =========================================================================
    // UNIT TESTS CHO DELETE /api/admin/categories/{categoryId}/attributes/{attributeId} (Xóa thuộc tính)
    // =========================================================================

    @Test
    @DisplayName("TC-46: controller_deleteCategoryAttribute_Success - Xóa thuộc tính danh mục thành công HTTP 200")
    void controller_deleteCategoryAttribute_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        doNothing().when(adminCategoryService).deleteCategoryAttribute(categoryId, attributeId);

        // Act
        ApiResponse<Void> response =
                adminCategoryController.deleteCategoryAttribute(categoryId, attributeId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category attribute deleted successfully", response.getMessage());
        assertNull(response.getData());
        verify(adminCategoryService).deleteCategoryAttribute(categoryId, attributeId);
    }

    @Test
    @DisplayName("TC-47: controller_deleteCategoryAttribute_CategoryNotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void controller_deleteCategoryAttribute_CategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        doThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND))
                .when(adminCategoryService).deleteCategoryAttribute(categoryId, attributeId);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-48: controller_deleteCategoryAttribute_AttributeNotFound - Thuộc tính không tồn tại ném ATTRIBUTE_NOT_FOUND (HTTP 404)")
    void controller_deleteCategoryAttribute_AttributeNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        doThrow(new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND))
                .when(adminCategoryService).deleteCategoryAttribute(categoryId, attributeId);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.ATTRIBUTE_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-49: controller_deleteCategoryAttribute_CannotDeleteLastAttribute - Thuộc tính cuối cùng ném CANNOT_DELETE_LAST_CATEGORY_ATTRIBUTE (HTTP 400)")
    void controller_deleteCategoryAttribute_CannotDeleteLastAttribute() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UUID attributeId = UUID.randomUUID();

        doThrow(new BusinessException(ErrorCode.CANNOT_DELETE_LAST_CATEGORY_ATTRIBUTE))
                .when(adminCategoryService).deleteCategoryAttribute(categoryId, attributeId);

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.deleteCategoryAttribute(categoryId, attributeId));

        assertEquals(ErrorCode.CANNOT_DELETE_LAST_CATEGORY_ATTRIBUTE, ex.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorCode().getStatus());
    }

    @Test
    @DisplayName("TC-50: controller_updateCategoryStatus_Success - Đổi trạng thái danh mục thành công trả về HTTP 200 OK")
    void controller_updateCategoryStatus_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.INACTIVE);

        CategoryDetailResponse detail = new CategoryDetailResponse(
                categoryId,
                "Laptop & Máy tính",
                "Mô tả laptop",
                CategoryStatus.INACTIVE,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(adminCategoryService.updateCategoryStatus(categoryId, request)).thenReturn(detail);

        // Act
        ApiResponse<CategoryDetailResponse> response = adminCategoryController.updateCategoryStatus(categoryId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category status updated successfully", response.getMessage());
        assertEquals(CategoryStatus.INACTIVE, response.getData().status());
        verify(adminCategoryService).updateCategoryStatus(categoryId, request);
    }

    @Test
    @DisplayName("TC-51: controller_updateCategoryStatus_NotFound - Danh mục không tồn tại ném CATEGORY_NOT_FOUND (HTTP 404)")
    void controller_updateCategoryStatus_NotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(CategoryStatus.INACTIVE);

        when(adminCategoryService.updateCategoryStatus(categoryId, request))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminCategoryController.updateCategoryStatus(categoryId, request));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorCode().getStatus());
        verify(adminCategoryService).updateCategoryStatus(categoryId, request);
    }

    @Test
    @DisplayName("TC-52: validation_updateCategoryStatus_Fail_NullStatus - Ràng buộc status null bị chặn bởi @NotNull")
    void validation_updateCategoryStatus_Fail_NullStatus() {
        // Arrange
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest(null);

        // Act
        Set<ConstraintViolation<UpdateCategoryStatusRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Category status is required")));
    }

    @Test
    @DisplayName("TC-53: controller_updateCategoryStatus_HasPreAuthorizeAdmin - Kiểm tra Controller có @PreAuthorize ROLE_ADMIN")
    void controller_updateCategoryStatus_HasPreAuthorizeAdmin() {
        // Assert: Class AdminCategoryController được bảo vệ bởi @PreAuthorize("hasRole('ADMIN')")
        PreAuthorize classAnnotation = AdminCategoryController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classAnnotation, "AdminCategoryController must have @PreAuthorize");
        assertEquals("hasRole('ADMIN')", classAnnotation.value());
    }
}
