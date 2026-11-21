"use client";

import React from "react";
import { Edit, Trash2, ListChecks, FolderTree } from "lucide-react";
import type { CategorySummaryResponse } from "@/types/adminCatalog";

interface AdminCategoryMobileCardProps {
  category: CategorySummaryResponse;
  onEdit: (cat: CategorySummaryResponse) => void;
  onDelete: (cat: CategorySummaryResponse) => void;
  onToggleStatus: (cat: CategorySummaryResponse) => void;
  onManageAttributes: (categoryId: string) => void;
  isStatusPending?: boolean;
  isDeletePending?: boolean;
}

export function AdminCategoryMobileCard({
  category,
  onEdit,
  onDelete,
  onToggleStatus,
  onManageAttributes,
  isStatusPending = false,
  isDeletePending = false,
}: AdminCategoryMobileCardProps) {
  const isActive = category.status === "ACTIVE";

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Name & Status */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center shrink-0">
            <FolderTree className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <h4 className="font-bold text-xs text-slate-900 truncate">
              {category.name}
            </h4>
          </div>
        </div>

        <button
          type="button"
          onClick={() => onToggleStatus(category)}
          disabled={isStatusPending}
          className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold cursor-pointer border transition-colors shrink-0 ${
            isActive
              ? "bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100"
              : "bg-slate-100 text-slate-600 border-slate-200 hover:bg-slate-200"
          }`}
          title="Bấm để chuyển đổi trạng thái"
        >
          {category.status}
        </button>
      </div>

      {/* 2. Description (if exists) */}
      {category.description && (
        <p className="text-xs text-slate-600 line-clamp-2 pl-10">
          {category.description}
        </p>
      )}

      {/* 3. Attributes Counter & Manage CTA */}
      <div className="bg-slate-50 p-2.5 rounded-xl flex items-center justify-between text-xs">
        <div className="flex items-center gap-1.5 text-slate-700">
          <ListChecks className="w-4 h-4 text-[#0045cc]" />
          <span className="font-semibold text-slate-900">
            {category.attributesCount} thuộc tính mẫu
          </span>
        </div>

        <button
          type="button"
          onClick={() => onManageAttributes(category.id)}
          className="px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-[#0045cc] hover:bg-[#0045cc] hover:text-white text-[11px] font-bold inline-flex items-center gap-1 transition-colors cursor-pointer shadow-2xs"
        >
          <span>Quản lý mẫu</span>
        </button>
      </div>

      {/* 4. Action bar */}
      <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
        <span className="text-[11px] text-slate-500">
          Trạng thái: <strong className={isActive ? "text-emerald-600" : "text-slate-500"}>{isActive ? "Hoạt động" : "Ngừng hoạt động"}</strong>
        </span>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => onEdit(category)}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:text-primary hover:bg-slate-50 transition-colors"
            title="Chỉnh sửa danh mục"
          >
            <Edit className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            onClick={() => onDelete(category)}
            disabled={isDeletePending}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:text-rose-600 hover:bg-rose-50 transition-colors"
            title="Xóa danh mục"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
