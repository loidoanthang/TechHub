"use client";

import React, { useState } from "react";
import type { CreateProductImageRequest } from "@/types/sellerProduct";
import {
  ImagePlus,
  Trash2,
  Star,
  Link as LinkIcon,
  HelpCircle,
  Eye,
  Check,
  Sparkles,
} from "lucide-react";

interface ProductMediaGalleryCardProps {
  images: CreateProductImageRequest[];
  onChange: (images: CreateProductImageRequest[]) => void;
  error?: string;
}

// Preset library of real high-resolution hardware images for quick sample selection
const HARDWARE_PRESET_GALLERY = [
  {
    name: "Màn hình OLED ASUS ROG",
    url: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&auto=format&fit=crop&q=80",
    tag: "Monitor",
  },
  {
    name: "Chuột Gaming Logitech G Pro",
    url: "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800&auto=format&fit=crop&q=80",
    tag: "Mouse",
  },
  {
    name: "Bàn phím cơ Custom Switch",
    url: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&auto=format&fit=crop&q=80",
    tag: "Keyboard",
  },
  {
    name: "Card đồ họa Gaming GPU",
    url: "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800&auto=format&fit=crop&q=80",
    tag: "GPU",
  },
  {
    name: "Tai nghe Gaming HyperX Cloud",
    url: "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80",
    tag: "Audio",
  },
  {
    name: "Linh kiện Mainboard & RAM",
    url: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop&q=80",
    tag: "PC Specs",
  },
];

