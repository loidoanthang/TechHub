"use client";

import React, { useState, useEffect } from "react";
import {
  X,
  AlertTriangle,
  CheckCircle2,
  Printer,
  ShieldCheck,
  AlertCircle,
  Loader2,
} from "lucide-react";
import { formatPrice } from "@/lib/utils";
import type { SellerOrderResponse } from "@/types/sellerOrders";
import type { OrderStatus, CancellationReasonCode } from "@/types/enums";

// Helper hook for Accessible Dialogs (Escape key + body scroll lock)
function useModalAccessibility(isOpen: boolean, onClose: () => void) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, onClose]);
}

// 1. STATUS TRANSITION MODAL
interface StatusTransitionModalProps {
  order: SellerOrderResponse | null;
  targetStatus: OrderStatus | null;
  title: string;
  description: string;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (orderId: string, status: OrderStatus, note?: string) => Promise<void>;
  isLoading?: boolean;
}

export function StatusTransitionModal({
  order,
  targetStatus,
  title,
  description,
  isOpen,
  onClose,
  onConfirm,
  isLoading,
}: StatusTransitionModalProps) {
  const [note, setNote] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useModalAccessibility(isOpen, onClose);

  useEffect(() => {
    if (isOpen) {
      setNote("");
      setErrorMessage(null);
    }
  }, [isOpen]);

  if (!isOpen || !order || !targetStatus) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    try {
      await onConfirm(order.orderId, targetStatus, note.trim() || undefined);
      setNote("");
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Không thể cập nhật trạng thái đơn hàng. Vui lòng thử lại.";
      setErrorMessage(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="transition-modal-title"
      aria-describedby="transition-modal-desc"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs motion-reduce:backdrop-blur-none"
    >
      <div className="bg-white rounded-3xl max-w-md w-full p-6 border border-slate-200 shadow-xl flex flex-col gap-5 animate-in fade-in zoom-in-95 motion-reduce:animate-none">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div
              aria-hidden="true"
              className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0"
            >
              <CheckCircle2 className="w-4 h-4" />
            </div>
            <h3 id="transition-modal-title" className="font-heading text-base font-bold text-on-surface">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
            className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-500 hover:text-on-surface hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p id="transition-modal-desc" className="text-xs text-on-surface-variant leading-relaxed">
          {description}
        </p>

        {errorMessage && (
          <div
            role="alert"
            className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-start gap-2"
          >
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 text-xs flex flex-col gap-1.5">
          <div className="flex justify-between">
            <span className="text-slate-600">Mã đơn hàng:</span>
            <span className="font-mono font-bold text-primary">#{order.orderNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-600">Người nhận:</span>
            <span className="font-semibold text-on-surface">
              {order.buyerInfo?.recipientName} ({order.buyerInfo?.recipientPhone})
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="status-transition-note" className="text-xs font-bold text-on-surface">
              Ghi chú nội bộ Shop (Tùy chọn):
            </label>
            <textarea
              id="status-transition-note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              maxLength={500}
              placeholder="VD: Đã kiểm tra số serial phần cứng, đóng gói 3 lớp xốp..."
              rows={3}
              className="w-full text-xs p-3 rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-on-surface placeholder:text-slate-500"
            />
            <span className="text-[10px] text-slate-600 text-right">
              {note.length}/500 ký tự
            </span>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="min-h-[40px] px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="min-h-[40px] px-5 py-2 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
            >
              {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none" />}
              <span>Xác nhận cập nhật</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 2. CANCEL ORDER MODAL
interface CancelOrderModalProps {
  order: SellerOrderResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (orderId: string, reasonCode: CancellationReasonCode, detail?: string) => Promise<void>;
  isLoading?: boolean;
}

export function CancelOrderModal({
  order,
  isOpen,
  onClose,
  onConfirm,
  isLoading,
}: CancelOrderModalProps) {
  const [reasonCode, setReasonCode] = useState<CancellationReasonCode>("KHAC");
  const [reasonDetail, setReasonDetail] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useModalAccessibility(isOpen, onClose);

  useEffect(() => {
    if (isOpen) {
      setReasonCode("KHAC");
      setReasonDetail("");
      setErrorMessage(null);
    }
  }, [isOpen]);

  if (!isOpen || !order) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    try {
      await onConfirm(order.orderId, reasonCode, reasonDetail.trim() || undefined);
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Không thể hủy đơn hàng vào lúc này. Vui lòng kiểm tra lại.";
      setErrorMessage(msg);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="cancel-order-modal-title"
      aria-describedby="cancel-order-modal-desc"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs motion-reduce:backdrop-blur-none"
    >
      <div className="bg-white rounded-3xl max-w-md w-full p-6 border border-slate-200 shadow-xl flex flex-col gap-5 animate-in fade-in zoom-in-95 motion-reduce:animate-none">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2 text-red-600">
            <AlertTriangle className="w-5 h-5 shrink-0" />
            <h3 id="cancel-order-modal-title" className="font-heading text-base font-bold text-red-700">
              Hủy Đơn Hàng #{order.orderNumber}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
            className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-500 hover:text-on-surface hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p id="cancel-order-modal-desc" className="text-xs text-slate-600 leading-relaxed">
          Việc hủy đơn từ phía người bán có thể ảnh hưởng đến tỷ lệ vận hành của gian hàng. Vui lòng xác định rõ lý do gửi đến người mua.
        </p>

        {errorMessage && (
          <div
            role="alert"
            className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-start gap-2"
          >
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cancel-order-reason-select" className="text-xs font-bold text-on-surface">
              Lý do hủy đơn <span className="text-red-600">*</span>
            </label>
            <select
              id="cancel-order-reason-select"
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value as CancellationReasonCode)}
              className="w-full text-xs p-2.5 rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-primary text-on-surface font-medium cursor-pointer"
            >
              <option value="KHAC">Sản phẩm tạm hết hàng trong kho</option>
              <option value="DON_MUON_MUA_NUA">Khách hàng yêu cầu hủy qua điện thoại</option>
              <option value="THAY_DOI_Y_DINH">Thông tin địa chỉ giao hàng không hợp lệ</option>
              <option value="TIM_THAY_GIA_TOT_HON">Sự cố niêm yết sai giá linh kiện</option>
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="cancel-order-reason-detail" className="text-xs font-bold text-on-surface">
              Chi tiết giải thích cho khách hàng:
            </label>
            <textarea
              id="cancel-order-reason-detail"
              value={reasonDetail}
              onChange={(e) => setReasonDetail(e.target.value)}
              placeholder="Nhập lý do cụ thể gửi đến người mua..."
              rows={3}
              className="w-full text-xs p-3 rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-primary text-on-surface placeholder:text-slate-500"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="min-h-[40px] px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
            >
              Không hủy
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="min-h-[40px] px-5 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-600 disabled:opacity-50"
            >
              {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none" />}
              <span>Xác nhận Hủy Đơn</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 3. REJECT REQUEST MODAL (CANCELLATION & RETURN DISPUTES)
interface RejectRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderNumber?: string;
  type: "CANCELLATION" | "RETURN";
  onConfirm: (note: string) => Promise<void>;
  isLoading?: boolean;
}

export function RejectRequestModal({
  isOpen,
  onClose,
  orderNumber,
  type,
  onConfirm,
  isLoading,
}: RejectRequestModalProps) {
  const [note, setNote] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useModalAccessibility(isOpen, onClose);

  useEffect(() => {
    if (isOpen) {
      setNote(
        type === "CANCELLATION"
          ? "Hàng đã được đóng gói và niêm phong gửi bưu tá, không thể chấp thuận hủy."
          : "Sản phẩm không thuộc diện đổi trả hoặc đã quá hạn theo quy định bảo hành."
      );
      setErrorMessage(null);
    }
  }, [isOpen, type]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!note.trim()) {
      setErrorMessage("Vui lòng nhập lý do từ chối để phản hồi đến người mua.");
      return;
    }
    setErrorMessage(null);
    try {
      await onConfirm(note.trim());
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Thao tác từ chối thất bại. Vui lòng thử lại.";
      setErrorMessage(msg);
    }
  };

  const title =
    type === "CANCELLATION"
      ? `Từ Chối Yêu Cầu Hủy Đơn #${orderNumber || ""}`
      : `Từ Chối Yêu Cầu Trả Hàng #${orderNumber || ""}`;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="reject-request-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs motion-reduce:backdrop-blur-none"
    >
      <div className="bg-white rounded-3xl max-w-md w-full p-6 border border-slate-200 shadow-xl flex flex-col gap-5 animate-in fade-in zoom-in-95 motion-reduce:animate-none">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2 text-slate-800">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
            <h3 id="reject-request-modal-title" className="font-heading text-base font-bold text-on-surface">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
            className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-500 hover:text-on-surface hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Vui lòng nêu rõ lý do từ chối để hệ thống gửi thông báo giải trình minh bạch đến người mua.
        </p>

        {errorMessage && (
          <div
            role="alert"
            className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-start gap-2"
          >
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="reject-request-note" className="text-xs font-bold text-on-surface">
              Lý do từ chối gửi người mua <span className="text-red-600">*</span>:
            </label>
            <textarea
              id="reject-request-note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              maxLength={400}
              placeholder="Nhập lý do giải trình cụ thể..."
              className="w-full text-xs p-3 rounded-xl bg-slate-50 border border-slate-200 focus:outline-none focus:border-primary text-on-surface placeholder:text-slate-500"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="min-h-[40px] px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="min-h-[40px] px-5 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-700 disabled:opacity-50"
            >
              {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none" />}
              <span>Xác nhận Từ chối</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 4. PRINT SHIPPING LABEL & HARDWARE TAMPER SEAL MODAL
interface PrintShippingLabelModalProps {
  order: SellerOrderResponse | null;
  isOpen: boolean;
  onClose: () => void;
}

export function PrintShippingLabelModal({
  order,
  isOpen,
  onClose,
}: PrintShippingLabelModalProps) {
  useModalAccessibility(isOpen, onClose);

  if (!isOpen || !order) return null;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="print-shipping-label-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs motion-reduce:backdrop-blur-none"
    >
      <div className="bg-white rounded-3xl max-w-xl w-full p-5 sm:p-7 border border-slate-200 shadow-2xl flex flex-col gap-5 max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95 motion-reduce:animate-none">
        {/* Header Actions */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <Printer className="w-5 h-5 text-primary" />
            <h3 id="print-shipping-label-title" className="font-heading text-base font-bold text-on-surface">
              Phiếu Gửi Hàng &amp; Tem Niêm Phong Phần Cứng
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng hộp thoại"
            className="min-w-[40px] min-h-[40px] flex items-center justify-center rounded-xl text-slate-500 hover:text-on-surface hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Printable Area */}
        <div
          id="shipping-label-printable"
          className="border-2 border-dashed border-slate-900 p-5 rounded-2xl flex flex-col gap-4 bg-white text-slate-900"
        >
          {/* Top: TechHub Logistics & Carrier Barcode */}
          <div className="flex items-center justify-between border-b-2 border-slate-900 pb-3">
            <div>
              <span className="font-heading font-extrabold text-lg tracking-tight">
                TECH<span className="text-primary">HUB</span> EXPRESS
              </span>
              <p className="text-[10px] text-slate-600">Đối tác vận chuyển: GHN Logistics</p>
            </div>
            <div className="text-right">
              <span className="font-mono text-base font-extrabold text-primary">
                #{order.orderNumber}
              </span>
              <p className="text-[10px] text-slate-600">Mã bưu gửi tiêu chuẩn</p>
            </div>
          </div>

          {/* Addresses: From & To */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs border-b-2 border-slate-900 pb-3">
            <div>
              <span className="font-bold uppercase tracking-wider text-[10px] text-slate-600">
                Người Gửi (From):
              </span>
              <p className="font-bold text-xs mt-0.5 text-on-surface">Gian hàng đối tác TechHub</p>
              <p className="text-[11px] text-on-surface-variant">Kho hàng trung tâm TechHub Hub</p>
            </div>
            <div>
              <span className="font-bold uppercase tracking-wider text-[10px] text-slate-600">
                Người Nhận (To):
              </span>
              <p className="font-bold text-xs mt-0.5 text-on-surface">
                {order.buyerInfo?.recipientName || order.buyerInfo?.buyerName}
              </p>
              <p className="font-mono text-[11px] font-semibold text-slate-700">
                {order.buyerInfo?.recipientPhone}
              </p>
              <p className="text-[11px] text-on-surface-variant line-clamp-2">
                {order.buyerInfo?.deliveryAddressSummary || order.buyerInfo?.provinceCity}
              </p>
            </div>
          </div>

          {/* Items checklist */}
          <div className="text-xs border-b-2 border-slate-900 pb-3 flex flex-col gap-1.5">
            <span className="font-bold uppercase tracking-wider text-[10px] text-slate-600">
              Danh mục linh kiện đóng gói ({order.totalItemsCount} món):
            </span>
            {order.items?.map((item, idx) => (
              <div key={idx} className="flex justify-between text-[11px] gap-2">
                <span className="truncate max-w-[200px] sm:max-w-[320px]">
                  • {item.productName} {item.variantName ? `(${item.variantName})` : ""}
                </span>
                <span className="font-mono font-bold shrink-0">x{item.quantity}</span>
              </div>
            ))}
          </div>

          {/* Bottom: COD & Tamper-Proof Seal Guarantee */}
          <div className="flex flex-wrap items-center justify-between gap-3 text-xs pt-1">
            <div>
              <span className="text-[10px] text-slate-600">Thu tiền mặt người nhận (COD):</span>
              <p className="font-mono font-extrabold text-base text-red-700">
                {order.paymentMethod === "COD"
                  ? formatPrice(order.buyerTotalAmount)
                  : "0₫ (ĐÃ THANH TOÁN ONLINE)"}
              </p>
            </div>

            <div className="flex items-center gap-1.5 bg-blue-50 border border-primary/30 px-3 py-1.5 rounded-xl text-center">
              <ShieldCheck className="w-5 h-5 text-primary shrink-0" />
              <div className="text-left">
                <span className="text-[10px] font-extrabold text-primary block leading-tight">
                  TEM CHỐNG SỐC &amp; NIÊM PHONG
                </span>
                <span className="text-[9px] text-slate-600">Đồng kiểm khi nhận kiện</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[40px] px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={handlePrint}
            className="min-h-[40px] px-6 py-2 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-md flex items-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <Printer className="w-4 h-4" />
            <span>In Phiếu Ngay</span>
          </button>
        </div>
      </div>
    </div>
  );
}
