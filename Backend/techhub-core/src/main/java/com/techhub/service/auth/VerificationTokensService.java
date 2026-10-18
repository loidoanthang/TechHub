package com.techhub.service.auth;

import com.techhub.model.entity.User;
import com.techhub.model.entity.VerificationTokens;
import com.techhub.model.enums.VerificationTokenType;

public interface VerificationTokensService {

    VerificationTokens create(User user, VerificationTokenType type);

    VerificationTokens getByUserAndType(User user, VerificationTokenType type);

    VerificationTokens validate(User user, String tokenValue, VerificationTokenType type);

    void delete(VerificationTokens token);
}
