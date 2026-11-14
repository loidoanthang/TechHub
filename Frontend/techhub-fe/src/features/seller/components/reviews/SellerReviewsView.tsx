"use client";

import React, { useState, useEffect } from "react";
import Image from "next/image";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Star,
  Search,
  MessageSquare,
  MessageSquareQuote,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Loader2,
  ImageIcon,
  X,
  Send,
  Eye,
  Store,
  Calendar,
} from "lucide-react";
import {
  useSellerReviewsQuery,
  useRespondToReviewMutation,
} from "@/features/seller/api/sellerReviewApi";
import type { SellerReviewResponse } from "@/types/review";
import type { SellerResponseFormValues } from "@/features/seller/schemas/sellerReviewSchema";
import { sellerResponseSchema } from "@/features/seller/schemas/sellerReviewSchema";
import { Pagination } from "@/components/shared/Pagination";
import { ProductThumbnail } from "@/components/shared";
import { UserAvatar } from "@/components/shared";

export function SellerReviewsView() {
  const [ratingFilter, setRatingFilter] = useState<number | "">("");
  const [hasRespondedFilter, setHasRespondedFilter] = useState<"" | "true" | "false">("");
  const [hasMediaFilter, setHasMediaFilter] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);

  const [respondingReview, setRespondingReview] =
    useState<SellerReviewResponse | null>(null);
  const [previewImageUrl, setPreviewImageUrl] = useState<string | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const t = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [keyword, debouncedKeyword]);

  const { data, isLoading, isFetching, refetch } = useSellerReviewsQuery({
    rating: ratingFilter === "" ? undefined : ratingFilter,
    hasResponded:
      hasRespondedFilter === ""
        ? undefined
        : hasRespondedFilter === "true",
    hasMedia: hasMediaFilter ? true : undefined,
    keyword: debouncedKeyword || undefined,
    page,
    size,
    sortBy: "createdAt",
    sortDirection: "DESC",
  });

  const respondMutation = useRespondToReviewMutation();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SellerResponseFormValues>({
    resolver: zodResolver(sellerResponseSchema),
    defaultValues: { response: "" },
  });

  const showToast = (msg: string) => {
    setErrorMsg(null);
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const handleOpenRespondModal = (rev: SellerReviewResponse) => {
    setRespondingReview(rev);
    reset({ response: "" });
    setErrorMsg(null);
  };

  const handleRespondSubmit = async (values: SellerResponseFormValues) => {
    if (!respondingReview) return;
    setErrorMsg(null);

    try {
      await respondMutation.mutateAsync({
        reviewId: respondingReview.id,
        data: { response: values.response.trim() },
      });

      showToast(`Đã gửi phản hồi thành công cho khách hàng "${respondingReview.reviewerName}".`);
      setRespondingReview(null);
      reset();
      refetch();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể gửi phản hồi. Vui lòng thử lại.";
      setErrorMsg(msg);
    }
  };

  const reviews = data?.data?.content || [];
  const totalPages = data?.data?.totalPages || 1;
  const totalElements = data?.data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* 1. Header */}
      <div>
        <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
          Quản lý Đánh giá & Phản hồi Khách hàng
        </h1>
        <p className="text-[11px] text-slate-500">
          Theo dõi nhận xét thực tế từ người mua đã nhận hàng và gửi phản hồi chăm sóc khách hàng
        </p>
      </div>

      {/* Feedbacks */}
      {toastMsg && (
        <div
          role="status"
          className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2 animate-in fade-in"
        >
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}
      {errorMsg && (
        <div
          role="alert"
          className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center gap-2 animate-in fade-in"
        >
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* 2. Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col gap-3">
        {/* Rating Tabs & Top Mini Pagination */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 pb-2 border-b border-slate-100">
          <div
            role="tablist"
            aria-label="Lọc theo số sao đánh giá"
            className="flex items-center gap-1.5 overflow-x-auto no-scrollbar"
          >
            {[
              { label: "Tất cả sao", val: "" },
              { label: "5 Sao", val: 5 },
              { label: "4 Sao", val: 4 },
              { label: "3 Sao", val: 3 },
              { label: "2 Sao", val: 2 },
              { label: "1 Sao", val: 1 },
            ].map((tab) => {
              const isActive = ratingFilter === tab.val;
              return (
                <button
                  key={tab.label}
                  type="button"
                  role="tab"
                  aria-selected={isActive}
                  onClick={() => {
                    setRatingFilter(tab.val as number | "");
                    setPage(0);
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors whitespace-nowrap shrink-0 cursor-pointer min-h-[34px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                    isActive
                      ? "bg-slate-900 text-white"
                      : "text-slate-600 hover:bg-slate-100"
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          <Pagination
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

        {/* Secondary Filters */}
        <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo tên sản phẩm, phân loại SKU, mã đơn..."
              className="w-full h-9 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-primary"
            />
          </div>

          {/* Response Status Filter */}
          <select
            value={hasRespondedFilter}
            onChange={(e) => {
              setHasRespondedFilter(e.target.value as "" | "true" | "false");
              setPage(0);
            }}
            className="h-9 px-3 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white focus:outline-none focus:border-primary"
          >
            <option value="">Tất cả trạng thái phản hồi</option>
            <option value="false">Chưa phản hồi (Cần xử lý)</option>
            <option value="true">Đã phản hồi</option>
          </select>

          {/* Has Media Toggle */}
          <button
            type="button"
            onClick={() => {
              setHasMediaFilter(!hasMediaFilter);
              setPage(0);
            }}
            className={`h-9 px-3 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer shrink-0 ${
              hasMediaFilter
                ? "bg-primary/10 border-primary text-primary"
                : "border-slate-200 text-slate-600 hover:bg-slate-50"
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>Có hình ảnh</span>
          </button>
        </div>
      </div>

      {/* 3. Review List Cards */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 p-12 text-center flex flex-col items-center justify-center gap-2">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <span className="text-xs text-slate-500">Đang tải danh sách đánh giá...</span>
        </div>
      ) : reviews.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 p-12 text-center flex flex-col items-center justify-center gap-2">
          <MessageSquare className="w-10 h-10 text-slate-300 mb-1" />
          <h3 className="text-sm font-bold text-slate-800">
            Không tìm thấy đánh giá nào
          </h3>
          <p className="text-xs text-slate-400 max-w-sm">
            Hiện chưa có nhận xét nào phù hợp với bộ lọc đã chọn. Hãy thử nới lỏng bộ lọc để xem các đánh giá khác.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {reviews.map((rev) => (
            <div
              key={rev.id}
              className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-2xs flex flex-col gap-3 hover:border-slate-300 transition-colors"
            >
              {/* Card Top: Customer Info & Order Details */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
                <div className="flex items-center gap-3 min-w-0">
                  <UserAvatar
                    src={rev.reviewerAvatar}
                    alt={rev.reviewerName}
                    size="sm"
                  />
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-900 truncate">
                        {rev.reviewerName}
                      </span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-emerald-50 text-emerald-700 font-semibold border border-emerald-200/60">
                        Người mua thực tế
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                      <span>Đơn hàng: #{rev.orderNumber || "N/A"}</span>
                      <span>•</span>
                      <span>{new Date(rev.createdAt).toLocaleDateString("vi-VN")}</span>
                    </div>
                  </div>
                </div>

                {/* Rating Stars & Response Badge */}
                <div className="flex items-center gap-3 shrink-0 self-end sm:self-auto">
                  <div className="flex items-center gap-0.5 text-amber-400">
                    {[...Array(5)].map((_, i) => (
                      <Star
                        key={i}
                        className={`w-4 h-4 ${
                          i < rev.rating ? "fill-current" : "text-slate-200"
                        }`}
                      />
                    ))}
                  </div>
                  {rev.sellerResponse ? (
                    <span className="text-[11px] font-bold px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200/80">
                      Đã phản hồi
                    </span>
                  ) : (
                    <span className="text-[11px] font-bold px-2.5 py-1 rounded-lg bg-amber-50 text-amber-700 border border-amber-200/80 animate-pulse">
                      Chưa phản hồi
                    </span>
                  )}
                </div>
              </div>

              {/* Product Reference */}
              <div className="flex items-center gap-3 p-2.5 rounded-xl bg-slate-50/70 border border-slate-100">
                <ProductThumbnail
                  src={rev.imageUrl}
                  alt={rev.productName || "Sản phẩm"}
                  sizeClassName="w-10 h-10"
                />
                <div className="min-w-0 flex-1">
                  <h4 className="text-xs font-bold text-slate-800 line-clamp-1">
                    {rev.productName || "Sản phẩm"}
                  </h4>
                  <p className="text-[11px] text-slate-500 truncate">
                    Phân loại SKU: <span className="text-slate-700 font-medium">{rev.variantName || rev.sku || "Mặc định"}</span>
                  </p>
                </div>
              </div>

              {/* Customer Comment */}
              {rev.comment && (
                <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
                  {rev.comment}
                </p>
              )}

              {/* Customer Uploaded Media */}
              {rev.mediaList && rev.mediaList.length > 0 && (
                <div className="flex items-center gap-2 pt-1 flex-wrap">
                  {rev.mediaList.map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => setPreviewImageUrl(m.mediaUrl)}
                      className="relative w-14 h-14 rounded-xl overflow-hidden border border-slate-200 bg-white group cursor-pointer hover:ring-2 hover:ring-primary transition-all shrink-0"
                    >
                      <Image
                        src={m.mediaUrl}
                        alt="Ảnh khách gửi"
                        fill
                        className="object-cover group-hover:scale-105 transition-transform"
                        unoptimized
                      />
                    </button>
                  ))}
                </div>
              )}

              {/* Existing Seller Response */}
              {rev.sellerResponse ? (
                <div className="p-3.5 rounded-xl bg-primary/5 border border-primary/20 text-xs mt-1">
                  <div className="flex items-center gap-1.5 font-bold text-primary mb-1">
                    <Store className="w-3.5 h-3.5 shrink-0" />
                    <span>Phản hồi của Shop:</span>
                    {rev.sellerResponseAt && (
                      <span className="text-[10px] font-normal text-slate-400">
                        • {new Date(rev.sellerResponseAt).toLocaleDateString("vi-VN")}
                      </span>
                    )}
                  </div>
                  <p className="text-slate-700 leading-relaxed pl-5">
                    {rev.sellerResponse}
                  </p>
                </div>
              ) : (
                /* Respond Button */
                <div className="flex justify-end pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => handleOpenRespondModal(rev)}
                    className="px-3.5 py-1.5 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 cursor-pointer"
                  >
                    <MessageSquareQuote className="w-3.5 h-3.5" />
                    <span>Phản hồi khách hàng</span>
                  </button>
                </div>
              )}
            </div>
          ))}

          {/* 4. Bottom Pagination */}
          <Pagination
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

      {/* 5. Response Modal (BR-20.25: 1 Response Only Invariant) */}
      {respondingReview && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="respond-modal-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/50 backdrop-blur-xs animate-in fade-in"
        >
          <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50 shrink-0">
              <div className="flex items-center gap-2">
                <MessageSquareQuote className="w-5 h-5 text-primary" />
                <h3
                  id="respond-modal-title"
                  className="text-base font-bold text-slate-900 font-heading"
                >
                  Phản hồi Đánh giá của Khách
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setRespondingReview(null)}
                className="w-8 h-8 rounded-xl flex items-center justify-center text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form
              onSubmit={handleSubmit(handleRespondSubmit)}
              className="p-6 overflow-y-auto space-y-4 flex-1"
            >
              {/* Important Rule BR-20.25 Warning Banner */}
              <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200/80 text-amber-900 text-xs flex items-start gap-2.5">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">
                  <strong>Quy định phản hồi sàn (BR-20.25):</strong>
                  <p className="mt-0.5 text-amber-800">
                    Mỗi đánh giá bạn <strong>chỉ được gửi phản hồi 1 lần duy nhất</strong> và không thể sửa đổi sau khi gửi. Hãy kiểm tra kỹ nội dung trước khi bấm Gửi.
                  </p>
                </div>
              </div>

              {/* Review Snippet */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs">
                <span className="font-bold text-slate-700">Khách hàng: </span>
                <span className="text-slate-900">{respondingReview.reviewerName}</span>
                <div className="flex items-center gap-1 text-amber-400 my-1">
                  {[...Array(respondingReview.rating)].map((_, i) => (
                    <Star key={i} className="w-3.5 h-3.5 fill-current" />
                  ))}
                </div>
                {respondingReview.comment && (
                  <p className="text-slate-600 italic mt-1 line-clamp-2">
                    &ldquo;{respondingReview.comment}&rdquo;
                  </p>
                )}
              </div>

              {/* Response Input */}
              <div className="space-y-1.5">
                <label
                  htmlFor="seller-response"
                  className="text-xs font-semibold text-slate-700"
                >
                  Nội dung phản hồi của Shop:
                </label>
                <textarea
                  id="seller-response"
                  {...register("response")}
                  rows={4}
                  placeholder="Cảm ơn quý khách đã tin tưởng mua sắm tại Shop! Nếu cần hỗ trợ thêm thông số kỹ thuật hoặc bảo hành, vui lòng liên hệ Shop nhé..."
                  className="w-full p-3 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                />
                {errors.response && (
                  <span className="text-rose-600 text-xs font-medium">
                    {errors.response.message}
                  </span>
                )}
              </div>

              {/* Actions */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setRespondingReview(null)}
                  disabled={isSubmitting || respondMutation.isPending}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer disabled:opacity-50"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || respondMutation.isPending}
                  className="px-5 py-2 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting || respondMutation.isPending ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang gửi...</span>
                    </>
                  ) : (
                    <>
                      <Send className="w-3.5 h-3.5" />
                      <span>Gửi phản hồi</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Image Preview Modal */}
      {previewImageUrl && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-xs animate-in fade-in"
          onClick={() => setPreviewImageUrl(null)}
        >
          <div
            className="relative max-w-3xl w-full max-h-[85vh] bg-black rounded-2xl overflow-hidden flex items-center justify-center"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setPreviewImageUrl(null)}
              className="absolute top-3 right-3 z-10 w-8 h-8 rounded-full bg-white/20 hover:bg-white/40 text-white flex items-center justify-center transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="relative w-full h-[70vh]">
              <Image
                src={previewImageUrl}
                alt="Ảnh khách hàng"
                fill
                className="object-contain"
                unoptimized
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
