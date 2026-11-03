import { resolveSellerSlug } from "@/features/shops";
import type {
  ProductSummaryResponse,
  PublicProductDetailResponse,
  Product,
} from "@/types/product";
import type {
  ProductDetailData,
  ProductVariant,
  AttributeOptionGroup,
  ProductSpec,
} from "@/types/productDetail";

export function mapProductSummaryToProduct(summary: ProductSummaryResponse): Product {
  const vendorSlug = resolveSellerSlug(summary.shopName);

  return {
    id: summary.id,
    slug: summary.slug,
    title: summary.name,
    price: Number(summary.price),
    vendorName: summary.shopName,
    vendorSlug,
    isVerifiedVendor: !summary.isShopPaused,
    imageUrl: summary.coverImageUrl || "/images/placeholder.png",
    rating: 5.0,
    reviewsCount: 0,
    inStock: summary.inStock,
  };
}

export function mapPublicDetailToProductDetailData(
  detail: PublicProductDetailResponse
): ProductDetailData {
  const images =
    detail.images && detail.images.length > 0
      ? detail.images
          .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
          .map((img) => img.imageUrl)
      : ["/images/placeholder.png"];

  const variants: ProductVariant[] = (detail.variants || []).map((v) => {
    const rawAttrs = (v.attributes as Record<string, string>) || {};
    return {
      id: v.id,
      sku: v.sku,
      name: v.variantName,
      attributes: rawAttrs,
      price: Number(v.price),
      originalPrice: Number(v.price),
      stock: v.stockQuantity,
      imageUrl: v.imageUrl || images[0],
      status: v.status === "INACTIVE" ? "INACTIVE" : "ACTIVE",
    };
  });

  // Extract attribute groups from variants
  const groupMap: Record<string, Set<string>> = {};
  variants.forEach((v) => {
    Object.entries(v.attributes).forEach(([k, val]) => {
      if (!groupMap[k]) groupMap[k] = new Set();
      groupMap[k].add(val);
    });
  });

  const attributeGroups: AttributeOptionGroup[] = Object.entries(groupMap).map(
    ([name, valuesSet]) => ({
      name,
      values: Array.from(valuesSet),
    })
  );

  // Extract specs
  const specs: ProductSpec[] = [];
  if (detail.brandName) {
    specs.push({ label: "Brand", value: detail.brandName });
  }
  if (detail.categoryName) {
    specs.push({ label: "Category", value: detail.categoryName });
  }
  if (detail.attributes) {
    Object.entries(detail.attributes).forEach(([k, val]) => {
      specs.push({ label: k, value: String(val) });
    });
  }

  // Parse description
  const descriptionParagraphs = detail.description
    ? detail.description.split("\n\n").filter(Boolean)
    : ["High performance hardware distributed through official TechHub marketplace merchants."];

  return {
    id: detail.id,
    slug: detail.slug,
    title: detail.name,
    brandName: detail.brandName || "TechHub Certified",
    categoryName: detail.categoryName || "Hardware",
    categorySlug: detail.categoryName ? detail.categoryName.toLowerCase().replace(/\s+/g, "-") : "all",
    rating: 5.0,
    reviewsCount: 0,
    soldCount: 0,
    images,
    variants: variants.length > 0 ? variants : [
      {
        id: detail.id,
        sku: "STD-DEFAULT",
        name: "Standard Edition",
        attributes: {},
        price: Number(detail.minPrice || 0),
        originalPrice: Number(detail.minPrice || 0),
        stock: detail.totalStock || 10,
        imageUrl: images[0],
        status: "ACTIVE",
      },
    ],
    attributeGroups,
    specs,
    merchant: {
      id: detail.shopId,
      name: detail.shopName,
      sellerName: resolveSellerSlug(detail.shopName),
      avatarUrl: detail.shopAvatarUrl || undefined,
      avatarText: (detail.shopName || "TH").substring(0, 2).toUpperCase(),
      joinedText: "Đối tác TechHub",
      rating: 5.0,
      positiveFeedback: "100%",
      totalProducts: 1,
      verified: true,
      status: detail.isShopPaused ? "PAUSED" : "ACTIVE",
    },
    description: {
      intro: descriptionParagraphs,
      features: [
        "Chính hãng 100%, bảo hành tiêu chuẩn từ nhà sản xuất",
        "Được kiểm định chất lượng và đóng gói an toàn bởi TechHub Verified Merchant",
        "Hỗ trợ đổi trả trong 30 ngày nếu phát hiện lỗi phần cứng",
      ],
      materials: [
        "Vật liệu và linh kiện cấu thành đạt chuẩn công nghiệp cao cấp",
      ],
      inTheBox: [
        `1x ${detail.name}`,
        "1x Hướng dẫn sử dụng & Thẻ bảo hành chính hãng",
      ],
    },
    reviewStats: {
      average: 5.0,
      totalReviews: 0,
      breakdown: [
        { star: 5, percentage: 100 },
        { star: 4, percentage: 0 },
        { star: 3, percentage: 0 },
        { star: 2, percentage: 0 },
        { star: 1, percentage: 0 },
      ],
    },
    reviews: [],
    recommendations: [],
  };
}
