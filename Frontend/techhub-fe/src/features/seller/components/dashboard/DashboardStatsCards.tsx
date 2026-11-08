"use client";

import React from "react";
import { DollarSign, ShoppingCart, Boxes, TrendingUp, CheckCircle2 } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { SellerDashboardSummaryDto } from "@/types/sellerDashboard";

interface DashboardStatsCardsProps {
  summary?: SellerDashboardSummaryDto;
  isLoading?: boolean;
}

export function DashboardStatsCards({ summary, isLoading }: DashboardStatsCardsProps) {
  const netRevenue = summary?.netRevenue ?? 0;
  const totalOrders = summary?.totalOrders ?? 0;
  const completedOrders = summary?.completedOrders ?? 0;
  const totalUnitsSold = summary?.totalUnitsSold ?? 0;
  const averageOrderValue = summary?.averageOrderValue ?? 0;

  const cards = [
    {
      title: "Doanh thu thuần",
      value: formatPrice(netRevenue),
      subtitle: "Doanh số thực nhận sau chiết khấu",
      icon: DollarSign,
      iconBg: "bg-[#0045cc]/10 text-[#0045cc]",
      accent: "border-[#0045cc]/20",
    },
    {
      title: "Tổng số đơn hàng",
      value: `${totalOrders.toLocaleString("vi-VN")} đơn`,
      subtitle: `${completedOrders.toLocaleString("vi-VN")} đơn đã hoàn thành`,
      icon: ShoppingCart,
      iconBg: "bg-emerald-50 text-emerald-600",
      accent: "border-emerald-200/60",
    },
    {
      title: "Sản phẩm đã bán",
      value: `${totalUnitsSold.toLocaleString("vi-VN")} chiếc`,
      subtitle: "Tổng linh kiện & thiết bị xuất kho",
      icon: Boxes,
      iconBg: "bg-indigo-50 text-indigo-600",
      accent: "border-indigo-200/60",
    },
    {
      title: "Giá trị đơn trung bình (AOV)",
      value: formatPrice(averageOrderValue),
      subtitle: "Doanh thu trung bình trên mỗi đơn",
      icon: TrendingUp,
      iconBg: "bg-amber-50 text-amber-600",
      accent: "border-amber-200/60",
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <div
            key={card.title}
            className={`bg-white rounded-3xl p-5 sm:p-6 border ${card.accent} shadow-2xs flex flex-col justify-between gap-4 transition-all hover:shadow-xs`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#737688]">
                {card.title}
              </span>
              <div
                className={`w-9 h-9 rounded-2xl ${card.iconBg} flex items-center justify-center font-bold`}
              >
                <Icon className="w-4 h-4" />
              </div>
            </div>

            <div className="flex flex-col gap-1">
              {isLoading ? (
                <div className="h-8 w-32 bg-slate-200 animate-pulse rounded-md" />
              ) : (
                <span className="font-heading text-2xl font-extrabold text-[#0b1c30] tracking-tight">
                  {card.value}
                </span>
              )}
              <span className="text-[11px] text-[#737688] font-medium">
                {card.subtitle}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
