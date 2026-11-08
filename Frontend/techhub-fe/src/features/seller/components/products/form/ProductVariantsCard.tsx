"use client";

import React, { useState } from "react";
import type { CreateProductVariantRequest } from "@/types/sellerProduct";
import {
  Layers,
  Plus,
  Trash2,
  SlidersHorizontal,
  Check,
  AlertCircle,
  Package,
} from "lucide-react";

interface ProductVariantsCardProps {
  variants: CreateProductVariantRequest[];
  onChange: (variants: CreateProductVariantRequest[]) => void;
  productName: string;
  brandName: string;
  errors: Record<string, string>;
}

export function ProductVariantsCard({
  variants,
  onChange,
  productName,
  brandName,
  errors,
}: ProductVariantsCardProps) {
  const [isMultiVariant, setIsMultiVariant] = useState(variants.length > 1);

  // Bulk Apply inputs
  const [bulkPrice, setBulkPrice] = useState("");
  const [bulkStock, setBulkStock] = useState("");
  const [showBulkSuccess, setShowBulkSuccess] = useState(false);

  // Generate a recommended clean SKU
  const generateCleanSku = (suffix = "") => {
    const brandPrefix = (brandName || "TH")
      .replace(/[^a-zA-Z0-9]/g, "")
      .toUpperCase()
      .slice(0, 4);
    const namePart = (productName || "PROD")
      .replace(/[^a-zA-Z0-9]/g, "")
      .toUpperCase()
      .slice(0, 6);
    const random = Math.floor(100 + Math.random() * 900);
    return `${brandPrefix}-${namePart}-${random}${suffix ? `-${suffix.toUpperCase()}` : ""}`;
  };

  const handleToggleMultiVariant = (enableMulti: boolean) => {
    setIsMultiVariant(enableMulti);
    if (!enableMulti) {
      // Keep only first variant or initialize a default single variant
      const first = variants[0] || {
        sku: generateCleanSku(),
        variantName: "Bản Tiêu Chuẩn",
        price: 100000,
        stockQuantity: 10,
      };
      onChange([first]);
    } else {
      if (variants.length === 1) {
        // Add a second variant
        onChange([
          variants[0],
          {
            sku: generateCleanSku("V2"),
            variantName: "Phân loại 2",
            price: variants[0].price || 100000,
            stockQuantity: variants[0].stockQuantity || 10,
          },
        ]);
      }
    }
  };

  const handleAddVariant = () => {
    if (variants.length >= 50) return;
    const newIdx = variants.length + 1;
    const newVariant: CreateProductVariantRequest = {
      sku: generateCleanSku(`V${newIdx}`),
      variantName: `Phân loại ${newIdx}`,
      price: variants[0]?.price || 100000,
      stockQuantity: variants[0]?.stockQuantity || 10,
    };
    onChange([...variants, newVariant]);
  };

  const handleRemoveVariant = (idxToRemove: number) => {
    if (variants.length <= 1) return;
    const filtered = variants.filter((_, idx) => idx !== idxToRemove);
    onChange(filtered);
    if (filtered.length === 1) {
      setIsMultiVariant(false);
    }
  };

  const handleUpdateVariant = (
    idx: number,
    field: keyof CreateProductVariantRequest,
    val: unknown
  ) => {
    const updated = variants.map((v, i) => {
      if (i === idx) {
        return { ...v, [field]: val };
      }
      return v;
    });
    onChange(updated);
  };

  const handleBulkApply = () => {
    if (!bulkPrice && !bulkStock) return;

    const priceNum = bulkPrice ? Number(bulkPrice) : null;
    const stockNum = bulkStock ? Number(bulkStock) : null;

    const updated = variants.map((v) => ({
      ...v,
      ...(priceNum !== null && priceNum > 0 ? { price: priceNum } : {}),
      ...(stockNum !== null && stockNum >= 0 ? { stockQuantity: stockNum } : {}),
    }));

    onChange(updated);
    setShowBulkSuccess(true);
    setTimeout(() => setShowBulkSuccess(false), 2000);
  };

  const formatVND = (val: number) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(val || 0);

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 bg-slate-50/70 border-b border-slate-200/80 flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center font-bold text-sm">
            4
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-headline font-bold text-sm text-slate-900">
                Phân loại sản phẩm & Bảng giá bán
              </h2>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-[#0045cc]">
                {variants.length} Phân loại (Tối đa 50)
              </span>
            </div>
            <p className="text-[11px] text-slate-500">
              Thiết lập mã phân loại (SKU), mức giá niêm yết và số lượng hàng sẵn sàng giao trong kho
            </p>
          </div>
        </div>

        {/* Toggle Mode */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl">
          <button
            type="button"
            onClick={() => handleToggleMultiVariant(false)}
            className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
              !isMultiVariant
                ? "bg-white text-slate-900 shadow-2xs"
                : "text-slate-500 hover:text-slate-900"
            }`}
          >
            Một phiên bản duy nhất
          </button>
          <button
            type="button"
            onClick={() => handleToggleMultiVariant(true)}
            className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
              isMultiVariant
                ? "bg-[#0045cc] text-white shadow-2xs"
                : "text-slate-500 hover:text-slate-900"
            }`}
          >
            Nhiều phiên bản / cấu hình
          </button>
        </div>
      </div>

      {/* Body */}
      <div className="p-6 flex flex-col gap-5">
        {/* Bulk Apply Bar when multi-variant */}
        {isMultiVariant && variants.length > 1 && (
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between flex-wrap gap-3">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
              <SlidersHorizontal className="w-4 h-4 text-[#0045cc]" />
              <span>Áp dụng nhanh cho tất cả phân loại:</span>
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <div className="relative">
                <input
                  type="number"
                  placeholder="Giá bán chung (đ)"
                  value={bulkPrice}
                  onChange={(e) => setBulkPrice(e.target.value)}
                  className="w-36 h-9 px-3 rounded-lg border border-slate-200 bg-white text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
              </div>

              <div className="relative">
                <input
                  type="number"
                  placeholder="Kho chung"
                  value={bulkStock}
                  onChange={(e) => setBulkStock(e.target.value)}
                  className="w-28 h-9 px-3 rounded-lg border border-slate-200 bg-white text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
              </div>

              <button
                type="button"
                onClick={handleBulkApply}
                disabled={!bulkPrice && !bulkStock}
                className="h-9 px-4 rounded-lg bg-[#0045cc] hover:bg-[#0038a8] text-white text-xs font-semibold transition-colors disabled:opacity-50 flex items-center gap-1"
              >
                {showBulkSuccess ? <Check className="w-3.5 h-3.5" /> : null}
                <span>{showBulkSuccess ? "Đã áp dụng!" : "Áp dụng tất cả"}</span>
              </button>
            </div>
          </div>
        )}

        {/* Variants Table */}
        <div className="border border-slate-200 rounded-xl overflow-hidden">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold select-none">
                <th className="py-2.5 px-3 w-[26%]">Tên phân loại *</th>
                <th className="py-2.5 px-3 w-[24%]">Mã phân loại (SKU) *</th>
                <th className="py-2.5 px-3 w-[22%] text-right">Giá bán (VND) *</th>
                <th className="py-2.5 px-3 w-[16%] text-center">Tồn kho *</th>
                {isMultiVariant && (
                  <th className="py-2.5 px-2 w-[12%] text-center">Xóa</th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {variants.map((variant, idx) => {
                const skuError = errors[`variant_sku_${idx}`];
                const nameError = errors[`variant_name_${idx}`];
                const priceError = errors[`variant_price_${idx}`];
                const stockError = errors[`variant_stock_${idx}`];

                return (
                  <tr key={idx} className="hover:bg-slate-50/50 transition-colors">
                    {/* 1. Variant Name */}
                    <td className="py-3 px-3">
                      <input
                        type="text"
                        value={variant.variantName}
                        onChange={(e) =>
                          handleUpdateVariant(idx, "variantName", e.target.value)
                        }
                        placeholder="VD: Tiêu chuẩn, Màu Đen..."
                        className={`w-full h-9 px-3 rounded-lg border text-xs text-slate-900 focus:outline-none focus:border-[#0045cc] ${
                          nameError ? "border-rose-300 bg-rose-50/40" : "border-slate-200"
                        }`}
                      />
                      {nameError && (
                        <p className="text-[10px] text-rose-600 mt-1">{nameError}</p>
                      )}
                    </td>

                    {/* 2. SKU */}
                    <td className="py-3 px-3">
                      <input
                        type="text"
                        value={variant.sku}
                        onChange={(e) =>
                          handleUpdateVariant(idx, "sku", e.target.value.toUpperCase())
                        }
                        placeholder="VD: ROG-SWIFT-27-01"
                        className={`w-full h-9 px-3 rounded-lg border text-xs font-mono uppercase text-slate-900 focus:outline-none focus:border-[#0045cc] ${
                          skuError ? "border-rose-300 bg-rose-50/40" : "border-slate-200"
                        }`}
                      />
                      {skuError && (
                        <p className="text-[10px] text-rose-600 mt-1">{skuError}</p>
                      )}
                    </td>

                    {/* 3. Price (VND) */}
                    <td className="py-3 px-3 text-right">
                      <div className="flex flex-col items-end">
                        <input
                          type="number"
                          min={1000}
                          max={1000000000}
                          step={1000}
                          value={variant.price || ""}
                          onChange={(e) =>
                            handleUpdateVariant(
                              idx,
                              "price",
                              e.target.value === "" ? 0 : Number(e.target.value)
                            )
                          }
                          placeholder="Nhập giá bán..."
                          className={`w-full h-9 px-3 text-right rounded-lg border text-xs font-bold text-slate-900 focus:outline-none focus:border-[#0045cc] ${
                            priceError ? "border-rose-300 bg-rose-50/40" : "border-slate-200"
                          }`}
                        />
                        <span className="text-[10px] text-slate-400 mt-0.5">
                          {formatVND(variant.price)}
                        </span>
                        {priceError && (
                          <p className="text-[10px] text-rose-600 mt-0.5">{priceError}</p>
                        )}
                      </div>
                    </td>

                    {/* 4. Stock Quantity */}
                    <td className="py-3 px-3 text-center">
                      <input
                        type="number"
                        min={0}
                        max={1000000}
                        value={variant.stockQuantity !== undefined ? variant.stockQuantity : ""}
                        onChange={(e) =>
                          handleUpdateVariant(
                            idx,
                            "stockQuantity",
                            e.target.value === "" ? 0 : Number(e.target.value)
                          )
                        }
                        placeholder="Số lượng"
                        className={`w-20 mx-auto h-9 px-2 text-center rounded-lg border text-xs font-semibold text-slate-900 focus:outline-none focus:border-[#0045cc] ${
                          stockError ? "border-rose-300 bg-rose-50/40" : "border-slate-200"
                        }`}
                      />
                      {stockError && (
                        <p className="text-[10px] text-rose-600 mt-1">{stockError}</p>
                      )}
                    </td>

                    {/* 5. Delete Action (Multi-variant only) */}
                    {isMultiVariant && (
                      <td className="py-3 px-2 text-center">
                        <button
                          type="button"
                          disabled={variants.length <= 1}
                          onClick={() => handleRemoveVariant(idx)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors disabled:opacity-30 disabled:hover:bg-transparent"
                          title="Xóa phân loại"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Add Variant Button */}
        {isMultiVariant && (
          <div className="flex items-center justify-between pt-1">
            <button
              type="button"
              disabled={variants.length >= 50}
              onClick={handleAddVariant}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-dashed border-[#0045cc] text-[#0045cc] hover:bg-blue-50 text-xs font-semibold transition-all disabled:opacity-50"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Thêm phiên bản cấu hình mới ({variants.length}/50)</span>
            </button>

            <span className="text-[11px] text-slate-400">
              Tổng tồn kho:{" "}
              <strong className="text-slate-900">
                {variants.reduce((acc, curr) => acc + (Number(curr.stockQuantity) || 0), 0)}
              </strong>{" "}
              linh kiện
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
