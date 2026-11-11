import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import type { ApiResponse, PageResponse } from "@/types/api";
import type {
  ShopVoucherResponse,
  ShopVoucherDetailResponse,
  CreateShopVoucherRequest,
  UpdateShopVoucherRequest,
  UpdateShopVoucherStatusRequest,
  SellerVoucherQueryParams,
} from "@/types/sellerVoucher";

export const sellerVoucherApi = {
  /**
   * Lấy danh sách mã giảm giá của shop
   */
  getVouchers: (
    params?: SellerVoucherQueryParams
  ): Promise<ApiResponse<PageResponse<ShopVoucherResponse>>> => {
    return apiClient.get(ENDPOINTS.SELLER.VOUCHERS, { params });
  },

  /**
   * Lấy chi tiết 1 mã giảm giá của shop
   */
  getVoucherDetail: (
    id: string
  ): Promise<ApiResponse<ShopVoucherDetailResponse>> => {
    return apiClient.get(ENDPOINTS.SELLER.VOUCHER_DETAIL(id));
  },

  /**
   * Tạo mã giảm giá mới
   */
  createVoucher: (
    data: CreateShopVoucherRequest
  ): Promise<ApiResponse<ShopVoucherResponse>> => {
    return apiClient.post(ENDPOINTS.SELLER.VOUCHERS, data);
  },

  /**
   * Cập nhật mã giảm giá
   */
  updateVoucher: (
    id: string,
    data: UpdateShopVoucherRequest
  ): Promise<ApiResponse<ShopVoucherDetailResponse>> => {
    return apiClient.put(ENDPOINTS.SELLER.VOUCHER_DETAIL(id), data);
  },

  /**
   * Cập nhật trạng thái hoạt động (bật / tắt) của mã giảm giá
   */
  updateVoucherStatus: (
    id: string,
    data: UpdateShopVoucherStatusRequest
  ): Promise<ApiResponse<ShopVoucherDetailResponse>> => {
    return apiClient.patch(ENDPOINTS.SELLER.VOUCHER_STATUS(id), data);
  },

  /**
   * Xóa mã giảm giá
   */
  deleteVoucher: (id: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.SELLER.VOUCHER_DETAIL(id));
  },
};
