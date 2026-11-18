import type { DiscountType, VoucherAppliesTo, VoucherStatus } from "./enums";

export type VoucherType = "SHOP" | "SYSTEM";
export type { DiscountType, VoucherAppliesTo, VoucherStatus };

export interface ShopVoucherResponse {
  id: string;
  shopId: string;
  code: string;
  name: string;
  type: VoucherType;
  appliesTo: VoucherAppliesTo;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount?: number | null;
  usageLimit?: number | null;
  usageLimitPerUser: number;
  reservedCount: number;
  usedCount: number;
  startDate: string;
  endDate: string;
  isActive: boolean;
  status: VoucherStatus;
  createdAt: string;
}

export interface ShopVoucherDetailResponse extends ShopVoucherResponse {
  remainingCount?: number | null;
  redemptionRate?: number | null;
  updatedAt?: string;
}

export interface CreateShopVoucherRequest {
  code: string;
  name: string;
  appliesTo: VoucherAppliesTo;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number | null;
  usageLimit?: number | null;
  usageLimitPerUser: number;
  startDate: string;
  endDate: string;
}

export interface UpdateShopVoucherRequest {
  name: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number | null;
  usageLimit?: number | null;
  usageLimitPerUser: number;
  startDate?: string;
  endDate: string;
}

export interface UpdateShopVoucherStatusRequest {
  isActive: boolean;
}

export interface SellerVoucherQueryParams {
  keyword?: string;
  appliesTo?: VoucherAppliesTo;
  discountType?: DiscountType;
  status?: VoucherStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
