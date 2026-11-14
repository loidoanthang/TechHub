import { cache } from "react";
import { productApi } from "@/features/products/api/productApi";
import { mapPublicDetailToProductDetailData } from "../utils/productMapper";
import type { ProductDetailData } from "@/types/productDetail";
import type { Metadata } from "next";

export type ResolveProductResult =
  | { type: "SUCCESS"; data: ProductDetailData }
  | { type: "REDIRECT"; targetSlug: string }
  | { type: "NOT_FOUND" };

/**
 * Centrally resolves a product detail request by slug directly against Backend API.
 * Wrapped in React cache() to deduplicate server fetches between generateMetadata and Page render.
 */
export const resolveProductDetail = cache(
  async (productIdOrSlug: string): Promise<ResolveProductResult> => {
    try {
      const res = await productApi.getProductBySlug(productIdOrSlug);
      if (res?.data) {
        if (res.data.slug && res.data.slug !== productIdOrSlug) {
          return {
            type: "REDIRECT",
            targetSlug: res.data.slug,
          };
        }
        return {
          type: "SUCCESS",
          data: mapPublicDetailToProductDetailData(res.data),
        };
      }
    } catch (err) {
      console.warn(`Product slug "${productIdOrSlug}" not found on backend:`, err);
    }

    // Not found in database -> Trigger 404
    return { type: "NOT_FOUND" };
  }
);

/**
 * Resolves SEO metadata cleanly without duplicating API logic.
 */
export async function resolveProductMetadata(
  productIdOrSlug: string
): Promise<Metadata> {
  const result = await resolveProductDetail(productIdOrSlug);

  if (result.type === "SUCCESS") {
    return {
      title: `${result.data.title} | TechHub Marketplace`,
      description:
        result.data.description?.intro?.[0] ||
        "High-performance tech hardware on TechHub Marketplace",
    };
  }

  return {
    title: "Không tìm thấy sản phẩm | TechHub Marketplace",
    description: "Sản phẩm bạn tìm kiếm không tồn tại hoặc đã ngừng kinh doanh trên TechHub.",
  };
}
