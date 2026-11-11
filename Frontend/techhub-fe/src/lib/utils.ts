import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatPrice(val: number): string {
  return new Intl.NumberFormat("vi-VN").format(val) + "₫";
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
