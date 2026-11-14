"use client";

import React, { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ROUTES } from "@/lib/constants";
import {
  useSellerCategoriesQuery,
  useCategoryAttributesQuery,
  useCreateProductMutation,
  useUpdateProductMutation,
  useSellerProductDetailQuery,
  useSubmitProductApprovalMutation,
} from "@/features/seller/hooks/useSellerProducts";
import { sellerProductApi } from "@/features/seller/api/sellerProductApi";
import type { CategorySummaryResponse } from "@/types/category";
import type {
  CreateProductRequest,
  UpdateProductRequest,
  ProductDetailResponse,
} from "@/types/sellerProduct";
import {
  createProductFormSchema,
  type CreateProductFormValues,
} from "@/features/seller/schemas/productFormSchema";
import { ProductBasicInfoCard } from "./ProductBasicInfoCard";
import { ProductMediaGalleryCard } from "./ProductMediaGalleryCard";
import { ProductCategorySpecsCard } from "./ProductCategorySpecsCard";
import { ProductVariantsCard } from "./ProductVariantsCard";
import { ProductFormFooterBar } from "./ProductFormFooterBar";
import { ProductSubmittedModal } from "./ProductSubmittedModal";
import { ProductApprovalWarningModal } from "./ProductApprovalWarningModal";
import { ChevronRight, ArrowLeft, AlertCircle, Loader2, Info } from "lucide-react";

interface ProductFormViewProps {
  productId?: string;
}

/**
 * So sánh dữ liệu cũ và mới để nhận diện loại thay đổi:
 * - isBaseInfoChanged: Đổi Tên, Danh mục, Thương hiệu, Mô tả, Ảnh, Thuộc tính động, hoặc SKU/Tên biến thể
 * - isOperationalChanged: Chỉ đổi Giá bán hoặc Tồn kho của các biến thể
 */
function detectProductChanges(
  oldProduct: ProductDetailResponse,
  newName: string,
  newCategoryId: string,
  newBrandName: string,
  newDescription: string | undefined,
  cleanImages: Array<{ imageUrl: string; displayOrder: number }>,
  cleanAttrs: Record<string, unknown>,
  cleanVariants: Array<{ id?: string; sku: string; variantName: string; price: number; stockQuantity: number; imageUrl?: string }>
) {
  // 1. Base Info
  const nameChanged = (oldProduct.name || "").trim() !== newName.trim();
  const categoryChanged = oldProduct.categoryId !== newCategoryId;
  const brandChanged = (oldProduct.brandName || "").trim() !== newBrandName.trim();
  const descChanged = (oldProduct.description || "").trim() !== (newDescription || "").trim();

  // 2. Images (count, URL, displayOrder)
  const oldImages = oldProduct.images || [];
  let imagesChanged = oldImages.length !== cleanImages.length;
  if (!imagesChanged) {
    for (let i = 0; i < cleanImages.length; i++) {
      if (
        oldImages[i]?.imageUrl !== cleanImages[i].imageUrl ||
        oldImages[i]?.displayOrder !== cleanImages[i].displayOrder
      ) {
        imagesChanged = true;
        break;
      }
    }
  }

  // 3. Dynamic Attributes
  const oldAttrs = oldProduct.attributes || {};
  const allAttrKeys = Array.from(new Set([...Object.keys(oldAttrs), ...Object.keys(cleanAttrs)]));
  let attributesChanged = false;
  for (const key of allAttrKeys) {
    const oldVal = oldAttrs[key] !== undefined && oldAttrs[key] !== null ? String(oldAttrs[key]).trim() : "";
    const newVal = cleanAttrs[key] !== undefined && cleanAttrs[key] !== null ? String(cleanAttrs[key]).trim() : "";
    if (oldVal !== newVal) {
      attributesChanged = true;
      break;
    }
  }

  // 4. Variant Content vs Operational Changes
  const oldVariants = oldProduct.variants || [];
  let variantContentChanged = oldVariants.length !== cleanVariants.length;
  let variantOperationalChanged = false;

  for (const newV of cleanVariants) {
    const matchingOld = newV.id
      ? oldVariants.find((ov) => ov.id === newV.id)
      : oldVariants.find((ov) => ov.sku.trim().toUpperCase() === newV.sku.trim().toUpperCase());

    if (!matchingOld) {
      variantContentChanged = true;
    } else {
      if (
        matchingOld.sku.trim().toUpperCase() !== newV.sku.trim().toUpperCase() ||
        matchingOld.variantName.trim() !== newV.variantName.trim()
      ) {
        variantContentChanged = true;
      }

      if (
        Number(matchingOld.price) !== Number(newV.price) ||
        Number(matchingOld.stockQuantity) !== Number(newV.stockQuantity)
      ) {
        variantOperationalChanged = true;
      }
    }
  }

  const isBaseInfoChanged =
    nameChanged || categoryChanged || brandChanged || descChanged || imagesChanged || attributesChanged || variantContentChanged;

  return {
    isBaseInfoChanged,
    isOperationalChanged: variantOperationalChanged,
  };
}

