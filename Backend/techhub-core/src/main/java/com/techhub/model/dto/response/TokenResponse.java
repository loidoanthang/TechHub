package com.techhub.model.dto.response;

public record TokenResponse(

        String accessToken,
        String refreshToken

) {
}
