import type { ProductApprovalStatus, ProductVisibilityStatus } from "./enums";

/**
 * Phản hồi danh sách tóm tắt sản phẩm trong Seller Centre (khớp 100% Backend SellerProductSummaryResponse.java)
 */
export interface SellerProductSummaryResponse {
  id: string;
  name: string;
  slug: string;
  brandName: string | null;
  categoryId: string;
  categoryName: string;
  coverImageUrl: string | null;
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

/**
 * 4 trường sắp xếp Backend hỗ trợ thực tế
 */
export type SellerProductSortBy = "createdAt" | "updatedAt" | "name" | "brandName";
export type SortDirection = "asc" | "desc";

/**
 * Tham số lọc danh sách sản phẩm Seller
 */
export interface SellerProductQueryParams {
  keyword?: string;
  categoryId?: string;
  approvalStatus?: ProductApprovalStatus;
  visibilityStatus?: ProductVisibilityStatus;
  isSuspended?: boolean;
  isDeleted?: boolean;
  page?: number;
  size?: number;
  sortBy?: SellerProductSortBy;
  sortDir?: SortDirection;
}

/**
 * Request cập nhật trạng thái hiển thị
 */
export interface UpdateProductVisibilityRequest {
  visibilityStatus: ProductVisibilityStatus;
}

/**
 * Tab lọc trạng thái UI
 */
export type SellerProductTab =
  | "ALL"
  | "PUBLISHED"
  | "PENDING_APPROVAL"
  | "DRAFT_REJECTED"
  | "UNPUBLISHED"
  | "SUSPENDED";

/**
 * Kiểu dữ liệu thuộc tính danh mục
 */
export type CategoryAttributeDataType = "TEXT" | "NUMBER" | "BOOLEAN" | "SELECT";

/**
 * Định nghĩa thuộc tính kỹ thuật động của danh mục (khớp CategoryAttributeResponse.java)
 */
export interface CategoryAttributeResponse {
  id: string;
  name: string;
  dataType: CategoryAttributeDataType;
  options?: string[] | null;
  sampleValue?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Request ảnh sản phẩm (1-6 ảnh, duy nhất 1 ảnh có displayOrder = 0 làm ảnh bìa)
 */
export interface CreateProductImageRequest {
  imageUrl: string;
  displayOrder: number;
}

/**
 * Request biến thể sản phẩm (1-50 variants, mã SKU duy nhất)
 */
export interface CreateProductVariantRequest {
  sku: string;
  variantName: string;
  description?: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  attributes?: Record<string, unknown>;
}

/**
 * Request tạo mới sản phẩm của người bán (POST /api/seller/products)
 */
export interface CreateProductRequest {
  categoryId: string;
  name: string;
  brandName: string;
  description?: string;
  attributes: Record<string, unknown>;
  images: CreateProductImageRequest[];
  variants: CreateProductVariantRequest[];
}

/**
 * Phản hồi chi tiết sản phẩm tạo mới / chỉnh sửa
 */
export interface ProductDetailResponse {
  id: string;
  shopId: string;
  categoryId: string;
  categoryName: string;
  brandName: string | null;
  name: string;
  slug: string;
  description: string | null;
  attributes: Record<string, unknown>;
  approvalStatus: ProductApprovalStatus;
  visibilityStatus: ProductVisibilityStatus;
  isSuspended: boolean;
  isDeleted: boolean;
  images: Array<{
    id: string;
    imageUrl: string;
    displayOrder: number;
  }>;
  variants: Array<{
    id: string;
    sku: string;
    variantName: string;
    price: number;
    stockQuantity: number;
    imageUrl?: string | null;
  }>;
  createdAt: string;
  updatedAt: string;
}
