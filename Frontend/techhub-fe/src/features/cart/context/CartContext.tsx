"use client";

import React, {
  createContext,
  useContext,
  useState,
  useMemo,
  useCallback,
  useEffect,
} from "react";
import { useRouter } from "next/navigation";
import type { AxiosError } from "axios";
import {
  useCartQuery,
  useCartCountQuery,
  useAddToCartMutation,
  useUpdateCartItemMutation,
  useDeleteCartItemMutation,
} from "../hooks/useCartQueries";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { CartItem } from "@/mocks";
import type { CartResponse } from "@/types/cart";

export interface AddToCartPayload {
  variantId: string;
  quantity?: number;
  title?: string;
  price?: number;
  imageUrl?: string;
  specs?: string;
  storeName?: string;
  storeId?: string;
}

export type AddToCartInput = string | AddToCartPayload;

export interface CartContextType {
  isOpen: boolean;
  openCart: () => void;
  closeCart: () => void;
  toggleCart: () => void;
  items: CartItem[];
  rawCart?: CartResponse;
  updateQuantity: (id: string, delta: number) => Promise<void>;
  removeItem: (id: string) => Promise<void>;
  addToCart: (input: AddToCartInput, quantity?: number) => Promise<void>;
  subtotal: number;
  totalCount: number;
  isLoading: boolean;
  isAddingToCart: boolean;
  error: string | null;
  clearError: () => void;
  refetchCart: () => void;
}

const LOCAL_CART_STORAGE_KEY = "techhub_cart_local_v1";

