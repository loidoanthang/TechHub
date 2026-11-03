"use client";

import Link from "next/link";
import {
  Laptop,
  Keyboard,
  Mouse,
  Headphones,
  Monitor,
  Cpu,
  Layers,
  Volume2,
  ChevronLeft,
  ChevronRight,
  Loader2,
} from "lucide-react";
import { useRef } from "react";
import { useCategoriesQuery } from "@/features/products/hooks/useProductQueries";
import { MOCK_HOME_CATEGORIES } from "@/mocks";

const ICON_MAP: Record<string, React.ElementType> = {
  Laptop,
  Keyboard,
  Mouse,
  Headphones,
  Monitor,
  Cpu,
  Layers,
  Volume2,
};

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
    <section className="w-full bg-[#eff4ff] py-16">
      <div className="max-w-6xl mx-auto px-6 lg:px-12">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <h2 className="text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight font-[family-name:var(--font-heading)]">
              Categories
            </h2>
            <p className="text-xs text-[#434656] mt-1">
              Explore specialized hardware components by category
            </p>
          </div>

          {/* Left / Right Carousel Controls */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => scroll("left")}
              aria-label="Previous Category"
              className="w-10 h-10 rounded-lg bg-white text-[#0b1c30] hover:bg-[#dce9ff] transition-colors flex items-center justify-center shadow-sm cursor-pointer"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              onClick={() => scroll("right")}
              aria-label="Next Category"
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
              <span>Loading categories...</span>
            </div>
          ) : hasServerData ? (
            serverCategories.map((cat, idx) => {
              const Icon = Object.values(ICON_MAP)[idx % Object.values(ICON_MAP).length] || Laptop;
              return (
                <Link
                  key={cat.id}
                  href={`/search?categoryId=${cat.id}`}
                  className="flex-shrink-0 w-48 bg-white rounded-xl p-5 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all group flex flex-col justify-between"
                >
                  <div className="w-12 h-12 rounded-lg bg-[#dce9ff] flex items-center justify-center text-[#0045cc] mb-6 group-hover:bg-[#0045cc] group-hover:text-white transition-colors">
                    <Icon className="w-6 h-6" />
                  </div>
                  <div>
                    <h4 className="font-semibold text-sm text-[#0b1c30] mb-1 line-clamp-1">
                      {cat.name}
                    </h4>
                    <span className="text-xs text-[#737688]">
                      {cat.description || "Browse Gear"}
                    </span>
                  </div>
                </Link>
              );
            })
          ) : (
            MOCK_HOME_CATEGORIES.map((cat, idx) => {
              const Icon = ICON_MAP[cat.iconName] || Laptop;
              return (
                <Link
                  key={idx}
                  href={`/search?category=${cat.slug}`}
                  className="flex-shrink-0 w-48 bg-white rounded-xl p-5 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all group flex flex-col justify-between"
                >
                  <div className="w-12 h-12 rounded-lg bg-[#dce9ff] flex items-center justify-center text-[#0045cc] mb-6 group-hover:bg-[#0045cc] group-hover:text-white transition-colors">
                    <Icon className="w-6 h-6" />
                  </div>
                  <div>
                    <h4 className="font-semibold text-sm text-[#0b1c30] mb-1 line-clamp-1">
                      {cat.name}
                    </h4>
                    <span className="text-xs text-[#737688]">
                      {cat.count ? `${cat.count} Items` : "Enthusiast Choice"}
                    </span>
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
