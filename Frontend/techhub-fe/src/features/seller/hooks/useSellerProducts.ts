import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { sellerProductApi } from "../api/sellerProductApi";
import type {
  SellerProductQueryParams,
  UpdateProductVisibilityRequest,
  CreateProductRequest,
} from "@/types/sellerProduct";

export const SELLER_PRODUCT_KEYS = {
  all: ["seller", "products"] as const,
  list: (params?: SellerProductQueryParams) =>
    ["seller", "products", "list", params] as const,
  detail: (id: string) => ["seller", "products", "detail", id] as const,
  categories: ["categories", "active-list"] as const,
  categoryAttributes: (categoryId: string) =>
    ["categories", categoryId, "attributes"] as const,
};

/**
 * Hook truy vấn danh sách sản phẩm Người Bán
 */
export function useSellerProductsQuery(params?: SellerProductQueryParams) {
  return useQuery({
    queryKey: SELLER_PRODUCT_KEYS.list(params),
    queryFn: async () => {
      const res = await sellerProductApi.getProducts(params);
      return res.data;
    },
    staleTime: 30 * 1000,
  });
}

/**
 * Hook lấy danh mục cho dropdown bộ lọc
 */
export function useSellerCategoriesQuery() {
  return useQuery({
    queryKey: SELLER_PRODUCT_KEYS.categories,
    queryFn: async () => {
      const res = await sellerProductApi.getCategories();
      return res.data;
    },
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook cập nhật trạng thái hiển thị (PUBLISHED / UNPUBLISHED)
 */
export function useUpdateProductVisibilityMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      productId,
      data,
    }: {
      productId: string;
      data: UpdateProductVisibilityRequest;
    }) => sellerProductApi.updateVisibility(productId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCT_KEYS.all });
    },
  });
}

/**
 * Hook gửi duyệt sản phẩm (DRAFT / REJECTED -> PENDING_APPROVAL)
 */
export function useSubmitProductApprovalMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (productId: string) => sellerProductApi.submitApproval(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCT_KEYS.all });
    },
  });
}

/**
 * Hook xoá mềm sản phẩm
 */
export function useDeleteProductMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (productId: string) => sellerProductApi.deleteProduct(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCT_KEYS.all });
    },
  });
}

/**
 * Hook lấy thuộc tính kỹ thuật động theo danh mục (BR-09)
 */
export function useCategoryAttributesQuery(categoryId?: string) {
  return useQuery({
    queryKey: SELLER_PRODUCT_KEYS.categoryAttributes(categoryId || ""),
    queryFn: async () => {
      if (!categoryId) return [];
      const res = await sellerProductApi.getCategoryAttributes(categoryId);
      return res.data;
    },
    enabled: Boolean(categoryId),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook tạo mới sản phẩm (POST /api/seller/products)
 */
export function useCreateProductMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProductRequest) => sellerProductApi.createProduct(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SELLER_PRODUCT_KEYS.all });
    },
  });
}
