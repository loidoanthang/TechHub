"use client";

import React, { useEffect, useRef } from "react";
import {
  AlertCircle,
  Camera,
  Loader2,
  RefreshCw,
  Trash2,
  Upload,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useUploadQueue } from "@/features/uploads/hooks/useUploadQueue";
import type { PresignedUrlResponse } from "@/features/uploads/types";

export interface SingleImageUploaderProps {
  value?: string | null;
  onChange: (url: string) => void;
  onClear?: () => void;
  aspectRatio?: "1:1" | "16:9";
  maxSize: number;
  presignFn: (file: File) => Promise<PresignedUrlResponse>;
  onUploadingChange?: (isUploading: boolean) => void;
  disabled?: boolean;
  className?: string;
}

export function SingleImageUploader({
  value,
  onChange,
  onClear,
  aspectRatio = "1:1",
  maxSize,
  presignFn,
  onUploadingChange,
  disabled = false,
  className,
}: SingleImageUploaderProps) {
  const {
    slots,
    isUploading,
    addFiles,
    removeUpload,
    retryUpload,
    removeUrl,
  } = useUploadQueue({
    value: value ? [value] : [],
    onChange: (urls) => onChange(urls[0] ?? ""),
    maxFiles: 1,
    maxSize,
    presignFn,
  });

  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    onUploadingChange?.(isUploading);
  }, [isUploading, onUploadingChange]);

  const slot = slots[0];
  const aspectClass = aspectRatio === "16:9" ? "aspect-video" : "aspect-square";

  const openPicker = () => {
    if (disabled) return;
    inputRef.current?.click();
  };

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    const currentUrlSlot = slots.find((item) => item.kind === "url");
    if (currentUrlSlot) {
      removeUrl(currentUrlSlot.url);
    }
    addFiles([file]);
  };

  const handleRemove = () => {
    if (slot?.kind === "upload") {
      removeUpload(slot.item.id);
      return;
    }
    if (onClear) {
      onClear();
      return;
    }
    if (slot?.kind === "url") {
      removeUrl(slot.url);
      return;
    }
    onChange("");
  };

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div
        className={cn(
          "relative w-full overflow-hidden rounded-2xl border bg-slate-50",
          aspectClass,
          slot?.kind === "upload" && slot.item.status === "error"
            ? "border-rose-300"
            : "border-slate-200"
        )}
      >
        {slot?.kind === "url" && (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={slot.url}
              alt="Selected image"
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-slate-900/50 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
              <button
                type="button"
                onClick={openPicker}
                disabled={disabled}
                title="Đổi ảnh"
                className="w-9 h-9 rounded-xl bg-white/90 text-slate-700 hover:bg-white flex items-center justify-center shadow disabled:opacity-50"
              >
                <Camera className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={handleRemove}
                disabled={disabled}
                title="Xóa ảnh"
                className="w-9 h-9 rounded-xl bg-white/90 text-rose-600 hover:bg-white flex items-center justify-center shadow disabled:opacity-50"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </>
        )}

        {slot?.kind === "upload" && (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={slot.item.previewUrl}
              alt={slot.item.file.name}
              className="w-full h-full object-cover"
            />

            {slot.item.status === "uploading" && (
              <div className="absolute inset-0 bg-slate-900/50 flex flex-col items-center justify-center text-white">
                <Loader2 className="w-6 h-6 animate-spin" />
                <span className="mt-1 text-xs font-bold">
                  {slot.item.progress}%
                </span>
                <div className="absolute bottom-0 inset-x-0 h-1 bg-white/25">
                  <div
                    className="h-full bg-[#0045cc] transition-all duration-150"
                    style={{ width: `${slot.item.progress}%` }}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => removeUpload(slot.item.id)}
                  title="Hủy tải lên"
                  className="absolute top-2 right-2 px-2 py-1 rounded-lg bg-slate-900/70 hover:bg-rose-600 text-[10px] font-bold text-white"
                >
                  Hủy
                </button>
              </div>
            )}

            {slot.item.status === "error" && (
              <div className="absolute inset-0 bg-rose-950/60 p-3 flex flex-col items-center justify-center gap-2 text-center text-white">
                <AlertCircle className="w-5 h-5" />
                <span className="text-[10px] leading-tight font-medium">
                  {slot.item.errorMessage || "Upload failed"}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => retryUpload(slot.item.id)}
                    disabled={disabled}
                    className="px-2 py-1 rounded-lg bg-white/90 text-amber-700 text-[10px] font-bold flex items-center gap-1 disabled:opacity-50"
                  >
                    <RefreshCw className="w-3 h-3" />
                    Thử lại
                  </button>
                  <button
                    type="button"
                    onClick={() => removeUpload(slot.item.id)}
                    disabled={disabled}
                    className="px-2 py-1 rounded-lg bg-white/90 text-rose-600 text-[10px] font-bold flex items-center gap-1 disabled:opacity-50"
                  >
                    <Trash2 className="w-3 h-3" />
                    Xóa
                  </button>
                </div>
              </div>
            )}
          </>
        )}

        {!slot && (
          <button
            type="button"
            onClick={openPicker}
            disabled={disabled}
            className="absolute inset-0 flex flex-col items-center justify-center gap-1.5 text-slate-500 hover:text-[#0045cc] hover:bg-[#eff4ff]/60 transition-colors disabled:opacity-50"
          >
            <span className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center shadow-2xs">
              <Upload className="w-4 h-4" />
            </span>
            <span className="text-[11px] font-semibold">Chọn ảnh</span>
            <span className="text-[10px] text-slate-400">
              JPG, PNG, WEBP
            </span>
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        onChange={handleInputChange}
        className="hidden"
        disabled={disabled || isUploading}
      />
    </div>
  );
}
