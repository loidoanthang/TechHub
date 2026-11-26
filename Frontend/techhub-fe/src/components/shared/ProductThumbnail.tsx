"use client";

import React, { useState } from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";
import { ImageIcon } from "lucide-react";

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
  const isCompactSize = /\bw-(?:8|9|10|11|12|14)\b/.test(sizeClassName);

  return (
    <div
      className={cn(
        "relative rounded-xl overflow-hidden bg-white shrink-0 border border-slate-200/80 flex items-center justify-center",
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
      ) : isCompactSize ? (
        <div
          className="w-full h-full flex items-center justify-center bg-slate-50/80 text-slate-400 select-none"
          title="Chưa có ảnh"
          aria-label="Chưa có ảnh"
        >
          <ImageIcon className="w-4 h-4 stroke-[1.75]" />
        </div>
      ) : (
        <div className="w-full h-full flex flex-col items-center justify-center gap-1.5 p-2 text-slate-400 bg-slate-50/70 select-none">
          <div className="w-8 h-8 rounded-lg bg-white border border-slate-200/60 shadow-2xs flex items-center justify-center text-slate-400">
            <ImageIcon className="w-4 h-4 stroke-[1.75]" />
          </div>
          <span className="text-[10px] font-medium text-slate-500 text-center tracking-tight leading-tight">
            Chưa có ảnh
          </span>
        </div>
      )}
    </div>
  );
}
