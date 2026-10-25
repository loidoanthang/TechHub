import Link from "next/link";
import Image from "next/image";
import { Star, ShieldCheck, ShoppingCart } from "lucide-react";
import type { Product } from "@/types/product";

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const formattedPrice = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(product.price);

  const formattedOriginalPrice = product.originalPrice
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(product.originalPrice)
    : null;

  return (
    <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col justify-between hover:shadow-lg hover:-translate-y-1 transition-all duration-200 group relative border border-[#eff4ff]">
      {/* Product Image viewport */}
      <Link
        href={`/products/${product.id}`}
        className="relative w-full aspect-square rounded-lg bg-[#eff4ff] overflow-hidden mb-3 p-4 flex items-center justify-center"
      >
        <Image
          src={product.imageUrl}
          alt={product.title}
          fill
          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
          className="object-contain p-2 group-hover:scale-105 transition-transform duration-300"
          unoptimized
        />

        {/* Discount Badge */}
        {product.discountPercentage && (
          <span className="absolute top-2 left-2 bg-[#a90900] text-white text-[11px] font-bold px-2 py-0.5 rounded-full z-10">
            -{product.discountPercentage}%
          </span>
        )}
      </Link>

      {/* Product Details */}
      <div className="flex flex-col flex-1">
        {/* Vendor tag */}
        <div className="flex items-center gap-1.5 text-[11px] text-[#434656] mb-1">
          <span className="font-semibold truncate">{product.vendorName}</span>
          {product.isVerifiedVendor && (
            <ShieldCheck className="w-3.5 h-3.5 text-[#0045cc] flex-shrink-0" />
          )}
        </div>

        {/* Title */}
        <Link
          href={`/products/${product.id}`}
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

          <button
            aria-label="Add to Cart"
            className="w-9 h-9 rounded-lg bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white flex items-center justify-center transition-colors shadow-sm"
          >
            <ShoppingCart className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
