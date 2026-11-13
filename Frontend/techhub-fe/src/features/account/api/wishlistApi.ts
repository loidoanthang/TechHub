import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/constants";
import { getAccessToken } from "@/lib/api/token";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { ApiResponse } from "@/types/api";
import type { WishlistResponse, WishlistPageResponse } from "@/types/wishlist";
import { toast } from "@/lib/toast";

export const wishlistApi = {
  getWishlist: async (
    page: number = 0,
    size: number = 10
  ): Promise<ApiResponse<WishlistPageResponse>> => {
    return apiClient.get(ENDPOINTS.WISHLIST.BASE, {
      params: { page, size },
    });
  },

  addToWishlist: async (productId: string): Promise<ApiResponse<WishlistResponse>> => {
    return apiClient.post(ENDPOINTS.WISHLIST.ITEM(productId));
  },

  removeFromWishlist: async (productId: string): Promise<ApiResponse<void>> => {
    return apiClient.delete(ENDPOINTS.WISHLIST.ITEM(productId));
  },
};

export const WISHLIST_KEYS = {
  all: ["wishlist"] as const,
  list: (page: number, size: number) => ["wishlist", "list", page, size] as const,
  ids: ["wishlist", "ids"] as const,
};

export function useWishlistQuery(page: number = 0, size: number = 10) {
  const token = typeof window !== "undefined" ? getAccessToken() : null;

  return useQuery({
    queryKey: WISHLIST_KEYS.list(page, size),
    queryFn: async () => {
      const res = await wishlistApi.getWishlist(page, size);
      return res.data;
    },
    enabled: !!token,
    staleTime: 30 * 1000,
  });
}

/**
 * Hook query lấy tất cả product IDs trong Wishlist (tối đa 20 món theo BR-05B).
 * Dùng để hiển thị trạng thái tim đỏ nhanh trên toàn sàn mà không cần gọi API riêng cho từng card.
 */
export function useWishlistIdsQuery() {
  const token = typeof window !== "undefined" ? getAccessToken() : null;

  return useQuery({
    queryKey: WISHLIST_KEYS.ids,
    queryFn: async () => {
      const res = await wishlistApi.getWishlist(0, 50);
      const items: WishlistResponse[] = res.data?.content || [];
      const idSet = new Set<string>();
      items.forEach((item: WishlistResponse) => {
        if (item.productId) idSet.add(item.productId);
      });
      return {
        ids: idSet,
        totalCount: res.data?.totalElements || items.length,
        items,
      };
    },
    enabled: !!token,
    staleTime: 60 * 1000,
  });
}

export function useAddToWishlistMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => wishlistApi.addToWishlist(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_KEYS.all });
      toast.success("Đã thêm vào danh sách yêu thích");
    },
    onError: (err: any) => {
      const errorCode = err?.response?.data?.metadata?.errorCode;
      if (errorCode === "WISHLIST_LIMIT_EXCEEDED") {
        toast.error("Danh sách yêu thích đã đạt tối đa 20 sản phẩm (BR-05B)");
      } else {
        toast.error(err?.response?.data?.message || "Không thể thêm vào yêu thích");
      }
    },
  });
}

export function useRemoveFromWishlistMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (productId: string) => wishlistApi.removeFromWishlist(productId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: WISHLIST_KEYS.all });
      toast.success("Đã xóa khỏi danh sách yêu thích");
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Không thể xóa khỏi yêu thích");
    },
  });
}

/**
 * Consolidated hook to toggle wishlist status with auth check & optimistic state
 */
export function useToggleWishlist(productId?: string) {
  const { isAuthenticated } = useAuth();
  const { data: wishlistData } = useWishlistIdsQuery();
  const addMutation = useAddToWishlistMutation();
  const removeMutation = useRemoveFromWishlistMutation();

  const isWishlisted = !!(productId && wishlistData?.ids.has(productId));
  const isPending = addMutation.isPending || removeMutation.isPending;

  const toggleWishlist = (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }

    if (!isAuthenticated) {
      toast.info("Vui lòng đăng nhập để lưu sản phẩm vào danh sách yêu thích");
      return;
    }

    if (!productId) return;

    if (isWishlisted) {
      removeMutation.mutate(productId);
    } else {
      addMutation.mutate(productId);
    }
  };

  return {
    isWishlisted,
    toggleWishlist,
    isPending,
  };
}
