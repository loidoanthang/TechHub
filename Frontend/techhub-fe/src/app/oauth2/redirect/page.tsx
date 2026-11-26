"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { ROUTES } from "@/lib/constants";

function OAuth2RedirectHandler() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [processed, setProcessed] = useState<boolean>(false);

  useEffect(() => {
    // Only process once to avoid re-triggering if searchParams/login reference updates
    if (processed) return;

    const accessToken = searchParams.get("accessToken") || searchParams.get("token");
    const refreshToken = searchParams.get("refreshToken");
    const errParam = searchParams.get("error");

    if (errParam) {
      setProcessed(true);
      setError(errParam);
      setTimeout(() => {
        router.push(`${ROUTES.LOGIN}?error=${encodeURIComponent(errParam)}`);
      }, 2000);
      return;
    }

    if (accessToken) {
      setProcessed(true);
      // Clean query string from browser history immediately before handling login to prevent token leakage in history
      if (typeof window !== "undefined") {
        window.history.replaceState({}, document.title, window.location.pathname);
      }
      login(
        {
          accessToken,
          refreshToken: refreshToken || "",
        },
        ROUTES.HOME
      );
    } else {
      setProcessed(true);
      setError("Không tìm thấy mã xác thực.");
      setTimeout(() => {
        router.push(ROUTES.LOGIN);
      }, 2000);
    }
  }, [searchParams, login, router, processed]);

  return (
    <div className="bg-white p-8 rounded-2xl shadow-[0_12px_40px_-10px_rgba(11,28,48,0.08)] flex flex-col items-center gap-4 max-w-sm text-center">
      {error ? (
        <div className="text-sm text-red-600 font-medium">
          Đăng nhập thất bại: {error}
        </div>
      ) : (
        <>
          <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          <h2 className="text-base font-bold text-[#0b1c30]">
            Đang hoàn tất đăng nhập Google...
          </h2>
          <p className="text-xs text-[#737688]">
            Vui lòng chờ trong giây lát để hệ thống xác thực tài khoản của bạn.
          </p>
        </>
      )}
    </div>
  );
}

export default function OAuth2RedirectPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff] p-4">
      <Suspense
        fallback={
          <div className="bg-white p-8 rounded-2xl shadow flex items-center justify-center">
            <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin" />
          </div>
        }
      >
        <OAuth2RedirectHandler />
      </Suspense>
    </div>
  );
}
