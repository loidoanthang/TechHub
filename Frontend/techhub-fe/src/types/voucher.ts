import type { DiscountType, VoucherAppliesTo, VoucherType } from "./enums";

export interface CartPlatformVoucherResponse {
  id: string; // UUID
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
  isEligible: boolean;
  estimatedDiscount: number;
  upsellRemainingAmount: number | null;
  ineligibilityReason: string | null;
}

export interface PlatformVoucherQueryParams {
  orderAmount?: number;
  shippingFee?: number;
  appliesTo?: VoucherAppliesTo;
}
