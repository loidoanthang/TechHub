"use client";

import React, { useRef, useState, useEffect, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface ScrollableTabsWrapperProps {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
}

export function ScrollableTabsWrapper({
  children,
  className = "",
  containerClassName = "",
}: ScrollableTabsWrapperProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);

  const checkScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;

    // Small threshold to prevent precision issues
    const atLeft = el.scrollLeft <= 4;
    const atRight = el.scrollLeft + el.clientWidth >= el.scrollWidth - 4;

    setCanScrollLeft(!atLeft);
    setCanScrollRight(!atRight);
  }, []);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;

    checkScroll();

    el.addEventListener("scroll", checkScroll, { passive: true });
    window.addEventListener("resize", checkScroll);

    // Observer to recheck if children size changes
    const observer = new ResizeObserver(checkScroll);
    observer.observe(el);

    return () => {
      el.removeEventListener("scroll", checkScroll);
      window.removeEventListener("resize", checkScroll);
      observer.disconnect();
    };
  }, [checkScroll]);

  const scrollBy = (offset: number) => {
    if (scrollRef.current) {
      scrollRef.current.scrollBy({ left: offset, behavior: "smooth" });
    }
  };

  return (
    <div className={`relative w-full group ${containerClassName}`}>
      {/* Slow pulsing Left Chevron indicator for Mobile/Overflow */}
      {canScrollLeft && (
        <button
          type="button"
          onClick={() => scrollBy(-150)}
          className="sm:hidden absolute left-0 top-1/2 -translate-y-1/2 z-20 p-1 flex items-center justify-center text-[#0045cc] animate-pulse cursor-pointer pointer-events-auto"
          aria-label="Cuộn sang trái"
        >
          <ChevronLeft className="w-4 h-4 stroke-[2.5]" />
        </button>
      )}

      {/* Main Scrollable Row */}
      <div
        ref={scrollRef}
        className={`flex items-center gap-1.5 overflow-x-auto no-scrollbar scroll-smooth w-full ${className}`}
      >
        {children}
      </div>

      {/* Slow pulsing Right Chevron indicator for Mobile/Overflow */}
      {canScrollRight && (
        <button
          type="button"
          onClick={() => scrollBy(150)}
          className="sm:hidden absolute right-0 top-1/2 -translate-y-1/2 z-20 p-1 flex items-center justify-center text-[#0045cc] animate-pulse cursor-pointer pointer-events-auto"
          aria-label="Cuộn sang phải"
        >
          <ChevronRight className="w-4 h-4 stroke-[2.5]" />
        </button>
      )}
    </div>
  );
}
