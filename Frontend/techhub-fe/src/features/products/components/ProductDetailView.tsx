"use client";

import React, { useState, useMemo } from "react";
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
} from "lucide-react";
import { ProductCard } from "@/components/shared";
import { useCart } from "@/features/cart/context/CartContext";
import type { ProductDetailData, ProductVariant } from "@/types/productDetail";

interface ProductDetailViewProps {
  product: ProductDetailData;
}

export function ProductDetailView({ product }: ProductDetailViewProps) {
  const [selectedImg, setSelectedImg] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [selectedStarFilter, setSelectedStarFilter] = useState<number | null>(null);
  const { openCart } = useCart();

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

  const handleAddToCart = () => {
    if (isShopPaused || activeVariant.stock === 0) return;
    openCart();
  };

  const discountPercent = activeVariant.originalPrice
    ? Math.round(
        ((activeVariant.originalPrice - activeVariant.price) /
          activeVariant.originalPrice) *
          100
      )
    : 0;

  const isShopPaused = product.merchant.status === "PAUSED";
  const isOutOfStock = activeVariant.stock === 0;

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
              <button className="flex items-center gap-1.5 hover:text-[#ba1a1a] transition-colors cursor-pointer">
                <Heart className="w-4 h-4" />
                <span>Add to Wishlist</span>
              </button>
              <button className="flex items-center gap-1.5 hover:text-[#0045cc] transition-colors cursor-pointer">
                <Share2 className="w-4 h-4" />
                <span>Share Component</span>
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
                  <span>{product.rating.toFixed(1)}</span>
                </div>
                <span className="text-slate-300">|</span>
                <span className="text-slate-500">
                  <strong className="text-slate-800">{product.reviewsCount}</strong> Verified Ratings
                </span>
                <span className="text-slate-300">|</span>
                <span className="text-slate-500">
                  <strong className="text-slate-800">{product.soldCount}+</strong> Units Shipped
                </span>
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
                  Quantity:
                </span>
                <div className="flex items-center bg-[#eff4ff] rounded-lg p-1 border border-slate-200">
                  <button
                    disabled={isOutOfStock || isShopPaused}
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    className="w-7 h-7 flex items-center justify-center rounded text-slate-600 hover:bg-white transition-colors disabled:opacity-40"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="w-8 text-center text-xs font-bold font-mono text-[#0b1c30]">
                    {isOutOfStock ? 0 : quantity}
                  </span>
                  <button
                    disabled={isOutOfStock || isShopPaused}
                    onClick={() => setQuantity((q) => Math.min(activeVariant.stock, q + 1))}
                    className="w-7 h-7 flex items-center justify-center rounded text-slate-600 hover:bg-white transition-colors disabled:opacity-40"
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
                      ? "Temporarily Out of Stock"
                      : `${activeVariant.stock} items available in warehouse`}
                  </span>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <button
                disabled={isOutOfStock || isShopPaused}
                onClick={handleAddToCart}
                className="py-3.5 px-6 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0045cc] font-bold text-sm transition-colors flex items-center justify-center gap-2 border border-[#0045cc]/20 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ShoppingCart className="w-4 h-4" />
                <span>Add to Cart</span>
              </button>
              <button
                disabled={isOutOfStock || isShopPaused}
                onClick={handleAddToCart}
                className="py-3.5 px-6 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white font-bold text-sm transition-colors flex items-center justify-center gap-2 shadow-md cursor-pointer active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span>Buy Now</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Merchant Profile Card */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        <div className="p-6 rounded-2xl bg-white shadow-sm border border-[#eff4ff] flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
          {/* Left: Avatar + Name + Chat / Visit Buttons */}
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 rounded-2xl bg-[#0045cc] flex items-center justify-center text-white font-bold text-xl shadow-md shrink-0">
              {product.merchant.avatarText}
            </div>
            <div className="flex flex-col gap-2">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-lg text-[#0b1c30] font-heading">
                    {product.merchant.name}
                  </h3>
                  {product.merchant.verified && (
                    <span className="px-2 py-0.5 rounded bg-blue-50 text-[#0045cc] text-[10px] font-bold">
                      VERIFIED STORE
                    </span>
                  )}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">{product.merchant.joinedText}</p>
              </div>

              {/* Action Buttons: Chat Now & Visit Shop directly under shop name */}
              <div className="flex items-center gap-2.5 pt-0.5">
                <button className="px-3.5 py-1.5 bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer border border-[#0045cc]/20">
                  <MessageCircle className="w-3.5 h-3.5" />
                  <span>Chat Now</span>
                </button>
                <Link
                  href={`/shop/${product.merchant.sellerName || "keychron-official"}`}
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
              <p className="text-slate-400">Shop Rating</p>
              <p className="font-bold text-amber-500 text-sm mt-0.5 flex items-center justify-start lg:justify-center gap-1">
                <span>{product.merchant.rating.toFixed(1)}</span>
                <Star className="w-3.5 h-3.5 fill-current" />
              </p>
            </div>
            <div>
              <p className="text-slate-400">Positive Feedback</p>
              <p className="font-bold text-[#006b5b] text-sm mt-0.5">
                {product.merchant.positiveFeedback}
              </p>
            </div>
            <div>
              <p className="text-slate-400">Total Products</p>
              <p className="font-bold text-[#0045cc] text-sm mt-0.5">
                {product.merchant.totalProducts} Items
              </p>
            </div>
          </div>
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

      {/* 6. Verified Customer Reviews & Interactive Star Filter */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full" id="reviews">
        <div className="p-6 md:p-8 rounded-2xl bg-white shadow-sm border border-[#eff4ff]">
          <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 gap-4">
            <div>
              <h2 className="text-xl font-bold text-[#0b1c30] font-heading">
                Verified Customer Reviews
              </h2>
            </div>
            {selectedStarFilter && (
              <button
                onClick={() => setSelectedStarFilter(null)}
                className="text-xs text-[#0045cc] hover:underline font-semibold"
              >
                Clear Filter ({selectedStarFilter} Stars)
              </button>
            )}
          </div>

          {/* Rating Overview Bento */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center pb-8 border-b border-slate-100">
            {/* Big Rating Number */}
            <div className="lg:col-span-4 flex flex-col items-center justify-center p-6 rounded-2xl bg-[#eff4ff] text-center">
              <span className="text-5xl font-extrabold text-[#0b1c30] tracking-tight font-mono">
                {product.reviewStats.average.toFixed(1)}
              </span>
              <div className="flex items-center gap-1 text-amber-500 my-2">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className="w-4 h-4 fill-current" />
                ))}
              </div>
              <span className="text-xs text-slate-500">
                Based on {product.reviewStats.totalReviews} reviews worldwide
              </span>
            </div>

            {/* Rating Progress Bars (Interactive Filtering) */}
            <div className="lg:col-span-8 flex flex-col gap-2.5 text-xs">
              {product.reviewStats.breakdown.map((row) => (
                <button
                  key={row.star}
                  type="button"
                  onClick={() =>
                    setSelectedStarFilter(
                      selectedStarFilter === row.star ? null : row.star
                    )
                  }
                  className={`flex items-center gap-3 p-1.5 rounded-xl transition-colors cursor-pointer text-left ${
                    selectedStarFilter === row.star
                      ? "bg-[#eff4ff] ring-1 ring-[#0045cc]"
                      : "hover:bg-[#eff4ff]/60"
                  }`}
                >
                  <span className="w-14 text-slate-600 font-semibold flex items-center gap-1">
                    <span>{row.star}</span>
                    <Star className="w-3 h-3 fill-amber-500 text-amber-500" />
                  </span>
                  <div className="flex-1 bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className="bg-[#0045cc] h-full rounded-full"
                      style={{ width: `${row.percentage}%` }}
                    />
                  </div>
                  <span className="w-10 text-right font-mono text-slate-400">
                    {row.percentage}%
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* Individual Review Cards */}
          <div className="space-y-4 pt-6">
            {filteredReviews.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-6">
                No reviews found for {selectedStarFilter} stars.
              </p>
            ) : (
              filteredReviews.map((rev) => (
                <div key={rev.id} className="p-5 rounded-2xl bg-[#eff4ff]/50 flex flex-col gap-3 border border-slate-100">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-xs ${rev.avatarColor}`}>
                        {rev.userAvatarText}
                      </div>
                      <div>
                        <span className="font-bold text-sm text-[#0b1c30]">{rev.userName}</span>
                        <div className="flex items-center gap-0.5 text-amber-500 mt-0.5">
                          {[...Array(rev.rating)].map((_, i) => (
                            <Star key={i} className="w-3.5 h-3.5 fill-current" />
                          ))}
                        </div>
                      </div>
                    </div>
                    <span className="text-xs text-slate-400">{rev.date}</span>
                  </div>

                  <p className="text-xs sm:text-sm text-slate-700 leading-relaxed">
                    {rev.comment}
                  </p>

                  <div className="flex items-center gap-4 pt-1 text-xs text-slate-500">
                    <button className="flex items-center gap-1.5 hover:text-[#0045cc] transition-colors cursor-pointer">
                      <ThumbsUp className="w-3.5 h-3.5" />
                      <span>Helpful ({rev.helpfulCount})</span>
                    </button>
                    <button className="flex items-center gap-1.5 hover:text-[#0b1c30] transition-colors cursor-pointer">
                      <MessageSquare className="w-3.5 h-3.5" />
                      <span>Reply</span>
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </section>

      {/* 7. Recommendations Section */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12 w-full">
        <h2 className="text-xl font-bold text-[#0b1c30] font-heading mb-4">
          Recommended Hardware &amp; Accessories
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
          {product.recommendations.map((item) => (
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
    </div>
  );
}
