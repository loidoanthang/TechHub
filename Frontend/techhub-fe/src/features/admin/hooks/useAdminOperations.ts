import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { adminOperationsApi } from "../api/adminOperationsApi";
import type {
  AdminCategoriesQueryParams,
  CreateCategoryAttributeRequest,
  CreateCategoryRequest,
  UpdateCategoryAttributeRequest,
  UpdateCategoryRequest,
  UpdateCategoryStatusRequest,
} from "@/types/adminCatalog";
import type {
  AdminCancelOrderRequest,
  AdminOrdersQueryParams,
  AdminUsersQueryParams,
  UpdateUserStatusRequest,
} from "@/types/admin";

export const ADMIN_OPERATIONS_KEYS = {
  categories: (params?: AdminCategoriesQueryParams) =>
    ["admin", "categories", params] as const,
  categoryDetail: (id: string) =>
    ["admin", "categories", "detail", id] as const,
  orders: (params?: AdminOrdersQueryParams) =>
    ["admin", "orders", params] as const,
  orderDetail: (id: string) => ["admin", "orders", "detail", id] as const,
  users: (params?: AdminUsersQueryParams) => ["admin", "users", params] as const,
};

// === CATEGORIES ===
export function useAdminCategoriesQuery(params?: AdminCategoriesQueryParams) {
  return useQuery({
    queryKey: ADMIN_OPERATIONS_KEYS.categories(params),
    queryFn: async () => {
      const res = await adminOperationsApi.getCategories(params);
      return res.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useAdminCategoryDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(id || ""),
    queryFn: async () => {
      const res = await adminOperationsApi.getCategoryDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useCreateCategoryMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) =>
      adminOperationsApi.createCategory(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
    },
  });
}

export function useUpdateCategoryMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCategoryRequest }) =>
      adminOperationsApi.updateCategory(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(variables.id),
      });
    },
  });
}

export function useUpdateCategoryStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: UpdateCategoryStatusRequest;
    }) => adminOperationsApi.updateCategoryStatus(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(variables.id),
      });
    },
  });
}

export function useDeleteCategoryMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminOperationsApi.deleteCategory(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
    },
  });
}

export function useCreateCategoryAttributeMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      categoryId,
      data,
    }: {
      categoryId: string;
      data: CreateCategoryAttributeRequest;
    }) => adminOperationsApi.createCategoryAttribute(categoryId, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(variables.categoryId),
      });
    },
  });
}

export function useUpdateCategoryAttributeMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      categoryId,
      attributeId,
      data,
    }: {
      categoryId: string;
      attributeId: string;
      data: UpdateCategoryAttributeRequest;
    }) =>
      adminOperationsApi.updateCategoryAttribute(categoryId, attributeId, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(variables.categoryId),
      });
    },
  });
}

export function useDeleteCategoryAttributeMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      categoryId,
      attributeId,
    }: {
      categoryId: string;
      attributeId: string;
    }) => adminOperationsApi.deleteCategoryAttribute(categoryId, attributeId),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.categoryDetail(variables.categoryId),
      });
    },
  });
}

// === ORDERS ===
export function useAdminOrdersQuery(params?: AdminOrdersQueryParams) {
  return useQuery({
    queryKey: ADMIN_OPERATIONS_KEYS.orders(params),
    queryFn: async () => {
      const res = await adminOperationsApi.getOrders(params);
      return res.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useAdminOrderDetailQuery(id: string | null) {
  return useQuery({
    queryKey: ADMIN_OPERATIONS_KEYS.orderDetail(id || ""),
    queryFn: async () => {
      const res = await adminOperationsApi.getOrderDetail(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });
}

export function useCancelAdminOrderMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminCancelOrderRequest }) =>
      adminOperationsApi.cancelOrder(id, data),
    onSuccess: (_res, variables) => {
      qc.invalidateQueries({ queryKey: ["admin", "orders"] });
      qc.invalidateQueries({
        queryKey: ADMIN_OPERATIONS_KEYS.orderDetail(variables.id),
      });
      qc.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}

// === USERS ===
export function useAdminUsersQuery(params?: AdminUsersQueryParams) {
  return useQuery({
    queryKey: ADMIN_OPERATIONS_KEYS.users(params),
    queryFn: async () => {
      const res = await adminOperationsApi.getUsers(params);
      return res.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useUpdateUserStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserStatusRequest }) =>
      adminOperationsApi.updateUserStatus(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "users"] });
    },
  });
}
