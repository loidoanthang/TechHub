import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  AdminCategoriesQueryParams,
  CategoryAttributeResponse,
  CategoryDetailResponse,
  CategorySummaryResponse,
  CreateCategoryAttributeRequest,
  CreateCategoryRequest,
  UpdateCategoryAttributeRequest,
  UpdateCategoryRequest,
  UpdateCategoryStatusRequest,
} from "@/types/adminCatalog";
import type {
  AdminCancelOrderRequest,
  AdminOrderDetailResponse,
  AdminOrderResponse,
  AdminOrdersQueryParams,
  AdminUserResponse,
  AdminUsersQueryParams,
  UpdateUserStatusRequest,
} from "@/types/admin";

function cleanParams(params?: object): Record<string, unknown> {
  if (!params) return {};
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, v]) => v !== undefined && v !== null && v !== ""
    )
  );
}

export const adminOperationsApi = {
  // === CATEGORIES & ATTRIBUTES ===
  getCategories: (
    params?: AdminCategoriesQueryParams
  ): Promise<ApiResponse<PageResponse<CategorySummaryResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.CATEGORIES, {
      params: cleanParams(params),
    });
  },

  getCategoryDetail: (
    id: string
  ): Promise<ApiResponse<CategoryDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.CATEGORY_DETAIL(id));
  },

  createCategory: (
    data: CreateCategoryRequest
  ): Promise<ApiResponse<CategoryDetailResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.CATEGORIES, data);
  },

  updateCategory: (
    id: string,
    data: UpdateCategoryRequest
  ): Promise<ApiResponse<CategoryDetailResponse>> => {
    return apiClient.put(ENDPOINTS.ADMIN.CATEGORY_DETAIL(id), data);
  },

  updateCategoryStatus: (
    id: string,
    data: UpdateCategoryStatusRequest
  ): Promise<ApiResponse<CategoryDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.CATEGORY_STATUS(id), data);
  },

  deleteCategory: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.ADMIN.CATEGORY_DETAIL(id));
  },

  createCategoryAttribute: (
    categoryId: string,
    data: CreateCategoryAttributeRequest
  ): Promise<ApiResponse<CategoryAttributeResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.CATEGORY_ATTRIBUTES(categoryId), data);
  },

  updateCategoryAttribute: (
    categoryId: string,
    attributeId: string,
    data: UpdateCategoryAttributeRequest
  ): Promise<ApiResponse<CategoryAttributeResponse>> => {
    return apiClient.put(
      ENDPOINTS.ADMIN.CATEGORY_ATTRIBUTE_DETAIL(categoryId, attributeId),
      data
    );
  },

  deleteCategoryAttribute: (
    categoryId: string,
    attributeId: string
  ): Promise<ApiResponse<void>> => {
    return apiClient.delete(
      ENDPOINTS.ADMIN.CATEGORY_ATTRIBUTE_DETAIL(categoryId, attributeId)
    );
  },

  // === ORDERS ===
  getOrders: (
    params?: AdminOrdersQueryParams
  ): Promise<ApiResponse<PageResponse<AdminOrderResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.ORDERS, {
      params: cleanParams(params),
    });
  },

  getOrderDetail: (
    id: string
  ): Promise<ApiResponse<AdminOrderDetailResponse>> => {
    return apiClient.get(ENDPOINTS.ADMIN.ORDER_DETAIL(id));
  },

  cancelOrder: (
    id: string,
    data: AdminCancelOrderRequest
  ): Promise<ApiResponse<AdminOrderDetailResponse>> => {
    return apiClient.post(ENDPOINTS.ADMIN.ORDER_CANCEL(id), data);
  },

  // === USERS ===
  getUsers: (
    params?: AdminUsersQueryParams
  ): Promise<ApiResponse<PageResponse<AdminUserResponse>>> => {
    return apiClient.get(ENDPOINTS.ADMIN.USERS, {
      params: cleanParams(params),
    });
  },

  updateUserStatus: (
    id: string,
    data: UpdateUserStatusRequest
  ): Promise<ApiResponse<AdminUserResponse>> => {
    return apiClient.patch(ENDPOINTS.ADMIN.USER_STATUS(id), data);
  },
};
