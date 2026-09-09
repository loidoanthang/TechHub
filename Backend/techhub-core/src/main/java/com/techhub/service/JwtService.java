package com.techhub.service;

import com.techhub.model.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateAccessToken(User user);

    String extractEmail(String token);

    boolean isTokenExpired(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

}
