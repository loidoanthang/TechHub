"use client";

import Link from "next/link";
import { Loader2, Store, AlertCircle, ArrowLeft } from "lucide-react";
import {
  useShopProfileQuery,
  useShopVouchersQuery,
  useShopProductsQuery,
} from "../hooks/useShopQueries";
import { ShopHeaderBanner } from "./ShopHeaderBanner";
import { ShopVouchersList } from "./ShopVouchersList";
import { ShopProductsGrid } from "./ShopProductsGrid";
import { ROUTES } from "@/lib/constants";

interface ShopProfileViewProps {
  sellerName: string;
}

export function ShopProfileView({ sellerName }: ShopProfileViewProps) {
  const {
    data: shop,
    isLoading: isShopLoading,
    error: shopError,
  } = useShopProfileQuery(sellerName);

  const { data: vouchers = [], isLoading: isVouchersLoading } = useShopVouchersQuery(
    sellerName
  );

  const { data: productsData } = useShopProductsQuery(shop?.id, shop?.name);

  if (isShopLoading) {
    return (
      <div className="w-full min-h-[50vh] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-10 h-10 animate-spin text-[#0045cc]" />
        <p className="text-sm font-medium text-[#737688]">Đang tải thông tin cửa hàng...</p>
      </div>
    );
  }

  if (shopError || !shop) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 flex flex-col items-center text-center gap-4">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-500 flex items-center justify-center">
          <Store className="w-8 h-8" />
        </div>
        <h1 className="text-xl font-bold text-[#0b1c30] font-heading">
          Không tìm thấy Cửa hàng
        </h1>
        <p className="text-xs text-[#737688] max-w-md leading-relaxed">
          Cửa hàng với định danh &ldquo;<strong>{sellerName}</strong>&rdquo; không tồn tại hoặc đã tạm dừng hoạt động trên sàn TechHub Marketplace.
        </p>
        <Link
          href={ROUTES.HOME}
          className="mt-2 px-5 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-bold transition-all flex items-center gap-2"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại Trang chủ</span>
        </Link>
      </div>
    );
  }

  return (
    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 flex flex-col gap-8">
      {/* 1. Header Banner & Identity */}
      <ShopHeaderBanner shop={shop} productCount={productsData?.totalElements ?? 0} />

      {/* 2. Exclusive Vouchers */}
      <ShopVouchersList vouchers={vouchers} shopName={shop.name} />

      {/* 3. Shop Products Grid & Filters */}
      <ShopProductsGrid shopId={shop.id} shopName={shop.name} />
    </main>
  );
}
