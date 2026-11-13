import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminProductDetailResponse,
  AdminProductsQueryParams,
  AdminProductSummaryResponse,
  UpdateProductApprovalRequest,
  UpdateProductSuspendRequest,
} from "@/types/adminCatalog";
import type {
  AdminSellerApprovalResponse,
  AdminSellerDetailResponse,
  AdminSellerResponse,
  AdminSellersQueryParams,
  AdminShopDetailResponse,
  AdminShopResponse,
  AdminShopsQueryParams,
  AdminUpdateShopStatusRequest,
  SellerApprovalRequest,
} from "@/types/admin";

function cleanParams(params?: object): Record<string, unknown> {
  if (!params) return {};
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, v]) => v !== undefined && v !== null && v !== ""
    )
  );
}

export const adminModerationApi = {
  // === PRODUCTS MODERATION ===
  getProducts: (
    params?: AdminProductsQueryParams
  ): Promise<ApiResponse<PageResponse<AdminProductSummaryResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.PRODUCTS, {
      params: cleanParams(params),
    });
  },

  getProductDetail: (
    id: string
  ): Promise<ApiResponse<AdminProductDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.PRODUCT_DETAIL(id));
  },

  updateProductApproval: (
    id: string,
    data: UpdateProductApprovalRequest
  ): Promise<ApiResponse<AdminProductDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.PRODUCT_APPROVAL(id), data);
  },

  updateProductSuspend: (
    id: string,
    data: UpdateProductSuspendRequest
  ): Promise<ApiResponse<AdminProductDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.PRODUCT_SUSPEND(id), data);
  },

  // === SELLERS VERIFICATION ===
  getSellers: (
    params?: AdminSellersQueryParams
  ): Promise<ApiResponse<PageResponse<AdminSellerResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.SELLERS, {
      params: cleanParams(params),
    });
  },

  getSellerDetail: (
    id: string
  ): Promise<ApiResponse<AdminSellerDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.SELLER_DETAIL(id));
  },

  updateSellerApproval: (
    id: string,
    data: SellerApprovalRequest
  ): Promise<ApiResponse<AdminSellerApprovalResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.SELLER_APPROVAL(id), data);
  },

  // === SHOPS MANAGEMENT ===
  getShops: (
    params?: AdminShopsQueryParams
  ): Promise<ApiResponse<PageResponse<AdminShopResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.SHOPS, {
      params: cleanParams(params),
    });
  },

  getShopDetail: (
    id: string
  ): Promise<ApiResponse<AdminShopDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.SHOP_DETAIL(id));
  },

  updateShopStatus: (
    id: string,
    data: AdminUpdateShopStatusRequest
  ): Promise<ApiResponse<AdminShopResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.SHOP_STATUS(id), data);
  },
};
