import { SellerVerificationStatus, ShopStatus } from "./enums";

export interface RegisterSellerRequest {
  sellerName: string;
  shopName: string;
  shopDescription?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  warehouseContactName: string;
  warehousePhone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  shippingFee: number;
}

export interface SellerRegistrationResponse {
  sellerId: string;
  shopId: string;
  sellerName: string;
  shopName: string;
  status: SellerVerificationStatus;
  submittedAt: string;
  isResubmitted: boolean;
}

export interface SellerVerificationStatusResponse {
  hasApplied: boolean;
  sellerId?: string | null;
  shopId?: string | null;
  sellerName?: string | null;
  shopName?: string | null;
  verificationStatus?: SellerVerificationStatus | null;
  shopStatus?: ShopStatus | null;
  rejectionReason?: string | null;
  submittedAt?: string | null;
  updatedAt?: string | null;
}

export interface SellerShopResponse {
  shopId: string;
  sellerId?: string;
  sellerName: string;
  name: string;
  shopName?: string; // fallback alias
  description?: string | null;
  avatarUrl?: string | null;
  bannerUrl?: string | null;
  status: ShopStatus;
  totalProducts?: number;
  rating?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateSellerShopRequest {
  name: string;
  description?: string;
  avatarUrl?: string;
  bannerUrl?: string;
}

export interface SellerWarehouseAddressResponse {
  id: string;
  shopId: string;
  contactName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  fullAddress?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateWarehouseAddressRequest {
  contactName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
}

export interface SellerShippingOptionResponse {
  id: string;
  shopId: string;
  name: string;
  shippingFee: number;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateShippingOptionRequest {
  shippingFee: number;
}

export interface SellerProfileResponse {
  sellerId: string;
  sellerName: string;
  verificationStatus: SellerVerificationStatus;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateSellerProfileRequest {
  sellerName: string;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
}
