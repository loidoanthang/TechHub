package com.techhub.service.shop.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.response.PublicShopResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.Seller;
import com.techhub.model.entity.Shop;
import com.techhub.model.entity.User;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.SellerRepository;
import com.techhub.repository.ShopRepository;
import com.techhub.service.shop.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
