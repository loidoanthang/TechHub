package com.techhub.modules.auth.dto.response;

public record LoginResponse(

        String accessToken,

        String refreshToken

) {
}
