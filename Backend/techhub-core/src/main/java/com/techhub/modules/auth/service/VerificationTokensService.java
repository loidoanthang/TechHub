package com.techhub.modules.auth.service;

import com.techhub.modules.user.entity.User;
import com.techhub.modules.auth.entity.VerificationTokens;
import com.techhub.modules.auth.enums.VerificationTokenType;

public interface VerificationTokensService {

    VerificationTokens create(User user, VerificationTokenType type);

    VerificationTokens getByUserAndType(User user, VerificationTokenType type);

    VerificationTokens validate(User user, String tokenValue, VerificationTokenType type);

    void delete(VerificationTokens token);
}
