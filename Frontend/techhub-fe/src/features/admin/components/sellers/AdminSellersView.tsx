"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import {
  Store,
  Search,
  Eye,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  UserCheck,
} from "lucide-react";
import {
  useAdminSellersQuery,
  useAdminShopsQuery,
} from "../../hooks/useAdminModeration";
import { SellerDetailDrawer } from "./SellerDetailDrawer";
import { ShopDetailDrawer } from "./ShopDetailDrawer";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { formatPrice } from "@/lib/utils";
import type { SellerVerificationStatus, ShopStatus } from "@/types/enums";

type MainTab = "APPLICATIONS" | "SHOPS";

export function AdminSellersView() {
  const searchParams = useSearchParams();
  const initialSellerStatus = searchParams.get("status") as
    | SellerVerificationStatus
    | null;

  const [mainTab, setMainTab] = useState<MainTab>("APPLICATIONS");

  // Applications state
  const [sellerStatusFilter, setSellerStatusFilter] = useState<
    SellerVerificationStatus | ""
  >(() =>
    initialSellerStatus &&
    ["PENDING", "APPROVED", "REJECTED"].includes(initialSellerStatus)
      ? initialSellerStatus
      : "PENDING"
  );
  // Shops state
  const [shopStatusFilter, setShopStatusFilter] = useState<ShopStatus | "">("");

  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);

  const [selectedSellerId, setSelectedSellerId] = useState<string | null>(null);
  const [selectedShopId, setSelectedShopId] = useState<string | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [keyword, debouncedKeyword]);

  const sellersQuery = useAdminSellersQuery(
    {
      status: sellerStatusFilter,
      keyword: debouncedKeyword || undefined,
      page,
      size,
    },
    mainTab === "APPLICATIONS"
  );

  const shopsQuery = useAdminShopsQuery(
    {
      status: shopStatusFilter,
      keyword: debouncedKeyword || undefined,
      page,
      size,
    },
    mainTab === "SHOPS"
  );

  const handleSuccess = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const activeQuery = mainTab === "APPLICATIONS" ? sellersQuery : shopsQuery;
  const totalPages = activeQuery.data?.totalPages || 1;
  const totalElements = activeQuery.data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div>
        <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
          Đối tác Seller & Quản lý Gian hàng
        </h1>
        <p className="text-[11px] text-slate-500">
          Thẩm định hồ sơ đăng ký mở Shop và giám sát trạng thái vận hành các gian hàng trên sàn.
        </p>
      </div>

      {/* Toast */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Main Switcher & Filters */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col gap-3">
        {/* Top-level Tabs & Mini-Pagination */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 border-b border-slate-100 pb-2.5">
          <div
            role="tablist"
            aria-label="Quản lý đối tác Seller và Gian hàng"
            className="flex items-center gap-2 overflow-x-auto no-scrollbar"
          >
            <button
              type="button"
              role="tab"
              aria-selected={mainTab === "APPLICATIONS"}
              onClick={() => {
                setMainTab("APPLICATIONS");
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-colors duration-150 whitespace-nowrap cursor-pointer min-h-[36px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                mainTab === "APPLICATIONS"
                  ? "bg-on-surface text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              <UserCheck className="w-4 h-4" />
              <span>Hồ sơ Đăng ký Seller</span>
            </button>

            <button
              type="button"
              role="tab"
              aria-selected={mainTab === "SHOPS"}
              onClick={() => {
                setMainTab("SHOPS");
                setPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-colors duration-150 whitespace-nowrap cursor-pointer min-h-[36px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                mainTab === "SHOPS"
                  ? "bg-on-surface text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              <Store className="w-4 h-4" />
              <span>Danh sách Gian hàng (Shops)</span>
            </button>
          </div>

          <AdminTablePagination
            variant="top-mini"
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={setPage}
            isLoading={activeQuery.isFetching}
            className="self-end sm:self-auto"
          />
        </div>

        {/* Sub Filters & Search */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label={
                mainTab === "APPLICATIONS"
                  ? "Tìm hồ sơ theo tên Shop, slug, email hoặc số điện thoại"
                  : "Tìm gian hàng theo tên gian hàng, chủ shop hoặc email"
              }
              placeholder={
                mainTab === "APPLICATIONS"
                  ? "Tìm theo tên Shop, slug, email, số điện thoại..."
                  : "Tìm theo tên gian hàng, chủ shop, email..."
              }
              className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
            />
          </div>

          {mainTab === "APPLICATIONS" ? (
            <select
              value={sellerStatusFilter}
              aria-label="Lọc hồ sơ theo trạng thái duyệt"
              onChange={(e) => {
                setSellerStatusFilter(
                  e.target.value as SellerVerificationStatus | ""
                );
                setPage(0);
              }}
              className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-52"
            >
              <option value="">Tất cả trạng thái duyệt</option>
              <option value="PENDING">Chờ duyệt (PENDING)</option>
              <option value="APPROVED">Đã duyệt (APPROVED)</option>
              <option value="REJECTED">Từ chối (REJECTED)</option>
            </select>
          ) : (
            <select
              value={shopStatusFilter}
              aria-label="Lọc gian hàng theo trạng thái"
              onChange={(e) => {
                setShopStatusFilter(e.target.value as ShopStatus | "");
                setPage(0);
              }}
              className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-52"
            >
              <option value="">Tất cả trạng thái Shop</option>
              <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
              <option value="PAUSED">Tạm nghỉ (PAUSED)</option>
              <option value="SUSPENDED">Đình chỉ (SUSPENDED)</option>
              <option value="BANNED">Cấm vĩnh viễn (BANNED)</option>
            </select>
          )}
        </div>
      </div>

      {/* Data Table */}
      {activeQuery.isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500">Đang tải dữ liệu...</span>
        </div>
      ) : activeQuery.isError ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <p className="text-xs font-bold text-slate-800">
            Lỗi tải dữ liệu đối tác / gian hàng
          </p>
          <button
            type="button"
            onClick={() => activeQuery.refetch()}
            className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : mainTab === "APPLICATIONS" ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {(sellersQuery.data?.content || []).length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-500">
              Không có hồ sơ đăng ký Seller nào phù hợp bộ lọc.
            </div>
          ) : (
            <div className="overflow-x-auto no-scrollbar">
              <table className="w-full min-w-[780px] text-left text-xs border-collapse table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                  <tr>
                    <th className="py-2.5 px-4 w-[24%]">Gian hàng / Slug</th>
                    <th className="py-2.5 px-3 w-[24%]">Chủ sở hữu</th>
                    <th className="py-2.5 px-3 w-[22%]">Ngân hàng</th>
                    <th className="py-2.5 px-3 w-[15%] text-center">Trạng thái</th>
                    <th className="py-2.5 px-4 w-[15%] text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(sellersQuery.data?.content || []).map((s) => (
                    <tr key={s.id} className="hover:bg-slate-50/70">
                      <td className="py-2 px-4">
                        <p className="font-bold text-slate-900 truncate">
                          {s.shopName || "—"}
                        </p>
                        <p className="text-[10px] font-mono text-[#0045cc] truncate">
                          @{s.sellerName}
                        </p>
                      </td>
                      <td className="py-2 px-3">
                        <p className="font-semibold text-slate-800 truncate">
                          {s.ownerName}
                        </p>
                        <p className="text-[10px] text-slate-500 truncate">
                          {s.ownerEmail}
                        </p>
                      </td>
                      <td className="py-2 px-3">
                        <p className="font-semibold text-slate-800 truncate">
                          {s.bankName}
                        </p>
                        <p className="text-[10px] font-mono text-slate-500 truncate">
                          {s.bankAccountNumber}
                        </p>
                      </td>
                      <td className="py-2 px-3 text-center">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                            s.verificationStatus === "APPROVED"
                              ? "bg-emerald-100 text-emerald-700"
                              : s.verificationStatus === "PENDING"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-rose-100 text-rose-700"
                          }`}
                        >
                          {s.verificationStatus}
                        </span>
                      </td>
                      <td className="py-2 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => setSelectedSellerId(s.id)}
                          className="px-2.5 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-bold transition-colors duration-150 inline-flex items-center gap-1 cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Thẩm định</span>
                        </button>
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
            isLoading={activeQuery.isFetching}
          />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          {(shopsQuery.data?.content || []).length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-500">
              Không tìm thấy gian hàng nào phù hợp.
            </div>
          ) : (
            <div className="overflow-x-auto no-scrollbar">
              <table className="w-full min-w-[800px] text-left text-xs border-collapse table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                  <tr>
                    <th className="py-2.5 px-4 w-[28%]">Gian hàng</th>
                    <th className="py-2.5 px-3 w-[22%]">Chủ sở hữu</th>
                    <th className="py-2.5 px-3 w-[10%] text-center">SP</th>
                    <th className="py-2.5 px-3 w-[16%] text-right">Số dư Ví</th>
                    <th className="py-2.5 px-3 w-[12%] text-center">Trạng thái</th>
                    <th className="py-2.5 px-4 w-[12%] text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(shopsQuery.data?.content || []).map((shop) => (
                    <tr key={shop.id} className="hover:bg-slate-50/70">
                      <td className="py-2 px-4">
                        <p className="font-bold text-slate-900 truncate">
                          {shop.name}
                        </p>
                        <p className="text-[10px] font-mono text-[#0045cc] truncate">
                          @{shop.sellerName}
                        </p>
                      </td>
                      <td className="py-2 px-3">
                        <p className="font-semibold text-slate-800 truncate">
                          {shop.ownerName}
                        </p>
                        <p className="text-[10px] text-slate-500 truncate">
                          {shop.ownerEmail}
                        </p>
                      </td>
                      <td className="py-2 px-3 text-center font-mono font-bold text-slate-700">
                        {shop.productCount}
                      </td>
                      <td className="py-2 px-3 text-right font-mono font-bold text-slate-900">
                        {formatPrice(shop.balance || 0)}
                      </td>
                      <td className="py-2 px-3 text-center">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                            shop.status === "ACTIVE"
                              ? "bg-emerald-100 text-emerald-700"
                              : shop.status === "PAUSED"
                              ? "bg-amber-100 text-amber-800"
                              : shop.status === "SUSPENDED"
                              ? "bg-orange-100 text-orange-800"
                              : "bg-rose-600 text-white"
                          }`}
                        >
                          {shop.status}
                        </span>
                      </td>
                      <td className="py-2 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => setSelectedShopId(shop.id)}
                          className="px-2.5 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-bold transition-colors duration-150 inline-flex items-center gap-1 cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Quản lý</span>
                        </button>
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
            isLoading={activeQuery.isFetching}
          />
        </div>
      )}

      <SellerDetailDrawer
        sellerId={selectedSellerId}
        onClose={() => setSelectedSellerId(null)}
        onActionSuccess={handleSuccess}
      />

      <ShopDetailDrawer
        shopId={selectedShopId}
        onClose={() => setSelectedShopId(null)}
        onActionSuccess={handleSuccess}
      />
    </div>
  );
}
