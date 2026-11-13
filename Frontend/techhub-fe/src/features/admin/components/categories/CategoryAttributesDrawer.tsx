"use client";

import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  Plus,
  Edit,
  Trash2,
  Loader2,
  AlertCircle,
  ListChecks,
} from "lucide-react";
import {
  useAdminCategoryDetailQuery,
  useCreateCategoryAttributeMutation,
  useUpdateCategoryAttributeMutation,
  useDeleteCategoryAttributeMutation,
} from "../../hooks/useAdminOperations";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import {
  categoryAttributeSchema,
  type CategoryAttributeFormValues,
} from "../../schemas/categorySchema";
import type { CategoryAttributeResponse } from "@/types/adminCatalog";

interface CategoryAttributesDrawerProps {
  categoryId: string | null;
  onClose: () => void;
  onActionSuccess?: (msg: string) => void;
}

export function CategoryAttributesDrawer({
  categoryId,
  onClose,
  onActionSuccess,
}: CategoryAttributesDrawerProps) {
  const { data: category, isLoading, isError, refetch } =
    useAdminCategoryDetailQuery(categoryId);

  const createAttrMutation = useCreateCategoryAttributeMutation();
  const updateAttrMutation = useUpdateCategoryAttributeMutation();
  const deleteAttrMutation = useDeleteCategoryAttributeMutation();

  const [editingAttr, setEditingAttr] =
    useState<CategoryAttributeResponse | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  useAdminDialogA11y(Boolean(categoryId), onClose);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<CategoryAttributeFormValues>({
    resolver: zodResolver(categoryAttributeSchema),
    defaultValues: {
      name: "",
      dataType: "TEXT",
      optionsText: "",
      sampleValue: "",
    },
  });

  if (!categoryId) return null;

  const watchedType = watch("dataType");

  const openAdd = () => {
    setEditingAttr(null);
    setServerError(null);
    reset({
      name: "",
      dataType: "TEXT",
      optionsText: "",
      sampleValue: "",
    });
    setShowAddForm(true);
  };

  const openEdit = (attr: CategoryAttributeResponse) => {
    setEditingAttr(attr);
    setServerError(null);
    reset({
      name: attr.name,
      dataType: attr.dataType,
      optionsText: (attr.options || []).join(", "),
      sampleValue: attr.sampleValue || "",
    });
    setShowAddForm(true);
  };

  const handleAttrSubmit = async (values: CategoryAttributeFormValues) => {
    setServerError(null);
    const payload = {
      name: values.name.trim(),
      dataType: values.dataType,
      options:
        values.dataType === "SELECT"
          ? (values.optionsText || "")
              .split(",")
              .map((s) => s.trim())
              .filter(Boolean)
          : undefined,
      sampleValue: values.sampleValue?.trim() || undefined,
    };

    try {
      if (editingAttr) {
        await updateAttrMutation.mutateAsync({
          categoryId,
          attributeId: editingAttr.id,
          data: payload,
        });
        onActionSuccess?.(`Đã cập nhật thuộc tính "${values.name}".`);
      } else {
        await createAttrMutation.mutateAsync({
          categoryId,
          data: payload,
        });
        onActionSuccess?.(`Đã thêm thuộc tính "${values.name}".`);
      }
      setShowAddForm(false);
      setEditingAttr(null);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Lưu thuộc tính thất bại.";
      setServerError(msg);
    }
  };

  const handleDeleteAttr = async (attr: CategoryAttributeResponse) => {
    setServerError(null);
    try {
      await deleteAttrMutation.mutateAsync({
        categoryId,
        attributeId: attr.id,
      });
      onActionSuccess?.(`Đã xóa thuộc tính "${attr.name}".`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Không thể xóa thuộc tính này.";
      setServerError(msg);
    }
  };

  const attrs = category?.attributes || [];

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="category-attrs-drawer-title"
      className="fixed inset-0 z-50 flex justify-end"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={onClose}
      />

      <div className="relative z-10 w-full max-w-xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-200 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-xl bg-surface-container-low text-primary flex items-center justify-center shrink-0">
              <ListChecks className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <h2
                id="category-attrs-drawer-title"
                className="font-heading text-base font-extrabold text-on-surface truncate"
              >
                Thuộc tính Kỹ thuật Danh mục
              </h2>
              <p className="text-[11px] text-slate-600 truncate">
                {category?.name || "Đang tải..."}
              </p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng bảng thuộc tính"
            onClick={onClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5 flex flex-col gap-4">
          {serverError && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-7 h-7 text-[#0045cc] animate-spin" />
              <span className="text-xs text-slate-500">
                Đang tải thuộc tính...
              </span>
            </div>
          ) : isError || !category ? (
            <div className="p-8 text-center flex flex-col items-center gap-3">
              <AlertCircle className="w-8 h-8 text-rose-500" />
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
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-700">
                  Danh sách thuộc tính ({attrs.length})
                </span>
                {!showAddForm && (
                  <button
                    type="button"
                    onClick={openAdd}
                    className="px-3 py-1.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Thêm thuộc tính</span>
                  </button>
                )}
              </div>

              {/* Add / Edit Inline Form */}
              {showAddForm && (
                <form
                  onSubmit={handleSubmit(handleAttrSubmit)}
                  className="p-4 rounded-2xl bg-[#eff4ff]/70 border border-[#0045cc]/20 flex flex-col gap-3 animate-in fade-in"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-extrabold text-[#0b1c30]">
                      {editingAttr
                        ? `Chỉnh sửa "${editingAttr.name}"`
                        : "Thêm thuộc tính kỹ thuật mới"}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setShowAddForm(false);
                        setEditingAttr(null);
                      }}
                      className="text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
                    >
                      Đóng
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    <div className="flex flex-col gap-1">
                      <input
                        type="text"
                        {...register("name")}
                        placeholder="Tên thuộc tính *"
                        className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                      />
                      {errors.name && (
                        <p className="text-[10px] text-rose-600">
                          {errors.name.message}
                        </p>
                      )}
                    </div>

                    <select
                      {...register("dataType")}
                      className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs font-semibold"
                    >
                      <option value="TEXT">TEXT (Văn bản)</option>
                      <option value="NUMBER">NUMBER (Số)</option>
                      <option value="SELECT">SELECT (Danh sách chọn)</option>
                    </select>
                  </div>

                  {watchedType === "SELECT" && (
                    <div className="flex flex-col gap-1">
                      <input
                        type="text"
                        {...register("optionsText")}
                        placeholder="Các lựa chọn (ngăn cách dấu phẩy)"
                        className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                      />
                      {errors.optionsText && (
                        <p className="text-[10px] text-rose-600">
                          {errors.optionsText.message}
                        </p>
                      )}
                    </div>
                  )}

                  <input
                    type="text"
                    {...register("sampleValue")}
                    placeholder="Giá trị mẫu gợi ý cho Seller (không bắt buộc)"
                    className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                  />

                  <div className="flex justify-end gap-2">
                    <button
                      type="submit"
                      disabled={
                        createAttrMutation.isPending ||
                        updateAttrMutation.isPending
                      }
                      className="px-4 py-2 rounded-xl bg-[#0045cc] text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                    >
                      {(createAttrMutation.isPending ||
                        updateAttrMutation.isPending) && (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      )}
                      <span>{editingAttr ? "Cập nhật" : "Thêm mới"}</span>
                    </button>
                  </div>
                </form>
              )}

              {/* Attribute Cards */}
              <div className="flex flex-col gap-2.5">
                {attrs.map((attr) => (
                  <div
                    key={attr.id}
                    className="p-3.5 rounded-xl border border-slate-200 bg-white flex items-start justify-between gap-3"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-xs text-slate-900">
                          {attr.name}
                        </span>
                        <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px] font-mono font-bold">
                          {attr.dataType}
                        </span>
                      </div>
                      {attr.sampleValue && (
                        <p className="text-[11px] text-slate-500 mt-1">
                          Mẫu: {attr.sampleValue}
                        </p>
                      )}
                      {attr.options && attr.options.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1.5">
                          {attr.options.map((opt) => (
                            <span
                              key={opt}
                              className="px-2 py-0.5 rounded-md bg-[#eff4ff] text-[#0045cc] text-[10px] font-medium"
                            >
                              {opt}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        type="button"
                        onClick={() => openEdit(attr)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-[#0045cc] hover:bg-[#eff4ff] cursor-pointer"
                        title="Sửa thuộc tính"
                      >
                        <Edit className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        disabled={attrs.length <= 1 || deleteAttrMutation.isPending}
                        onClick={() => handleDeleteAttr(attr)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 disabled:opacity-30 cursor-pointer"
                        title={
                          attrs.length <= 1
                            ? "Không thể xóa thuộc tính cuối cùng"
                            : "Xóa thuộc tính"
                        }
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
