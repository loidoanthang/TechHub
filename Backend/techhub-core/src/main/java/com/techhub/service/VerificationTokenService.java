package com.techhub.service;

import com.techhub.model.entity.EmailVerificationToken;
import com.techhub.model.entity.User;

public interface VerificationTokenService {

    EmailVerificationToken create(User user);

    EmailVerificationToken getByUser(User user);

    EmailVerificationToken validate(User user, String token);

    void delete(EmailVerificationToken token);
}