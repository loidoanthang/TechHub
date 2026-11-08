import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { CategorySummaryResponse } from "@/types/category";
import type {
  SellerProductSummaryResponse,
  SellerProductQueryParams,
  UpdateProductVisibilityRequest,
  CreateProductRequest,
  ProductDetailResponse,
  CategoryAttributeResponse,
} from "@/types/sellerProduct";

export const sellerProductApi = {
  /**
   * Tạo mới sản phẩm (POST /api/seller/products)
   */
  createProduct: (
    data: CreateProductRequest
  ): Promise<ApiResponse<ProductDetailResponse>> => {
    return apiClient.post(ENDPOINTS.SELLER.PRODUCTS, data);
  },

  /**
   * Lấy danh sách sản phẩm phân trang & bộ lọc của Seller
   */
  getProducts: (
    params?: SellerProductQueryParams
  ): Promise<ApiResponse<PageResponse<SellerProductSummaryResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.PRODUCTS, { params });
  },

  /**
   * Lấy chi tiết 1 sản phẩm
   */
  getProductDetail: (id: string): Promise<ApiResponse<unknown>> => {
    return apiClient.get(ENDPOINTS.SELLER.PRODUCT_DETAIL(id));
  },

  /**
   * Cập nhật trạng thái hiển thị (PUBLISHED / UNPUBLISHED)
   */
  updateVisibility: (
    id: string,
    data: UpdateProductVisibilityRequest
  ): Promise<ApiResponse<unknown>> => {
    return apiClient.patch(ENDPOINTS.SELLER.VISIBILITY(id), data);
  },

  /**
   * Gửi duyệt sản phẩm (DRAFT hoặc REJECTED -> PENDING_APPROVAL)
   */
  submitApproval: (id: string): Promise<ApiResponse<unknown>> => {
    return apiClient.patch(ENDPOINTS.SELLER.SUBMIT_APPROVAL(id));
  },

  /**
   * Xoá mềm sản phẩm
   */
  deleteProduct: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.SELLER.PRODUCT_DETAIL(id));
  },

  /**
   * Lấy danh sách danh mục để làm bộ lọc động
   */
  getCategories: (): Promise<ApiResponse<CategorySummaryResponse[]>> => {
    return apiClient.get(ENDPOINTS.CATEGORIES.BASE, {
      params: { sortBy: "name", sortDir: "asc" },
    });
  },

  /**
   * Lấy danh sách thuộc tính kỹ thuật động của danh mục (BR-09)
   */
  getCategoryAttributes: (
    categoryId: string
  ): Promise<ApiResponse<CategoryAttributeResponse[]>> => {
    return apiClient.get(ENDPOINTS.CATEGORIES.ATTRIBUTES(categoryId));
  },
};
