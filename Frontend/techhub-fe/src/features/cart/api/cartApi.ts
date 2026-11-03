import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse } from "@/types/api";
import type {
  CartResponse,
  CartCountResponse,
  CartItemResponse,
  AddToCartRequest,
  UpdateCartItemRequest,
  CartPreviewRequest,
  CartPreviewResponse,
} from "@/types/cart";

export const cartApi = {
  getCart: (): Promise<ApiResponse<CartResponse>> => {
    return apiClient.get(API_ENDPOINTS.CART.BASE);
  },

  getCartCount: (): Promise<ApiResponse<CartCountResponse>> => {
    return apiClient.get(API_ENDPOINTS.CART.COUNT);
  },

  addToCart: (data: AddToCartRequest): Promise<ApiResponse<CartItemResponse>> => {
    return apiClient.post(API_ENDPOINTS.CART.ITEMS, data);
  },

  updateCartItem: (
    itemId: string,
    data: UpdateCartItemRequest
  ): Promise<ApiResponse<CartItemResponse>> => {
    return apiClient.put(API_ENDPOINTS.CART.ITEM(itemId), data);
  },

  deleteCartItem: (itemId: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(API_ENDPOINTS.CART.ITEM(itemId));
  },

  previewCart: (
    data: CartPreviewRequest
  ): Promise<ApiResponse<CartPreviewResponse>> => {
    return apiClient.post(API_ENDPOINTS.CART.PREVIEW, data);
  },
};
