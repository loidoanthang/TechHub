import { useQuery } from "@tanstack/react-query";
import { productApi, type GetProductsParams } from "../api/productApi";

export const PRODUCT_KEYS = {
  products: (params?: GetProductsParams) => ["products", params] as const,
  productDetail: (slug: string) => ["products", "detail", slug] as const,
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
