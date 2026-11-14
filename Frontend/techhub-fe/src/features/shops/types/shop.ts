import type { DiscountType, VoucherAppliesTo, VoucherType } from "@/types/enums";

export type ShopStatus = "ACTIVE" | "PAUSED" | "INACTIVE";

export interface PublicShopResponse {
  id: string;
  sellerName: string;
  name: string;
  description: string | null;
  avatarUrl: string | null;
  bannerUrl: string | null;
  status: ShopStatus;
  warehouseProvince: string | null;
  warehouseDistrict: string | null;
  createdAt: string;
  // Seller's User id for chat.
  sellerUserId?: string | null;
}

export interface CartShopVoucherResponse {
  id: string;
  shopId: string;
  code: string;
  name: string;
  type: VoucherType;
  appliesTo: VoucherAppliesTo;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount: number | null;
  startDate: string;
  endDate: string;
  isEligible?: boolean;
  estimatedDiscount?: number;
  upsellRemainingAmount?: number | null;
  ineligibilityReason?: string | null;
}
