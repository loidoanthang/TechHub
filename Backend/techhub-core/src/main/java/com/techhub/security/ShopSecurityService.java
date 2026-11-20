package com.techhub.security;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("shopSecurity")
@RequiredArgsConstructor
public class ShopSecurityService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;

    public boolean isActiveShop() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.user() == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        // 1. Kiểm tra User status: Nếu User chủ shop bị BANNED hoặc DELETED -> Chặn tuyệt đối
        UserStatus userStatus = currentUser.user().getStatus();
        if (userStatus == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (userStatus == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        // 2. Kiểm tra tư cách Seller và Shop
        UUID currentUserId = currentUser.user().getId();
        Seller seller = sellerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_SELLER));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SELLER_NOT_VERIFIED);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED || shop.getStatus() == ShopStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_ACTIVE);
        }
        return true;
    }
}
