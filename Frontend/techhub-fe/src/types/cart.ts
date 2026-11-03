import { ShopStatus } from "./enums";

export interface CartCountResponse {
  totalQuantity: number;
}

export interface CartItemResponse {
  id: string;
  productId: string;
  productName: string;
  productSlug: string;
  variantId: string;
  variantSku: string;
  variantName: string;
  variantAttributes?: Record<string, unknown>;
  price: number;
  quantity: number;
  itemSubtotal: number;
  imageUrl?: string | null;
  stockQuantity: number;
  isOutOfStock: boolean;
  isAvailable: boolean;
  unavailableReason?: string | null;
  createdAt: string;
}

export interface ShopCartGroupResponse {
  shopId: string;
  shopName: string;
  sellerName: string;
  shopStatus: ShopStatus;
  isShopPaused: boolean;
  items: CartItemResponse[];
}

export interface CartResponse {
  id: string;
  totalItemTypes: number;
  totalQuantity: number;
  shops: ShopCartGroupResponse[];
}

export interface AddToCartRequest {
  productVariantId: string;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CartPreviewRequest {
  selectedCartItemIds: string[];
  shopVoucherIds?: string[];
  platformVoucherId?: string | null;
}

export interface CartPreviewResponse {
  subtotalAmount: number;
  totalDiscountAmount: number;
  finalTotalAmount: number;
  totalItemsCount: number;
  shops: Array<{
    shopId: string;
    shopName: string;
    shopSubtotal: number;
    shopDiscount: number;
    shopFinalAmount: number;
  }>;
}
