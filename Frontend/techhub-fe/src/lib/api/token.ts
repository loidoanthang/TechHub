import { getCookie, setCookie, deleteCookie } from "cookies-next";
import { STORAGE_KEYS } from "@/lib/constants";

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  const token = getCookie(STORAGE_KEYS.ACCESS_TOKEN);
  return typeof token === "string" ? token : null;
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  const token = getCookie(STORAGE_KEYS.REFRESH_TOKEN);
  return typeof token === "string" ? token : null;
}

export function setTokens(accessToken: string, refreshToken: string) {
  const isSecure = typeof window !== "undefined" && window.location.protocol === "https:";

  // Access token valid for 15 minutes (match backend JWT_ACCESS_EXPIRATION)
  setCookie(STORAGE_KEYS.ACCESS_TOKEN, accessToken, {
    maxAge: 15 * 60,
    path: "/",
    sameSite: "lax",
    secure: isSecure,
  });

  // Refresh token valid for 7 days (match backend JWT_REFRESH_EXPIRATION)
  setCookie(STORAGE_KEYS.REFRESH_TOKEN, refreshToken, {
    maxAge: 7 * 24 * 60 * 60,
    path: "/",
    sameSite: "lax",
    secure: isSecure,
  });
}

export function clearTokens() {
  deleteCookie(STORAGE_KEYS.ACCESS_TOKEN, { path: "/" });
  deleteCookie(STORAGE_KEYS.REFRESH_TOKEN, { path: "/" });
  if (typeof window !== "undefined") {
    localStorage.removeItem(STORAGE_KEYS.USER);
  }
}

export type PendingAuthFlow = "verify-email" | "reset-password";

export function setPendingAuth(
  email: string,
  flow: PendingAuthFlow = "verify-email",
  resetToken?: string
) {
  const options = {
    maxAge: 15 * 60, // 15 phút
    path: "/",
    sameSite: "lax" as const,
  };

  setCookie(STORAGE_KEYS.PENDING_EMAIL, email, options);
  setCookie(STORAGE_KEYS.PENDING_FLOW, flow, options);

  if (resetToken) {
    setCookie(STORAGE_KEYS.PENDING_RESET_TOKEN, resetToken, options);
  }
}

export function getPendingAuth(): {
  email: string | null;
  flow: PendingAuthFlow | null;
  resetToken: string | null;
} {
  const email = getCookie(STORAGE_KEYS.PENDING_EMAIL);
  const flow = getCookie(STORAGE_KEYS.PENDING_FLOW);
  const resetToken = getCookie(STORAGE_KEYS.PENDING_RESET_TOKEN);

  return {
    email: typeof email === "string" ? email : null,
    flow: (flow === "reset-password" || flow === "verify-email") ? flow : null,
    resetToken: typeof resetToken === "string" ? resetToken : null,
  };
}

export function clearPendingAuth() {
  deleteCookie(STORAGE_KEYS.PENDING_EMAIL, { path: "/" });
  deleteCookie(STORAGE_KEYS.PENDING_FLOW, { path: "/" });
  deleteCookie(STORAGE_KEYS.PENDING_RESET_TOKEN, { path: "/" });
}
