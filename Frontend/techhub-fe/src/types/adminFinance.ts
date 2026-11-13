import type {
  DiscountType,
  NotificationType,
  PayoutProcessAction,
  PayoutStatus,
  ShopStatus,
  TargetAudience,
  VoucherAppliesTo,
  VoucherStatus,
  VoucherType,
} from "./enums";

// ==================== ADMIN VOUCHERS ====================
export interface AdminVoucherResponse {
  id: string;
  shopId?: string | null;
  shopName?: string | null;
  shopStatus?: ShopStatus | null;
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
  isDeleted: boolean;
  status: VoucherStatus;
  createdAt: string;
}

export interface AdminVoucherDetailResponse {
  id: string;
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
  remainingCount?: number | null;
  redemptionRate?: number | null;
  shopId?: string | null;
  shopName?: string | null;
  shopStatus?: ShopStatus | null;
  shopAvatarUrl?: string | null;
  sponsor: string;
  subsidyNote: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
  status: VoucherStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlatformVoucherRequest {
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

export interface UpdatePlatformVoucherRequest {
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

export interface UpdatePlatformVoucherStatusRequest {
  isActive: boolean;
}

export interface AdminVouchersQueryParams {
  type?: VoucherType | "";
  shopId?: string;
  keyword?: string;
  appliesTo?: VoucherAppliesTo | "";
  discountType?: DiscountType | "";
  status?: VoucherStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

// ==================== PAYOUTS ====================
export interface AdminPayoutResponse {
  id: string;
  sellerId: string;
  sellerName: string;
  shopName?: string | null;
  amount: number;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  status: PayoutStatus;
  rejectionReason?: string | null;
  processedById?: string | null;
  processedByName?: string | null;
  processedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProcessPayoutRequest {
  action: PayoutProcessAction;
  rejectionReason?: string;
}

export interface AdminPayoutsQueryParams {
  status?: PayoutStatus | "";
  keyword?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "ASC" | "DESC";
}

// ==================== PLATFORM POLICIES ====================
export interface PlatformPolicyResponse {
  id: string;
  policyKey: string;
  policyValue: string;
  description?: string | null;
  updatedById?: string | null;
  updatedByName?: string | null;
  updatedAt: string;
  createdAt: string;
}

export interface UpdatePlatformPolicyRequest {
  policyKey: string;
  policyValue: string;
  description?: string;
}

// ==================== BROADCAST NOTIFICATIONS ====================
export interface BroadcastNotificationRequest {
  targetAudience: TargetAudience;
  title: string;
  content: string;
  type?: NotificationType;
  referenceType?: string;
  referenceId?: string;
}

export interface BroadcastNotificationResponse {
  totalTargetUsers: number;
  totalSent: number;
  targetAudience: TargetAudience;
  type: NotificationType;
  title: string;
  broadcastAt: string;
}

// ==================== SHIPPING SIMULATION ====================
export interface ShippingSimulationRequest {
  forceAdvance?: boolean;
  batchSize?: number;
}

export interface ShippingSimulationResponse {
  processedWaitingOrders: number;
  processedShippingOrders: number;
  updatedCodPayments: number;
  executionDurationMs: number;
  message: string;
}
