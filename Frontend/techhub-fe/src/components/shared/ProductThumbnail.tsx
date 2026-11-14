import React from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";

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
  return (
    <div
      className={cn(
        "relative rounded-xl overflow-hidden bg-slate-50 shrink-0 border border-slate-200/80 flex items-center justify-center",
        sizeClassName,
        className
      )}
    >
      {src ? (
        <Image
          src={src}
          alt={alt}
          fill
          priority={priority}
          className={cn("object-cover", imageClassName)}
          unoptimized
        />
      ) : (
        <span className="text-[10px] font-semibold text-slate-400">TechHub</span>
      )}
    </div>
  );
}
