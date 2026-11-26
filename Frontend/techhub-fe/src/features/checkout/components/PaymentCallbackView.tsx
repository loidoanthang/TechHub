"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, XCircle, Loader2, ArrowRight } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { checkoutApi } from "../api/checkoutApi";
import { PAYMENT_INFO_KEYS } from "@/features/account/hooks/usePaymentInfoQueries";

interface CallbackResult {
  success: boolean;
  transactionNo?: string;
  amount?: number;
  bankCode?: string;
  orderNumbers?: string[];
  message?: string;
}

function PaymentCallbackContent() {
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const isTokenLink =
    searchParams.get("vnp_command") === "token_create" ||
    searchParams.get("vnp_Command") === "token_create" ||
    searchParams.get("vnp_txn_ref")?.startsWith("LINK_") ||
    searchParams.get("vnp_TxnRef")?.startsWith("LINK_");

  const [state, setState] = useState<{
    isLoading: boolean;
    result: CallbackResult | null;
    errorMessage: string | null;
  }>({
    isLoading: true,
    result: null,
    errorMessage: null,
  });

  useEffect(() => {
    let isMounted = true;

    async function verifyPayment() {
      const queryString = searchParams.toString();
      const hasSecureHash =
        searchParams.get("vnp_SecureHash") ||
        searchParams.get("vnp_secure_hash");

      // If no query parameters are present, reject immediately (prevents direct unauthorized visit)
      if (!queryString || !hasSecureHash) {
        if (isMounted) {
          setState({
            isLoading: false,
            result: null,
            errorMessage: "Invalid callback request. Missing transaction verification parameters.",
          });
        }
        return;
      }

      try {
        const queryRecord: Record<string, string> = {};
        searchParams.forEach((val, key) => {
          queryRecord[key] = val;
        });

        const res = await checkoutApi.processVNPayCallback(queryRecord);

        if (isMounted) {
          if (res.success && res.data?.success) {
            if (isTokenLink) {
              queryClient.invalidateQueries({ queryKey: PAYMENT_INFO_KEYS.all });
            }
            setState({
              isLoading: false,
              result: res.data,
              errorMessage: null,
            });
          } else {
            setState({
              isLoading: false,
              result: null,
              errorMessage: res.message || res.data?.message || "Payment verification failed. Checksum or transaction invalid.",
            });
          }
        }
      } catch (err: unknown) {
        if (isMounted) {
          const errObj = err as { response?: { data?: { message?: string } }; message?: string };
          setState({
            isLoading: false,
            result: null,
            errorMessage:
              errObj?.response?.data?.message ||
              errObj?.message ||
              "Unable to verify payment with server. Please check your network or try again.",
          });
        }
      }
    }

    verifyPayment();

    return () => {
      isMounted = false;
    };
  }, [searchParams]);

  const { isLoading, result, errorMessage } = state;
  const isSuccess = !isLoading && result?.success;

  // Only display verified transaction data from backend response
  const formattedAmount = result?.amount
    ? new Intl.NumberFormat("vi-VN").format(result.amount) + "₫"
    : "---";

  const transactionNo = result?.transactionNo || "---";

  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-16 flex items-center justify-center">
      <div className="max-w-md w-full mx-4 bg-white rounded-2xl shadow-lg border border-[#c3c5d9]/30 p-8 text-center flex flex-col items-center gap-4">
        {isLoading ? (
          <>
            <div className="w-16 h-16 rounded-full bg-blue-50 text-[#0045cc] flex items-center justify-center">
              <Loader2 className="w-8 h-8 animate-spin" />
            </div>
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              {isTokenLink ? "Đang xác thực liên kết thẻ..." : "Đang xác thực thanh toán..."}
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              Vui lòng chờ trong khi hệ thống kiểm tra trạng thái giao dịch và chữ ký bảo mật với cổng VNPay.
            </p>
          </>
        ) : isSuccess ? (
          <>
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              {isTokenLink ? "Liên kết thẻ thành công!" : "Thanh toán thành công!"}
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              {isTokenLink
                ? "Thẻ ngân hàng của bạn đã được liên kết thành công với TechHub. Bạn có thể sử dụng thanh toán 1-click cho các lần mua hàng tiếp theo."
                : "Giao dịch VNPay của bạn đã được xác nhận. Đơn hàng đang được nhà bán xử lý."}
            </p>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center">
              <XCircle className="w-10 h-10" />
            </div>
            <h1 className="font-heading text-2xl sm:text-3xl font-bold text-[#0b1c30] tracking-tight">
              {isTokenLink ? "Liên kết thẻ thất bại" : "Thanh toán thất bại"}
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              {errorMessage || "Không thể xác thực giao dịch hoặc giao dịch đã bị hủy."}
            </p>
          </>
        )}

        {!isLoading && (
          <div className="w-full bg-[#eff4ff] rounded-xl p-4 text-xs flex flex-col gap-2 my-2 text-left">
            <div className="flex justify-between gap-2">
              <span className="text-[#737688] shrink-0">Mã giao dịch:</span>
              <span className="font-mono font-bold text-[#0b1c30] text-right break-all">{transactionNo}</span>
            </div>
            {!isTokenLink && (
              <div className="flex justify-between gap-2">
                <span className="text-[#737688] shrink-0">Số tiền:</span>
                <span className="font-mono font-bold text-[#0045cc] text-right">{formattedAmount}</span>
              </div>
            )}
            <div className="flex justify-between gap-2">
              <span className="text-[#737688] shrink-0">Phương thức:</span>
              <span className="font-semibold text-[#0b1c30] text-right">
                {isTokenLink ? "VNPAY Token (Liên kết thẻ)" : "VNPay QR / Ngân hàng"}
              </span>
            </div>
            {result?.bankCode && (
              <div className="flex justify-between gap-2">
                <span className="text-[#737688] shrink-0">Ngân hàng:</span>
                <span className="font-semibold text-[#0b1c30] text-right">{result.bankCode}</span>
              </div>
            )}
            {!isTokenLink && result?.orderNumbers && result.orderNumbers.length > 0 && (
              <div className="flex justify-between gap-2">
                <span className="text-[#737688] shrink-0">Mã đơn hàng:</span>
                <span className="font-mono text-[#0b1c30] text-right break-all">{result.orderNumbers.join(", ")}</span>
              </div>
            )}
          </div>
        )}

        <div className="flex flex-col gap-2.5 w-full mt-2">
          {isLoading ? null : isSuccess ? (
            isTokenLink ? (
              <Link
                href="/account/payment-methods"
                className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
              >
                <span>Xem danh sách thẻ đã lưu</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            ) : (
              <Link
                href="/"
                className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
              >
                <span>Về trang chủ</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            )
          ) : (
            isTokenLink ? (
              <>
                <Link
                  href="/account/payment-methods"
                  className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
                >
                  <span>Thử liên kết lại</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/"
                  className="w-full py-3 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-[#0b1c30] text-sm font-semibold transition-colors flex items-center justify-center gap-2"
                >
                  <span>Về trang chủ</span>
                </Link>
              </>
            ) : (
              <>
                <Link
                  href="/account/orders"
                  className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
                >
                  <span>Xem đơn hàng (Thanh toán lại)</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/"
                  className="w-full py-3 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-[#0b1c30] text-sm font-semibold transition-colors flex items-center justify-center gap-2"
                >
                  <span>Về trang chủ</span>
                </Link>
              </>
            )
          )}
        </div>
      </div>
    </div>
  );
}

export function PaymentCallbackView() {
  return (
    <Suspense
      fallback={
        <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-16 flex items-center justify-center">
          <div className="max-w-md w-full mx-4 h-80 bg-white rounded-2xl animate-pulse"></div>
        </div>
      }
    >
      <PaymentCallbackContent />
    </Suspense>
  );
}
