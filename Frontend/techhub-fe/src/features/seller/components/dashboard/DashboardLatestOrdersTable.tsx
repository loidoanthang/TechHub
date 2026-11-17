"use client";

import React from "react";
import Link from "next/link";
import { formatPrice } from "@/lib/utils";
import { useSellerOrdersQuery } from "../../hooks/useSellerOrders";
import {
  MoreHorizontal,
  SlidersHorizontal,
  Filter,
  Download,
  ShoppingBag,
  ExternalLink,
} from "lucide-react";
import type { SellerOrderResponse } from "@/types/sellerOrders";

function getStatusBadge(status: string) {
  switch (status) {
    case "COMPLETED":
    case "DELIVERED":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
          Completed
        </span>
      );
    case "SHIPPING":
    case "WAITING_FOR_PICKUP":
    case "PREPARING":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-[#0045cc] border border-blue-200">
          Processing
        </span>
      );
    case "PENDING_CONFIRMATION":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
          Pending
        </span>
      );
    case "CANCELLED":
    case "REFUNDED":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-red-50 text-red-600 border border-red-200">
          Cancelled
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600">
          {status}
        </span>
      );
  }
}

export function DashboardLatestOrdersTable() {
  const { data: pageData, isLoading } = useSellerOrdersQuery({
    page: 0,
    size: 5,
  });

  const orders: SellerOrderResponse[] = pageData?.content || [];

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Table Header Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
        <div>
          <h3 className="font-heading text-base font-bold text-[#0b1c30]">
            Latest Orders
          </h3>
          <p className="text-[11px] text-[#737688]">
            Đơn hàng mới nhất phát sinh từ khách hàng
          </p>
        </div>

        {/* Action buttons (Customize, Filter, Export) */}
        <div className="flex items-center gap-2">
          <Link
            href="/seller/orders"
            className="px-3 py-1.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors inline-flex items-center gap-1.5"
          >
            <SlidersHorizontal className="w-3.5 h-3.5 text-slate-500" />
            <span>Customize</span>
          </Link>
          <Link
            href="/seller/orders"
            className="px-3 py-1.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors inline-flex items-center gap-1.5"
          >
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <span>Filter</span>
          </Link>
          <button
            type="button"
            onClick={() => alert("Xuất danh sách đơn hàng ra file Excel")}
            className="px-3 py-1.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors inline-flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            <span>Export</span>
          </button>
        </div>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto -mx-5 sm:mx-0">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-100 text-slate-400 font-semibold text-[11px] tracking-wider uppercase">
              <th className="py-2.5 px-3">Order ID</th>
              <th className="py-2.5 px-3">Customer / Product</th>
              <th className="py-2.5 px-3">Order Date</th>
              <th className="py-2.5 px-3">Price</th>
              <th className="py-2.5 px-3">Payment</th>
              <th className="py-2.5 px-3">Status</th>
              <th className="py-2.5 px-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              [1, 2, 3, 4].map((i) => (
                <tr key={i} className="animate-pulse">
                  <td className="py-3.5 px-3">
                    <div className="h-4 bg-slate-100 rounded w-16" />
                  </td>
                  <td className="py-3.5 px-3">
                    <div className="h-4 bg-slate-100 rounded w-36" />
                  </td>
                  <td className="py-3.5 px-3">
                    <div className="h-4 bg-slate-100 rounded w-24" />
                  </td>
                  <td className="py-3.5 px-3">
                    <div className="h-4 bg-slate-100 rounded w-16" />
                  </td>
                  <td className="py-3.5 px-3">
                    <div className="h-4 bg-slate-100 rounded w-20" />
                  </td>
                  <td className="py-3.5 px-3">
                    <div className="h-5 bg-slate-100 rounded-full w-20" />
                  </td>
                  <td className="py-3.5 px-3 text-right">
                    <div className="h-4 bg-slate-100 rounded w-6 ml-auto" />
                  </td>
                </tr>
              ))
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-10 text-center text-slate-400">
                  <ShoppingBag className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                  <span>Chưa có đơn hàng nào phát sinh gần đây</span>
                </td>
              </tr>
            ) : (
              orders.map((order) => {
                const orderCode = `#${order.orderNumber || order.orderId.substring(0, 8)}`;
                const createdDate = order.createdAt
                  ? new Date(order.createdAt).toLocaleDateString("vi-VN", {
                      day: "2-digit",
                      month: "short",
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "Vừa xong";

                const paymentMethod = order.paymentMethodName || order.paymentMethod || "COD / Chuyển khoản";
                const customerName =
                  order.buyerInfo?.recipientName ||
                  order.buyerInfo?.buyerName ||
                  (order.items?.[0]?.productName
                    ? `${order.items[0].productName} (${order.totalItemsCount || 1} sp)`
                    : "Khách mua hàng");

                return (
                  <tr
                    key={order.orderId}
                    className="hover:bg-slate-50/70 transition-colors font-medium text-slate-700"
                  >
                    <td className="py-3.5 px-3 font-bold text-[#0b1c30]">
                      <Link
                        href={`/seller/orders/${order.orderId}`}
                        className="hover:text-[#0045cc] hover:underline"
                      >
                        {orderCode}
                      </Link>
                    </td>
                    <td className="py-3.5 px-3">
                      <div className="truncate max-w-[180px] sm:max-w-[220px]" title={customerName}>
                        {customerName}
                      </div>
                    </td>
                    <td className="py-3.5 px-3 text-slate-500 text-[11px]">
                      {createdDate}
                    </td>
                    <td className="py-3.5 px-3 font-bold text-[#0b1c30]">
                      {formatPrice(order.buyerTotalAmount ?? order.totalSellerEarnings ?? 0)}
                    </td>
                    <td className="py-3.5 px-3 text-slate-500">
                      {paymentMethod}
                    </td>
                    <td className="py-3.5 px-3">
                      {getStatusBadge(order.status)}
                    </td>
                    <td className="py-3.5 px-3 text-right">
                      <Link
                        href={`/seller/orders/${order.orderId}`}
                        className="p-1 rounded-lg text-slate-400 hover:text-[#0045cc] hover:bg-blue-50 inline-flex items-center transition-colors"
                        title="Xem chi tiết đơn"
                      >
                        <MoreHorizontal className="w-4 h-4" />
                      </Link>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Footer link */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
        <span className="text-slate-400 text-[11px]">
          Hiển thị 5 đơn hàng mới nhất
        </span>
        <Link
          href="/seller/orders"
          className="font-bold text-[#0045cc] hover:underline inline-flex items-center gap-1"
        >
          <span>Xem tất cả danh sách đơn</span>
          <ExternalLink className="w-3 h-3" />
        </Link>
      </div>
    </div>
  );
}
