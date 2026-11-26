"use client";

import React, { useState, useEffect } from "react";
import Image from "next/image";
import {
  Search,
  Star,
  ShieldAlert,
  ShieldCheck,
  Store,
  Eye,
  Trash2,
  Image as ImageIcon,
  Loader2,
  Filter,
  RotateCcw,
  MessageSquareText,
  AlertTriangle,
  CheckCircle2,
} from "lucide-react";
import {
  useAdminReviewsQuery,
  useAdminReviewStatsQuery,
} from "../../hooks/useAdminModeration";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { AdminReviewMobileCard } from "./AdminReviewMobileCard";
import { ReviewDetailDrawer } from "./ReviewDetailDrawer";
import { ReviewDeleteConfirmModal } from "./ReviewDeleteConfirmModal";
import { SelectDropdown } from "@/components/shared/SelectDropdown";
import type { AdminReviewResponse } from "@/types/review";
import { formatDate } from "@/lib/utils";

export function AdminReviewsView() {
  // Query parameters state
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [selectedRating, setSelectedRating] = useState<number | null>(null);
  const [isReportedFilter, setIsReportedFilter] = useState<boolean | null>(null);
  const [hasMediaFilter, setHasMediaFilter] = useState<boolean | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  // Active selection state for Drawers & Modals
  const [detailReview, setDetailReview] = useState<AdminReviewResponse | null>(null);
  const [reviewToDelete, setReviewToDelete] = useState<AdminReviewResponse | null>(null);

  // Debounce search input
  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [keyword, debouncedKeyword]);

  // Fetch reviews & stats
  const {
    data: reviewsPage,
    isLoading,
    isFetching,
    refetch,
  } = useAdminReviewsQuery({
    keyword: debouncedKeyword || undefined,
    rating: selectedRating !== null ? selectedRating : undefined,
    isReported: isReportedFilter !== null ? isReportedFilter : undefined,
    hasMedia: hasMediaFilter !== null ? hasMediaFilter : undefined,
    page,
    size,
    sortBy: "createdAt",
    sortDirection: "DESC",
  });

  const { data: stats } = useAdminReviewStatsQuery();

  const handleResetFilters = () => {
    setKeyword("");
    setDebouncedKeyword("");
    setSelectedRating(null);
    setIsReportedFilter(null);
    setHasMediaFilter(null);
    setPage(0);
  };

  const hasActiveFilters =
    Boolean(debouncedKeyword) ||
    selectedRating !== null ||
    isReportedFilter !== null ||
    hasMediaFilter !== null;

  return (
    <div className="space-y-6 p-8">
      {/* 1. Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2.5">
            Kiểm duyệt Đánh giá Sàn
          </h1>
        </div>
      </div>

      {/* 2. Quick Stat KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Card 1: Tổng đánh giá */}
        <div className="p-4 bg-white rounded-2xl border border-slate-200/80 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-xl shrink-0">
            <MessageSquareText className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500">Tổng đánh giá</p>
            <p className="text-xl font-black text-slate-900">
              {stats?.totalReviews ?? reviewsPage?.totalElements ?? 0}
            </p>
          </div>
        </div>

        {/* Card 2: Bị báo cáo vi phạm */}
        <div
          onClick={() => {
            setIsReportedFilter(isReportedFilter === true ? null : true);
            setPage(0);
          }}
          className={`p-4 rounded-2xl border transition-all cursor-pointer shadow-xs flex items-center gap-3.5 ${
            isReportedFilter === true
              ? "bg-rose-50 border-rose-300 ring-2 ring-rose-500/20"
              : "bg-white border-slate-200/80 hover:border-rose-200 hover:bg-rose-50/30"
          }`}
        >
          <div className="p-3 bg-rose-50 text-rose-600 rounded-xl shrink-0">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500">Bị báo cáo vi phạm</p>
            <p className="text-xl font-black text-rose-600">
              {stats?.reportedReviews ?? 0}
            </p>
          </div>
        </div>

        {/* Card 3: Đánh giá tiêu cực 1-2 sao */}
        <div
          onClick={() => {
            setSelectedRating(selectedRating === 1 ? null : 1);
            setPage(0);
          }}
          className={`p-4 rounded-2xl border transition-all cursor-pointer shadow-xs flex items-center gap-3.5 ${
            selectedRating === 1
              ? "bg-amber-50 border-amber-300 ring-2 ring-amber-500/20"
              : "bg-white border-slate-200/80 hover:border-amber-200 hover:bg-amber-50/30"
          }`}
        >
          <div className="p-3 bg-amber-50 text-amber-600 rounded-xl shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500">Đánh giá tiêu cực (1★)</p>
            <p className="text-xl font-black text-amber-600">
              {stats?.negativeReviews ?? 0}
            </p>
          </div>
        </div>

        {/* Card 4: Điểm trung bình */}
        <div className="p-4 bg-white rounded-2xl border border-slate-200/80 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-amber-50 text-amber-500 rounded-xl shrink-0">
            <Star className="w-5 h-5 fill-amber-400" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500">Điểm TB toàn sàn</p>
            <div className="flex items-baseline gap-1">
              <span className="text-xl font-black text-slate-900">
                {stats?.averageRating ?? 5.0}
              </span>
              <span className="text-xs text-slate-400 font-medium">/ 5.0</span>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Filter & Search Bar */}
      <div className="p-4 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-3.5">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          {/* Keyword Search */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm kiếm theo nội dung, tên người mua, tên shop, mã đơn..."
              className="w-full text-xs sm:text-sm pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
            />
          </div>

          {/* Rating filter dropdown */}
          <div className="w-full sm:w-44">
            <SelectDropdown
              value={selectedRating !== null ? String(selectedRating) : ""}
              ariaLabel="Lọc theo số sao đánh giá"
              onChange={(val) => {
                setSelectedRating(val ? Number(val) : null);
                setPage(0);
              }}
              buttonClassName="h-10 px-3.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              options={[
                { value: "", label: "Tất cả số sao" },
                { value: "5", label: "5 sao (Tuyệt vời)" },
                { value: "4", label: "4 sao (Hài lòng)" },
                { value: "3", label: "3 sao (Bình thường)" },
                { value: "2", label: "2 sao (Không tốt)" },
                { value: "1", label: "1 sao (Rất tệ)" },
              ]}
            />
          </div>

          {/* Reset Filters button */}
          {hasActiveFilters && (
            <button
              type="button"
              onClick={handleResetFilters}
              className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors cursor-pointer shrink-0"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Xóa bộ lọc</span>
            </button>
          )}
        </div>

        {/* Quick filter badges */}
        <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-slate-100 text-xs">
          <span className="text-slate-400 font-semibold text-[11px] uppercase tracking-wider mr-1">
            Lọc nhanh:
          </span>

          <button
            type="button"
            onClick={() => {
              setIsReportedFilter(isReportedFilter === true ? null : true);
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-lg font-medium transition-colors cursor-pointer flex items-center gap-1.5 ${
              isReportedFilter === true
                ? "bg-rose-600 text-white shadow-xs"
                : "bg-slate-100 text-slate-700 hover:bg-slate-200"
            }`}
          >
            <ShieldAlert className="w-3.5 h-3.5" />
            <span>Có cờ vi phạm</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setHasMediaFilter(hasMediaFilter === true ? null : true);
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-lg font-medium transition-colors cursor-pointer flex items-center gap-1.5 ${
              hasMediaFilter === true
                ? "bg-primary text-white shadow-xs"
                : "bg-slate-100 text-slate-700 hover:bg-slate-200"
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>Có hình ảnh đính kèm</span>
          </button>
        </div>
      </div>

      {/* 4. Main Content: Table & Mobile Cards */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden">
        {/* Loading Spinner */}
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-400 space-y-3">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <p className="text-sm">Đang tải danh sách đánh giá...</p>
          </div>
        ) : !reviewsPage || reviewsPage.content.length === 0 ? (
          /* Empty state */
          <div className="py-16 text-center space-y-3">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400">
              <MessageSquareText className="w-7 h-7" />
            </div>
            <h3 className="text-base font-bold text-slate-800">
              Không tìm thấy đánh giá nào
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Không có đánh giá nào phù hợp với điều kiện tìm kiếm hoặc bộ lọc hiện tại.
            </p>
            {hasActiveFilters && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="mt-2 px-4 py-2 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary/20 rounded-xl transition-colors cursor-pointer"
              >
                Đặt lại tất cả bộ lọc
              </button>
            )}
          </div>
        ) : (
          <>
            {/* Desktop Table View (≥ md) */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/75 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                    <th className="py-3.5 px-4">Người mua</th>
                    <th className="py-3.5 px-4">Sản phẩm & Shop</th>
                    <th className="py-3.5 px-4">Đánh giá & Nội dung</th>
                    <th className="py-3.5 px-4">Trạng thái</th>
                    <th className="py-3.5 px-4">Thời gian</th>
                    <th className="py-3.5 px-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {reviewsPage.content.map((review) => (
                    <tr
                      key={review.id}
                      className="hover:bg-slate-50/80 transition-colors group"
                    >
                      {/* 1. Người mua */}
                      <td className="py-3.5 px-4 align-top">
                        <div className="flex items-center gap-2.5">
                          {review.reviewerAvatar ? (
                            <div className="relative w-8 h-8 rounded-full overflow-hidden border border-slate-200 shrink-0">
                              <Image
                                src={review.reviewerAvatar}
                                alt={review.reviewerName}
                                fill
                                className="object-cover"
                                sizes="32px"
                              />
                            </div>
                          ) : (
                            <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs shrink-0">
                              {review.reviewerName.charAt(0).toUpperCase()}
                            </div>
                          )}
                          <div className="min-w-0">
                            <p className="text-xs font-bold text-slate-900 truncate">
                              {review.reviewerName}
                            </p>
                            {review.orderNumber && (
                              <p className="text-[10px] text-slate-400 font-mono truncate">
                                {review.orderNumber}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* 2. Sản phẩm & Shop */}
                      <td className="py-3.5 px-4 align-top max-w-[240px]">
                        <div className="flex items-start gap-2.5">
                          {review.imageUrl && (
                            <div className="relative w-9 h-9 rounded-lg overflow-hidden bg-slate-100 border border-slate-200 shrink-0">
                              <Image
                                src={review.imageUrl}
                                alt={review.productName}
                                fill
                                className="object-cover"
                                sizes="36px"
                              />
                            </div>
                          )}
                          <div className="min-w-0">
                            <p className="text-xs font-semibold text-slate-900 truncate" title={review.productName}>
                              {review.productName}
                            </p>
                            {review.variantName && (
                              <p className="text-[10px] text-slate-400 truncate">
                                {review.variantName}
                              </p>
                            )}
                            {review.shopName && (
                              <p className="text-[11px] text-primary flex items-center gap-1 font-medium truncate mt-0.5">
                                <Store className="w-3 h-3 shrink-0" />
                                <span className="truncate">{review.shopName}</span>
                              </p>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* 3. Đánh giá & Nội dung */}
                      <td className="py-3.5 px-4 align-top max-w-[320px]">
                        <div className="space-y-1">
                          <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <Star
                                key={star}
                                className={`w-3.5 h-3.5 ${
                                  star <= review.rating
                                    ? "fill-amber-400 text-amber-400"
                                    : "fill-slate-100 text-slate-200"
                                }`}
                              />
                            ))}
                            {review.mediaList && review.mediaList.length > 0 && (
                              <span className="inline-flex items-center gap-0.5 text-[10px] font-medium text-slate-500 bg-slate-100 px-1.5 py-0.2 rounded ml-1">
                                <ImageIcon className="w-3 h-3 text-slate-400" />
                                {review.mediaList.length}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-700 line-clamp-2" title={review.comment || ""}>
                            &ldquo;{review.comment || "Không có bình luận chữ"}&rdquo;
                          </p>
                        </div>
                      </td>

                      {/* 4. Trạng thái */}
                      <td className="py-3.5 px-4 align-top whitespace-nowrap">
                        {review.isReported ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-700">
                            Bị báo cáo ({review.reportCount || 1})
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700">
                            Bình thường
                          </span>
                        )}
                      </td>

                      {/* 5. Thời gian */}
                      <td className="py-3.5 px-4 align-top whitespace-nowrap text-xs text-slate-500">
                        {formatDate(review.createdAt)}
                      </td>

                      {/* 6. Thao tác */}
                      <td className="py-3.5 px-4 align-top text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => setDetailReview(review)}
                            className="p-1.5 text-slate-500 hover:text-slate-900 hover:bg-slate-200/70 rounded-lg transition-colors cursor-pointer"
                            title="Xem chi tiết"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setReviewToDelete(review)}
                            className="p-1.5 text-rose-500 hover:text-rose-700 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                            title="Gỡ bỏ vi phạm"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards View (< md) */}
            <div className="md:hidden divide-y divide-slate-100 p-3 space-y-3">
              {reviewsPage.content.map((review) => (
                <AdminReviewMobileCard
                  key={review.id}
                  review={review}
                  onViewDetail={(r) => setDetailReview(r)}
                  onDelete={(r) => setReviewToDelete(r)}
                />
              ))}
            </div>

            {/* Pagination footer */}
            <div className="p-4 border-t border-slate-100 flex items-center justify-between">
              <AdminTablePagination
                page={reviewsPage.pageNumber}
                totalPages={reviewsPage.totalPages}
                size={reviewsPage.pageSize}
                totalElements={reviewsPage.totalElements}
                onPageChange={(nextPage) => setPage(nextPage)}
                onSizeChange={(nextSize: number) => {
                  setSize(nextSize);
                  setPage(0);
                }}
              />
            </div>
          </>
        )}
      </div>

      {/* 5. Modals & Drawers */}
      <ReviewDetailDrawer
        review={detailReview}
        isOpen={Boolean(detailReview)}
        onClose={() => setDetailReview(null)}
        onDeleteClick={(r) => {
          setDetailReview(null);
          setReviewToDelete(r);
        }}
      />

      <ReviewDeleteConfirmModal
        review={reviewToDelete}
        isOpen={Boolean(reviewToDelete)}
        onClose={() => setReviewToDelete(null)}
        onSuccess={() => {
          refetch();
        }}
      />
    </div>
  );
}
