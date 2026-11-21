"use client";

import React from "react";
import { ShieldCheck, ShieldAlert, Ban, MailCheck, MailWarning, User } from "lucide-react";
import type { AdminUserResponse } from "@/types/admin";
import type { UserStatus } from "@/types/enums";

interface AdminUserMobileCardProps {
  user: AdminUserResponse;
  onUpdateStatus: (user: AdminUserResponse, targetStatus: UserStatus) => void;
}

export function AdminUserMobileCard({
  user,
  onUpdateStatus,
}: AdminUserMobileCardProps) {
  const fullName =
    `${user.firstName || ""} ${user.lastName || ""}`.trim() ||
    "Người dùng TechHub";

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      case "SUSPENDED":
        return "bg-amber-100 text-amber-800 border-amber-200";
      default:
        return "bg-rose-100 text-rose-800 border-rose-200";
    }
  };

  return (
    <div className="p-3.5 space-y-2.5 bg-white">
      {/* 1. Header: Full name, email & Account status */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-1.5">
            <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <h4 className="font-bold text-xs text-slate-900 truncate">
              {fullName}
            </h4>
          </div>
          <p className="text-[10px] font-mono text-slate-500 truncate">
            {user.email}
          </p>
        </div>

        <span
          className={`text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${getStatusBadge(
            user.status
          )}`}
        >
          {user.status}
        </span>
      </div>

      {/* 2. Contact & Verification */}
      <div className="bg-slate-50 p-2.5 rounded-xl space-y-1.5 text-xs">
        <div className="flex items-center justify-between text-slate-700">
          <span className="text-slate-500 text-[11px]">SĐT:</span>
          <span className="font-mono text-slate-800 text-[11px]">
            {user.phone || "—"}
          </span>
        </div>

        <div className="flex items-center justify-between pt-1 border-t border-slate-200/60">
          <span className="text-slate-500 text-[11px]">Email xác thực:</span>
          {user.emailVerified ? (
            <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              <MailCheck className="w-3 h-3" /> Đã xác thực
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
              <MailWarning className="w-3 h-3" /> Chưa xác thực
            </span>
          )}
        </div>
      </div>

      {/* 3. Actions */}
      <div className="flex items-center justify-end gap-1.5 pt-1 border-t border-slate-100 text-xs">
        {user.status !== "ACTIVE" && (
          <button
            type="button"
            onClick={() => onUpdateStatus(user, "ACTIVE")}
            className="px-2.5 py-1.5 rounded-lg border border-emerald-200 text-emerald-700 hover:bg-emerald-50 text-xs font-semibold inline-flex items-center gap-1"
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Mở khóa</span>
          </button>
        )}

        {user.status !== "SUSPENDED" && (
          <button
            type="button"
            onClick={() => onUpdateStatus(user, "SUSPENDED")}
            className="px-2.5 py-1.5 rounded-lg border border-amber-200 text-amber-700 hover:bg-amber-50 text-xs font-semibold inline-flex items-center gap-1"
          >
            <ShieldAlert className="w-3.5 h-3.5" />
            <span>Tạm khóa</span>
          </button>
        )}

        {user.status !== "BANNED" && (
          <button
            type="button"
            onClick={() => onUpdateStatus(user, "BANNED")}
            className="px-2.5 py-1.5 rounded-lg border border-rose-200 text-rose-700 hover:bg-rose-50 text-xs font-semibold inline-flex items-center gap-1"
          >
            <Ban className="w-3.5 h-3.5" />
            <span>Cấm</span>
          </button>
        )}
      </div>
    </div>
  );
}
