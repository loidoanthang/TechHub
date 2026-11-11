"use client";

import React, { useState, useEffect, useMemo } from "react";
import {
  useSellerVouchersQuery,
  useUpdateVoucherStatusMutation,
  useDeleteVoucherMutation,
} from "@/features/seller/hooks/useSellerVouchers";
import type {
  ShopVoucherResponse,
  ShopVoucherDetailResponse,
  VoucherStatus,
  DiscountType,
  VoucherAppliesTo,
} from "@/types/sellerVoucher";
import { SellerVouchersHeader } from "./SellerVouchersHeader";
import { SellerVoucherTabs } from "./SellerVoucherTabs";
import { SellerVouchersTable } from "./SellerVouchersTable";
import { VoucherFormModal } from "./VoucherFormModal";
import { VoucherDeleteConfirmModal } from "./VoucherDeleteConfirmModal";

export function SellerVouchersView() {
  // Query Filters & State
  const [page, setPage] = useState(0);
  const size = 10;
  const [status, setStatus] = useState<VoucherStatus | undefined>(undefined);
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [discountType, setDiscountType] = useState<DiscountType | undefined>(undefined);
  const [appliesTo, setAppliesTo] = useState<VoucherAppliesTo | undefined>(undefined);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [keyword]);

  // Handle status tab change
  const handleStatusChange = (newStatus?: VoucherStatus) => {
    setStatus(newStatus);
    setPage(0);
  };

  // Queries
  const queryParams = useMemo(() => {
    return {
      keyword: debouncedKeyword || undefined,
      status: status || undefined,
      discountType: discountType || undefined,
      appliesTo: appliesTo || undefined,
      page,
      size,
      sortBy: "createdAt",
      sortDir: "desc" as const,
    };
  }, [debouncedKeyword, status, discountType, appliesTo, page]);

  const { data: pageData, isLoading } = useSellerVouchersQuery(queryParams);
  const vouchers = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  // Query un-filtered list for tab counts & header KPIs
  const { data: allVouchersData } = useSellerVouchersQuery({
    page: 0,
    size: 100,
  });
  const allVouchers = allVouchersData?.content || [];

  // Compute status counts for tabs
  const statusCounts = useMemo(() => {
    return {
      all: allVouchers.length,
      ongoing: allVouchers.filter((v) => v.status === "ONGOING").length,
      upcoming: allVouchers.filter((v) => v.status === "UPCOMING").length,
      expired: allVouchers.filter((v) => v.status === "EXPIRED").length,
      disabled: allVouchers.filter((v) => v.status === "DISABLED").length,
    };
  }, [allVouchers]);

  // Mutations
  const updateStatusMutation = useUpdateVoucherStatusMutation();
  const deleteMutation = useDeleteVoucherMutation();
  const [updatingVoucherId, setUpdatingVoucherId] = useState<string | null>(null);

  // Modal States
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState<ShopVoucherDetailResponse | null>(null);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deletingVoucher, setDeletingVoucher] = useState<ShopVoucherResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Handlers
  const handleOpenCreateModal = () => {
    setEditingVoucher(null);
    setIsFormModalOpen(true);
  };

  const handleEditVoucher = (voucher: ShopVoucherResponse) => {
    setEditingVoucher(voucher as ShopVoucherDetailResponse);
    setIsFormModalOpen(true);
  };

  const handleToggleStatus = async (voucher: ShopVoucherResponse) => {
    setUpdatingVoucherId(voucher.id);
    try {
      await updateStatusMutation.mutateAsync({
        id: voucher.id,
        data: { isActive: !voucher.isActive },
      });
    } catch (err) {
      console.error("Toggle voucher status error:", err);
    } finally {
      setUpdatingVoucherId(null);
    }
  };

  const handleOpenDeleteModal = (voucher: ShopVoucherResponse) => {
    setDeletingVoucher(voucher);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deletingVoucher) return;
    setIsDeleting(true);
    try {
      await deleteMutation.mutateAsync(deletingVoucher.id);
      setIsDeleteModalOpen(false);
      setDeletingVoucher(null);
    } catch (err) {
      console.error("Delete voucher error:", err);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full pb-16">
      {/* 1. Header & KPI Cards */}
      <SellerVouchersHeader
        vouchers={allVouchers}
        onOpenCreateModal={handleOpenCreateModal}
      />

      {/* 2. Status Tabs & Filters */}
      <SellerVoucherTabs
        currentStatus={status}
        onStatusChange={handleStatusChange}
        keyword={keyword}
        onKeywordChange={setKeyword}
        discountType={discountType}
        onDiscountTypeChange={(dt) => {
          setDiscountType(dt);
          setPage(0);
        }}
        appliesTo={appliesTo}
        onAppliesToChange={(at) => {
          setAppliesTo(at);
          setPage(0);
        }}
        statusCounts={statusCounts}
      />

      {/* 3. Vouchers Data Table */}
      <SellerVouchersTable
        vouchers={vouchers}
        isLoading={isLoading}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        size={size}
        onPageChange={setPage}
        onEdit={handleEditVoucher}
        onDelete={handleOpenDeleteModal}
        onToggleStatus={handleToggleStatus}
        updatingVoucherId={updatingVoucherId}
        onOpenCreateModal={handleOpenCreateModal}
      />

      {/* 4. Voucher Form Modal (Create & Edit) */}
      <VoucherFormModal
        isOpen={isFormModalOpen}
        onClose={() => {
          setIsFormModalOpen(false);
          setEditingVoucher(null);
        }}
        initialData={editingVoucher}
        shopName="TechHub Flagship Store"
      />

      {/* 5. Voucher Delete Confirmation Modal */}
      <VoucherDeleteConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setDeletingVoucher(null);
        }}
        onConfirm={handleConfirmDelete}
        voucherCode={deletingVoucher?.code}
        voucherName={deletingVoucher?.name}
        isLoading={isDeleting}
      />
    </div>
  );
}