const isUuid = (val: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val);

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  const [isOpen, setIsOpen] = useState(false);
  const [localItems, setLocalItems] = useState<CartItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Initialize local items from localStorage safely in browser
  useEffect(() => {
    try {
      const saved = localStorage.getItem(LOCAL_CART_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          setLocalItems(parsed);
        }
      }
    } catch {
      // Ignore parse error
    }
  }, []);

  // Save local items changes to localStorage
  const saveLocalItems = useCallback((newItems: CartItem[]) => {
    setLocalItems(newItems);
    try {
      localStorage.setItem(LOCAL_CART_STORAGE_KEY, JSON.stringify(newItems));
    } catch {
      // Ignore storage error
    }
  }, []);

  // TanStack Query server state (only active when user is logged in)
  const cartQuery = useCartQuery();
  const cartCountQuery = useCartCountQuery();
  const addToCartMutation = useAddToCartMutation();
  const updateCartItemMutation = useUpdateCartItemMutation();
  const deleteCartItemMutation = useDeleteCartItemMutation();

  const openCart = useCallback(() => setIsOpen(true), []);
  const closeCart = useCallback(() => setIsOpen(false), []);
  const toggleCart = useCallback(() => setIsOpen((prev) => !prev), []);
  const clearError = useCallback(() => setError(null), []);

  // Map server response to UI normalized CartItem[]
  const serverItems = useMemo<CartItem[]>(() => {
    if (!cartQuery.data?.shops) return [];
    return cartQuery.data.shops.flatMap((shop) =>
      shop.items.map((item) => ({
        id: item.id,
        storeId: shop.shopId,
        storeName: shop.shopName,
        title: item.productName,
        specs:
          item.variantName ||
          (item.variantSku ? `SKU: ${item.variantSku}` : "Standard"),
        price: Number(item.price),
        quantity: item.quantity,
        imageUrl: item.imageUrl || "/images/placeholder.png",
        stockQuantity: item.stockQuantity,
        isOutOfStock: item.isOutOfStock,
        isAvailable: item.isAvailable,
        unavailableReason: item.unavailableReason,
        variantId: item.variantId,
        productId: item.productId,
      }))
    );
  }, [cartQuery.data]);

  // Combined items: server items take priority, merged with any mock/local items
  const items = useMemo(() => {
    if (!isAuthenticated) return localItems;
    if (serverItems.length === 0) return localItems;
    const serverItemIds = new Set(serverItems.map((i) => i.id));
    const extraLocal = localItems.filter((li) => !serverItemIds.has(li.id));
    return [...serverItems, ...extraLocal];
  }, [isAuthenticated, serverItems, localItems]);

  const totalCount = useMemo(() => {
    if (isAuthenticated) {
      if (typeof cartCountQuery.data?.totalQuantity === "number") {
        return cartCountQuery.data.totalQuantity;
      }
      if (typeof cartQuery.data?.totalQuantity === "number") {
        return cartQuery.data.totalQuantity;
      }
    }
    return items.reduce((sum, item) => sum + item.quantity, 0);
  }, [isAuthenticated, cartCountQuery.data, cartQuery.data, items]);

  const subtotal = useMemo(() => {
    return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }, [items]);

  const addToCart = useCallback(
    async (input: AddToCartInput, explicitQty?: number) => {
      setError(null);

      const payload: AddToCartPayload =
        typeof input === "string"
          ? { variantId: input, quantity: explicitQty ?? 1 }
          : { ...input, quantity: input.quantity ?? explicitQty ?? 1 };

      const qty = Math.max(1, payload.quantity ?? 1);
      const variantId = payload.variantId;

      // Helper to add to local items (for mock testing or guest fallback)
      const addToLocal = () => {
        const fallbackItem: CartItem = {
          id: variantId || `cart-item-${Date.now()}`,
          variantId,
          storeId: payload.storeId || "store-default",
          storeName: payload.storeName || "TechHub Verified Store",
          title: payload.title || "Selected Hardware Item",
          specs: payload.specs || "Standard Edition",
          price: payload.price ?? 0,
          quantity: qty,
          imageUrl: payload.imageUrl || "/images/placeholder.png",
        };

        const existingIndex = localItems.findIndex(
          (i) => i.id === fallbackItem.id || (i.variantId && i.variantId === variantId)
        );

        let next: CartItem[];
        if (existingIndex > -1) {
          next = localItems.map((item, idx) =>
            idx === existingIndex
              ? { ...item, quantity: item.quantity + qty }
              : item
          );
        } else {
          next = [fallbackItem, ...localItems];
        }

        saveLocalItems(next);
      };

      // Case 1: Authenticated AND valid UUID -> sync with Backend API
      if (isAuthenticated && isUuid(variantId)) {
        try {
          await addToCartMutation.mutateAsync({
            productVariantId: variantId,
            quantity: qty,
          });
          return;
        } catch (err: unknown) {
          // If Backend doesn't have this variant in DB (e.g. 404), fallback to local cart
          console.warn("Backend cart sync error, falling back to client cart:", err);
          addToLocal();
          return;
        }
      }

      // Case 2: Guest or mock variant ID -> seamlessly store in local cart
      addToLocal();
    },
    [isAuthenticated, localItems, addToCartMutation, saveLocalItems]
  );

  const updateQuantity = useCallback(
    async (id: string, delta: number) => {
      const currentItem = items.find((i) => i.id === id);
      if (!currentItem) return;

      const nextQty = currentItem.quantity + delta;

      // If it's a server item (UUID and authenticated), update via BE
      if (isAuthenticated && isUuid(id)) {
        setError(null);
        try {
          if (nextQty <= 0) {
            await deleteCartItemMutation.mutateAsync(id);
          } else {
            await updateCartItemMutation.mutateAsync({
              itemId: id,
              data: { quantity: nextQty },
            });
          }
          return;
        } catch (err: unknown) {
          const axiosErr = err as AxiosError<{ message?: string }>;
          const msg =
            axiosErr.response?.data?.message ||
            "Không thể cập nhật số lượng sản phẩm.";
          setError(msg);
        }
      }

      // Local update
      const updated = localItems
        .map((item) => {
          if (item.id === id) {
            return nextQty > 0 ? { ...item, quantity: nextQty } : null;
          }
          return item;
        })
        .filter(Boolean) as CartItem[];

      saveLocalItems(updated);
    },
    [items, isAuthenticated, deleteCartItemMutation, updateCartItemMutation, localItems, saveLocalItems]
  );

  const removeItem = useCallback(
    async (id: string) => {
      if (isAuthenticated && isUuid(id)) {
        setError(null);
        try {
          await deleteCartItemMutation.mutateAsync(id);
          return;
        } catch (err: unknown) {
          const axiosErr = err as AxiosError<{ message?: string }>;
          const msg =
            axiosErr.response?.data?.message || "Không thể xóa sản phẩm khỏi giỏ.";
          setError(msg);
        }
      }

      const updated = localItems.filter((item) => item.id !== id);
      saveLocalItems(updated);
    },
    [isAuthenticated, deleteCartItemMutation, localItems, saveLocalItems]
  );

  const isLoading =
    cartQuery.isLoading ||
    updateCartItemMutation.isPending ||
    deleteCartItemMutation.isPending;

  return (
    <CartContext.Provider
      value={{
        isOpen,
        openCart,
        closeCart,
        toggleCart,
        items,
        rawCart: cartQuery.data,
        updateQuantity,
        removeItem,
        addToCart,
        subtotal,
        totalCount,
        isLoading,
        isAddingToCart: addToCartMutation.isPending,
        error,
        clearError,
        refetchCart: () => {
          cartQuery.refetch();
          cartCountQuery.refetch();
        },
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return context;
}
