import type { PageResponse } from "@/types/api";

export interface FollowShopResponse {
  shopId: string;
  shopName: string;
  sellerName: string;
  followedAt: string;
  followerCount: number;
  following: boolean;
}

export interface FollowedShopResponse {
  shopId: string;
  sellerName: string;
  shopName: string;
  avatarUrl: string | null;
  bannerUrl: string | null;
  status: "ACTIVE" | "PAUSED" | "SUSPENDED" | "BANNED";
  isPaused: boolean;
  followerCount: number;
  totalProducts: number;
  followedAt: string;
}

export type FollowedShopPageResponse = PageResponse<FollowedShopResponse>;
