import type { PageResponse } from "@/types/api";

export interface WishlistResponse {
  id: string; // WishlistItem UUID
  productId: string;
  productName: string;
  slug: string;
  coverImageUrl: string;
  minPrice: number;
  inStock: boolean;
  isShopPaused: boolean;
  createdAt: string;
}

export type WishlistPageResponse = PageResponse<WishlistResponse>;
