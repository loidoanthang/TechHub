import { ROUTES } from "@/lib/constants";
import { buyerReviewApi } from "@/features/account/api/buyerReviewApi";
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
    REVIEW: (id) => (id ? `${ROUTES.SELLER.REVIEWS}?reviewId=${id}` : ROUTES.SELLER.REVIEWS),
    SELLER_REVIEW: (id) => (id ? `${ROUTES.SELLER.REVIEWS}?reviewId=${id}` : ROUTES.SELLER.REVIEWS),
    SHOP: () => ROUTES.SELLER.SETTINGS,
    PROMOTION: () => ROUTES.SELLER.VOUCHERS,
  },
  admin: {
    ORDER: () => ROUTES.ADMIN.ORDERS,
    PRODUCT: () => ROUTES.ADMIN.PRODUCTS,
    REVIEW: () => ROUTES.ADMIN.REVIEWS,
    SELLER_REVIEW: () => ROUTES.ADMIN.REVIEWS,
    BUYER_REVIEW: () => ROUTES.ADMIN.REVIEWS,
    SHOP: (id) => (id ? `${ROUTES.ADMIN.SELLERS}/${id}` : ROUTES.ADMIN.SELLERS),
    PROMOTION: () => ROUTES.ADMIN.FINANCE,
  },
  buyer: {
    ORDER: (id) => (id ? `${ROUTES.ACCOUNT.ORDERS}/${id}` : ROUTES.ACCOUNT.ORDERS),
    PRODUCT: (id) => (id ? `${ROUTES.PRODUCTS}/${id}` : ROUTES.PRODUCTS),
    REVIEW: (id) => (id ? `${ROUTES.PRODUCTS}#reviews` : ROUTES.ACCOUNT.ORDERS),
    BUYER_REVIEW: (id) => (id ? `${ROUTES.PRODUCTS}#reviews` : ROUTES.ACCOUNT.ORDERS),
    SHOP: (id) => resolveBuyerShopRoute(id),
    PROMOTION: () => ROUTES.PRODUCTS,
  },
};

const DEFAULT_PORTAL_ROUTES: Record<PortalContext, string> = {
  admin: ROUTES.ADMIN.DASHBOARD,
  seller: ROUTES.SELLER.NOTIFICATIONS,
  buyer: ROUTES.ACCOUNT.NOTIFICATIONS,
};

function isSellerReviewNotification(notification: NotificationResponse): boolean {
  if (notification.referenceType === "SELLER_REVIEW") return true;
  if (notification.type !== "REVIEW") return false;
  const title = (notification.title || "").toLowerCase();
  const content = (notification.content || "").toLowerCase();
  return (
    title.includes("đánh giá sản phẩm mới") ||
    title.includes("sản phẩm mới") ||
    content.includes("vừa nhận được đánh giá")
  );
}

export async function resolveNotificationRouteAsync(
  notification: NotificationResponse,
  portalContext: PortalContext = "buyer"
): Promise<string> {
  // 1. Explicit SELLER_REVIEW or seller portal context or fallback detector
  if (
    notification.referenceType === "SELLER_REVIEW" ||
    portalContext === "seller" ||
    isSellerReviewNotification(notification)
  ) {
    return notification.referenceId
      ? `${ROUTES.SELLER.REVIEWS}?reviewId=${notification.referenceId}`
      : ROUTES.SELLER.REVIEWS;
  }

  // 2. Buyer REVIEW notifications (either referenceType === "BUYER_REVIEW" or type === "REVIEW" in buyer portal): fetch productSlug for direct SEO link
  const isBuyerReview =
    notification.referenceType === "BUYER_REVIEW" ||
    (portalContext === "buyer" && notification.type === "REVIEW");

  if (isBuyerReview && notification.referenceId) {
    try {
      const res = await buyerReviewApi.getReviewById(notification.referenceId);
      if (res?.data?.productSlug) {
        return `${ROUTES.PRODUCTS}/${res.data.productSlug}?highlightReviewId=${notification.referenceId}#reviews`;
      }
    } catch (err) {
      console.warn("Failed to resolve product slug for review notification:", err);
    }
    return ROUTES.ACCOUNT.ORDERS;
  }

  return resolveNotificationRoute(notification, portalContext);
}

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
