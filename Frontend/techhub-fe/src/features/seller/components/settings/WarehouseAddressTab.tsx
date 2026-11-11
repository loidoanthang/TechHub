"use client";

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  MapPin,
  Phone,
  User,
  Building2,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Truck,
  Info,
} from "lucide-react";
import {
  warehouseAddressSchema,
  type WarehouseAddressFormValues,
} from "../../schemas/settingsSchema";
import {
  useWarehouseAddressQuery,
  useUpdateWarehouseAddressMutation,
} from "../../hooks/useSellerQueries";

// Danh sách 63 Tỉnh / Thành phố Việt Nam cho datalist gợi ý nhanh
const VIETNAM_PROVINCES = [
  "TP. Hồ Chí Minh",
  "Hà Nội",
  "Đà Nẵng",
  "Hải Phòng",
  "Cần Thơ",
  "An Giang",
  "Bà Rịa - Vũng Tàu",
  "Bắc Giang",
  "Bắc Kạn",
  "Bạc Liêu",
  "Bắc Ninh",
  "Bến Tre",
  "Bình Định",
  "Bình Dương",
  "Bình Phước",
  "Bình Thuận",
  "Cà Mau",
  "Cao Bằng",
  "Đắk Lắk",
  "Đắk Nông",
  "Điện Biên",
  "Đồng Nai",
  "Đồng Tháp",
  "Gia Lai",
  "Hà Giang",
  "Hà Nam",
  "Hà Tĩnh",
  "Hải Dương",
  "Hậu Giang",
  "Hòa Bình",
  "Hưng Yên",
  "Khánh Hòa",
  "Kiên Giang",
  "Kon Tum",
  "Lai Châu",
  "Lâm Đồng",
  "Lạng Sơn",
  "Lào Cai",
  "Long An",
  "Nam Định",
  "Nghệ An",
  "Ninh Bình",
  "Ninh Thuận",
  "Phú Thọ",
  "Phú Yên",
  "Quảng Bình",
  "Quảng Nam",
  "Quảng Ngãi",
  "Quảng Ninh",
  "Quảng Trị",
  "Sóc Trăng",
  "Sơn La",
  "Tây Ninh",
  "Thái Bình",
  "Thái Nguyên",
  "Thanh Hóa",
  "Thừa Thiên Huế",
  "Tiền Giang",
  "Trà Vinh",
  "Tuyên Quang",
  "Vĩnh Long",
  "Vĩnh Phúc",
  "Yên Bái",
];

