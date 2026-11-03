import { Suspense } from "react";
import { connection } from "next/server";
import { ProductDetailView } from "@/features/products/components";
import { productApi } from "@/features/products/api/productApi";
import { mapPublicDetailToProductDetailData } from "@/features/products/utils/productMapper";
import { getMockProductDetail } from "@/mocks";
import type { Metadata } from "next";

interface ProductPageProps {
  params: Promise<{
    productId: string;
  }>;
}

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  await connection();
  const { productId } = await params;
  try {
    const res = await productApi.getProductBySlug(productId);
    if (res?.data) {
      return {
        title: `${res.data.name} | TechHub Marketplace`,
        description: res.data.description || "High-performance tech hardware on TechHub",
      };
    }
  } catch {
    // Fallback to title from slug
  }
  return {
    title: "Product Details | TechHub Marketplace",
  };
}

async function ProductDetailLoader({ params }: ProductPageProps) {
  await connection();
  const { productId } = await params;

  try {
    // Attempt fetching real product from Backend by slug
    const res = await productApi.getProductBySlug(productId);
    if (res?.data) {
      const realProductData = mapPublicDetailToProductDetailData(res.data);
      return <ProductDetailView product={realProductData} />;
    }
  } catch (err) {
    // If not found in DB, fallback to mock details for seamless developer testing
    console.warn(`Product slug "${productId}" not found on backend DB, falling back to mock:`, err);
  }

  const mockProductData = getMockProductDetail(productId);
  return <ProductDetailView product={mockProductData} />;
}

export default function ProductDetailPage(props: ProductPageProps) {
  return (
    <Suspense
      fallback={
        <div className="max-w-6xl mx-auto px-6 py-20 text-center text-sm text-slate-500">
          Loading hardware specifications...
        </div>
      }
    >
      <ProductDetailLoader {...props} />
    </Suspense>
  );
}
