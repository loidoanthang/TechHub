"use client";

import React, { useState, useEffect } from "react";
import Image from "next/image";
import { ProductThumbnail } from "@/components/shared";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  Star,
  Upload,
  ImagePlus,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  Edit2,
} from "lucide-react";
import type { BuyerOrderItemResponse } from "@/types/order";
import type { CreateReviewFormValues } from "@/features/account/schemas/reviewSchema";
import { createReviewSchema } from "@/features/account/schemas/reviewSchema";
import {
  useCreateReviewMutation,
  useUpdateReviewMutation,
} from "@/features/account/api/buyerReviewApi";

interface ReviewProductModalProps {
  item: BuyerOrderItemResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (message: string) => void;
  initialData?: {
    reviewId?: string;
    rating: number;
    comment?: string;
    mediaList?: Array<{
      mediaUrl: string;
      mediaType: "IMAGE" | "VIDEO";
      displayOrder: number;
    }>;
  };
}

const RATING_DESCRIPTIONS = [
  "Rất tệ - Sản phẩm có lỗi hoặc không thể sử dụng",
  "Chưa tốt - Không đạt như kỳ vọng ban đầu",
  "Bình thường - Đạt mức sử dụng cơ bản",
  "Hài lòng - Chất lượng tốt, đóng gói chu đáo",
  "Tuyệt vời - Vượt xa mong đợi, rất đáng tiền!",
];

const QUICK_CHIPS = [
  "Hàng chính hãng",
  "Đóng gói cẩn thận",
  "Giao hàng siêu nhanh",
  "Hiệu năng mạnh mẽ",
  "Đúng mô tả",
];

const SAMPLE_HARDWARE_IMAGES = [
  {
    name: "Ảnh mở hộp linh kiện",
    url: "/images/sample-evidence.svg",
  },
  {
    name: "Ảnh góc máy / thiết bị",
    url: "/images/sample-evidence.svg",
  },
  {
    name: "Ảnh phụ kiện kèm theo",
    url: "/images/sample-evidence.svg",
  },
  {
    name: "Ảnh kiểm tra hoạt động",
    url: "/images/sample-evidence.svg",
  },
];

