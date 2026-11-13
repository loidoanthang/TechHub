"use client";

import React, { useState, useEffect } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  X,
  Plus,
  Trash2,
  Loader2,
  AlertCircle,
  FolderTree,
} from "lucide-react";
import {
  createCategorySchema,
  updateCategorySchema,
  type CreateCategoryFormValues,
  type UpdateCategoryFormValues,
} from "../../schemas/categorySchema";
import {
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
} from "../../hooks/useAdminOperations";
import { useAdminDialogA11y } from "../../hooks/useAdminDialogA11y";
import type { CategorySummaryResponse } from "@/types/adminCatalog";

interface CategoryFormModalProps {
  isOpen: boolean;
  editingCategory: CategorySummaryResponse | null;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export function CategoryFormModal({
  isOpen,
  editingCategory,
  onClose,
  onSuccess,
}: CategoryFormModalProps) {
  const isEdit = Boolean(editingCategory);
  const createMutation = useCreateCategoryMutation();
  const updateMutation = useUpdateCategoryMutation();
  const [serverError, setServerError] = useState<string | null>(null);

  useAdminDialogA11y(isOpen, onClose);

  const createForm = useForm<CreateCategoryFormValues>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: {
      name: "",
      description: "",
      attributes: [
        { name: "", dataType: "TEXT", optionsText: "", sampleValue: "" },
      ],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control: createForm.control,
    name: "attributes",
  });

  const updateForm = useForm<UpdateCategoryFormValues>({
    resolver: zodResolver(updateCategorySchema),
    defaultValues: {
      name: "",
      description: "",
    },
  });

  const { reset: resetCreateForm } = createForm;
  const { reset: resetUpdateForm } = updateForm;

  useEffect(() => {
    if (isOpen) {
      setServerError(null);
      if (editingCategory) {
        resetUpdateForm({
          name: editingCategory.name,
          description: editingCategory.description || "",
        });
      } else {
        resetCreateForm({
          name: "",
          description: "",
          attributes: [
            { name: "", dataType: "TEXT", optionsText: "", sampleValue: "" },
          ],
        });
      }
    }
  }, [isOpen, editingCategory, resetCreateForm, resetUpdateForm]);

  if (!isOpen) return null;

  const onCreateSubmit = async (values: CreateCategoryFormValues) => {
    setServerError(null);
    try {
      await createMutation.mutateAsync({
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        attributes: values.attributes.map((attr) => ({
          name: attr.name.trim(),
          dataType: attr.dataType,
          options:
            attr.dataType === "SELECT"
              ? (attr.optionsText || "")
                  .split(",")
                  .map((s) => s.trim())
                  .filter(Boolean)
              : undefined,
          sampleValue: attr.sampleValue?.trim() || undefined,
        })),
      });
      onSuccess(`Đã tạo danh mục "${values.name}" thành công!`);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Tạo danh mục thất bại.";
      setServerError(msg);
    }
  };

