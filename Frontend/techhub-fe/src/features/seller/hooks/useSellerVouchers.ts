import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sellerVoucherApi } from "../api/sellerVoucherApi";
import type {
  SellerVoucherQueryParams,
  CreateShopVoucherRequest,
  UpdateShopVoucherRequest,
  UpdateShopVoucherStatusRequest,
} from "@/types/sellerVoucher";

export const SELLER_VOUCHER_KEYS = {
  all: ["seller", "vouchers"] as const,
  list: (params?: SellerVoucherQueryParams) =>
    ["seller", "vouchers", "list", params] as const,
  detail: (id: string) => ["seller", "vouchers", "detail", id] as const,
};

/**
 * Hook lấy danh sách voucher của shop phân trang & lọc
 */
export function useSellerVouchersQuery(params?: SellerVoucherQueryParams) {
  return useQuery({
    queryKey: SELLER_VOUCHER_KEYS.list(params),
    queryFn: async () => {
      const res = await sellerVoucherApi.getVouchers(params);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

/**
 * Hook lấy chi tiết 1 voucher
 */
export function useSellerVoucherDetailQuery(id?: string) {
  return useQuery({
    queryKey: SELLER_VOUCHER_KEYS.detail(id || ""),
    queryFn: async () => {
      if (!id) return null;
      const res = await sellerVoucherApi.getVoucherDetail(id);
      return res.data;
    },
    enabled: Boolean(id),
    staleTime: 60 * 1000,
  });
}

/**
 * Hook tạo mới voucher shop
 */
export function useCreateVoucherMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateShopVoucherRequest) =>
      sellerVoucherApi.createVoucher(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_VOUCHER_KEYS.all });
    },
  });
}

/**
 * Hook cập nhật voucher shop
 */
export function useUpdateVoucherMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdateShopVoucherRequest;
    }) => sellerVoucherApi.updateVoucher(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_VOUCHER_KEYS.all });
    },
  });
}

/**
 * Hook cập nhật trạng thái bật/tắt voucher
 */
export function useUpdateVoucherStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdateShopVoucherStatusRequest;
    }) => sellerVoucherApi.updateVoucherStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_VOUCHER_KEYS.all });
    },
  });
}

/**
 * Hook xóa voucher shop
 */
export function useDeleteVoucherMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => sellerVoucherApi.deleteVoucher(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_VOUCHER_KEYS.all });
    },
  });
}
