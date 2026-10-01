package com.techhub.service.seller.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.RegisterSellerRequest;
import com.techhub.model.dto.request.UpdateSellerProfileRequest;
import com.techhub.model.dto.request.UpdateSellerShopRequest;
import com.techhub.model.dto.request.UpdateShippingOptionRequest;
import com.techhub.model.dto.request.UpdateShopStatusRequest;
import com.techhub.model.dto.request.UpdateWarehouseAddressRequest;
import com.techhub.model.dto.response.SellerProfileResponse;
import com.techhub.model.dto.response.SellerRegistrationResponse;
import com.techhub.model.dto.response.SellerShippingOptionResponse;
import com.techhub.model.dto.response.SellerShopResponse;
import com.techhub.model.dto.response.SellerVerificationStatusResponse;
import com.techhub.model.dto.response.SellerWarehouseAddressResponse;
import com.techhub.model.entity.*;
import com.techhub.model.enums.SellerVerificationStatus;
import com.techhub.model.enums.ShopStatus;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.*;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.service.seller.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final ShopRepository shopRepository;
    private final AddressRepository addressRepository;
    private final ShippingOptionRepository shippingOptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SellerRegistrationResponse registerSeller(RegisterSellerRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Optional<Seller> existingSellerOpt = sellerRepository.findByUserId(user.getId());
        if (existingSellerOpt.isPresent()) {
            Seller existingSeller = existingSellerOpt.get();
            return switch (existingSeller.getVerificationStatus()) {
                case PENDING -> throw new BusinessException(ErrorCode.SELLER_ALREADY_PENDING);
                case APPROVED -> throw new BusinessException(ErrorCode.ALREADY_A_SELLER);
                case REJECTED -> handleResubmitFlow(existingSeller, user, request);
            };
        }

        // New application flow
        String sellerName = request.sellerName().trim();
        String shopName = request.shopName().trim();

        if (sellerRepository.existsBySellerName(sellerName)) {
            throw new BusinessException(ErrorCode.SELLER_NAME_ALREADY_EXISTS);
        }

        Seller newSeller = Seller.builder()
                .user(user)
                .sellerName(sellerName)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .rejectionReason(null)
                .bankName(request.bankName().trim())
                .bankAccountNumber(request.bankAccountNumber().trim())
                .bankAccountHolder(request.bankAccountHolder().trim().toUpperCase())
                .build();
        Seller savedSeller = sellerRepository.save(newSeller);

        Shop newShop = Shop.builder()
                .seller(savedSeller)
                .name(shopName)
                .description(request.shopDescription() != null ? request.shopDescription().trim() : null)
                .avatarUrl(request.avatarUrl() != null ? request.avatarUrl().trim() : null)
                .bannerUrl(request.bannerUrl() != null ? request.bannerUrl().trim() : null)
                .status(ShopStatus.SUSPENDED)
                .build();
        Shop savedShop = shopRepository.save(newShop);

        Address warehouseAddress = Address.builder()
                .shopId(savedShop.getId())
                .user(null)
                .label(null)
                .recipientName(request.warehouseContactName().trim())
                .phone(request.warehousePhone().trim())
                .province(request.province().trim())
                .district(request.district().trim())
                .ward(request.ward().trim())
                .streetAddress(request.streetAddress().trim())
                .isDefault(true)
                .build();
        Address savedAddress = addressRepository.save(warehouseAddress);

        ShippingOption shippingOption = ShippingOption.builder()
                .shop(savedShop)
                .name("Standard Delivery")
                .shippingFee(request.shippingFee())
                .isActive(true)
                .build();
        ShippingOption savedShippingOption = shippingOptionRepository.save(shippingOption);

        log.info("User {} successfully registered new seller application for shop '{}' (sellerName: '{}')",
                user.getId(), shopName, sellerName);

        return new SellerRegistrationResponse(
                savedSeller.getId(),
                savedShop.getId(),
                savedSeller.getSellerName(),
                savedShop.getName(),
                savedSeller.getVerificationStatus(),
                savedShop.getStatus(),
                savedSeller.getBankName(),
                savedSeller.getBankAccountNumber(),
                savedSeller.getBankAccountHolder(),
                formatAddress(savedAddress),
                savedShippingOption.getShippingFee(),
                savedSeller.getCreatedAt(),
                false
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerVerificationStatusResponse getVerificationStatus() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Optional<Seller> sellerOpt = sellerRepository.findByUserId(user.getId());
        if (sellerOpt.isEmpty()) {
            return SellerVerificationStatusResponse.notApplied();
        }

        Seller seller = sellerOpt.get();
        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        return new SellerVerificationStatusResponse(
                true,
                seller.getId(),
                shop.getId(),
                seller.getSellerName(),
                shop.getName(),
                seller.getVerificationStatus(),
                shop.getStatus(),
                seller.getRejectionReason(),
                seller.getCreatedAt(),
                seller.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        return new SellerProfileResponse(
                seller.getId(),
                seller.getSellerName(),
                seller.getVerificationStatus(),
                seller.getBankName(),
                seller.getBankAccountNumber(),
                seller.getBankAccountHolder(),
                seller.getCreatedAt(),
                seller.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SellerProfileResponse updateSellerProfile(UpdateSellerProfileRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        String newSellerName = request.sellerName().trim().toLowerCase();
        if (!seller.getSellerName().equalsIgnoreCase(newSellerName)) {
            if (sellerRepository.existsBySellerName(newSellerName)) {
                throw new BusinessException(ErrorCode.SELLER_NAME_ALREADY_EXISTS);
            }
            seller.setSellerName(newSellerName);
        }

        seller.setBankName(request.bankName().trim());
        seller.setBankAccountNumber(request.bankAccountNumber().trim());
        seller.setBankAccountHolder(request.bankAccountHolder().trim().toUpperCase());

        Seller savedSeller = sellerRepository.save(seller);

        log.info("User {} successfully updated seller profile (sellerId: {}, sellerName: '{}')",
                user.getId(), savedSeller.getId(), savedSeller.getSellerName());

        return new SellerProfileResponse(
                savedSeller.getId(),
                savedSeller.getSellerName(),
                savedSeller.getVerificationStatus(),
                savedSeller.getBankName(),
                savedSeller.getBankAccountNumber(),
                savedSeller.getBankAccountHolder(),
                savedSeller.getCreatedAt(),
                savedSeller.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerShopResponse getShopInfo() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        return new SellerShopResponse(
                shop.getId(),
                seller.getId(),
                seller.getSellerName(),
                shop.getName(),
                shop.getDescription(),
                shop.getAvatarUrl(),
                shop.getBannerUrl(),
                shop.getStatus(),
                shop.getCreatedAt(),
                shop.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SellerShopResponse updateShopInfo(UpdateSellerShopRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }

        shop.setName(request.name().trim());
        shop.setDescription(request.description() != null && !request.description().isBlank() ? request.description().trim() : null);
        shop.setAvatarUrl(request.avatarUrl() != null && !request.avatarUrl().isBlank() ? request.avatarUrl().trim() : null);
        shop.setBannerUrl(request.bannerUrl() != null && !request.bannerUrl().isBlank() ? request.bannerUrl().trim() : null);

        Shop savedShop = shopRepository.save(shop);

        log.info("User {} successfully updated shop details (shopId: {}, sellerId: {}, shopName: '{}')",
                user.getId(), savedShop.getId(), seller.getId(), savedShop.getName());

        return new SellerShopResponse(
                savedShop.getId(),
                seller.getId(),
                seller.getSellerName(),
                savedShop.getName(),
                savedShop.getDescription(),
                savedShop.getAvatarUrl(),
                savedShop.getBannerUrl(),
                savedShop.getStatus(),
                savedShop.getCreatedAt(),
                savedShop.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SellerShopResponse updateShopStatus(UpdateShopStatusRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (request.status() != ShopStatus.ACTIVE && request.status() != ShopStatus.PAUSED) {
            throw new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION);
        }

        if (shop.getStatus() != ShopStatus.ACTIVE && shop.getStatus() != ShopStatus.PAUSED) {
            throw new BusinessException(ErrorCode.INVALID_SHOP_STATUS_TRANSITION);
        }

        if (shop.getStatus() == request.status()) {
            return new SellerShopResponse(
                    shop.getId(),
                    seller.getId(),
                    seller.getSellerName(),
                    shop.getName(),
                    shop.getDescription(),
                    shop.getAvatarUrl(),
                    shop.getBannerUrl(),
                    shop.getStatus(),
                    shop.getCreatedAt(),
                    shop.getUpdatedAt()
            );
        }

        shop.setStatus(request.status());
        Shop savedShop = shopRepository.save(shop);

        log.info("User {} successfully updated shop status (shopId: {}, sellerId: {}, status: '{}')",
                user.getId(), savedShop.getId(), seller.getId(), savedShop.getStatus());

        return new SellerShopResponse(
                savedShop.getId(),
                seller.getId(),
                seller.getSellerName(),
                savedShop.getName(),
                savedShop.getDescription(),
                savedShop.getAvatarUrl(),
                savedShop.getBannerUrl(),
                savedShop.getStatus(),
                savedShop.getCreatedAt(),
                savedShop.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerWarehouseAddressResponse getWarehouseAddress() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        Address address = addressRepository.findByShopId(shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        return new SellerWarehouseAddressResponse(
                address.getId(),
                shop.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getStreetAddress(),
                formatAddress(address),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SellerWarehouseAddressResponse updateWarehouseAddress(UpdateWarehouseAddressRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }

        Address address = addressRepository.findByShopId(shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        address.setRecipientName(request.contactName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setStreetAddress(request.streetAddress().trim());

        Address savedAddress = addressRepository.save(address);

        log.info("User {} successfully updated warehouse address for shop {} (addressId: {})",
                user.getId(), shop.getId(), savedAddress.getId());

        return new SellerWarehouseAddressResponse(
                savedAddress.getId(),
                shop.getId(),
                savedAddress.getRecipientName(),
                savedAddress.getPhone(),
                savedAddress.getProvince(),
                savedAddress.getDistrict(),
                savedAddress.getWard(),
                savedAddress.getStreetAddress(),
                formatAddress(savedAddress),
                savedAddress.getCreatedAt(),
                savedAddress.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SellerShippingOptionResponse getShippingOption() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        ShippingOption shippingOption = shippingOptionRepository.findByShopId(shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_OPTION_NOT_FOUND));

        return new SellerShippingOptionResponse(
                shippingOption.getId(),
                shop.getId(),
                shippingOption.getName(),
                shippingOption.getShippingFee(),
                shippingOption.isActive(),
                shippingOption.getCreatedAt(),
                shippingOption.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SellerShippingOptionResponse updateShippingOption(UpdateShippingOptionRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getVerificationStatus() != SellerVerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.NOT_A_SELLER);
        }

        Shop shop = shopRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));

        if (shop.getStatus() == ShopStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }

        ShippingOption shippingOption = shippingOptionRepository.findByShopId(shop.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_OPTION_NOT_FOUND));

        shippingOption.setShippingFee(request.shippingFee());
        ShippingOption savedShippingOption = shippingOptionRepository.save(shippingOption);

        log.info("User {} successfully updated shipping fee for shop {} (shippingFee: {})",
                user.getId(), shop.getId(), savedShippingOption.getShippingFee());

        return new SellerShippingOptionResponse(
                savedShippingOption.getId(),
                shop.getId(),
                savedShippingOption.getName(),
                savedShippingOption.getShippingFee(),
                savedShippingOption.isActive(),
                savedShippingOption.getCreatedAt(),
                savedShippingOption.getUpdatedAt()
        );
    }

    private SellerRegistrationResponse handleResubmitFlow(Seller existingSeller, User user, RegisterSellerRequest request) {
        String sellerName = request.sellerName().trim();
        String shopName = request.shopName().trim();

        if (!existingSeller.getSellerName().equalsIgnoreCase(sellerName)) {
            if (sellerRepository.existsBySellerName(sellerName)) {
                throw new BusinessException(ErrorCode.SELLER_NAME_ALREADY_EXISTS);
            }
            existingSeller.setSellerName(sellerName);
        }

        existingSeller.setBankName(request.bankName().trim());
        existingSeller.setBankAccountNumber(request.bankAccountNumber().trim());
        existingSeller.setBankAccountHolder(request.bankAccountHolder().trim().toUpperCase());
        existingSeller.setVerificationStatus(SellerVerificationStatus.PENDING);
        existingSeller.setRejectionReason(null);
        Seller savedSeller = sellerRepository.save(existingSeller);

        Shop shop = shopRepository.findBySellerId(savedSeller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_NOT_FOUND));
        shop.setName(shopName);
        shop.setDescription(request.shopDescription() != null ? request.shopDescription().trim() : null);
        shop.setAvatarUrl(request.avatarUrl() != null ? request.avatarUrl().trim() : null);
        shop.setBannerUrl(request.bannerUrl() != null ? request.bannerUrl().trim() : null);
        shop.setStatus(ShopStatus.SUSPENDED);
        Shop savedShop = shopRepository.save(shop);

        Address warehouseAddress = addressRepository.findByShopId(savedShop.getId())
                .orElseGet(() -> Address.builder()
                        .shopId(savedShop.getId())
                        .user(null)
                        .label(null)
                        .isDefault(true)
                        .build());
        warehouseAddress.setRecipientName(request.warehouseContactName().trim());
        warehouseAddress.setPhone(request.warehousePhone().trim());
        warehouseAddress.setProvince(request.province().trim());
        warehouseAddress.setDistrict(request.district().trim());
        warehouseAddress.setWard(request.ward().trim());
        warehouseAddress.setStreetAddress(request.streetAddress().trim());
        warehouseAddress.setDefault(true);
        Address savedAddress = addressRepository.save(warehouseAddress);

        ShippingOption shippingOption = shippingOptionRepository.findByShopId(savedShop.getId())
                .orElseGet(() -> ShippingOption.builder()
                        .shop(savedShop)
                        .name("Standard Delivery")
                        .build());
        shippingOption.setShippingFee(request.shippingFee());
        shippingOption.setActive(true);
        ShippingOption savedShippingOption = shippingOptionRepository.save(shippingOption);

        log.info("User {} re-submitted seller application for shop '{}' (sellerName: '{}')",
                user.getId(), shopName, sellerName);

        return new SellerRegistrationResponse(
                savedSeller.getId(),
                savedShop.getId(),
                savedSeller.getSellerName(),
                savedShop.getName(),
                savedSeller.getVerificationStatus(),
                savedShop.getStatus(),
                savedSeller.getBankName(),
                savedSeller.getBankAccountNumber(),
                savedSeller.getBankAccountHolder(),
                formatAddress(savedAddress),
                savedShippingOption.getShippingFee(),
                savedSeller.getCreatedAt(),
                true
        );
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }
    }

    private void validateUserActiveAndNonLocked(User user) {
        validateUserStatus(user);
        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (!user.isAccountNonLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    private String formatAddress(Address addr) {
        if (addr == null) {
            return null;
        }
        String street = addr.getStreetAddress() != null ? addr.getStreetAddress() : "";
        String ward = addr.getWard() != null ? addr.getWard() : "";
        String district = addr.getDistrict() != null ? addr.getDistrict() : "";
        String province = addr.getProvince() != null ? addr.getProvince() : "";
        return String.format("%s, %s, %s, %s", street, ward, district, province);
    }
}
