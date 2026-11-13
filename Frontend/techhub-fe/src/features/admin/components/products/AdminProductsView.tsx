"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import {
  Search,
  Eye,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Package,
} from "lucide-react";
import { useAdminProductsQuery } from "../../hooks/useAdminModeration";
import { useAdminCategoriesQuery } from "../../hooks/useAdminOperations";
import { ProductModerationDrawer } from "./ProductModerationDrawer";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { formatPrice, getThumbnailUrl } from "@/lib/utils";
import type { ProductApprovalStatus } from "@/types/enums";

type FilterTab = "ALL" | ProductApprovalStatus | "SUSPENDED";

const TABS: { id: FilterTab; label: string }[] = [
  { id: "ALL", label: "Tất cả" },
  { id: "PENDING_APPROVAL", label: "Chờ duyệt" },
  { id: "APPROVED", label: "Đã duyệt" },
  { id: "REJECTED", label: "Từ chối" },
  { id: "DRAFT", label: "Bản nháp" },
  { id: "SUSPENDED", label: "Bị đình chỉ" },
];

export function AdminProductsView() {
  const searchParams = useSearchParams();
  const initialStatus = searchParams.get("approvalStatus") as FilterTab | null;

  const [activeTab, setActiveTab] = useState<FilterTab>(() =>
    initialStatus && TABS.some((t) => t.id === initialStatus)
      ? initialStatus
      : "PENDING_APPROVAL"
  );
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [selectedProductId, setSelectedProductId] = useState<string | null>(
    null
  );
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [keyword, debouncedKeyword]);

  const { data: categoriesData } = useAdminCategoriesQuery({ size: 100 });
  const {
    data,
    isLoading,
    isFetching,
    isPlaceholderData,
    isError,
    refetch,
  } = useAdminProductsQuery({
    approvalStatus:
      activeTab === "ALL" || activeTab === "SUSPENDED" ? "" : activeTab,
    isSuspended: activeTab === "SUSPENDED" ? true : undefined,
    categoryId: categoryId || undefined,
    keyword: debouncedKeyword || undefined,
    page,
    size,
  });

  const handleSuccess = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const products = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div>
        <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
          Kiểm duyệt Sản phẩm Toàn sàn
        </h1>
        <p className="text-[11px] text-slate-500">
          Thẩm định chất lượng, thông số kỹ thuật và phê duyệt sản phẩm từ các Shop lên sàn.
        </p>
      </div>

      {/* Toast Feedback */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col gap-3">
        {/* Tabs & Top Mini-Pagination */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 pb-2 border-b border-slate-100">
          <div
            role="tablist"
            aria-label="Lọc sản phẩm theo trạng thái duyệt"
            className="flex items-center gap-1.5 overflow-x-auto no-scrollbar"
          >
            {TABS.map((t) => (
              <button
                key={t.id}
                type="button"
                role="tab"
                aria-selected={activeTab === t.id}
                onClick={() => {
                  setActiveTab(t.id);
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors duration-150 whitespace-nowrap shrink-0 cursor-pointer min-h-[34px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                  activeTab === t.id
                    ? "bg-on-surface text-white"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          <AdminTablePagination
            variant="top-mini"
            page={page}
            size={size}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={setPage}
            isLoading={isFetching}
            className="self-end sm:self-auto"
          />
        </div>

        {/* Search & Category */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label="Tìm theo tên sản phẩm, thương hiệu hoặc SKU"
              placeholder="Tìm theo tên sản phẩm, thương hiệu, SKU..."
              className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
            />
          </div>

          <select
            value={categoryId}
            aria-label="Lọc theo danh mục sản phẩm"
            onChange={(e) => {
              setCategoryId(e.target.value);
              setPage(0);
            }}
            className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:border-primary sm:w-56"
          >
            <option value="">Tất cả Danh mục</option>
            {(categoriesData?.content || []).map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500">
            Đang tải danh sách sản phẩm...
          </span>
        </div>
      ) : isError ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <p className="text-xs font-bold text-slate-800">
            Lỗi tải danh sách sản phẩm kiểm duyệt
          </p>
          <button
            type="button"
            onClick={() => refetch()}
            className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : products.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center flex flex-col items-center gap-3">
          <Package className="w-10 h-10 text-slate-300" />
          <p className="text-sm font-bold text-slate-800">
            Không có sản phẩm nào phù hợp
          </p>
          <p className="text-xs text-slate-500">
            Thử chuyển trạng thái tab hoặc điều chỉnh từ khóa tìm kiếm.
          </p>
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
                  <th className="py-2.5 px-4 w-[32%]">Sản phẩm</th>
                  <th className="py-2.5 px-3 w-[18%]">Gian hàng</th>
                  <th className="py-2.5 px-3 w-[15%] text-right">Khoảng giá</th>
                  <th className="py-2.5 px-2 w-[9%] text-center">Kho</th>
                  <th className="py-2.5 px-3 w-[14%] text-center">Trạng thái</th>
                  <th className="py-2.5 px-4 w-[12%] text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody
                className={`divide-y divide-slate-100 transition-opacity duration-150 ${
                  isPlaceholderData && isFetching ? "opacity-60" : "opacity-100"
                }`}
              >
                {products.map((p) => (
                  <tr
                    key={p.id}
                    className="hover:bg-slate-50/70 [contain:layout_style]"
                  >
                    <td className="py-2 px-4">
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-9 h-9 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                          {p.coverImageUrl ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                              src={getThumbnailUrl(p.coverImageUrl, 96)}
                              alt={p.name}
                              width={36}
                              height={36}
                              decoding="async"
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <Package className="w-4 h-4 text-slate-500" />
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="font-bold text-slate-900 truncate">
                            {p.name}
                          </p>
                          <p className="text-[10px] text-slate-600 truncate">
                            {p.brandName} • {p.categoryName}
                          </p>
                        </div>
                      </div>
                    </td>

                    <td className="py-2 px-3">
                      <p className="font-semibold text-slate-800 truncate">
                        {p.shopName}
                      </p>
                      <p className="text-[10px] font-mono text-slate-600 truncate">
                        @{p.sellerName}
                      </p>
                    </td>

                    <td className="py-2 px-3 text-right font-mono font-bold text-slate-900">
                      {p.minPrice !== p.maxPrice ? (
                        <div className="flex flex-col items-end">
                          <span>{formatPrice(p.minPrice)}</span>
                          <span className="text-[10px] font-normal text-slate-500">
                            - {formatPrice(p.maxPrice)}
                          </span>
                        </div>
                      ) : (
                        <span>{formatPrice(p.minPrice)}</span>
                      )}
                    </td>

                    <td className="py-2 px-2 text-center font-mono font-semibold text-slate-700">
                      {p.totalStock}
                    </td>

                    <td className="py-2 px-3 text-center">
                      <div className="flex flex-col items-center gap-0.5">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                            p.approvalStatus === "APPROVED"
                              ? "bg-emerald-100 text-emerald-700"
                              : p.approvalStatus === "PENDING_APPROVAL"
                              ? "bg-amber-100 text-amber-800"
                              : p.approvalStatus === "REJECTED"
                              ? "bg-rose-100 text-rose-700"
                              : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {p.approvalStatus === "PENDING_APPROVAL"
                            ? "Chờ duyệt"
                            : p.approvalStatus === "APPROVED"
                            ? "Đã duyệt"
                            : p.approvalStatus === "REJECTED"
                            ? "Từ chối"
                            : "Nháp"}
                        </span>
                        {p.isSuspended && (
                          <span className="px-2 py-0.5 rounded-full text-[9px] font-extrabold bg-rose-600 text-white">
                            ĐÌNH CHỈ
                          </span>
                        )}
                      </div>
                    </td>

                    <td className="py-2 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => setSelectedProductId(p.id)}
                        className="px-2.5 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-bold transition-colors duration-150 inline-flex items-center gap-1 cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Kiểm tra</span>
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

      <ProductModerationDrawer
        productId={selectedProductId}
        onClose={() => setSelectedProductId(null)}
        onActionSuccess={handleSuccess}
      />
    </div>
  );
}
