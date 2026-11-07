package com.techhub.controller.policy;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.policy.UpdatePlatformPolicyRequest;
import com.techhub.model.dto.response.policy.PlatformPolicyResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.policy.AdminPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final AdminPolicyService adminPolicyService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PlatformPolicyResponse>> getAllPolicies() {
        List<PlatformPolicyResponse> policies = adminPolicyService.getAllPolicies();
        return ApiResponse.success(policies, "Platform policies retrieved successfully", null);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PlatformPolicyResponse> updatePolicy(
            @Valid @RequestBody UpdatePlatformPolicyRequest request
    ) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        PlatformPolicyResponse policy = adminPolicyService.updatePolicy(adminId, request);
        return ApiResponse.success(policy, "Platform policy updated successfully", null);
    }
}
