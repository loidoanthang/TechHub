package com.techhub.service.auth;

import com.techhub.modules.auth.entity.RefreshToken;
import com.techhub.modules.user.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken getByToken(String token);

    void revoke(RefreshToken token);

    RefreshToken validate(String token);

    void revokeAll(User user);

    void revokeByToken(String token);
}
