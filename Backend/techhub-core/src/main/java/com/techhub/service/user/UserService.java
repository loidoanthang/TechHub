package com.techhub.service.user;

import com.techhub.modules.user.dto.request.UpdateProfileRequest;
import com.techhub.modules.user.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);
}