export function ProductMediaGalleryCard({
  images,
  onChange,
  error,
}: ProductMediaGalleryCardProps) {
  const [urlInput, setUrlInput] = useState("");
  const [showUrlModal, setShowUrlModal] = useState(false);
  const [showPresetModal, setShowPresetModal] = useState(false);
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  const handleAddImage = (newUrl: string) => {
    const trimmed = newUrl.trim();
    if (!trimmed || images.length >= 6) return;

    // Validate that it's a valid http/https URL
    if (!/^https?:\/\/.+/i.test(trimmed)) return;

    // Check if duplicate url
    if (images.some((img) => img.imageUrl === trimmed)) return;

    const newDisplayOrder = images.length;
    const updated = [
      ...images,
      { imageUrl: trimmed, displayOrder: newDisplayOrder },
    ];
    onChange(updated);
    setUrlInput("");
    setShowUrlModal(false);
  };

  const handleRemoveImage = (indexToRemove: number) => {
    const filtered = images.filter((_, idx) => idx !== indexToRemove);
    // Re-index displayOrders from 0 to N-1 (ensuring Slot 0 is always Cover Image)
    const reindexed = filtered.map((img, idx) => ({
      ...img,
      displayOrder: idx,
    }));
    onChange(reindexed);
  };

  const handleSetAsCover = (indexToPromote: number) => {
    if (indexToPromote === 0) return;
    const item = images[indexToPromote];
    const remaining = images.filter((_, idx) => idx !== indexToPromote);
    const updated = [item, ...remaining].map((img, idx) => ({
      ...img,
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
                {images.length} / 6 Ảnh (Bắt buộc 1–6 ảnh)
              </span>
            </div>
            <p className="text-[11px] text-slate-500">
              Ảnh vuông 1:1, phông nền sáng rõ nét để thu hút người mua phần cứng
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowPresetModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-50 text-[#0045cc] hover:bg-blue-100 text-xs font-semibold transition-colors"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Thư viện mẫu TechHub</span>
          </button>
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

      {/* 6-Slot Grid */}
      <div className="p-6">
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3.5">
          {[0, 1, 2, 3, 4, 5].map((slotIdx) => {
            const currentImg = images[slotIdx];

            if (currentImg) {
              const isCover = slotIdx === 0;
              return (
                <div
                  key={currentImg.imageUrl}
                  className="group relative aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-slate-50 shadow-2xs"
                >
                  <img
                    src={currentImg.imageUrl}
                    alt={`Ảnh ${slotIdx + 1}`}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                  />

                  {/* Cover Badge */}
                  {isCover && (
                    <div className="absolute top-2 left-2 z-10 px-2 py-0.5 rounded-lg bg-[#0045cc] text-white text-[10px] font-bold shadow-sm">
                      Ảnh bìa
                    </div>
                  )}

                  {/* Actions Overlay on Hover */}
                  <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 p-2">
                    {!isCover && (
                      <button
                        type="button"
                        onClick={() => handleSetAsCover(slotIdx)}
                        className="w-7 h-7 rounded-lg bg-white/90 text-amber-500 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow"
                        title="Đặt làm Ảnh bìa chính"
                      >
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" />
                      </button>
                    )}

                    <button
                      type="button"
                      onClick={() => setPreviewImage(currentImg.imageUrl)}
                      className="w-7 h-7 rounded-lg bg-white/90 text-slate-700 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow"
                      title="Xem ảnh lớn"
                    >
                      <Eye className="w-3.5 h-3.5" />
                    </button>

                    <button
                      type="button"
                      onClick={() => handleRemoveImage(slotIdx)}
                      className="w-7 h-7 rounded-lg bg-white/90 text-rose-600 hover:bg-white flex items-center justify-center transition-transform hover:scale-110 shadow"
                      title="Xóa ảnh"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            }

            // Empty Slot (Add Image Button)
            return (
              <div
                key={`empty-slot-${slotIdx}`}
                onClick={() => setShowUrlModal(true)}
                className={`aspect-square rounded-2xl border-2 border-dashed flex flex-col items-center justify-center p-3 text-center cursor-pointer transition-all ${
                  slotIdx === images.length
                    ? "border-[#0045cc]/50 bg-blue-50/30 hover:bg-blue-50/70 hover:border-[#0045cc]"
                    : "border-slate-200 bg-slate-50/40 opacity-60 hover:opacity-100"
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center mb-1.5 ${
                    slotIdx === images.length
                      ? "bg-[#0045cc]/10 text-[#0045cc]"
                      : "bg-slate-200 text-slate-400"
                  }`}
                >
                  <ImagePlus className="w-4 h-4" />
                </div>
                <span className="text-[11px] font-bold text-slate-700">
                  {slotIdx === 0 ? "Ảnh bìa *" : `Slot ${slotIdx + 1}`}
                </span>
                <span className="text-[9px] text-slate-400 mt-0.5">
                  {slotIdx === images.length ? "+ Thêm ảnh" : "Chưa có"}
                </span>
              </div>
            );
          })}
        </div>

        {error && <p className="text-xs text-rose-600 font-medium mt-3">{error}</p>}
      </div>

      {/* Modal: Input Direct URL */}
      {showUrlModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-200 animate-in fade-in-0 zoom-in-95">
            <div className="flex items-center gap-2 mb-3">
              <LinkIcon className="w-4 h-4 text-[#0045cc]" />
              <h3 className="font-headline font-bold text-sm text-slate-900">
                Thêm hình ảnh qua đường link URL
              </h3>
            </div>
            <p className="text-xs text-slate-500 mb-4 leading-relaxed">
              Dán đường dẫn ảnh trực tiếp (JPG, PNG, WebP) từ hosting, Cloudinary, Imgur hoặc
              Unsplash.
            </p>

            <input
              type="url"
              value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)}
              placeholder="https://example.com/images/product-cover.jpg"
              className="w-full h-10 px-3.5 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] mb-4"
              autoFocus
            />

            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setUrlInput("");
                  setShowUrlModal(false);
                }}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={!urlInput.trim()}
                onClick={() => handleAddImage(urlInput)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-[#0045cc] hover:bg-[#0038a8] transition-colors disabled:opacity-50"
              >
                Thêm ảnh
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Preset Hardware Gallery */}
      {showPresetModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full shadow-2xl border border-slate-200 animate-in fade-in-0 zoom-in-95">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#0045cc]" />
                <h3 className="font-headline font-bold text-sm text-slate-900">
                  Thư viện ảnh mẫu linh kiện công nghệ TechHub
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowPresetModal(false)}
                className="text-slate-400 hover:text-slate-600 text-xs"
              >
                ✕
              </button>
            </div>
            <p className="text-xs text-slate-500 mb-4">
              Chọn nhanh các ảnh thực tế sắc nét để kiểm thử đăng bán ngay mà không cần tải ảnh ngoài:
            </p>

            <div className="grid grid-cols-3 gap-3 mb-5 max-h-[320px] overflow-y-auto pr-1">
              {HARDWARE_PRESET_GALLERY.map((preset) => {
                const isSelected = images.some((img) => img.imageUrl === preset.url);
                return (
                  <div
                    key={preset.url}
                    onClick={() => {
                      if (!isSelected && images.length < 6) {
                        handleAddImage(preset.url);
                      }
                    }}
                    className={`group relative aspect-square rounded-xl overflow-hidden border cursor-pointer transition-all ${
                      isSelected
                        ? "border-[#0045cc] ring-2 ring-[#0045cc]/30"
                        : "border-slate-200 hover:border-slate-400"
                    }`}
                  >
                    <img
                      src={preset.url}
                      alt={preset.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                    <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/80 to-transparent p-2 text-[10px] text-white font-medium truncate">
                      {preset.name}
                    </div>
                    {isSelected && (
                      <div className="absolute top-1.5 right-1.5 w-5 h-5 rounded-full bg-[#0045cc] text-white flex items-center justify-center shadow">
                        <Check className="w-3 h-3" />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="flex items-center justify-end">
              <button
                type="button"
                onClick={() => setShowPresetModal(false)}
                className="px-5 py-2 rounded-xl text-xs font-semibold text-white bg-[#0045cc] hover:bg-[#0038a8] transition-colors"
              >
                Xong
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Big Image Preview */}
      {previewImage && (
        <div
          onClick={() => setPreviewImage(null)}
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-6 cursor-pointer"
        >
          <div className="max-w-2xl max-h-[85vh] rounded-2xl overflow-hidden shadow-2xl border border-white/20 relative">
            <img
              src={previewImage}
              alt="Preview"
              className="w-full h-full object-contain max-h-[80vh]"
            />
          </div>
        </div>
      )}
    </div>
  );
}
