package com.techhub.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.UpdateProfileRequest;
import com.techhub.model.dto.response.UserProfileResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone().trim());

        if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);

        return UserProfileResponse.from(updatedUser);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
    }

    private void validateUserActiveAndNonLocked(User user) {
        validateUserStatus(user);
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (!user.isAccountNonLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }
}
