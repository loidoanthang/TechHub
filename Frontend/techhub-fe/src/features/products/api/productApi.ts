import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  ProductSummaryResponse,
  PublicProductDetailResponse,
} from "@/types/product";
import type {
  CategorySummaryResponse,
  CategoryAttributeResponse,
} from "@/types/category";

export interface GetProductsParams {
  keyword?: string;
  categoryId?: string;
  brandName?: string;
  minPrice?: number;
  maxPrice?: number;
  inStockOnly?: boolean;
  sortBy?: string;
  page?: number;
  size?: number;
}

export const productApi = {
  getProducts: (
    params?: GetProductsParams
  ): Promise<ApiResponse<PageResponse<ProductSummaryResponse>>> => {
    return apiClient.get(API_ENDPOINTS.PRODUCTS.BASE, { params });
  },

  getProductBySlug: (
    slug: string
  ): Promise<ApiResponse<PublicProductDetailResponse>> => {
    return apiClient.get(API_ENDPOINTS.PRODUCTS.BY_SLUG(slug));
  },

  getCategories: (params?: {
    keyword?: string;
    sortBy?: string;
    sortDir?: string;
  }): Promise<ApiResponse<CategorySummaryResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.CATEGORIES.BASE, { params });
  },

  getCategoryAttributes: (
    id: string
  ): Promise<ApiResponse<CategoryAttributeResponse[]>> => {
    return apiClient.get(API_ENDPOINTS.CATEGORIES.ATTRIBUTES(id));
  },

  getShopInfo: (sellerName: string): Promise<ApiResponse<unknown>> => {
    return apiClient.get(API_ENDPOINTS.SHOPS.DETAIL(sellerName));
  },

  getShopVouchers: (
    sellerName: string,
    orderAmount?: number
  ): Promise<ApiResponse<unknown>> => {
    return apiClient.get(API_ENDPOINTS.SHOPS.VOUCHERS(sellerName), {
      params: { orderAmount },
    });
  },

  getPlatformVouchers: (params?: {
    orderAmount?: number;
    shippingFee?: number;
    appliesTo?: string;
  }): Promise<ApiResponse<unknown>> => {
    return apiClient.get(API_ENDPOINTS.VOUCHERS.PLATFORM, { params });
  },
};
