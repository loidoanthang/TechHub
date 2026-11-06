"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import {
  X,
  AlertTriangle,
  Eye,
  EyeOff,
  Trash2,
  Loader2,
  ShieldAlert,
  AlertCircle,
} from "lucide-react";
import { useDeleteAccountMutation } from "@/features/auth/api/useAuthMutations";
import { clearTokens } from "@/lib/api/token";
import { ROUTES } from "@/lib/constants";
import {
  deleteAccountSchema,
  type DeleteAccountFormData,
} from "../schemas/securitySchema";
import type { AxiosError } from "axios";
import type { ApiResponse } from "@/types/api";

interface DeleteAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const DELETE_REASONS = [
  "Tôi không có nhu cầu mua sắm tại TechHub nữa",
  "Lo ngại về quyền riêng tư và bảo mật thông tin cá nhân",
  "Trải nghiệm dịch vụ hoặc đơn hàng chưa hài lòng",
  "Tôi muốn tạo một tài khoản mới",
  "Lý do khác",
];

export function DeleteAccountModal({ isOpen, onClose }: DeleteAccountModalProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [showPassword, setShowPassword] = useState(false);
  const [selectedReason, setSelectedReason] = useState(DELETE_REASONS[0]);
  const [customReasonNote, setCustomReasonNote] = useState("");
  const [serverError, setServerError] = useState<string | null>(null);

  const deleteAccountMutation = useDeleteAccountMutation();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DeleteAccountFormData>({
    resolver: zodResolver(deleteAccountSchema),
    defaultValues: {
      password: "",
      reason: "",
      confirmConsent: false,
    },
    mode: "onTouched",
  });

  if (!isOpen) return null;

  const handleClose = () => {
    reset();
    setServerError(null);
    setCustomReasonNote("");
    onClose();
  };

  const onSubmit = async (data: DeleteAccountFormData) => {
    setServerError(null);

    const fullReason =
      selectedReason === "Lý do khác"
        ? (customReasonNote.trim() || "Lý do khác")
        : customReasonNote.trim()
        ? `${selectedReason}: ${customReasonNote.trim()}`
        : selectedReason;

    try {
      await deleteAccountMutation.mutateAsync({
        password: data.password,
        reason: fullReason.slice(0, 500),
      });

      // Clear all tokens and cache
      clearTokens();
      queryClient.clear();

      // Redirect to login page with notification
      window.location.href = `${ROUTES.LOGIN}?account_deleted=true`;
    } catch (err: unknown) {
      const axiosErr = err as AxiosError<ApiResponse<unknown>>;
      const msg =
        axiosErr.response?.data?.message ||
        axiosErr.message ||
        "Không thể xóa tài khoản. Vui lòng kiểm tra lại mật khẩu.";

      if (
        msg.toLowerCase().includes("wrong") ||
        msg.toLowerCase().includes("mật khẩu") ||
        msg.includes("WRONG_CURRENT_PASSWORD")
      ) {
        setServerError("Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại.");
      } else {
        setServerError(msg);
      }
    }
  };

  const isPending = isSubmitting || deleteAccountMutation.isPending;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="relative z-10 w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-rose-100 p-6 sm:p-7 flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between pb-3 border-b border-rose-100">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-rose-100 text-rose-600 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-heading text-lg font-bold text-slate-900">
                Xóa tài khoản vĩnh viễn
              </h3>
              <p className="text-xs text-rose-600 font-medium">
                Vùng nguy hiểm • Không thể khôi phục
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={isPending}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Warning Banner */}
        <div className="bg-rose-50/70 border border-rose-200/80 rounded-2xl p-4 flex flex-col gap-2.5">
          <div className="flex items-center gap-2 text-rose-700 text-xs font-bold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>Hành động này mang tính vĩnh viễn và không thể hoàn tác:</span>
          </div>
          <ul className="text-[11px] text-rose-800 space-y-1.5 pl-6 list-disc leading-relaxed">
            <li>Tài khoản của bạn sẽ bị đóng băng và vô hiệu hóa ngay lập tức.</li>
            <li>Toàn bộ phiên đăng nhập trên các thiết bị khác sẽ bị thu hồi.</li>
            <li>
              Bạn sẽ mất quyền truy cập vào lịch sử đơn hàng, sổ địa chỉ và các mã giảm giá cá nhân.
            </li>
            <li>Địa chỉ email của bạn sẽ không thể tái sử dụng để đăng ký tài khoản mới.</li>
          </ul>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <div className="p-3.5 rounded-xl bg-rose-100/80 border border-rose-300 text-xs text-rose-800 flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-600 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          {/* Reason Selection */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-700">
              Lý do bạn muốn rời bỏ TechHub? (Tùy chọn)
            </label>
            <select
              value={selectedReason}
              onChange={(e) => setSelectedReason(e.target.value)}
              disabled={isPending}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-xs text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-colors"
            >
              {DELETE_REASONS.map((reason) => (
                <option key={reason} value={reason}>
                  {reason}
                </option>
              ))}
            </select>
          </div>

          {/* Optional Reason Details */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-600">
              Ý kiến đóng góp thêm (Tối đa 500 ký tự)
            </label>
            <textarea
              rows={2}
              maxLength={500}
              value={customReasonNote}
              onChange={(e) => setCustomReasonNote(e.target.value)}
              placeholder="Chia sẻ thêm lý do giúp chúng tôi cải thiện chất lượng dịch vụ..."
              disabled={isPending}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-xs text-slate-800 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-colors resize-none"
            />
          </div>

          {/* Password Confirmation */}
          <div className="flex flex-col gap-1.5 pt-1">
            <label className="text-xs font-bold text-slate-800 flex items-center justify-between">
              <span>
                Nhập mật khẩu hiện tại để xác nhận <span className="text-rose-500">*</span>
              </span>
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                placeholder="Nhập mật khẩu của bạn"
                disabled={isPending}
                {...register("password")}
                className={`w-full px-3.5 py-2.5 pr-10 rounded-xl border text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 transition-colors ${
                  errors.password
                    ? "border-rose-400 focus:ring-rose-500/20 focus:border-rose-500"
                    : "border-slate-200 focus:ring-rose-500/20 focus:border-rose-500"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && (
              <p className="text-[11px] text-rose-500 font-medium">{errors.password.message}</p>
            )}
          </div>

          {/* Consent Checkbox */}
          <div className="pt-2">
            <label className="flex items-start gap-2.5 cursor-pointer select-none">
              <input
                type="checkbox"
                disabled={isPending}
                {...register("confirmConsent")}
                className="w-4 h-4 rounded border-slate-300 text-rose-600 focus:ring-rose-500 mt-0.5"
              />
              <span className="text-xs text-slate-600 leading-snug">
                Tôi đã đọc, hiểu rõ các rủi ro và xác nhận muốn{" "}
                <strong className="text-rose-600 font-semibold">xóa vĩnh viễn</strong> tài khoản
                của mình khỏi hệ thống TechHub.
              </span>
            </label>
            {errors.confirmConsent && (
              <p className="text-[11px] text-rose-500 font-medium mt-1 pl-6">
                {errors.confirmConsent.message}
              </p>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={handleClose}
              disabled={isPending}
              className="px-5 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 active:scale-[0.99] text-white text-xs font-bold transition-all shadow-sm flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Xác nhận xóa tài khoản</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
