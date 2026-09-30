package com.techhub.service.admin;

import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.AdminUserResponse;
import com.techhub.model.dto.request.UpdateUserStatusRequest;
import com.techhub.model.enums.UserStatus;

import java.util.UUID;

public interface AdminUserService {

    PageResponse<AdminUserResponse> getUsers(
            String keyword,
            UserStatus status,
            Boolean emailVerified,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request);
}
