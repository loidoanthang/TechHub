package com.techhub.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.*;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.dto.response.TokenResponse;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.model.enums.VerificationTokenType;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokensService verificationTokensService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    @Override
    public void register(RegisterRequest request) {
        User existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null) {
            if (existingUser.isEmailVerified()) {
                throw new BusinessException(ErrorCode.EMAIL_IS_EXISTED);
            } else {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS_UNVERIFIED);
            }
        }
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        user.setEmailVerified(false);
        userRepository.save(user);
        VerificationTokens token = verificationTokensService.create(user, VerificationTokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateUserStatus(user);
        if (user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        VerificationTokens token = verificationTokensService.validate(user, request.token(), VerificationTokenType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokensService.delete(token);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User authenticatedUser = userDetails.user();

            if (authenticatedUser.getFailedLoginAttempts() > 0 || authenticatedUser.getLockoutEndTime() != null) {
                authenticatedUser.setFailedLoginAttempts(0);
                authenticatedUser.setLockoutEndTime(null);
                userRepository.save(authenticatedUser);
            }

            String accessToken = jwtService.generateAccessToken(authenticatedUser);
            RefreshToken refreshToken = refreshTokenService.create(authenticatedUser);
            return new LoginResponse(accessToken, refreshToken.getToken());
        } catch (LockedException ex) {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            if (user != null) {
                if (user.getStatus() == UserStatus.BANNED) {
                    throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
                }
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
                }
            }
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException ex) {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            if (user != null) {
                if (user.getStatus() == UserStatus.DELETED) {
                    throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
                }
                if (!user.isEmailVerified()) {
                    throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
                }
            }
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        } catch (BadCredentialsException ex) {
            User user = userRepository.findByEmail(request.email()).orElse(null);
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) {
                    user.setLockoutEndTime(LocalDateTime.now().plusMinutes(15));
                    user.setFailedLoginAttempts(0);
                    userRepository.save(user);
                    throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
                }
                userRepository.save(user);
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken().trim();
        RefreshToken oldRefreshToken = refreshTokenService.validate(token);
        User user = oldRefreshToken.getUser();

        validateUserActiveAndNonLocked(user);

        refreshTokenService.revoke(oldRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.create(user);

        return new TokenResponse(newAccessToken, newRefreshToken.getToken());
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String token = request.refreshToken().trim();
        refreshTokenService.revokeByToken(token);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserStatus(user);

        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        VerificationTokens token = verificationTokensService.create(user, VerificationTokenType.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(
                user.getEmail(),
                token.getToken()
        );
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserStatus(user);

        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        VerificationTokens token = verificationTokensService.validate(user, request.token(), VerificationTokenType.PASSWORD_RESET);
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockoutEndTime(null);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
        verificationTokensService.delete(token);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserStatus(user);
        if (user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        VerificationTokens token = verificationTokensService.create(user, VerificationTokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenService.revokeAll(user);
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
