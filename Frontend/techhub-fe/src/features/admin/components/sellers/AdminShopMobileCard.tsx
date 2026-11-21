"use client";

import React from "react";
import { Eye, Store } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { AdminShopResponse } from "@/types/admin";

interface AdminShopMobileCardProps {
  shop: AdminShopResponse;
  onManage: (shopId: string) => void;
}

export function AdminShopMobileCard({
  shop,
  onManage,
}: AdminShopMobileCardProps) {
  const getShopBadge = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "PAUSED":
        return "bg-amber-100 text-amber-800 border-amber-200";
      case "SUSPENDED":
        return "bg-orange-100 text-orange-800 border-orange-200";
      default:
        return "bg-rose-100 text-rose-800 border-rose-200";
    }
  };

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Shop Name & Status */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <Store className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <h4 className="font-bold text-xs text-slate-900 truncate">
              {shop.name}
            </h4>
          </div>
          <p className="text-[10px] font-mono text-[#0045cc]">
            @{shop.sellerName}
          </p>
        </div>

        <span
          className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getShopBadge(
            shop.status
          )}`}
        >
          {shop.status}
        </span>
      </div>

      {/* 2. Owner & Shop Performance */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1.5 text-xs">
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">Chủ gian hàng:</span>
          <span className="font-medium text-slate-900 truncate max-w-[180px]">
            {shop.ownerName}
          </span>
        </div>

        <div className="flex items-center justify-between pt-1 border-t border-slate-200/60">
          <div className="text-[11px] text-slate-500">
            Sản phẩm: <strong className="text-slate-800 font-mono">{shop.productCount}</strong>
          </div>
          <div className="text-[11px] text-slate-500">
            Ví: <strong className="text-[#0045cc] font-mono font-bold">{formatPrice(shop.balance || 0)}</strong>
          </div>
        </div>
      </div>

      {/* 3. Actions */}
      <div className="flex items-center justify-end pt-1 border-t border-slate-100 text-xs">
        <button
          type="button"
          onClick={() => onManage(shop.id)}
          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-[#0045cc] hover:text-white text-slate-700 text-xs font-semibold transition-colors cursor-pointer"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>Quản lý gian hàng</span>
        </button>
      </div>
    </div>
  );
}
