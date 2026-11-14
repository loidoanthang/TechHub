"use client";

import React from "react";
import Link from "next/link";
import { Star, ShieldCheck, ChevronRight, Heart } from "lucide-react";
import type { Product } from "@/types/product";
import { useToggleWishlist } from "@/features/account/api/wishlistApi";
import { formatPrice } from "@/lib/utils";
import { ProductThumbnail } from "@/components/shared";

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const { isWishlisted, toggleWishlist, isPending } = useToggleWishlist(product.id);

  const formattedPrice = formatPrice(product.price);
  const formattedOriginalPrice = product.originalPrice
    ? formatPrice(product.originalPrice)
    : null;

  const productUrl = `/products/${product.slug || product.id}`;

  return (
    <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col justify-between hover:shadow-lg hover:-translate-y-1 transition-all duration-200 group relative border border-[#eff4ff]">
      {/* Product Image viewport */}
      <div className="relative w-full aspect-square rounded-lg bg-[#eff4ff] overflow-hidden mb-3 p-4 flex items-center justify-center group/img">
        <Link
          href={productUrl}
          className="absolute inset-0 flex items-center justify-center"
        >
          <ProductThumbnail
            src={product.imageUrl}
            alt={product.title}
            sizeClassName="w-full h-full"
            className="rounded-none border-0 bg-transparent"
            imageClassName="object-contain p-2 group-hover:scale-105 transition-transform duration-300"
          />
        </Link>

        {/* Discount Badge */}
        {product.discountPercentage && (
          <span className="absolute top-2 left-2 bg-[#a90900] text-white text-[11px] font-bold px-2 py-0.5 rounded-full z-10 pointer-events-none">
            -{product.discountPercentage}%
          </span>
        )}

        {/* Wishlist Floating Button */}
        <button
          type="button"
          onClick={toggleWishlist}
          disabled={isPending}
          className={`absolute top-2 right-2 z-20 w-8 h-8 rounded-full flex items-center justify-center backdrop-blur-md transition-all duration-200 shadow-sm cursor-pointer ${
            isWishlisted
              ? "bg-white text-rose-500 shadow-rose-100"
              : "bg-white/80 hover:bg-white text-slate-500 hover:text-rose-500 hover:scale-110 opacity-80 group-hover/img:opacity-100"
          }`}
          aria-label={isWishlisted ? "Xóa khỏi yêu thích" : "Thêm vào yêu thích"}
          title={isWishlisted ? "Xóa khỏi yêu thích" : "Thêm vào yêu thích"}
        >
          <Heart
            className={`w-4 h-4 transition-transform active:scale-75 ${
              isWishlisted ? "fill-rose-500 text-rose-500" : ""
            }`}
          />
        </button>
      </div>

      {/* Product Details */}
      <div className="flex flex-col flex-1">
        {/* Vendor tag */}
        <div className="flex items-center gap-1.5 text-[11px] text-[#434656] mb-1 z-10">
          {product.vendorSlug ? (
            <Link
              href={`/shops/${product.vendorSlug}`}
              className="font-semibold truncate hover:text-[#0045cc] transition-colors hover:underline"
              onClick={(e) => e.stopPropagation()}
            >
              {product.vendorName}
            </Link>
          ) : (
            <span className="font-semibold truncate">{product.vendorName}</span>
          )}
          {product.isVerifiedVendor && (
            <ShieldCheck className="w-3.5 h-3.5 text-[#0045cc] flex-shrink-0" />
          )}
        </div>

        {/* Title */}
        <Link
          href={productUrl}
          className="text-sm font-semibold text-[#0b1c30] line-clamp-2 hover:text-[#0045cc] transition-colors mb-2 leading-snug"
          title={product.title}
        >
          {product.title}
        </Link>

        {/* Spec tags */}
        {product.tags && product.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {product.tags.slice(0, 3).map((tag, idx) => (
              <span
                key={idx}
                className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#eff4ff] text-[#434656]"
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        {/* Rating */}
        <div className="flex items-center gap-1 text-xs text-[#434656] mt-auto mb-3">
          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
          <span className="font-bold text-[#0b1c30]">{product.rating.toFixed(1)}</span>
          <span className="text-[11px]">({product.reviewsCount})</span>
        </div>

        {/* Price & Action button */}
        <div className="pt-2 border-t border-[#eff4ff] flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-base font-bold text-[#0b1c30] font-mono">
              {formattedPrice}
            </span>
            {formattedOriginalPrice && (
              <span className="text-xs text-[#737688] line-through font-mono">
                {formattedOriginalPrice}
              </span>
            )}
          </div>

          <span className="text-xs font-semibold text-[#0045cc] group-hover:underline flex items-center gap-0.5">
            Chi tiết
            <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
          </span>
        </div>
      </div>
    </div>
  );
}
