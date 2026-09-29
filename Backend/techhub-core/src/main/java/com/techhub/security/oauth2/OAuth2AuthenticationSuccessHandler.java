package com.techhub.security.oauth2;

import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.UserRepository;
import com.techhub.service.JwtService;
import com.techhub.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");

        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(email, givenName, familyName, name));

        if (user.getStatus() == UserStatus.BANNED) {
            sendErrorRedirect(request, response, "ACCOUNT_BANNED");
            return;
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            sendErrorRedirect(request, response, "ACCOUNT_SUSPENDED");
            return;
        }
        if (user.getStatus() == UserStatus.DELETED) {
            sendErrorRedirect(request, response, "ACCOUNT_DELETED");
            return;
        }
        if (!user.isAccountNonLocked()) {
            sendErrorRedirect(request, response, "ACCOUNT_LOCKED");
            return;
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User createGoogleUser(String email, String givenName, String familyName, String name) {
        try {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(givenName != null ? givenName : (name != null ? name : ""));
            newUser.setLastName(familyName != null ? familyName : "");
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setEmailVerified(true);
            newUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent Google registration detected for email: {}", email);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User could not be found after duplicate key violation: " + email));
        }
    }

    private void sendErrorRedirect(HttpServletRequest request, HttpServletResponse response, String errorCode) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
