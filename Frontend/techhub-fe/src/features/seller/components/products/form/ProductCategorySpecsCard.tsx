"use client";

import React from "react";
import type { CategoryAttributeResponse } from "@/types/sellerProduct";
import { Cpu, Loader2, AlertCircle, CheckCircle2 } from "lucide-react";

interface ProductCategorySpecsCardProps {
  categoryId: string;
  categoryName?: string;
  attributes: CategoryAttributeResponse[];
  isLoading: boolean;
  values: Record<string, unknown>;
  onChange: (key: string, value: unknown) => void;
  errors: Record<string, string>;
}

export function ProductCategorySpecsCard({
  categoryId,
  categoryName,
  attributes,
  isLoading,
  values,
  onChange,
  errors,
}: ProductCategorySpecsCardProps) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center font-bold text-sm">
            3
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-headline font-bold text-sm text-slate-900">
                Thông số kỹ thuật chi tiết
              </h2>
              {categoryName && (
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-[#0045cc]">
                  Ngành hàng: {categoryName}
                </span>
              )}
              {attributes.length > 0 && (
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700">
                  {attributes.length} Tiêu chí kỹ thuật
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-500">
              Điền thông số chi tiết giúp người mua dễ dàng tìm kiếm, so sánh và tăng độ tin cậy cho sản phẩm
            </p>
          </div>
        </div>

        <span className="text-[11px] text-slate-400 font-medium">
          Bước 3 / 4
        </span>
      </div>

      {/* Body */}
      <div className="p-6">
        {!categoryId ? (
          <div className="py-10 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mb-3">
              <Cpu className="w-6 h-6" />
            </div>
            <h4 className="font-headline font-bold text-xs text-slate-700">
              Chưa chọn danh mục sản phẩm
            </h4>
            <p className="text-[11px] text-slate-400 max-w-sm mt-1">
              Vui lòng chọn <strong>Ngành hàng / Danh mục</strong> tại Bước 1 để hệ thống tự động
              tải các trường thông số kỹ thuật chuẩn (Socket, Chipset, Bus RAM, Chuẩn bàn phím...).
            </p>
          </div>
        ) : isLoading ? (
          <div className="py-10 flex flex-col items-center justify-center text-center">
            <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin mb-2" />
            <p className="text-xs text-slate-500 font-medium">
              Đang tải danh sách thuộc tính chuẩn của ngành hàng...
            </p>
          </div>
        ) : attributes.length === 0 ? (
          <div className="py-8 px-4 rounded-xl bg-slate-50 border border-slate-200/80 text-center">
            <p className="text-xs text-slate-600 font-medium">
              Danh mục này hiện chưa có thuộc tính kỹ thuật đặc thù bắt buộc.
            </p>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Bạn có thể tiếp tục sang Bước 4 để thiết lập bảng giá & phân loại SKU.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {attributes.map((attr) => {
              const currentValue = values[attr.name];
              const fieldError = errors[`attr_${attr.name}`];

              return (
                <div key={attr.id} className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-headline font-bold text-slate-900 flex items-center gap-1">
                      {attr.name} <span className="text-rose-500">*</span>
                    </label>
                    <span className="text-[10px] text-slate-400 uppercase font-mono">
                      {attr.dataType}
                    </span>
                  </div>

                  {/* Render based on CategoryAttributeDataType */}
                  {attr.dataType === "SELECT" && attr.options && attr.options.length > 0 ? (
                    <select
                      value={(currentValue as string) || ""}
                      onChange={(e) => onChange(attr.name, e.target.value)}
                      className={`w-full h-10 px-3 rounded-xl bg-slate-50/60 border text-xs text-slate-900 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all cursor-pointer ${
                        fieldError ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
                      }`}
                    >
                      <option value="">-- Chọn {attr.name} --</option>
                      {attr.options.map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                  ) : attr.dataType === "NUMBER" ? (
                    <input
                      type="number"
                      value={currentValue !== undefined && currentValue !== null ? String(currentValue) : ""}
                      onChange={(e) => {
                        const num = e.target.value === "" ? "" : Number(e.target.value);
                        onChange(attr.name, num);
                      }}
                      placeholder={attr.sampleValue || "Nhập số lượng / thông số..."}
                      className={`w-full h-10 px-3.5 rounded-xl bg-slate-50/60 border text-xs text-slate-900 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all ${
                        fieldError ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
                      }`}
                    />
                  ) : attr.dataType === "BOOLEAN" ? (
                    <div className="flex items-center gap-4 h-10 px-3 bg-slate-50/60 rounded-xl border border-slate-200">
                      <label className="inline-flex items-center gap-2 cursor-pointer text-xs text-slate-700">
                        <input
                          type="radio"
                          name={`radio_${attr.name}`}
                          checked={currentValue === true}
                          onChange={() => onChange(attr.name, true)}
                          className="w-3.5 h-3.5 text-[#0045cc] focus:ring-[#0045cc]"
                        />
                        <span>Có / Hỗ trợ</span>
                      </label>
                      <label className="inline-flex items-center gap-2 cursor-pointer text-xs text-slate-700">
                        <input
                          type="radio"
                          name={`radio_${attr.name}`}
                          checked={currentValue === false}
                          onChange={() => onChange(attr.name, false)}
                          className="w-3.5 h-3.5 text-[#0045cc] focus:ring-[#0045cc]"
                        />
                        <span>Không</span>
                      </label>
                    </div>
                  ) : (
                    /* Default TEXT */
                    <input
                      type="text"
                      value={(currentValue as string) || ""}
                      onChange={(e) => onChange(attr.name, e.target.value)}
                      placeholder={attr.sampleValue || `Nhập ${attr.name}...`}
                      className={`w-full h-10 px-3.5 rounded-xl bg-slate-50/60 border text-xs text-slate-900 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 focus:border-[#0045cc] transition-all ${
                        fieldError ? "border-rose-300 bg-rose-50/30" : "border-slate-200"
                      }`}
                    />
                  )}

                  {fieldError ? (
                    <p className="text-[11px] text-rose-600 font-medium">{fieldError}</p>
                  ) : attr.sampleValue ? (
                    <p className="text-[10px] text-slate-400">
                      Ví dụ mẫu: <span className="font-medium text-slate-600">{attr.sampleValue}</span>
                    </p>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
