"use client";

import React, { useState, useEffect } from "react";
import {
  Search,
  Eye,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useAdminOrdersQuery } from "../../hooks/useAdminOperations";
import { AdminOrderDetailDrawer } from "./AdminOrderDetailDrawer";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { AdminOrderMobileCard } from "./AdminOrderMobileCard";
import { ScrollableTabsWrapper } from "@/components/shared/ScrollableTabsWrapper";
import { formatPrice } from "@/lib/utils";
import type { OrderStatus } from "@/types/enums";

const ORDER_STATUS_TABS: { id: "" | OrderStatus; label: string }[] = [
  { id: "", label: "Tất cả" },
  { id: "PENDING_CONFIRMATION", label: "Chờ xác nhận" },
  { id: "PREPARING", label: "Đang chuẩn bị" },
  { id: "WAITING_FOR_PICKUP", label: "Chờ lấy hàng" },
  { id: "SHIPPING", label: "Đang giao" },
  { id: "DELIVERED", label: "Đã giao" },
  { id: "COMPLETED", label: "Hoàn thành" },
  { id: "RETURN_REQUESTED", label: "Khiếu nại/Trả hàng" },
  { id: "CANCELLED", label: "Đã hủy" },
];

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "Chờ thanh toán",
  PENDING_CONFIRMATION: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  WAITING_FOR_PICKUP: "Chờ lấy hàng",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  COMPLETED: "Hoàn thành",
  RETURN_REQUESTED: "Khiếu nại/Trả hàng",
  REFUNDED: "Đã hoàn tiền",
  CANCELLED: "Đã hủy",
};

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PAID: "Đã thanh toán",
  PENDING: "Chờ thanh toán",
  FAILED: "Thất bại",
  REFUNDED: "Đã hoàn tiền",
};