export function ProductFormView({ productId }: ProductFormViewProps = {}) {
  const router = useRouter();
  const isEditMode = Boolean(productId);

  // Queries
  const { data: rawCategories = [], isLoading: isLoadingCategories } = useSellerCategoriesQuery();
  const categories: CategorySummaryResponse[] = rawCategories || [];
  const createProductMutation = useCreateProductMutation();
  const updateProductMutation = useUpdateProductMutation();
  const submitApprovalMutation = useSubmitProductApprovalMutation();

  // Query product detail if in edit mode
  const { data: productDetail, isLoading: isLoadingDetail } = useSellerProductDetailQuery(productId);

  // React Hook Form + Zod
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    setError,
    clearErrors,
    reset,
    formState: { errors, isValid },
  } = useForm<CreateProductFormValues>({
    resolver: zodResolver(createProductFormSchema),
    defaultValues: {
      name: "",
      categoryId: "",
      brandName: "",
      description: "",
      images: [],
      variants: [
        {
          sku: "ROG-OLED-27-01",
          variantName: "Bản Tiêu Chuẩn",
          price: 19990000,
          stockQuantity: 15,
        },
      ],
      attributes: {},
    },
    mode: "onChange",
  });

  const watchedName = watch("name");
  const watchedCategoryId = watch("categoryId");
  const watchedBrandName = watch("brandName");
  const watchedDescription = watch("description");
  const watchedImages = watch("images");
  const watchedVariants = watch("variants");
  const watchedAttributes = watch("attributes");

  // Pre-fill form when productDetail is loaded in edit mode
  React.useEffect(() => {
    if (productDetail && isEditMode) {
      reset({
        name: productDetail.name,
        categoryId: productDetail.categoryId,
        brandName: productDetail.brandName || "",
        description: productDetail.description || "",
        images:
          productDetail.images && productDetail.images.length > 0
            ? productDetail.images.map((img) => ({
                imageUrl: img.imageUrl,
                displayOrder: img.displayOrder,
              }))
            : [],
        variants:
          productDetail.variants && productDetail.variants.length > 0
            ? productDetail.variants.map((v) => ({
                id: v.id,
                sku: v.sku,
                variantName: v.variantName,
                price: v.price,
                stockQuantity: v.stockQuantity,
                imageUrl: v.imageUrl || undefined,
              }))
            : [
                {
                  sku: "SKU-DEFAULT",
                  variantName: "Bản Tiêu Chuẩn",
                  price: 100000,
                  stockQuantity: 10,
                },
              ],
        attributes: productDetail.attributes || {},
      });
    }
  }, [productDetail, isEditMode, reset]);

  // Submitting status
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittingAction, setSubmittingAction] = useState<"DRAFT" | "SUBMIT" | null>(null);

  // Success Modal & Warning Modal
  const [isSubmittedModalOpen, setIsSubmittedModalOpen] = useState(false);
  const [submittedProductName, setSubmittedProductName] = useState("");
  const [isApprovalWarningModalOpen, setIsApprovalWarningModalOpen] = useState(false);
  const [pendingSubmission, setPendingSubmission] = useState<{
    data: CreateProductFormValues;
    action: "DRAFT" | "SUBMIT";
  } | null>(null);

  // Query category attributes
  const { data: categoryAttrs = [], isLoading: isLoadingAttrs } =
    useCategoryAttributesQuery(watchedCategoryId);

  const selectedCategory = useMemo(() => {
    return categories.find((c) => c.id === watchedCategoryId);
  }, [categories, watchedCategoryId]);

  // Handle category change
  const handleCategoryChange = (newCatId: string) => {
    setValue("categoryId", newCatId, { shouldValidate: true });
    setValue("attributes", {}, { shouldValidate: true });
  };

  const handleAttributeChange = (key: string, val: unknown) => {
    setValue(
      "attributes",
      { ...watchedAttributes, [key]: val },
      { shouldValidate: true }
    );
    clearErrors(`attributes.${key}` as unknown as `attributes`);
  };

  // Convert RHF errors to plain key-value map for cards
  const formattedErrors = useMemo(() => {
    const errMap: Record<string, string> = {};

    if (errors.name?.message) errMap.name = errors.name.message;
    if (errors.categoryId?.message) errMap.categoryId = errors.categoryId.message;
    if (errors.brandName?.message) errMap.brandName = errors.brandName.message;
    if (errors.images?.message || errors.images?.root?.message) {
      errMap.images = (errors.images?.message || errors.images?.root?.message) as string;
    }
    if (errors.variants?.message || errors.variants?.root?.message) {
      errMap.variants = (errors.variants?.message || errors.variants?.root?.message) as string;
    }

    if (errors.variants && Array.isArray(errors.variants)) {
      errors.variants.forEach((vErr, idx) => {
        if (vErr?.sku?.message) errMap[`variant_sku_${idx}`] = vErr.sku.message;
        if (vErr?.variantName?.message) errMap[`variant_name_${idx}`] = vErr.variantName.message;
        if (vErr?.price?.message) errMap[`variant_price_${idx}`] = vErr.price.message;
        if (vErr?.stockQuantity?.message) errMap[`variant_stock_${idx}`] = vErr.stockQuantity.message;
      });
    }

    if (errors.attributes) {
      Object.entries(errors.attributes).forEach(([k, errObj]) => {
        if (errObj && typeof errObj === "object" && "message" in errObj) {
          errMap[`attr_${k}`] = (errObj as { message: string }).message;
        }
      });
    }

    return errMap;
  }, [errors]);

  // Submit Logic with Zod validation
  const onValidSubmit = async (
    data: CreateProductFormValues,
    action: "DRAFT" | "SUBMIT",
    skipApprovalWarning = false
  ) => {
    // Validate dynamic category attributes if present
    if (categoryAttrs.length > 0) {
      let hasAttrError = false;
      for (const attr of categoryAttrs) {
        const val = data.attributes?.[attr.name];
        if (
          val === undefined ||
          val === null ||
          (typeof val === "string" && val.trim() === "")
        ) {
          setError(`attributes.${attr.name}` as unknown as `attributes`, {
            type: "manual",
            message: `Thuộc tính "${attr.name}" là bắt buộc theo chuẩn ngành hàng.`,
          });
          hasAttrError = true;
        }
      }
      if (hasAttrError) {
        setGlobalError("Vui lòng hoàn thiện các trường thông số kỹ thuật bắt buộc.");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }
    }

    // 1. Prepare dynamic category attributes
    const cleanAttrs: Record<string, unknown> = {};
    if (categoryAttrs.length > 0) {
      for (const attr of categoryAttrs) {
        if (data.attributes?.[attr.name] !== undefined && data.attributes?.[attr.name] !== null) {
          cleanAttrs[attr.name] = data.attributes[attr.name];
        }
      }
    }

    // 2. Prepare images
    const cleanImages = data.images.map((img, idx) => ({
      imageUrl: img.imageUrl,
      displayOrder: idx,
    }));

    // 3. Prepare variants
    const cleanVariants = data.variants.map((v) => ({
      id: (v as { id?: string }).id,
      sku: v.sku.trim().toUpperCase(),
      variantName: v.variantName.trim(),
      price: Number(v.price),
      stockQuantity: Number(v.stockQuantity),
      imageUrl: v.imageUrl || cleanImages[0]?.imageUrl,
    }));

    // 4. Dirty Check & Scenario Branching (Edit Mode on APPROVED product)
    if (isEditMode && productId && productDetail) {
      const changes = detectProductChanges(
        productDetail,
        data.name,
        data.categoryId,
        data.brandName,
        data.description,
        cleanImages,
        cleanAttrs,
        cleanVariants
      );

      // KỊCH BẢN B: Sản phẩm đang APPROVED và có thay đổi thông tin cốt lõi -> cần xác nhận cảnh báo
      if (
        productDetail.approvalStatus === "APPROVED" &&
        changes.isBaseInfoChanged &&
        !skipApprovalWarning
      ) {
        setPendingSubmission({ data, action });
        setIsApprovalWarningModalOpen(true);
        return;
      }

      setIsSubmitting(true);
      setSubmittingAction(action);
      setGlobalError(null);

      try {
        // KỊCH BẢN A: Chỉ đổi Giá/Kho (isBaseInfoChanged = false) -> BỎ QUA PUT product gốc!
        // Chỉ gọi PUT product gốc nếu sản phẩm chưa APPROVED hoặc thực sự có đổi thông tin cốt lõi
        const shouldUpdateBaseProduct =
          productDetail.approvalStatus !== "APPROVED" || changes.isBaseInfoChanged;

        if (shouldUpdateBaseProduct) {
          const updatePayload: UpdateProductRequest = {
            categoryId: data.categoryId,
            name: data.name.trim(),
            brandName: data.brandName.trim(),
            description: data.description?.trim() || undefined,
            attributes: cleanAttrs,
            images: cleanImages,
          };
          await updateProductMutation.mutateAsync({ id: productId, data: updatePayload });
        }

        // Cập nhật các biến thể SKU (kể cả kịch bản A hay B)
        for (const v of cleanVariants) {
          if (v.id) {
            try {
              await sellerProductApi.updateVariant(productId, v.id, {
                sku: v.sku,
                variantName: v.variantName,
                price: v.price,
                stockQuantity: v.stockQuantity,
                imageUrl: v.imageUrl,
              });
            } catch (vErr) {
              console.error("Update variant error:", vErr);
            }
          } else {
            try {
              await sellerProductApi.addVariant(productId, {
                sku: v.sku,
                variantName: v.variantName,
                price: v.price,
                stockQuantity: v.stockQuantity,
                imageUrl: v.imageUrl,
              });
            } catch (vErr) {
              console.error("Add variant error:", vErr);
            }
          }
        }

        if (shouldUpdateBaseProduct && action === "SUBMIT") {
          // Bị reset duyệt do đổi thông tin gốc -> gửi duyệt và mở modal
          try {
            await submitApprovalMutation.mutateAsync(productId);
          } catch (approvalErr) {
            console.error("Submit approval error:", approvalErr);
          }
          setSubmittedProductName(data.name);
          setIsSubmittedModalOpen(true);
        } else {
          // KỊCH BẢN A: Chỉ sửa giá/kho -> redirect thẳng về danh sách sản phẩm, giữ nguyên APPROVED
          router.push(ROUTES.SELLER.PRODUCTS);
        }
      } catch (err: unknown) {
        console.error("Update product failed:", err);
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          "Đã có lỗi xảy ra khi cập nhật sản phẩm. Vui lòng thử lại.";
        setGlobalError(msg);
        window.scrollTo({ top: 0, behavior: "smooth" });
      } finally {
        setIsSubmitting(false);
        setSubmittingAction(null);
      }
      return;
    }

    // CREATE MODE:
    setIsSubmitting(true);
    setSubmittingAction(action);
    setGlobalError(null);

    try {
      const requestPayload: CreateProductRequest = {
        categoryId: data.categoryId,
        name: data.name.trim(),
        brandName: data.brandName.trim(),
        description: data.description?.trim() || undefined,
        attributes: cleanAttrs,
        images: cleanImages,
        variants: cleanVariants,
      };

      const res = await createProductMutation.mutateAsync(requestPayload);
      const createdProduct = res.data;

      if (action === "SUBMIT" && createdProduct?.id) {
        // Automatically submit for platform moderation
        try {
          await submitApprovalMutation.mutateAsync(createdProduct.id);
        } catch (approvalErr) {
          console.error("Auto submit approval error:", approvalErr);
        }
        setSubmittedProductName(data.name);
        setIsSubmittedModalOpen(true);
      } else {
        // Save as Draft -> Redirect back to products list
        router.push(ROUTES.SELLER.PRODUCTS);
      }
    } catch (err: unknown) {
      console.error("Create product failed:", err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        "Đã có lỗi xảy ra khi tạo sản phẩm. Vui lòng kiểm tra lại dữ liệu và thử lại.";
      setGlobalError(msg);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } finally {
      setIsSubmitting(false);
      setSubmittingAction(null);
    }
  };

  const handleTriggerSubmit = (action: "DRAFT" | "SUBMIT") => {
    handleSubmit(
      (data) => onValidSubmit(data, action),
      () => {
        setGlobalError("Vui lòng hoàn thiện các trường thông tin còn thiếu hoặc có lỗi phía dưới.");
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    )();
  };

  const handleConfirmApprovalWarning = async () => {
    if (!pendingSubmission) return;
    const { data, action } = pendingSubmission;
    setIsApprovalWarningModalOpen(false);
    setPendingSubmission(null);
    await onValidSubmit(data, action, true);
  };

  const handleResetForm = () => {
    reset({
      name: "",
      categoryId: "",
      brandName: "",
      description: "",
      images: [],
      variants: [
        {
          sku: `TH-${Date.now().toString().slice(-4)}`,
          variantName: "Bản Tiêu Chuẩn",
          price: 100000,
          stockQuantity: 10,
        },
      ],
      attributes: {},
    });
    setGlobalError(null);
    setIsSubmittedModalOpen(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  if (isEditMode && isLoadingDetail) {
    return (
      <div className="bg-white border border-slate-200/80 rounded-2xl p-12 flex flex-col items-center justify-center min-h-[400px]">
        <Loader2 className="w-8 h-8 text-[#0045cc] animate-spin mb-3" />
        <p className="text-xs text-slate-500 font-medium">Đang tải thông tin sản phẩm...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full pb-24">
      {/* Top Breadcrumb & Page Header */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link
            href={ROUTES.SELLER.DASHBOARD}
            className="hover:text-[#0045cc] transition-colors"
          >
            Kênh Người Bán
          </Link>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <Link
            href={ROUTES.SELLER.PRODUCTS}
            className="hover:text-[#0045cc] transition-colors"
          >
            Kho sản phẩm
          </Link>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-slate-900 font-semibold">
            {isEditMode ? "Chỉnh sửa sản phẩm" : "Đăng bán sản phẩm mới"}
          </span>
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link
              href={ROUTES.SELLER.PRODUCTS}
              className="p-2 rounded-xl bg-white border border-slate-200/90 text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors shadow-2xs"
              title="Quay lại danh sách"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <h1 className="font-headline font-bold text-xl text-slate-900 tracking-tight">
                {isEditMode ? "Chỉnh Sửa Sản Phẩm" : "Đăng Bán Sản Phẩm Mới"}
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                {isEditMode
                  ? "Cập nhật thông tin chi tiết, hình ảnh và thuộc tính kỹ thuật linh kiện"
                  : "Thiết lập thông tin linh kiện, thư viện hình ảnh và bảng giá phân loại cấu hình"}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Previously Approved Notice */}
      {isEditMode && productDetail?.approvalStatus === "APPROVED" && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-3 shadow-2xs animate-in fade-in">
          <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-1 font-medium leading-relaxed">
            <strong>Lưu ý về kiểm duyệt:</strong> Sản phẩm này hiện đã được duyệt mở bán. Khi bạn lưu các thay đổi về nội dung, sản phẩm sẽ tự động chuyển về trạng thái <strong>Chờ duyệt</strong> để Ban quản trị TechHub xác thực lại thông số.
          </div>
        </div>
      )}

      {/* Global Error Alert */}
      {globalError && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-3 shadow-2xs animate-in fade-in">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1 font-medium leading-relaxed">{globalError}</div>
        </div>
      )}

      {/* Main 4 Form Cards */}
      <div className="flex flex-col gap-6">
        {/* Card 1: Basic Information */}
        <ProductBasicInfoCard
          name={watchedName}
          onNameChange={(val) => setValue("name", val, { shouldValidate: true })}
          categoryId={watchedCategoryId}
          onCategoryChange={handleCategoryChange}
          brandName={watchedBrandName}
          onBrandNameChange={(val) => setValue("brandName", val, { shouldValidate: true })}
          description={watchedDescription || ""}
          onDescriptionChange={(val) => setValue("description", val, { shouldValidate: true })}
          categories={categories}
          errors={formattedErrors}
        />

        {/* Card 2: Media Gallery */}
        <ProductMediaGalleryCard
          images={watchedImages}
          onChange={(newImages) => setValue("images", newImages, { shouldValidate: true })}
          error={formattedErrors.images}
        />

        {/* Card 3: Dynamic Category Specifications */}
        <ProductCategorySpecsCard
          categoryId={watchedCategoryId}
          categoryName={selectedCategory?.name}
          attributes={categoryAttrs}
          isLoading={isLoadingAttrs}
          values={watchedAttributes || {}}
          onChange={handleAttributeChange}
          errors={formattedErrors}
        />

        {/* Card 4: Variants & Pricing Matrix */}
        <ProductVariantsCard
          variants={watchedVariants}
          onChange={(newVariants) => setValue("variants", newVariants, { shouldValidate: true })}
          productName={watchedName}
          brandName={watchedBrandName}
          errors={formattedErrors}
        />
      </div>

      {/* Sticky Bottom Footer Bar */}
      <ProductFormFooterBar
        imageCount={watchedImages.length}
        variantCount={watchedVariants.length}
        isSubmitting={isSubmitting}
        submittingAction={submittingAction}
        onSaveDraft={() => handleTriggerSubmit("DRAFT")}
        onSubmitApproval={() => handleTriggerSubmit("SUBMIT")}
        isEditMode={isEditMode}
      />

      {/* Success Modal */}
      <ProductSubmittedModal
        isOpen={isSubmittedModalOpen}
        productName={submittedProductName}
        onResetForm={handleResetForm}
      />

      {/* Warning Modal for APPROVED product content changes */}
      <ProductApprovalWarningModal
        isOpen={isApprovalWarningModalOpen}
        onClose={() => {
          setIsApprovalWarningModalOpen(false);
          setPendingSubmission(null);
        }}
        onConfirm={handleConfirmApprovalWarning}
        isLoading={isSubmitting}
      />
    </div>
  );
}
