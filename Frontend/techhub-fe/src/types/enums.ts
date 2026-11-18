// Role
export const ROLES = ["BUYER", "SELLER", "ADMIN"] as const;
export type Role = (typeof ROLES)[number];

// User & Shop Status
export const USER_STATUSES = ["ACTIVE", "SUSPENDED", "BANNED", "DELETED"] as const;
export type UserStatus = (typeof USER_STATUSES)[number];

export const SHOP_STATUSES = ["ACTIVE", "PAUSED", "SUSPENDED", "BANNED"] as const;
export type ShopStatus = (typeof SHOP_STATUSES)[number];

export const SELLER_VERIFICATION_STATUSES = ["PENDING", "APPROVED", "REJECTED"] as const;
export type SellerVerificationStatus = (typeof SELLER_VERIFICATION_STATUSES)[number];

// Product Status
export const PRODUCT_APPROVAL_STATUSES = ["DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED"] as const;
export type ProductApprovalStatus = (typeof PRODUCT_APPROVAL_STATUSES)[number];

export const PRODUCT_VISIBILITY_STATUSES = ["PUBLISHED", "UNPUBLISHED"] as const;
export type ProductVisibilityStatus = (typeof PRODUCT_VISIBILITY_STATUSES)[number];

export const PRODUCT_VARIANT_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export type ProductVariantStatus = (typeof PRODUCT_VARIANT_STATUSES)[number];

export const CATEGORY_STATUSES = ["ACTIVE", "INACTIVE"] as const;
export type CategoryStatus = (typeof CATEGORY_STATUSES)[number];

export const CATEGORY_ATTRIBUTE_DATA_TYPES = ["TEXT", "NUMBER", "SELECT"] as const;
export type CategoryAttributeDataType = (typeof CATEGORY_ATTRIBUTE_DATA_TYPES)[number];

// Address Label
export const ADDRESS_LABELS = ["HOME", "OFFICE", "OTHER"] as const;
export type AddressLabel = (typeof ADDRESS_LABELS)[number];

// Order & Payment
export const ORDER_STATUSES = [
  "PENDING_PAYMENT",
  "PENDING_CONFIRMATION",
  "PROCESSING",
  "CONFIRMED",
  "PREPARING",
  "WAITING_FOR_PICKUP",
  "SHIPPING",
  "SHIPPED",
  "DELIVERED",
  "RETURN_REQUESTED",
  "COMPLETED",
  "CANCELLED",
  "RETURNED",
  "REFUNDED",
] as const;
export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const PAYMENT_METHODS = ["COD", "VNPAY"] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];

export const PAYMENT_STATUSES = ["PENDING", "COMPLETED", "FAILED", "EXPIRED", "REFUNDED"] as const;
export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];

// Cancellation & Return
export const CANCELLATION_REASON_CODES = [
  "BUYER_CHANGED_MIND",
  "BUYER_FOUND_CHEAPER_PRICE",
  "BUYER_INCORRECT_ADDRESS",
  "BUYER_PAYMENT_ISSUE",
  "SELLER_OUT_OF_STOCK",
  "SELLER_FORCE_MAJEURE",
  "OTHER",
] as const;
export type CancellationReasonCode = (typeof CANCELLATION_REASON_CODES)[number];

export const CANCELLATION_REQUEST_STATUSES = ["PENDING", "APPROVED", "REJECTED"] as const;
export type CancellationRequestStatus = (typeof CANCELLATION_REQUEST_STATUSES)[number];

export const RETURN_REQUEST_STATUSES = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"] as const;
export type ReturnRequestStatus = (typeof RETURN_REQUEST_STATUSES)[number];

// Voucher
export const VOUCHER_TYPES = ["PLATFORM", "SHOP"] as const;
export type VoucherType = (typeof VOUCHER_TYPES)[number];

export const VOUCHER_STATUSES = ["UPCOMING", "ONGOING", "EXPIRED", "DISABLED"] as const;
export type VoucherStatus = (typeof VOUCHER_STATUSES)[number];

export const DISCOUNT_TYPES = ["PERCENTAGE", "FIXED_AMOUNT"] as const;
export type DiscountType = (typeof DISCOUNT_TYPES)[number];

export const VOUCHER_APPLIES_TO = ["PRODUCT", "SHIPPING_FEE"] as const;
export type VoucherAppliesTo = (typeof VOUCHER_APPLIES_TO)[number];

// Admin Dashboard, Payouts & Notifications
export const DASHBOARD_PERIODS = ["TODAY", "LAST_7_DAYS", "THIS_MONTH", "CUSTOM"] as const;
export type DashboardPeriod = (typeof DASHBOARD_PERIODS)[number];

export const PAYOUT_STATUSES = ["PENDING", "COMPLETED", "REJECTED"] as const;
export type PayoutStatus = (typeof PAYOUT_STATUSES)[number];

export const PAYOUT_PROCESS_ACTIONS = ["APPROVE", "REJECT"] as const;
export type PayoutProcessAction = (typeof PAYOUT_PROCESS_ACTIONS)[number];

export const TARGET_AUDIENCES = ["ALL_USERS", "ALL_BUYERS", "ALL_SELLERS"] as const;
export type TargetAudience = (typeof TARGET_AUDIENCES)[number];

export const NOTIFICATION_TYPES = ["ORDER", "REVIEW", "PROMOTION", "SYSTEM", "SHOP"] as const;
export type NotificationType = (typeof NOTIFICATION_TYPES)[number];
