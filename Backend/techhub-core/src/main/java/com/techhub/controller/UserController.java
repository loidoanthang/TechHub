package com.techhub.controller;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.UpdateProfileRequest;
import com.techhub.model.dto.response.UserProfileResponse;
import com.techhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userService.getProfile(), "User profile retrieved successfully.", null);
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request), "Profile updated successfully.", null);
    }
}
