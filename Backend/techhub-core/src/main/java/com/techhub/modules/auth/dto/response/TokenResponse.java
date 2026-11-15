package com.techhub.modules.auth.dto.response;

public record TokenResponse(

        String accessToken,
        String refreshToken

) {
}
