"use client";

import React, { useState, useMemo, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import {
  Star,
  ShieldCheck,
  Heart,
  Share2,
  Check,
  Plus,
  Minus,
  ShoppingCart,
  Download,
  Building2,
  MessageCircle,
  ChevronRight,
  ThumbsUp,
  MessageSquare,
  AlertTriangle,
  Loader2,
  X,
  Ticket,
  MapPin,
  Store,
  ImageIcon,
} from "lucide-react";
import { ProductCard } from "@/components/shared";
import { Pagination } from "@/components/shared/Pagination";
import { useCart } from "@/features/cart/context/CartContext";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useToggleWishlist } from "@/features/account/api/wishlistApi";
import { toast } from "@/lib/toast";
import { useProductsQuery } from "@/features/products/hooks/useProductQueries";
import { mapProductSummaryToProduct } from "@/features/products/utils/productMapper";
import {
  useProductRatingSummaryQuery,
  useProductReviewsQuery,
  useToggleHelpfulVoteMutation,
} from "@/features/products/api/reviewApi";
import {
  useShopProfileQuery,
  useShopVouchersQuery,
  useShopProductsQuery,
  resolveSellerSlug,
} from "@/features/shops";
import { ROUTES } from "@/lib/constants";
import type { ProductDetailData, ProductVariant } from "@/types/productDetail";

interface ProductDetailViewProps {
  product: ProductDetailData;
}

