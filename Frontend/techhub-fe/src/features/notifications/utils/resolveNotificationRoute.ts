import { ROUTES } from "@/lib/constants";
import type { NotificationResponse, PortalContext } from "../types";

type RouteResolver = (
  referenceId: string | null | undefined,
  notification: NotificationResponse
) => string;

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function resolveBuyerShopRoute(id?: string | null): string {
  if (!id) return ROUTES.ACCOUNT.FOLLOWED_SHOPS;
  // If referenceId is a UUID, route to Followed Shops list to avoid 404 on slug-based /shops/[sellerName]
  return UUID_REGEX.test(id) ? ROUTES.ACCOUNT.FOLLOWED_SHOPS : ROUTES.SHOPS(id);
}

const ROUTE_STRATEGIES: Record<PortalContext, Record<string, RouteResolver>> = {
  seller: {
    ORDER: (id) => (id ? `${ROUTES.SELLER.ORDERS}/${id}` : ROUTES.SELLER.ORDERS),
    PRODUCT: () => ROUTES.SELLER.PRODUCTS,
    REVIEW: () => ROUTES.SELLER.REVIEWS,
    SHOP: () => ROUTES.SELLER.SETTINGS,
    PROMOTION: () => ROUTES.PRODUCTS,
  },
  admin: {
    ORDER: () => ROUTES.ADMIN.ORDERS,
    PRODUCT: (id) => (id ? `${ROUTES.PRODUCTS}/${id}` : ROUTES.PRODUCTS),
    REVIEW: () => ROUTES.ADMIN.DASHBOARD,
    SHOP: (id) => (id ? `${ROUTES.ADMIN.SELLERS}/${id}` : ROUTES.ADMIN.SELLERS),
    PROMOTION: () => ROUTES.PRODUCTS,
  },
  buyer: {
    ORDER: (id) => (id ? `${ROUTES.ACCOUNT.ORDERS}/${id}` : ROUTES.ACCOUNT.ORDERS),
    PRODUCT: (id) => (id ? `${ROUTES.PRODUCTS}/${id}` : ROUTES.PRODUCTS),
    REVIEW: (id) => (id ? `${ROUTES.PRODUCTS}/${id}#reviews` : ROUTES.ACCOUNT.ORDERS),
    SHOP: (id) => resolveBuyerShopRoute(id),
    PROMOTION: () => ROUTES.PRODUCTS,
  },
};

const DEFAULT_PORTAL_ROUTES: Record<PortalContext, string> = {
  admin: ROUTES.ADMIN.DASHBOARD,
  seller: ROUTES.SELLER.NOTIFICATIONS,
  buyer: ROUTES.ACCOUNT.NOTIFICATIONS,
};

export function resolveNotificationRoute(
  notification: NotificationResponse,
  portalContext: PortalContext = "buyer"
): string {
  const portalStrategies = ROUTE_STRATEGIES[portalContext];

  // 1. REVIEW notifications always route to review views (Seller reviews / Product #reviews)
  if (notification.type === "REVIEW" && portalStrategies?.["REVIEW"]) {
    return portalStrategies["REVIEW"](notification.referenceId, notification);
  }

  // 2. If referenceType is specified and valid, route by referenced entity (e.g. SYSTEM alert referencing a PRODUCT/ORDER)
  if (
    notification.referenceType &&
    portalStrategies?.[notification.referenceType]
  ) {
    return portalStrategies[notification.referenceType](
      notification.referenceId,
      notification
    );
  }

  // 3. Fallback to notification.type strategy (e.g. ORDER, PRODUCT, SHOP, PROMOTION)
  if (notification.type && portalStrategies?.[notification.type]) {
    return portalStrategies[notification.type](
      notification.referenceId,
      notification
    );
  }

  // 4. Default fallback to portal notification center
  return DEFAULT_PORTAL_ROUTES[portalContext] || ROUTES.ACCOUNT.NOTIFICATIONS;
}
