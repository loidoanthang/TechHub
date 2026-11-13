"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  CheckCircle2,
  XCircle,
  ShieldAlert,
  ShieldCheck,
  Loader2,
  AlertCircle,
  Store,
  Tag,
  Layers,
  Image as ImageIcon,
} from "lucide-react";
import {
  useAdminProductDetailQuery,
  useUpdateProductApprovalMutation,
  useUpdateProductSuspendMutation,
} from "../../hooks/useAdminModeration";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  rejectionReasonSchema,
  type RejectionReasonFormValues,
} from "../../schemas/moderationSchema";
import { formatPrice, getThumbnailUrl } from "@/lib/utils";

interface ProductModerationDrawerProps {
  productId: string | null;
  onClose: () => void;
  onActionSuccess?: (message: string) => void;
}

const REJECT_PRESETS = [
  "Hình ảnh sản phẩm mờ, thiếu hoặc chứa watermark đối thủ",
  "Thông số kỹ thuật không khớp với tên / phiên bản sản phẩm",
  "Giá bán bất thường hoặc có dấu hiệu gian lận thương mại",
  "Sản phẩm thuộc danh mục hàng cấm / vi phạm bản quyền trên sàn",
];

export function ProductModerationDrawer({
  productId,
  onClose,
  onActionSuccess,
}: ProductModerationDrawerProps) {
  const {
    data: product,
    isLoading,
    isError,
    refetch,
  } = useAdminProductDetailQuery(productId);
  const approvalMutation = useUpdateProductApprovalMutation();
  const suspendMutation = useUpdateProductSuspendMutation();

  const [showRejectBox, setShowRejectBox] = useState(false);
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleClose = useCallback(() => {
    setShowRejectBox(false);
    setSelectedImageIdx(0);
    setActionError(null);
    onClose();
  }, [onClose]);

  useAdminDialogA11y(Boolean(productId), handleClose);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<RejectionReasonFormValues>({
    resolver: zodResolver(rejectionReasonSchema),
    defaultValues: { reason: "" },
  });

  useEffect(() => {
    if (productId) {
      reset({ reason: "" });
    }
  }, [productId, reset]);

  if (!productId) return null;

  const handleApprove = async () => {
    setActionError(null);
    try {
      await approvalMutation.mutateAsync({
        id: productId,
        data: { status: "APPROVED" },
      });
      onActionSuccess?.(`Đã phê duyệt sản phẩm "${product?.name}" thành công!`);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể phê duyệt sản phẩm lúc này.";
      setActionError(msg);
    }
  };

  const handleRejectSubmit = async (values: RejectionReasonFormValues) => {
    setActionError(null);
    try {
      await approvalMutation.mutateAsync({
        id: productId,
        data: {
          status: "REJECTED",
          rejectionReason: values.reason.trim(),
        },
      });
      onActionSuccess?.(`Đã từ chối sản phẩm "${product?.name}".`);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Từ chối sản phẩm thất bại.";
      setActionError(msg);
    }
  };

  const handleToggleSuspend = async () => {
    if (!product) return;
    setActionError(null);
    try {
      const nextSuspended = !product.isSuspended;
      await suspendMutation.mutateAsync({
        id: productId,
        data: { isSuspended: nextSuspended },
      });
      onActionSuccess?.(
        nextSuspended
          ? `Đã tạm đình chỉ hiển thị sản phẩm "${product.name}".`
          : `Đã gỡ đình chỉ sản phẩm "${product.name}".`
      );
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Thao tác cập nhật đình chỉ thất bại.";
      setActionError(msg);
    }
  };

  const images = [...(product?.images || [])].sort(
    (a, b) => a.displayOrder - b.displayOrder
  );
  const activeImage = images[selectedImageIdx]?.imageUrl;
  const specEntries = Object.entries(product?.attributes || {});

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-moderation-drawer-title"
      className="fixed inset-0 z-50 flex justify-end"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/50 transition-opacity"
        onClick={handleClose}
      />

      {/* Drawer Panel */}
      <div className="relative z-10 w-full max-w-2xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-200 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70 shrink-0">
          <div className="min-w-0">
            <h2
              id="product-moderation-drawer-title"
              className="font-heading text-base font-extrabold text-on-surface truncate"
            >
              Hồ sơ Kiểm duyệt Sản phẩm
            </h2>
            <p className="text-[11px] text-slate-600 font-mono truncate">
              ID: {productId}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng hồ sơ kiểm duyệt"
            onClick={handleClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 hover:text-slate-900 transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 flex flex-col gap-5">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
              <span className="text-xs text-slate-500">
                Đang tải chi tiết sản phẩm...
              </span>
            </div>
          ) : isError || !product ? (
            <div className="p-8 text-center flex flex-col items-center gap-3">
              <AlertCircle className="w-8 h-8 text-rose-500" />
              <p className="text-xs font-bold text-slate-800">
                Không thể tải thông tin chi tiết sản phẩm
              </p>
              <button
                type="button"
                onClick={() => refetch()}
                className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
              >
                Thử lại
              </button>
            </div>
          ) : (
            <>
              {actionError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{actionError}</span>
                </div>
              )}

              {/* Status Banner */}
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`px-2.5 py-1 rounded-full text-[11px] font-extrabold ${
                    product.approvalStatus === "APPROVED"
                      ? "bg-emerald-100 text-emerald-700"
                      : product.approvalStatus === "PENDING_APPROVAL"
                      ? "bg-amber-100 text-amber-800"
                      : product.approvalStatus === "REJECTED"
                      ? "bg-rose-100 text-rose-700"
                      : "bg-slate-100 text-slate-700"
                  }`}
                >
                  Duyệt: {product.approvalStatus}
                </span>

                <span
                  className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                    product.visibilityStatus === "PUBLISHED"
                      ? "bg-blue-100 text-[#0045cc]"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  Hiển thị: {product.visibilityStatus}
                </span>

                {product.isSuspended && (
                  <span className="px-2.5 py-1 rounded-full text-[11px] font-extrabold bg-rose-600 text-white">
                    ĐANG BỊ ĐÌNH CHỈ
                  </span>
                )}
              </div>

              {product.rejectionReason && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800">
                  <p className="font-bold mb-1">Lý do từ chối trước đó:</p>
                  <p>{product.rejectionReason}</p>
                </div>
              )}

              {/* Product Title & Meta */}
              <div className="flex flex-col gap-2">
                <h3 className="font-heading text-lg font-bold text-[#0b1c30]">
                  {product.name}
                </h3>
                <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600">
                  <span className="flex items-center gap-1 font-semibold text-[#0045cc]">
                    <Store className="w-3.5 h-3.5" />
                    {product.shopName} ({product.shopStatus})
                  </span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <Tag className="w-3.5 h-3.5 text-slate-400" />
                    {product.categoryName} ({product.categoryStatus})
                  </span>
                  <span>•</span>
                  <span>Thương hiệu: {product.brandName}</span>
                </div>
              </div>

              {/* Gallery */}
              <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/80 flex flex-col gap-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                    <ImageIcon className="w-4 h-4 text-[#0045cc]" />
                    Thư viện hình ảnh ({images.length})
                  </span>
                  {activeImage && (
                    <a
                      href={activeImage}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[11px] font-semibold text-[#0045cc] hover:underline"
                    >
                      Mở ảnh gốc ↗
                    </a>
                  )}
                </div>
                {activeImage ? (
                  <div className="w-full h-56 rounded-xl bg-white border border-slate-200 overflow-hidden flex items-center justify-center">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={activeImage}
                      alt={product.name}
                      decoding="async"
                      className="max-h-full max-w-full object-contain"
                    />
                  </div>
                ) : (
                  <div className="w-full h-36 rounded-xl bg-white border border-dashed border-slate-300 flex items-center justify-center text-xs text-slate-400">
                    Không có hình ảnh
                  </div>
                )}

                {images.length > 1 && (
                  <div className="flex items-center gap-2 overflow-x-auto no-scrollbar">
                    {images.map((img, i) => (
                      <button
                        key={img.id}
                        type="button"
                        onClick={() => setSelectedImageIdx(i)}
                        className={`w-14 h-14 rounded-lg border-2 overflow-hidden shrink-0 bg-white cursor-pointer ${
                          selectedImageIdx === i
                            ? "border-[#0045cc]"
                            : "border-slate-200"
                        }`}
                      >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img
                          src={getThumbnailUrl(img.imageUrl, 120)}
                          alt=""
                          width={56}
                          height={56}
                          decoding="async"
                          className="w-full h-full object-cover"
                        />
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Variants Table */}
              <div className="flex flex-col gap-2">
                <span className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                  <Layers className="w-4 h-4 text-[#0045cc]" />
                  Danh sách Phân loại & Giá bán ({product.variants.length})
                </span>
                <div className="border border-slate-200 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                        <th className="py-2 px-3">Phân loại</th>
                        <th className="py-2 px-3">SKU</th>
                        <th className="py-2 px-3 text-right">Giá bán</th>
                        <th className="py-2 px-3 text-center">Kho</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {product.variants.map((v) => (
                        <tr key={v.id}>
                          <td className="py-2.5 px-3 font-semibold text-slate-800">
                            {v.variantName}
                          </td>
                          <td className="py-2.5 px-3 font-mono text-[11px] text-slate-600">
                            {v.sku}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-900">
                            {formatPrice(v.price)}
                          </td>
                          <td className="py-2.5 px-3 text-center font-mono">
                            {v.stockQuantity}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Specs */}
              {specEntries.length > 0 && (
                <div className="flex flex-col gap-2">
                  <span className="text-xs font-bold text-slate-700">
                    Thông số kỹ thuật
                  </span>
                  <div className="bg-slate-50 rounded-xl border border-slate-200 divide-y divide-slate-200/70">
                    {specEntries.map(([k, val]) => (
                      <div
                        key={k}
                        className="px-3.5 py-2 flex items-center justify-between gap-4 text-xs"
                      >
                        <span className="font-semibold text-slate-600">{k}</span>
                        <span className="font-medium text-slate-900 text-right">
                          {String(val)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Description */}
              {product.description && (
                <div className="flex flex-col gap-1.5">
                  <span className="text-xs font-bold text-slate-700">
                    Mô tả chi tiết
                  </span>
                  <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-line bg-slate-50 p-3.5 rounded-xl border border-slate-200/70">
                    {product.description}
                  </p>
                </div>
              )}

              {/* Reject Reason Form (Expanded when Reject clicked) */}
              {showRejectBox && (
                <form
                  onSubmit={handleSubmit(handleRejectSubmit)}
                  className="bg-rose-50/70 border border-rose-200 rounded-2xl p-4 flex flex-col gap-3 animate-in fade-in"
                >
                  <label className="text-xs font-bold text-rose-900">
                    Lý do từ chối phê duyệt (Bắt buộc) *
                  </label>
                  <textarea
                    rows={3}
                    {...register("reason")}
                    placeholder="Nhập rõ lỗi để người bán biết cách chỉnh sửa và gửi duyệt lại..."
                    className="w-full p-3 rounded-xl border border-rose-300 bg-white text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-rose-400/30"
                  />
                  {errors.reason && (
                    <p className="text-[11px] text-rose-600 font-medium">
                      {errors.reason.message}
                    </p>
                  )}

                  {/* Quick presets */}
                  <div className="flex flex-wrap gap-1.5">
                    {REJECT_PRESETS.map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() =>
                          setValue("reason", preset, { shouldValidate: true })
                        }
                        className="text-[10px] px-2.5 py-1 rounded-lg bg-white border border-rose-200 text-rose-700 hover:bg-rose-100 transition-colors text-left cursor-pointer"
                      >
                        + {preset}
                      </button>
                    ))}
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setShowRejectBox(false)}
                      className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-white cursor-pointer"
                    >
                      Hủy
                    </button>
                    <button
                      type="submit"
                      disabled={approvalMutation.isPending}
                      className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                    >
                      {approvalMutation.isPending && (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      )}
                      <span>Xác nhận Từ chối</span>
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>

        {/* Sticky Actions Footer */}
        {product && (
          <div className="px-5 py-4 border-t border-slate-200 bg-slate-50 flex flex-wrap items-center justify-between gap-2 shrink-0">
            <button
              type="button"
              onClick={handleToggleSuspend}
              disabled={suspendMutation.isPending}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold border transition-colors duration-150 flex items-center gap-1.5 cursor-pointer ${
                product.isSuspended
                  ? "bg-emerald-50 border-emerald-300 text-emerald-700 hover:bg-emerald-100"
                  : "bg-white border-rose-300 text-rose-700 hover:bg-rose-50"
              }`}
            >
              {suspendMutation.isPending ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : product.isSuspended ? (
                <ShieldCheck className="w-3.5 h-3.5" />
              ) : (
                <ShieldAlert className="w-3.5 h-3.5" />
              )}
              <span>
                {product.isSuspended ? "Gỡ Đình Chỉ" : "Đình Chỉ Vi Phạm"}
              </span>
            </button>

            <div className="flex items-center gap-2">
              {product.approvalStatus !== "REJECTED" && !showRejectBox && (
                <button
                  type="button"
                  onClick={() => setShowRejectBox(true)}
                  className="px-4 py-2 rounded-xl border border-rose-300 bg-white hover:bg-rose-50 text-rose-600 text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                >
                  <XCircle className="w-4 h-4" />
                  <span>Từ chối</span>
                </button>
              )}

              {product.approvalStatus !== "APPROVED" && (
                <button
                  type="button"
                  onClick={handleApprove}
                  disabled={approvalMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1.5 shadow-xs cursor-pointer"
                >
                  {approvalMutation.isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="w-4 h-4" />
                  )}
                  <span>Phê duyệt Lên sàn</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
