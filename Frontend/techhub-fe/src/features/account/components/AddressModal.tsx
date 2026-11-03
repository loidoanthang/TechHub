"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, Home, Building2, MapPin, AlertCircle, Loader2 } from "lucide-react";
import type { AddressResponse } from "@/types/address";
import type { AddressLabel } from "@/types/enums";
import {
  addressSchema,
  type AddressFormData,
} from "../schemas/addressSchema";

// 63 Tỉnh/Thành phố Việt Nam cho datalist gợi ý nhanh
const VIETNAM_PROVINCES_LIST = [
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

interface AddressModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (address: {
    recipientName: string;
    phone: string;
    province: string;
    district: string;
    ward: string;
    streetAddress: string;
    label: AddressLabel;
    isDefault?: boolean;
  }) => void;
  editingAddress?: AddressResponse | null;
  isLoading?: boolean;
  serverError?: string | null;
  isFirstAddress?: boolean;
}

export function AddressModal({
  isOpen,
  onClose,
  onSave,
  editingAddress,
  isLoading,
  serverError,
  isFirstAddress,
}: AddressModalProps) {
  const isCreatingFirst = !editingAddress && !!isFirstAddress;
  const isDefaultDisabled = Boolean(editingAddress?.isDefault || isCreatingFirst);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<AddressFormData>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      recipientName: "",
      phone: "",
      province: "",
      district: "",
      ward: "",
      streetAddress: "",
      label: "HOME",
      isDefault: isCreatingFirst,
    },
    mode: "onTouched",
  });

  const selectedLabel = watch("label");
  const currentRecipientName = watch("recipientName") || "";
  const currentStreetAddress = watch("streetAddress") || "";

  // Reset form khi modal mở hoặc thay đổi địa chỉ cần edit
  useEffect(() => {
    if (editingAddress) {
      reset({
        recipientName: editingAddress.recipientName,
        phone: editingAddress.phone,
        province: editingAddress.province,
        district: editingAddress.district,
        ward: editingAddress.ward,
        streetAddress: editingAddress.streetAddress,
        label: editingAddress.label || "HOME",
        isDefault: editingAddress.isDefault,
      });
    } else {
      reset({
        recipientName: "",
        phone: "",
        province: "",
        district: "",
        ward: "",
        streetAddress: "",
        label: "HOME",
        isDefault: isCreatingFirst,
      });
    }
  }, [editingAddress, isOpen, isCreatingFirst, reset]);

  if (!isOpen) return null;

  const onValidSubmit = (data: AddressFormData) => {
    // Chuẩn hóa loại bỏ khoảng trắng, dấu ngoặc, dấu gạch ngang của số điện thoại
    const cleanedPhone = data.phone.replace(/[\s\-\(\)\.]/g, "");

    onSave({
      recipientName: data.recipientName.trim(),
      phone: cleanedPhone,
      province: data.province.trim(),
      district: data.district.trim(),
      ward: data.ward.trim(),
      streetAddress: data.streetAddress.trim(),
      label: data.label,
      isDefault: isCreatingFirst ? true : data.isDefault,
    });
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="fixed inset-0" onClick={onClose} aria-hidden="true" />

      <div className="relative z-10 w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200/80 p-6 sm:p-7 flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center">
              <MapPin className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 font-heading">
                {editingAddress ? "Cập nhật địa chỉ nhận hàng" : "Thêm địa chỉ mới"}
              </h2>
              <p className="text-xs text-slate-500">
                {editingAddress
                  ? "Chỉnh sửa thông tin địa chỉ giao nhận hiện có"
                  : "Nhập thông tin người nhận để giao hàng nhanh chóng"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
            aria-label="Đóng cửa sổ"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Server Error Banner */}
        {serverError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
            <span className="leading-relaxed font-medium">{serverError}</span>
          </div>
        )}

        {/* Form Body validated by React Hook Form & Zod */}
        <form onSubmit={handleSubmit(onValidSubmit)} className="flex flex-col gap-4">
          {/* Recipient Full Name */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
              <span>
                Họ và tên người nhận <span className="text-rose-500">*</span>
              </span>
              <span className="text-[10px] text-slate-400 font-normal">
                {currentRecipientName.length}/150
              </span>
            </label>
            <input
              type="text"
              {...register("recipientName")}
              maxLength={150}
              placeholder="VD: Nguyễn Văn A"
              className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                errors.recipientName
                  ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                  : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
              }`}
            />
            {errors.recipientName && (
              <span className="text-[11px] text-rose-600 font-medium">
                {errors.recipientName.message}
              </span>
            )}
          </div>

          {/* Phone Number */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Số điện thoại liên hệ <span className="text-rose-500">*</span>
            </label>
            <input
              type="tel"
              {...register("phone")}
              placeholder="VD: 0912345678 hoặc +84912345678"
              className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                errors.phone
                  ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                  : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
              }`}
            />
            {errors.phone ? (
              <span className="text-[11px] text-rose-600 font-medium">
                {errors.phone.message}
              </span>
            ) : (
              <span className="text-[11px] text-slate-400">
                Định dạng: 10 chữ số (đầu 03, 05, 07, 08, 09) hoặc quốc tế +84.
              </span>
            )}
          </div>

          {/* Province / City & District Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Tỉnh / Thành phố <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                list="vn-provinces-list"
                {...register("province")}
                maxLength={100}
                placeholder="VD: TP. Hồ Chí Minh"
                className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                  errors.province
                    ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                    : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
                }`}
              />
              <datalist id="vn-provinces-list">
                {VIETNAM_PROVINCES_LIST.map((prov) => (
                  <option key={prov} value={prov} />
                ))}
              </datalist>
              {errors.province && (
                <span className="text-[11px] text-rose-600 font-medium">
                  {errors.province.message}
                </span>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Quận / Huyện <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                {...register("district")}
                maxLength={100}
                placeholder="VD: Quận 1"
                className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                  errors.district
                    ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                    : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
                }`}
              />
              {errors.district && (
                <span className="text-[11px] text-rose-600 font-medium">
                  {errors.district.message}
                </span>
              )}
            </div>
          </div>

          {/* Ward / Sub-district */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Phường / Xã <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              {...register("ward")}
              maxLength={100}
              placeholder="VD: Phường Đa Kao"
              className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                errors.ward
                  ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                  : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
              }`}
            />
            {errors.ward && (
              <span className="text-[11px] text-rose-600 font-medium">
                {errors.ward.message}
              </span>
            )}
          </div>

          {/* Street Address */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
              <span>
                Địa chỉ cụ thể (Số nhà, tên đường, tòa nhà...) <span className="text-rose-500">*</span>
              </span>
              <span className="text-[10px] text-slate-400 font-normal">
                {currentStreetAddress.length}/500
              </span>
            </label>
            <input
              type="text"
              {...register("streetAddress")}
              maxLength={500}
              placeholder="VD: 123 Đường Điện Biên Phủ, Tầng 12"
              className={`rounded-xl border px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
                errors.streetAddress
                  ? "border-rose-400 bg-rose-50/20 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/15"
                  : "border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/15"
              }`}
            />
            {errors.streetAddress && (
              <span className="text-[11px] text-rose-600 font-medium">
                {errors.streetAddress.message}
              </span>
            )}
          </div>

          {/* Address Type Tag */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Loại địa chỉ
            </label>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setValue("label", "HOME", { shouldValidate: true })}
                className={`px-3.5 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  selectedLabel === "HOME"
                    ? "bg-[#eff4ff] text-[#0045cc] border border-[#0045cc]/40 font-bold shadow-xs"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Home className="w-3.5 h-3.5" />
                <span>Nhà riêng</span>
              </button>

              <button
                type="button"
                onClick={() => setValue("label", "OFFICE", { shouldValidate: true })}
                className={`px-3.5 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  selectedLabel === "OFFICE"
                    ? "bg-purple-50 text-purple-700 border border-purple-200 font-bold shadow-xs"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Building2 className="w-3.5 h-3.5" />
                <span>Văn phòng</span>
              </button>

              <button
                type="button"
                onClick={() => setValue("label", "OTHER", { shouldValidate: true })}
                className={`px-3.5 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all cursor-pointer ${
                  selectedLabel === "OTHER"
                    ? "bg-slate-100 text-slate-800 border border-slate-300 font-bold shadow-xs"
                    : "border border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <MapPin className="w-3.5 h-3.5" />
                <span>Khác</span>
              </button>
            </div>
          </div>

          {/* Set as Default Checkbox */}
          <div className="flex items-center gap-2.5 pt-1">
            <input
              id="default-address-checkbox"
              type="checkbox"
              {...register("isDefault")}
              disabled={isDefaultDisabled}
              className="h-4 w-4 rounded border-slate-300 accent-[#0045cc] cursor-pointer disabled:cursor-not-allowed disabled:opacity-40"
            />
            <label
              htmlFor="default-address-checkbox"
              className={`text-xs select-none ${
                isDefaultDisabled
                  ? "text-slate-400 cursor-not-allowed"
                  : "text-slate-700 cursor-pointer"
              }`}
            >
              Đặt làm địa chỉ mặc định
            </label>
          </div>

          {/* Modal Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="px-5 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold shadow-sm transition-all cursor-pointer active:scale-[0.99] disabled:opacity-50 flex items-center gap-2"
            >
              {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              <span>
                {isLoading
                  ? "Đang lưu..."
                  : editingAddress
                  ? "Cập nhật địa chỉ"
                  : "Lưu địa chỉ"}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
