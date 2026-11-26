"use client";

import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AxiosError } from "axios";
import {
  Save,
  ShieldCheck,
  Mail,
  Loader2,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";
import {
  useProfileQuery,
  useUpdateProfileMutation,
} from "../hooks/useAccountQueries";
import { SingleImageUploader } from "@/components/shared/upload/SingleImageUploader";
import { uploadApi } from "@/lib/api/upload";
import { UPLOAD_LIMITS } from "@/features/uploads/types";
import {
  profileSchema,
  type ProfileFormValues,
} from "../schemas/profileSchema";

export function ProfileView() {
  const { data: profile, isLoading, error } = useProfileQuery();
  const updateProfileMutation = useUpdateProfileMutation();

  const [isAvatarUploading, setIsAvatarUploading] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const hasInitializedRef = useRef(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    setError,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    mode: "onTouched",
    defaultValues: {
      firstName: "",
      lastName: "",
      phone: "",
      avatarUrl: null,
    },
  });

  const watchedAvatar = watch("avatarUrl");

  // Sync profile data once or when form is pristine
  useEffect(() => {
    if (profile && (!hasInitializedRef.current || !isDirty)) {
      reset({
        firstName: profile.firstName || "",
        lastName: profile.lastName || "",
        phone: profile.phone || "",
        avatarUrl: profile.avatarUrl || null,
      });
      hasInitializedRef.current = true;
    }
  }, [profile, reset, isDirty]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const handleCancel = () => {
    if (profile) {
      reset({
        firstName: profile.firstName || "",
        lastName: profile.lastName || "",
        phone: profile.phone || "",
        avatarUrl: profile.avatarUrl || null,
      });
    }
  };

  const onSubmit = async (values: ProfileFormValues) => {
    try {
      const sanitizedPhone = values.phone.replace(/[\s\-\(\)\.]/g, "");
      const res = await updateProfileMutation.mutateAsync({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        phone: sanitizedPhone,
        avatarUrl: values.avatarUrl || null,
      });

      // Re-baseline form on success
      reset({
        firstName: res.data.firstName || "",
        lastName: res.data.lastName || "",
        phone: res.data.phone || "",
        avatarUrl: res.data.avatarUrl || null,
      });

      showToast("Đã lưu thông tin hồ sơ thành công.");
    } catch (err: unknown) {
      if (err instanceof AxiosError && err.response) {
        const errorData = err.response.data as { message?: string } | undefined;
        const msg = errorData?.message || "Cập nhật hồ sơ thất bại.";
        if (
          err.response.status === 409 ||
          msg.toLowerCase().includes("phone")
        ) {
          setError("phone", { type: "server", message: msg });
        }
        showToast(msg);
      } else {
        showToast("Lỗi kết nối mạng. Vui lòng thử lại.");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="w-full h-96 flex items-center justify-center bg-white rounded-2xl border border-[#c3c5d9]/25">
        <Loader2 className="w-8 h-8 animate-spin text-[#0045cc]" />
      </div>
    );
  }

  return (
    <div className="w-full h-full flex flex-col gap-6">
      <form
        onSubmit={handleSubmit(onSubmit)}
        className="bg-white rounded-2xl p-4 sm:p-8 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-6 flex-1 h-full"
      >
        <div className="flex flex-col gap-1 border-b border-[#eff4ff] pb-4">
          <h1 className="font-heading text-xl sm:text-2xl font-bold text-[#0b1c30] tracking-tight">
            Hồ sơ cá nhân &amp; Thông tin liên hệ
          </h1>
          <p className="text-xs text-[#737688]">
            Quản lý thông tin cá nhân, số điện thoại xác thực và email tài khoản.
          </p>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>Không thể tải hồ sơ từ máy chủ. Đang hiển thị dữ liệu lưu tạm.</span>
          </div>
        )}

        <div className="flex flex-col-reverse md:flex-row items-stretch gap-8 lg:gap-10 flex-1">
          <div className="flex-1 w-full flex flex-col gap-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <label
                  htmlFor="first-name"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Họ
                </label>
                <input
                  id="first-name"
                  type="text"
                  placeholder="Nguyễn"
                  aria-invalid={!!errors.firstName}
                  aria-describedby={
                    errors.firstName ? "first-name-error" : undefined
                  }
                  {...register("firstName")}
                  className={`w-full h-11 px-4 rounded-xl text-[#0b1c30] text-sm outline-none transition-all border ${
                    errors.firstName
                      ? "border-rose-400 bg-rose-50/20 focus:ring-2 focus:ring-rose-500/20"
                      : "bg-[#eff4ff]/60 border-[#c3c5d9]/20 focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20"
                  }`}
                />
                {errors.firstName && (
                  <span
                    id="first-name-error"
                    role="alert"
                    className="text-[11px] font-medium text-rose-500"
                  >
                    {errors.firstName.message}
                  </span>
                )}
              </div>

              <div className="flex flex-col gap-1.5">
                <label
                  htmlFor="last-name"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Tên
                </label>
                <input
                  id="last-name"
                  type="text"
                  placeholder="Văn A"
                  aria-invalid={!!errors.lastName}
                  aria-describedby={
                    errors.lastName ? "last-name-error" : undefined
                  }
                  {...register("lastName")}
                  className={`w-full h-11 px-4 rounded-xl text-[#0b1c30] text-sm outline-none transition-all border ${
                    errors.lastName
                      ? "border-rose-400 bg-rose-50/20 focus:ring-2 focus:ring-rose-500/20"
                      : "bg-[#eff4ff]/60 border-[#c3c5d9]/20 focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20"
                  }`}
                />
                {errors.lastName && (
                  <span
                    id="last-name-error"
                    role="alert"
                    className="text-[11px] font-medium text-rose-500"
                  >
                    {errors.lastName.message}
                  </span>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="phone-number"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Số điện thoại
              </label>
              <input
                id="phone-number"
                type="tel"
                placeholder="0912345678"
                aria-invalid={!!errors.phone}
                aria-describedby={errors.phone ? "phone-error" : undefined}
                {...register("phone")}
                className={`w-full h-11 px-4 rounded-xl text-[#0b1c30] text-sm outline-none transition-all border ${
                  errors.phone
                    ? "border-rose-400 bg-rose-50/20 focus:ring-2 focus:ring-rose-500/20"
                    : "bg-[#eff4ff]/60 border-[#c3c5d9]/20 focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20"
                }`}
              />
              {errors.phone && (
                <span
                  id="phone-error"
                  role="alert"
                  className="text-[11px] font-medium text-rose-500"
                >
                  {errors.phone.message}
                </span>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="email-address"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Email đã đăng ký
                </label>
                {profile?.emailVerified && (
                  <span className="text-[11px] font-semibold text-emerald-600 flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    Đã xác thực
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <input
                  id="email-address"
                  type="email"
                  value={profile?.email || ""}
                  disabled
                  className="w-full h-11 pl-4 pr-10 rounded-xl bg-neutral-100 text-[#737688] text-sm outline-none cursor-not-allowed select-none border border-[#c3c5d9]/30"
                />
                <Mail className="absolute right-3.5 w-4 h-4 text-[#737688]" />
              </div>
            </div>

            <div className="pt-4 flex items-center gap-3 mt-auto">
              <button
                type="button"
                onClick={handleCancel}
                disabled={!isDirty || updateProfileMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] disabled:opacity-40 disabled:cursor-not-allowed text-[#0b1c30] text-xs font-semibold transition-colors cursor-pointer"
              >
                Hủy thay đổi
              </button>
              <button
                type="submit"
                disabled={
                  updateProfileMutation.isPending ||
                  isAvatarUploading ||
                  !isDirty
                }
                className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-all shadow-sm flex items-center justify-center gap-2 active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                {isAvatarUploading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang tải ảnh...</span>
                  </>
                ) : updateProfileMutation.isPending ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4" />
                    <span>Lưu thay đổi</span>
                  </>
                )}
              </button>
            </div>
          </div>

          <div className="w-full md:w-64 flex flex-col items-center justify-center text-center p-6 bg-[#eff4ff]/40 md:bg-transparent rounded-2xl md:rounded-none md:border-l md:border-[#eff4ff] shrink-0 gap-5 self-stretch">
            <div className="w-32">
              <SingleImageUploader
                value={watchedAvatar || null}
                onChange={(url) =>
                  setValue("avatarUrl", url || null, {
                    shouldDirty: true,
                    shouldValidate: true,
                  })
                }
                maxSize={UPLOAD_LIMITS.USER_AVATAR}
                presignFn={uploadApi.getAvatarPresignedUrl}
                onUploadingChange={setIsAvatarUploading}
                aspectRatio="1:1"
                disabled={updateProfileMutation.isPending}
                className="rounded-full overflow-hidden ring-4 ring-[#0045cc]/15 shadow-sm"
              />
            </div>

            <div className="text-[11px] text-[#737688] leading-relaxed flex flex-col gap-0.5">
              <span>Dung lượng tối đa: 2 MB</span>
              <span>Định dạng hỗ trợ: .JPEG, .PNG, .WEBP</span>
            </div>
          </div>
        </div>
      </form>

      <div
        className={`fixed bottom-8 right-8 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 transform transition-all duration-300 ${
          toastMessage
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-12 opacity-0 pointer-events-none"
        }`}
      >
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <div className="flex flex-col text-left">
          <span className="text-xs font-bold">Cập nhật hồ sơ</span>
          <span className="text-[11px] text-neutral-300">{toastMessage}</span>
        </div>
      </div>
    </div>
  );
}
