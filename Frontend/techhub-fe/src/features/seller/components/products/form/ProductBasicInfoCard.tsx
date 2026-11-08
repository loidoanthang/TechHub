"use client";

import React from "react";
import type { CategorySummaryResponse } from "@/types/category";
import { FolderTree, Sparkles, Tag, FileText } from "lucide-react";

interface ProductBasicInfoCardProps {
  name: string;
  onNameChange: (val: string) => void;
  categoryId: string;
  onCategoryChange: (val: string) => void;
  brandName: string;
  onBrandNameChange: (val: string) => void;
  description: string;
  onDescriptionChange: (val: string) => void;
  categories: CategorySummaryResponse[];
  errors: Record<string, string>;
}

const COMMON_TECH_BRANDS = [
  "ASUS ROG",
  "Logitech",
  "Corsair",
  "Intel",
  "AMD",
  "Kingston",
  "Samsung",
  "LG UltraGear",
  "Razer",
  "MSI",
  "Gigabyte",
  "NVIDIA",
  "Keychron",
  "Akko",
  "HyperX",
];

export function ProductBasicInfoCard({
  name,
  onNameChange,
  categoryId,
  onCategoryChange,
  brandName,
  onBrandNameChange,
  description,
  onDescriptionChange,
  categories,
  errors,
}: ProductBasicInfoCardProps) {
  const insertSampleDescription = () => {
    const sample = `Sản phẩm linh kiện phần cứng chính hãng, đáp ứng tiêu chuẩn chất lượng cao và tương thích tối đa cho hệ thống máy tính, gaming gear và văn phòng hiện đại.

Đặc điểm kỹ thuật nổi bật:
• Hiệu năng vượt trội: Tối ưu hoá vi kiến trúc, độ trễ cực thấp và độ bền linh kiện chuẩn công nghiệp.
• Tản nhiệt & Hoàn thiện cao cấp: Thiết kế tản nhiệt tối ưu, chống bám bụi và hoàn thiện bề mặt sang trọng.
• Khả năng tương thích: Hỗ trợ đa nền tảng phần cứng, plug & play dễ dàng thiết lập.
• Quy cách đóng gói: Đầy đủ phụ kiện cáp kết nối, sách hướng dẫn sử dụng và tem bảo hành chính hãng.`;
    onDescriptionChange(sample);
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center font-bold text-sm">
            1
          </div>
          <div>
            <h2 className="font-headline font-bold text-sm text-slate-900">
              Thông tin cơ bản sản phẩm
            </h2>
            <p className="text-[11px] text-slate-500">
              Các thông tin định danh sản phẩm được người mua tìm kiếm nhiều nhất trên TechHub
            </p>
          </div>
        </div>

        <span className="text-[11px] text-slate-400 font-medium">
          Bước 1 / 4
        </span>
      </div>

      {/* Body Fields */}
      <div className="p-6 flex flex-col gap-5">
        {/* 1. Product Name */}
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-headline font-bold text-slate-900 flex items-center gap-1">
              Tên sản phẩm & Model <span className="text-rose-500">*</span>
            </label>
            <span
              className={`text-[11px] font-medium ${
                name.length > 300
                  ? "text-rose-600 font-bold"
                  : name.length < 5 && name.length > 0
                  ? "text-amber-600"
                  : "text-slate-400"
              }`}
            >
              {name.length} / 300 ký tự
            </span>
          </div>

          <div className="relative">
            <input
              type="text"
              value={name}
              onChange={(e) => onNameChange(e.target.value)}
              placeholder="VD: ASUS ROG Swift OLED PG27AQDM 27-inch 240Hz 0.03ms Gaming Monitor..."
              className={`w-full h-10 px-3.5 rounded-xl bg-slate-50/60 border text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all ${
                errors.name ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
              }`}
            />
          </div>

          {errors.name ? (
            <p className="text-[11px] text-rose-600 font-medium">{errors.name}</p>
          ) : (
            <p className="text-[11px] text-slate-400 leading-normal flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-[#0045cc] shrink-0" />
              Gợi ý tên chuẩn SEO:{" "}
              <strong className="text-slate-600">
                [Thương hiệu] + [Dòng sản phẩm] + [Model] + [Thông số cốt lõi]
              </strong>
            </p>
          )}
        </div>

        {/* 2. Category Selector & Brand in 2 Columns */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Category */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-headline font-bold text-slate-900 flex items-center gap-1">
              Ngành hàng / Danh mục linh kiện <span className="text-rose-500">*</span>
            </label>

            <div className="relative">
              <select
                value={categoryId}
                onChange={(e) => onCategoryChange(e.target.value)}
                className={`w-full h-10 pl-3.5 pr-8 rounded-xl bg-slate-50/60 border text-xs text-slate-900 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] appearance-none cursor-pointer transition-all ${
                  errors.categoryId ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
                }`}
              >
                <option value="">-- Chọn danh mục linh kiện thiết bị --</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              <FolderTree className="w-4 h-4 text-slate-400 absolute right-3 top-3 pointer-events-none" />
            </div>

            {errors.categoryId ? (
              <p className="text-[11px] text-rose-600 font-medium">{errors.categoryId}</p>
            ) : (
              <p className="text-[11px] text-slate-400">
                Chọn danh mục sẽ tự động mở các thuộc tính kỹ thuật động ở Bước 3.
              </p>
            )}
          </div>

          {/* Brand */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-headline font-bold text-slate-900 flex items-center gap-1">
              Thương hiệu hãng sản xuất <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={brandName}
              onChange={(e) => onBrandNameChange(e.target.value)}
              placeholder="VD: ASUS, Logitech, Corsair, Intel..."
              list="brand-suggestions"
              className={`w-full h-10 px-3.5 rounded-xl bg-slate-50/60 border text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all ${
                errors.brandName ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
              }`}
            />
            <datalist id="brand-suggestions">
              {COMMON_TECH_BRANDS.map((b) => (
                <option key={b} value={b} />
              ))}
            </datalist>
            {errors.brandName ? (
              <p className="text-[11px] text-rose-600 font-medium">{errors.brandName}</p>
            ) : (
              <p className="text-[11px] text-slate-400">
                Hãng sản xuất linh kiện phần cứng hoặc thiết bị ngoại vi
              </p>
            )}
          </div>
        </div>

        {/* Quick Brand Badges */}
        <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
          <span className="text-[11px] text-slate-400 flex items-center gap-1 mr-1">
            <Tag className="w-3 h-3" /> Chọn nhanh hãng:
          </span>
          {COMMON_TECH_BRANDS.slice(0, 10).map((b) => (
            <button
              key={b}
              type="button"
              onClick={() => onBrandNameChange(b)}
              className={`px-2 py-0.5 rounded-md text-[10px] font-semibold transition-all ${
                brandName === b
                  ? "bg-[#0045cc] text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {b}
            </button>
          ))}
        </div>

        {/* 3. Rich Description */}
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-headline font-bold text-slate-900 flex items-center gap-1">
              Mô tả chi tiết sản phẩm
            </label>
            <button
              type="button"
              onClick={insertSampleDescription}
              className="text-[11px] font-semibold text-[#0045cc] hover:underline flex items-center gap-1"
            >
              <FileText className="w-3 h-3" />
              Điền mẫu mô tả chuẩn sàn
            </button>
          </div>

          <textarea
            rows={6}
            value={description}
            onChange={(e) => onDescriptionChange(e.target.value)}
            placeholder="Nêu rõ các ưu điểm nổi bật, thông số kỹ thuật, cổng kết nối, phụ kiện đi kèm trong hộp..."
            className="w-full p-3.5 rounded-xl bg-slate-50/60 border border-slate-200 text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] resize-y leading-relaxed transition-all"
          />
        </div>
      </div>
    </div>
  );
}
