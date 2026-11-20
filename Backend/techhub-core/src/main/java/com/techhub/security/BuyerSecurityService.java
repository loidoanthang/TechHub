package com.techhub.security;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.user.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component("buyerSecurity")
public class BuyerSecurityService {

    public boolean isActiveBuyer() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.user() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        UserStatus status = currentUser.user().getStatus();
        if (status == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (status == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
        if (status == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.BUYER_ACCOUNT_SUSPENDED);
        }
        if (status != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        return true;
    }
}
