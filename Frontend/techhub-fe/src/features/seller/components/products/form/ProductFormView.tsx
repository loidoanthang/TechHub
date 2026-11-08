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
  useSubmitProductApprovalMutation,
} from "@/features/seller/hooks/useSellerProducts";
import type { CategorySummaryResponse } from "@/types/category";
import type { CreateProductRequest } from "@/types/sellerProduct";
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
import { ChevronRight, ArrowLeft, AlertCircle } from "lucide-react";

export function ProductFormView() {
  const router = useRouter();

  // Queries
  const { data: rawCategories = [], isLoading: isLoadingCategories } = useSellerCategoriesQuery();
  const categories: CategorySummaryResponse[] = rawCategories || [];
  const createProductMutation = useCreateProductMutation();
  const submitApprovalMutation = useSubmitProductApprovalMutation();

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
      images: [
        {
          imageUrl:
            "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&auto=format&fit=crop&q=80",
          displayOrder: 0,
        },
      ],
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

  // Submitting status
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittingAction, setSubmittingAction] = useState<"DRAFT" | "SUBMIT" | null>(null);

  // Success Modal
  const [isSubmittedModalOpen, setIsSubmittedModalOpen] = useState(false);
  const [submittedProductName, setSubmittedProductName] = useState("");

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
    action: "DRAFT" | "SUBMIT"
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

    setIsSubmitting(true);
    setSubmittingAction(action);
    setGlobalError(null);

    try {
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
        sku: v.sku.trim().toUpperCase(),
        variantName: v.variantName.trim(),
        price: Number(v.price),
        stockQuantity: Number(v.stockQuantity),
        imageUrl: v.imageUrl || cleanImages[0]?.imageUrl,
      }));

      // 4. Construct Request Payload
      const requestPayload: CreateProductRequest = {
        categoryId: data.categoryId,
        name: data.name.trim(),
        brandName: data.brandName.trim(),
        description: data.description?.trim() || undefined,
        attributes: cleanAttrs,
        images: cleanImages,
        variants: cleanVariants,
      };

      // Call API Create Product
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

  const handleResetForm = () => {
    reset({
      name: "",
      categoryId: "",
      brandName: "",
      description: "",
      images: [
        {
          imageUrl:
            "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&auto=format&fit=crop&q=80",
          displayOrder: 0,
        },
      ],
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
          <span className="text-slate-900 font-semibold">Đăng bán sản phẩm mới</span>
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
                Đăng Bán Sản Phẩm Mới
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                Thiết lập thông tin linh kiện, thư viện hình ảnh và bảng giá phân loại cấu hình
              </p>
            </div>
          </div>
        </div>
      </div>

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
      />

      {/* Success Modal */}
      <ProductSubmittedModal
        isOpen={isSubmittedModalOpen}
        productName={submittedProductName}
        onResetForm={handleResetForm}
      />
    </div>
  );
}