export function ReviewProductModal({
  item,
  isOpen,
  onClose,
  onSuccess,
  initialData,
}: ReviewProductModalProps) {
  const [hoverRating, setHoverRating] = useState(0);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const createReviewMutation = useCreateReviewMutation();
  const updateReviewMutation = useUpdateReviewMutation();

  const isEditMode = Boolean(initialData?.reviewId);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateReviewFormValues>({
    resolver: zodResolver(createReviewSchema),
    defaultValues: {
      orderItemId: item?.orderItemId || "",
      rating: initialData?.rating || 5,
      comment: initialData?.comment || "",
      mediaList: initialData?.mediaList || [],
    },
  });

  const rating = watch("rating");
  const comment = watch("comment") || "";
  const mediaList = watch("mediaList") || [];

  useEffect(() => {
    if (item && isOpen) {
      reset({
        orderItemId: item.orderItemId,
        rating: initialData?.rating || 5,
        comment: initialData?.comment || "",
        mediaList: initialData?.mediaList || [],
      });
      setErrorMsg(null);
    }
  }, [item, isOpen, initialData, reset]);

  if (!isOpen || !item) return null;

  const currentDescription =
    RATING_DESCRIPTIONS[(hoverRating || rating) - 1] || RATING_DESCRIPTIONS[4];

  const handleChipClick = (chip: string) => {
    const trimmed = comment.trim();
    if (!trimmed) {
      setValue("comment", chip, { shouldValidate: true });
    } else if (!trimmed.includes(chip)) {
      setValue("comment", `${trimmed}, ${chip.toLowerCase()}`, {
        shouldValidate: true,
      });
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const remainingSlots = 5 - mediaList.length;
    if (remainingSlots <= 0) return;

    // Use hosted static image to avoid invalid temporary client blob URLs
    const samplePool = [
      "/images/sample-evidence.svg",
    ];

    const files = Array.from(e.target.files).slice(0, remainingSlots);
    const newMediaItems = files.map((_, idx) => ({
      mediaUrl: samplePool[(mediaList.length + idx) % samplePool.length],
      mediaType: "IMAGE" as const,
      displayOrder: mediaList.length + idx,
    }));

    setValue("mediaList", [...mediaList, ...newMediaItems], {
      shouldValidate: true,
    });
    e.target.value = "";
  };

  const handleAddSampleImage = (url: string) => {
    if (mediaList.length >= 5) return;
    if (mediaList.some((m) => m.mediaUrl === url)) return;

    setValue(
      "mediaList",
      [
        ...mediaList,
        {
          mediaUrl: url,
          mediaType: "IMAGE" as const,
          displayOrder: mediaList.length,
        },
      ],
      { shouldValidate: true }
    );
  };

  const handleRemoveMedia = (index: number) => {
    const updated = mediaList
      .filter((_, idx) => idx !== index)
      .map((m, idx) => ({ ...m, displayOrder: idx }));
    setValue("mediaList", updated, { shouldValidate: true });
  };

  const onSubmit = async (values: CreateReviewFormValues) => {
    setErrorMsg(null);
    try {
      // Ensure only valid public HTTPS/HTTP URLs are sent
      const validMedia = (values.mediaList || [])
        .filter(
          (m) =>
            m.mediaUrl.startsWith("http://") || m.mediaUrl.startsWith("https://")
        )
        .map((m, idx) => ({ ...m, displayOrder: idx }));

      if (isEditMode && initialData?.reviewId) {
        await updateReviewMutation.mutateAsync({
          id: initialData.reviewId,
          data: {
            rating: values.rating,
            comment: values.comment?.trim() || undefined,
            mediaList: validMedia.length > 0 ? validMedia : undefined,
          },
        });
        onSuccess?.("Cập nhật đánh giá thành công!");
      } else {
        await createReviewMutation.mutateAsync({
          orderItemId: values.orderItemId,
          rating: values.rating,
          comment: values.comment?.trim() || undefined,
          mediaList: validMedia.length > 0 ? validMedia : undefined,
        });
        onSuccess?.("Cảm ơn bạn đã gửi đánh giá cho sản phẩm!");
      }

      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể gửi đánh giá. Vui lòng thử lại sau.";
      setErrorMsg(msg);
    }
  };

  const isPending =
    isSubmitting ||
    createReviewMutation.isPending ||
    updateReviewMutation.isPending;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in"
    >
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-600 flex items-center justify-center">
              {isEditMode ? <Edit2 className="w-4 h-4" /> : <Star className="w-4 h-4 fill-amber-500" />}
            </div>
            <div>
              <h2 className="text-base font-bold text-[#0b1c30]">
                {isEditMode ? "Chỉnh sửa Đánh giá" : "Đánh giá Sản phẩm"}
              </h2>
              <p className="text-[11px] text-[#737688]">
                Chia sẻ trải nghiệm sử dụng thực tế của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-200/60 text-slate-500 flex items-center justify-center transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit(onSubmit)} className="overflow-y-auto p-6 space-y-5">
          {/* Product Mini Bar */}
          <div className="flex items-center gap-3 p-3 rounded-2xl bg-[#eff4ff]/40 border border-[#c3c5d9]/30">
            <ProductThumbnail
              src={item.imageUrl}
              alt={item.productName}
              sizeClassName="w-12 h-12"
              imageClassName="object-contain p-1"
            />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold text-[#0b1c30] truncate">
                {item.productName}
              </p>
              {item.variantName && (
                <p className="text-[11px] text-[#737688] truncate mt-0.5">
                  Phân loại: {item.variantName}
                </p>
              )}
            </div>
          </div>

          {/* Star Rating Selector */}
          <div className="flex flex-col items-center justify-center py-2 bg-[#f8faff] rounded-2xl border border-slate-100 gap-2">
            <span className="text-xs font-semibold text-[#434656]">
              Chất lượng sản phẩm
            </span>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((starVal) => {
                const isFilled = (hoverRating || rating) >= starVal;
                return (
                  <button
                    key={starVal}
                    type="button"
                    onMouseEnter={() => setHoverRating(starVal)}
                    onMouseLeave={() => setHoverRating(0)}
                    onClick={() => setValue("rating", starVal, { shouldValidate: true })}
                    className="p-1 hover:scale-125 transition-transform cursor-pointer focus:outline-none"
                    aria-label={`${starVal} sao`}
                  >
                    <Star
                      className={`w-7 h-7 transition-colors ${
                        isFilled
                          ? "fill-amber-400 text-amber-400 drop-shadow-sm"
                          : "text-slate-300"
                      }`}
                    />
                  </button>
                );
              })}
            </div>
            <span className="text-xs font-bold text-amber-700 font-mono transition-all">
              {currentDescription}
            </span>
            {errors.rating && (
              <p className="text-[11px] text-rose-500 font-medium">
                {errors.rating.message}
              </p>
            )}
          </div>

          {/* Quick Review Chips */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-[#0b1c30] flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-[#0045cc]" />
              <span>Gợi ý nhận xét nhanh:</span>
            </label>
            <div className="flex flex-wrap gap-1.5">
              {QUICK_CHIPS.map((chip) => (
                <button
                  key={chip}
                  type="button"
                  onClick={() => handleChipClick(chip)}
                  className="px-2.5 py-1 rounded-lg text-[11px] font-medium bg-slate-100 hover:bg-[#eff4ff] text-[#434656] hover:text-[#0045cc] border border-slate-200 transition-colors cursor-pointer"
                >
                  +{chip}
                </button>
              ))}
            </div>
          </div>

          {/* Comment Textarea */}
          <div className="space-y-1.5">
            <div className="flex justify-between items-center text-xs">
              <label className="font-semibold text-[#0b1c30]">
                Cảm nhận chi tiết (tùy chọn)
              </label>
              <span className="text-[11px] text-[#737688] font-mono">
                {comment.length}/1000
              </span>
            </div>
            <textarea
              {...register("comment")}
              rows={3}
              maxLength={1000}
              placeholder="Hãy chia sẻ thêm về trải nghiệm dùng thử, chất lượng hoàn thiện, đóng gói..."
              className="w-full p-3 rounded-xl border border-slate-200 focus:border-[#0045cc] focus:ring-2 focus:ring-[#0045cc]/10 text-xs text-[#0b1c30] placeholder:text-slate-400 outline-none transition-all resize-none"
            />
            {errors.comment && (
              <p className="text-[11px] text-rose-500 font-medium">
                {errors.comment.message}
              </p>
            )}
          </div>

          {/* Photo Uploader */}
          <div className="space-y-2">
            <div className="flex justify-between items-center text-xs">
              <label className="font-semibold text-[#0b1c30] flex items-center gap-1.5">
                <ImagePlus className="w-4 h-4 text-[#0045cc]" />
                <span>Thêm hình ảnh thực tế</span>
              </label>
              <span className="text-[11px] text-[#737688] font-mono">
                {mediaList.length}/5 ảnh (tối đa 5)
              </span>
            </div>

            {/* Media Thumbnails preview */}
            <div className="flex flex-wrap gap-2.5">
              {mediaList.map((m, idx) => (
                <div
                  key={idx}
                  className="relative w-16 h-16 rounded-xl overflow-hidden border border-slate-200 bg-slate-50 group shadow-2xs"
                >
                  <Image
                    src={m.mediaUrl}
                    alt={`Ảnh đánh giá ${idx + 1}`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                  <button
                    type="button"
                    onClick={() => handleRemoveMedia(idx)}
                    className="absolute top-1 right-1 w-5 h-5 rounded-full bg-slate-900/80 hover:bg-rose-600 text-white flex items-center justify-center transition-colors shadow-sm"
                    aria-label="Xóa ảnh"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}

              {mediaList.length < 5 && (
                <label className="w-16 h-16 rounded-xl border-2 border-dashed border-slate-200 hover:border-[#0045cc] flex flex-col items-center justify-center gap-1 cursor-pointer bg-slate-50/50 hover:bg-[#eff4ff]/50 transition-colors">
                  <Upload className="w-4 h-4 text-slate-400 group-hover:text-[#0045cc]" />
                  <span className="text-[10px] text-slate-500 font-medium leading-none">
                    Tải ảnh
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>

            {/* Sample Images quick picker */}
            {mediaList.length < 5 && (
              <div className="pt-2 border-t border-dashed border-slate-200">
                <span className="text-[11px] text-slate-500 block mb-1.5 font-medium">
                  Hoặc chọn nhanh ảnh chụp mẫu:
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {SAMPLE_HARDWARE_IMAGES.map((sample, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handleAddSampleImage(sample.url)}
                      className="px-2 py-1 rounded-lg text-[10px] bg-slate-100 hover:bg-blue-50 text-slate-600 hover:text-[#0045cc] border border-slate-200 transition-colors cursor-pointer"
                    >
                      +{sample.name}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Error Message */}
          {errorMsg && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-rose-700 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Submit Actions */}
          <div className="pt-2 flex items-center justify-end gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs font-semibold cursor-pointer transition-colors"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="px-5 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0037a3] text-white text-xs font-bold shadow-md hover:shadow-lg transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang gửi...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>{isEditMode ? "Lưu thay đổi" : "Gửi đánh giá"}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
