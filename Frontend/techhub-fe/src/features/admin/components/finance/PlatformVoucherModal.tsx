"use client";

import React, { useEffect, useState } from "react";
import { useForm, Controller, useWatch, type Control } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  Loader2,
  Ticket,
  Percent,
  DollarSign,
  Package,
  Truck,
  Wand2,
  Calendar,
  AlertCircle,
} from "lucide-react";
import {
  platformVoucherSchema,
  type PlatformVoucherFormValues,
} from "../../schemas/financeSettingsSchema";
import {
  useCreatePlatformVoucherMutation,
  useUpdatePlatformVoucherMutation,
} from "../../hooks/useAdminFinance";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import { VoucherLivePreviewCard } from "@/features/seller/components/vouchers/VoucherLivePreviewCard";
import { formatCurrencyVND, parseCurrencyVND } from "@/lib/utils";
import type { AdminVoucherResponse } from "@/types/adminFinance";

interface PlatformVoucherModalProps {
  isOpen: boolean;
  initialData: AdminVoucherResponse | null;
  onClose: () => void;
  onSuccess: (msg: string) => void;
}

function toDatetimeLocalValue(isoOrDate?: string | Date): string {
  const d = isoOrDate ? new Date(isoOrDate) : new Date();
  if (isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}

function PlatformVoucherPreview({
  control,
}: {
  control: Control<PlatformVoucherFormValues>;
}) {
  const watchedValues = useWatch({ control });
  return (
    <VoucherLivePreviewCard
      code={watchedValues.code || "TECHHUB"}
      name={watchedValues.name || "Ưu đãi Toàn sàn TechHub"}
      appliesTo={watchedValues.appliesTo || "PRODUCT"}
      discountType={watchedValues.discountType || "PERCENTAGE"}
      discountValue={Number(watchedValues.discountValue) || 0}
      minOrderAmount={Number(watchedValues.minOrderAmount) || 0}
      maxDiscountAmount={watchedValues.maxDiscountAmount}
      startDate={watchedValues.startDate}
      endDate={watchedValues.endDate}
      shopName="TechHub Platform (Toàn Sàn)"
    />
  );
}

export function PlatformVoucherModal({
  isOpen,
  initialData,
  onClose,
  onSuccess,
}: PlatformVoucherModalProps) {
  const isEditMode = Boolean(initialData);
  const createMutation = useCreatePlatformVoucherMutation();
  const updateMutation = useUpdatePlatformVoucherMutation();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    formState: { errors },
  } = useForm<PlatformVoucherFormValues>({
    resolver: zodResolver(platformVoucherSchema),
    defaultValues: {
      code: "",
      name: "",
      appliesTo: "PRODUCT",
      discountType: "PERCENTAGE",
      discountValue: 10,
      minOrderAmount: 0,
      maxDiscountAmount: null,
      usageLimit: null,
      usageLimitPerUser: 1,
      startDate: "",
      endDate: "",
    },
  });

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        reset({
          code: initialData.code,
          name: initialData.name,
          appliesTo:
            initialData.appliesTo === "SHIPPING_FEE"
              ? "SHIPPING_FEE"
              : "PRODUCT",
          discountType: initialData.discountType,
          discountValue: initialData.discountValue,
          minOrderAmount: initialData.minOrderAmount || 0,
          maxDiscountAmount: initialData.maxDiscountAmount ?? null,
          usageLimit: initialData.usageLimit ?? null,
          usageLimitPerUser: initialData.usageLimitPerUser || 1,
          startDate: toDatetimeLocalValue(initialData.startDate),
          endDate: toDatetimeLocalValue(initialData.endDate),
        });
      } else {
        reset({
          code: "",
          name: "",
          appliesTo: "PRODUCT",
          discountType: "PERCENTAGE",
          discountValue: 10,
          minOrderAmount: 0,
          maxDiscountAmount: null,
          usageLimit: 500,
          usageLimitPerUser: 1,
          startDate: toDatetimeLocalValue(new Date(Date.now() + 5 * 60 * 1000)),
          endDate: toDatetimeLocalValue(
            new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
          ),
        });
      }
    }
  }, [isOpen, initialData, reset]);

  const discountType = useWatch({ control, name: "discountType" });

  const handleClose = () => {
    setServerError(null);
    onClose();
  };

  useAdminDialogA11y(isOpen, handleClose);

  if (!isOpen) return null;

  const handleGenerateCode = () => {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let suffix = "";
    for (let i = 0; i < 6; i++) {
      suffix += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setValue("code", `TECH${suffix}`, { shouldValidate: true });
  };

  const onSubmit = async (values: PlatformVoucherFormValues) => {
    setServerError(null);
    const formatIsoLocal = (dt: string) =>
      dt.length === 16 ? `${dt}:00` : dt;

    try {
      if (isEditMode && initialData) {
        await updateMutation.mutateAsync({
          id: initialData.id,
          data: {
            name: values.name.trim(),
            discountType: values.discountType,
            discountValue: values.discountValue,
            minOrderAmount: values.minOrderAmount,
            maxDiscountAmount:
              values.discountType === "PERCENTAGE"
                ? values.maxDiscountAmount ?? null
                : null,
            usageLimit: values.usageLimit ?? null,
            usageLimitPerUser: values.usageLimitPerUser,
            startDate: formatIsoLocal(values.startDate),
            endDate: formatIsoLocal(values.endDate),
          },
        });
        onSuccess(`Đã cập nhật Platform Voucher "${values.code}"!`);
      } else {
        await createMutation.mutateAsync({
          code: values.code.trim().toUpperCase(),
          name: values.name.trim(),
          appliesTo: values.appliesTo,
          discountType: values.discountType,
          discountValue: values.discountValue,
          minOrderAmount: values.minOrderAmount,
          maxDiscountAmount:
            values.discountType === "PERCENTAGE"
              ? values.maxDiscountAmount ?? null
              : null,
          usageLimit: values.usageLimit ?? null,
          usageLimitPerUser: values.usageLimitPerUser,
          startDate: formatIsoLocal(values.startDate),
          endDate: formatIsoLocal(values.endDate),
        });
        onSuccess(`Đã phát hành Platform Voucher "${values.code.toUpperCase()}"!`);
      }
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Lưu Platform Voucher thất bại.";
      setServerError(msg);
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="platform-voucher-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={handleClose}
      />

      <div className="relative z-10 w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-slate-200 max-h-[92vh] flex flex-col overflow-hidden animate-in zoom-in-95 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-surface-container-low text-primary flex items-center justify-center">
              <Ticket className="w-5 h-5" />
            </div>
            <div>
              <h2
                id="platform-voucher-modal-title"
                className="font-heading text-base font-extrabold text-on-surface"
              >
                {isEditMode
                  ? `Cập nhật Voucher Toàn Sàn (${initialData?.code})`
                  : "Phát hành Voucher Toàn Sàn (TechHub Subsidized)"}
              </h2>
              <p className="text-[11px] text-slate-600">
                Áp dụng toàn bộ gian hàng trên TechHub, ngân sách do Sàn tài trợ 100% (BR-16D).
              </p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng hộp thoại"
            onClick={handleClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-6">
          {serverError && (
            <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* LEFT FORM */}
            <form
              id="platform-voucher-form"
              onSubmit={handleSubmit(onSubmit)}
              className="lg:col-span-7 flex flex-col gap-4"
            >
              {/* Code & Name */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Mã Voucher <span className="text-rose-500">*</span>
                  </label>
                  <div className="flex gap-1.5">
                    <input
                      type="text"
                      disabled={isEditMode}
                      {...register("code")}
                      placeholder="VD: TECHHUB100K"
                      className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs font-mono font-bold uppercase disabled:bg-slate-100"
                    />
                    {!isEditMode && (
                      <button
                        type="button"
                        onClick={handleGenerateCode}
                        className="px-2.5 h-10 rounded-xl bg-slate-100 hover:bg-[#eff4ff] text-slate-700 hover:text-[#0045cc] text-xs font-bold shrink-0 cursor-pointer"
                        title="Sinh mã tự động"
                      >
                        <Wand2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                  {errors.code && (
                    <p className="text-[11px] text-rose-600">
                      {errors.code.message}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Tên chương trình <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    {...register("name")}
                    placeholder="VD: Siêu Sale Công Nghệ Tháng 11"
                    className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs"
                  />
                  {errors.name && (
                    <p className="text-[11px] text-rose-600">
                      {errors.name.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Applies To */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-slate-700">
                  Phạm vi áp dụng
                </label>
                <Controller
                  name="appliesTo"
                  control={control}
                  render={({ field }) => (
                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        disabled={isEditMode}
                        onClick={() => field.onChange("PRODUCT")}
                        className={`h-10 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer ${
                          field.value === "PRODUCT"
                            ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc]"
                            : "border-slate-200 text-slate-600"
                        }`}
                      >
                        <Package className="w-4 h-4" />
                        <span>Giảm Tiền hàng</span>
                      </button>
                      <button
                        type="button"
                        disabled={isEditMode}
                        onClick={() => field.onChange("SHIPPING_FEE")}
                        className={`h-10 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer ${
                          field.value === "SHIPPING_FEE"
                            ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc]"
                            : "border-slate-200 text-slate-600"
                        }`}
                      >
                        <Truck className="w-4 h-4" />
                        <span>Giảm Phí ship (FreeShip)</span>
                      </button>
                    </div>
                  )}
                />
              </div>

              {/* Discount Type & Value */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Hình thức giảm
                  </label>
                  <Controller
                    name="discountType"
                    control={control}
                    render={({ field }) => (
                      <div className="grid grid-cols-2 gap-2">
                        <button
                          type="button"
                          onClick={() => field.onChange("PERCENTAGE")}
                          className={`h-10 px-2 rounded-xl border text-xs font-bold flex items-center justify-center gap-1 cursor-pointer ${
                            field.value === "PERCENTAGE"
                              ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc]"
                              : "border-slate-200 text-slate-600"
                          }`}
                        >
                          <Percent className="w-3.5 h-3.5" /> Theo %
                        </button>
                        <button
                          type="button"
                          onClick={() => field.onChange("FIXED_AMOUNT")}
                          className={`h-10 px-2 rounded-xl border text-xs font-bold flex items-center justify-center gap-1 cursor-pointer ${
                            field.value === "FIXED_AMOUNT"
                              ? "border-[#0045cc] bg-[#eff4ff] text-[#0045cc]"
                              : "border-slate-200 text-slate-600"
                          }`}
                        >
                          <DollarSign className="w-3.5 h-3.5" /> Tiền VNĐ
                        </button>
                      </div>
                    )}
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Mức giảm ({discountType === "PERCENTAGE" ? "%" : "VNĐ"}) *
                  </label>
                  <Controller
                    name="discountValue"
                    control={control}
                    render={({ field }) => (
                      <input
                        type={discountType === "PERCENTAGE" ? "number" : "text"}
                        inputMode="numeric"
                        value={
                          discountType === "PERCENTAGE"
                            ? field.value || ""
                            : formatCurrencyVND(field.value)
                        }
                        onChange={(e) => {
                          if (discountType === "PERCENTAGE") {
                            field.onChange(
                              e.target.value === "" ? 0 : Number(e.target.value)
                            );
                          } else {
                            field.onChange(parseCurrencyVND(e.target.value));
                          }
                        }}
                        className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs font-bold font-mono"
                      />
                    )}
                  />
                  {errors.discountValue && (
                    <p className="text-[11px] text-rose-600">
                      {errors.discountValue.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Max Discount & Min Order Amount */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {discountType === "PERCENTAGE" && (
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-slate-700">
                      Giảm tối đa (VNĐ)
                    </label>
                    <Controller
                      name="maxDiscountAmount"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="text"
                          inputMode="numeric"
                          placeholder="Không giới hạn"
                          value={formatCurrencyVND(field.value)}
                          onChange={(e) => {
                            const raw = e.target.value.replace(/\D/g, "");
                            field.onChange(raw ? parseInt(raw, 10) : null);
                          }}
                          className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs font-mono"
                        />
                      )}
                    />
                  </div>
                )}

                <div
                  className={`flex flex-col gap-1 ${
                    discountType !== "PERCENTAGE" ? "sm:col-span-2" : ""
                  }`}
                >
                  <label className="text-xs font-bold text-slate-700">
                    Đơn tối thiểu (VNĐ)
                  </label>
                  <Controller
                    name="minOrderAmount"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="text"
                        inputMode="numeric"
                        placeholder="0"
                        value={formatCurrencyVND(field.value, true)}
                        onChange={(e) =>
                          field.onChange(parseCurrencyVND(e.target.value))
                        }
                        className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs font-mono"
                      />
                    )}
                  />
                </div>
              </div>

              {/* Usage Limit & Per User */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Tổng lượt dùng toàn sàn
                  </label>
                  <input
                    type="number"
                    placeholder="Để trống nếu vô hạn"
                    {...register("usageLimit", {
                      setValueAs: (v) =>
                        v === "" || isNaN(Number(v)) ? null : Number(v),
                    })}
                    className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs"
                  />
                  {errors.usageLimit && (
                    <p className="text-[11px] text-rose-600">
                      {errors.usageLimit.message}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700">
                    Lượt dùng / Người <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    min={1}
                    {...register("usageLimitPerUser", { valueAsNumber: true })}
                    className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs"
                  />
                  {errors.usageLimitPerUser && (
                    <p className="text-[11px] text-rose-600">
                      {errors.usageLimitPerUser.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Start & End Date */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>Bắt đầu *</span>
                  </label>
                  <input
                    type="datetime-local"
                    {...register("startDate")}
                    className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs"
                  />
                  {errors.startDate && (
                    <p className="text-[11px] text-rose-600">
                      {errors.startDate.message}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>Kết thúc *</span>
                  </label>
                  <input
                    type="datetime-local"
                    {...register("endDate")}
                    className="w-full h-10 px-3 rounded-xl border border-slate-200 text-xs"
                  />
                  {errors.endDate && (
                    <p className="text-[11px] text-rose-600">
                      {errors.endDate.message}
                    </p>
                  )}
                </div>
              </div>
            </form>

            {/* RIGHT LIVE PREVIEW */}
            <div className="lg:col-span-5 flex flex-col gap-3">
              <span className="text-xs font-bold text-slate-700">
                Xem trước Thẻ Voucher Sàn
              </span>
              <PlatformVoucherPreview control={control} />
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 bg-slate-50 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="submit"
            form="platform-voucher-form"
            disabled={isPending}
            className="px-5 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-2 cursor-pointer"
          >
            {isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>{isEditMode ? "Lưu Cập Nhật" : "Phát Hành Voucher"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
