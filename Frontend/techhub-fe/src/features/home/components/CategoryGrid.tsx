"use client";

import Link from "next/link";
import {
  ChevronLeft,
  ChevronRight,
  ArrowUpRight,
  Loader2,
} from "lucide-react";
import { useRef } from "react";
import { useCategoriesQuery } from "@/features/products/hooks/useProductQueries";
import { MOCK_HOME_CATEGORIES } from "@/mocks";

export function CategoryGrid() {
  const scrollRef = useRef<HTMLDivElement>(null);
  const { data: serverCategories = [], isLoading } = useCategoriesQuery();

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const scrollAmount = 300;
      scrollRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      });
    }
  };

  const hasServerData = serverCategories.length > 0;

  return (
    <section className="w-full bg-[#eff4ff] py-10 sm:py-16">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-12">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-6 sm:mb-8 gap-4">
          <div>
            <h2 className="text-xl sm:text-2xl lg:text-3xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
              Danh mục sản phẩm
            </h2>
            <p className="text-xs text-[#434656] mt-1">
              Khám phá thiết bị và linh kiện công nghệ theo từng danh mục
            </p>
          </div>

          {/* Left / Right Carousel Controls */}
          <div className="hidden sm:flex items-center gap-2">
            <button
              onClick={() => scroll("left")}
              aria-label="Danh mục trước"
              className="w-10 h-10 rounded-lg bg-white text-[#0b1c30] hover:bg-[#dce9ff] transition-colors flex items-center justify-center shadow-sm cursor-pointer"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              onClick={() => scroll("right")}
              aria-label="Danh mục tiếp theo"
              className="w-10 h-10 rounded-lg bg-white text-[#0b1c30] hover:bg-[#dce9ff] transition-colors flex items-center justify-center shadow-sm cursor-pointer"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Carousel Row */}
        <div
          ref={scrollRef}
          className="flex gap-4 overflow-x-auto scroll-smooth pb-4 pt-1"
          style={{ scrollbarWidth: "none" }}
        >
          {isLoading && !hasServerData ? (
            <div className="w-full py-8 flex items-center justify-center gap-2 text-xs text-[#737688]">
              <Loader2 className="w-4 h-4 animate-spin text-[#0045cc]" />
              <span>Đang tải danh mục...</span>
            </div>
          ) : hasServerData ? (
            serverCategories.map((cat) => {
              return (
                <Link
                  key={cat.id}
                  href={`/search?categoryId=${cat.id}`}
                  className="group relative flex-shrink-0 w-44 sm:w-48 bg-white rounded-2xl p-5 shadow-xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 border border-slate-200/80 hover:border-primary/40 flex flex-col justify-between overflow-hidden"
                >
                  {/* Subtle Brand Accent Bar - Only active on hover */}
                  <div className="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary to-blue-700 opacity-0 group-hover:opacity-100 group-hover:h-1 transition-all duration-200" />

                  {/* Main Category Info */}
                  <div className="pt-1">
                    <span className="text-[10px] font-mono font-semibold tracking-wider text-slate-400 group-hover:text-primary transition-colors uppercase">
                      Danh mục
                    </span>
                    <h3 className="font-bold text-sm text-[#0b1c30] group-hover:text-primary transition-colors line-clamp-2 mt-1 leading-snug">
                      {cat.name}
                    </h3>
                  </div>

                  {/* Bottom Action Footer */}
                  <div className="pt-3 mt-3 flex items-center justify-between border-t border-slate-100">
                    <span className="text-[11px] font-medium text-slate-400 group-hover:text-primary transition-colors">
                      Khám phá
                    </span>
                    <div className="w-6 h-6 rounded-full bg-slate-50 group-hover:bg-primary group-hover:text-white text-slate-400 flex items-center justify-center transition-colors">
                      <ArrowUpRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                    </div>
                  </div>
                </Link>
              );
            })
          ) : (
            MOCK_HOME_CATEGORIES.map((cat, idx) => {
              return (
                <Link
                  key={idx}
                  href={`/search?category=${cat.slug}`}
                  className="group relative flex-shrink-0 w-44 sm:w-48 bg-white rounded-2xl p-5 shadow-xs hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 border border-slate-200/80 hover:border-primary/40 flex flex-col justify-between overflow-hidden"
                >
                  {/* Subtle Brand Accent Bar - Only active on hover */}
                  <div className="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-primary to-blue-700 opacity-0 group-hover:opacity-100 group-hover:h-1 transition-all duration-200" />

                  {/* Main Category Info */}
                  <div className="pt-1">
                    <span className="text-[10px] font-mono font-semibold tracking-wider text-slate-400 group-hover:text-primary transition-colors uppercase">
                      Danh mục
                    </span>
                    <h3 className="font-bold text-sm text-[#0b1c30] group-hover:text-primary transition-colors line-clamp-2 mt-1 leading-snug">
                      {cat.name}
                    </h3>
                  </div>

                  {/* Bottom Action Footer */}
                  <div className="pt-3 mt-3 flex items-center justify-between border-t border-slate-100">
                    <span className="text-[11px] font-medium text-slate-400 group-hover:text-primary transition-colors">
                      Khám phá
                    </span>
                    <div className="w-6 h-6 rounded-full bg-slate-50 group-hover:bg-primary group-hover:text-white text-slate-400 flex items-center justify-center transition-colors">
                      <ArrowUpRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                    </div>
                  </div>
                </Link>
              );
            })
          )}
        </div>
      </div>
    </section>
  );
}
