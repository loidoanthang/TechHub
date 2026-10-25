"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, XCircle, Clock, ArrowRight } from "lucide-react";

function PaymentCallbackContent() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"success" | "failed" | "pending">("pending");

  const vnpResponseCode = searchParams.get("vnp_ResponseCode");
  const vnpTransactionNo = searchParams.get("vnp_TransactionNo") || "VN9481203";
  const vnpAmount = searchParams.get("vnp_Amount");

  const formattedAmount = vnpAmount
    ? new Intl.NumberFormat("vi-VN").format(parseInt(vnpAmount) / 100) + "₫"
    : "50.430.000₫";

  useEffect(() => {
    // VNPay Response Code "00" indicates success
    if (vnpResponseCode === "00" || !vnpResponseCode) {
      setStatus("success");
    } else {
      setStatus("failed");
    }
  }, [vnpResponseCode]);

  return (
    <div className="w-full bg-[#f8f9ff] min-h-[calc(100vh-160px)] py-16 flex items-center justify-center">
      <div className="max-w-md w-full mx-4 bg-white rounded-2xl shadow-lg border border-[#c3c5d9]/30 p-8 text-center flex flex-col items-center gap-4">
        {status === "success" ? (
          <>
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <h1 className="font-heading text-2xl font-bold text-[#0b1c30]">
              Payment Successful!
            </h1>
            <p className="text-sm text-[#434656] leading-relaxed">
              Your VNPay transaction has been authorized successfully. Your order is now being processed by our verified merchants.
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
              The VNPay transaction could not be completed or was cancelled. Please try again or select another payment method.
            </p>
          </>
        )}

        <div className="w-full bg-[#eff4ff] rounded-xl p-4 text-xs flex flex-col gap-2 my-2 text-left">
          <div className="flex justify-between">
            <span className="text-[#737688]">Transaction ID:</span>
            <span className="font-mono font-bold text-[#0b1c30]">{vnpTransactionNo}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#737688]">Amount:</span>
            <span className="font-mono font-bold text-[#0045cc]">{formattedAmount}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#737688]">Payment Method:</span>
            <span className="font-semibold text-[#0b1c30]">VNPay QR / Banking</span>
          </div>
        </div>

        <div className="flex flex-col gap-2.5 w-full mt-2">
          {status === "success" ? (
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
