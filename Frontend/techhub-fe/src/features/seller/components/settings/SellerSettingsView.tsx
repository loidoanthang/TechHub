"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Store,
  MapPin,
  Truck,
  Landmark,
  ChevronRight,
  Settings,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";
import { ShopProfileTab } from "./ShopProfileTab";
import { WarehouseAddressTab } from "./WarehouseAddressTab";
import { ShippingOptionsTab } from "./ShippingOptionsTab";
import { PayoutProfileTab } from "./PayoutProfileTab";

type SettingsTab = "profile" | "warehouse" | "shipping" | "payout";

interface TabItem {
  id: SettingsTab;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  description: string;
}

const TABS: TabItem[] = [
  {
    id: "profile",
    label: "Hồ sơ & Nhận diện",
    icon: Store,
    description: "Tên shop, bio, ảnh đại diện, ảnh bìa",
  },
  {
    id: "warehouse",
    label: "Kho hàng Lấy hàng",
    icon: MapPin,
    description: "Địa chỉ để bưu tá đến nhận kiện hàng",
  },
  {
    id: "shipping",
    label: "Cấu hình Vận chuyển",
    icon: Truck,
    description: "Mức cước tiêu chuẩn áp dụng cho đơn hàng",
  },
  {
    id: "payout",
    label: "Tài khoản Ngân hàng",
    icon: Landmark,
    description: "Định danh và nhận tiền quyết toán doanh thu",
  },
];

export function SellerSettingsView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const currentTabParam = searchParams.get("tab") as SettingsTab | null;

  const [activeTab, setActiveTab] = useState<SettingsTab>(() => {
    if (
      currentTabParam &&
      ["profile", "warehouse", "shipping", "payout"].includes(currentTabParam)
    ) {
      return currentTabParam;
    }
    return "profile";
  });

  const handleSelectTab = (tabId: SettingsTab) => {
    setActiveTab(tabId);
    router.replace(`/seller/settings?tab=${tabId}`, { scroll: false });
  };

  return (
    <div className="flex-1 w-full p-4 sm:p-6 lg:p-8 flex flex-col gap-6 max-w-[1400px] mx-auto animate-in fade-in duration-300">
      {/* 1. BREADCRUMBS & HEADER */}
      <div className="flex flex-col gap-2">
        <nav className="flex items-center gap-1.5 text-xs text-slate-500">
          <Link
            href={ROUTES.SELLER.DASHBOARD}
            className="hover:text-[#0045cc] transition-colors"
          >
            Kênh Người Bán
          </Link>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <span className="font-semibold text-slate-800">
            Kho hàng & Cài đặt
          </span>
        </nav>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-1">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#eff4ff] text-[#0045cc] flex items-center justify-center shadow-2xs">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h1 className="font-heading text-lg sm:text-xl font-extrabold text-[#0b1c30]">
                Kho hàng & Cài đặt Gian hàng
              </h1>
              <p className="text-xs text-slate-500">
                Quản lý hồ sơ nhận diện, kho vận chuyển, biểu phí giao hàng và tài khoản đối soát thanh toán.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 2. HORIZONTAL TAB SWITCHER */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-1.5 sm:p-2">
        <div className="flex items-center gap-1 sm:gap-2 overflow-x-auto no-scrollbar">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => handleSelectTab(tab.id)}
                className={`flex items-center gap-2 px-3 sm:px-4 py-2.5 rounded-xl text-xs font-bold transition-all shrink-0 whitespace-nowrap ${
                  isActive
                    ? "bg-[#0045cc] text-white shadow-xs"
                    : "text-slate-600 hover:text-[#0045cc] hover:bg-[#eff4ff]"
                }`}
              >
                <Icon
                  className={`w-4 h-4 shrink-0 ${
                    isActive ? "text-white" : "text-slate-500"
                  }`}
                />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. TAB CONTENT */}
      <div className="w-full">
        {activeTab === "profile" && <ShopProfileTab />}
        {activeTab === "warehouse" && <WarehouseAddressTab />}
        {activeTab === "shipping" && <ShippingOptionsTab />}
        {activeTab === "payout" && <PayoutProfileTab />}
      </div>
    </div>
  );
}
