import { PaymentMethod, PaymentStatus } from "./enums";

export interface ShopOrderRequest {
  shopId: string;
  note?: string | null;
  shippingOptionId?: string | null;
}

export interface CheckoutPreviewRequest {
  selectedCartItemIds: string[];
  addressId?: string | null;
  shopVoucherIds?: string[];
  platformVoucherId?: string | null;
  platformShippingVoucherId?: string | null;
}

export interface ShopCheckoutPreviewResponse {
  shopId: string;
  shopName: string;
  totalProductAmount: number;
  shippingFee: number;
  shopDiscountAmount: number;
  finalShopAmount: number;
  items: Array<{
    cartItemId: string;
    variantId: string;
    productName: string;
    variantName: string;
    price: number;
    quantity: number;
    subtotal: number;
    imageUrl?: string | null;
  }>;
}

export interface CheckoutPreviewResponse {
  totalProductAmount: number;
  totalShippingFee: number;
  totalShopDiscount: number;
  platformDiscountAmount: number;
  platformShippingDiscountAmount: number;
  finalTotalAmount: number;
  shops: ShopCheckoutPreviewResponse[];
}

export interface PlaceOrderRequest {
  addressId: string;
  paymentMethod: PaymentMethod;
  bankCode?: string | null;
  selectedCartItemIds: string[];
  shopOrders: ShopOrderRequest[];
  platformVoucherId?: string | null;
  platformShippingVoucherId?: string | null;
  idempotencyKey?: string | null;
}

export interface OrderSummaryResponse {
  orderId: string;
  orderNumber: string;
  shopId: string;
  shopName: string;
  totalAmount: number;
}

export interface PlaceOrderResponse {
  paymentId: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  finalTotalAmount: number;
  totalOrdersCount: number;
  orders: OrderSummaryResponse[];
  paymentUrl?: string | null;
  bankCode?: string | null;
  createdAt: string;
}

export interface PaymentMethodResponse {
  method: PaymentMethod;
  name: string;
  description: string;
  isAvailable: boolean;
  bankCodes?: Array<{
    code: string;
    name: string;
    logoUrl?: string;
  }>;
}

export interface BuyerPaymentStatusResponse {
  paymentId: string;
  status: PaymentStatus;
  paymentMethod: PaymentMethod;
  amount: number;
  paymentUrl?: string | null;
  isExpired: boolean;
  retryAllowed: boolean;
}

export interface VNPayCallbackResponse {
  success: boolean;
  transactionId?: string | null;
  orderNumber?: string | null;
  amount: number;
  responseCode: string;
  message: string;
}