export function WarehouseAddressTab() {
  const { data: warehouse, isLoading, isError, refetch } = useWarehouseAddressQuery();
  const updateAddressMutation = useUpdateWarehouseAddressMutation();

  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<WarehouseAddressFormValues>({
    resolver: zodResolver(warehouseAddressSchema),
    defaultValues: {
      contactName: "",
      phone: "",
      province: "",
      district: "",
      ward: "",
      streetAddress: "",
    },
  });

  // Sync warehouse data when loaded
  useEffect(() => {
    if (warehouse) {
      reset({
        contactName: warehouse.contactName || "",
        phone: warehouse.phone || "",
        province: warehouse.province || "",
        district: warehouse.district || "",
        ward: warehouse.ward || "",
        streetAddress: warehouse.streetAddress || "",
      });
    }
  }, [warehouse, reset]);

  const watchedValues = watch();
  const fullAddressPreview = [
    watchedValues.streetAddress,
    watchedValues.ward,
    watchedValues.district,
    watchedValues.province,
  ]
    .filter(Boolean)
    .join(", ");

  const onSubmit = async (values: WarehouseAddressFormValues) => {
    setFeedback(null);
    try {
      await updateAddressMutation.mutateAsync({
        contactName: values.contactName.trim(),
        phone: values.phone.trim(),
        province: values.province.trim(),
        district: values.district.trim(),
        ward: values.ward.trim(),
        streetAddress: values.streetAddress.trim(),
      });
      setFeedback({
        type: "success",
        message: "Đã cập nhật địa chỉ kho lấy hàng thành công!",
      });
      reset(values);
    } catch (err: unknown) {
      console.error("Update warehouse error:", err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật thất bại. Vui lòng thử lại sau.";
      setFeedback({ type: "error", message: msg });
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl border border-slate-200/80 p-8 flex flex-col items-center justify-center min-h-[360px] gap-3">
        <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
        <span className="text-xs text-slate-500 font-medium">
          Đang tải địa chỉ kho hàng...
        </span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
        <AlertCircle className="w-8 h-8 text-rose-500" />
        <p className="text-sm font-semibold text-slate-800">
          Không thể tải thông tin địa chỉ kho hàng
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          className="px-4 py-2 text-xs font-bold text-white bg-[#0045cc] rounded-xl hover:bg-[#0d5bff] transition-colors"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* LEFT FORM COLUMN */}
      <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 flex flex-col gap-6">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-[#0b1c30]">
                Địa chỉ Kho hàng Lấy hàng
              </h2>
              <p className="text-xs text-slate-500">
                Nơi nhân viên bưu tá vận chuyển sẽ đến nhận kiện hàng khi bạn chuẩn bị hàng xong.
              </p>
            </div>
          </div>
        </div>

        {/* Feedback Alert */}
        {feedback && (
          <div
            className={`p-3.5 rounded-xl text-xs flex items-center gap-2.5 border animate-in fade-in ${
              feedback.type === "success"
                ? "bg-emerald-50 border-emerald-200 text-emerald-800"
                : "bg-rose-50 border-rose-200 text-rose-800"
            }`}
          >
            {feedback.type === "success" ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span className="font-medium">{feedback.message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
          {/* Row 1: Contact Name & Phone */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-slate-400" />
                <span>Người phụ trách kho</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("contactName")}
                placeholder="VD: Nguyễn Văn Quản Kho"
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.contactName
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.contactName && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.contactName.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <Phone className="w-3.5 h-3.5 text-slate-400" />
                <span>Số điện thoại kho</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="tel"
                {...register("phone")}
                placeholder="VD: 0912345678 hoặc +84912345678"
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 font-mono transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.phone
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.phone && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.phone.message}
                </p>
              )}
            </div>
          </div>

          {/* Row 2: Province & District */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <Building2 className="w-3.5 h-3.5 text-slate-400" />
                <span>Tỉnh / Thành phố</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                list="vietnam-provinces-list"
                {...register("province")}
                placeholder="Chọn hoặc nhập Tỉnh/Thành phố..."
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.province
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              <datalist id="vietnam-provinces-list">
                {VIETNAM_PROVINCES.map((prov) => (
                  <option key={prov} value={prov} />
                ))}
              </datalist>
              {errors.province && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.province.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <span>Quận / Huyện</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("district")}
                placeholder="VD: Quận 1, Quận Cầu Giấy..."
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.district
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.district && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.district.message}
                </p>
              )}
            </div>
          </div>

          {/* Row 3: Ward & Street Address */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="flex flex-col gap-1.5 sm:col-span-1">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <span>Phường / Xã</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("ward")}
                placeholder="VD: Phường Bến Nghé..."
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.ward
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.ward && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.ward.message}
                </p>
              )}
            </div>

            <div className="flex flex-col gap-1.5 sm:col-span-2">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                <span>Địa chỉ chi tiết (Số nhà, Tên đường)</span>
                <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("streetAddress")}
                placeholder="VD: Tòa nhà TechHub, Số 123 Đường Lê Duẩn..."
                className={`w-full h-10 px-3.5 rounded-xl border text-xs text-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] ${
                  errors.streetAddress
                    ? "border-rose-400 bg-rose-50/20"
                    : "border-slate-200 bg-white"
                }`}
              />
              {errors.streetAddress && (
                <p className="text-[11px] text-rose-600 font-medium">
                  {errors.streetAddress.message}
                </p>
              )}
            </div>
          </div>

          {/* Action Footer */}
          <div className="border-t border-slate-100 pt-4 flex items-center justify-between">
            <span className="text-[11px] text-slate-400">
              {isDirty ? "Có thay đổi chưa lưu" : "Dữ liệu địa chỉ đã đồng bộ"}
            </span>
            <button
              type="submit"
              disabled={updateAddressMutation.isPending || !isDirty}
              className="h-10 px-5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] disabled:bg-slate-200 disabled:text-slate-400 disabled:cursor-not-allowed text-white text-xs font-bold transition-all shadow-xs flex items-center gap-2"
            >
              {updateAddressMutation.isPending && (
                <Loader2 className="w-4 h-4 animate-spin" />
              )}
              <span>Lưu địa chỉ kho</span>
            </button>
          </div>
        </form>
      </div>

      {/* RIGHT INFORMATION & PREVIEW COLUMN */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        {/* Full Address Card */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-800 border-b border-slate-100 pb-3">
            <MapPin className="w-4 h-4 text-[#0045cc]" />
            <span>Địa chỉ lấy hàng đầy đủ (In trên vận đơn)</span>
          </div>

          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200/60 flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-900">
                {watchedValues.contactName || "Chưa có tên người phụ trách"}
              </span>
              <span className="text-xs font-mono font-semibold text-[#0045cc]">
                {watchedValues.phone || "Chưa có SĐT"}
              </span>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              {fullAddressPreview || "Vui lòng nhập đầy đủ thông tin địa chỉ kho..."}
            </p>
          </div>

          <div className="text-[11px] text-slate-500 leading-normal flex items-start gap-2">
            <Info className="w-4 h-4 text-[#0045cc] shrink-0 mt-0.5" />
            <span>
              Địa chỉ này sẽ tự động được gán vào mã vận đơn khi đơn vị giao hàng in phiếu lấy hàng tại kho của bạn.
            </span>
          </div>
        </div>

        {/* Logistics Shipping Notice */}
        <div className="bg-[#eff4ff]/60 border border-[#0045cc]/15 rounded-2xl p-4 flex items-start gap-3">
          <div className="w-8 h-8 rounded-xl bg-white text-[#0045cc] flex items-center justify-center shadow-2xs shrink-0">
            <Truck className="w-4 h-4" />
          </div>
          <div className="flex flex-col gap-1 text-xs">
            <span className="font-bold text-[#0b1c30]">
              Quy trình bưu tá lấy hàng
            </span>
            <span className="text-slate-600 leading-relaxed text-[11px]">
              Khi bạn bấm <strong>&quot;Xác nhận chuẩn bị hàng&quot;</strong> tại mục Quản lý đơn hàng, hệ thống sàn TechHub sẽ tự động phát tín hiệu điều phối tài xế bưu tá gần nhất tới địa chỉ kho này trong khung giờ 8:00 - 17:30.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
