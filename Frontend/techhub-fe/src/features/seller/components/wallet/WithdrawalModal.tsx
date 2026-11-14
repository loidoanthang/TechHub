"use client";

import React, { useEffect, useMemo } from "react";
import Link from "next/link";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  CreditCard,
  Building2,
  Loader2,
  CheckCircle2,
  ArrowRight,
  ShieldAlert,
} from "lucide-react";
import {
  createWithdrawalSchema,
  type WithdrawalFormValues,
} from "../../schemas/withdrawalSchema";
import { useRequestWithdrawalMutation } from "../../hooks/useSellerWalletQueries";
import {
  WALLET_LIMITS,
  type SellerBankAccountSummary,
} from "../../types/wallet";
import { formatPrice } from "@/lib/utils";
import { toast } from "@/lib/toast";
import { ROUTES } from "@/lib/constants";
import { isBankAccountIncomplete } from "../../utils/walletUtils";

interface WithdrawalModalProps {
  isOpen: boolean;
  onClose: () => void;
  availableBalance: number;
  bankInfo: SellerBankAccountSummary;
  isBankIncomplete?: boolean;
}

const QUICK_CHIPS = [
  { label: "+500K", val: 500_000 },
  { label: "+2M", val: 2_000_000 },
  { label: "+5M", val: 5_000_000 },
  { label: "+10M", val: 10_000_000 },
] as const;

