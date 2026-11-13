"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, ShieldAlert, Loader2, AlertCircle } from "lucide-react";
import {
  statusReasonSchema,
  type StatusReasonFormValues,
} from "../../schemas/moderationSchema";
import { useUpdateUserStatusMutation } from "../../hooks/useAdminOperations";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import type { AdminUserResponse } from "@/types/admin";
import type { UserStatus } from "@/types/enums";

interface UserStatusModalProps {
  user: AdminUserResponse | null;
  targetStatus: UserStatus | null;
  onClose: () => void;
  onSuccess: (msg: string) => void;
}

export function UserStatusModal({
  user,
  targetStatus,
  onClose,
  onSuccess,
}: UserStatusModalProps) {
  const mutation = useUpdateUserStatusMutation();
  const [serverError, setServerError] = useState<string | null>(null);

  const handleClose = useCallback(() => {
    setServerError(null);
    onClose();
  }, [onClose]);

  useAdminDialogA11y(Boolean(user && targetStatus), handleClose);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StatusReasonFormValues>({
    resolver: zodResolver(statusReasonSchema),
    defaultValues: { reason: "" },
  });

  useEffect(() => {
    if (user && targetStatus) {
      reset({ reason: "" });
    }
  }, [user, targetStatus, reset]);

  if (!user || !targetStatus) return null;

  const fullName =
    `${user.firstName || ""} ${user.lastName || ""}`.trim() || user.email;

  const onSubmit = async (values: StatusReasonFormValues) => {
    setServerError(null);
    try {
      await mutation.mutateAsync({
        id: user.id,
        data: {
          status: targetStatus,
          reason: values.reason.trim(),
        },
      });
      onSuccess(
        `Đã chuyển trạng thái tài khoản "${fullName}" sang ${targetStatus}.`
      );
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật trạng thái tài khoản thất bại.";
      setServerError(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="user-status-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={handleClose}
      />

      <div className="relative z-10 w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden animate-in zoom-in-95 motion-reduce:animate-none">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
              <ShieldAlert className="w-4 h-4" />
            </div>
            <h3
              id="user-status-modal-title"
              className="font-heading text-sm font-extrabold text-on-surface"
            >
              Cập nhật Trạng thái Tài khoản
            </h3>
          </div>
          <button
            type="button"
            aria-label="Đóng hộp thoại"
            onClick={handleClose}
            className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 flex flex-col gap-4">
          {serverError && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs">
            <p className="font-bold text-slate-900">{fullName}</p>
            <p className="font-mono text-slate-500 mt-0.5">{user.email}</p>
            <p className="mt-2">
              Chuyển từ <strong>{user.status}</strong> →{" "}
              <strong
                className={
                  targetStatus === "ACTIVE"
                    ? "text-emerald-700"
                    : targetStatus === "SUSPENDED"
                    ? "text-amber-700"
                    : "text-rose-600"
                }
              >
                {targetStatus}
              </strong>
            </p>
          </div>

          {targetStatus !== "ACTIVE" && (
            <p className="text-[11px] text-amber-800 bg-amber-50 p-3 rounded-xl border border-amber-200 leading-relaxed">
              Lưu ý: Khi khóa hoặc cấm tài khoản, hệ thống sẽ lập tức thu hồi toàn bộ Refresh Token đang hoạt động trên mọi thiết bị.
            </p>
          )}

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-700">
              Lý do xử lý (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={3}
              {...register("reason")}
              placeholder="Nhập rõ lý do khóa/cấm/mở khóa tài khoản..."
              className="w-full p-3 rounded-xl border border-slate-200 text-xs text-slate-800 focus:outline-none focus:border-[#0045cc]"
            />
            {errors.reason && (
              <p className="text-[11px] text-rose-600 font-medium">
                {errors.reason.message}
              </p>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
              className={`px-4 py-2 rounded-xl text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer ${
                targetStatus === "ACTIVE"
                  ? "bg-emerald-600 hover:bg-emerald-700"
                  : targetStatus === "SUSPENDED"
                  ? "bg-amber-600 hover:bg-amber-700"
                  : "bg-rose-600 hover:bg-rose-700"
              }`}
            >
              {mutation.isPending && (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              )}
              <span>Xác nhận {targetStatus}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
