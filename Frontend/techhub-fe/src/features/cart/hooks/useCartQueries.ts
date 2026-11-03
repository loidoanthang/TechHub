import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { cartApi } from "../api/cartApi";
import type {
  AddToCartRequest,
  UpdateCartItemRequest,
  CartPreviewRequest,
} from "@/types/cart";
import { useAuth } from "@/features/auth/hooks/useAuth";

export const CART_KEYS = {
  cart: ["cart"] as const,
  count: ["cart", "count"] as const,
};

export function useCartQuery() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: CART_KEYS.cart,
    queryFn: async () => {
      const res = await cartApi.getCart();
      return res.data;
    },
    enabled: isAuthenticated,
  });
}

export function useCartCountQuery() {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: CART_KEYS.count,
    queryFn: async () => {
      const res = await cartApi.getCartCount();
      return res.data;
    },
    enabled: isAuthenticated,
  });
}

export function useAddToCartMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AddToCartRequest) => cartApi.addToCart(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CART_KEYS.cart });
      queryClient.invalidateQueries({ queryKey: CART_KEYS.count });
    },
  });
}

export function useUpdateCartItemMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      itemId,
      data,
    }: {
      itemId: string;
      data: UpdateCartItemRequest;
    }) => cartApi.updateCartItem(itemId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CART_KEYS.cart });
      queryClient.invalidateQueries({ queryKey: CART_KEYS.count });
    },
  });
}

export function useDeleteCartItemMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: string) => cartApi.deleteCartItem(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CART_KEYS.cart });
      queryClient.invalidateQueries({ queryKey: CART_KEYS.count });
    },
  });
}

export function usePreviewCartMutation() {
  return useMutation({
    mutationFn: (data: CartPreviewRequest) => cartApi.previewCart(data),
  });
}