export function ProductDetailView({ product }: ProductDetailViewProps) {
  const [selectedImg, setSelectedImg] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [selectedStarFilter, setSelectedStarFilter] = useState<number | null>(null);
  const [onlyMediaFilter, setOnlyMediaFilter] = useState<boolean>(false);
  const [reviewSortBy, setReviewSortBy] = useState<"createdAt" | "rating">("createdAt");
  const [reviewPage, setReviewPage] = useState<number>(0);
  const [previewMediaUrl, setPreviewMediaUrl] = useState<string | null>(null);

  // Real Reviews & Rating Summary Queries
  const { data: ratingSummaryRes } = useProductRatingSummaryQuery(product.id);
  const { data: reviewsRes, isLoading: isReviewsLoading } = useProductReviewsQuery(
    product.id,
    {
      rating: selectedStarFilter || undefined,
      hasMedia: onlyMediaFilter || undefined,
      sortBy: reviewSortBy,
      sortDirection: "DESC",
      page: reviewPage,
      size: 5,
    }
  );
  const toggleHelpfulMutation = useToggleHelpfulVoteMutation(product.id);

  // Real Wishlist Integration
  const { isWishlisted, toggleWishlist, isPending: isWishlistPending } = useToggleWishlist(product.id);

  const ratingSummary = ratingSummaryRes?.data;
  const avgRating = ratingSummary?.averageRating ?? product.rating ?? 5.0;
  const totalReviewsCount = ratingSummary?.totalReviews ?? product.reviewsCount ?? 0;
  const mediaReviewsCount = ratingSummary?.mediaReviewCount ?? 0;

  const realReviews = reviewsRes?.data?.content || [];
  const reviewTotalPages = reviewsRes?.data?.totalPages || 1;
  const reviewTotalElements = reviewsRes?.data?.totalElements ?? totalReviewsCount;

  const ratingBreakdown = useMemo(() => {
    return [5, 4, 3, 2, 1].map((star) => {
      const count = ratingSummary?.ratingCounts?.[star] ?? 0;
      const percentage =
        totalReviewsCount > 0
          ? Math.round((count / totalReviewsCount) * 100)
          : 0;
      return { star, count, percentage };
    });
  }, [ratingSummary, totalReviewsCount]);
  const router = useRouter();
  const { openCart, addToCart, isAddingToCart } = useCart();
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [isBuyNowPending, setIsBuyNowPending] = useState(false);
  const [cartToast, setCartToast] = useState<{
    message: string;
    itemInfo?: string;
  } | null>(null);

  // Query real related products from catalog
  const { data: relatedPage } = useProductsQuery({
    size: 4,
    sortBy: "createdAt",
  });

  const realRelatedProducts = useMemo(() => {
    if (!relatedPage?.content) return [];
    return relatedPage.content
      .filter((p) => p.id !== product.id)
      .slice(0, 3)
      .map(mapProductSummaryToProduct);
  }, [relatedPage, product.id]);
  const toastTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }
    };
  }, []);

  // Real Shop Data Integration
  const shopSellerName = useMemo(() => {
    return resolveSellerSlug(product.merchant.sellerName || product.merchant.name);
  }, [product.merchant.sellerName, product.merchant.name]);

  const { data: realShop } = useShopProfileQuery(shopSellerName);
  const { data: shopVouchers } = useShopVouchersQuery(shopSellerName);
  const displayShopName = realShop?.name || product.merchant.name;
  const { data: shopProductsData } = useShopProductsQuery(
    product.merchant.id || realShop?.id || "",
    displayShopName
  );
  const displayAvatarUrl = realShop?.avatarUrl || product.merchant.avatarUrl;
  const displayWarehouse = realShop?.warehouseProvince
    ? `${realShop.warehouseDistrict ? realShop.warehouseDistrict + ", " : ""}${realShop.warehouseProvince}`
    : null;
  const displayJoined = useMemo(() => {
    if (realShop?.createdAt) {
      const d = new Date(realShop.createdAt);
      return `Tham gia từ ${d.toLocaleDateString("vi-VN", { month: "2-digit", year: "numeric" })}`;
    }
    return product.merchant.joinedText || "Đối tác TechHub";
  }, [realShop?.createdAt, product.merchant.joinedText]);

  const totalShopProducts = shopProductsData?.totalElements ?? product.merchant.totalProducts;
  const isShopPaused = realShop
    ? realShop.status === "PAUSED"
    : product.merchant.status === "PAUSED";

  // Multi-Attribute Selection State
  // Extract initial attributes from default variant or first variant
  const initialVariant = product.variants[0];
  const [selectedAttributes, setSelectedAttributes] = useState<Record<string, string>>(
    initialVariant?.attributes || {}
  );

  // Group attributes definition (e.g. Colorway, Switch Type)
  const attributeGroups = useMemo(() => {
    if (product.attributeGroups && product.attributeGroups.length > 0) {
      return product.attributeGroups;
    }
    // Fallback: deduce from variants
    const groupMap: Record<string, Set<string>> = {};
    product.variants.forEach((v) => {
      if (v.attributes) {
        Object.entries(v.attributes).forEach(([k, val]) => {
          if (!groupMap[k]) groupMap[k] = new Set();
          groupMap[k].add(val);
        });
      }
    });
    return Object.entries(groupMap).map(([name, set]) => ({
      name,
      values: Array.from(set),
    }));
  }, [product]);

  // Find exact matched variant based on active attribute choices
  const matchedVariant: ProductVariant | undefined = useMemo(() => {
    return product.variants.find((v) => {
      if (!v.attributes) return false;
      return Object.entries(selectedAttributes).every(
        ([key, val]) => v.attributes[key] === val
      );
    });
  }, [product.variants, selectedAttributes]);

  // Fallback variant if no exact match (or initial)
  const activeVariant: ProductVariant = matchedVariant || initialVariant;

  // Cross-Attribute validation algorithm: Check if an attribute value is available/in-stock
  const isOptionAvailable = (groupName: string, value: string) => {
    // Check if there is at least one active variant with stock > 0 matching this choice
    const hypothetical = { ...selectedAttributes, [groupName]: value };
    return product.variants.some((v) => {
      if (!v.attributes || v.status === "INACTIVE") return false;
      // Match current option
      if (v.attributes[groupName] !== value) return false;
      // Match other selected options if possible
      const matchesOthers = Object.entries(hypothetical).every(
        ([k, val]) => k === groupName || v.attributes[k] === val
      );
      return matchesOthers && v.stock > 0;
    });
  };

  const handleSelectAttribute = (groupName: string, value: string) => {
    const nextAttributes = { ...selectedAttributes, [groupName]: value };
    setSelectedAttributes(nextAttributes);

    // If new combination has a variant with specific imageUrl, switch preview
    const found = product.variants.find((v) => {
      if (!v.attributes) return false;
      return Object.entries(nextAttributes).every(
        ([k, val]) => v.attributes[k] === val
      );
    });

    if (found && found.imageUrl) {
      const imgIdx = product.images.indexOf(found.imageUrl);
      if (imgIdx !== -1) {
        setSelectedImg(imgIdx);
      }
    }
    // Reset quantity if exceeds new stock
    if (found && quantity > found.stock) {
      setQuantity(Math.max(1, found.stock));
    }
  };

  const handleAddToCart = async () => {
    if (isShopPaused || isOutOfStock || isAddingToCart || isBuyNowPending) return;
    setActionError(null);
    setActionSuccess(null);
    if (!activeVariant) {
      setActionError("Vui lòng chọn phân loại sản phẩm.");
      return;
    }
    try {
      await addToCart({
        variantId: activeVariant.id,
        quantity,
        title: product.title,
        price: activeVariant.price,
        imageUrl: activeVariant.imageUrl || product.images[0] || "/images/placeholder.png",
        specs:
          activeVariant.name ||
          Object.entries(selectedAttributes)
            .map(([k, v]) => `${k}: ${v}`)
            .join(" • ") ||
          "Tiêu chuẩn",
        storeName: product.merchant.name,
        storeId: product.merchant.id,
      });

      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current);
      }

      const variantDesc = activeVariant.name || Object.values(selectedAttributes).join(", ");
      setCartToast({
        message: "Đã thêm vào giỏ hàng thành công!",
        itemInfo: `${product.title}${variantDesc ? ` • ${variantDesc}` : ""} (x${quantity})`,
      });

      // Tự động đóng thông báo sau đúng 5 giây
      toastTimeoutRef.current = setTimeout(() => {
        setCartToast(null);
      }, 5000);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg =
        axiosErr?.response?.data?.message ||
        "Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.";
      setActionError(msg);
      setTimeout(() => setActionError(null), 4000);
    }
  };

  const handleBuyNow = async () => {
    if (isShopPaused || isOutOfStock || isAddingToCart || isBuyNowPending) return;
    setActionError(null);
    setActionSuccess(null);
    if (!activeVariant) {
      setActionError("Vui lòng chọn phân loại sản phẩm.");
      return;
    }
    setIsBuyNowPending(true);
    try {
      await addToCart({
        variantId: activeVariant.id,
        quantity,
        title: product.title,
        price: activeVariant.price,
        imageUrl: activeVariant.imageUrl || product.images[0] || "/images/placeholder.png",
        specs:
          activeVariant.name ||
          Object.entries(selectedAttributes)
            .map(([k, v]) => `${k}: ${v}`)
            .join(" • ") ||
          "Tiêu chuẩn",
        storeName: product.merchant.name,
        storeId: product.merchant.id,
      });
      router.push(ROUTES.CHECKOUT);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg =
        axiosErr?.response?.data?.message ||
        "Không thể tiến hành mua ngay. Vui lòng thử lại.";
      setActionError(msg);
      setTimeout(() => setActionError(null), 4000);
      setIsBuyNowPending(false);
    }
  };

  const discountPercent = activeVariant?.originalPrice
    ? Math.round(
        ((activeVariant.originalPrice - activeVariant.price) /
          activeVariant.originalPrice) *
          100
      )
    : 0;

  const isOutOfStock = !activeVariant || activeVariant.stock === 0;

  // Review Filter
  const filteredReviews = selectedStarFilter
    ? product.reviews.filter((r) => r.rating === selectedStarFilter)
    : product.reviews;

  // Dynamic Specs with Variant Attribute Override (BR-09 / Module 4: products.attributes & product_variants.attributes)
  const resolvedSpecs = useMemo(() => {
    // Clone base specs
    const specsMap = new Map<string, string>();
    product.specs.forEach((spec) => {
      specsMap.set(spec.label.toLowerCase(), spec.value);
    });

    // Check if activeVariant has overriding attributes
    if (activeVariant && activeVariant.attributes) {
      Object.entries(activeVariant.attributes).forEach(([attrKey, attrVal]) => {
        // If spec already exists with similar label or variant specifies it
        specsMap.set(attrKey.toLowerCase(), attrVal);
      });
    }

    // Reconstruct list keeping base spec order, then adding any variant-specific attributes
    const renderedKeys = new Set<string>();
    const result: { label: string; value: string; isOverridden?: boolean }[] = [];

    product.specs.forEach((spec) => {
      const lowerKey = spec.label.toLowerCase();
      renderedKeys.add(lowerKey);
      const variantOverride = activeVariant?.attributes?.[spec.label];
      result.push({
        label: spec.label,
        value: variantOverride || spec.value,
        isOverridden: !!variantOverride && variantOverride !== spec.value,
      });
    });

    // Add remaining variant attributes not explicitly in base specs (e.g. Colorway, Switch Type)
    if (activeVariant && activeVariant.attributes) {
      Object.entries(activeVariant.attributes).forEach(([attrKey, attrVal]) => {
        if (!renderedKeys.has(attrKey.toLowerCase())) {
          result.unshift({
            label: attrKey,
            value: attrVal,
            isOverridden: true,
          });
        }
      });
    }

    return result;
  }, [product.specs, activeVariant]);

  return (
    <div className="w-full pb-20 space-y-8">
      {/* 1. Breadcrumbs */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 pt-4">
        <nav className="flex items-center gap-1.5 text-xs text-slate-500">
          <Link href="/" className="hover:text-[#0045cc] transition-colors">
            Home
          </Link>
          <ChevronRight className="w-3 h-3 text-slate-300" />
          <Link
            href={`/products?category=${product.categorySlug}`}
            className="hover:text-[#0045cc] transition-colors"
          >
            {product.categoryName}
          </Link>
          <ChevronRight className="w-3 h-3 text-slate-300" />
          <span className="text-slate-800 font-semibold truncate max-w-sm">
            {product.title}
          </span>
        </nav>
      </div>

      {/* 2. Product Hero Showcase */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        {/* Shop Paused Banner if applicable */}
        {isShopPaused && (
          <div className="mb-6 p-4 rounded-2xl bg-[#fef3c7] border border-[#fde68a] text-xs text-[#92400e] flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <AlertTriangle className="w-4 h-4 text-[#d97706] shrink-0" />
              <span>
                <strong>Shop is currently on vacation mode.</strong> Products are displayed for preview only; orders are temporarily paused.
              </span>
            </div>
            <span className="font-bold uppercase text-[11px] bg-[#f59e0b]/20 px-2.5 py-0.5 rounded-full">
              Paused
            </span>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 bg-white p-6 md:p-8 rounded-2xl shadow-sm border border-[#eff4ff]">
          {/* Visual Media Bench (5 cols) */}
          <div className="lg:col-span-5 flex flex-col gap-4">
            {/* Main Canvas Frame */}
            <div className="relative w-full aspect-[4/3] rounded-2xl bg-[#eff4ff] overflow-hidden flex items-center justify-center border border-slate-100">
              <div className="absolute top-3 left-3 z-10 px-2.5 py-1 rounded-full bg-slate-900/80 backdrop-blur-md text-white text-[11px] font-medium font-mono">
                Image {selectedImg + 1} / {product.images.length}
              </div>

              <Image
                src={product.images[selectedImg] || product.images[0]}
                alt={product.title}
                fill
                priority
                className="object-contain p-4 hover:scale-105 transition-transform duration-300"
                unoptimized
              />
            </div>

            {/* Thumbnail Navigation Row */}
            <div className="grid grid-cols-4 gap-2.5">
              {product.images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedImg(idx)}
                  className={`relative aspect-[4/3] rounded-xl overflow-hidden bg-[#eff4ff] p-1 border-2 transition-all cursor-pointer ${
                    selectedImg === idx
                      ? "border-[#0045cc] shadow-sm"
                      : "border-transparent hover:border-slate-300"
                  }`}
                >
                  <Image
                    src={img}
                    alt={`Thumbnail ${idx + 1}`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                </button>
              ))}
            </div>

            {/* Wishlist & Share */}
            <div className="flex items-center justify-between pt-2 text-xs text-slate-500 border-t border-slate-100">
              <button
                type="button"
                onClick={toggleWishlist}
                disabled={isWishlistPending}
                className={`flex items-center gap-1.5 transition-colors cursor-pointer ${
                  isWishlisted
                    ? "text-rose-600 font-semibold"
                    : "hover:text-[#ba1a1a] text-slate-600"
                }`}
                title={isWishlisted ? "Bỏ yêu thích" : "Thêm vào danh sách yêu thích"}
              >
                <Heart
                  className={`w-4 h-4 transition-transform active:scale-75 ${
                    isWishlisted ? "fill-rose-500 text-rose-500" : ""
                  }`}
                />
                <span>{isWishlisted ? "Đã yêu thích" : "Thêm vào Yêu thích"}</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  if (typeof navigator !== "undefined" && navigator.share) {
                    navigator.share({ title: product.title, url: window.location.href });
                  } else if (typeof navigator !== "undefined") {
                    navigator.clipboard.writeText(window.location.href);
                    toast.success("Đã sao chép liên kết sản phẩm");
                  }
                }}
                className="flex items-center gap-1.5 hover:text-[#0045cc] transition-colors cursor-pointer"
              >
                <Share2 className="w-4 h-4" />
                <span>Chia sẻ</span>
              </button>
            </div>
          </div>

          {/* Decision Console (7 cols) */}
          <div className="lg:col-span-7 flex flex-col justify-between space-y-6">
            <div>
              {/* Badges */}
              <div className="flex flex-wrap items-center gap-2 mb-2">
                {product.flagshipBadge && (
                  <span className="px-2.5 py-0.5 rounded-full bg-[#0045cc]/10 text-[#0045cc] font-bold text-xs">
                    {product.flagshipBadge}
                  </span>
                )}
                {product.brandName && (
                  <span className="px-2.5 py-0.5 rounded-full bg-[#eff4ff] text-[#434656] font-semibold text-xs font-mono">
                    Brand: {product.brandName}
                  </span>
                )}
                {product.merchant.verified && (
                  <span className="px-2.5 py-0.5 rounded-full bg-[#53f8d9]/20 text-[#006b5b] font-bold text-xs flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" /> VERIFIED GENUINE
                  </span>
                )}
              </div>

              {/* Title */}
              <h1 className="text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight font-heading leading-snug">
                {product.title}
              </h1>

              {/* Rating & Stats */}
              <div className="flex items-center gap-4 mt-3 pb-4 border-b border-slate-100 text-xs">
                <div className="flex items-center gap-1 text-amber-500 font-bold">
                  <Star className="w-4 h-4 fill-current" />
                  <span>{avgRating.toFixed(1)}</span>
                </div>
                <span className="text-slate-300">|</span>
                <span className="text-slate-500">
                  {totalReviewsCount > 0 ? (
                    <>
                      <strong className="text-slate-800">{totalReviewsCount}</strong> Đánh giá
                    </>
                  ) : (
                    <span className="text-slate-400">Chưa có đánh giá</span>
                  )}
                </span>
                {product.soldCount > 0 && (
                  <>
                    <span className="text-slate-300">|</span>
                    <span className="text-slate-500">
                      <strong className="text-slate-800">{product.soldCount}</strong> Đã bán
                    </span>
                  </>
                )}
                <span className="text-slate-300">|</span>
                <span className="font-mono text-slate-500">
                  SKU: <strong className="text-[#0b1c30]">{activeVariant.sku}</strong>
                </span>
              </div>

              {/* Price Box */}
              <div className="mt-4 p-4 rounded-2xl bg-[#eff4ff] flex items-baseline justify-between border border-[#dce9ff]">
                <div className="flex items-baseline gap-3">
                  <span className="text-2xl sm:text-3xl font-bold text-[#0045cc] font-mono tracking-tight">
                    {new Intl.NumberFormat("vi-VN", {
                      style: "currency",
                      currency: "VND",
                    }).format(activeVariant.price)}
                  </span>
                  {activeVariant.originalPrice > activeVariant.price && (
                    <span className="text-xs text-slate-400 line-through font-mono">
                      {new Intl.NumberFormat("vi-VN", {
                        style: "currency",
                        currency: "VND",
                      }).format(activeVariant.originalPrice)}
                    </span>
                  )}
                </div>
                {discountPercent > 0 && (
                  <span className="bg-[#a90900] text-white text-xs font-bold px-2.5 py-1 rounded-full">
                    SAVE {discountPercent}%
                  </span>
                )}
              </div>

              {/* Multi-Attribute Variant Selectors (Cross-Attribute Disable Pattern) */}
              <div className="mt-6 space-y-4">
                {attributeGroups.map((group) => {
                  const selectedVal = selectedAttributes[group.name];

                  return (
                    <div key={group.name} className="flex flex-col gap-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-[#0b1c30] uppercase tracking-wider">
                          {group.name}:
                        </span>
                        <span className="text-xs font-semibold text-[#0045cc]">
                          {selectedVal}
                        </span>
                      </div>

                      {/* Chip/Pill Buttons */}
                      <div className="flex items-center gap-2.5 flex-wrap">
                        {group.values.map((val) => {
                          const isSelected = selectedVal === val;
                          const available = isOptionAvailable(group.name, val);

                          return (
                            <button
                              key={val}
                              type="button"
                              disabled={!available && !isSelected}
                              onClick={() => handleSelectAttribute(group.name, val)}
                              className={`px-3.5 py-2 rounded-xl text-xs font-semibold border-2 transition-all flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed ${
                                isSelected
                                  ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc] shadow-xs"
                                  : available
                                  ? "border-slate-200 hover:border-slate-300 bg-white text-[#434656]"
                                  : "border-slate-100 bg-slate-50 text-slate-300 line-through opacity-60"
                              }`}
                            >
                              {isSelected && <Check className="w-3.5 h-3.5 text-[#0045cc]" />}
                              <span>{val}</span>
                              {!available && (
                                <span className="text-[10px] text-slate-400 font-normal no-underline ml-0.5">
                                  (Out of stock)
                                </span>
                              )}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Quantity & Stock Indicator */}
              <div className="mt-6 flex items-center gap-6">
                <span className="text-xs font-bold text-[#0b1c30] uppercase tracking-wider">
                  Số lượng:
                </span>
                <div className="flex items-center bg-[#eff4ff] rounded-lg p-1 border border-slate-200">
                  <button
                    disabled={isOutOfStock || isShopPaused || quantity <= 1}
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    className="w-7 h-7 flex items-center justify-center rounded text-slate-600 hover:bg-white transition-colors disabled:opacity-40 cursor-pointer"
                    aria-label="Giảm số lượng"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="w-8 text-center text-xs font-bold font-mono text-[#0b1c30]">
                    {isOutOfStock ? 0 : quantity}
                  </span>
                  <button
                    disabled={isOutOfStock || isShopPaused || quantity >= (activeVariant?.stock || 0)}
                    onClick={() => setQuantity((q) => Math.min(activeVariant?.stock || 1, q + 1))}
                    className="w-7 h-7 flex items-center justify-center rounded text-slate-600 hover:bg-white transition-colors disabled:opacity-40 cursor-pointer"
                    aria-label="Tăng số lượng"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={`text-xs font-mono font-semibold ${
                      isOutOfStock ? "text-[#ba1a1a]" : "text-slate-500"
                    }`}
                  >
                    {isOutOfStock
                      ? "Tạm thời hết hàng"
                      : `${activeVariant?.stock || 0} sản phẩm có sẵn`}
                  </span>
                </div>
              </div>
            </div>

            {/* Action Feedback */}
            {actionError && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-600 text-xs flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            {/* Action Buttons */}
            <div className="pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <button
                disabled={isOutOfStock || isShopPaused || isAddingToCart || isBuyNowPending}
                onClick={handleAddToCart}
                className="py-3.5 px-6 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] font-bold text-sm transition-colors flex items-center justify-center gap-2 border border-[#0045cc]/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isAddingToCart ? (
                  <Loader2 className="w-4 h-4 animate-spin text-[#0045cc]" />
                ) : (
                  <ShoppingCart className="w-4 h-4" />
                )}
                <span>
                  {isAddingToCart
                    ? "Đang thêm..."
                    : isShopPaused
                    ? "Cửa hàng tạm nghỉ"
                    : isOutOfStock
                    ? "Hết hàng"
                    : "Thêm vào giỏ hàng"}
                </span>
              </button>
              <button
                disabled={isOutOfStock || isShopPaused || isAddingToCart || isBuyNowPending}
                onClick={handleBuyNow}
                className="py-3.5 px-6 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white font-bold text-sm transition-colors flex items-center justify-center gap-2 shadow-md cursor-pointer active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isBuyNowPending ? (
                  <Loader2 className="w-4 h-4 animate-spin text-white" />
                ) : (
                  <span>Mua ngay</span>
                )}
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Merchant Profile Card */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        <div className="p-6 rounded-2xl bg-white shadow-sm border border-[#eff4ff] flex flex-col gap-5">
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
            {/* Left: Avatar + Name + Chat / Visit Buttons */}
            <div className="flex items-start gap-4">
              {displayAvatarUrl ? (
                <img
                  src={displayAvatarUrl}
                  alt={displayShopName}
                  className="w-16 h-16 rounded-2xl object-cover shadow-md shrink-0 border border-slate-200"
                />
              ) : (
                <div className="w-16 h-16 rounded-2xl bg-[#0045cc] flex items-center justify-center text-white font-bold text-xl shadow-md shrink-0">
                  {(displayShopName || "TH").substring(0, 2).toUpperCase()}
                </div>
              )}
              <div className="flex flex-col gap-2">
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <Link
                      href={`/shops/${shopSellerName || product.merchant.id}`}
                      className="font-bold text-lg text-[#0b1c30] hover:text-[#0045cc] transition-colors font-heading"
                    >
                      {displayShopName}
                    </Link>
                    {product.merchant.verified && (
                      <span className="px-2 py-0.5 rounded bg-blue-50 text-[#0045cc] text-[10px] font-bold">
                        VERIFIED STORE
                      </span>
                    )}
                    {isShopPaused && (
                      <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-600 text-[10px] font-bold">
                        TẠM NGHỈ
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2.5 text-xs text-slate-500 mt-1 flex-wrap">
                    <span>{displayJoined}</span>
                    {displayWarehouse && (
                      <>
                        <span>•</span>
                        <span className="flex items-center gap-1 text-slate-600 font-medium">
                          <MapPin className="w-3.5 h-3.5 text-[#0045cc]" />
                          {displayWarehouse}
                        </span>
                      </>
                    )}
                  </div>
                </div>

                {/* Action Buttons: Chat Now & Visit Shop directly under shop name */}
                <div className="flex items-center gap-2.5 pt-0.5">
                  <button className="px-3.5 py-1.5 bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer border border-[#0045cc]/20">
                    <MessageCircle className="w-3.5 h-3.5" />
                    <span>Chat Now</span>
                  </button>
                  <Link
                    href={`/shops/${shopSellerName || product.merchant.id}`}
                    className="px-3.5 py-1.5 bg-white hover:bg-slate-900 text-[#434656] hover:text-white border border-[#c3c5d9]/40 hover:border-slate-900 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer shadow-2xs"
                  >
                    <Building2 className="w-3.5 h-3.5" />
                    <span>Visit Shop</span>
                  </Link>
                </div>
              </div>
            </div>

            {/* Right: Stats Matrix */}
            <div className="grid grid-cols-3 gap-6 w-full lg:w-auto text-left lg:text-center text-xs border-t lg:border-t-0 pt-4 lg:pt-0 border-slate-100">
              <div>
                <p className="text-slate-400">Đánh giá Shop</p>
                <p className="font-bold text-amber-500 text-sm mt-0.5 flex items-center justify-start lg:justify-center gap-1">
                  <span>{product.merchant.rating.toFixed(1)}</span>
                  <Star className="w-3.5 h-3.5 fill-current" />
                </p>
              </div>
              <div>
                <p className="text-slate-400">Phản hồi</p>
                <p className="font-bold text-[#006b5b] text-sm mt-0.5">
                  {product.merchant.positiveFeedback}
                </p>
              </div>
              <div>
                <p className="text-slate-400">Tổng sản phẩm</p>
                <p className="font-bold text-[#0045cc] text-sm mt-0.5">
                  {totalShopProducts} sản phẩm
                </p>
              </div>
            </div>
          </div>

          {/* Shop Vouchers highlight strip */}
          {shopVouchers && shopVouchers.length > 0 && (
            <div className="pt-3 border-t border-slate-100 flex items-center gap-2.5 flex-wrap">
              <span className="text-xs font-bold text-slate-700 flex items-center gap-1.5 mr-1">
                <Ticket className="w-3.5 h-3.5 text-[#0045cc]" />
                Voucher Cửa hàng:
              </span>
              {shopVouchers.slice(0, 3).map((v) => (
                <Link
                  key={v.id}
                  href={`/shops/${shopSellerName}`}
                  className="px-2.5 py-1 rounded-lg bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] border border-[#0045cc]/20 text-xs font-mono font-bold transition-colors flex items-center gap-1"
                  title={v.name}
                >
                  <span>{v.code}</span>
                  <span className="text-[10px] font-sans font-medium text-slate-500">
                    ({v.discountType === "PERCENTAGE" ? `-${v.discountValue}%` : `-${new Intl.NumberFormat("vi-VN").format(v.discountValue)}₫`})
                  </span>
                </Link>
              ))}
              <Link
                href={`/shops/${shopSellerName}`}
                className="text-xs text-[#0045cc] font-semibold hover:underline ml-auto"
              >
                Xem tất cả ({shopVouchers.length}) &rarr;
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* 4. Technical Specification Matrix */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        <div className="p-6 md:p-8 rounded-2xl bg-white shadow-sm border border-[#eff4ff]">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 gap-4">
            <div>
              <h2 className="text-xl font-bold text-[#0b1c30] font-heading">
                Technical Specifications
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Standardized category specifications verified by TechHub platform
              </p>
            </div>
            {activeVariant && (
              <div className="flex items-center gap-2 self-start sm:self-auto">
                <span className="text-[11px] font-medium text-slate-500">Selected Variant:</span>
                <span className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-[#eff4ff] text-[#0045cc] border border-[#dce9ff]">
                  {activeVariant.name}
                </span>
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-2 pt-6">
            {resolvedSpecs.map((spec, idx) => (
              <div
                key={idx}
                className={`flex items-center justify-between py-2.5 px-3 rounded-lg text-xs transition-colors ${
                  spec.isOverridden
                    ? "bg-[#eff4ff]/80 border border-[#dce9ff]"
                    : "even:bg-[#eff4ff]/40"
                }`}
              >
                <div className="flex items-center gap-1.5">
                  <span className="text-slate-500 font-medium">{spec.label}</span>
                  {spec.isOverridden && (
                    <span className="text-[10px] font-semibold text-[#0045cc] bg-white px-1.5 py-0.5 rounded border border-[#dce9ff]">
                      Variant
                    </span>
                  )}
                </div>
                <span className="font-mono font-semibold text-[#0b1c30] text-right">
                  {spec.value}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 5. Product Description */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        <div className="p-6 md:p-8 rounded-2xl bg-white shadow-sm border border-[#eff4ff]">
          <div className="border-b border-slate-100 pb-4 mb-6">
            <h2 className="text-xl font-bold text-[#0b1c30] font-heading">
              Product Description
            </h2>
          </div>

          <div className="max-w-4xl space-y-6 text-sm text-slate-700 leading-relaxed">
            {product.description.intro.map((p, idx) => (
              <p key={idx}>{p}</p>
            ))}

            <div>
              <h3 className="font-bold text-[#0b1c30] text-base mb-3 font-heading">
                Key Features &amp; Highlights
              </h3>
              <ul className="space-y-2 text-slate-600 pl-2">
                {product.description.features.map((feat, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-[#0045cc] font-bold mt-0.5">•</span>
                    <span>{feat}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3 className="font-bold text-[#0b1c30] text-base mb-3 font-heading">
                Materials &amp; Build Quality
              </h3>
              <ul className="space-y-2 text-slate-600 pl-2">
                {product.description.materials.map((mat, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-[#006b5b] font-bold mt-0.5">•</span>
                    <span>{mat}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3 className="font-bold text-[#0b1c30] text-base mb-3 font-heading">
                What&apos;s in the Box
              </h3>
              <ul className="space-y-2 text-slate-600 pl-2">
                {product.description.inTheBox.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-slate-400 font-bold mt-0.5">•</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Verified Customer Reviews & Interactive Real Filter */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full" id="reviews">
        <div className="p-6 md:p-8 rounded-2xl bg-white shadow-sm border border-[#eff4ff]">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 gap-4">
            <div>
              <h2 className="text-xl font-bold text-[#0b1c30] font-heading">
                Đánh giá từ Khách hàng
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Nhận xét thực tế từ người mua đã nhận hàng (Verified Purchase)
              </p>
            </div>
            {selectedStarFilter && (
              <button
                type="button"
                onClick={() => {
                  setSelectedStarFilter(null);
                  setReviewPage(0);
                }}
                className="text-xs text-primary hover:underline font-semibold cursor-pointer"
              >
                Xóa bộ lọc ({selectedStarFilter} Sao)
              </button>
            )}
          </div>

          {/* Rating Overview Bento */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center py-6 border-b border-slate-100">
            {/* Big Rating Number */}
            <div className="lg:col-span-4 flex flex-col items-center justify-center p-6 rounded-2xl bg-[#eff4ff] text-center">
              <span className="text-5xl font-extrabold text-[#0b1c30] tracking-tight font-mono">
                {avgRating.toFixed(1)}
              </span>
              <div className="flex items-center gap-1 text-amber-500 my-2">
                {[...Array(5)].map((_, i) => (
                  <Star
                    key={i}
                    className={`w-4 h-4 ${
                      i < Math.round(avgRating)
                        ? "fill-current"
                        : "text-slate-300"
                    }`}
                  />
                ))}
              </div>
              <span className="text-xs text-slate-500">
                Dựa trên {totalReviewsCount} đánh giá toàn sàn
              </span>
            </div>

            {/* Rating Progress Bars (Interactive Filtering) */}
            <div className="lg:col-span-8 flex flex-col gap-2 text-xs">
              {ratingBreakdown.map((row) => (
                <button
                  key={row.star}
                  type="button"
                  onClick={() => {
                    setSelectedStarFilter(
                      selectedStarFilter === row.star ? null : row.star
                    );
                    setReviewPage(0);
                  }}
                  className={`flex items-center gap-3 p-1.5 rounded-xl transition-colors cursor-pointer text-left ${
                    selectedStarFilter === row.star
                      ? "bg-[#eff4ff] ring-1 ring-primary"
                      : "hover:bg-[#eff4ff]/60"
                  }`}
                >
                  <span className="w-16 text-slate-700 font-semibold flex items-center gap-1">
                    <span>{row.star} sao</span>
                    <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                  </span>
                  <div className="flex-1 bg-slate-100 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="bg-primary h-full rounded-full transition-all duration-300"
                      style={{ width: `${row.percentage}%` }}
                    />
                  </div>
                  <span className="w-20 text-right font-mono text-slate-500 text-[11px]">
                    {row.count} ({row.percentage}%)
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* Filter & Sort Chips Bar */}
          <div className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100">
            <div className="flex items-center gap-1.5 flex-wrap">
              <button
                type="button"
                onClick={() => {
                  setSelectedStarFilter(null);
                  setOnlyMediaFilter(false);
                  setReviewPage(0);
                }}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                  selectedStarFilter === null && !onlyMediaFilter
                    ? "bg-slate-900 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                Tất cả ({totalReviewsCount})
              </button>
              {[5, 4, 3, 2, 1].map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => {
                    setSelectedStarFilter(selectedStarFilter === s ? null : s);
                    setReviewPage(0);
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                    selectedStarFilter === s
                      ? "bg-primary text-white"
                      : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                  }`}
                >
                  {s} Sao ({ratingSummary?.ratingCounts?.[s] ?? 0})
                </button>
              ))}
              <button
                type="button"
                onClick={() => {
                  setOnlyMediaFilter(!onlyMediaFilter);
                  setReviewPage(0);
                }}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1 transition-colors cursor-pointer ${
                  onlyMediaFilter
                    ? "bg-primary text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                <ImageIcon className="w-3.5 h-3.5" />
                <span>Có hình ảnh ({mediaReviewsCount})</span>
              </button>
            </div>

            <div className="flex items-center gap-2 shrink-0 self-end sm:self-auto">
              <span className="text-[11px] text-slate-400">Sắp xếp:</span>
              <select
                value={reviewSortBy}
                onChange={(e) => {
                  setReviewSortBy(e.target.value as "createdAt" | "rating");
                  setReviewPage(0);
                }}
                className="h-8 px-2.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:border-primary"
              >
                <option value="createdAt">Mới nhất</option>
                <option value="rating">Đánh giá cao nhất</option>
              </select>
            </div>
          </div>

          {/* Individual Review Cards */}
          <div className="space-y-4 pt-6">
            {isReviewsLoading ? (
              <div className="p-8 text-center flex flex-col items-center justify-center gap-2">
                <Loader2 className="w-6 h-6 animate-spin text-primary" />
                <span className="text-xs text-slate-400">Đang tải đánh giá...</span>
              </div>
            ) : realReviews.length === 0 ? (
              <div className="p-8 text-center bg-slate-50/70 rounded-2xl border border-dashed border-slate-200 my-2">
                <MessageSquare className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-700">
                  Chưa có đánh giá nào phù hợp với bộ lọc
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  Hãy thử chọn số sao khác hoặc bấm &ldquo;Tất cả&rdquo; để xem toàn bộ nhận xét.
                </p>
              </div>
            ) : (
              realReviews.map((rev) => (
                <div
                  key={rev.id}
                  className="p-5 rounded-2xl bg-[#eff4ff]/40 flex flex-col gap-3 border border-slate-100 transition-colors"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3">
                      {rev.reviewerAvatar ? (
                        <div className="relative w-9 h-9 rounded-full overflow-hidden shrink-0 border border-slate-200">
                          <Image
                            src={rev.reviewerAvatar}
                            alt={rev.reviewerName}
                            fill
                            className="object-cover"
                            unoptimized
                          />
                        </div>
                      ) : (
                        <div className="w-9 h-9 rounded-full bg-primary/10 text-primary font-bold text-xs flex items-center justify-center shrink-0 border border-primary/20">
                          {rev.reviewerName ? rev.reviewerName.charAt(0).toUpperCase() : "U"}
                        </div>
                      )}
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-slate-900">
                            {rev.reviewerName}
                          </span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-emerald-50 text-emerald-700 font-semibold border border-emerald-200/60">
                            Đã mua hàng
                          </span>
                        </div>
                        <div className="flex items-center gap-0.5 text-amber-400 mt-1">
                          {[...Array(5)].map((_, i) => (
                            <Star
                              key={i}
                              className={`w-3.5 h-3.5 ${
                                i < rev.rating
                                  ? "fill-current"
                                  : "text-slate-200"
                              }`}
                            />
                          ))}
                        </div>
                      </div>
                    </div>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {new Date(rev.createdAt).toLocaleDateString("vi-VN")}
                    </span>
                  </div>

                  {rev.variantName && (
                    <div className="text-[11px] text-slate-500 flex items-center gap-1.5">
                      <span className="text-slate-400">Phân loại hàng:</span>
                      <span className="font-medium text-slate-700 bg-white px-2 py-0.5 rounded-md border border-slate-200/80">
                        {rev.variantName}
                      </span>
                    </div>
                  )}

                  {rev.comment && (
                    <p className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line">
                      {rev.comment}
                    </p>
                  )}

                  {/* Review Media Images */}
                  {rev.mediaList && rev.mediaList.length > 0 && (
                    <div className="flex items-center gap-2 pt-1 flex-wrap">
                      {rev.mediaList.map((m) => (
                        <button
                          key={m.id}
                          type="button"
                          onClick={() => setPreviewMediaUrl(m.mediaUrl)}
                          className="relative w-16 h-16 sm:w-20 sm:h-20 rounded-xl overflow-hidden border border-slate-200 bg-white group cursor-pointer hover:ring-2 hover:ring-primary transition-all shrink-0"
                        >
                          <Image
                            src={m.mediaUrl}
                            alt="Ảnh đánh giá"
                            fill
                            className="object-cover group-hover:scale-105 transition-transform"
                            unoptimized
                          />
                        </button>
                      ))}
                    </div>
                  )}

                  {/* Shop Response (if present) */}
                  {rev.sellerResponse && (
                    <div className="p-3.5 rounded-xl bg-white border border-primary/15 text-xs shadow-2xs">
                      <div className="flex items-center gap-1.5 font-bold text-primary mb-1">
                        <Store className="w-3.5 h-3.5 shrink-0" />
                        <span>Phản hồi của Người bán:</span>
                        {rev.sellerResponseAt && (
                          <span className="text-[10px] font-normal text-slate-400">
                            • {new Date(rev.sellerResponseAt).toLocaleDateString("vi-VN")}
                          </span>
                        )}
                      </div>
                      <p className="text-slate-700 leading-relaxed pl-5">
                        {rev.sellerResponse}
                      </p>
                    </div>
                  )}

                  {/* Review Footer Actions */}
                  <div className="flex items-center gap-4 pt-1 text-xs">
                    <button
                      type="button"
                      onClick={() => toggleHelpfulMutation.mutate(rev.id)}
                      disabled={toggleHelpfulMutation.isPending}
                      className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg transition-colors cursor-pointer ${
                        rev.hasVotedHelpful
                          ? "bg-primary/10 text-primary font-bold"
                          : "text-slate-500 hover:bg-slate-100"
                      }`}
                    >
                      <ThumbsUp
                        className={`w-3.5 h-3.5 ${
                          rev.hasVotedHelpful ? "fill-primary" : ""
                        }`}
                      />
                      <span>Hữu ích ({rev.helpfulCount})</span>
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Pagination */}
          {reviewTotalPages > 1 && (
            <div className="mt-6 pt-4 border-t border-slate-100">
              <Pagination
                page={reviewPage}
                size={5}
                totalElements={reviewTotalElements}
                totalPages={reviewTotalPages}
                onPageChange={setReviewPage}
                isLoading={isReviewsLoading}
                variant="bottom-full"
              />
            </div>
          )}
        </div>

        {/* Media Fullscreen Preview Modal */}
        {previewMediaUrl && (
          <div
            role="dialog"
            aria-modal="true"
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-xs animate-in fade-in"
            onClick={() => setPreviewMediaUrl(null)}
          >
            <div
              className="relative max-w-3xl w-full max-h-[85vh] bg-black rounded-2xl overflow-hidden flex items-center justify-center"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                type="button"
                onClick={() => setPreviewMediaUrl(null)}
                className="absolute top-3 right-3 z-10 w-8 h-8 rounded-full bg-white/20 hover:bg-white/40 text-white flex items-center justify-center transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
              <div className="relative w-full h-[70vh]">
                <Image
                  src={previewMediaUrl}
                  alt="Ảnh phóng to"
                  fill
                  className="object-contain"
                  unoptimized
                />
              </div>
            </div>
          </div>
        )}
      </section>

      {/* 7. Recommendations Section */}
      {(realRelatedProducts.length > 0 || product.recommendations.length > 0) && (
        <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
          <h2 className="text-xl font-bold text-[#0b1c30] font-heading mb-4">
            Recommended Hardware &amp; Accessories
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
            {realRelatedProducts.length > 0
              ? realRelatedProducts.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))
              : product.recommendations.map((item) => (
                  <ProductCard
                    key={item.id}
                    product={{
                      id: item.id,
                      title: item.title,
                      vendorName: item.brand,
                      price: item.price,
                      rating: item.rating,
                      reviewsCount: item.reviewsCount,
                      imageUrl: item.imageUrl,
                      inStock: true,
                      tags: item.specs,
                    }}
                  />
                ))}
          </div>
        </section>
      )}

      {/* Floating 5-Second Toast Notification */}
      {cartToast && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-[#0b1c30] text-white px-4 py-3.5 rounded-2xl shadow-2xl border border-slate-700/60 animate-in fade-in slide-in-from-bottom-5 duration-300">
          <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
            <Check className="w-4 h-4 stroke-[2.5]" />
          </div>
          <div className="text-xs">
            <p className="font-bold text-white">{cartToast.message}</p>
            {cartToast.itemInfo && (
              <p className="text-slate-300 text-[11px] truncate max-w-[240px] mt-0.5">
                {cartToast.itemInfo}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={() => openCart()}
            className="ml-2 px-2.5 py-1 rounded-lg bg-[#0045cc] hover:bg-[#0d5bff] text-white text-[11px] font-bold shrink-0 transition-colors cursor-pointer"
          >
            Xem giỏ
          </button>
          <button
            type="button"
            onClick={() => setCartToast(null)}
            className="text-slate-400 hover:text-white p-1 rounded cursor-pointer"
            aria-label="Đóng thông báo"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </div>
  );
}
