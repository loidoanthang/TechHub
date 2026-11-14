import { ProductVariantStatus } from "./enums";

export interface ProductSummaryResponse {
  id: string;
  name: string;
  slug: string;
  brandName?: string | null;
  categoryId: string;
  categoryName: string;
  shopId: string;
  shopName: string;
  isShopPaused: boolean;
  coverImageUrl: string;
  price: number;
  inStock: boolean;
  createdAt: string;
}

export interface ProductImageResponse {
  id: string;
  imageUrl: string;
  displayOrder: number;
  isCover: boolean;
}

export interface ProductVariantResponse {
  id: string;
  sku: string;
  variantName: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string | null;
  attributes?: Record<string, unknown>;
  status: ProductVariantStatus;
}

export interface PublicProductDetailResponse {
  id: string;
  name: string;
  slug: string;
  brandName?: string | null;
  description?: string | null;
  categoryId: string;
  categoryName: string;
  shopId: string;
  shopName: string;
  sellerName?: string | null;
  shopAvatarUrl?: string | null;
  isShopPaused: boolean;
  attributes?: Record<string, unknown>;
  images: ProductImageResponse[];
  variants: ProductVariantResponse[];
  defaultVariantId?: string | null;
  minPrice: number;
  maxPrice: number;
  totalStock: number;
  inStock: boolean;
  createdAt: string;
  //sellerid - for chat
  sellerUserId?: string | null;
}

// UI product model
export interface Product {
  id: string;
  title: string;
  slug?: string;
  price: number;
  originalPrice?: number;
  discountPercentage?: number;
  rating: number;
  reviewsCount: number;
  vendorName: string;
  vendorSlug?: string;
  isVerifiedVendor?: boolean;
  imageUrl: string;
  tags?: string[];
  inStock?: boolean;
}
