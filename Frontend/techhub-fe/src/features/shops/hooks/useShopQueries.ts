import { useQuery } from "@tanstack/react-query";
import { shopApi, type ShopProductsQueryParams } from "../api/shopApi";

export const SHOP_KEYS = {
  all: ["shops"] as const,
  detail: (sellerName: string) => ["shops", "detail", sellerName] as const,
  vouchers: (sellerName: string) => ["shops", "vouchers", sellerName] as const,
  products: (shopId?: string, shopName?: string, params?: ShopProductsQueryParams) =>
    ["shops", "products", shopId, shopName, params] as const,
};

export function useShopProfileQuery(sellerName: string) {
  return useQuery({
    queryKey: SHOP_KEYS.detail(sellerName),
    queryFn: async () => {
      const res = await shopApi.getPublicShopProfile(sellerName);
      return res.data;
    },
    enabled: Boolean(sellerName),
    staleTime: 60 * 1000,
  });
}

export function useShopVouchersQuery(sellerName: string, orderAmount?: number) {
  return useQuery({
    queryKey: SHOP_KEYS.vouchers(sellerName),
    queryFn: async () => {
      const res = await shopApi.getShopVouchers(sellerName, orderAmount);
      return res.data;
    },
    enabled: Boolean(sellerName),
    staleTime: 60 * 1000,
  });
}

export function useShopProductsQuery(
  shopId?: string,
  shopName?: string,
  params?: ShopProductsQueryParams
) {
  return useQuery({
    queryKey: SHOP_KEYS.products(shopId, shopName, params),
    queryFn: async () => {
      const res = await shopApi.getShopProducts(params);
      const page = res.data;
      if (!page) return { content: [], totalElements: 0, totalPages: 0 };

      // Resilient filtering by shopId OR shopName
      const normalizedShopId = shopId?.trim().toLowerCase();
      const normalizedShopName = shopName?.trim().toLowerCase();

      const filteredContent = page.content.filter((p) => {
        if (normalizedShopId && p.shopId?.toLowerCase() === normalizedShopId) {
          return true;
        }
        if (normalizedShopName && p.shopName?.toLowerCase() === normalizedShopName) {
          return true;
        }
        return !normalizedShopId && !normalizedShopName;
      });

      return {
        ...page,
        content: filteredContent,
        totalElements: filteredContent.length,
      };
    },
    enabled: Boolean(shopId || shopName),
    staleTime: 60 * 1000,
  });
}
