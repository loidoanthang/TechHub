"use client";

import React, { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { Package } from "lucide-react";
import type { SellerProductSummaryResponse } from "@/types/sellerProduct";

interface ProductHoverCardProps {
  children: React.ReactNode;
  product: SellerProductSummaryResponse;
}

export function ProductHoverCard({
  children,
  product,
}: ProductHoverCardProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [coords, setCoords] = useState<{
    top: number;
    left: number;
    placeAbove: boolean;
  } | null>(null);
  const [imageError, setImageError] = useState(false);

  const triggerRef = useRef<HTMLDivElement>(null);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleMouseEnter = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    if (!triggerRef.current) return;

    const rect = triggerRef.current.getBoundingClientRect();
    const modalWidth = 340;
    const estimatedHeight = 90;

    // Determine whether to place above or below based on viewport space
    const spaceBelow = window.innerHeight - rect.bottom;
    const placeAbove = spaceBelow < estimatedHeight + 15 && rect.top > estimatedHeight + 15;

    let top = placeAbove ? rect.top - estimatedHeight - 8 : rect.bottom + 6;
    let left = rect.left;

    // Prevent overflowing viewport on the right
    if (left + modalWidth > window.innerWidth - 16) {
      left = Math.max(16, window.innerWidth - modalWidth - 16);
    }
    if (left < 16) left = 16;

    setCoords({ top, left, placeAbove });
    setIsOpen(true);
  };

  const handleMouseLeave = () => {
    timeoutRef.current = setTimeout(() => {
      setIsOpen(false);
    }, 60);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  return (
    <div
      ref={triggerRef}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      className="block min-w-0 max-w-full"
    >
      {children}

      {isOpen &&
        coords &&
        typeof document !== "undefined" &&
        createPortal(
          <div
            style={{
              position: "fixed",
              top: `${coords.top}px`,
              left: `${coords.left}px`,
              width: "340px",
              zIndex: 99999,
            }}
            className="pointer-events-none animate-in fade-in-0 zoom-in-95 duration-100"
          >
            <div className="bg-white rounded-2xl p-3 shadow-2xl border border-slate-200/90 text-slate-900 flex items-start gap-3 backdrop-blur-md">
              {/* Product Thumbnail Preview (64x64px) */}
              <div className="w-16 h-16 rounded-xl bg-slate-50 border border-slate-200 shrink-0 overflow-hidden relative flex items-center justify-center shadow-2xs">
                {product.coverImageUrl && !imageError ? (
                  <img
                    src={product.coverImageUrl}
                    alt={product.name}
                    onError={() => setImageError(true)}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Package className="w-7 h-7 text-slate-400" />
                )}
              </div>

              {/* Product Metadata & Full Name */}
              <div className="flex-1 min-w-0 flex flex-col gap-1">
                <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-medium">
                  {product.brandName && (
                    <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-[#0045cc]">
                      {product.brandName}
                    </span>
                  )}
                  <span className="truncate">{product.categoryName || "Linh kiện điện tử"}</span>
                </div>

                {/* Full Product Name without truncation */}
                <p className="font-bold text-xs text-slate-900 leading-snug whitespace-normal">
                  {product.name}
                </p>
              </div>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
}
