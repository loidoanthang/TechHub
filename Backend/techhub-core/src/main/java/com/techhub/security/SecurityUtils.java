package com.techhub.security;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }
}
