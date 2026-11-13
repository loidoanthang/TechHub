import type {
  DashboardPeriod,
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  Role,
  SellerVerificationStatus,
  ShopStatus,
  UserStatus,
} from "./enums";

// ==================== DASHBOARD ====================
export interface AdminDashboardSummaryDto {
  totalGmv: number;
  completedGmv: number;
  platformCommissionRevenue: number;
  totalOrders: number;
  completedOrders: number;
  totalUnitsSold: number;
  averageOrderValue: number;
  cancellationRate: number;
  disputeRate: number;
  totalSellersWalletBalance: number;
  pendingPayoutAmount: number;
  completedPayoutAmount: number;
}

export interface AdminOrderStatusBreakdownDto {
  pendingConfirmation: number;
  preparing: number;
  waitingForPickup: number;
  shipping: number;
  delivered: number;
  completed: number;
  cancelled: number;
  returnRequested: number;
  refunded: number;
}

export interface PlatformGrowthDto {
  newUsersCount: number;
  newShopsCount: number;
  totalActiveShops: number;
  totalSuspendedShops: number;
  totalPausedShops: number;
  totalBannedShops: number;
}

export interface TopPerformingShopDto {
  shopId: string;
  shopName: string;
  logoUrl?: string | null;
  totalOrders: number;
  totalGmv: number;
  commissionContributed: number;
}

export interface AdminDashboardMetricsResponse {
  period: DashboardPeriod;
  startDate: string;
  endDate: string;
  summary: AdminDashboardSummaryDto;
  orderStatusBreakdown: AdminOrderStatusBreakdownDto;
  growthMetrics: PlatformGrowthDto;
  topPerformingShops: TopPerformingShopDto[];
}

export interface AdminDashboardQueryParams {
  period?: DashboardPeriod;
  startDate?: string;
  endDate?: string;
}

