import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import { getAccessToken } from "@/lib/api/token";
import type { ApiResponse } from "@/types/api";
import type {
  FollowShopResponse,
  FollowedShopResponse,
  FollowedShopPageResponse,
} from "@/types/shopFollowing";
import { toast } from "@/lib/toast";

export const shopFollowingApi = {
  followShop: async (sellerName: string): Promise<ApiResponse<FollowShopResponse>> => {
    return apiClient.post(ENDPOINTS.SHOPS.FOLLOW(sellerName));
  },

  unfollowShop: async (sellerName: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.SHOPS.FOLLOW(sellerName));
  },

  getFollowedShops: async (
    page: number = 0,
    size: number = 10
  ): Promise<ApiResponse<FollowedShopPageResponse>> => {
    return apiClient.get(ENDPOINTS.BUYER_FOLLOWED_SHOPS, {
      params: { page, size },
    });
  },
};

export const SHOP_FOLLOWING_KEYS = {
  all: ["shop-following"] as const,
  list: (page: number, size: number) => ["shop-following", "list", page, size] as const,
  followedIds: ["shop-following", "followed-slugs"] as const,
};

export function useFollowedShopsQuery(page: number = 0, size: number = 10) {
  const token = typeof window !== "undefined" ? getAccessToken() : null;

  return useQuery({
    queryKey: SHOP_FOLLOWING_KEYS.list(page, size),
    queryFn: async () => {
      const res = await shopFollowingApi.getFollowedShops(page, size);
      return res.data;
    },
    enabled: !!token,
    staleTime: 30 * 1000,
  });
}

/**
 * Hook query tập hợp tất cả sellerName/shopId mà Buyer đang follow
 * để hiển thị trạng thái nút "Đang theo dõi" trên Shop Banner hoặc thẻ sản phẩm.
 */
export function useFollowedShopSlugsQuery() {
  const token = typeof window !== "undefined" ? getAccessToken() : null;

  return useQuery({
    queryKey: SHOP_FOLLOWING_KEYS.followedIds,
    queryFn: async () => {
      const res = await shopFollowingApi.getFollowedShops(0, 100);
      const items: FollowedShopResponse[] = res.data?.content || [];
      const slugSet = new Set<string>();
      items.forEach((item: FollowedShopResponse) => {
        if (item.sellerName) slugSet.add(item.sellerName.toLowerCase());
      });
      return {
        slugs: slugSet,
        totalCount: res.data?.totalElements || items.length,
        items,
      };
    },
    enabled: !!token,
    staleTime: 60 * 1000,
  });
}

export function useFollowShopMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sellerName: string) => shopFollowingApi.followShop(sellerName),
    onSuccess: (res, sellerName) => {
      queryClient.invalidateQueries({ queryKey: SHOP_FOLLOWING_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ["shop", sellerName] });
      toast.success(res?.message || `Đã theo dõi gian hàng`);
    },
    onError: (err: any) => {
      const errorCode = err?.response?.data?.metadata?.errorCode;
      if (errorCode === "CANNOT_FOLLOW_OWN_SHOP") {
        toast.error("Bạn không thể tự theo dõi gian hàng của chính mình (BR-23)");
      } else {
        toast.error(err?.response?.data?.message || "Không thể theo dõi gian hàng");
      }
    },
  });
}

export function useUnfollowShopMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (sellerName: string) => shopFollowingApi.unfollowShop(sellerName),
    onSuccess: (_res, sellerName) => {
      queryClient.invalidateQueries({ queryKey: SHOP_FOLLOWING_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ["shop", sellerName] });
      toast.success("Đã bỏ theo dõi gian hàng");
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Không thể bỏ theo dõi gian hàng");
    },
  });
}
