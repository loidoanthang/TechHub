import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { productApi, type GetProductsParams } from "../api/productApi";

export const PRODUCT_KEYS = {
  products: (params?: GetProductsParams) => ["products", params] as const,
  productDetail: (slug: string) => ["products", "detail", slug] as const,
  similarProducts: (id: string) => ["products", id, "similar"] as const,
  recentlyViewed: () => ["products", "recently-viewed"] as const,
  categories: (params?: { keyword?: string; sortBy?: string; sortDir?: string }) =>
    ["categories", params] as const,
  categoryAttributes: (id: string) => ["categories", id, "attributes"] as const,
};

export function useProductsQuery(params?: GetProductsParams) {
  return useQuery({
    queryKey: PRODUCT_KEYS.products(params),
    queryFn: async () => {
      const res = await productApi.getProducts(params);
      return res.data;
    },
  });
}

export function useProductDetailQuery(slug: string) {
  return useQuery({
    queryKey: PRODUCT_KEYS.productDetail(slug),
    queryFn: async () => {
      const res = await productApi.getProductBySlug(slug);
      return res.data;
    },
    enabled: !!slug,
  });
}

export function useCategoriesQuery(params?: {
  keyword?: string;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: PRODUCT_KEYS.categories(params),
    queryFn: async () => {
      const res = await productApi.getCategories(params);
      return res.data;
    },
  });
}

export function useCategoryAttributesQuery(id: string) {
  return useQuery({
    queryKey: PRODUCT_KEYS.categoryAttributes(id),
    queryFn: async () => {
      const res = await productApi.getCategoryAttributes(id);
      return res.data;
    },
    enabled: !!id,
  });
}

export function useSimilarProductsQuery(id: string) {
  return useQuery({
    queryKey: PRODUCT_KEYS.similarProducts(id),
    queryFn: async () => {
      const res = await productApi.getSimilarProducts(id);
      return res.data;
    },
    enabled: !!id,
  });
}

export function useRecentlyViewedProductsQuery(enabled = true) {
  return useQuery({
    queryKey: PRODUCT_KEYS.recentlyViewed(),
    queryFn: async () => {
      const res = await productApi.getRecentlyViewed();
      return res.data;
    },
    enabled,
  });
}

export function useRecordProductViewMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (productId: string) => productApi.recordProductView(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.recentlyViewed() });
    },
  });
}