  const onUpdateSubmit = async (values: UpdateCategoryFormValues) => {
    if (!editingCategory) return;
    setServerError(null);
    try {
      await updateMutation.mutateAsync({
        id: editingCategory.id,
        data: {
          name: values.name.trim(),
          description: values.description?.trim() || undefined,
        },
      });
      onSuccess(`Đã cập nhật thông tin danh mục "${values.name}".`);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Cập nhật danh mục thất bại.";
      setServerError(msg);
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="category-form-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div
        className="fixed inset-0 bg-slate-950/50"
        onClick={onClose}
      />

      <div className="relative z-10 w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-slate-200 max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 motion-reduce:animate-none">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-surface-container-low text-primary flex items-center justify-center">
              <FolderTree className="w-4 h-4" />
            </div>
            <h2
              id="category-form-modal-title"
              className="font-heading text-base font-extrabold text-on-surface"
            >
              {isEdit ? "Chỉnh sửa Danh mục" : "Tạo Danh mục Sản phẩm Mới"}
            </h2>
          </div>
          <button
            type="button"
            aria-label="Đóng hộp thoại"
            onClick={onClose}
            className="min-w-[40px] min-h-[40px] inline-flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-200/70 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-6">
          {serverError && (
            <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{serverError}</span>
            </div>
          )}

          {isEdit ? (
            <form
              id="category-edit-form"
              onSubmit={updateForm.handleSubmit(onUpdateSubmit)}
              className="flex flex-col gap-4"
            >
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Tên danh mục <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  {...updateForm.register("name")}
                  className="w-full h-10 px-3.5 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
                {updateForm.formState.errors.name && (
                  <p className="text-[11px] text-rose-600">
                    {updateForm.formState.errors.name.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Mô tả ngắn
                </label>
                <textarea
                  rows={3}
                  {...updateForm.register("description")}
                  className="w-full p-3 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
                {updateForm.formState.errors.description && (
                  <p className="text-[11px] text-rose-600">
                    {updateForm.formState.errors.description.message}
                  </p>
                )}
              </div>
            </form>
          ) : (
            <form
              id="category-create-form"
              onSubmit={createForm.handleSubmit(onCreateSubmit)}
              className="flex flex-col gap-5"
            >
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Tên danh mục <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  {...createForm.register("name")}
                  placeholder="VD: Bàn phím cơ Custom, Card đồ họa GPU..."
                  className="w-full h-10 px-3.5 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
                {createForm.formState.errors.name && (
                  <p className="text-[11px] text-rose-600">
                    {createForm.formState.errors.name.message}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-700">
                  Mô tả danh mục
                </label>
                <textarea
                  rows={2}
                  {...createForm.register("description")}
                  placeholder="Mô tả nhóm thiết bị công nghệ..."
                  className="w-full p-3 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:border-[#0045cc]"
                />
              </div>

              {/* Dynamic Attributes */}
              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <label className="text-xs font-bold text-slate-800">
                      Thuộc tính kỹ thuật mẫu (Tối thiểu 1){" "}
                      <span className="text-rose-500">*</span>
                    </label>
                    <p className="text-[11px] text-slate-500">
                      Định nghĩa các trường thông số kỹ thuật bắt buộc khi Seller đăng sản phẩm thuộc danh mục này.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      append({
                        name: "",
                        dataType: "TEXT",
                        optionsText: "",
                        sampleValue: "",
                      })
                    }
                    className="px-3 py-1.5 rounded-xl bg-[#eff4ff] hover:bg-[#0045cc] text-[#0045cc] hover:text-white text-xs font-bold flex items-center gap-1 transition-colors duration-150 cursor-pointer shrink-0"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Thêm thuộc tính</span>
                  </button>
                </div>

                {createForm.formState.errors.attributes?.message && (
                  <p className="text-[11px] text-rose-600">
                    {createForm.formState.errors.attributes.message}
                  </p>
                )}

                <div className="flex flex-col gap-3">
                  {fields.map((field, index) => {
                    const attrType = createForm.watch(
                      `attributes.${index}.dataType`
                    );
                    const attrErrors =
                      createForm.formState.errors.attributes?.[index];

                    return (
                      <div
                        key={field.id}
                        className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col gap-2.5"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[11px] font-bold text-slate-500">
                            Thuộc tính #{index + 1}
                          </span>
                          {fields.length > 1 && (
                            <button
                              type="button"
                              onClick={() => remove(index)}
                              className="p-1 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 cursor-pointer"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                          <div className="flex flex-col gap-1">
                            <input
                              type="text"
                              {...createForm.register(
                                `attributes.${index}.name`
                              )}
                              placeholder="Tên (VD: Dung lượng VRAM)"
                              className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                            />
                            {attrErrors?.name && (
                              <p className="text-[10px] text-rose-600">
                                {attrErrors.name.message}
                              </p>
                            )}
                          </div>

                          <select
                            {...createForm.register(
                              `attributes.${index}.dataType`
                            )}
                            className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs font-semibold"
                          >
                            <option value="TEXT">Văn bản (TEXT)</option>
                            <option value="NUMBER">Số (NUMBER)</option>
                            <option value="SELECT">Danh sách chọn (SELECT)</option>
                          </select>

                          <input
                            type="text"
                            {...createForm.register(
                              `attributes.${index}.sampleValue`
                            )}
                            placeholder="Mẫu (VD: 16GB GDDR6X)"
                            className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                          />
                        </div>

                        {attrType === "SELECT" && (
                          <div className="flex flex-col gap-1">
                            <input
                              type="text"
                              {...createForm.register(
                                `attributes.${index}.optionsText`
                              )}
                              placeholder="Các lựa chọn ngăn cách dấu phẩy (VD: Đen, Trắng, Bạc)"
                              className="h-9 px-3 rounded-xl border border-slate-200 bg-white text-xs"
                            />
                            {attrErrors?.optionsText && (
                              <p className="text-[10px] text-rose-600">
                                {attrErrors.optionsText.message}
                              </p>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </form>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 bg-slate-50 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="submit"
            form={isEdit ? "category-edit-form" : "category-create-form"}
            disabled={isPending}
            className="px-5 py-2 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold flex items-center gap-2 cursor-pointer"
          >
            {isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>{isEdit ? "Lưu Thay Đổi" : "Tạo Danh Mục"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
