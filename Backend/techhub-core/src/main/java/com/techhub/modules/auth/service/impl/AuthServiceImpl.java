package com.techhub.modules.auth.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.auth.dto.response.LoginResponse;
import com.techhub.modules.auth.dto.response.TokenResponse;
import com.techhub.modules.auth.entity.RefreshToken;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.auth.entity.VerificationTokens;
import com.techhub.modules.user.enums.Role;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.auth.enums.VerificationTokenType;
import com.techhub.modules.user.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.modules.auth.service.AuthService;
import com.techhub.modules.auth.service.EmailService;
import com.techhub.modules.auth.service.JwtService;
import com.techhub.modules.auth.service.RefreshTokenService;
import com.techhub.modules.auth.service.VerificationTokensService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.techhub.modules.auth.dto.request.ChangePasswordRequest;
import com.techhub.modules.auth.dto.request.DeleteAccountRequest;
import com.techhub.modules.auth.dto.request.ForgotPasswordRequest;
import com.techhub.modules.auth.dto.request.LoginRequest;
import com.techhub.modules.auth.dto.request.LogoutRequest;
import com.techhub.modules.auth.dto.request.RefreshTokenRequest;
import com.techhub.modules.auth.dto.request.RegisterRequest;
import com.techhub.modules.auth.dto.request.ResendVerificationRequest;
import com.techhub.modules.auth.dto.request.ResetPasswordRequest;
import com.techhub.modules.auth.dto.request.VerifyEmailRequest;
import com.techhub.modules.order.enums.OrderStatus;
import com.techhub.modules.order.repository.OrderRepository;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.shop.repository.ShopRepository;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Set<OrderStatus> TERMINAL_ORDER_STATUSES = Set.of(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED,
            OrderStatus.REFUNDED
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokensService verificationTokensService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;

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
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(DeleteAccountRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        validateUserActiveAndNonLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }

        // 1. Condition 1: Check active buyer orders in progress
        if (orderRepository.existsActiveOrdersByUserId(user.getId(), TERMINAL_ORDER_STATUSES)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_ACCOUNT_ACTIVE_BUYER_ORDERS);
        }

        // 2 & 3: Check Seller conditions if user has a registered seller profile
        Optional<Seller> sellerOpt = sellerRepository.findWithUserForUpdateByUserId(user.getId());
        if (sellerOpt.isPresent()) {
            Seller seller = sellerOpt.get();

            // Condition 3: Check remaining seller wallet balance
            if (seller.getBalance() != null && seller.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(ErrorCode.CANNOT_DELETE_ACCOUNT_WALLET_HAS_BALANCE);
            }

            // Find linked shop
            Optional<Shop> shopOpt = shopRepository.findBySellerId(seller.getId());
            if (shopOpt.isPresent()) {
                Shop shop = shopOpt.get();

                // Condition 2: Check active seller orders in progress or pending returns
                if (orderRepository.existsActiveOrdersByShopId(shop.getId(), TERMINAL_ORDER_STATUSES)) {
                    throw new BusinessException(ErrorCode.CANNOT_DELETE_ACCOUNT_ACTIVE_SELLER_ORDERS);
                }

                // Cascade Shop status to BANNED
                shop.setStatus(ShopStatus.BANNED);
                shopRepository.save(shop);
            }
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
