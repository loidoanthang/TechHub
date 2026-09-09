package com.techhub.model.dto.response;

public record LoginResponse(

        String accessToken,

        String refreshToken

) {
}
