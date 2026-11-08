export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

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
    PAYMENT_METHODS: "/api/buyer/payment-methods",
    PAYMENT_STATUS: (id: string) => `/api/buyer/payments/${id}/status`,
    ORDERS: "/api/buyer/orders",
    ORDER_DETAIL: (id: string) => `/api/buyer/orders/${id}`,
    ORDER_COMPLETE: (id: string) => `/api/buyer/orders/${id}/complete`,
    ORDER_CANCEL: (id: string) => `/api/buyer/orders/${id}/cancel`,
    ORDER_CANCELLATION_REQ: (id: string) => `/api/buyer/orders/${id}/cancellation-request`,
    ORDER_RETURN_REQ: (id: string) => `/api/buyer/orders/${id}/return-request`,
    RETURN_REQUESTS: "/api/buyer/return-requests",
    RETURN_REQUEST_DETAIL: (id: string) => `/api/buyer/return-requests/${id}`,
    RETURN_REQUEST_CANCEL: (id: string) => `/api/buyer/return-requests/${id}/cancel`,
  },
  PAYMENT: {
    VNPAY_CALLBACK: "/api/payment/vnpay/callback",
    VNPAY_RETRY: (paymentId: string) => `/api/payment/vnpay/retry/${paymentId}`,
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
  },
  SHOPS: {
    DETAIL: (sellerName: string) => `/api/shops/${sellerName}`,
    VOUCHERS: (sellerName: string) => `/api/shops/${sellerName}/vouchers`,
  },
  VOUCHERS: {
    PLATFORM: "/api/vouchers/platform",
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
    SECURITY: "/account/security",
  },
  SELLER: {
    DASHBOARD: "/seller/dashboard",
    REGISTER: "/seller/register",
    PRODUCTS: "/seller/products",
    NEW_PRODUCT: "/seller/products/new",
    ORDERS: "/seller/orders",
    VOUCHERS: "/seller/vouchers",
    SETTINGS: "/seller/settings",
  },
  SHOPS: (sellerName: string) => `/shops/${sellerName}`,
} as const;
