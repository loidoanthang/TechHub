"use client";

import React, { useState } from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";
import { Package } from "lucide-react";

export interface ProductThumbnailProps {
  src?: string | null;
  alt: string;
  className?: string;
  sizeClassName?: string;
  imageClassName?: string;
  priority?: boolean;
}

export function ProductThumbnail({
  src,
  alt,
  className,
  sizeClassName = "w-16 h-16",
  imageClassName = "object-cover",
  priority = false,
}: ProductThumbnailProps) {
  const [hasError, setHasError] = useState(false);

  return (
    <div
      className={cn(
        "relative rounded-xl overflow-hidden bg-slate-50 shrink-0 border border-slate-200/80 flex items-center justify-center",
        sizeClassName,
        className
      )}
    >
      {src && !hasError ? (
        <Image
          src={src}
          alt={alt}
          fill
          priority={priority}
          className={cn("object-cover", imageClassName)}
          onError={() => setHasError(true)}
          unoptimized
        />
      ) : (
        <div className="flex flex-col items-center justify-center gap-0.5 text-slate-400">
          <Package className="w-4 h-4 text-slate-300" />
          <span className="text-[9px] font-semibold tracking-tight text-slate-400">TechHub</span>
        </div>
      )}
    </div>
  );
}
