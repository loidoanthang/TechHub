"use client";

import React, { useState } from "react";
import Image from "next/image";

export type UserAvatarSize = "xs" | "sm" | "md" | "lg" | "xl" | number;

export interface UserAvatarProps {
  src?: string | null;
  alt?: string;
  size?: UserAvatarSize;
  className?: string;
  fallbackSrc?: string;
}

const SIZE_MAP: Record<string, { className: string; px: number }> = {
  xs: { className: "w-6 h-6", px: 24 },
  sm: { className: "w-8 h-8", px: 32 },
  md: { className: "w-10 h-10", px: 40 },
  lg: { className: "w-16 h-16", px: 64 },
  xl: { className: "w-24 h-24", px: 96 },
};

export function UserAvatar({
  src,
  alt = "TechHub User Avatar",
  size = "md",
  className = "",
  fallbackSrc = "/images/default-avatar.svg",
}: UserAvatarProps) {
  const [imgSrc, setImgSrc] = useState<string>(src || fallbackSrc);

  React.useEffect(() => {
    setImgSrc(src || fallbackSrc);
  }, [src, fallbackSrc]);

  let sizeClass = "";
  let pixelSize = 40;

  if (typeof size === "number") {
    pixelSize = size;
  } else {
    const config = SIZE_MAP[size] || SIZE_MAP.md;
    sizeClass = config.className;
    pixelSize = config.px;
  }

  const customStyle =
    typeof size === "number"
      ? { width: `${size}px`, height: `${size}px` }
      : undefined;

  return (
    <div
      style={customStyle}
      className={`relative rounded-full overflow-hidden shrink-0 border border-slate-200/80 bg-[#eff4ff] ${sizeClass} ${className}`}
    >
      <Image
        src={imgSrc}
        alt={alt}
        fill
        sizes={`${pixelSize}px`}
        className="object-cover"
        onError={() => setImgSrc(fallbackSrc)}
        unoptimized
      />
    </div>
  );
}
