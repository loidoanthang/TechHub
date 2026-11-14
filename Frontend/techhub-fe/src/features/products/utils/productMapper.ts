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
  return {
    id: summary.id,
    slug: summary.slug,
    title: summary.name,
    price: Number(summary.price),
    vendorName: summary.shopName,
    vendorSlug: summary.shopName,
    isVerifiedVendor: !summary.isShopPaused,
    imageUrl: summary.coverImageUrl || "",
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
      : [];

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
    specs.push({ label: "Thương hiệu", value: detail.brandName });
  }
  if (detail.categoryName) {
    specs.push({ label: "Danh mục", value: detail.categoryName });
  }
  if (detail.attributes) {
    Object.entries(detail.attributes).forEach(([k, val]) => {
      specs.push({ label: k, value: String(val) });
    });
  }

  // Parse description
  const descriptionParagraphs = detail.description
    ? detail.description.split("\n\n").filter(Boolean)
    : ["Thông tin chi tiết sản phẩm đang được cập nhật."];

  return {
    id: detail.id,
    slug: detail.slug,
    title: detail.name,
    brandName: detail.brandName || undefined,
    categoryName: detail.categoryName || "Thiết bị",
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
      sellerName: detail.sellerName || detail.shopName,
      sellerUserId: detail.sellerUserId ?? undefined,
      avatarUrl: detail.shopAvatarUrl || undefined,
      avatarText: (detail.shopName || "TH").substring(0, 2).toUpperCase(),
      joinedText: "Đối tác TechHub",
      rating: 5.0,
      positiveFeedback: "100%",
      totalProducts: 1,
      verified: !detail.isShopPaused,
      status: detail.isShopPaused ? "PAUSED" : "ACTIVE",
    },
    description: {
      intro: descriptionParagraphs,
      features: [],
      materials: [],
      inTheBox: [],
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
