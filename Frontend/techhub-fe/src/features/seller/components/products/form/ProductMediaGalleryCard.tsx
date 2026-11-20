"use client";

import React from "react";
import type { CreateProductImageRequest } from "@/types/sellerProduct";
import { HelpCircle } from "lucide-react";
import { GalleryImageUploader } from "@/components/shared/upload/GalleryImageUploader";
import { uploadApi } from "@/lib/api/upload";
import { UPLOAD_LIMITS } from "@/features/uploads/types";

interface ProductMediaGalleryCardProps {
  images: CreateProductImageRequest[];
  onChange: (images: CreateProductImageRequest[]) => void;
  error?: string;
  onUploadingChange?: (isUploading: boolean) => void;
}

export function ProductMediaGalleryCard({
  images,
  onChange,
  error,
  onUploadingChange,
}: ProductMediaGalleryCardProps) {
  const urlList = images.map((img) => img.imageUrl);

  const handleGalleryChange = (urls: string[]) => {
    const updated: CreateProductImageRequest[] = urls.map((url, idx) => ({
      imageUrl: url,
      displayOrder: idx,
    }));
    onChange(updated);
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center font-bold text-sm">
            2
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-headline font-bold text-sm text-slate-900">
                Thư viện hình ảnh sản phẩm
              </h2>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-[#0045cc]">
                {urlList.length} / 6 Ảnh (Bắt buộc 1–6 ảnh)
              </span>
            </div>
            <p className="text-[11px] text-slate-500">
              Ảnh vuông 1:1, phông nền sáng rõ nét để thu hút người mua phần cứng
            </p>
          </div>
        </div>
      </div>

      {/* Helper Banner */}
      <div className="mx-6 mt-5 p-3 rounded-xl bg-blue-50/60 border border-blue-100 flex items-start gap-2.5 text-xs text-slate-600">
        <HelpCircle className="w-4 h-4 text-[#0045cc] shrink-0 mt-0.5" />
        <div className="flex-1 text-[11px] leading-relaxed">
          Ảnh ở vị trí đầu tiên (<strong>Slot 1</strong>) sẽ tự động làm{" "}
          <strong className="text-[#0045cc]">Ảnh Bìa chính (Cover Image)</strong> trên toàn bộ
          trang marketplace và giỏ hàng. Bạn có thể hoán đổi bất kỳ ảnh nào lên đầu bằng nút sao.
        </div>
      </div>

      {/* Gallery */}
      <div className="p-6">
        <GalleryImageUploader
          value={urlList}
          onChange={handleGalleryChange}
          maxFiles={6}
          maxSize={UPLOAD_LIMITS.PRODUCT_IMAGE}
          presignFn={uploadApi.getProductImagePresignedUrl}
          onUploadingChange={onUploadingChange}
          isCoverSelectorEnabled
        />

        {error && (
          <p className="text-xs text-rose-600 font-medium mt-3">{error}</p>
        )}
      </div>
    </div>
  );
}
