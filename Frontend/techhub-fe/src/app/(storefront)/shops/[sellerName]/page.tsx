import { Suspense } from "react";
import { ShopProfileView, resolveSellerSlug } from "@/features/shops";
import type { Metadata } from "next";

interface ShopPageProps {
  params: Promise<{
    sellerName: string;
  }>;
}

export async function generateMetadata({ params }: ShopPageProps): Promise<Metadata> {
  const { sellerName } = await params;
  const decoded = decodeURIComponent(sellerName);
  const formattedName = decoded
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

  return {
    title: `${formattedName} | Cửa hàng TechHub Marketplace`,
    description: `Khám phá các sản phẩm công nghệ và ưu đãi voucher chính hãng từ ${formattedName} trên TechHub Marketplace.`,
  };
}

async function ShopProfileLoader({ params }: ShopPageProps) {
  const { sellerName } = await params;
  const canonicalSlug = resolveSellerSlug(decodeURIComponent(sellerName));
  return <ShopProfileView sellerName={canonicalSlug} />;
}

export default function ShopPage({ params }: ShopPageProps) {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#f8fafc] py-8">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
            <div className="h-64 bg-slate-200/70 rounded-3xl animate-pulse" />
            <div className="h-28 bg-slate-200/70 rounded-2xl animate-pulse" />
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-80 bg-slate-200/70 rounded-2xl animate-pulse" />
              ))}
            </div>
          </div>
        </div>
      }
    >
      <ShopProfileLoader params={params} />
    </Suspense>
  );
}
