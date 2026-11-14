import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { PublicShopResponse, CartShopVoucherResponse } from "../types/shop";
import type { ProductSummaryResponse } from "@/types/product";

export interface ShopProductsQueryParams {
  keyword?: string;
  sortBy?: "newest" | "price_asc" | "price_desc" | "rating";
  page?: number;
  size?: number;
}

export const shopApi = {
  getPublicShopProfile: (
    sellerName: string
  ): Promise<ApiResponse<PublicShopResponse>> => {
    return apiClient.get(API_ENDPOINTS.SHOPS.DETAIL(sellerName));
  },

  getShopVouchers: (
    sellerName: string,
    orderAmount?: number
  ): Promise<ApiResponse<CartShopVoucherResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.SHOPS.VOUCHERS(sellerName), {
      params: orderAmount ? { orderAmount } : undefined,
    });
  },

  getShopProducts: (
    params?: ShopProductsQueryParams
  ): Promise<ApiResponse<PageResponse<ProductSummaryResponse>>> => {
    return apiClient.get(API_ENDPOINTS.PRODUCTS.BASE, {
      params: {
        keyword: params?.keyword,
        sortBy: params?.sortBy ?? "newest",
        page: params?.page ?? 0,
        size: params?.size ?? 24,
      },
    });
  },
};
