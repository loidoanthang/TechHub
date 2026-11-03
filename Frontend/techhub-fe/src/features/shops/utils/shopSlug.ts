export const KNOWN_SHOP_SLUG_MAP: Record<string, string> = {
  "techhub-flagship-store": "techhub-official",
  "techhub-flagship": "techhub-official",
  "techhub-store": "techhub-official",
  "techhub": "techhub-official",
  "gearzone-gaming-store": "gearzone-gaming",
  "gearzone-store": "gearzone-gaming",
  "gearzone": "gearzone-gaming",
};

/**
 * Resolves a shop display name or raw slug into the canonical sellerName
 * accepted by the Backend API (e.g. "TechHub Flagship Store" -> "techhub-official").
 */
export function resolveSellerSlug(shopNameOrSlug?: string | null): string {
  if (!shopNameOrSlug) return "techhub-official";

  const rawSlug = shopNameOrSlug
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

  return KNOWN_SHOP_SLUG_MAP[rawSlug] || rawSlug;
}
