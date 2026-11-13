import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { adminModerationApi } from "../api/adminModerationApi";
import type {
  AdminProductsQueryParams,
  UpdateProductApprovalRequest,
  UpdateProductSuspendRequest,
} from "@/types/adminCatalog";
import type {
  AdminSellersQueryParams,
  AdminShopsQueryParams,
  AdminUpdateShopStatusRequest,
  SellerApprovalRequest,
} from "@/types/admin";

export const ADMIN_MODERATION_KEYS = {
  products: (params?: AdminProductsQueryParams) =>
    ["admin", "products", params] as const,
  productDetail: (id: string) => ["admin", "products", "detail", id] as const,
  sellers: (params?: AdminSellersQueryParams) =>
    ["admin", "sellers", params] as const,
  sellerDetail: (id: string) => ["admin", "sellers", "detail", id] as const,
  shops: (params?: AdminShopsQueryParams) => ["admin", "shops", params] as const,
  shopDetail: (id: string) => ["admin", "shops", "detail", id] as const,
};

export function useAdminProductsQuery(params?: AdminProductsQueryParams) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.products(params),
    queryFn: async () => {
      const res = await adminModerationApi.getProducts(params);
      return res.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useAdminProductDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.productDetail(id || ""),
    queryFn: async () => {
      const res = await adminModerationApi.getProductDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useUpdateProductApprovalMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdateProductApprovalRequest;
    }) => adminModerationApi.updateProductApproval(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "products"] });
      qc.invalidateQueries({
        queryKey: ADMIN_MODERATION_KEYS.productDetail(variables.id),
      });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}

export function useUpdateProductSuspendMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdateProductSuspendRequest;
    }) => adminModerationApi.updateProductSuspend(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "products"] });
      qc.invalidateQueries({
        queryKey: ADMIN_MODERATION_KEYS.productDetail(variables.id),
      });
    },
  });
}

export function useAdminSellersQuery(
  params?: AdminSellersQueryParams,
  enabled = true
) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.sellers(params),
    queryFn: async () => {
      const res = await adminModerationApi.getSellers(params);
      return res.data;
    },
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminSellerDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.sellerDetail(id || ""),
    queryFn: async () => {
      const res = await adminModerationApi.getSellerDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useUpdateSellerApprovalMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: SellerApprovalRequest }) =>
      adminModerationApi.updateSellerApproval(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "sellers"] });
      qc.invalidateQueries({ queryKey: ["admin", "shops"] });
      qc.invalidateQueries({
        queryKey: ADMIN_MODERATION_KEYS.sellerDetail(variables.id),
      });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}

export function useAdminShopsQuery(
  params?: AdminShopsQueryParams,
  enabled = true
) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.shops(params),
    queryFn: async () => {
      const res = await adminModerationApi.getShops(params);
      return res.data;
    },
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminShopDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_MODERATION_KEYS.shopDetail(id || ""),
    queryFn: async () => {
      const res = await adminModerationApi.getShopDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useUpdateAdminShopStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: AdminUpdateShopStatusRequest;
    }) => adminModerationApi.updateShopStatus(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "shops"] });
      qc.invalidateQueries({
        queryKey: ADMIN_MODERATION_KEYS.shopDetail(variables.id),
      });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}