export function AdminOrdersView() {
  const [statusFilter, setStatusFilter] = useState<"" | OrderStatus>("");
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
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
  } = useAdminOrdersQuery({
    status: statusFilter,
    keyword: debouncedKeyword || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    page,
    size,
  });

  const handleSuccess = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const orders = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div>
        <h1 className="font-heading text-xl sm:text-2xl font-bold text-[#0b1c30] tracking-tight">
          Giám sát Đơn hàng Toàn sàn
        </h1>
      </div>

      {/* Toast */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col gap-3">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-2.5 pb-2 border-b border-slate-100">
          <ScrollableTabsWrapper className="w-full min-w-0 lg:flex-1">
            <div
              role="tablist"
              aria-label="Lọc đơn hàng theo trạng thái"
              className="flex items-center gap-1.5 overflow-x-auto no-scrollbar"
            >
              {ORDER_STATUS_TABS.map((tab) => (
                <button
                  key={tab.id || "ALL"}
                  type="button"
                  role="tab"
                  aria-selected={statusFilter === tab.id}
                  onClick={() => {
                    setStatusFilter(tab.id);
                    setPage(0);
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap shrink-0 cursor-pointer transition-colors duration-150 min-h-[34px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                    statusFilter === tab.id
                      ? "bg-[#0045cc] text-white"
                      : "text-slate-600 hover:bg-slate-100"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </ScrollableTabsWrapper>

          <AdminTablePagination
            variant="top-mini"
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={setPage}
            isLoading={isFetching}
            className="self-end lg:self-auto shrink-0"
          />
        </div>

        <div className="flex flex-col md:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label="Tìm theo mã đơn hàng, tên khách hàng hoặc tên shop"
              placeholder="Tìm theo mã đơn hàng, tên khách hàng, tên shop..."
              className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
            />
          </div>
          <div className="grid grid-cols-2 gap-2 w-full md:w-auto md:flex md:items-center">
            <div className="flex flex-col md:flex-row md:items-center gap-1">
              <span className="text-[10px] text-slate-500 font-medium md:hidden">Từ ngày:</span>
              <input
                type="date"
                value={startDate}
                aria-label="Lọc đơn từ ngày"
                onChange={(e) => {
                  setStartDate(e.target.value);
                  setPage(0);
                }}
                className="w-full md:w-auto h-10 px-3 rounded-xl border border-slate-200 text-xs text-slate-700 bg-white"
              />
            </div>
            <span aria-hidden="true" className="text-xs text-slate-400 hidden md:inline">
              →
            </span>
            <div className="flex flex-col md:flex-row md:items-center gap-1">
              <span className="text-[10px] text-slate-500 font-medium md:hidden">Đến ngày:</span>
              <input
                type="date"
                value={endDate}
                aria-label="Lọc đơn đến ngày"
                onChange={(e) => {
                  setEndDate(e.target.value);
                  setPage(0);
                }}
                className="w-full md:w-auto h-10 px-3 rounded-xl border border-slate-200 text-xs text-slate-700 bg-white"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Orders Table */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500">
            Đang tải danh sách đơn hàng...
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
      ) : orders.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-xs text-slate-500">
          Không có đơn hàng nào khớp bộ lọc.
        </div>
      ) : (
        <div className="relative bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {isFetching && (
            <div className="absolute top-0 left-0 right-0 h-0.5 bg-primary-container z-10 animate-pulse motion-reduce:animate-none" />
          )}

          {/* Mobile Card View (< sm) */}
          <div className="divide-y divide-slate-100 sm:hidden">
            {orders.map((o) => (
              <AdminOrderMobileCard
                key={o.orderId}
                order={o}
                onViewDetails={setSelectedOrderId}
              />
            ))}
          </div>

          {/* Desktop Table (>= sm) */}
          <div className="overflow-x-auto no-scrollbar hidden sm:block">
            <table className="w-full min-w-[860px] text-left text-xs border-collapse table-fixed">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                <tr>
                  <th className="py-2.5 px-4 w-[16%]">Mã đơn / Ngày</th>
                  <th className="py-2.5 px-3 w-[17%]">Gian hàng</th>
                  <th className="py-2.5 px-3 w-[17%]">Người mua</th>
                  <th className="py-2.5 px-3 w-[13%] text-right">Tổng thanh toán</th>
                  <th className="py-2.5 px-3 w-[12%] text-right">Hoa hồng</th>
                  <th className="py-2.5 px-2 w-[14%] text-center">Trạng thái</th>
                  <th className="py-2.5 px-4 w-[11%] text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody
                className={`divide-y divide-slate-100 transition-opacity duration-150 ${
                  isPlaceholderData && isFetching ? "opacity-60" : "opacity-100"
                }`}
              >
                {orders.map((o) => (
                  <tr key={o.orderId} className="hover:bg-slate-50/70">
                    <td className="py-2 px-4">
                      <p className="font-mono font-bold text-on-surface truncate">
                        #{o.orderNumber}
                      </p>
                      <p className="text-[10px] text-slate-600">
                        {new Date(o.createdAt).toLocaleString("vi-VN")}
                      </p>
                    </td>
                    <td className="py-2 px-3">
                      <p className="font-semibold text-slate-800 truncate">
                        {o.shop.shopName}
                      </p>
                      <p className="text-[10px] text-slate-600 truncate">
                        {o.totalItemsCount} sản phẩm
                      </p>
                    </td>
                    <td className="py-2 px-3">
                      <p className="font-semibold text-slate-800 truncate">
                        {o.buyer.buyerName}
                      </p>
                      <p className="text-[10px] text-slate-600 truncate">
                        {o.paymentMethodName} • {PAYMENT_STATUS_LABELS[o.paymentStatus] || o.paymentStatus}
                      </p>
                    </td>
                    <td className="py-2 px-3 text-right font-mono font-bold text-slate-900">
                      {formatPrice(o.buyerTotalAmount)}
                    </td>
                    <td className="py-2 px-3 text-right font-mono font-bold text-emerald-700">
                      {formatPrice(o.totalCommission)}
                    </td>
                    <td className="py-2 px-2 text-center">
                      <span
                        className={`inline-block whitespace-nowrap px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
                          o.status === "COMPLETED"
                            ? "bg-emerald-100 text-emerald-700"
                            : o.status === "CANCELLED" || o.status === "REFUNDED"
                            ? "bg-rose-100 text-rose-700"
                            : "bg-blue-100 text-[#0045cc]"
                        }`}
                      >
                        {ORDER_STATUS_LABELS[o.status] || o.status}
                      </span>
                    </td>
                    <td className="py-2 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => setSelectedOrderId(o.orderId)}
                        className="px-2.5 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-bold inline-flex items-center gap-1 whitespace-nowrap transition-colors duration-150 cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Xem</span>
                      </button>
                    </td>
                  </tr>
                ))}
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

      <AdminOrderDetailDrawer
        orderId={selectedOrderId}
        onClose={() => setSelectedOrderId(null)}
        onActionSuccess={handleSuccess}
      />
    </div>
  );
}
