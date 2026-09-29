package com.techhub.service;

import com.techhub.config.properties.JwtProperties;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.impl.JwtServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private static final String SECRET = "NjQzYmY4MjQ4YmQ5ZjM4ZjJkZjM3N2Q3OTI0ZmI0NzI4Y2RkZmE5OTMwN2Q5OGE2MGM2M2ZmY2JhMzY5Y2Y2ZA==";
    private User sampleUser;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getSecret()).thenReturn(SECRET);

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("multi@techhub.com");
        sampleUser.setEmailVerified(true);
        sampleUser.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
    }

    @Test
    @DisplayName("Tạo Access Token với nhiều roles - Claim roles chứa đầy đủ danh sách role")
    void generateAccessToken_ContainsMultipleRoles() {
        when(jwtProperties.getAccessExpiration()).thenReturn(900000L);

        String token = jwtService.generateAccessToken(sampleUser);
        assertNotNull(token);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("multi@techhub.com", claims.getSubject());
        
        @SuppressWarnings("unchecked")
        List<String> rolesClaim = claims.get("roles", List.class);
        assertNotNull(rolesClaim);
        assertEquals(2, rolesClaim.size());
        assertTrue(rolesClaim.contains("BUYER"));
        assertTrue(rolesClaim.contains("SELLER"));
    }

    @Test
    @DisplayName("Kiểm tra CustomUserDetails mapping đầy đủ GrantedAuthorities từ User roles")
    void customUserDetails_MapsAuthorities() {
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        var authorities = userDetails.getAuthorities();

        assertEquals(2, authorities.size());
        var authNames = authorities.stream().map(a -> a.getAuthority()).toList();
        assertTrue(authNames.contains("ROLE_BUYER"));
        assertTrue(authNames.contains("ROLE_SELLER"));
    }
}
