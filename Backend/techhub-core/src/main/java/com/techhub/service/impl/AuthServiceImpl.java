package com.techhub.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.*;
import com.techhub.model.dto.response.LoginResponse;
import com.techhub.model.dto.response.TokenResponse;
import com.techhub.model.entity.EmailVerificationToken;
import com.techhub.model.entity.PasswordResetToken;
import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;


    @Transactional
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_IS_EXISTED);
        }
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.BUYER);
        user.setEnabled(false);
        userRepository.save(user);
        EmailVerificationToken token = verificationTokenService.create(user);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    @Override
    public void verifyEmail(String email, String tokenValue) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isEnabled()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        EmailVerificationToken token = verificationTokenService.validate(user, tokenValue);
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenService.delete(token);
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
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException ex) {
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
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken oldRefreshToken = refreshTokenService.validate(request.refreshToken());
        User user = oldRefreshToken.getUser();

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (!user.isAccountNonLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        refreshTokenService.revoke(oldRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.create(user);

        return new TokenResponse(newAccessToken, newRefreshToken.getToken());
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revokeByToken(request.refreshToken());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        PasswordResetToken token = passwordResetTokenService.create(user);
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
        PasswordResetToken token = passwordResetTokenService.validate(user, request.token());
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockoutEndTime(null);
        userRepository.save(user);
        refreshTokenService.revokeAll(user);
        passwordResetTokenService.delete(token);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

        if (user.isEnabled()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        EmailVerificationToken token = verificationTokenService.create(user);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }
}
