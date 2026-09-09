package com.techhub.service;

import com.techhub.model.entity.RefreshToken;
import com.techhub.model.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken getByToken(String token);

    void revoke(RefreshToken token);

    RefreshToken validate(String token);

    void revokeAll(User user);

    void revokeByToken(String token);
}
