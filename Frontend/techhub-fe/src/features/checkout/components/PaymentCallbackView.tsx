"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, XCircle, Loader2, ArrowRight } from "lucide-react";
import { checkoutApi } from "../api/checkoutApi";

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

      // If no query parameters are present, reject immediately (prevents direct unauthorized visit)
      if (!queryString || !searchParams.get("vnp_SecureHash")) {
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
            <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
              Verifying Payment...
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              Please wait while we verify your transaction status and digital checksum with VNPay secure servers.
            </p>
          </>
        ) : isSuccess ? (
          <>
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <h1 className="font-heading text-2xl font-bold text-[#0b1c30]">
              Payment Successful!
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              Your VNPay transaction has been verified successfully. Your order is now being processed by our verified merchants.
            </p>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center">
              <XCircle className="w-10 h-10" />
            </div>
            <h1 className="font-heading text-2xl font-bold text-[#0b1c30]">
              Payment Failed
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              {errorMessage || "The transaction could not be verified or was cancelled."}
            </p>
          </>
        )}

        {!isLoading && (
          <div className="w-full bg-[#eff4ff] rounded-xl p-4 text-xs flex flex-col gap-2 my-2 text-left">
            <div className="flex justify-between">
              <span className="text-[#737688]">Transaction ID:</span>
              <span className="font-mono font-bold text-[#0b1c30]">{transactionNo}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Amount:</span>
              <span className="font-mono font-bold text-[#0045cc]">{formattedAmount}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#737688]">Payment Method:</span>
              <span className="font-semibold text-[#0b1c30]">VNPay QR / Banking</span>
            </div>
            {result?.orderNumbers && result.orderNumbers.length > 0 && (
              <div className="flex justify-between">
                <span className="text-[#737688]">Orders:</span>
                <span className="font-mono text-[#0b1c30]">{result.orderNumbers.join(", ")}</span>
              </div>
            )}
          </div>
        )}

        <div className="flex flex-col gap-2.5 w-full mt-2">
          {isLoading ? null : isSuccess ? (
            <Link
              href="/"
              className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Back to Marketplace</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          ) : (
            <Link
              href="/checkout"
              className="w-full py-3 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            >
              <span>Return to Checkout</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
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
