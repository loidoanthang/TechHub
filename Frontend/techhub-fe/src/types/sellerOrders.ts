import type {
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  CancellationReasonCode,
  CancellationRequestStatus,
  ReturnRequestStatus,
} from "./enums";

export interface SellerOrderBuyerResponse {
  buyerId: string;
  buyerName: string;
  recipientName: string;
  recipientPhone: string;
  provinceCity: string;
  deliveryAddressSummary: string;
}

export interface SellerOrderItemResponse {
  orderItemId: string;
  productVariantId: string;
  productName: string;
  variantName: string;
  sku: string;
  imageUrl: string;
  originalPrice: number;
  discountedUnitPrice: number;
  quantity: number;
  finalSubtotal: number;
}

export interface AddressSnapshotResponse {
  contactName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
}

export interface OrderTimelineStepResponse {
  stepNumber: number;
  stepName: string;
  description: string;
  timestamp?: string | null;
  isCompleted: boolean;
  isCurrent: boolean;
}

export interface SellerOrderResponse {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  statusDescription: string;
  buyerInfo: SellerOrderBuyerResponse;
  totalItemsCount: number;
  items: SellerOrderItemResponse[];
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

export interface SellerOrderDetailResponse extends SellerOrderResponse {
  shopId: string;
  shopName: string;
  deliveryAddress: AddressSnapshotResponse;
  deliveredAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  timeline: OrderTimelineStepResponse[];
}

export interface UpdateSellerOrderStatusRequest {
  status: OrderStatus;
  note?: string;
}

export interface CancelOrderRequest {
  reasonCode: CancellationReasonCode;
  reasonDetail?: string;
}

export interface CancellationRequestResponse {
  id: string;
  orderId: string;
  orderNumber: string;
  requestedBy: string;
  reasonCode: CancellationReasonCode;
  reasonDetail?: string | null;
  status: CancellationRequestStatus;
  sellerResponseNote?: string | null;
  requestedAt: string;
  processedAt?: string | null;
}

export interface ProcessCancellationRequest {
  approved: boolean;
  note?: string;
}

export interface ReturnRequestResponse {
  id: string;
  orderId: string;
  orderNumber: string;
  reasonCode: string;
  reasonDetail?: string | null;
  evidenceImages?: string[];
  status: ReturnRequestStatus;
  sellerResponseNote?: string | null;
  requestedAt: string;
  processedAt?: string | null;
}

export interface ProcessReturnRequest {
  approved: boolean;
  note?: string;
}

export interface SellerOrderFilterParams {
  status?: OrderStatus;
  keyword?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
