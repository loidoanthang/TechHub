import {
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  CancellationReasonCode,
  CancellationRequestStatus,
  ReturnRequestStatus,
  AddressLabel,
} from "./enums";

export interface AddressSnapshotResponse {
  recipientName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  fullAddress: string;
  label?: AddressLabel | null;
}

export interface BuyerOrderItemResponse {
  orderItemId: string;
  productVariantId?: string;
  productId?: string;
  productName: string;
  productSlug?: string | null;
  variantId?: string;
  sku?: string;
  variantSku?: string;
  variantName: string;
  variantAttributes?: Record<string, unknown>;
  originalPrice?: number;
  discountedUnitPrice?: number;
  itemDiscountAmount?: number;
  finalSubtotal?: number;
  price?: number;
  quantity: number;
  subtotal?: number;
  imageUrl?: string | null;
  hasReviewed?: boolean;
}

export interface OrderTimelineStepResponse {
  status: OrderStatus;
  title: string;
  description: string;
  timestamp?: string | null;
  isCompleted: boolean;
  isCurrent: boolean;
}

export interface BuyerOrderResponse {
  orderId: string;
  orderNumber: string;
  shopId: string;
  shopName: string;
  shopSlug?: string | null;
  status: OrderStatus;
  statusDescription: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  platformDiscountAmount: number;
  totalAmount: number;
  totalItemsCount: number;
  items: BuyerOrderItemResponse[];
  createdAt: string;
  deliveredAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  availableActions: string[];
}

export interface BuyerOrderDetailResponse {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  statusDescription: string;
  shopId: string;
  shopName: string;
  shopSlug?: string | null;
  shopAvatarUrl?: string | null;
  deliveryAddress: AddressSnapshotResponse;
  items: BuyerOrderItemResponse[];
  totalItemsCount: number;
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  platformDiscountAmount: number;
  totalAmount: number;
  note?: string | null;
  paymentMethod: PaymentMethod;
  paymentMethodName: string;
  paymentStatus: PaymentStatus;
  paymentStatusDescription: string;
  createdAt: string;
  deliveredAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  timeline: OrderTimelineStepResponse[];
  availableActions: string[];
}

export interface CreateCancellationRequest {
  reason?: string;
  reasonCode?: CancellationReasonCode;
  reasonNote?: string;
}

export interface CancelOrderRequest {
  reasonCode: CancellationReasonCode;
  reasonNote?: string;
}

export interface CancellationRequestResponse {
  id: string;
  orderId: string;
  orderNumber: string;
  shopId: string;
  shopName: string;
  status: CancellationRequestStatus;
  reason?: string | null;
  reasonNote?: string | null;
  requestedAt: string;
  respondedAt?: string | null;
}

export interface CreateReturnRequest {
  reason: string;
  proofImages?: string;
  bankName?: string;
  bankAccountNumber?: string;
  bankAccountHolder?: string;
}

export interface ReturnRequestResponse {
  id: string;
  orderId: string;
  orderNumber: string;
  shopId: string;
  shopName: string;
  status: ReturnRequestStatus;
  statusDescription: string;
  reason: string;
  proofImages?: string | null;
  refundAmount: number;
  sellerNote?: string | null;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  createdAt: string;
  respondedAt?: string | null;
}
