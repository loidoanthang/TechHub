"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "next/navigation";
import {
  Package,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Info,
} from "lucide-react";
import { SellerOrdersHeader } from "./SellerOrdersHeader";
import { SellerOrdersTabs, type SellerOrderTabKey } from "./SellerOrdersTabs";
import { SellerOrderItemCard } from "./SellerOrderItemCard";
import {
  StatusTransitionModal,
  CancelOrderModal,
  PrintShippingLabelModal,
  RejectRequestModal,
} from "./SellerOrderActionModals";
import {
  useSellerOrdersQuery,
  useUpdateSellerOrderStatusMutation,
  useSellerCancelOrderMutation,
  useSellerCancellationRequestsQuery,
  useProcessCancellationRequestMutation,
  useSellerReturnRequestsQuery,
  useProcessReturnRequestMutation,
} from "../../hooks/useSellerOrders";
import { useSellerDashboardMetricsQuery } from "../../hooks/useSellerDashboard";
import type { SellerOrderResponse } from "@/types/sellerOrders";
import type { OrderStatus, CancellationReasonCode } from "@/types/enums";

export function SellerOrdersListView() {
  const searchParams = useSearchParams();

  // Read initial status or tab from URL query params (e.g. from Dashboard To-Do links)
  const initialStatus = searchParams.get("status") as OrderStatus | null;
  const initialTab = searchParams.get("tab") as string | null;

  const [activeTab, setActiveTab] = useState<SellerOrderTabKey>(() => {
    if (initialTab === "cancellations") return "CANCELLATIONS";
    if (initialTab === "returns") return "RETURNS";
    if (initialStatus) return initialStatus as SellerOrderTabKey;
    return "ALL";
  });

  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);

  // Debounce search input by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [keyword]);

  // Dashboard counts for tab badges
  const { data: dashboardMetrics } = useSellerDashboardMetricsQuery();
  const breakdown = dashboardMetrics?.orderStatusBreakdown;

  // 1. ORDERS QUERY
  const isOrdersTab = activeTab !== "CANCELLATIONS" && activeTab !== "RETURNS";
  const orderStatusParam =
    activeTab === "ALL" ? undefined : (activeTab as OrderStatus);

  const {
    data: ordersData,
    isLoading: isOrdersLoading,
    error: ordersError,
  } = useSellerOrdersQuery(
    isOrdersTab
      ? {
          status: orderStatusParam,
          keyword: debouncedKeyword || undefined,
          startDate: startDate || undefined,
          endDate: endDate || undefined,
          page,
          size: 10,
        }
      : undefined
  );

  // 2. CANCELLATION REQUESTS QUERY
  const {
    data: cancellationsData,
    isLoading: isCancellationsLoading,
  } = useSellerCancellationRequestsQuery(page, 10);

  // 3. RETURN REQUESTS QUERY
  const {
    data: returnsData,
    isLoading: isReturnsLoading,
  } = useSellerReturnRequestsQuery(page, 10);

  // Map real-time counts into tab badges
  const tabCounts: Partial<Record<SellerOrderTabKey, number>> = {
    PENDING_CONFIRMATION: breakdown?.pendingConfirmation,
    PREPARING: breakdown?.preparing,
    WAITING_FOR_PICKUP: breakdown?.waitingForPickup,
    SHIPPING: breakdown?.shipping,
    DELIVERED: breakdown?.delivered,
    COMPLETED: breakdown?.completed,
    CANCELLED: breakdown?.cancelled,
    CANCELLATIONS: cancellationsData?.totalElements,
    RETURNS: returnsData?.totalElements,
  };

  // MUTATIONS
  const updateStatusMutation = useUpdateSellerOrderStatusMutation();
  const cancelOrderMutation = useSellerCancelOrderMutation();
  const processCancellationMutation = useProcessCancellationRequestMutation();
  const processReturnMutation = useProcessReturnRequestMutation();

  // MODAL STATES
  const [selectedOrder, setSelectedOrder] = useState<SellerOrderResponse | null>(null);
  const [transitionTargetStatus, setTransitionTargetStatus] = useState<OrderStatus | null>(null);
  const [transitionModalTitle, setTransitionModalTitle] = useState("");
  const [transitionModalDesc, setTransitionModalDesc] = useState("");
  const [isTransitionOpen, setIsTransitionOpen] = useState(false);

  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [isPrintLabelOpen, setIsPrintLabelOpen] = useState(false);

  // Reject Request Modal state for disputes
  const [rejectModalState, setRejectModalState] = useState<{
    isOpen: boolean;
    requestId: string;
    orderNumber: string;
    type: "CANCELLATION" | "RETURN";
  }>({
    isOpen: false,
    requestId: "",
    orderNumber: "",
    type: "CANCELLATION",
  });

  // Handlers for Order Actions (memoized with useCallback)
  const handleConfirmOrder = useCallback((order: SellerOrderResponse) => {
    setSelectedOrder(order);
    setTransitionTargetStatus("CONFIRMED");
    setTransitionModalTitle("Xác Nhận Đơn Hàng");
    setTransitionModalDesc(
      "Xác nhận nhận đơn hàng để cam kết chuẩn bị linh kiện phần cứng giao cho bưu tá."
    );
    setIsTransitionOpen(true);
  }, []);

  const handleStartPreparing = useCallback((order: SellerOrderResponse) => {
    setSelectedOrder(order);
    setTransitionTargetStatus("PREPARING");
    setTransitionModalTitle("Bắt Đầu Đóng Gói");
    setTransitionModalDesc(
      "Chuyển trạng thái sang Đang chuẩn bị để nhân viên kho tiến hành kiểm tra số serial và quấn xốp niêm phong chống sốc."
    );
    setIsTransitionOpen(true);
  }, []);

  const handleReadyForPickup = useCallback((order: SellerOrderResponse) => {
    setSelectedOrder(order);
    setTransitionTargetStatus("WAITING_FOR_PICKUP");
    setTransitionModalTitle("Đóng Gói Xong - Chờ Bưu Tá Lấy");
    setTransitionModalDesc(
      "Xác nhận kiện hàng đã dán tem niêm phong hoàn tất và sẵn sàng để đơn vị vận chuyển GHN đến lấy hàng."
    );
    setIsTransitionOpen(true);
  }, []);

  const handlePrintLabel = useCallback((order: SellerOrderResponse) => {
    setSelectedOrder(order);
    setIsPrintLabelOpen(true);
  }, []);

  const handleOpenCancel = useCallback((order: SellerOrderResponse) => {
    setSelectedOrder(order);
    setIsCancelOpen(true);
  }, []);

  const handleConfirmTransition = useCallback(
    async (orderId: string, status: OrderStatus, note?: string) => {
      await updateStatusMutation.mutateAsync({ orderId, data: { status, note } });
    },
    [updateStatusMutation]
  );

  const handleConfirmCancel = useCallback(
    async (orderId: string, reasonCode: CancellationReasonCode, detail?: string) => {
      await cancelOrderMutation.mutateAsync({
        orderId,
        data: { reasonCode, reasonDetail: detail },
      });
    },
    [cancelOrderMutation]
  );

  // Rejection confirmation handler
  const handleConfirmReject = useCallback(
    async (note: string) => {
      if (rejectModalState.type === "CANCELLATION") {
        await processCancellationMutation.mutateAsync({
          requestId: rejectModalState.requestId,
          data: { approved: false, note },
        });
      } else {
        await processReturnMutation.mutateAsync({
          requestId: rejectModalState.requestId,
          data: { approved: false, note },
        });
      }
    },
    [rejectModalState, processCancellationMutation, processReturnMutation]
  );

  // Pagination calculation
  const totalElements = isOrdersTab
    ? ordersData?.totalElements ?? 0
    : activeTab === "CANCELLATIONS"
    ? cancellationsData?.totalElements ?? 0
    : returnsData?.totalElements ?? 0;

  const totalPages = isOrdersTab
    ? ordersData?.totalPages ?? 1
    : activeTab === "CANCELLATIONS"
    ? cancellationsData?.totalPages ?? 1
    : returnsData?.totalPages ?? 1;

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto">
      {/* 1. Header with search & date filters */}
      <SellerOrdersHeader
        keyword={keyword}
        onKeywordChange={setKeyword}
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={(val) => {
          setStartDate(val);
          setPage(0);
        }}
        onEndDateChange={(val) => {
          setEndDate(val);
          setPage(0);
        }}
        totalElements={totalElements}
      />

      {/* 2. Status & Flow Tabs */}
      <SellerOrdersTabs
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setPage(0);
        }}
        counts={tabCounts}
      />

      {/* Hint when on dispute tabs that search/dates don't apply */}
      {!isOrdersTab && (debouncedKeyword || startDate || endDate) && (
        <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-center gap-2">
          <Info className="w-4 h-4 text-primary shrink-0" />
          <span>
            Lưu ý: Bộ lọc từ khóa và ngày chỉ áp dụng cho danh sách đơn hàng chính.
          </span>
        </div>
      )}

      {/* 3. Error Banner */}
      {ordersError && isOrdersTab && (
        <div
          role="alert"
          className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-center gap-2"
        >
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>Lỗi tải danh sách đơn hàng: {(ordersError as Error).message}</span>
        </div>
      )}

      {/* 4. CONTENT AREA (WAI-ARIA Tabpanel) */}
      <div id="seller-orders-tab-content" role="tabpanel" aria-labelledby={`tab-${activeTab}`}>
        {isOrdersTab && (
          <div className="flex flex-col gap-4">
            {isOrdersLoading ? (
              <div className="flex flex-col gap-4">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="bg-white rounded-2xl p-6 border border-slate-200 animate-pulse motion-reduce:animate-none flex flex-col gap-4"
                  >
                    <div className="h-4 bg-slate-200 rounded w-1/3" />
                    <div className="flex gap-4">
                      <div className="w-14 h-14 bg-slate-200 rounded-xl shrink-0" />
                      <div className="flex-1 space-y-2">
                        <div className="h-4 bg-slate-200 rounded w-1/2" />
                        <div className="h-3 bg-slate-200 rounded w-1/4" />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : !ordersData?.content || ordersData.content.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center flex flex-col items-center gap-3 border border-slate-200">
                <div
                  aria-hidden="true"
                  className="w-14 h-14 rounded-2xl bg-primary/10 text-primary flex items-center justify-center"
                >
                  <Package className="w-7 h-7" />
                </div>
                <h3 className="font-heading text-base font-bold text-on-surface">
                  Không có đơn hàng nào
                </h3>
                <p className="text-xs text-slate-600 max-w-sm">
                  {debouncedKeyword || startDate || endDate
                    ? "Không tìm thấy đơn hàng nào phù hợp với điều kiện tìm kiếm và thời gian đã chọn."
                    : "Chưa có đơn hàng nào trong phân loại trạng thái này."}
                </p>
              </div>
            ) : (
              <div className="flex flex-col gap-4">
                {ordersData.content.map((order) => (
                  <SellerOrderItemCard
                    key={order.orderId}
                    order={order}
                    onConfirmOrder={handleConfirmOrder}
                    onStartPreparing={handleStartPreparing}
                    onReadyForPickup={handleReadyForPickup}
                    onPrintShippingLabel={handlePrintLabel}
                    onCancelOrder={handleOpenCancel}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB CANCELLATIONS: Yêu cầu hủy đơn */}
        {activeTab === "CANCELLATIONS" && (
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-2xs flex flex-col gap-4">
            <div className="border-b border-slate-100 pb-3">
              <h3 className="font-heading text-base font-bold text-on-surface">
                Yêu Cầu Hủy Đơn Từ Người Mua
              </h3>
              <p className="text-xs text-slate-600">
                Xử lý phê duyệt hoặc từ chối các yêu cầu hủy đơn hàng trước khi bưu tá lấy hàng.
              </p>
            </div>

            {isCancellationsLoading ? (
              <div className="p-8 text-center text-xs text-slate-600">
                Đang tải danh sách yêu cầu hủy...
              </div>
            ) : !cancellationsData?.content || cancellationsData.content.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-600 flex flex-col items-center gap-2">
                <CheckCircle2 className="w-8 h-8 text-emerald-600" />
                <span className="font-bold text-on-surface">Không có yêu cầu hủy đơn nào</span>
                <p>Hiện không có yêu cầu hủy đơn hàng nào cần bạn phê duyệt.</p>
              </div>
            ) : (
              <div className="flex flex-col divide-y divide-slate-100">
                {cancellationsData.content.map((req) => (
                  <div
                    key={req.id}
                    className="py-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
                  >
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-primary">
                          #{req.orderNumber}
                        </span>
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-900 border border-amber-300">
                          {req.status}
                        </span>
                      </div>
                      <p className="text-xs text-on-surface-variant">
                        Lý do: <strong>{req.reasonCode}</strong>{" "}
                        {req.reasonDetail ? `(${req.reasonDetail})` : ""}
                      </p>
                      <time className="text-[11px] text-slate-500 font-medium">
                        Gửi lúc: {new Date(req.requestedAt).toLocaleString("vi-VN")}
                      </time>
                    </div>

                    {req.status === "PENDING" && (
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            processCancellationMutation.mutate({
                              requestId: req.id,
                              data: { approved: true, note: "Shop đồng ý hủy đơn" },
                            })
                          }
                          disabled={processCancellationMutation.isPending}
                          className="min-h-[38px] px-3.5 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold transition-all shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-600 disabled:opacity-50"
                        >
                          Chấp thuận hủy
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setRejectModalState({
                              isOpen: true,
                              requestId: req.id,
                              orderNumber: req.orderNumber,
                              type: "CANCELLATION",
                            })
                          }
                          disabled={processCancellationMutation.isPending}
                          className="min-h-[38px] px-3.5 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
                        >
                          Từ chối
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB RETURNS: Yêu cầu trả hàng */}
        {activeTab === "RETURNS" && (
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-2xs flex flex-col gap-4">
            <div className="border-b border-slate-100 pb-3">
              <h3 className="font-heading text-base font-bold text-on-surface">
                Yêu Cầu Trả Hàng &amp; Hoàn Tiền
              </h3>
              <p className="text-xs text-slate-600">
                Khiếu nại về lỗi linh kiện, vỡ hỏng do vận chuyển hoặc không tương thích phần cứng.
              </p>
            </div>

            {isReturnsLoading ? (
              <div className="p-8 text-center text-xs text-slate-600">
                Đang tải danh sách khiếu nại...
              </div>
            ) : !returnsData?.content || returnsData.content.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-600 flex flex-col items-center gap-2">
                <CheckCircle2 className="w-8 h-8 text-emerald-600" />
                <span className="font-bold text-on-surface">Không có yêu cầu trả hàng nào</span>
                <p>Tất cả đơn hàng giao thành công đều không phát sinh khiếu nại trả hàng.</p>
              </div>
            ) : (
              <div className="flex flex-col divide-y divide-slate-100">
                {returnsData.content.map((ret) => (
                  <div
                    key={ret.id}
                    className="py-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
                  >
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-primary">
                          #{ret.orderNumber}
                        </span>
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-900 border border-red-300">
                          {ret.status}
                        </span>
                      </div>
                      <p className="text-xs text-on-surface-variant">
                        Lý do: <strong>{ret.reasonCode}</strong>{" "}
                        {ret.reasonDetail ? `(${ret.reasonDetail})` : ""}
                      </p>
                      <time className="text-[11px] text-slate-500 font-medium">
                        Gửi lúc: {new Date(ret.requestedAt).toLocaleString("vi-VN")}
                      </time>
                    </div>

                    {ret.status === "PENDING" && (
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            processReturnMutation.mutate({
                              requestId: ret.id,
                              data: { approved: true, note: "Đồng ý nhận lại hàng" },
                            })
                          }
                          disabled={processReturnMutation.isPending}
                          className="min-h-[38px] px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 disabled:opacity-50"
                        >
                          Chấp thuận hoàn tiền
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setRejectModalState({
                              isOpen: true,
                              requestId: ret.id,
                              orderNumber: ret.orderNumber,
                              type: "RETURN",
                            })
                          }
                          disabled={processReturnMutation.isPending}
                          className="min-h-[38px] px-3.5 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
                        >
                          Từ chối
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 5. PAGINATION FOOTER */}
      {totalPages > 1 && (
        <nav
          aria-label="Phân trang danh sách đơn hàng"
          className="flex items-center justify-between bg-white px-5 py-3.5 rounded-2xl border border-slate-200 shadow-2xs text-xs"
        >
          <span className="text-slate-600">
            Trang <strong className="text-on-surface">{page + 1}</strong> /{" "}
            <strong>{totalPages}</strong> (Tổng {totalElements} kết quả)
          </span>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              aria-label="Trang trước"
              className="min-w-[36px] min-h-[36px] flex items-center justify-center rounded-lg border border-slate-200 text-slate-600 hover:bg-blue-50 hover:text-primary transition-colors disabled:opacity-30 disabled:pointer-events-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span
              aria-current="page"
              className="min-w-[36px] min-h-[36px] flex items-center justify-center rounded-lg bg-primary text-white font-bold"
            >
              {page + 1}
            </span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              aria-label="Trang tiếp theo"
              className="min-w-[36px] min-h-[36px] flex items-center justify-center rounded-lg border border-slate-200 text-slate-600 hover:bg-blue-50 hover:text-primary transition-colors disabled:opacity-30 disabled:pointer-events-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </nav>
      )}

      {/* ACTION MODALS */}
      <StatusTransitionModal
        order={selectedOrder}
        targetStatus={transitionTargetStatus}
        title={transitionModalTitle}
        description={transitionModalDesc}
        isOpen={isTransitionOpen}
        onClose={() => setIsTransitionOpen(false)}
        onConfirm={handleConfirmTransition}
        isLoading={updateStatusMutation.isPending}
      />

      <CancelOrderModal
        order={selectedOrder}
        isOpen={isCancelOpen}
        onClose={() => setIsCancelOpen(false)}
        onConfirm={handleConfirmCancel}
        isLoading={cancelOrderMutation.isPending}
      />

      <RejectRequestModal
        isOpen={rejectModalState.isOpen}
        orderNumber={rejectModalState.orderNumber}
        type={rejectModalState.type}
        onClose={() =>
          setRejectModalState((prev) => ({ ...prev, isOpen: false }))
        }
        onConfirm={handleConfirmReject}
        isLoading={
          rejectModalState.type === "CANCELLATION"
            ? processCancellationMutation.isPending
            : processReturnMutation.isPending
        }
      />

      <PrintShippingLabelModal
        order={selectedOrder}
        isOpen={isPrintLabelOpen}
        onClose={() => setIsPrintLabelOpen(false)}
      />
    </div>
  );
}
