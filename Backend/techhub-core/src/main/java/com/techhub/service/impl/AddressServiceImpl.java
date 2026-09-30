package com.techhub.service.impl;

import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.CreateAddressRequest;
import com.techhub.model.dto.request.UpdateAddressRequest;
import com.techhub.model.dto.response.AddressResponse;
import com.techhub.model.entity.Address;
import com.techhub.model.entity.User;
import com.techhub.model.enums.AddressLabel;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.AddressRepository;
import com.techhub.repository.UserRepository;
import com.techhub.security.CustomUserDetails;
import com.techhub.security.SecurityUtils;
import com.techhub.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        long currentCount = addressRepository.countByUserIdAndShopIdIsNull(user.getId());
        if (currentCount >= 10) {
            throw new BusinessException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        boolean willBeDefault;
        if (currentCount == 0) {
            willBeDefault = true;
        } else if (Boolean.TRUE.equals(request.isDefault())) {
            addressRepository.resetDefaultAddressesByUserId(user.getId());
            willBeDefault = true;
        } else {
            willBeDefault = false;
        }

        AddressLabel label = (request.label() == null) ? AddressLabel.HOME : request.label();

        Address address = Address.builder()
                .user(user)
                .shopId(null)
                .label(label)
                .recipientName(request.recipientName().trim())
                .phone(request.phone().trim())
                .province(request.province().trim())
                .district(request.district().trim())
                .ward(request.ward().trim())
                .streetAddress(request.streetAddress().trim())
                .isDefault(willBeDefault)
                .build();

        Address savedAddress = addressRepository.save(address);

        return AddressResponse.from(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        List<Address> addresses = addressRepository
                .findAllByUserIdAndShopIdIsNullOrderByIsDefaultDescCreatedAtDesc(user.getId());

        return addresses.stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Address address = addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (address.isDefault()) {
            if (Boolean.FALSE.equals(request.isDefault())) {
                throw new BusinessException(ErrorCode.CANNOT_UNSET_DEFAULT_ADDRESS);
            }
        } else {
            if (Boolean.TRUE.equals(request.isDefault())) {
                addressRepository.resetDefaultAddressesByUserId(user.getId());
                address.setDefault(true);
            } else {
                address.setDefault(false);
            }
        }

        address.setLabel((request.label() == null) ? AddressLabel.HOME : request.label());
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setStreetAddress(request.streetAddress().trim());

        Address updatedAddress = addressRepository.save(address);

        return AddressResponse.from(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID addressId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Address address = addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (address.isDefault()) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS);
        }

        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID addressId) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.user().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserActiveAndNonLocked(user);

        Address address = addressRepository.findByIdAndUserIdAndShopIdIsNull(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.isDefault()) {
            addressRepository.resetDefaultAddressesByUserId(user.getId());
            address.setDefault(true);
            address = addressRepository.save(address);
        }

        return AddressResponse.from(address);
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
}
