export type DashboardPeriod = "TODAY" | "LAST_7_DAYS" | "THIS_MONTH" | "CUSTOM";

export interface SellerDashboardSummaryDto {
  netRevenue: number;
  totalOrders: number;
  completedOrders: number;
  totalUnitsSold: number;
  walletBalance: number;
  pendingPayoutAmount: number;
  averageOrderValue: number;
}

export interface SellerOrderStatusBreakdownDto {
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

export interface TopSellingProductDto {
  productId: string;
  productName: string;
  imageUrl: string;
  unitsSold: number;
  totalRevenue: number;
}

export interface LowStockVariantDto {
  variantId: string;
  productId: string;
  productName: string;
  variantName: string;
  sku: string;
  imageUrl: string;
  stockQuantity: number;
  price: number;
}

export interface SellerDashboardMetricsResponse {
  period: DashboardPeriod;
  startDate: string | null;
  endDate: string | null;
  summary: SellerDashboardSummaryDto;
  orderStatusBreakdown: SellerOrderStatusBreakdownDto;
  topSellingProducts: TopSellingProductDto[];
  lowStockAlerts: LowStockVariantDto[];
}

export interface SellerDashboardParams {
  period?: DashboardPeriod;
  startDate?: string;
  endDate?: string;
}
