"use client";

import React, { useEffect, useRef, useState } from "react";
import {
  AlertCircle,
  ImagePlus,
  Loader2,
  RefreshCw,
  Star,
  Trash2,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useUploadQueue } from "@/features/uploads/hooks/useUploadQueue";
import type { PresignedUrlResponse } from "@/features/uploads/types";

export interface GalleryImageUploaderProps {
  value: string[];
  onChange: (urls: string[]) => void;
  maxFiles: number;
  maxSize: number;
  presignFn: (file: File) => Promise<PresignedUrlResponse>;
  onUploadingChange?: (isUploading: boolean) => void;
  isCoverSelectorEnabled?: boolean;
  disabled?: boolean;
  className?: string;
}

export function GalleryImageUploader({
  value,
  onChange,
  maxFiles,
  maxSize,
  presignFn,
  onUploadingChange,
  isCoverSelectorEnabled = false,
  disabled = false,
  className,
}: GalleryImageUploaderProps) {
  const {
    slots,
    isUploading,
    addFiles,
    removeUpload,
    retryUpload,
    removeUrl,
    promoteUrl,
  } = useUploadQueue({ value, onChange, maxFiles, maxSize, presignFn });

  const inputRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    onUploadingChange?.(isUploading);
  }, [isUploading, onUploadingChange]);

  const isFull = slots.length >= maxFiles;

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files) {
      addFiles(event.target.files);
    }
    event.target.value = "";
  };

  const openPicker = () => {
    if (disabled) return;
    inputRef.current?.click();
  };

  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
        {slots.map((slot, index) => {
          const isCover = index === 0 && isCoverSelectorEnabled;

          if (slot.kind === "url") {
            return (
              <div
                key={slot.key}
                className="group relative aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-slate-50 shadow-2xs"
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={slot.url}
                  alt={`Image ${index + 1}`}
                  className="w-full h-full object-cover transition-transform duration-200 group-hover:scale-105"
                />

                {isCover && (
                  <span className="absolute top-2 left-2 z-10 px-2 py-0.5 rounded-lg bg-[#0045cc] text-white text-[10px] font-bold shadow-sm">
                    Ảnh bìa
                  </span>
                )}

                <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 p-2">
                  {isCoverSelectorEnabled && !isCover && (
                    <button
                      type="button"
                      onClick={() => promoteUrl(slot.url)}
                      disabled={disabled}
                      title="Đặt làm ảnh bìa"
                      className="w-7 h-7 rounded-lg bg-white/90 text-amber-500 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow disabled:opacity-50"
                    >
                      <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" />
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => setPreviewUrl(slot.url)}
                    title="Xem ảnh lớn"
                    className="w-7 h-7 rounded-lg bg-white/90 text-slate-700 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow"
                  >
                    <ImagePlus className="w-3.5 h-3.5" />
                  </button>

                  <button
                    type="button"
                    onClick={() => removeUrl(slot.url)}
                    disabled={disabled}
                    title="Xóa ảnh"
                    className="w-7 h-7 rounded-lg bg-white/90 text-rose-600 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow disabled:opacity-50"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          }

          const { item } = slot;
          const isError = item.status === "error";
          const isItemUploading = item.status === "uploading";

          return (
            <div
              key={slot.key}
              className={cn(
                "relative aspect-square rounded-2xl overflow-hidden border shadow-2xs",
                isError
                  ? "border-rose-300 bg-rose-50"
                  : "border-slate-200 bg-slate-50"
              )}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={item.previewUrl}
                alt={item.file.name}
                className="w-full h-full object-cover"
              />

              {isCover && (
                <span className="absolute top-2 left-2 z-10 px-2 py-0.5 rounded-lg bg-[#0045cc] text-white text-[10px] font-bold shadow-sm">
                  Ảnh bìa
                </span>
              )}

              {isItemUploading && (
                <div className="absolute inset-0 bg-slate-900/45 flex flex-col items-center justify-center text-white">
                  <Loader2 className="w-5 h-5 animate-spin" />
                  <span className="mt-1 text-[10px] font-bold">
                    {item.progress}%
                  </span>
                  <div className="absolute bottom-0 inset-x-0 h-1 bg-white/25">
                    <div
                      className="h-full bg-[#0045cc] transition-all duration-150"
                      style={{ width: `${item.progress}%` }}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeUpload(item.id)}
                    title="Hủy tải lên"
                    className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-slate-900/70 hover:bg-rose-600 text-white flex items-center justify-center"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              )}

              {isError && (
                <div className="absolute inset-0 bg-rose-950/55 p-2 flex flex-col items-center justify-center gap-1.5 text-center text-white">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span className="text-[9px] leading-tight font-medium line-clamp-3">
                    {item.errorMessage || "Upload failed"}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => retryUpload(item.id)}
                      disabled={disabled}
                      title="Thử lại"
                      className="w-6 h-6 rounded-md bg-white/90 text-amber-600 hover:bg-white flex items-center justify-center disabled:opacity-50"
                    >
                      <RefreshCw className="w-3 h-3" />
                    </button>
                    <button
                      type="button"
                      onClick={() => removeUpload(item.id)}
                      disabled={disabled}
                      title="Xóa"
                      className="w-6 h-6 rounded-md bg-white/90 text-rose-600 hover:bg-white flex items-center justify-center disabled:opacity-50"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {!isFull && (
          <button
            type="button"
            onClick={openPicker}
            disabled={disabled}
            className="aspect-square rounded-2xl border-2 border-dashed border-[#0045cc]/40 bg-blue-50/30 hover:bg-blue-50/70 hover:border-[#0045cc] flex flex-col items-center justify-center p-3 text-center transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span className="w-8 h-8 rounded-full bg-[#0045cc]/10 text-[#0045cc] flex items-center justify-center mb-1.5">
              <ImagePlus className="w-4 h-4" />
            </span>
            <span className="text-[11px] font-bold text-slate-700">Thêm ảnh</span>
            <span className="text-[9px] text-slate-400 mt-0.5">
              {slots.length}/{maxFiles}
            </span>
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        onChange={handleInputChange}
        className="hidden"
        disabled={disabled || isFull}
      />

      {previewUrl && (
        <div
          onClick={() => setPreviewUrl(null)}
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-6 cursor-pointer"
        >
          <div className="max-w-2xl max-h-[85vh] rounded-2xl overflow-hidden shadow-2xl border border-white/20 relative">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={previewUrl}
              alt="Preview"
              className="w-full h-full object-contain max-h-[80vh]"
            />
          </div>
        </div>
      )}
    </div>
  );
}
