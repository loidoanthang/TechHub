"use client";

import React, { useState, useCallback, useMemo } from "react";
import type {
  SellerProductSummaryResponse,
  SellerProductQueryParams,
  SellerProductTab,
  SellerProductSortBy,
  SortDirection,
} from "@/types/sellerProduct";
import {
  useSellerProductsQuery,
  useSellerCategoriesQuery,
  useUpdateProductVisibilityMutation,
  useSubmitProductApprovalMutation,
  useDeleteProductMutation,
} from "../../hooks/useSellerProducts";
import { SellerProductsHeader } from "./SellerProductsHeader";
import { SellerProductsTabs } from "./SellerProductsTabs";
import { SellerProductsTable } from "./SellerProductsTable";
import { SubmitApprovalModal, DeleteProductModal } from "./SellerProductActionModals";
import { AlertCircle, CheckCircle2 } from "lucide-react";

export function SellerProductsView() {
  // 1. Filter States
  const [keyword, setKeyword] = useState<string>("");
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | undefined>(undefined);
  const [activeTab, setActiveTab] = useState<SellerProductTab>("ALL");
  const [sortBy, setSortBy] = useState<SellerProductSortBy>("createdAt");
  const [sortDir, setSortDir] = useState<SortDirection>("desc");
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(20);

  // 2. Modals state
  const [submitApprovalTarget, setSubmitApprovalTarget] = useState<SellerProductSummaryResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<SellerProductSummaryResponse | null>(null);
  const [updatingProductId, setUpdatingProductId] = useState<string | null>(null);
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // 3. Compute API query params based on activeTab
  const queryParams = useMemo<SellerProductQueryParams>(() => {
    const params: SellerProductQueryParams = {
      page,
      size,
      sortBy,
      sortDir,
    };

    if (keyword) params.keyword = keyword;
    if (selectedCategoryId) params.categoryId = selectedCategoryId;

    switch (activeTab) {
      case "PUBLISHED":
        params.visibilityStatus = "PUBLISHED";
        params.approvalStatus = "APPROVED";
        break;
      case "PENDING_APPROVAL":
        params.approvalStatus = "PENDING_APPROVAL";
        break;
      case "DRAFT_REJECTED":
        params.approvalStatus = "DRAFT";
        break;
      case "UNPUBLISHED":
        params.visibilityStatus = "UNPUBLISHED";
        break;
      case "SUSPENDED":
        params.isSuspended = true;
        break;
      case "ALL":
      default:
        break;
    }

    return params;
  }, [keyword, selectedCategoryId, activeTab, sortBy, sortDir, page, size]);

  // 4. Data Fetching
  const { data: pageData, isLoading, error } = useSellerProductsQuery(queryParams);
  const { data: categories = [] } = useSellerCategoriesQuery();

  // 5. Mutations
  const updateVisibilityMutation = useUpdateProductVisibilityMutation();
  const submitApprovalMutation = useSubmitProductApprovalMutation();
  const deleteProductMutation = useDeleteProductMutation();

  // Handlers
  const handleKeywordChange = useCallback((newKw: string) => {
    setKeyword(newKw);
    setPage(0);
  }, []);

  const handleCategoryChange = useCallback((newCatId: string | undefined) => {
    setSelectedCategoryId(newCatId);
    setPage(0);
  }, []);

  const handleTabChange = useCallback((newTab: SellerProductTab) => {
    setActiveTab(newTab);
    setPage(0);
  }, []);

  const handleSortChange = useCallback((newSortBy: SellerProductSortBy, newSortDir: SortDirection) => {
    setSortBy(newSortBy);
    setSortDir(newSortDir);
    setPage(0);
  }, []);

  const handleResetFilters = useCallback(() => {
    setKeyword("");
    setSelectedCategoryId(undefined);
    setActiveTab("ALL");
    setSortBy("createdAt");
    setSortDir("desc");
    setPage(0);
  }, []);

  // Action: Toggle Visibility (PUBLISHED <-> UNPUBLISHED)
  const handleToggleVisibility = useCallback(
    async (productId: string, currentStatus: "PUBLISHED" | "UNPUBLISHED") => {
      const nextStatus = currentStatus === "PUBLISHED" ? "UNPUBLISHED" : "PUBLISHED";
      setUpdatingProductId(productId);
      setFeedbackMsg(null);

      try {
        await updateVisibilityMutation.mutateAsync({
          productId,
          data: { visibilityStatus: nextStatus },
        });
        setFeedbackMsg({
          type: "success",
          text: nextStatus === "PUBLISHED" ? "Đã mở bán sản phẩm thành công trên sàn." : "Đã tạm ẩn sản phẩm khỏi sàn.",
        });
      } catch (err: unknown) {
        const errorResponse = err as { response?: { data?: { message?: string } } };
        const msg = errorResponse?.response?.data?.message || "Không thể cập nhật trạng thái hiển thị của sản phẩm.";
        setFeedbackMsg({ type: "error", text: msg });
      } finally {
        setUpdatingProductId(null);
      }
    },
    [updateVisibilityMutation]
  );

  // Action: Submit for approval
  const handleConfirmSubmitApproval = useCallback(async () => {
    if (!submitApprovalTarget) return;
    setFeedbackMsg(null);

    try {
      await submitApprovalMutation.mutateAsync(submitApprovalTarget.id);
      setFeedbackMsg({
        type: "success",
        text: `Đã gửi duyệt sản phẩm "${submitApprovalTarget.name}" thành công. Ban Quản Trị sẽ kiểm tra trong 24h.`,
      });
      setSubmitApprovalTarget(null);
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } } };
      const msg = errorResponse?.response?.data?.message || "Không thể gửi duyệt sản phẩm. Vui lòng kiểm tra lại.";
      setFeedbackMsg({ type: "error", text: msg });
    }
  }, [submitApprovalTarget, submitApprovalMutation]);

  // Action: Delete product
  const handleConfirmDelete = useCallback(async () => {
    if (!deleteTarget) return;
    setFeedbackMsg(null);

    try {
      await deleteProductMutation.mutateAsync(deleteTarget.id);
      setFeedbackMsg({
        type: "success",
        text: `Đã xóa sản phẩm "${deleteTarget.name}" thành công.`,
      });
      setDeleteTarget(null);
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } } };
      const msg = errorResponse?.response?.data?.message || "Không thể xóa sản phẩm.";
      setFeedbackMsg({ type: "error", text: msg });
    }
  }, [deleteTarget, deleteProductMutation]);

  const products = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full">
      {/* Feedback Alert Toast/Banner */}
      {feedbackMsg && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs animate-in fade-in duration-200 ${
            feedbackMsg.type === "success"
              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
              : "bg-rose-50 text-rose-800 border-rose-200"
          }`}
        >
          <div className="flex items-center gap-2">
            {feedbackMsg.type === "success" ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span>{feedbackMsg.text}</span>
          </div>
          <button
            onClick={() => setFeedbackMsg(null)}
            className="text-slate-400 hover:text-slate-600 ml-4 font-semibold"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Header with Title and Search/Filters Toolbar */}
      <SellerProductsHeader
        keyword={keyword}
        onKeywordChange={handleKeywordChange}
        selectedCategoryId={selectedCategoryId}
        onCategoryChange={handleCategoryChange}
        categories={categories}
        sortBy={sortBy}
        sortDir={sortDir}
        onSortChange={handleSortChange}
        onResetFilters={handleResetFilters}
        totalElements={totalElements}
      />

      {/* Tabs matching SellerOrdersTabs standard */}
      <SellerProductsTabs
        activeTab={activeTab}
        onTabChange={handleTabChange}
      />

      {/* Table Container */}
      <SellerProductsTable
        products={products}
        isLoading={isLoading}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        size={size}
        onPageChange={setPage}
        onPageSizeChange={(newSize) => {
          setSize(newSize);
          setPage(0);
        }}
        onToggleVisibility={handleToggleVisibility}
        onSubmitApproval={setSubmitApprovalTarget}
        onDeleteProduct={setDeleteTarget}
        updatingProductId={updatingProductId}
      />

      {/* Modals */}
      <SubmitApprovalModal
        isOpen={!!submitApprovalTarget}
        onClose={() => setSubmitApprovalTarget(null)}
        onConfirm={handleConfirmSubmitApproval}
        productName={submitApprovalTarget?.name || ""}
        isLoading={submitApprovalMutation.isPending}
      />

      <DeleteProductModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        productName={deleteTarget?.name || ""}
        isLoading={deleteProductMutation.isPending}
      />
    </div>
  );
}
