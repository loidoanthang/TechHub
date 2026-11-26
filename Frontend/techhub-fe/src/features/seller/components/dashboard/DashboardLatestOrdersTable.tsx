"use client";

import React from "react";
import Link from "next/link";
import { formatPrice } from "@/lib/utils";
import { useSellerOrdersQuery } from "../../hooks/useSellerOrders";
import {
  MoreHorizontal,
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
          Hoàn thành
        </span>
      );
    case "CONFIRMED":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
          Đã xác nhận
        </span>
      );
    case "SHIPPING":
    case "WAITING_FOR_PICKUP":
    case "PREPARING":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-[#0045cc] border border-blue-200">
          Đang xử lý
        </span>
      );
    case "PENDING_CONFIRMATION":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
          Chờ xác nhận
        </span>
      );
    case "CANCELLED":
    case "REFUNDED":
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-red-50 text-red-600 border border-red-200">
          Đã hủy
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

  function formatDisplayPaymentMethod(rawMethod?: string | null): string {
    if (!rawMethod) return "COD";
    if (rawMethod.includes("VNPAY") || rawMethod.toLowerCase().includes("vnpay")) {
      return "Chuyển Khoản";
    }
    if (rawMethod.includes("COD") || rawMethod.toLowerCase().includes("cod")) {
      return "COD";
    }
    return rawMethod;
  }

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#e2e8f0] shadow-2xs flex flex-col gap-4">
      {/* Table Header */}
      <div className="border-b border-slate-100 pb-3">
        <h3 className="font-heading text-base font-bold text-[#0b1c30]">
          Đơn Hàng Mới Nhất
        </h3>
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto -mx-5 sm:mx-0">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-100 text-slate-400 font-semibold text-[11px] tracking-wider uppercase">
              <th className="py-2.5 px-3">Mã đơn</th>
              <th className="py-2.5 px-3">Khách hàng</th>
              <th className="py-2.5 px-3">Ngày đặt</th>
              <th className="py-2.5 px-3 text-right">Tổng tiền</th>
              <th className="py-2.5 px-3">Thanh toán</th>
              <th className="py-2.5 px-3">Trạng thái</th>
              <th className="py-2.5 px-3 text-right">Thao tác</th>
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

                const paymentMethod = formatDisplayPaymentMethod(
                  order.paymentMethodName || order.paymentMethod
                );
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
                    <td className="py-3.5 px-3 font-bold text-[#0b1c30] text-right">
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
