package com.techhub.controller.admin;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.response.AdminUserResponse;
import com.techhub.model.enums.UserStatus;
import com.techhub.service.admin.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminUserController")
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("getUsers gọi service đúng tham số và trả về ApiResponse chuẩn")
    void getUsers_CallsServiceAndReturnsSuccessResponse() {
        AdminUserResponse userResponse = new AdminUserResponse(
                UUID.randomUUID(),
                "Thang",
                "Loi",
                "thang@techhub.com",
                "0987654321",
                true,
                UserStatus.ACTIVE,
                "123 Le Loi, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh",
                "https://techhub.com/avatar.png",
                LocalDateTime.now()
        );
        PageResponse<AdminUserResponse> mockPageResponse = new PageResponse<>(
                List.of(userResponse), 0, 20, 1, 1, true
        );

        when(adminUserService.getUsers("thang", UserStatus.ACTIVE, true, 0, 20, "createdAt", "desc"))
                .thenReturn(mockPageResponse);

        ApiResponse<PageResponse<AdminUserResponse>> response = adminUserController.getUsers(
                "thang", UserStatus.ACTIVE, true, 0, 20, "createdAt", "desc"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User list retrieved successfully.", response.getMessage());
        assertEquals(mockPageResponse, response.getData());
        verify(adminUserService, times(1)).getUsers("thang", UserStatus.ACTIVE, true, 0, 20, "createdAt", "desc");
    }

    @Test
    @DisplayName("updateUserStatus gọi service đúng tham số và trả về ApiResponse chuẩn")
    void updateUserStatus_CallsServiceAndReturnsSuccessResponse() {
        UUID targetUserId = UUID.randomUUID();
        com.techhub.model.dto.request.UpdateUserStatusRequest request =
                new com.techhub.model.dto.request.UpdateUserStatusRequest(UserStatus.BANNED, "Vi phạm quy định");

        AdminUserResponse userResponse = new AdminUserResponse(
                targetUserId,
                "Thang",
                "Loi",
                "thang@techhub.com",
                "0987654321",
                true,
                UserStatus.BANNED,
                "123 Le Loi, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh",
                null,
                LocalDateTime.now()
        );

        when(adminUserService.updateUserStatus(targetUserId, request)).thenReturn(userResponse);

        ApiResponse<AdminUserResponse> response = adminUserController.updateUserStatus(targetUserId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User status updated successfully.", response.getMessage());
        assertEquals(userResponse, response.getData());
        verify(adminUserService, times(1)).updateUserStatus(targetUserId, request);
    }
}
