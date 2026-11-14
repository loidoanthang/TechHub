import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const VI_NUMBER_FORMATTER = new Intl.NumberFormat("vi-VN");

export function formatPrice(val: number): string {
  return VI_NUMBER_FORMATTER.format(val) + "₫";
}

/**
 * Formats a number to Vietnamese currency string with dots (e.g., 19990000 -> "19.990.000")
 */
export function formatCurrencyVND(
  value: number | string | undefined | null,
  allowZero = false
): string {
  if (value === undefined || value === null || value === "") return "";
  const num =
    typeof value === "number"
      ? value
      : parseInt(String(value).replace(/\D/g, ""), 10);
  if (isNaN(num)) return "";
  if (num === 0) return allowZero ? "0" : "";
  return num.toLocaleString("vi-VN");
}

/**
 * Parses a string formatted with dots or any non-digits into a clean integer number
 * e.g. "19.990.000" -> 19990000
 */
export function parseCurrencyVND(
  value: string | number | undefined | null
): number {
  if (value === undefined || value === null || value === "") return 0;
  if (typeof value === "number") return isNaN(value) ? 0 : value;
  const clean = String(value).replace(/\D/g, "");
  return clean ? parseInt(clean, 10) : 0;
}

/**
 * Downscales remote CDN thumbnails (Unsplash / Cloudinary) so small table avatars
 * do not decode multi-megapixel bitmaps on the GPU/raster thread while scrolling.
 */
export function getThumbnailUrl(
  url: string | undefined | null,
  size = 120
): string {
  if (!url) return "";
  try {
    const parsed = new URL(url);
    if (parsed.hostname.includes("images.unsplash.com")) {
      parsed.searchParams.set("auto", "format");
      parsed.searchParams.set("fit", "crop");
      parsed.searchParams.set("w", String(size));
      parsed.searchParams.set("h", String(size));
      parsed.searchParams.set("q", "75");
      return parsed.toString();
    }
    if (
      parsed.hostname.includes("res.cloudinary.com") &&
      parsed.pathname.includes("/upload/") &&
      !parsed.pathname.includes("/upload/w_")
    ) {
      parsed.pathname = parsed.pathname.replace(
        "/upload/",
        `/upload/w_${size},h_${size},c_fill,q_auto,f_auto/`
      );
      return parsed.toString();
    }
  } catch {
    return url;
  }
  return url;
}

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isValidUuid(val: string | undefined | null): boolean {
  if (!val) return false;
  return UUID_REGEX.test(val);
}
