package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.UpdateUserStatusRequest;
import com.techhub.model.dto.response.AdminUserResponse;
import com.techhub.model.enums.UserStatus;
import com.techhub.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<AdminUserResponse> data = adminUserService.getUsers(
                keyword, status, emailVerified, page, size, sortBy, sortDir
        );
        return ApiResponse.success(data, "User list retrieved successfully.", null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        AdminUserResponse data = adminUserService.updateUserStatus(id, request);
        return ApiResponse.success(data, "User status updated successfully.", null);
    }
}
