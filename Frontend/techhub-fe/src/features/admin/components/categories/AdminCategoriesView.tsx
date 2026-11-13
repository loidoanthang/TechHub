"use client";

import React, { useState, useEffect } from "react";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  ListChecks,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
} from "lucide-react";
import {
  useAdminCategoriesQuery,
  useUpdateCategoryStatusMutation,
  useDeleteCategoryMutation,
} from "../../hooks/useAdminOperations";
import { CategoryFormModal } from "./CategoryFormModal";
import { CategoryAttributesDrawer } from "./CategoryAttributesDrawer";
import { AdminTablePagination } from "../shared/AdminTablePagination";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import type { CategoryStatus } from "@/types/enums";
import type { CategorySummaryResponse } from "@/types/adminCatalog";

export function AdminCategoriesView() {
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<CategoryStatus | "">("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);

  const [formModalOpen, setFormModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] =
    useState<CategorySummaryResponse | null>(null);
  const [attrDrawerCategoryId, setAttrDrawerCategoryId] = useState<
    string | null
  >(null);
  const [hibernateConfirmCat, setHibernateConfirmCat] =
    useState<CategorySummaryResponse | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useAdminDialogA11y(Boolean(hibernateConfirmCat), () =>
    setHibernateConfirmCat(null)
  );

  useEffect(() => {
    if (keyword === debouncedKeyword) return;
    const t = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [keyword, debouncedKeyword]);

  const { data, isLoading, isError, refetch } = useAdminCategoriesQuery({
    keyword: debouncedKeyword || undefined,
    status: statusFilter,
    page,
    size,
  });

  const statusMutation = useUpdateCategoryStatusMutation();
  const deleteMutation = useDeleteCategoryMutation();

  const handleSuccess = (msg: string) => {
    setErrorMsg(null);
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 4000);
  };

  const handleToggleStatus = async (cat: CategorySummaryResponse) => {
    if (cat.status === "ACTIVE") {
      // Show warning modal for Forced Hibernation policy
      setHibernateConfirmCat(cat);
      return;
    }

    try {
      await statusMutation.mutateAsync({
        id: cat.id,
        data: { status: "ACTIVE" },
      });
      handleSuccess(`Đã kích hoạt danh mục "${cat.name}".`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật trạng thái thất bại.";
      setErrorMsg(msg);
    }
  };

  const confirmHibernate = async () => {
    if (!hibernateConfirmCat) return;
    try {
      await statusMutation.mutateAsync({
        id: hibernateConfirmCat.id,
        data: { status: "INACTIVE" },
      });
      handleSuccess(
        `Đã tạm ngưng danh mục "${hibernateConfirmCat.name}" (chính sách Ngủ đông).`
      );
      setHibernateConfirmCat(null);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Thao tác thất bại.";
      setErrorMsg(msg);
      setHibernateConfirmCat(null);
    }
  };

  const handleDeleteCategory = async (cat: CategorySummaryResponse) => {
    setErrorMsg(null);
    try {
      await deleteMutation.mutateAsync(cat.id);
      handleSuccess(`Đã xóa danh mục "${cat.name}".`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        "Không thể xóa danh mục này (vẫn còn sản phẩm trực thuộc).";
      setErrorMsg(msg);
    }
  };

  const categories = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="flex-1 w-full p-4 sm:p-6 flex flex-col gap-4 max-w-[1440px] mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="font-heading text-lg font-bold text-[#0b1c30]">
            Quản lý Danh mục & Thuộc tính
          </h1>
          <p className="text-[11px] text-slate-500">
            Quản lý cây danh mục và bộ thông số kỹ thuật động cho từng nhóm linh kiện.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setEditingCategory(null);
            setFormModalOpen(true);
          }}
          className="h-10 px-4 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-1.5 shadow-xs self-start sm:self-auto cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Tạo Danh mục Mới</span>
        </button>
      </div>

      {/* Feedbacks */}
      {toastMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}
      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 sm:p-4 shadow-2xs flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <div className="flex flex-col sm:flex-row gap-3 flex-1">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              aria-label="Tìm theo tên hoặc mô tả danh mục"
              placeholder="Tìm theo tên hoặc mô tả danh mục..."
              className="w-full h-10 pl-9 pr-4 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-500 focus:outline-none focus:border-primary"
            />
          </div>

          <select
            value={statusFilter}
            aria-label="Lọc danh mục theo trạng thái"
            onChange={(e) => {
              setStatusFilter(e.target.value as CategoryStatus | "");
              setPage(0);
            }}
            className="h-10 px-3.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 bg-white sm:w-52"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
            <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
          </select>
        </div>

        <AdminTablePagination
          variant="top-mini"
          page={page}
          size={size}
          totalElements={data?.totalElements || 0}
          totalPages={data?.totalPages || 1}
          onPageChange={setPage}
          isLoading={isLoading}
          className="self-end sm:self-auto"
        />
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 flex flex-col items-center justify-center gap-3 min-h-[300px]">
          <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
          <span className="text-xs text-slate-500">
            Đang tải danh sách danh mục...
          </span>
        </div>
      ) : isError ? (
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center flex flex-col items-center gap-3">
          <AlertCircle className="w-8 h-8 text-rose-500" />
          <button
            type="button"
            onClick={() => refetch()}
            className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold"
          >
            Thử lại
          </button>
        </div>
      ) : categories.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-xs text-slate-500">
          Chưa có danh mục nào phù hợp.
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col">
          <div className="overflow-x-auto no-scrollbar">
            <table className="w-full min-w-[760px] text-left text-xs border-collapse table-fixed">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                <tr>
                  <th className="py-2.5 px-4 w-[28%]">Tên danh mục</th>
                  <th className="py-2.5 px-3 w-[30%]">Mô tả</th>
                  <th className="py-2.5 px-2 w-[12%] text-center">Thuộc tính</th>
                  <th className="py-2.5 px-2 w-[12%] text-center">Trạng thái</th>
                  <th className="py-2.5 px-4 w-[18%] text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {categories.map((cat) => (
                  <tr key={cat.id} className="hover:bg-slate-50/70">
                    <td className="py-2 px-4 font-bold text-slate-900">
                      {cat.name}
                    </td>
                    <td className="py-2 px-3 text-slate-500 truncate">
                      {cat.description || "—"}
                    </td>
                    <td className="py-2 px-2 text-center">
                      <button
                        type="button"
                        onClick={() => setAttrDrawerCategoryId(cat.id)}
                        className="px-2 py-1 rounded-lg bg-[#eff4ff] text-[#0045cc] hover:bg-[#0045cc] hover:text-white text-[11px] font-bold inline-flex items-center gap-1 transition-colors duration-150 cursor-pointer"
                      >
                        <ListChecks className="w-3.5 h-3.5" />
                        <span>{cat.attributesCount} mẫu</span>
                      </button>
                    </td>
                    <td className="py-2 px-2 text-center">
                      <button
                        type="button"
                        onClick={() => handleToggleStatus(cat)}
                        disabled={statusMutation.isPending}
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold cursor-pointer transition-colors ${
                          cat.status === "ACTIVE"
                            ? "bg-emerald-100 text-emerald-700 hover:bg-emerald-200"
                            : "bg-slate-200 text-slate-600 hover:bg-slate-300"
                        }`}
                        title="Bấm để chuyển đổi trạng thái"
                      >
                        {cat.status}
                      </button>
                    </td>
                    <td className="py-2 px-4 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => {
                            setEditingCategory(cat);
                            setFormModalOpen(true);
                          }}
                          className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-slate-600 hover:text-primary hover:bg-surface-container-low cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                          title="Sửa tên/mô tả"
                          aria-label={`Sửa danh mục ${cat.name}`}
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteCategory(cat)}
                          disabled={deleteMutation.isPending}
                          className="min-w-[34px] min-h-[34px] inline-flex items-center justify-center rounded-lg text-slate-600 hover:text-rose-600 hover:bg-rose-50 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-600"
                          title="Xóa danh mục"
                          aria-label={`Xóa danh mục ${cat.name}`}
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

          {/* Bottom Pagination */}
          <AdminTablePagination
            variant="bottom-full"
            page={page}
            size={size}
            totalElements={data?.totalElements || 0}
            totalPages={data?.totalPages || 1}
            onPageChange={setPage}
            onSizeChange={setSize}
            sizeOptions={[5, 10, 25, 50]}
            isLoading={isLoading}
          />
        </div>
      )}

      {/* Hibernation Confirmation Modal */}
      {hibernateConfirmCat && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="hibernate-cat-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            className="fixed inset-0 bg-slate-950/50"
            onClick={() => setHibernateConfirmCat(null)}
          />
          <div className="relative z-10 w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-amber-200 flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3
                  id="hibernate-cat-title"
                  className="font-heading text-base font-extrabold text-on-surface"
                >
                  Xác nhận Ngủ đông Danh mục
                </h3>
                <p className="text-xs text-slate-500">
                  {hibernateConfirmCat.name}
                </p>
              </div>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              Theo chính sách <strong>Ngủ đông Cưỡng chế</strong>, khi chuyển danh mục sang <code>INACTIVE</code>, toàn bộ sản phẩm trực thuộc sẽ tự động chuyển về trạng thái <strong>DRAFT</strong> và <strong>UNPUBLISHED</strong> (ẩn khỏi sàn). Bạn có chắc chắn muốn tiếp tục?
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setHibernateConfirmCat(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={confirmHibernate}
                disabled={statusMutation.isPending}
                className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
              >
                {statusMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                )}
                <span>Xác nhận Ngưng hoạt động</span>
              </button>
            </div>
          </div>
        </div>
      )}

      <CategoryFormModal
        isOpen={formModalOpen}
        editingCategory={editingCategory}
        onClose={() => {
          setFormModalOpen(false);
          setEditingCategory(null);
        }}
        onSuccess={handleSuccess}
      />

      <CategoryAttributesDrawer
        categoryId={attrDrawerCategoryId}
        onClose={() => setAttrDrawerCategoryId(null)}
        onActionSuccess={handleSuccess}
      />
    </div>
  );
}
