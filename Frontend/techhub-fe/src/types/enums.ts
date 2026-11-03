// Role
export type Role = "BUYER" | "SELLER" | "ADMIN";

// User & Shop Status
export type UserStatus = "ACTIVE" | "SUSPENDED" | "BANNED" | "DELETED";
export type ShopStatus = "ACTIVE" | "PAUSED" | "SUSPENDED" | "BANNED";
export type SellerVerificationStatus = "PENDING" | "APPROVED" | "REJECTED";

// Product Status
export type ProductApprovalStatus = "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED";
export type ProductVisibilityStatus = "ACTIVE" | "INACTIVE";
export type ProductVariantStatus = "ACTIVE" | "INACTIVE";
export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type CategoryAttributeDataType = "TEXT" | "NUMBER" | "SELECT";

// Address Label
export type AddressLabel = "HOME" | "OFFICE" | "OTHER";

// Order & Payment
export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PROCESSING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "RETURNED";

export type PaymentMethod = "COD" | "VNPAY";
export type PaymentStatus = "PENDING" | "COMPLETED" | "FAILED" | "EXPIRED" | "REFUNDED";

// Cancellation & Return
export type CancellationReasonCode =
  | "CHANGE_SHIPPING_ADDRESS"
  | "CHANGE_PAYMENT_METHOD"
  | "DON_MUON_MUA_NUA"
  | "THAY_DOI_Y_DINH"
  | "TIM_THAY_GIA_TOT_HON"
  | "KHAC";

export type CancellationRequestStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ReturnRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

// Voucher
export type VoucherType = "PLATFORM" | "SHOP";
export type VoucherStatus = "ACTIVE" | "INACTIVE" | "EXPIRED" | "EXHAUSTED";
export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type VoucherAppliesTo = "PRODUCT" | "SHIPPING_FEE" | "ALL_PRODUCTS" | "SPECIFIC_PRODUCTS" | "SHIPPING";
