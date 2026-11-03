import { NextRequest, NextResponse } from "next/server";
import { STORAGE_KEYS } from "./lib/constants";
import { isSafeRelativeUrl } from "./lib/security";

const protectedRoutes = ["/account", "/checkout", "/seller", "/admin"];
const guestRoutes = ["/login", "/register", "/forgot-password", "/reset-password", "/verify-otp"];

export default async function proxy(req: NextRequest) {
  const accessToken = req.cookies.get(STORAGE_KEYS.ACCESS_TOKEN)?.value;
  const refreshToken = req.cookies.get(STORAGE_KEYS.REFRESH_TOKEN)?.value;
  const hasAuth = Boolean(accessToken || refreshToken);

  const path = req.nextUrl.pathname;

  // VNPay payment callback should always be accessible to process transaction result
  if (path.startsWith("/checkout/payment-callback")) {
    return NextResponse.next();
  }

  // 1. Check if trying to access a protected route without any session token
  const isProtected = protectedRoutes.some((route) => path.startsWith(route));
  if (isProtected && !hasAuth) {
    const loginUrl = new URL("/login", req.nextUrl);
    if (isSafeRelativeUrl(path)) {
      loginUrl.searchParams.set("returnUrl", path);
    }
    return NextResponse.redirect(loginUrl);
  }

  // 2. Check if authenticated user is trying to access guest-only routes
  const isGuestOnly = guestRoutes.some((route) => path.startsWith(route));
  if (isGuestOnly && hasAuth) {
    return NextResponse.redirect(new URL("/", req.nextUrl));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico, logo.svg, images, icons (static media assets)
     */
    "/((?!api|_next/static|_next/image|favicon\\.ico|logo\\.svg|images|icons).*)",
  ],
};
