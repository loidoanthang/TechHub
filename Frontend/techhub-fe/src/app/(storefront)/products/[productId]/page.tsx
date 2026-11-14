import { notFound, redirect } from "next/navigation";
import { connection } from "next/server";
import { ProductDetailView } from "@/features/products/components";
import {
  resolveProductDetail,
  resolveProductMetadata,
} from "@/features/products/api/productResolver";
import type { Metadata } from "next";

// Blocking route: product detail reads runtime data (connection()) before
// render; opt out of prerendering per project convention.
export const instant = false;

interface ProductPageProps {
  params: Promise<{ productId: string }>;
}

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { productId } = await params;
  return resolveProductMetadata(productId);
}

export default async function ProductDetailPage({ params }: ProductPageProps) {
  await connection();
  const { productId } = await params;

  const result = await resolveProductDetail(productId);

  if (result.type === "REDIRECT") {
    redirect(`/products/${result.targetSlug}`);
  }

  if (result.type === "NOT_FOUND") {
    notFound();
  }

  return <ProductDetailView product={result.data} />;
}
