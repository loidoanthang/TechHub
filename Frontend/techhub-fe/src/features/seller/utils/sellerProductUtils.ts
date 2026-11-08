import type {
  ProductApprovalStatus,
  ProductVisibilityStatus,
} from "@/types/enums";

/**
 * Định dạng khoảng giá sản phẩm linh kiện
 * Nếu có nhiều biến thể và giá khác nhau: "22.490.000₫ - 33.990.000₫"
 * Nếu giá đồng nhất: "22.490.000₫"
 */
export function formatProductPriceRange(
  minPrice: number,
  maxPrice: number,
  variantsCount: number
): string {
  const fmt = (val: number) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(val);

  if (variantsCount > 1 && minPrice !== maxPrice && minPrice > 0 && maxPrice > 0) {
    return `${fmt(minPrice)} - ${fmt(maxPrice)}`;
  }
  return fmt(minPrice || maxPrice || 0);
}

/**
 * Kiểm tra điều kiện cho phép bật bán (PUBLISHED) theo Business Rules của Backend:
 * Theo SellerProductServiceImpl: Chỉ sản phẩm APPROVED và !isSuspended mới được PUBLISHED.
 */
export function canPublishProduct(
  approvalStatus: ProductApprovalStatus,
  isSuspended: boolean
): { canPublish: boolean; reason?: string } {
  if (isSuspended) {
    return {
      canPublish: false,
      reason: "Sản phẩm đang bị Ban Quản trị đình chỉ vi phạm.",
    };
  }
  if (approvalStatus !== "APPROVED") {
    return {
      canPublish: false,
      reason: "Chỉ sản phẩm đã được Ban Quản trị phê duyệt mới có thể bật hiển thị bán.",
    };
  }
  return { canPublish: true };
}

/**
 * Lấy nhãn và style màu sắc cho Trạng thái kiểm duyệt
 */
export function getApprovalStatusMeta(status: ProductApprovalStatus) {
  switch (status) {
    case "APPROVED":
      return {
        label: "Đã duyệt",
        badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
        dotClass: "bg-emerald-500",
      };
    case "PENDING_APPROVAL":
      return {
        label: "Chờ duyệt",
        badgeClass: "bg-blue-50 text-blue-700 border-blue-200",
        dotClass: "bg-blue-500",
      };
    case "REJECTED":
      return {
        label: "Bị từ chối",
        badgeClass: "bg-rose-50 text-rose-700 border-rose-200",
        dotClass: "bg-rose-500",
      };
    case "DRAFT":
    default:
      return {
        label: "Bản nháp",
        badgeClass: "bg-slate-100 text-slate-700 border-slate-200",
        dotClass: "bg-slate-400",
      };
  }
}

/**
 * Lấy nhãn và style màu sắc cho Trạng thái hiển thị
 */
export function getVisibilityStatusMeta(status: ProductVisibilityStatus) {
  switch (status) {
    case "PUBLISHED":
      return {
        label: "Đang mở bán",
        badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
      };
    case "UNPUBLISHED":
    default:
      return {
        label: "Đã ẩn",
        badgeClass: "bg-slate-100 text-slate-600 border-slate-200",
      };
  }
}