export function WithdrawalModal({
  isOpen,
  onClose,
  availableBalance,
  bankInfo,
  isBankIncomplete: customIsBankIncomplete,
}: WithdrawalModalProps) {
  const isBankIncomplete =
    customIsBankIncomplete ?? isBankAccountIncomplete(bankInfo);
  const schema = useMemo(
    () => createWithdrawalSchema(availableBalance),
    [availableBalance]
  );

  const {
    control,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<WithdrawalFormValues>({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: {
      amount: 0,
    },
  });

  const watchedAmount = watch("amount") || 0;
  const withdrawMutation = useRequestWithdrawalMutation();

  useEffect(() => {
    if (isOpen) {
      reset({ amount: 0 });
    }
  }, [isOpen, reset]);

  if (!isOpen) return null;

  // Maximum allowed per withdrawal action (cannot exceed available balance nor daily ceiling)
  const maxAllowedWithdrawal = Math.min(
    availableBalance,
    WALLET_LIMITS.MAX_DAILY_WITHDRAWAL
  );

  const handleQuickAdd = (addValue: number) => {
    const current = watchedAmount || 0;
    const next = Math.min(maxAllowedWithdrawal, current + addValue);
    setValue("amount", next, { shouldValidate: true, shouldDirty: true });
  };

  const handleWithdrawAll = () => {
    setValue("amount", maxAllowedWithdrawal, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  const onSubmit = (data: WithdrawalFormValues) => {
    withdrawMutation.mutate(
      { amount: data.amount },
      {
        onSuccess: (res) => {
          const bankDisplay =
            res?.data?.bankName || bankInfo.bankName || "tài khoản ngân hàng";
          const maskedAcc =
            res?.data?.bankAccountNumber ||
            (bankInfo.bankAccountNumber
              ? `****${bankInfo.bankAccountNumber.slice(-4)}`
              : "");
          toast.success(
            `Yêu cầu rút ${formatPrice(data.amount)} thành công! Tiền sẽ được chuyển về ${bankDisplay} ${maskedAcc} trong 24h làm việc.`
          );
          onClose();
        },
        onError: (err: unknown) => {
          const apiError = err as { response?: { data?: { message?: string } } };
          const msg =
            apiError?.response?.data?.message ||
            "Không thể tạo yêu cầu rút tiền. Vui lòng thử lại sau.";
          toast.error(msg);
        },
      }
    );
  };

  const projectedRemaining = Math.max(0, availableBalance - watchedAmount);
  const maskedAccount = bankInfo.bankAccountNumber
    ? `**** ${bankInfo.bankAccountNumber.slice(-4)}`
    : "";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#eff4ff] flex items-center justify-center text-[#0045cc]">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-heading font-bold text-base text-slate-900">
                Yêu cầu Rút tiền Doanh thu
              </h3>
              <p className="text-xs text-slate-500">
                Giải ngân về tài khoản ngân hàng chính chủ đã đăng ký
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-6">
          {isBankIncomplete ? (
            /* Empty State khi chưa liên kết ngân hàng */
            <div className="py-8 text-center flex flex-col items-center">
              <div className="w-14 h-14 rounded-2xl bg-amber-50 flex items-center justify-center text-amber-600 mb-4">
                <ShieldAlert className="w-8 h-8" />
              </div>
              <h4 className="text-base font-bold text-slate-900 mb-1">
                Chưa hoàn tất tài khoản ngân hàng
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mb-6 leading-relaxed">
                Hệ thống yêu cầu liên kết tài khoản ngân hàng chính chủ trước khi có thể thực hiện rút tiền nhằm đảm bảo an toàn tài chính.
              </p>
              <Link
                href={`${ROUTES.SELLER.SETTINGS}?tab=payout`}
                onClick={onClose}
                className="py-2.5 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-2"
              >
                <span>Cập nhật tài khoản ngay</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          ) : (
            <form id="withdraw-form" onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Thẻ Ngân Hàng Nhận Tiền */}
              <div className="p-4 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-800 text-white shadow-sm flex flex-col justify-between relative overflow-hidden">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
                    <Building2 className="w-4 h-4 text-blue-400" />
                    <span>{bankInfo.bankName}</span>
                  </div>
                  <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400">
                    Đã liên kết
                  </span>
                </div>
                <div className="my-4">
                  <p className="text-xl font-mono tracking-widest text-slate-200">
                    {maskedAccount}
                  </p>
                </div>
                <div className="flex items-center justify-between text-xs text-slate-400">
                  <span>Chủ tài khoản</span>
                  <span className="font-bold text-white uppercase">
                    {bankInfo.bankAccountHolder}
                  </span>
                </div>
              </div>

              {/* Nhập số tiền */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-800">
                    Số tiền muốn rút <span className="text-rose-500">*</span>
                  </label>
                  <span className="text-xs text-slate-500">
                    Khả dụng: <strong className="text-[#0045cc] font-mono">{formatPrice(availableBalance)}</strong>
                  </span>
                </div>

                <div className="relative">
                  <Controller
                    name="amount"
                    control={control}
                    render={({ field }) => {
                      const displayValue =
                        field.value && field.value > 0
                          ? new Intl.NumberFormat("vi-VN").format(field.value)
                          : "";
                      return (
                        <input
                          type="text"
                          inputMode="numeric"
                          value={displayValue}
                          onChange={(e) => {
                            const rawVal = e.target.value.replace(/\D/g, "");
                            const num = rawVal ? parseInt(rawVal, 10) : 0;
                            field.onChange(num);
                          }}
                          onBlur={field.onBlur}
                          name={field.name}
                          ref={field.ref}
                          placeholder={`Nhập số tiền (tối thiểu ${formatPrice(WALLET_LIMITS.MIN_WITHDRAWAL)})`}
                          className={`w-full h-12 pl-4 pr-12 rounded-xl border text-sm font-bold font-mono text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                            errors.amount
                              ? "border-rose-400 bg-rose-50/20"
                              : "border-slate-200 bg-white"
                          }`}
                        />
                      );
                    }}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 pointer-events-none">
                    VNĐ
                  </span>
                </div>
                {errors.amount && (
                  <p className="text-[11px] text-rose-600 font-medium">
                    {errors.amount.message}
                  </p>
                )}
              </div>

              {/* Quick Chips Chọn nhanh */}
              <div>
                <p className="text-[11px] font-bold text-slate-500 mb-2 uppercase tracking-wider">
                  Chọn nhanh số tiền:
                </p>
                <div className="flex flex-wrap gap-2">
                  {QUICK_CHIPS.map((chip) => {
                    const remainingHeadroom = Math.max(
                      0,
                      maxAllowedWithdrawal - watchedAmount
                    );
                    const isDisabled = remainingHeadroom < chip.val;
                    return (
                      <button
                        key={chip.label}
                        type="button"
                        onClick={() => handleQuickAdd(chip.val)}
                        disabled={isDisabled}
                        className="px-3 py-1.5 rounded-lg border border-slate-200 hover:border-[#0045cc] hover:text-[#0045cc] hover:bg-blue-50/30 text-xs font-semibold text-slate-700 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                        title={
                          isDisabled
                            ? `Đã đạt hạn mức tối đa có thể rút (+${chip.label})`
                            : `Thêm ${chip.label}`
                        }
                      >
                        {chip.label}
                      </button>
                    );
                  })}
                  <button
                    type="button"
                    onClick={handleWithdrawAll}
                    disabled={
                      availableBalance < WALLET_LIMITS.MIN_WITHDRAWAL ||
                      watchedAmount >= maxAllowedWithdrawal
                    }
                    className="px-3 py-1.5 rounded-lg border border-[#0045cc]/30 bg-[#eff4ff] text-[#0045cc] hover:bg-[#dce9ff] text-xs font-bold transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                    title={
                      watchedAmount >= maxAllowedWithdrawal
                        ? "Đã điền số tiền rút tối đa"
                        : availableBalance > WALLET_LIMITS.MAX_DAILY_WITHDRAWAL
                        ? `Rút tối đa ${formatPrice(WALLET_LIMITS.MAX_DAILY_WITHDRAWAL)} (hạn mức ngày)`
                        : "Rút toàn bộ số dư khả dụng"
                    }
                  >
                    Rút hết
                  </button>
                </div>
              </div>

              {/* Thông tin số dư dự kiến sau khi rút */}
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Số dư ví còn lại sau khi rút:</span>
                <span className="font-bold font-mono text-slate-800">
                  {formatPrice(projectedRemaining)}
                </span>
              </div>

              {/* Ghi chú quy định */}
              <div className="text-[11px] text-slate-500 space-y-1 bg-blue-50/40 p-3 rounded-xl border border-blue-100/60">
                <p className="flex items-center gap-1.5 font-medium text-slate-700">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0045cc]" />
                  <span>Miễn phí rút tiền doanh thu về tài khoản ngân hàng liên kết.</span>
                </p>
                <p className="flex items-center gap-1.5 font-medium text-slate-700">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0045cc]" />
                  <span>Thời gian xử lý và chuyển khoản: Trong vòng 24 giờ làm việc.</span>
                </p>
                <p className="flex items-center gap-1.5 font-medium text-slate-700">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#0045cc]" />
                  <span>
                    Hạn mức rút: Tối thiểu {formatPrice(WALLET_LIMITS.MIN_WITHDRAWAL)}, tối đa {formatPrice(WALLET_LIMITS.MAX_DAILY_WITHDRAWAL)}/ngày.
                  </span>
                </p>
              </div>
            </form>
          )}
        </div>

        {/* Footer */}
        {!isBankIncomplete && (
          <div className="p-6 border-t border-slate-100 flex items-center justify-end gap-3 bg-slate-50/50">
            <button
              type="button"
              onClick={onClose}
              disabled={withdrawMutation.isPending}
              className="py-2.5 px-4 rounded-xl border border-slate-200 hover:bg-white text-xs font-bold text-slate-600 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              form="withdraw-form"
              disabled={withdrawMutation.isPending}
              className="py-2.5 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {withdrawMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-white" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <span>Xác nhận rút tiền</span>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
