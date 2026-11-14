package com.techhub.modules.shop.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.modules.shop.dto.response.PublicShopResponse;
import com.techhub.modules.user.entity.Address;
import com.techhub.modules.seller.entity.Seller;
import com.techhub.modules.shop.entity.Shop;
import com.techhub.modules.user.entity.User;
import com.techhub.modules.seller.enums.SellerVerificationStatus;
import com.techhub.modules.shop.enums.ShopStatus;
import com.techhub.modules.user.enums.UserStatus;
import com.techhub.modules.user.repository.AddressRepository;
import com.techhub.modules.seller.repository.SellerRepository;
import com.techhub.modules.shop.repository.ShopRepository;
import com.techhub.modules.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import com.techhub.modules.shop.service.ShopService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicShopResponse getPublicShopProfile(String sellerName) {
        if (sellerName == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        String normalizedSlug = sellerName.trim().toLowerCase();
        if (normalizedSlug.length() < 3 || normalizedSlug.length() > 50 || !normalizedSlug.matches("^[a-z0-9-]+$")) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Seller seller = sellerRepository.findBySellerName(normalizedSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        User user = seller.getUser();
        if (user != null && (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.DELETED)) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.SUSPENDED || shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }

        Optional<Address> addressOpt = addressRepository.findByShopId(shop.getId());
        String warehouseProvince = addressOpt.map(Address::getProvince).orElse(null);
        String warehouseDistrict = addressOpt.map(Address::getDistrict).orElse(null);

        log.info("Public shop profile retrieved successfully for slug '{}' (shopId: {}, status: {})",
                normalizedSlug, shop.getId(), shop.getStatus());

        return new PublicShopResponse(
                shop.getId(),
                seller.getSellerName(),
                shop.getName(),
                shop.getDescription(),
                shop.getAvatarUrl(),
                shop.getBannerUrl(),
                shop.getStatus(),
                warehouseProvince,
                warehouseDistrict,
                shop.getCreatedAt()
        );
    }
}