// ==================== SELLERS ====================
export interface AdminSellerResponse {
  id: string;
  userId: string;
  ownerName: string;
  ownerEmail: string;
  ownerPhone?: string | null;
  ownerStatus: UserStatus;
  shopId?: string | null;
  shopName?: string | null;
  shopStatus?: ShopStatus | null;
  sellerName: string;
  verificationStatus: SellerVerificationStatus;
  rejectionReason?: string | null;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminSellerDetailResponse {
  id: string;
  sellerName: string;
  verificationStatus: SellerVerificationStatus;
  rejectionReason?: string | null;
  bankName: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  createdAt: string;
  updatedAt: string;
  user: {
    id: string;
    firstName: string;
    lastName: string;
    fullName: string;
    email: string;
    emailVerified: boolean;
    phone?: string | null;
    avatarUrl?: string | null;
    status: UserStatus;
    roles: Role[];
    createdAt: string;
  };
  shop: {
    id: string;
    name: string;
    description?: string | null;
    avatarUrl?: string | null;
    bannerUrl?: string | null;
    status: ShopStatus;
    createdAt: string;
    updatedAt: string;
  } | null;
  warehouseAddress: {
    id: string;
    contactName: string;
    phone: string;
    province: string;
    district: string;
    ward: string;
    streetAddress: string;
    fullAddress: string;
    createdAt: string;
    updatedAt: string;
  } | null;
  shippingOption: {
    id: string;
    name: string;
    shippingFee: number;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
  } | null;
}

export interface SellerApprovalRequest {
  status: SellerVerificationStatus;
  rejectionReason?: string;
}

export interface AdminSellerApprovalResponse {
  sellerId: string;
  userId: string;
  shopId: string;
  sellerName: string;
  shopName: string;
  verificationStatus: SellerVerificationStatus;
  shopStatus: ShopStatus;
  rejectionReason?: string | null;
  roles: Role[];
  updatedAt: string;
}

export interface AdminSellersQueryParams {
  keyword?: string;
  status?: SellerVerificationStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

// ==================== SHOPS ====================
export interface AdminShopResponse {
  id: string;
  name: string;
  description?: string | null;
  avatarUrl?: string | null;
  bannerUrl?: string | null;
  status: ShopStatus;
  sellerId: string;
  sellerName: string;
  sellerVerificationStatus: SellerVerificationStatus;
  ownerId: string;
  ownerName: string;
  ownerEmail: string;
  ownerPhone?: string | null;
  ownerStatus: UserStatus;
  balance: number;
  productCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminShopDetailResponse {
  shop: {
    id: string;
    name: string;
    description?: string | null;
    avatarUrl?: string | null;
    bannerUrl?: string | null;
    status: ShopStatus;
    createdAt: string;
    updatedAt: string;
  };
  seller: {
    id: string;
    sellerName: string;
    verificationStatus: SellerVerificationStatus;
    rejectionReason?: string | null;
    bankName: string;
    bankAccountNumber: string;
    bankAccountHolder: string;
    balance: number;
    createdAt: string;
    updatedAt: string;
  };
  owner: AdminSellerDetailResponse["user"];
  warehouseAddress: AdminSellerDetailResponse["warehouseAddress"];
  shippingOption: AdminSellerDetailResponse["shippingOption"];
  metrics: {
    totalProducts: number;
    activeProducts: number;
    totalCompletedOrders: number;
    cumulativeRevenue: number;
  };
}

export interface AdminUpdateShopStatusRequest {
  status: ShopStatus;
  reason?: string;
}

export interface AdminShopsQueryParams {
  keyword?: string;
  status?: ShopStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

// ==================== USERS ====================
export interface AdminUserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  emailVerified: boolean;
  status: UserStatus;
  defaultAddress?: string | null;
  avatarUrl?: string | null;
  createdAt: string;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
  reason: string;
}

export interface AdminUsersQueryParams {
  keyword?: string;
  status?: UserStatus | "";
  emailVerified?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

// ==================== ORDERS ====================
export interface AdminOrderShopResponse {
  shopId: string;
  shopName: string;
  shopSlug: string;
  sellerEmail: string;
  sellerPhone?: string | null;
}

export interface AdminOrderBuyerResponse {
  buyerId: string;
  buyerName: string;
  buyerEmail: string;
  recipientName: string;
  recipientPhone: string;
  provinceCity: string;
  deliveryAddressSummary: string;
}

export interface AdminOrderItemResponse {
  orderItemId: string;
  productVariantId: string;
  productName: string;
  variantName: string;
  sku: string;
  imageUrl?: string | null;
  originalPrice: number;
  discountedUnitPrice: number;
  quantity: number;
  finalSubtotal: number;
}

export interface AdminOrderPaymentResponse {
  paymentId: string;
  paymentMethod: PaymentMethod;
  paymentMethodName: string;
  paymentStatus: PaymentStatus;
  paymentStatusDescription: string;
  transactionId?: string | null;
  paidAt?: string | null;
}

export interface AdminOrderResponse {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  statusDescription: string;
  shop: AdminOrderShopResponse;
  buyer: AdminOrderBuyerResponse;
  totalItemsCount: number;
  items: AdminOrderItemResponse[];
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  platformDiscountAmount: number;
  totalCommission: number;
  totalSellerEarnings: number;
  buyerTotalAmount: number;
  paymentMethod: PaymentMethod;
  paymentMethodName: string;
  paymentStatus: PaymentStatus;
  paymentStatusDescription: string;
  note?: string | null;
  createdAt: string;
  updatedAt: string;
  availableActions: string[];
}

export interface AdminOrderDetailResponse {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  statusDescription: string;
  shop: AdminOrderShopResponse;
  buyer: AdminOrderBuyerResponse;
  deliveryAddress: {
    recipientName: string;
    phone: string;
    province: string;
    district: string;
    ward: string;
    streetAddress: string;
    fullAddress?: string;
  };
  items: AdminOrderItemResponse[];
  totalItemsCount: number;
  note?: string | null;
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  platformDiscountAmount: number;
  totalCommission: number;
  totalSellerEarnings: number;
  buyerTotalAmount: number;
  paymentInfo: AdminOrderPaymentResponse | null;
  createdAt: string;
  updatedAt: string;
  deliveredAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  timeline: {
    status: string;
    title: string;
    description?: string;
    timestamp?: string | null;
    completed: boolean;
  }[];
  availableActions: string[];
}

export interface AdminCancelOrderRequest {
  reasonCode?: string;
  note: string;
}

export interface AdminOrdersQueryParams {
  shopId?: string;
  userId?: string;
  status?: OrderStatus | "";
  keyword?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
