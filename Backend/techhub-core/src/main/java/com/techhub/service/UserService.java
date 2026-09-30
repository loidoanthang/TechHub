package com.techhub.service;

import com.techhub.model.dto.request.UpdateProfileRequest;
import com.techhub.model.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);
}
