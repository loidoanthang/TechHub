"use client";

import React, { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
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
  voucherFormSchema,
  type VoucherFormValues,
} from "@/features/seller/schemas/voucherSchema";
import { formatCurrencyVND, parseCurrencyVND } from "@/lib/utils";
import {
  useCreateVoucherMutation,
  useUpdateVoucherMutation,
} from "@/features/seller/hooks/useSellerVouchers";
import type {
  ShopVoucherDetailResponse,
  CreateShopVoucherRequest,
  UpdateShopVoucherRequest,
} from "@/types/sellerVoucher";
import { VoucherLivePreviewCard } from "./VoucherLivePreviewCard";

interface VoucherFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialData?: ShopVoucherDetailResponse | null;
  shopName?: string;
}

export function VoucherFormModal({
  isOpen,
  onClose,
  initialData,
  shopName,
}: VoucherFormModalProps) {
  const isEditMode = Boolean(initialData?.id);
  const createMutation = useCreateVoucherMutation();
  const updateMutation = useUpdateVoucherMutation();
  const [serverError, setServerError] = useState<string | null>(null);

  // Helper format local datetime string for input type="datetime-local"
  const toInputDateTime = (date: Date) => {
    const pad = (n: number) => n.toString().padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
      date.getDate()
    )}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };

  const defaultStartDate = new Date(Date.now() + 5 * 60 * 1000); // 5 mins from now
  const defaultEndDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days from now

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<VoucherFormValues>({
    resolver: zodResolver(voucherFormSchema),
    defaultValues: {
      code: "",
      name: "",
      appliesTo: "PRODUCT",
      discountType: "PERCENTAGE",
      discountValue: 10,
      minOrderAmount: 0,
      maxDiscountAmount: 50000,
      usageLimit: 100,
      usageLimitPerUser: 1,
      startDate: toInputDateTime(defaultStartDate),
      endDate: toInputDateTime(defaultEndDate),
    },
    mode: "onChange",
  });

  // Watch form fields for live preview
  const watchedCode = watch("code");
  const watchedName = watch("name");
  const watchedAppliesTo = watch("appliesTo");
  const watchedDiscountType = watch("discountType");
  const watchedDiscountValue = watch("discountValue");
  const watchedMinOrder = watch("minOrderAmount");
  const watchedMaxDiscount = watch("maxDiscountAmount");
  const watchedStartDate = watch("startDate");
  const watchedEndDate = watch("endDate");

  // Pre-fill form on edit or reset on create
  useEffect(() => {
    if (isOpen) {
      setServerError(null);
      if (initialData) {
        reset({
          code: initialData.code,
          name: initialData.name,
          appliesTo: initialData.appliesTo,
          discountType: initialData.discountType,
          discountValue: Number(initialData.discountValue),
          minOrderAmount: Number(initialData.minOrderAmount || 0),
          maxDiscountAmount: initialData.maxDiscountAmount
            ? Number(initialData.maxDiscountAmount)
            : null,
          usageLimit: initialData.usageLimit || null,
          usageLimitPerUser: initialData.usageLimitPerUser || 1,
          startDate: initialData.startDate
            ? toInputDateTime(new Date(initialData.startDate))
            : toInputDateTime(defaultStartDate),
          endDate: initialData.endDate
            ? toInputDateTime(new Date(initialData.endDate))
            : toInputDateTime(defaultEndDate),
        });
      } else {
        reset({
          code: "",
          name: "",
          appliesTo: "PRODUCT",
          discountType: "PERCENTAGE",
          discountValue: 10,
          minOrderAmount: 0,
          maxDiscountAmount: 50000,
          usageLimit: 100,
          usageLimitPerUser: 1,
          startDate: toInputDateTime(defaultStartDate),
          endDate: toInputDateTime(defaultEndDate),
        });
      }
    }
  }, [isOpen, initialData, reset]);

  // Generate random voucher code
  const handleGenerateRandomCode = () => {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let randomPart = "";
    for (let i = 0; i < 6; i++) {
      randomPart += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    const newCode = `TH${randomPart}`;
    setValue("code", newCode, { shouldValidate: true });
  };

  const onSubmit = async (values: VoucherFormValues) => {
    setServerError(null);
    try {
      if (isEditMode && initialData?.id) {
        const updatePayload: UpdateShopVoucherRequest = {
          name: values.name.trim(),
          discountType: values.discountType,
          discountValue: Number(values.discountValue),
          minOrderAmount: Number(values.minOrderAmount || 0),
          maxDiscountAmount:
            values.discountType === "PERCENTAGE" && values.maxDiscountAmount
              ? Number(values.maxDiscountAmount)
              : null,
          usageLimit: values.usageLimit ? Number(values.usageLimit) : null,
          usageLimitPerUser: Number(values.usageLimitPerUser || 1),
          startDate: new Date(values.startDate).toISOString(),
          endDate: new Date(values.endDate).toISOString(),
        };
        await updateMutation.mutateAsync({
          id: initialData.id,
          data: updatePayload,
        });
      } else {
        const createPayload: CreateShopVoucherRequest = {
          code: values.code.trim().toUpperCase(),
          name: values.name.trim(),
          appliesTo: values.appliesTo,
          discountType: values.discountType,
          discountValue: Number(values.discountValue),
          minOrderAmount: Number(values.minOrderAmount || 0),
          maxDiscountAmount:
            values.discountType === "PERCENTAGE" && values.maxDiscountAmount
              ? Number(values.maxDiscountAmount)
              : null,
          usageLimit: values.usageLimit ? Number(values.usageLimit) : null,
          usageLimitPerUser: Number(values.usageLimitPerUser || 1),
          startDate: new Date(values.startDate).toISOString(),
          endDate: new Date(values.endDate).toISOString(),
        };
        await createMutation.mutateAsync(createPayload);
      }
      onClose();
    } catch (err: unknown) {
      console.error("Voucher form submission error:", err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Đã xảy ra lỗi khi lưu mã giảm giá. Vui lòng kiểm tra lại.";
      setServerError(msg);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-5 overflow-y-auto">
      <div className="bg-white rounded-3xl w-full max-w-4xl relative border border-slate-200 shadow-2xl overflow-hidden my-auto animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="px-6 py-4.5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-[#0045cc] flex items-center justify-center shadow-2xs">
              <Ticket className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-headline font-bold text-base text-slate-900">
                {isEditMode ? "Chỉnh Sửa Mã Giảm Giá" : "Tạo Mã Giảm Giá Mới"}
              </h3>
              <p className="text-[11px] text-slate-500">
                {isEditMode
                  ? `Cập nhật thông số ưu đãi cho voucher ${initialData?.code}`
                  : "Thiết lập mã coupon khuyến mãi độc quyền của gian hàng"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 flex items-center justify-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Server Error Alert */}
        {serverError && (
          <div className="mx-6 mt-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{serverError}</span>
          </div>
        )}

        {/* Form Body - 2 Columns on Desktop */}
        <form onSubmit={handleSubmit(onSubmit)} className="p-6">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left Column: Form Controls (7 cols) */}
            <div className="lg:col-span-7 flex flex-col gap-4">
              {/* Row 1: Voucher Code & Random Button */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Mã Voucher <span className="text-rose-500">*</span>
                </label>
                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      disabled={isEditMode}
                      placeholder="VD: TECHHUB50K"
                      {...register("code")}
                      className={`w-full h-10 px-3.5 rounded-xl border text-xs font-mono font-bold tracking-wider uppercase transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                        errors.code
                          ? "border-rose-400 bg-rose-50/30"
                          : "border-slate-200 bg-white"
                      } ${isEditMode ? "bg-slate-100 text-slate-500 cursor-not-allowed" : ""}`}
                    />
                  </div>
                  {!isEditMode && (
                    <button
                      type="button"
                      onClick={handleGenerateRandomCode}
                      className="h-10 px-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors shrink-0"
                      title="Sinh mã ngẫu nhiên"
                    >
                      <Wand2 className="w-3.5 h-3.5 text-[#0045cc]" />
                      <span className="hidden sm:inline">Tạo ngẫu nhiên</span>
                    </button>
                  )}
                </div>
                {errors.code && (
                  <p className="text-[11px] text-rose-600 font-medium">
                    {errors.code.message}
                  </p>
                )}
                {isEditMode && (
                  <p className="text-[10px] text-slate-400">
                    Mã voucher cố định và không thể thay đổi sau khi tạo.
                  </p>
                )}
              </div>

              {/* Row 2: Voucher Name */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Tên chương trình voucher <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="VD: Giảm 10% tri ân khách hàng thân thiết"
                  {...register("name")}
                  className={`w-full h-10 px-3.5 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                    errors.name
                      ? "border-rose-400 bg-rose-50/30"
                      : "border-slate-200 bg-white"
                  }`}
                />
                {errors.name && (
                  <p className="text-[11px] text-rose-600 font-medium">
                    {errors.name.message}
                  </p>
                )}
              </div>

              {/* Row 3: Applies To (Scope) */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Phạm vi áp dụng <span className="text-rose-500">*</span>
                </label>
                <Controller
                  name="appliesTo"
                  control={control}
                  render={({ field }) => (
                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => field.onChange("PRODUCT")}
                        className={`h-9 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                          field.value === "PRODUCT"
                            ? "border-[#0045cc] bg-blue-50/60 text-[#0045cc] shadow-2xs"
                            : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                        }`}
                      >
                        <Package className="w-3.5 h-3.5" />
                        <span>Toàn bộ sản phẩm</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => field.onChange("SHIPPING_FEE")}
                        className={`h-9 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                          field.value === "SHIPPING_FEE"
                            ? "border-[#0045cc] bg-blue-50/60 text-[#0045cc] shadow-2xs"
                            : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                        }`}
                      >
                        <Truck className="w-3.5 h-3.5" />
                        <span>Phí vận chuyển</span>
                      </button>
                    </div>
                  )}
                />
              </div>

              {/* Row 4: Discount Type & Discount Value */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Loại giảm giá <span className="text-rose-500">*</span>
                  </label>
                  <Controller
                    name="discountType"
                    control={control}
                    render={({ field }) => (
                      <div className="grid grid-cols-2 gap-1.5 p-1 bg-slate-100 rounded-xl">
                        <button
                          type="button"
                          onClick={() => field.onChange("PERCENTAGE")}
                          className={`py-1.5 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1 ${
                            field.value === "PERCENTAGE"
                              ? "bg-white text-[#0045cc] shadow-xs"
                              : "text-slate-600 hover:text-slate-900"
                          }`}
                        >
                          <Percent className="w-3 h-3" />
                          <span>Theo %</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => field.onChange("FIXED_AMOUNT")}
                          className={`py-1.5 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1 ${
                            field.value === "FIXED_AMOUNT"
                              ? "bg-white text-[#0045cc] shadow-xs"
                              : "text-slate-600 hover:text-slate-900"
                          }`}
                        >
                          <DollarSign className="w-3 h-3" />
                          <span>Tiền mặt</span>
                        </button>
                      </div>
                    )}
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Mức giảm ({watchedDiscountType === "PERCENTAGE" ? "%" : "VNĐ"}){" "}
                    <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <Controller
                      name="discountValue"
                      control={control}
                      render={({ field }) => (
                        <input
                          type={watchedDiscountType === "PERCENTAGE" ? "number" : "text"}
                          inputMode="numeric"
                          step={watchedDiscountType === "PERCENTAGE" ? "1" : undefined}
                          placeholder={watchedDiscountType === "PERCENTAGE" ? "VD: 15" : "VD: 50.000"}
                          value={
                            watchedDiscountType === "PERCENTAGE"
                              ? field.value || ""
                              : formatCurrencyVND(field.value)
                          }
                          onChange={(e) => {
                            if (watchedDiscountType === "PERCENTAGE") {
                              const val = e.target.value === "" ? 0 : Number(e.target.value);
                              field.onChange(val);
                            } else {
                              const num = parseCurrencyVND(e.target.value);
                              field.onChange(num);
                            }
                          }}
                          className={`w-full h-10 px-3.5 pr-8 rounded-xl border text-xs font-bold transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                            errors.discountValue
                              ? "border-rose-400 bg-rose-50/30"
                              : "border-slate-200 bg-white"
                          }`}
                        />
                      )}
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400 select-none pointer-events-none">
                      {watchedDiscountType === "PERCENTAGE" ? "%" : "đ"}
                    </span>
                  </div>
                  {errors.discountValue && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.discountValue.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Row 5: Max Discount (Only if PERCENTAGE) & Min Order Amount */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {watchedDiscountType === "PERCENTAGE" ? (
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-bold text-slate-700">
                      Mức giảm tối đa (VNĐ)
                    </label>
                    <div className="relative">
                      <Controller
                        name="maxDiscountAmount"
                        control={control}
                        render={({ field }) => (
                          <input
                            type="text"
                            inputMode="numeric"
                            placeholder="Để trống nếu không giới hạn"
                            value={formatCurrencyVND(field.value)}
                            onChange={(e) => {
                              const raw = e.target.value.replace(/\D/g, "");
                              field.onChange(raw ? parseInt(raw, 10) : null);
                            }}
                            className={`w-full h-10 px-3.5 pr-8 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                              errors.maxDiscountAmount
                                ? "border-rose-400 bg-rose-50/30"
                                : "border-slate-200 bg-white"
                            }`}
                          />
                        )}
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400 select-none pointer-events-none">
                        đ
                      </span>
                    </div>
                    {errors.maxDiscountAmount && (
                      <p className="text-[11px] text-rose-600 font-medium">
                        {errors.maxDiscountAmount.message}
                      </p>
                    )}
                  </div>
                ) : null}

                <div className={`flex flex-col gap-1.5 ${watchedDiscountType !== "PERCENTAGE" ? "sm:col-span-2" : ""}`}>
                  <label className="text-xs font-bold text-slate-700">
                    Giá trị đơn tối thiểu (VNĐ)
                  </label>
                  <div className="relative">
                    <Controller
                      name="minOrderAmount"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="text"
                          inputMode="numeric"
                          placeholder="VD: 200.000"
                          value={formatCurrencyVND(field.value)}
                          onChange={(e) => {
                            const num = parseCurrencyVND(e.target.value);
                            field.onChange(num);
                          }}
                          className={`w-full h-10 px-3.5 pr-8 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                            errors.minOrderAmount
                              ? "border-rose-400 bg-rose-50/30"
                              : "border-slate-200 bg-white"
                          }`}
                        />
                      )}
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400 select-none pointer-events-none">
                      đ
                    </span>
                  </div>
                  {errors.minOrderAmount && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.minOrderAmount.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Row 6: Usage Limit & Usage Limit Per User */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Tổng lượt dùng tối đa
                  </label>
                  <input
                    type="number"
                    placeholder="VD: 100 (để trống nếu vô hạn)"
                    {...register("usageLimit", {
                      setValueAs: (v) =>
                        v === "" || v === undefined || isNaN(v) ? null : Number(v),
                    })}
                    className={`w-full h-10 px-3.5 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                      errors.usageLimit
                        ? "border-rose-400 bg-rose-50/30"
                        : "border-slate-200 bg-white"
                    }`}
                  />
                  {errors.usageLimit && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.usageLimit.message}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Lượt dùng / khách hàng <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="1"
                    placeholder="VD: 1"
                    {...register("usageLimitPerUser", {
                      setValueAs: (v) =>
                        v === "" || v === undefined || isNaN(v) ? 1 : Number(v),
                    })}
                    className={`w-full h-10 px-3.5 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                      errors.usageLimitPerUser
                        ? "border-rose-400 bg-rose-50/30"
                        : "border-slate-200 bg-white"
                    }`}
                  />
                  {errors.usageLimitPerUser && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.usageLimitPerUser.message}
                    </p>
                  )}
                </div>
              </div>

              {/* Row 7: Date Range (Start Date & End Date) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Thời gian bắt đầu <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="datetime-local"
                      {...register("startDate")}
                      className={`w-full h-10 px-3 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                        errors.startDate
                          ? "border-rose-400 bg-rose-50/30"
                          : "border-slate-200 bg-white"
                      }`}
                    />
                  </div>
                  {errors.startDate && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.startDate.message}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">
                    Thời gian kết thúc <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="datetime-local"
                      {...register("endDate")}
                      className={`w-full h-10 px-3 rounded-xl border text-xs transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                        errors.endDate
                          ? "border-rose-400 bg-rose-50/30"
                          : "border-slate-200 bg-white"
                      }`}
                    />
                  </div>
                  {errors.endDate && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.endDate.message}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Right Column: Sticky Live Preview Card (5 cols) */}
            <div className="lg:col-span-5 flex flex-col gap-4 lg:sticky lg:top-4 bg-slate-50/80 p-4 rounded-2xl border border-slate-200/80">
              <VoucherLivePreviewCard
                code={watchedCode}
                name={watchedName}
                appliesTo={watchedAppliesTo}
                discountType={watchedDiscountType}
                discountValue={Number(watchedDiscountValue) || 0}
                minOrderAmount={Number(watchedMinOrder) || 0}
                maxDiscountAmount={
                  watchedDiscountType === "PERCENTAGE"
                    ? Number(watchedMaxDiscount) || null
                    : null
                }
                startDate={watchedStartDate}
                endDate={watchedEndDate}
                shopName={shopName}
              />

              <div className="p-3 bg-blue-50/50 rounded-xl border border-blue-100 text-[11px] text-slate-600 leading-relaxed">
                💡 <strong>Mẹo tăng chuyển đổi</strong>: Đặt mã voucher ngắn gọn, dễ nhớ (VD: <span className="font-mono font-bold text-[#0045cc]">SALE50K</span>). Khách hàng có xu hướng chốt đơn nhanh hơn khi đơn hàng có mức ưu đãi từ 10% trở lên.
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
            <button
              type="button"
              disabled={isSubmitting}
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-xs font-semibold text-slate-700 transition-colors disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-bold transition-all shadow-sm hover:shadow-md flex items-center gap-1.5 disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <span>{isEditMode ? "Lưu Thay Đổi" : "Tạo Voucher Ngay"}</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
