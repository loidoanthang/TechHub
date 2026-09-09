package com.techhub.service;

import com.techhub.model.entity.PasswordResetToken;
import com.techhub.model.entity.User;

public interface PasswordResetTokenService {

    PasswordResetToken create(User user);

    PasswordResetToken validate(User user, String token);

    void delete(PasswordResetToken token);

}