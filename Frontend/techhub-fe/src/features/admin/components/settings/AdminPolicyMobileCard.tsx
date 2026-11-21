"use client";

import React from "react";
import { Edit, Sliders } from "lucide-react";
import type { PlatformPolicyResponse } from "@/types/adminFinance";

interface AdminPolicyMobileCardProps {
  policy: PlatformPolicyResponse;
  onEdit: (policy: PlatformPolicyResponse) => void;
}

export function AdminPolicyMobileCard({
  policy,
  onEdit,
}: AdminPolicyMobileCardProps) {
  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Policy Key & Edit CTA */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <div className="w-7 h-7 rounded-lg bg-blue-50 text-[#0045cc] flex items-center justify-center shrink-0">
            <Sliders className="w-3.5 h-3.5" />
          </div>
          <span className="font-mono font-bold text-xs text-slate-900 truncate">
            {policy.policyKey}
          </span>
        </div>

        <button
          type="button"
          onClick={() => onEdit(policy)}
          className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-[#0045cc] hover:text-white text-slate-700 text-xs font-semibold inline-flex items-center gap-1 transition-colors shrink-0 cursor-pointer"
        >
          <Edit className="w-3 h-3" />
          <span>Sửa</span>
        </button>
      </div>

      {/* 2. Value Box */}
      <div className="bg-slate-50 p-2.5 rounded-xl flex items-center justify-between text-xs">
        <span className="text-slate-500 text-[11px] font-medium">Giá trị cấu hình:</span>
        <span className="px-2.5 py-0.5 rounded-lg bg-[#eff4ff] text-[#0045cc] font-mono font-extrabold border border-blue-200">
          {policy.policyValue}
        </span>
      </div>

      {/* 3. Description */}
      {policy.description && (
        <p className="text-xs text-slate-600 line-clamp-2 pt-0.5">
          {policy.description}
        </p>
      )}
    </div>
  );
}
