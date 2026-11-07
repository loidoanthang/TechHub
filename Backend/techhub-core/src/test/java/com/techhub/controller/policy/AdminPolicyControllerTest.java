package com.techhub.controller.policy;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.policy.UpdatePlatformPolicyRequest;
import com.techhub.model.dto.response.policy.PlatformPolicyResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.policy.AdminPolicyService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminPolicyController (Platform Policies)")
class AdminPolicyControllerTest {

    @Mock
    private AdminPolicyService adminPolicyService;

    @InjectMocks
    private AdminPolicyController controller;

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TC-ADM-POL-01b: Controller Happy Path - Lấy danh sách chính sách trả về 200 OK")
    void getAllPolicies_Success_Returns200() {
        PlatformPolicyResponse policy = PlatformPolicyResponse.builder()
                .id(UUID.randomUUID())
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.05")
                .description("Tỷ lệ hoa hồng chiết khấu sàn (5%)")
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(adminPolicyService.getAllPolicies()).thenReturn(List.of(policy));

        ApiResponse<List<PlatformPolicyResponse>> response = controller.getAllPolicies();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Platform policies retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("PLATFORM_COMMISSION_RATE", response.getData().get(0).policyKey());
        verify(adminPolicyService, times(1)).getAllPolicies();
    }

    @Test
    @DisplayName("TC-ADM-POL-07: Authorization & Mapping - Bắt buộc @PreAuthorize(\"hasRole('ADMIN')\") và @GetMapping")
    void getAllPolicies_PreAuthorize_RequiresRoleAdmin() throws NoSuchMethodException {
        Method method = AdminPolicyController.class.getMethod("getAllPolicies");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getAllPolicies phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('ADMIN')"),
                "@PreAuthorize phải yêu cầu hasRole('ADMIN')");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getAllPolicies phải có annotation @GetMapping");

        RequestMapping classMapping = AdminPolicyController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/admin/policies"}, classMapping.value());
    }

    // =========================================================================
    // UNIT TESTS CHO UPDATE PLATFORM POLICY (PUT /api/admin/policies)
    // =========================================================================

    @Test
    @DisplayName("TC-ADM-UPOL-14: Controller Happy Path - Cập nhật chính sách thành công trả về 200 OK")
    void updatePolicy_Success_Returns200() {
        UUID adminId = UUID.randomUUID();
        UpdatePlatformPolicyRequest request = new UpdatePlatformPolicyRequest(
                "PLATFORM_COMMISSION_RATE", "0.08", "Cập nhật hoa hồng 8%"
        );

        PlatformPolicyResponse mockResponse = PlatformPolicyResponse.builder()
                .id(UUID.randomUUID())
                .policyKey("PLATFORM_COMMISSION_RATE")
                .policyValue("0.08")
                .description("Cập nhật hoa hồng 8%")
                .updatedById(adminId)
                .updatedByName("Admin Test")
                .updatedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(adminId);
            when(adminPolicyService.updatePolicy(adminId, request)).thenReturn(mockResponse);

            ApiResponse<PlatformPolicyResponse> response = controller.updatePolicy(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("Platform policy updated successfully", response.getMessage());
            assertEquals("PLATFORM_COMMISSION_RATE", response.getData().policyKey());
            assertEquals("0.08", response.getData().policyValue());
            assertEquals(adminId, response.getData().updatedById());

            verify(adminPolicyService).updatePolicy(adminId, request);
        }
    }

    @Test
    @DisplayName("TC-ADM-UPOL-15: Validation - Request body vi phạm @NotBlank hoặc @Size")
    void updatePolicy_ValidationError_Enforced() {
        // Blank policyKey
        UpdatePlatformPolicyRequest blankKey = new UpdatePlatformPolicyRequest("", "0.05", "Valid desc");
        Set<ConstraintViolation<UpdatePlatformPolicyRequest>> violations1 = validator.validate(blankKey);
        assertFalse(violations1.isEmpty());
        assertTrue(violations1.stream().anyMatch(v -> v.getPropertyPath().toString().equals("policyKey")));

        // Blank policyValue
        UpdatePlatformPolicyRequest blankValue = new UpdatePlatformPolicyRequest("PLATFORM_COMMISSION_RATE", "   ", "Valid desc");
        Set<ConstraintViolation<UpdatePlatformPolicyRequest>> violations2 = validator.validate(blankValue);
        assertFalse(violations2.isEmpty());
        assertTrue(violations2.stream().anyMatch(v -> v.getPropertyPath().toString().equals("policyValue")));

        // Description > 1000 chars
        String longDesc = "A".repeat(1001);
        UpdatePlatformPolicyRequest tooLongDesc = new UpdatePlatformPolicyRequest("PLATFORM_COMMISSION_RATE", "0.05", longDesc);
        Set<ConstraintViolation<UpdatePlatformPolicyRequest>> violations3 = validator.validate(tooLongDesc);
        assertFalse(violations3.isEmpty());
        assertTrue(violations3.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));

        // Key > 100 chars
        UpdatePlatformPolicyRequest tooLongKey = new UpdatePlatformPolicyRequest("K".repeat(101), "0.05", "Valid desc");
        Set<ConstraintViolation<UpdatePlatformPolicyRequest>> violations4 = validator.validate(tooLongKey);
        assertFalse(violations4.isEmpty());
        assertTrue(violations4.stream().anyMatch(v -> v.getPropertyPath().toString().equals("policyKey")));

        // Value > 255 chars
        UpdatePlatformPolicyRequest tooLongVal = new UpdatePlatformPolicyRequest("PLATFORM_COMMISSION_RATE", "V".repeat(256), "Valid desc");
        Set<ConstraintViolation<UpdatePlatformPolicyRequest>> violations5 = validator.validate(tooLongVal);
        assertFalse(violations5.isEmpty());
        assertTrue(violations5.stream().anyMatch(v -> v.getPropertyPath().toString().equals("policyValue")));
    }

    @Test
    @DisplayName("TC-ADM-UPOL-16: Authorization & Mapping - Bắt buộc @PreAuthorize(\"hasRole('ADMIN')\") và @PutMapping")
    void updatePolicy_PreAuthorize_RequiresRoleAdmin() throws NoSuchMethodException {
        Method method = AdminPolicyController.class.getMethod("updatePolicy", UpdatePlatformPolicyRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updatePolicy phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('ADMIN')"),
                "@PreAuthorize phải yêu cầu hasRole('ADMIN')");

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        assertNotNull(putMapping, "Method updatePolicy phải có annotation @PutMapping");
    }
}
