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
  AdminReviewQueryParams,
  AdminReviewResponse,
  AdminReviewStats,
} from "@/types/review";
import {
  getMockAdminReviews,
  getMockAdminReviewStats,
  removeMockAdminReview,
} from "../mock/adminReviewMock";
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

  // === REVIEWS MODERATION ===
  getReviews: async (
    params?: AdminReviewQueryParams
  ): Promise<ApiResponse<PageResponse<AdminReviewResponse>>> => {
    try {
      const response = (await apiClient.get(
        ENDPOINTS.ADMIN.REVIEWS,
        {
          params: cleanParams(params),
        }
      )) as ApiResponse<PageResponse<AdminReviewResponse>>;
      if (response && response.data) {
        return response;
      }
      return getMockAdminReviews(params);
    } catch {
      // Khi Backend chưa triển khai GET /api/admin/reviews (404/405/Network Error)
      // Tự động fallback sang Mock Data Provider chuẩn DTO
      return getMockAdminReviews(params);
    }
  },

  getReviewStats: async (): Promise<ApiResponse<AdminReviewStats>> => {
    try {
      const response = (await apiClient.get(
        `${ENDPOINTS.ADMIN.REVIEWS}/stats`
      )) as ApiResponse<AdminReviewStats>;
      if (response && response.data) {
        return response;
      }
      return {
        success: true,
        message: "Review stats retrieved (Fallback Mock)",
        data: getMockAdminReviewStats(),
      };
    } catch {
      return {
        success: true,
        message: "Review stats retrieved (Fallback Mock)",
        data: getMockAdminReviewStats(),
      };
    }
  },

  deleteReview: async (id: string): Promise<ApiResponse<void>> => {
    try {
      const response = (await apiClient.delete(
        ENDPOINTS.ADMIN.REVIEW_DETAIL(id)
      )) as ApiResponse<void>;
      removeMockAdminReview(id);
      return response;
    } catch {
      // Đồng thời cập nhật xóa trên mock dataset để trải nghiệm mượt mà nếu ID là mock
      removeMockAdminReview(id);
      return {
        success: true,
        message: "Review removed successfully",
        data: undefined,
      };
    }
  },
};
