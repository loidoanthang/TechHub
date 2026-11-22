export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

//Web Socket
export const WS_CHAT_URL =
  process.env.NEXT_PUBLIC_WS_URL ||
  (API_BASE_URL.replace(/^http/, "ws") + "/ws-chat");

export const GOOGLE_CLIENT_ID =
  process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "";

export const STORAGE_KEYS = {
  ACCESS_TOKEN: "accessToken",
  REFRESH_TOKEN: "refreshToken",
  USER: "techhub_user",
  PENDING_EMAIL: "techhub_pending_email",
  PENDING_FLOW: "techhub_pending_flow",
  PENDING_RESET_TOKEN: "techhub_pending_reset_token",
} as const;

export const API_ENDPOINTS = {
  CONVERSATIONS: {
    BASE: "/api/conversations",
    MESSAGES: (id: string) => `/api/conversations/${id}/messages`,
    DELETE: (id: string) => `/api/conversations/${id}`,
  },
  AUTH: {
    LOGIN: "/api/auth/login",
    REGISTER: "/api/auth/register",
    VERIFY_EMAIL: "/api/auth/verify-email",
    REFRESH: "/api/auth/refresh",
    LOGOUT: "/api/auth/logout",
    FORGOT_PASSWORD: "/api/auth/forgot-password",
    RESET_PASSWORD: "/api/auth/reset-password",
    CHANGE_PASSWORD: "/api/auth/change-password",
    RESEND_VERIFICATION: "/api/auth/resend-verification",
    DELETE_ACCOUNT: "/api/auth/delete-account",
  },
  USER: {
    PROFILE: "/api/users/profile",
  },
  ADDRESSES: {
    BASE: "/api/addresses",
    DETAIL: (id: string) => `/api/addresses/${id}`,
    DEFAULT: (id: string) => `/api/addresses/${id}/default`,
  },
  CATEGORIES: {
    BASE: "/api/categories",
    ATTRIBUTES: (id: string) => `/api/categories/${id}/attributes`,
  },
  PRODUCTS: {
    BASE: "/api/products",
    BY_SLUG: (slug: string) => `/api/products/${slug}`,
    RECORD_VIEW: (id: string) => `/api/products/${id}/view`,
    RECENTLY_VIEWED: "/api/products/recently-viewed",
    SIMILAR: (id: string) => `/api/products/${id}/similar`,
  },
  CART: {
    BASE: "/api/cart",
    COUNT: "/api/cart/count",
    ITEMS: "/api/cart/items",
    ITEM: (id: string) => `/api/cart/items/${id}`,
    PREVIEW: "/api/cart/preview",
  },
  CHECKOUT: {
    PREVIEW: "/api/checkout/preview",
    PLACE_ORDER: "/api/checkout/place-order",
  },
  BUYER: {
    PAYMENT_METHODS: "/api/payment-methods",
    PAYMENT_STATUS: (id: string) => `/api/payments/${id}/status`,
    PAYMENT_INFO: "/api/users/payment-info",
    PAYMENT_INFO_LINK_INIT: "/api/users/payment-info/link-init",
    PAYMENT_INFO_DEFAULT: (id: string) => `/api/users/payment-info/${id}/default`,
    PAYMENT_INFO_DELETE: (id: string) => `/api/users/payment-info/${id}`,
    ORDERS: "/api/orders",
    ORDER_DETAIL: (id: string) => `/api/orders/${id}`,
    ORDER_COMPLETE: (id: string) => `/api/orders/${id}/complete`,
    ORDER_CANCEL: (id: string) => `/api/orders/${id}/cancel`,
    ORDER_REORDER: (id: string) => `/api/orders/${id}/reorder`,
    ORDER_CANCELLATION_REQ: (id: string) => `/api/orders/${id}/cancellation-request`,
    ORDER_RETURN_REQ: (id: string) => `/api/orders/${id}/return-request`,
    RETURN_REQUESTS: "/api/return-requests",
    RETURN_REQUEST_DETAIL: (id: string) => `/api/return-requests/${id}`,
    RETURN_REQUEST_CANCEL: (id: string) => `/api/return-requests/${id}/cancel`,
  },
  PAYMENT: {
    VNPAY_CALLBACK: "/api/payment/vnpay/callback",
    VNPAY_RETRY: (paymentId: string) => `/api/payment/vnpay/retry/${paymentId}`,
    VNPAY_RETRY_ORDER: (orderId: string) => `/api/payment/vnpay/retry-order/${orderId}`,
    VNPAY_QUERY: (paymentId: string) => `/api/payment/vnpay/query/${paymentId}`,
    VNPAY_REFUND: (paymentId: string) => `/api/payment/vnpay/refund/${paymentId}`,
  },
  SELLER: {
    REGISTER: "/api/seller/register",
    VERIFICATION_STATUS: "/api/seller/verification-status",
    DASHBOARD_METRICS: "/api/seller/dashboard/metrics",
    PROFILE: "/api/seller/profile",
    SHOP: "/api/seller/shop",
    SHOP_STATUS: "/api/seller/shop/status",
    WAREHOUSE_ADDRESS: "/api/seller/shop/address",
    SHIPPING_OPTION: "/api/seller/shop/shipping-option",
    PRODUCTS: "/api/seller/products",
    PRODUCT_DETAIL: (id: string) => `/api/seller/products/${id}`,
    PRODUCT_VARIANTS: (id: string) => `/api/seller/products/${id}/variants`,
    PRODUCT_VARIANT_DETAIL: (productId: string, variantId: string) =>
      `/api/seller/products/${productId}/variants/${variantId}`,
    SUBMIT_APPROVAL: (id: string) => `/api/seller/products/${id}/submit-approval`,
    VISIBILITY: (id: string) => `/api/seller/products/${id}/visibility`,
    ORDERS: "/api/seller/orders",
    ORDER_DETAIL: (id: string) => `/api/seller/orders/${id}`,
    ORDER_STATUS: (id: string) => `/api/seller/orders/${id}/status`,
    ORDER_CANCEL: (id: string) => `/api/seller/orders/${id}/cancel`,
    CANCELLATION_REQUESTS: "/api/seller/orders/cancellation-requests",
    PROCESS_CANCELLATION: (id: string) => `/api/seller/orders/cancellation-requests/${id}/process`,
    RETURN_REQUESTS: "/api/seller/return-requests",
    RETURN_REQUEST_DETAIL: (id: string) => `/api/seller/return-requests/${id}`,
    PROCESS_RETURN: (id: string) => `/api/seller/return-requests/${id}/process`,
    VOUCHERS: "/api/seller/vouchers",
    VOUCHER_DETAIL: (id: string) => `/api/seller/vouchers/${id}`,
    VOUCHER_STATUS: (id: string) => `/api/seller/vouchers/${id}/status`,
    WALLET_WITHDRAW: "/api/seller/wallet/withdraw",
    WALLET_TRANSACTIONS: "/api/seller/wallet/transactions",
  },
  SHOPS: {
    DETAIL: (sellerName: string) => `/api/shops/${sellerName}`,
    FOLLOW: (sellerName: string) => `/api/shops/${sellerName}/follow`,
    VOUCHERS: (sellerName: string) => `/api/shops/${sellerName}/vouchers`,
  },
  BUYER_FOLLOWED_SHOPS: "/api/followed-shops",
  WISHLIST: {
    BASE: "/api/wishlist",
    ITEM: (productId: string) => `/api/wishlist/${productId}`,
  },
  VOUCHERS: {
    PLATFORM: "/api/vouchers/platform",
  },
  UPLOAD: {
    AVATAR: "/api/upload/avatar",
    SHOP_AVATAR: (shopId: number | string) => `/api/upload/shops/${shopId}/avatar`,
    SHOP_BANNER: (shopId: number | string) => `/api/upload/shops/${shopId}/banner`,
    PRODUCTS: "/api/upload/products",
    REVIEWS: "/api/upload/reviews",
    RETURNS: "/api/upload/returns",
    CHAT: "/api/upload/chat",
  },
  NOTIFICATIONS: {
    BASE: "/api/notifications",
    READ: (id: string) => `/api/notifications/${id}/read`,
    READ_ALL: "/api/notifications/read-all",
    DELETE: (id: string) => `/api/notifications/${id}`,
    STREAM: "/api/notifications/stream",
  },
  REVIEWS: {
    PRODUCT_REVIEWS: (productId: string) => `/api/products/${productId}/reviews`,
    PRODUCT_RATING_SUMMARY: (productId: string) => `/api/products/${productId}/rating-summary`,
    TOGGLE_HELPFUL: (reviewId: string) => `/api/reviews/${reviewId}/helpful`,
    BUYER_CREATE: "/api/reviews",
    BUYER_UPDATE: (id: string) => `/api/reviews/${id}`,
    SELLER_REVIEWS: "/api/seller/reviews",
    SELLER_RESPOND: (id: string) => `/api/seller/reviews/${id}/response`,
  },
  ADMIN: {
    DASHBOARD_METRICS: "/api/admin/dashboard/metrics",
    PRODUCTS: "/api/admin/products",
    PRODUCT_DETAIL: (id: string) => `/api/admin/products/${id}`,
    PRODUCT_APPROVAL: (id: string) => `/api/admin/products/${id}/approval`,
    PRODUCT_SUSPEND: (id: string) => `/api/admin/products/${id}/suspend`,
    SELLERS: "/api/admin/sellers",
    SELLER_DETAIL: (id: string) => `/api/admin/sellers/${id}`,
    SELLER_APPROVAL: (id: string) => `/api/admin/sellers/${id}/approval`,
    SHOPS: "/api/admin/shops",
    SHOP_DETAIL: (id: string) => `/api/admin/shops/${id}`,
    SHOP_STATUS: (id: string) => `/api/admin/shops/${id}/status`,
    CATEGORIES: "/api/admin/categories",
    CATEGORY_DETAIL: (id: string) => `/api/admin/categories/${id}`,
    CATEGORY_STATUS: (id: string) => `/api/admin/categories/${id}/status`,
    CATEGORY_ATTRIBUTES: (id: string) => `/api/admin/categories/${id}/attributes`,
    CATEGORY_ATTRIBUTE_DETAIL: (categoryId: string, attributeId: string) =>
      `/api/admin/categories/${categoryId}/attributes/${attributeId}`,
    ORDERS: "/api/admin/orders",
    ORDER_DETAIL: (id: string) => `/api/admin/orders/${id}`,
    ORDER_CANCEL: (id: string) => `/api/admin/orders/${id}/cancel`,
    USERS: "/api/admin/users",
    USER_STATUS: (id: string) => `/api/admin/users/${id}/status`,
    VOUCHERS: "/api/admin/vouchers",
    VOUCHER_DETAIL: (id: string) => `/api/admin/vouchers/${id}`,
    VOUCHER_STATUS: (id: string) => `/api/admin/vouchers/${id}/status`,
    PAYOUTS: "/api/admin/payouts",
    PAYOUT_PROCESS: (id: string) => `/api/admin/payouts/${id}/process`,
    POLICIES: "/api/admin/policies",
    NOTIFICATIONS_BROADCAST: "/api/admin/notifications/broadcast",
    SHIPPING_SIMULATE: "/api/admin/shipping/simulate-advance",
    REVIEWS: "/api/admin/reviews",
    REVIEW_DETAIL: (id: string) => `/api/admin/reviews/${id}`,
  },
} as const;

