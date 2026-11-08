"use client";

import React from "react";
import Link from "next/link";
import { ROUTES } from "@/lib/constants";
import { CheckCircle2, Package, PlusCircle, ArrowRight } from "lucide-react";

interface ProductSubmittedModalProps {
  isOpen: boolean;
  productName: string;
  onResetForm: () => void;
}

export function ProductSubmittedModal({
  isOpen,
  productName,
  onResetForm,
}: ProductSubmittedModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl p-8 max-w-md w-full text-center relative border border-slate-200 shadow-2xl flex flex-col items-center animate-in zoom-in-95 duration-200">
        {/* Large Iconic Badge */}
        <div className="w-16 h-16 rounded-full bg-blue-50 text-[#0045cc] flex items-center justify-center mb-5 ring-8 ring-blue-50/50">
          <CheckCircle2 className="w-9 h-9" />
        </div>

        {/* Title */}
        <h3 className="font-headline font-bold text-xl text-slate-900 tracking-tight mb-2">
          Gửi duyệt sản phẩm thành công!
        </h3>

        {/* Product Name Snippet */}
        {productName && (
          <div className="px-3 py-1.5 rounded-lg bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-700 truncate max-w-xs mb-3">
            {productName}
          </div>
        )}

        {/* Description */}
        <p className="text-xs text-slate-500 leading-relaxed mb-6">
          Sản phẩm của bạn đã được lưu và đưa vào danh sách kiểm duyệt. Đội ngũ kiểm duyệt sàn
          TechHub sẽ thẩm định tiêu chuẩn linh kiện và phản hồi trong vòng <strong>2–4 giờ</strong> (tối đa 24h).
        </p>

        {/* Actions */}
        <div className="flex flex-col gap-2.5 w-full">
          <Link
            href={ROUTES.SELLER.PRODUCTS}
            className="w-full py-2.5 px-4 rounded-xl bg-[#0045cc] hover:bg-[#0038a8] text-xs font-headline font-bold text-white transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-2"
          >
            <Package className="w-4 h-4" />
            <span>Xem Danh Sách Sản Phẩm</span>
          </Link>

          <button
            type="button"
            onClick={onResetForm}
            className="w-full py-2.5 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-headline font-bold text-slate-800 transition-colors flex items-center justify-center gap-2"
          >
            <PlusCircle className="w-4 h-4 text-slate-500" />
            <span>Đăng Tiếp Sản Phẩm Khác</span>
          </button>
        </div>
      </div>
    </div>
  );
}
