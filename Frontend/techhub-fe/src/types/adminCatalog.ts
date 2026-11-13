import type {
  CategoryAttributeDataType,
  CategoryStatus,
  ProductApprovalStatus,
  ProductVariantStatus,
  ProductVisibilityStatus,
  ShopStatus,
} from "./enums";

// ==================== PRODUCT MODERATION ====================
export interface AdminProductImageDto {
  id: string;
  imageUrl: string;
  displayOrder: number;
}

export interface AdminProductVariantDto {
  id: string;
  sku: string;
  variantName: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string | null;
  attributes?: Record<string, unknown> | null;
  status: ProductVariantStatus;
}

export interface AdminProductSummaryResponse {
  id: string;
  shopId: string;
  shopName: string;
  sellerName: string;
  name: string;
  slug: string;
  brandName: string;
  categoryId: string;
  categoryName: string;
  coverImageUrl?: string | null;
  minPrice: number;
  maxPrice: number;
  totalStock: number;
  variantsCount: number;
  approvalStatus: ProductApprovalStatus;
  visibilityStatus: ProductVisibilityStatus;
  isSuspended: boolean;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminProductDetailResponse {
  id: string;
  shopId: string;
  shopName: string;
  sellerName: string;
  shopStatus: ShopStatus;
  categoryId: string;
  categoryName: string;
  categoryStatus: CategoryStatus;
  brandName: string;
  name: string;
  slug: string;
  description?: string | null;
  attributes?: Record<string, unknown> | null;
  approvalStatus: ProductApprovalStatus;
  visibilityStatus: ProductVisibilityStatus;
  rejectionReason?: string | null;
  isSuspended: boolean;
  isDeleted: boolean;
  images: AdminProductImageDto[];
  variants: AdminProductVariantDto[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProductApprovalRequest {
  status: ProductApprovalStatus;
  rejectionReason?: string;
}

export interface UpdateProductSuspendRequest {
  isSuspended: boolean;
}

export interface AdminProductsQueryParams {
  shopId?: string;
  categoryId?: string;
  keyword?: string;
  approvalStatus?: ProductApprovalStatus | "";
  visibilityStatus?: ProductVisibilityStatus | "";
  isSuspended?: boolean;
  isDeleted?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}

// ==================== CATEGORIES & ATTRIBUTES ====================
export interface CategoryAttributeResponse {
  id: string;
  name: string;
  dataType: CategoryAttributeDataType;
  options?: string[] | null;
  sampleValue?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CategorySummaryResponse {
  id: string;
  name: string;
  description?: string | null;
  status: CategoryStatus;
  attributesCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryDetailResponse {
  id: string;
  name: string;
  description?: string | null;
  status: CategoryStatus;
  attributes: CategoryAttributeResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryAttributeRequest {
  name: string;
  dataType: CategoryAttributeDataType;
  options?: string[];
  sampleValue?: string;
}

export interface UpdateCategoryAttributeRequest {
  name: string;
  dataType: CategoryAttributeDataType;
  options?: string[];
  sampleValue?: string;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  attributes: CreateCategoryAttributeRequest[];
}

export interface UpdateCategoryRequest {
  name: string;
  description?: string;
}

export interface UpdateCategoryStatusRequest {
  status: CategoryStatus;
}

export interface AdminCategoriesQueryParams {
  keyword?: string;
  status?: CategoryStatus | "";
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
}