export const ENDPOINTS = API_ENDPOINTS;

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  VERIFY_OTP: "/verify-otp",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  SUSPENDED: "/suspended",
  PRODUCTS: "/products",
  SEARCH: "/search",
  CART: "/cart",
  CHECKOUT: "/checkout",
  PAYMENT_CALLBACK: "/checkout/payment-callback",
  ACCOUNT: {
    PROFILE: "/account/profile",
    ORDERS: "/account/orders",
    ORDER_DETAIL: (orderId: string) => `/account/orders/${orderId}`,
    ADDRESSES: "/account/addresses",
    PAYMENT_METHODS: "/account/payment-methods",
    SECURITY: "/account/security",
    WISHLIST: "/account/wishlist",
    RECENTLY_VIEWED: "/account/recently-viewed",
    FOLLOWED_SHOPS: "/account/followed-shops",
    NOTIFICATIONS: "/account/notifications",
    MESSAGES: "/account/messages",
  },
  SELLER: {
    DASHBOARD: "/seller/dashboard",
    REGISTER: "/seller/register",
    PRODUCTS: "/seller/products",
    NEW_PRODUCT: "/seller/products/new",
    ORDERS: "/seller/orders",
    ORDER_DETAIL: (id: string) => `/seller/orders/${id}`,
    REVIEWS: "/seller/reviews",
    WALLET: "/seller/wallet",
    VOUCHERS: "/seller/vouchers",
    SETTINGS: "/seller/settings",
    NOTIFICATIONS: "/seller/notifications",
    CHAT: "/seller/chat",
  },
  ADMIN: {
    DASHBOARD: "/admin/dashboard",
    PRODUCTS: "/admin/products",
    REVIEWS: "/admin/reviews",
    SELLERS: "/admin/sellers",
    CATEGORIES: "/admin/categories",
    ORDERS: "/admin/orders",
    USERS: "/admin/users",
    FINANCE: "/admin/finance",
    SETTINGS: "/admin/settings",
  },
  SHOPS: (sellerName: string) => `/shops/${sellerName}`,
} as const;

