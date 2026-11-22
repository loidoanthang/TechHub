"use client";

import React, { useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  CreditCard,
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
  Star,
  ExternalLink,
} from "lucide-react";
import {
  usePaymentInfosQuery,
  useInitiateTokenLinkMutation,
  useSetDefaultPaymentInfoMutation,
  useDeletePaymentInfoMutation,
} from "../hooks/usePaymentInfoQueries";
import type { PaymentInfoResponse } from "@/types/paymentInfo";

export function PaymentMethodsView() {
  const searchParams = useSearchParams();
  const linkStatus = searchParams.get("status");

  const { data: cards = [], isLoading, refetch } = usePaymentInfosQuery();
  const initiateMutation = useInitiateTokenLinkMutation();
  const setDefaultMutation = useSetDefaultPaymentInfoMutation();
  const deleteMutation = useDeletePaymentInfoMutation();

  const [toastMessage, setToastMessage] = useState<string | null>(() => {
    if (linkStatus === "success") return "Liên kết thẻ thanh toán VNPAY thành công!";
    if (linkStatus === "failed") return "Liên kết thẻ thất bại hoặc đã bị hủy.";
    return null;
  });

  const [cardToDelete, setCardToDelete] = useState<PaymentInfoResponse | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const isLimitReached = cards.length >= 5;

  const handleLinkNewCard = async () => {
    if (isLimitReached) {
      showToast("Bạn đã đạt giới hạn tối đa 5 thẻ liên kết.");
      return;
    }

    try {
      const res = await initiateMutation.mutateAsync();
      if (res.data?.redirectUrl) {
        window.location.href = res.data.redirectUrl;
      } else {
        showToast("Không nhận được đường dẫn liên kết thẻ từ VNPAY.");
      }
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      showToast(errObj?.response?.data?.message || errObj?.message || "Không thể khởi tạo liên kết thẻ.");
    }
  };

  const handleSetDefault = async (cardId: string) => {
    try {
      await setDefaultMutation.mutateAsync(cardId);
      showToast("Đã đặt làm thẻ thanh toán mặc định.");
    } catch {
      showToast("Không thể cập nhật thẻ mặc định. Vui lòng thử lại.");
    }
  };

  const confirmDeleteCard = async () => {
    if (!cardToDelete) return;
    try {
      await deleteMutation.mutateAsync(cardToDelete.id);
      showToast(`Đã hủy liên kết thẻ ${cardToDelete.cardMask || ""}.`);
      setCardToDelete(null);
      refetch();
    } catch {
      showToast("Không thể hủy liên kết thẻ. Vui lòng thử lại sau.");
    }
  };

  return (
    <div className="flex flex-col gap-6 w-full">
      {/* Header Panel */}
      <div className="bg-white rounded-2xl p-6 border border-[#c3c5d9]/25 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-[#0b1c30] flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-[#0045cc]" />
            Phương Thức Thanh Toán
          </h1>
          <p className="text-xs text-[#737688] mt-1">
            Quản lý thẻ ngân hàng liên kết VNPAY để thanh toán 1-Click nhanh chóng và bảo mật.
          </p>
        </div>

        <button
          type="button"
          onClick={handleLinkNewCard}
          disabled={isLimitReached || initiateMutation.isPending}
          className="px-4 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold flex items-center gap-2 transition-all shadow-sm cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
          title={isLimitReached ? "Đã đạt giới hạn tối đa 5 thẻ" : "Thêm thẻ mới"}
        >
          {initiateMutation.isPending ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Plus className="w-4 h-4" />
          )}
          <span>{initiateMutation.isPending ? "Đang kết nối..." : "Liên kết thẻ mới"}</span>
        </button>
      </div>

      {/* Capacity Note */}
      {isLimitReached && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-3.5 flex items-center gap-3 text-xs text-amber-800">
          <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
          <span>Bạn đã liên kết tối đa 5 thẻ thanh toán. Vui lòng hủy liên kết bớt thẻ cũ nếu muốn thêm thẻ mới.</span>
        </div>
      )}

      {/* Cards Grid */}
      {isLoading ? (
        <div className="bg-white rounded-2xl p-12 border border-[#c3c5d9]/25 flex items-center justify-center">
          <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
        </div>
      ) : cards.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 border border-[#c3c5d9]/25 text-center flex flex-col items-center justify-center gap-3">
          <div className="w-16 h-16 rounded-full bg-[#eff4ff] flex items-center justify-center text-[#0045cc]">
            <CreditCard className="w-8 h-8" />
          </div>
          <h3 className="text-sm font-bold text-[#0b1c30]">Chưa có thẻ thanh toán nào</h3>
          <p className="text-xs text-[#737688] max-w-sm">
            Liên kết thẻ ghi nợ / thẻ tín dụng qua VNPAY Token để mua hàng tức thì chỉ với 1 click tại bước thanh toán.
          </p>
          <button
            type="button"
            onClick={handleLinkNewCard}
            disabled={initiateMutation.isPending}
            className="mt-2 px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold flex items-center gap-2 shadow-sm cursor-pointer disabled:opacity-50"
          >
            {initiateMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <ExternalLink className="w-4 h-4" />
            )}
            <span>Liên kết thẻ VNPAY ngay</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {cards.map((card) => {
            const isDef = card.isDefault;
            return (
              <div
                key={card.id}
                className={`rounded-2xl p-5 border transition-all relative overflow-hidden flex flex-col justify-between h-48 ${
                  isDef
                    ? "bg-gradient-to-br from-[#0b1c30] to-[#0045cc] text-white border-transparent shadow-md"
                    : "bg-white text-[#0b1c30] border-[#c3c5d9]/30 shadow-sm"
                }`}
              >
                {/* Top Row: Bank Code / Card Type & Badges */}
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div
                      className={`px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wider uppercase ${
                        isDef ? "bg-white/20 text-white" : "bg-[#eff4ff] text-[#0045cc]"
                      }`}
                    >
                      {card.bankCode || "VNPAY"}
                    </div>
                    <span className={`text-xs ${isDef ? "text-blue-100" : "text-[#737688]"}`}>
                      {card.cardType || "Debit / Credit"}
                    </span>
                  </div>

                  {isDef && (
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 flex items-center gap-1">
                      <Star className="w-3 h-3 fill-emerald-300" />
                      Mặc định
                    </span>
                  )}
                </div>

                {/* Middle Row: Card Masked Number */}
                <div className="my-auto">
                  <div className="text-base sm:text-lg font-mono font-bold tracking-widest">
                    {card.cardMask || "**** **** **** ****"}
                  </div>
                  {card.accountHolderName && (
                    <div className={`text-[11px] uppercase tracking-wider mt-1 ${isDef ? "text-blue-100/80" : "text-[#737688]"}`}>
                      {card.accountHolderName}
                    </div>
                  )}
                </div>

                {/* Bottom Row: Actions */}
                <div
                  className={`pt-3 flex items-center justify-between border-t ${
                    isDef ? "border-white/10" : "border-[#eff4ff]"
                  }`}
                >
                  <div>
                    {!isDef ? (
                      <button
                        type="button"
                        onClick={() => handleSetDefault(card.id)}
                        disabled={setDefaultMutation.isPending}
                        className="text-xs font-semibold text-[#0045cc] hover:underline cursor-pointer disabled:opacity-50"
                      >
                        Đặt làm mặc định
                      </button>
                    ) : (
                      <span className="text-[11px] text-blue-200/90 flex items-center gap-1">
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                        Ưu tiên thanh toán
                      </span>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => setCardToDelete(card)}
                    className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                      isDef
                        ? "text-white/60 hover:text-rose-300 hover:bg-white/10"
                        : "text-[#737688] hover:text-rose-600 hover:bg-rose-50"
                    }`}
                    title="Hủy liên kết thẻ"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {cardToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl border border-[#c3c5d9]/30 flex flex-col gap-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center">
              <h3 className="text-sm font-bold text-[#0b1c30]">Xác nhận hủy liên kết thẻ</h3>
              <p className="text-xs text-[#737688] mt-1.5 leading-relaxed">
                Bạn có chắc chắn muốn hủy liên kết thẻ{" "}
                <strong className="text-[#0b1c30]">{cardToDelete.cardMask}</strong>? Bạn sẽ không thể sử dụng thanh toán 1-Click bằng thẻ này trừ khi liên kết lại.
              </p>
            </div>

            <div className="flex items-center gap-3 mt-2">
              <button
                type="button"
                onClick={() => setCardToDelete(null)}
                disabled={deleteMutation.isPending}
                className="flex-1 py-2.5 rounded-xl border border-[#c3c5d9]/40 hover:bg-[#eff4ff] text-[#0b1c30] text-xs font-semibold transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={confirmDeleteCard}
                disabled={deleteMutation.isPending}
                className="flex-1 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition-colors cursor-pointer flex items-center justify-center gap-1.5 disabled:opacity-50"
              >
                {deleteMutation.isPending ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <span>Xác nhận xóa</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      <div
        className={`fixed bottom-8 right-8 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 transform transition-all duration-300 ${
          toastMessage
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-12 opacity-0 pointer-events-none"
        }`}
      >
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <span className="text-xs font-semibold">{toastMessage}</span>
      </div>
    </div>
  );
}
