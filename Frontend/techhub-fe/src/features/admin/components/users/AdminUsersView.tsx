"use client";

import React, { useState, useEffect } from "react";
import {
  Search,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  ShieldCheck,
  Ban,
  MailCheck,
  MailWarning,
} from "lucide-react";
import { useAdminUsersQuery } from "../../hooks/useAdminOperations";
import { UserStatusModal } from "./UserStatusModal";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import type { AdminUserResponse } from "@/types/admin";
import type { UserStatus } from "@/types/enums";

export function AdminUsersView() {
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<"" | UserStatus>("");
  const [verifiedFilter, setVerifiedFilter] = useState<"" | "true" | "false">(
    ""
  );
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);

  const [modalState, setModalState] = useState<{
    user: AdminUserResponse | null;
    targetStatus: UserStatus | null;
  }>({ user: null, targetStatus: null });
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const t = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [keyword, debouncedKeyword]);

  const {
    data,
    isLoading,
    isFetching,
    isPlaceholderData,
    isError,
    refetch,
  } = useAdminUsersQuery({
    keyword: debouncedKeyword || undefined,
    status: statusFilter,
    emailVerified:
      verifiedFilter === "" ? undefined : verifiedFilter === "true",
    page,
    size,
  });

  const handleSuccess = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const users = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div>
        <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
          Quản lý Người dùng Hệ thống
        </h1>
        <p className="text-[11px] text-slate-500">
          Tra cứu tài khoản Buyer/Seller, kiểm tra xác thực email và thực thi chế tài tạm khóa hoặc cấm vĩnh viễn.
        </p>
      </div>

      {/* Toast */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col md:flex-row gap-3 items-stretch md:items-center">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            aria-label="Tìm theo tên, email hoặc số điện thoại"
            placeholder="Tìm theo tên, email, số điện thoại..."
            className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
          />
        </div>

        <select
          value={statusFilter}
          aria-label="Lọc theo trạng thái tài khoản"
          onChange={(e) => {
            setStatusFilter(e.target.value as "" | UserStatus);
            setPage(0);
          }}
          className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-44"
        >
          <option value="">Tất cả Trạng thái</option>
          <option value="ACTIVE">Hoạt động (ACTIVE)</option>
          <option value="SUSPENDED">Tạm khóa (SUSPENDED)</option>
          <option value="BANNED">Cấm vĩnh viễn (BANNED)</option>
        </select>

        <select
          value={verifiedFilter}
          aria-label="Lọc theo trạng thái xác thực email"
          onChange={(e) => {
            setVerifiedFilter(e.target.value as "" | "true" | "false");
            setPage(0);
          }}
          className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-44"
        >
          <option value="">Tất cả Xác thực</option>
          <option value="true">Đã xác thực Email</option>
          <option value="false">Chưa xác thực Email</option>
        </select>

        <AdminTablePagination
          variant="top-mini"
          page={page}
          size={size}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={setPage}
          isLoading={isFetching}
          className="self-end md:self-auto"
        />
      </div>

      {/* Users Table */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500">
            Đang tải danh sách người dùng...
          </span>
        </div>
      ) : isError ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <button
            type="button"
            onClick={() => refetch()}
            className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : users.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-xs text-slate-500">
          Không tìm thấy người dùng nào phù hợp.
        </div>
      ) : (
        <div className="relative bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {isFetching && (
            <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-container z-10 animate-pulse motion-reduce:animate-none" />
          )}
          <div className="overflow-x-auto no-scrollbar">
            <table className="w-full min-w-[820px] text-left text-xs border-collapse table-fixed">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                <tr>
                  <th className="py-2.5 px-4 w-[24%]">Người dùng / Email</th>
                  <th className="py-2.5 px-3 w-[14%]">Điện thoại</th>
                  <th className="py-2.5 px-3 w-[24%]">Địa chỉ mặc định</th>
                  <th className="py-2.5 px-2 w-[12%] text-center">Xác thực</th>
                  <th className="py-2.5 px-2 w-[12%] text-center">Trạng thái</th>
                  <th className="py-2.5 px-4 w-[14%] text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody
                className={`divide-y divide-slate-100 transition-opacity duration-150 ${
                  isPlaceholderData && isFetching ? "opacity-60" : "opacity-100"
                }`}
              >
                {users.map((u) => {
                  const fullName =
                    `${u.firstName || ""} ${u.lastName || ""}`.trim() ||
                    "Người dùng TechHub";
                  return (
                    <tr key={u.id} className="hover:bg-slate-50/70">
                      <td className="py-2 px-4">
                        <p className="font-bold text-slate-900 truncate">
                          {fullName}
                        </p>
                        <p className="text-[10px] font-mono text-slate-500 truncate">
                          {u.email}
                        </p>
                      </td>
                      <td className="py-2 px-3 font-mono text-slate-700 truncate">
                        {u.phone || "—"}
                      </td>
                      <td className="py-2 px-3 text-slate-600 truncate">
                        {u.defaultAddress || "Chưa thiết lập"}
                      </td>
                      <td className="py-2 px-2 text-center">
                        {u.emailVerified ? (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                            <MailCheck className="w-3 h-3" /> Đã xác thực
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full">
                            <MailWarning className="w-3 h-3" /> Chưa xác thực
                          </span>
                        )}
                      </td>
                      <td className="py-2 px-2 text-center">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                            u.status === "ACTIVE"
                              ? "bg-emerald-100 text-emerald-700"
                              : u.status === "SUSPENDED"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-rose-600 text-white"
                          }`}
                        >
                          {u.status}
                        </span>
                      </td>
                      <td className="py-2 px-4 text-right">
                        <div className="inline-flex items-center gap-1">
                          {u.status !== "ACTIVE" && (
                            <button
                              type="button"
                              onClick={() =>
                                setModalState({
                                  user: u,
                                  targetStatus: "ACTIVE",
                                })
                              }
                              className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-emerald-600 hover:bg-emerald-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600"
                              title="Mở khóa ACTIVE"
                              aria-label={`Mở khóa tài khoản ${fullName}`}
                            >
                              <ShieldCheck className="w-4 h-4" />
                            </button>
                          )}
                          {u.status === "ACTIVE" && (
                            <button
                              type="button"
                              onClick={() =>
                                setModalState({
                                  user: u,
                                  targetStatus: "SUSPENDED",
                                })
                              }
                              className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-amber-600 hover:bg-amber-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-600"
                              title="Tạm khóa (SUSPENDED)"
                              aria-label={`Tạm khóa tài khoản ${fullName}`}
                            >
                              <ShieldAlert className="w-4 h-4" />
                            </button>
                          )}
                          {u.status !== "BANNED" && (
                            <button
                              type="button"
                              onClick={() =>
                                setModalState({
                                  user: u,
                                  targetStatus: "BANNED",
                                })
                              }
                              className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-rose-600 hover:bg-rose-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-600"
                              title="Cấm vĩnh viễn (BANNED)"
                              aria-label={`Cấm vĩnh viễn tài khoản ${fullName}`}
                            >
                              <Ban className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Bottom Pagination */}
          <AdminTablePagination
            variant="bottom-full"
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={setPage}
            onSizeChange={setSize}
            sizeOptions={[5, 10, 25, 50]}
            isLoading={isFetching}
          />
        </div>
      )}

      <UserStatusModal
        user={modalState.user}
        targetStatus={modalState.targetStatus}
        onClose={() => setModalState({ user: null, targetStatus: null })}
        onSuccess={handleSuccess}
      />
    </div>
  );
}