import type { CancellationReasonCode } from "@/types/enums";

export interface CancellationReasonOption {
  code: CancellationReasonCode;
  label: string;
}

export const BUYER_CANCELLATION_REASONS: CancellationReasonOption[] = [
  {
    code: "BUYER_CHANGED_MIND",
    label: "Tôi thay đổi ý định / Không có nhu cầu mua nữa",
  },
  {
    code: "BUYER_FOUND_CHEAPER_PRICE",
    label: "Tìm thấy sản phẩm với mức giá tốt hơn ở nơi khác",
  },
  {
    code: "BUYER_INCORRECT_ADDRESS",
    label: "Thông tin địa chỉ nhận hàng chưa chính xác / Cần cập nhật",
  },
  {
    code: "BUYER_PAYMENT_ISSUE",
    label: "Gặp sự cố khi thanh toán / Muốn đổi phương thức thanh toán",
  },
  {
    code: "OTHER",
    label: "Lý do khác",
  },
];

export const SELLER_CANCELLATION_REASONS: CancellationReasonOption[] = [
  {
    code: "SELLER_OUT_OF_STOCK",
    label: "Sản phẩm tạm hết hàng trong kho",
  },
  {
    code: "SELLER_FORCE_MAJEURE",
    label: "Sự cố bất khả kháng từ cửa hàng / Vận hành gian hàng",
  },
  {
    code: "BUYER_INCORRECT_ADDRESS",
    label: "Không liên lạc được hoặc thông tin địa chỉ người mua không hợp lệ",
  },
  {
    code: "OTHER",
    label: "Lý do khác",
  },
];

export const ADMIN_CANCELLATION_REASONS: CancellationReasonOption[] = [
  {
    code: "SELLER_FORCE_MAJEURE",
    label: "Người bán vi phạm quy định / Sự cố vận hành gian hàng",
  },
  {
    code: "SELLER_OUT_OF_STOCK",
    label: "Người bán hết hàng / Không thể bàn giao sản phẩm",
  },
  {
    code: "BUYER_CHANGED_MIND",
    label: "Theo yêu cầu giải quyết tranh chấp cho Người mua",
  },
  {
    code: "BUYER_INCORRECT_ADDRESS",
    label: "Địa chỉ nhận hàng không hợp lệ / Gian lận thông tin",
  },
  {
    code: "BUYER_PAYMENT_ISSUE",
    label: "Sự cố giao dịch thanh toán / Gian lận tài chính",
  },
  {
    code: "OTHER",
    label: "Quyết định can thiệp khác của Quản trị viên",
  },
];
