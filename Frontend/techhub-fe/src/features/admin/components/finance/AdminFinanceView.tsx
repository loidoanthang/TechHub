"use client";

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Ticket,
  Plus,
  Search,
  Edit,
  Trash2,
  Loader2,
  AlertCircle,
  CheckCircle2,
  XCircle,
  ChevronLeft,
  ChevronRight,
  Landmark,
  X,
  AlertTriangle,
} from "lucide-react";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  useAdminVouchersQuery,
  useUpdatePlatformVoucherStatusMutation,
  useDeletePlatformVoucherMutation,
  useAdminPayoutsQuery,
  useProcessPayoutMutation,
} from "../../hooks/useAdminFinance";
import {
  rejectionReasonSchema,
  type RejectionReasonFormValues,
} from "../../schemas/moderationSchema";
import { PlatformVoucherModal } from "./PlatformVoucherModal";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { formatPrice } from "@/lib/utils";
import type { AdminPayoutResponse, AdminVoucherResponse } from "@/types/adminFinance";
import type { PayoutStatus, VoucherType } from "@/types/enums";

type FinanceTab = "VOUCHERS" | "PAYOUTS";

export function AdminFinanceView() {
  const [activeTab, setActiveTab] = useState<FinanceTab>("VOUCHERS");
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [voucherTypeFilter, setVoucherTypeFilter] = useState<"" | VoucherType>("PLATFORM");
  const [payoutStatusFilter, setPayoutStatusFilter] = useState<"" | PayoutStatus>("PENDING");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);

  const [voucherModalOpen, setVoucherModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] =
    useState<AdminVoucherResponse | null>(null);
  const [rejectingPayout, setRejectingPayout] =
    useState<AdminPayoutResponse | null>(null);
  const [confirmAction, setConfirmAction] = useState<
    | { type: "DELETE_VOUCHER"; voucher: AdminVoucherResponse }
    | { type: "APPROVE_PAYOUT"; payout: AdminPayoutResponse }
    | null
  >(null);

  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useAdminDialogA11y(Boolean(rejectingPayout), () => setRejectingPayout(null));
  useAdminDialogA11y(Boolean(confirmAction), () => setConfirmAction(null));

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const t = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [keyword, debouncedKeyword]);

  const vouchersQuery = useAdminVouchersQuery(
    {
      type: voucherTypeFilter,
      keyword: debouncedKeyword || undefined,
      page,
      size,
    },
    activeTab === "VOUCHERS"
  );

  const payoutsQuery = useAdminPayoutsQuery(
    {
      status: payoutStatusFilter,
      keyword: debouncedKeyword || undefined,
      page,
      size,
    },
    activeTab === "PAYOUTS"
  );

  const toggleVoucherStatusMutation = useUpdatePlatformVoucherStatusMutation();
  const deleteVoucherMutation = useDeletePlatformVoucherMutation();
  const processPayoutMutation = useProcessPayoutMutation();

  const rejectForm = useForm<RejectionReasonFormValues>({
    resolver: zodResolver(rejectionReasonSchema),
    defaultValues: { reason: "" },
  });

  const handleSuccess = (msg: string) => {
    setErrorMsg(null);
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const handleToggleVoucher = async (v: AdminVoucherResponse) => {
    try {
      await toggleVoucherStatusMutation.mutateAsync({
        id: v.id,
        data: { isActive: !v.isActive },
      });
      handleSuccess(
        `Đã ${!v.isActive ? "kích hoạt" : "tạm tắt"} voucher "${v.code}".`
      );
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể thay đổi trạng thái voucher.";
      setErrorMsg(msg);
    }
  };

  const handleDeleteVoucher = async (v: AdminVoucherResponse) => {
    try {
      await deleteVoucherMutation.mutateAsync(v.id);
      setConfirmAction(null);
      handleSuccess(`Đã xóa voucher "${v.code}".`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Xóa voucher thất bại.";
      setConfirmAction(null);
      setErrorMsg(msg);
    }
  };

  const handleApprovePayout = async (p: AdminPayoutResponse) => {
    try {
      await processPayoutMutation.mutateAsync({
        id: p.id,
        data: { action: "APPROVE" },
      });
      setConfirmAction(null);
      handleSuccess(
        `Đã duyệt giải ngân ${formatPrice(p.amount)} cho Shop "${p.shopName || p.sellerName}".`
      );
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Duyệt giải ngân thất bại.";
      setConfirmAction(null);
      setErrorMsg(msg);
    }
  };

  const handleRejectPayoutSubmit = async (values: RejectionReasonFormValues) => {
    if (!rejectingPayout) return;
    try {
      await processPayoutMutation.mutateAsync({
        id: rejectingPayout.id,
        data: {
          action: "REJECT",
          rejectionReason: values.reason.trim(),
        },
      });
      handleSuccess(
        `Đã từ chối yêu cầu rút tiền của "${rejectingPayout.shopName || rejectingPayout.sellerName}".`
      );
      setRejectingPayout(null);
      rejectForm.reset();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Từ chối giải ngân thất bại.";
      setErrorMsg(msg);
    }
  };

  const currentQuery = activeTab === "VOUCHERS" ? vouchersQuery : payoutsQuery;
  const totalPages = currentQuery.data?.totalPages || 1;
  const totalElements = currentQuery.data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="font-heading text-lg font-bold text-on-surface">
            Tài chính, Payouts &amp; Voucher Toàn sàn
          </h1>
          <p className="text-[11px] text-slate-600">
            Quản lý mã khuyến mãi toàn sàn do TechHub tài trợ và phê duyệt giải ngân doanh thu về ví Seller.
          </p>
        </div>

        {activeTab === "VOUCHERS" && (
          <button
            type="button"
            onClick={() => {
              setEditingVoucher(null);
              setVoucherModalOpen(true);
            }}
            className="h-10 px-4 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold flex items-center gap-1.5 shadow-xs self-start sm:self-auto cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo Voucher Sàn</span>
          </button>
        )}
      </div>

      {/* Feedbacks */}
      {toastMsg && (
        <div
          role="status"
          className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2"
        >
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}
      {errorMsg && (
        <div
          role="alert"
          className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center gap-2"
        >
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Tabs & Filters */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col gap-3">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 border-b border-slate-100 pb-2.5">
          <div
            role="tablist"
            aria-label="Phân mục Tài chính & Voucher"
            className="flex items-center gap-2 overflow-x-auto no-scrollbar"
          >
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "VOUCHERS"}
              onClick={() => {
                setActiveTab("VOUCHERS");
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 whitespace-nowrap cursor-pointer min-h-[36px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                activeTab === "VOUCHERS"
                  ? "bg-on-surface text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Ticket className="w-4 h-4" />
              <span>Mã Giảm Giá Toàn Sàn</span>
            </button>

            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "PAYOUTS"}
              onClick={() => {
                setActiveTab("PAYOUTS");
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 whitespace-nowrap cursor-pointer min-h-[36px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                activeTab === "PAYOUTS"
                  ? "bg-on-surface text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Landmark className="w-4 h-4" />
              <span>Yêu cầu Rút tiền (Payouts)</span>
            </button>
          </div>

          <AdminTablePagination
            variant="top-mini"
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={setPage}
            isLoading={currentQuery.isFetching}
            className="self-end sm:self-auto"
          />
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label={
                activeTab === "VOUCHERS"
                  ? "Tìm theo mã voucher hoặc tên chương trình"
                  : "Tìm theo tên shop, slug seller, số tài khoản"
              }
              placeholder={
                activeTab === "VOUCHERS"
                  ? "Tìm theo mã voucher hoặc tên chương trình..."
                  : "Tìm theo tên shop, slug seller, số tài khoản..."
              }
              className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
            />
          </div>

          {activeTab === "VOUCHERS" ? (
            <select
              value={voucherTypeFilter}
              aria-label="Lọc theo loại voucher"
              onChange={(e) => {
                setVoucherTypeFilter(e.target.value as "" | VoucherType);
                setPage(0);
              }}
              className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-52"
            >
              <option value="PLATFORM">Voucher Toàn Sàn (PLATFORM)</option>
              <option value="SHOP">Voucher của Shop (SHOP)</option>
              <option value="">Tất cả Voucher</option>
            </select>
          ) : (
            <select
              value={payoutStatusFilter}
              aria-label="Lọc theo trạng thái giải ngân"
              onChange={(e) => {
                setPayoutStatusFilter(e.target.value as "" | PayoutStatus);
                setPage(0);
              }}
              className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-52"
            >
              <option value="PENDING">Chờ duyệt giải ngân (PENDING)</option>
              <option value="COMPLETED">Đã giải ngân (COMPLETED)</option>
              <option value="REJECTED">Đã từ chối (REJECTED)</option>
              <option value="">Tất cả trạng thái</option>
            </select>
          )}
        </div>
      </div>

      {/* Content */}
      {currentQuery.isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-primary animate-spin motion-reduce:animate-none" />
          <span className="text-xs text-slate-600">Đang tải dữ liệu...</span>
        </div>
      ) : currentQuery.isError ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <button
            type="button"
            onClick={() => currentQuery.refetch()}
            className="px-4 py-2 rounded-xl bg-primary text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : activeTab === "VOUCHERS" ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {(vouchersQuery.data?.content || []).length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-500">
              Chưa có mã giảm giá nào phù hợp.
            </div>
          ) : (
            <div className="overflow-x-auto no-scrollbar">
              <table className="w-full min-w-[820px] text-left text-xs border-collapse table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                  <tr>
                    <th className="py-2.5 px-4 w-[24%]">Mã / Chương trình</th>
                    <th className="py-2.5 px-3 w-[14%]">Loại / Phạm vi</th>
                    <th className="py-2.5 px-3 w-[16%] text-right">Mức giảm</th>
                    <th className="py-2.5 px-3 w-[14%] text-center">Đã dùng</th>
                    <th className="py-2.5 px-2 w-[14%] text-center">Trạng thái</th>
                    <th className="py-2.5 px-4 w-[18%] text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(vouchersQuery.data?.content || []).map((v) => (
                    <tr key={v.id} className="hover:bg-slate-50/70">
                      <td className="py-2 px-4">
                        <p className="font-mono font-extrabold text-[#0045cc] truncate">
                          {v.code}
                        </p>
                        <p className="text-[11px] font-semibold text-slate-800 truncate">
                          {v.name}
                        </p>
                      </td>
                      <td className="py-2 px-3">
                        <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[10px] font-bold">
                          {v.type}
                        </span>
                        <p className="text-[10px] text-slate-500 mt-0.5">
                          {v.appliesTo === "SHIPPING_FEE"
                            ? "FreeShip"
                            : "Tiền hàng"}
                        </p>
                      </td>
                      <td className="py-2 px-3 text-right font-mono font-bold text-slate-900">
                        {v.discountType === "PERCENTAGE"
                          ? `${v.discountValue}%`
                          : formatPrice(v.discountValue)}
                        {v.minOrderAmount > 0 && (
                          <p className="text-[10px] font-normal text-slate-500">
                            Đơn từ {formatPrice(v.minOrderAmount)}
                          </p>
                        )}
                      </td>
                      <td className="py-2 px-3 text-center font-mono">
                        {v.usedCount} / {v.usageLimit ?? "∞"}
                      </td>
                      <td className="py-2 px-2 text-center">
                        <button
                          type="button"
                          disabled={v.type !== "PLATFORM"}
                          onClick={() => handleToggleVoucher(v)}
                          className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
                            v.isActive
                              ? "bg-emerald-100 text-emerald-700"
                              : "bg-slate-200 text-slate-600"
                          } ${v.type === "PLATFORM" ? "cursor-pointer" : ""}`}
                        >
                          {v.status}
                        </button>
                      </td>
                      <td className="py-2 px-4 text-right">
                        {v.type === "PLATFORM" ? (
                          <div className="inline-flex items-center gap-1">
                            <button
                              type="button"
                              aria-label={`Sửa voucher ${v.code}`}
                              title={`Sửa voucher ${v.code}`}
                              onClick={() => {
                                setEditingVoucher(v);
                                setVoucherModalOpen(true);
                              }}
                              className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-slate-600 hover:text-primary hover:bg-surface-container-low cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                            <button
                              type="button"
                              aria-label={`Xóa voucher ${v.code}`}
                              title={`Xóa voucher ${v.code}`}
                              onClick={() =>
                                setConfirmAction({
                                  type: "DELETE_VOUCHER",
                                  voucher: v,
                                })
                              }
                              className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-slate-600 hover:text-rose-600 hover:bg-rose-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-600"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        ) : (
                          <span className="text-[11px] text-slate-600">
                            {v.shopName}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

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
            isLoading={currentQuery.isFetching}
          />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {(payoutsQuery.data?.content || []).length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-500">
              Không có yêu cầu rút tiền (Payout) nào phù hợp bộ lọc.
            </div>
          ) : (
            <div className="overflow-x-auto no-scrollbar">
              <table className="w-full min-w-[820px] text-left text-xs border-collapse table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                  <tr>
                    <th className="py-2.5 px-4 w-[22%]">Gian hàng / Seller</th>
                    <th className="py-2.5 px-3 w-[26%]">Ngân hàng thụ hưởng</th>
                    <th className="py-2.5 px-3 w-[16%] text-right">Số tiền rút</th>
                    <th className="py-2.5 px-3 w-[14%] text-center">Trạng thái</th>
                    <th className="py-2.5 px-4 w-[22%] text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(payoutsQuery.data?.content || []).map((p) => (
                    <tr key={p.id} className="hover:bg-slate-50/70">
                      <td className="py-2 px-4">
                        <p className="font-bold text-slate-900 truncate">
                          {p.shopName || p.sellerName}
                        </p>
                        <p className="text-[11px] font-mono text-slate-500 truncate">
                          @{p.sellerName}
                        </p>
                      </td>
                      <td className="py-2 px-3">
                        <p className="font-semibold text-slate-800 truncate">
                          {p.bankName}
                        </p>
                        <p className="text-[11px] font-mono text-slate-500 truncate">
                          {p.bankAccountNumber} • {p.bankAccountHolder}
                        </p>
                      </td>
                      <td className="py-2 px-3 text-right font-mono font-extrabold text-[#0045cc]">
                        {formatPrice(p.amount)}
                      </td>
                      <td className="py-2 px-3 text-center">
                        <span
                          className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
                            p.status === "COMPLETED"
                              ? "bg-emerald-100 text-emerald-700"
                              : p.status === "PENDING"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-rose-100 text-rose-700"
                          }`}
                        >
                          {p.status}
                        </span>
                      </td>
                      <td className="py-2 px-4 text-right">
                        {p.status === "PENDING" ? (
                          <div className="inline-flex items-center gap-1.5">
                            <button
                              type="button"
                              disabled={processPayoutMutation.isPending}
                              onClick={() =>
                                setConfirmAction({
                                  type: "APPROVE_PAYOUT",
                                  payout: p,
                                })
                              }
                              className="min-h-[34px] px-2.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-bold inline-flex items-center gap-1 cursor-pointer"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Duyệt chi</span>
                            </button>
                            <button
                              type="button"
                              disabled={processPayoutMutation.isPending}
                              onClick={() => {
                                setRejectingPayout(p);
                                rejectForm.reset({ reason: "" });
                              }}
                              className="min-h-[34px] px-2.5 py-1.5 rounded-xl border border-rose-300 bg-white hover:bg-rose-50 text-rose-600 text-[11px] font-bold inline-flex items-center gap-1 cursor-pointer"
                            >
                              <XCircle className="w-3.5 h-3.5" />
                              <span>Từ chối</span>
                            </button>
                          </div>
                        ) : (
                          <span className="text-[11px] text-slate-600">
                            {p.processedByName || "Đã xử lý"}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

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
            isLoading={currentQuery.isFetching}
          />
        </div>
      )}

      {/* Confirmation Modal for Delete Voucher / Approve Payout */}
      {confirmAction && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="finance-confirm-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            className="fixed inset-0 bg-slate-950/50"
            onClick={() => setConfirmAction(null)}
          />
          <div className="relative z-10 w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-200 flex flex-col gap-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-2xl flex items-center justify-center shrink-0 ${
                    confirmAction.type === "DELETE_VOUCHER"
                      ? "bg-rose-100 text-rose-700"
                      : "bg-emerald-100 text-emerald-700"
                  }`}
                >
                  {confirmAction.type === "DELETE_VOUCHER" ? (
                    <AlertTriangle className="w-5 h-5" />
                  ) : (
                    <Landmark className="w-5 h-5" />
                  )}
                </div>
                <div>
                  <h3
                    id="finance-confirm-title"
                    className="font-heading text-sm font-extrabold text-on-surface"
                  >
                    {confirmAction.type === "DELETE_VOUCHER"
                      ? `Xác nhận xóa Voucher ${confirmAction.voucher.code}`
                      : "Xác nhận duyệt giải ngân Payout"}
                  </h3>
                  <p className="text-xs text-slate-600 mt-0.5">
                    {confirmAction.type === "DELETE_VOUCHER"
                      ? confirmAction.voucher.name
                      : confirmAction.payout.shopName ||
                        confirmAction.payout.sellerName}
                  </p>
                </div>
              </div>
              <button
                type="button"
                aria-label="Đóng hộp thoại"
                onClick={() => setConfirmAction(null)}
                className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              {confirmAction.type === "DELETE_VOUCHER"
                ? `Thao tác này sẽ xóa vĩnh viễn mã khuyến mãi "${confirmAction.voucher.code}" khỏi hệ thống và không thể hoàn tác.`
                : `Xác nhận giải ngân số tiền ${formatPrice(
                    confirmAction.payout.amount
                  )} về tài khoản ${confirmAction.payout.bankName} (${
                    confirmAction.payout.bankAccountNumber
                  })?`}
            </p>

            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={() => setConfirmAction(null)}
                className="min-h-[40px] px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                disabled={
                  deleteVoucherMutation.isPending ||
                  processPayoutMutation.isPending
                }
                onClick={() =>
                  confirmAction.type === "DELETE_VOUCHER"
                    ? handleDeleteVoucher(confirmAction.voucher)
                    : handleApprovePayout(confirmAction.payout)
                }
                className={`min-h-[40px] px-4 py-2 rounded-xl text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer disabled:opacity-50 ${
                  confirmAction.type === "DELETE_VOUCHER"
                    ? "bg-rose-600 hover:bg-rose-700"
                    : "bg-emerald-600 hover:bg-emerald-700"
                }`}
              >
                {(deleteVoucherMutation.isPending ||
                  processPayoutMutation.isPending) && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none" />
                )}
                <span>
                  {confirmAction.type === "DELETE_VOUCHER"
                    ? "Xác nhận Xóa"
                    : "Xác nhận Duyệt chi"}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Payout Modal */}
      {rejectingPayout && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="reject-payout-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            className="fixed inset-0 bg-slate-950/50"
            onClick={() => setRejectingPayout(null)}
          />
          <div className="relative z-10 w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between mb-3">
              <h3
                id="reject-payout-title"
                className="font-heading text-sm font-extrabold text-on-surface"
              >
                Từ chối Yêu cầu Giải ngân
              </h3>
              <button
                type="button"
                aria-label="Đóng hộp thoại"
                onClick={() => setRejectingPayout(null)}
                className="min-w-[36px] min-h-[36px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <form
              onSubmit={rejectForm.handleSubmit(handleRejectPayoutSubmit)}
              className="flex flex-col gap-3"
            >
              <textarea
                rows={3}
                {...rejectForm.register("reason")}
                placeholder="Nhập lý do từ chối chuyển khoản (VD: Sai tên chủ tài khoản ngân hàng)..."
                className="w-full p-3 rounded-xl border border-slate-200 text-xs"
              />
              {rejectForm.formState.errors.reason && (
                <p className="text-[11px] text-rose-600">
                  {rejectForm.formState.errors.reason.message}
                </p>
              )}
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setRejectingPayout(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={processPayoutMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-rose-600 text-white text-xs font-bold flex items-center gap-1.5"
                >
                  {processPayoutMutation.isPending && (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  )}
                  <span>Xác nhận Từ chối</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <PlatformVoucherModal
        isOpen={voucherModalOpen}
        initialData={editingVoucher}
        onClose={() => {
          setVoucherModalOpen(false);
          setEditingVoucher(null);
        }}
        onSuccess={handleSuccess}
      />
    </div>
  );
}
