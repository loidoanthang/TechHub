// Role
export type Role = "BUYER" | "SELLER" | "ADMIN";

// User & Shop Status
export type UserStatus = "ACTIVE" | "SUSPENDED" | "BANNED" | "DELETED";
export type ShopStatus = "ACTIVE" | "PAUSED" | "SUSPENDED" | "BANNED";
export type SellerVerificationStatus = "PENDING" | "APPROVED" | "REJECTED";

// Product Status
export type ProductApprovalStatus = "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED";
export type ProductVisibilityStatus = "PUBLISHED" | "UNPUBLISHED";
export type ProductVariantStatus = "ACTIVE" | "INACTIVE";
export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type CategoryAttributeDataType = "TEXT" | "NUMBER" | "SELECT";

// Address Label
export type AddressLabel = "HOME" | "OFFICE" | "OTHER";

// Order & Payment
export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PENDING_CONFIRMATION"
  | "PROCESSING"
  | "CONFIRMED"
  | "PREPARING"
  | "WAITING_FOR_PICKUP"
  | "SHIPPING"
  | "SHIPPED"
  | "DELIVERED"
  | "RETURN_REQUESTED"
  | "COMPLETED"
  | "CANCELLED"
  | "RETURNED"
  | "REFUNDED";

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
export type VoucherStatus = "ACTIVE" | "INACTIVE" | "EXPIRED" | "EXHAUSTED" | "DISABLED" | "UPCOMING" | "ONGOING";
export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type VoucherAppliesTo = "PRODUCT" | "SHIPPING_FEE" | "ALL_PRODUCTS" | "SPECIFIC_PRODUCTS" | "SHIPPING";

// Admin Dashboard, Payouts & Notifications
export type DashboardPeriod = "TODAY" | "LAST_7_DAYS" | "THIS_MONTH" | "CUSTOM";
export type PayoutStatus = "PENDING" | "COMPLETED" | "REJECTED";
export type PayoutProcessAction = "APPROVE" | "REJECT";
export type TargetAudience = "ALL_USERS" | "ALL_BUYERS" | "ALL_SELLERS";
export type NotificationType = "ORDER" | "REVIEW" | "PROMOTION" | "SYSTEM" | "SHOP";
